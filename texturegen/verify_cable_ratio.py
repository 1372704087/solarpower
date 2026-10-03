# -*- coding: utf-8 -*-
# 校验：每根电缆容量 = 同名面板最大输出 × 4（永夜 = 天终蓄电 × 64）。
import io, re, sys
sys.set_int_max_str_digits(100000)

solar = io.open('../src/main/java/com/example/solarpower/solar/SolarTier.java', encoding='utf-8').read()
srows = re.findall(
    r'^    ([A-Z_]+)\("([a-z_0-9]+)", (\d+), (\d+), (\d+)L, (\d+)L, (\d+)L, (\d+)L, (\d+)L, (\d+)L, 0x',
    solar, re.M)
gen, out, store = {}, {}, {}
for r in srows:
    name = r[0]
    e = int(r[3])
    nN, nD, oN, oD, sN, sD = map(int, r[4:10])
    gen[name] = 2 ** e
    out[name] = 2 ** e * oN // oD
    store[name] = 2 ** e * sN // sD
print('面板数:', len(gen))

cable = io.open('../src/main/java/com/example/solarpower/solar/GlassCableTier.java', encoding='utf-8').read()
zh = {}
for line in io.open('../src/main/resources/assets/solarpower/lang/zh_cn.lang', encoding='utf-8'):
    m = re.match(r'tile\.solarpower\.([^=]+)\.name=(.+)', line.strip())
    if m and 'glass_cable' in m.group(1):
        zh[m.group(1)] = m.group(2)

print('%-10s %-28s %-16s %-16s %s' % ('电缆', 'id', '容量 EU/t', '面板最大输出', '倍数'))
bad = []
for m in re.finditer(r'^    ([A-Z_]+)\("([a-z_0-9]+)", ([\d.]+)D, SolarTier\.([A-Z_]+)(?:, (\d+)L)?',
                     cable, re.M):
    name, cid, loss, src, smult = m.groups()
    if smult:  # 永夜：天终蓄电 ×64
        cap = store[src] * 64
        print('%-10s %-28s %-16s %-16s %s' % (zh[cid], cid, '{:,}'.format(cap), '天终蓄电×64', '64'))
        continue
    cap = out[src] * 4
    if cap != out[src] * 4:
        bad.append(name)
    print('%-10s %-28s %-16s %-16s %.0f' % (zh[cid], cid, '{:,}'.format(cap), '{:,}'.format(out[src]), cap / out[src]))
print('倍数≠4:', bad if bad else '无')
