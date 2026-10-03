# -*- coding: utf-8 -*-
# 一次性脚本：从 lang 文件的面板名派生「XX阳光化合物」「XX小块阳光化合物」条目
# （2 档起各 66 行），插入到 bluecomponent（SSP 光谱组件）行之后。
# 基础档沿用两张本体，不加行；富集阳光化合物本体不动。
# 幂等：重跑时先清掉上一次插入的行（按派生出的 id 集合精确匹配）。
import io, os

BASE = os.path.dirname(os.path.abspath(__file__))
LANG_DIR = os.path.normpath(os.path.join(BASE, '..', 'src', 'main', 'resources',
                                         'assets', 'solarpower', 'lang'))

FAMILIES = [
    ('_sunnarium', '阳光化合物', ' Sunnarium'),
    ('_sunnarium_part', '小块阳光化合物', ' Sunnarium Part'),
    ('_sunnarium_alloy', '阳光合金', ' Sunnarium Alloy'),
]

def base_id(tier_id):
    return tier_id[:-len('_solar_panel')]

for fn in ('zh_cn.lang', 'en_us.lang'):
    path = os.path.join(LANG_DIR, fn)
    lines = io.open(path, encoding='utf-8').read().splitlines()
    variants = {}
    for line in lines:
        if '.name=' in line and line.startswith('tile.solarpower.'):
            key = line.split('.name=', 1)[0][len('tile.solarpower.'):]
            if key.endswith('_solar_panel') and key != 'solar_panel':
                panel_name = line.split('.name=', 1)[1]
                for suffix, zh_tail, en_tail in FAMILIES:
                    if fn.startswith('zh'):
                        variants[base_id(key) + suffix] = \
                            panel_name[:-len('太阳能发电机')] + zh_tail
                    else:
                        variants[base_id(key) + suffix] = \
                            panel_name[:-len(' Solar Panel')] + en_tail
    assert len(variants) == 198, (fn, len(variants))
    new_lines = ['item.solarpower.%s.name=%s' % (vid, variants[vid]) for vid in variants]
    out = []
    inserted = False
    for line in lines:
        if line.startswith('item.solarpower.') and '.name=' in line:
            key = line.split('.name=', 1)[0][len('item.solarpower.'):]
            if key in variants:
                continue  # 上一次插入的行，先清掉
        out.append(line)
        if not inserted and line.startswith('item.solarpower.bluecomponent.name='):
            out.extend(new_lines)
            inserted = True
    assert inserted, fn
    io.open(path, 'w', encoding='utf-8', newline='\n').write('\n'.join(out) + '\n')
    print(fn, len(new_lines), 'variant lines inserted')
