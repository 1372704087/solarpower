# -*- coding: utf-8 -*-
# 修补：新电缆 zh 名称被写成「英文面板名+玻璃电缆」——原因是 fn.startswith('zh')
# 对 'lang/zh_cn.lang' 永远为 False。本脚本按面板中文名重写 21 行，并修正 migrate
# 脚本的判断条件（改用 'zh_cn' in fn）。
from __future__ import annotations
import io, os, re

BASE = os.path.dirname(os.path.abspath(__file__))
A = os.path.normpath(os.path.join(BASE, '..', 'src', 'main', 'resources', 'assets', 'solarpower'))

# 1) 面板 id → 中文名（从 zh_cn.lang 的面板行解析）
zh_panels = {}
for line in io.open(os.path.join(A, 'lang', 'zh_cn.lang'), encoding='utf-8'):
    m = re.match(r'tile\.solarpower\.([a-z_0-9]+_solar_panel)\.name=(.+)', line.strip())
    if m:
        zh_panels[m.group(1)] = m.group(2)

NEW_IDS = ['antimatter_glass_cable', 'zero_point_glass_cable', 'dark_energy_glass_cable',
           'cosmic_string_glass_cable', 'hyperdimensional_glass_cable', 'void_glass_cable',
           'singularity_glass_cable', 'star_core_glass_cable', 'genesis_glass_cable',
           'superstring_glass_cable', 'brane_glass_cable', 'heat_death_glass_cable',
           'zeroing_glass_cable', 'eternity_glass_cable', 'transcendent_glass_cable',
           'myriad_glass_cable', 'demiurge_glass_cable', 'boundless_glass_cable',
           'absolute_infinity_glass_cable', 'abyss_glass_cable', 'annihilation_glass_cable']

path = os.path.join(A, 'lang', 'zh_cn.lang')
s = io.open(path, encoding='utf-8').read()
fixed = 0
for cid in NEW_IDS:
    pid = cid[:-len('_glass_cable')] + '_solar_panel'
    zh = zh_panels[pid].replace('太阳能发电机', '') + '玻璃电缆'
    pat = 'tile.solarpower.%s.name=' % cid
    i = s.index(pat)
    e = s.index('\n', i)
    s = s[:i] + pat + zh + s[e:]
    fixed += 1
io.open(path, 'w', encoding='utf-8', newline='\n').write(s)
print('zh lines fixed:', fixed)

# 2) 修正 migrate_cable_tiers.py 的文件判断
mp = 'migrate_cable_tiers.py'
ms = io.open(mp, encoding='utf-8').read()
old = "pname = (ZHL if fn.startswith('zh') else ENL)[panels[name]['pid']]"
new = "pname = (ZHL if 'zh_cn' in fn else ENL)[panels[name]['pid']]"
if old in ms:
    ms = ms.replace(old, new)
    io.open(mp, 'w', encoding='utf-8', newline='\n').write(ms)
    print('migrate lang detection fixed')
else:
    print('migrate detection already OK')
