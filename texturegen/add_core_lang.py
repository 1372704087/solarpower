# -*- coding: utf-8 -*-
# 一次性脚本：从 lang 文件的面板名派生「XX核心」条目（2 档起 66 行），
# 插入到最后一条 _sunnarium_alloy 行之后。基础档无核心本体，不加行。
# 幂等：重跑时先清掉上一次插入的行（按派生出的 id 集合精确匹配）。
import io, os

BASE = os.path.dirname(os.path.abspath(__file__))
LANG_DIR = os.path.normpath(os.path.join(BASE, '..', 'src', 'main', 'resources',
                                         'assets', 'solarpower', 'lang'))

def core_id(tier_id):
    return tier_id[:-len('_solar_panel')] + '_core'

for fn in ('zh_cn.lang', 'en_us.lang'):
    path = os.path.join(LANG_DIR, fn)
    lines = io.open(path, encoding='utf-8').read().splitlines()
    cores = {}
    for line in lines:
        if '.name=' in line and line.startswith('tile.solarpower.'):
            key = line.split('.name=', 1)[0][len('tile.solarpower.'):]
            if key.endswith('_solar_panel') and key != 'solar_panel':
                panel_name = line.split('.name=', 1)[1]
                if fn.startswith('zh'):
                    cores[core_id(key)] = panel_name[:-len('太阳能发电机')] + '核心'
                else:
                    cores[core_id(key)] = panel_name[:-len(' Solar Panel')] + ' Core'
    assert len(cores) == 66, (fn, len(cores))
    new_lines = ['item.solarpower.%s.name=%s' % (cid, cores[cid]) for cid in cores]
    anchor = None
    for line in lines:                       # 锚点：最后一条合金变体行
        if '_sunnarium_alloy.name=' in line:
            anchor = line
    out = []
    inserted = False
    for line in lines:
        if line.startswith('item.solarpower.') and '.name=' in line:
            key = line.split('.name=', 1)[0][len('item.solarpower.'):]
            if key in cores:
                continue  # 上一次插入的行，先清掉
        out.append(line)
        if not inserted and anchor is not None and line == anchor:
            out.extend(new_lines)
            inserted = True
    assert inserted, fn
    io.open(path, 'w', encoding='utf-8', newline='\n').write('\n'.join(out) + '\n')
    print(fn, len(new_lines), 'core lines inserted')
