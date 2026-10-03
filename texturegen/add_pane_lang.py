# -*- coding: utf-8 -*-
# 一次性脚本：从 lang 文件的面板名派生「XX玻璃板」条目，插入到 quantum_core 行之后。
# 派生规则：zh 去掉「太阳能发电机」后缀加「玻璃板」（基础档为「基础玻璃板」）；
#           en 去掉 " Solar Panel" 后缀加 " Glass Pane"。
# 幂等：重跑时先清掉上一次插入的行（按派生出的 pane id 集合精确匹配，不碰光辉玻璃板）。
import io, os

BASE = os.path.dirname(os.path.abspath(__file__))
LANG_DIR = os.path.normpath(os.path.join(BASE, '..', 'src', 'main', 'resources',
                                         'assets', 'solarpower', 'lang'))

def pane_id(tier_id):
    if tier_id == 'solar_panel':
        return 'basic_glass_pane'
    return tier_id[:-len('_solar_panel')] + '_glass_pane'

def pane_name_zh(panel_name):
    base = panel_name[:-len('太阳能发电机')]
    return (base or '基础') + '玻璃板'

def pane_name_en(panel_name):
    base = panel_name[:-len(' Solar Panel')]
    return base + ' Glass Pane'

for fn, derive in (('zh_cn.lang', pane_name_zh), ('en_us.lang', pane_name_en)):
    path = os.path.join(LANG_DIR, fn)
    lines = io.open(path, encoding='utf-8').read().splitlines()
    panes = {}
    for line in lines:
        if '.name=' in line and line.startswith('tile.solarpower.'):
            key = line.split('.name=', 1)[0][len('tile.solarpower.'):]
            if key == 'solar_panel' or key.endswith('_solar_panel'):
                panes[pane_id(key)] = derive(line.split('.name=', 1)[1])
    assert len(panes) == 67, (fn, len(panes))
    pane_lines = ['item.solarpower.%s.name=%s' % (pid, panes[pid]) for pid in panes]
    out = []
    inserted = False
    for line in lines:
        if line.startswith('item.solarpower.') and line.endswith('.name=') is False:
            key = line.split('.name=', 1)[0][len('item.solarpower.'):]
            if key in panes:
                continue  # 上一次插入的行，先清掉
        out.append(line)
        if not inserted and line.startswith('item.solarpower.quantum_core.name='):
            out.extend(pane_lines)
            inserted = True
    assert inserted, fn
    io.open(path, 'w', encoding='utf-8', newline='\n').write('\n'.join(out) + '\n')
    print(fn, len(pane_lines), 'panes inserted')
