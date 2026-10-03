# -*- coding: utf-8 -*-
# 一次性迁移脚本（2026-10-03）：补全电缆阶梯——每个太阳能面板对应一根电缆（共 56 档面板绑定 + 永夜 = 57）。
# 规则（用户指定）：输电上限 = 对应面板「最大输出」的 4 倍；
#                  永夜电缆 = 最强发电机（天终）蓄电上限的 64 倍。
# 同时：重写 GlassCableTier.java、给 gen_cables_iu.py 补 MAP/NAMES、克隆 21 个新档的 JSON 资产、插中英 lang。
from __future__ import annotations
import io, os, re, glob

BASE = os.path.dirname(os.path.abspath(__file__))
A = os.path.normpath(os.path.join(BASE, '..', 'src', 'main', 'resources', 'assets', 'solarpower'))
JAVA = os.path.normpath(os.path.join(BASE, '..', 'src', 'main', 'java', 'com', 'example',
                                     'solarpower', 'solar', 'GlassCableTier.java'))
GEN = os.path.join(BASE, 'gen_cables_iu.py')

# ---------- 1) 解析 SolarTier：enum 名 → (面板 id, 主题色, 中文名, 英文名) ----------
solar = io.open(SRC := os.path.normpath(os.path.join(BASE, '..', 'src', 'main', 'java', 'com',
                'example', 'solarpower', 'solar', 'SolarTier.java')), encoding='utf-8').read()
panels = {}
for m in re.finditer(r'^    ([A-Z_]+)\("([a-z_0-9]+)", (\d+), \d+,.*?0x([0-9A-Fa-f]{6})\)[,;]', solar, re.M | re.S):
    panels[m.group(1)] = {'pid': m.group(2), 'accent': int(m.group(4), 16)}
assert len(panels) == 67, len(panels)

def lang_names():
    zh, en = {}, {}
    for fn, d in (('zh_cn.lang', zh), ('en_us.lang', en)):
        for line in io.open(os.path.join(A, 'lang', fn), encoding='utf-8'):
            m = re.match(r'tile\.solarpower\.([a-z_0-9]+)\.name=(.+)', line.strip())
            if m:
                d[m.group(1)] = m.group(2)
    return zh, en
ZHL, ENL = lang_names()

# ---------- 2) 电缆阶梯定义（顺序 = 面板顺序 + 永夜殿后） ----------
# (enum 名, 电缆 id, 线损, 绑定面板 enum 名)；id=None 表示沿用既有 id（在 KEEP 表里查）
KEEP = {
    'BASIC': ('glass_cable', 0.06),
    'ADVANCED': ('advanced_glass_cable', 0.065),
    'BASIC': ('glass_cable', 0.06),
    'ADVANCED': ('advanced_glass_cable', 0.065),
    'HYBRID': ('glass_cable_2', 0.07),
    'ULTIMATE_HYBRID': ('ultimate_hybrid_glass_cable', 0.08),
    'QUANTUM': ('glass_cable_4', 0.09),
    'SPECTRAL': ('glass_cable_5', 0.10),
    'PROTON': ('glass_cable_6', 0.11),
    'SINGULAR': ('glass_cable_7', 0.12),
    'DIFFRACTION': ('glass_cable_8', 0.15),
    'PHOTONIC': ('glass_cable_9', 0.18),
    'NEUTRON': ('glass_cable_10', 0.20),
    'BARYON': ('baryon_glass_cable', 0.21),
    'HADRON': ('hadron_glass_cable', 0.22),
    'GRAVITON': ('graviton_glass_cable', 0.23),
    'QUARK': ('glass_cable_11', 0.25),
    'FUSION': ('glass_cable_12', 0.30),
    'OMNIVERSAL': ('glass_cable_13', 0.32),
    'VACUUM_DECAY': ('glass_cable_14', 0.34),
    'PARADOX': ('glass_cable_15', 0.36),
    'APOTHEOSIS': ('glass_cable_16', 0.38),
    'CHAOS': ('glass_cable_17', 0.40),
    'VOID_RIFT': ('glass_cable_18', 0.42),
    'NEGENTROPY': ('glass_cable_19', 0.44),
    'AWAKENING': ('glass_cable_20', 0.46),
    'CREATOR': ('glass_cable_21', 0.48),
    'STAR_SEA': ('glass_cable_22', 0.50),
    'SUPREME_PRINCIPLE': ('glass_cable_23', 0.52),
    'FINALE': ('glass_cable_24', 0.54),
    'FINALITY': ('glass_cable_25', 0.56),
    'DIVINE_LIGHT': ('glass_cable_26', 0.58),
    'FIRMAMENT': ('glass_cable_27', 0.60),
    'PANTHEON': ('glass_cable_28', 0.62),
    'MANDATE_OF_HEAVEN': ('glass_cable_29', 0.64),
    'APOCALYPSE': ('glass_cable_30', 0.66),
    'HEAVENS_END': ('glass_cable_31', 0.68),
    'ETERNAL_NIGHT': ('glass_cable_32', 0.70),
}
NEW_LOSS = {
    'ANTIMATTER': 0.304, 'ZERO_POINT': 0.308, 'DARK_ENERGY': 0.312, 'COSMIC_STRING': 0.316,
    'HYPERDIMENSIONAL': 0.318, 'VOID': 0.322, 'SINGULARITY': 0.324, 'STAR_CORE': 0.326,
    'GENESIS': 0.328, 'SUPERSTRING': 0.332, 'BRANE': 0.334, 'HEAT_DEATH': 0.336,
    'ZEROING': 0.338,
    'ORDER': 0.405, 'SANDS_OF_TIME': 0.41, 'ARBITER': 0.425, 'PRIMORDIAL': 0.43,
    'FINAL_LAW': 0.435, 'CONVERGENCE': 0.445, 'SUPREME_ONE': 0.45, 'GODHEAD': 0.455,
    'ENDGAME': 0.4525, 'THE_ABSOLUTE': 0.455, 'SUPREME_VOID': 0.4575,
    'ETERNITY': 0.3625, 'TRANSCENDENT': 0.365, 'MYRIAD': 0.3675, 'DEMIURGE': 0.37,
    'BOUNDLESS': 0.3825, 'ABSOLUTE_INFINITY': 0.385, 'ABYSS': 0.3875, 'ANNIHILATION': 0.39,
}
ORDER = ['BASIC', 'ADVANCED', 'HYBRID', 'ULTIMATE_HYBRID', 'QUANTUM', 'SPECTRAL', 'PROTON',
         'SINGULAR', 'DIFFRACTION', 'PHOTONIC', 'NEUTRON', 'BARYON', 'HADRON', 'GRAVITON',
         'QUARK', 'FUSION', 'ANTIMATTER', 'ZERO_POINT', 'DARK_ENERGY', 'COSMIC_STRING',
         'HYPERDIMENSIONAL', 'OMNIVERSAL', 'VOID', 'SINGULARITY', 'STAR_CORE', 'GENESIS',
         'VACUUM_DECAY', 'SUPERSTRING', 'BRANE', 'HEAT_DEATH', 'ZEROING', 'PARADOX',
         'ETERNITY', 'TRANSCENDENT', 'MYRIAD', 'DEMIURGE', 'APOTHEOSIS', 'BOUNDLESS',
         'ABSOLUTE_INFINITY', 'ABYSS', 'ANNIHILATION', 'CHAOS', 'ORDER', 'SANDS_OF_TIME',
         'VOID_RIFT', 'ARBITER', 'PRIMORDIAL', 'FINAL_LAW', 'CONVERGENCE', 'SUPREME_ONE',
         'GODHEAD', 'NEGENTROPY', 'ENDGAME', 'THE_ABSOLUTE', 'SUPREME_VOID', 'AWAKENING',
         'CREATOR', 'STAR_SEA', 'SUPREME_PRINCIPLE', 'FINALE', 'FINALITY', 'DIVINE_LIGHT',
         'FIRMAMENT', 'PANTHEON', 'MANDATE_OF_HEAVEN', 'APOCALYPSE', 'HEAVENS_END',
         'ETERNAL_NIGHT']
assert len(ORDER) == 68

PANEL_NAME = {'ULTIMATE_HYBRID': 'PERFECT', 'ETERNAL_NIGHT': 'HEAVENS_END'}

def panel_of(name):
    return panels[PANEL_NAME.get(name, name)]

def cable_id(name):
    if name in KEEP:
        return KEEP[name][0]
    return panel_of(name)['pid'][:-len('_solar_panel')] + '_glass_cable'

def cable_loss(name):
    return KEEP[name][1] if name in KEEP else NEW_LOSS[name]

def mixw(c, t):
    return tuple(round(v + (255 - v) * t) for v in c)

# ---------- 3) 重写 GlassCableTier.java ----------
consts = []
for name in ORDER:
    cid, loss = cable_id(name), cable_loss(name)
    accent = 0x2B2B2B if name == 'ETERNAL_NIGHT' else panel_of(name)['accent']
    if name == 'ETERNAL_NIGHT':
        consts.append('    %s("%s", %.2fD, SolarTier.HEAVENS_END, 64L, 0x%06X);'
                      % (name, cid, loss, accent))
    else:
        consts.append('    %s("%s", %.2fD, SolarTier.%s, 0x%06X),'
                      % (name, cid, loss, PANEL_NAME.get(name, name), accent))

java = '''package com.example.solarpower.solar;

import java.math.BigInteger;

/**
 * 玻璃电缆等级（2026-10-03 补全：与太阳能面板一一对应，共 57 档，含无面板的永夜）。
 * <p>输电上限 = 对应面板「最大输出」的 4 倍——一根电缆保证满载带动 4 根同级面板；
 * 末档永夜电缆没有同名面板，容量取最强发电机（天终）蓄电上限的 64 倍
 * （满仓放电也带得走）。容量用 {@link BigInteger} 承载，不设上限。
 * <p>线损（EU/格）为手工调校的装饰性数值。
 */
public enum GlassCableTier {

''' + '\n'.join(consts) + '''

    private final String id;
    private final double lossPerBlock;
    private final BigInteger capacityPerTick;
    /** 主体色（与贴图管线 gen_cables_iu.py 的 mid 一致），用于物品名等显示。 */
    private final int midColor;

    GlassCableTier(String id, double lossPerBlock, SolarTier source, int midColor) {
        this.id = id;
        this.lossPerBlock = lossPerBlock;
        // 输电上限 = 对应面板「最大输出」的 4 倍
        this.capacityPerTick = source.maxOutputEu().multiply(BigInteger.valueOf(4));
        this.midColor = midColor;
    }

    /** 永夜：容量 = 最强发电机（天终）蓄电上限 × 指定倍数。 */
    GlassCableTier(String id, double lossPerBlock, SolarTier source, long storageMult, int midColor) {
        this.id = id;
        this.lossPerBlock = lossPerBlock;
        this.capacityPerTick = source.capacityEu().multiply(BigInteger.valueOf(storageMult));
        this.midColor = midColor;
    }

    /** 注册名（同时也是贴图、模型与方块的文件名）。 */
    public String id() {
        return this.id;
    }

    /** 线损（EU/格）：每经过一根电缆，传输量扣除该数值。 */
    public double lossPerBlock() {
        return this.lossPerBlock;
    }

    /** 主体色（0xRRGGBB）。 */
    public int midColor() {
        return this.midColor;
    }

    /** 单根电缆每 tick 的最大传输量（EU）。 */
    public BigInteger capacityPerTick() {
        return this.capacityPerTick;
    }

    /** 方块/物品的翻译键（1.12.2 使用 tile. 前缀）。 */
    public String translationKey() {
        return "tile.solarpower." + this.id;
    }

    /**
     * 配方所用「XX小块阳光化合物」对应的太阳能档位（合成表：中列本档小块，
     * 两侧上一档电缆）。除终极混合对应面板 PERFECT 档、永夜沿用天终档外，其余同名。
     */
    public SolarTier partTier() {
        switch (this) {
            case ULTIMATE_HYBRID: return SolarTier.PERFECT;
            case ETERNAL_NIGHT: return SolarTier.HEAVENS_END;
            default:
                return SolarTier.valueOf(name());
        }
    }
}
'''
io.open(JAVA, 'w', encoding='utf-8', newline='\n').write(java)
print('GlassCableTier.java rewritten:', len(ORDER), 'tiers')

# ---------- 4) gen_cables_iu.py 的 MAP/NAMES 补全新档 ----------
gen = io.open(GEN, encoding='utf-8').read()
map_m = re.search(r'MAP = \[.*?\n\]', gen, re.S)
names_m = re.search(r'NAMES = \[.*?\n\]', gen, re.S)
old_map_rows = dict()
for mm in re.finditer(r'\("([^"]+)", \(([\d, ]+)\), \(([\d, ]+)\), \(([\d, ]+)\)\)', map_m.group(0)):
    old_map_rows[mm.group(1)] = (
    tuple(int(v) for v in mm.group(2).split(',')),
    tuple(int(v) for v in mm.group(3).split(',')),
    tuple(int(v) for v in mm.group(4).split(',')))

new_rows, new_names = [], []
for name in ORDER:
    cid = cable_id(name)
    if cid in old_map_rows:
        d, m_, b = old_map_rows[cid]
    else:
        acc = panel_of(name)['accent']
        c = ((acc >> 16) & 255, (acc >> 8) & 255, acc & 255)
        d = tuple(round(v * 0.55) for v in c)
        b = mixw(c, 0.6)
        m_ = c
    new_rows.append('    ("%s", (%d, %d, %d), (%d, %d, %d), (%d, %d, %d)),'
                    % (cid, d[0], d[1], d[2], m_[0], m_[1], m_[2], b[0], b[1], b[2]))
    if name == 'ETERNAL_NIGHT':
        new_names.append('"永夜玻璃电缆"')
        continue
    base_zh = ZHL[panel_of(name)['pid']]
    new_names.append('"%s"' % (base_zh.replace('太阳能发电机', '') + '玻璃电缆'))

gen = gen[:map_m.start()] + 'MAP = [\n' + '\n'.join(new_rows) + '\n]' + gen[map_m.end():]
gen = re.sub(r'NAMES = \[.*?\n\]', 'NAMES = [\n    ' + ', '.join(new_names) + '\n]', gen, count=1, flags=re.S)
io.open(GEN, 'w', encoding='utf-8', newline='\n').write(gen)
print('gen_cables_iu.py MAP/NAMES updated:', len(new_rows), 'entries')

# ---------- 5) 克隆 21 个新档的 JSON 资产（模板 = glass_cable_2） ----------
TEMPLATE = 'glass_cable_2'
rels = ['blockstates/%s.json', 'models/item/%s.json', 'models/block/%s_core.json',
        'models/block/block/%s_core.json', 'models/block/block/%s.json']
rels += ['models/block/%s_arm_%s.json' % ('%s', d) for d in ('down', 'east', 'north', 'south', 'up', 'west')]
rels += ['models/block/block/%s_arm_%s.json' % ('%s', d) for d in ('down', 'east', 'north', 'south', 'up', 'west')]

new_ids = [cable_id(n) for n in ORDER if n not in KEEP]
total = 0
for cid in new_ids:
    n = 0
    for rel in rels:
        src = os.path.join(A, rel % TEMPLATE)
        dst = os.path.join(A, rel % cid)
        s = io.open(src, encoding='utf-8').read().replace(TEMPLATE, cid)
        io.open(dst, 'w', encoding='utf-8', newline='\n').write(s)
        n += 1
    total += n
print('cloned', total, 'jsons for', len(new_ids), 'new tiers')

# ---------- 6) lang：新档名插在前一档电缆名之后 ----------
for fn, mk in (('lang/zh_cn.lang', lambda zh: zh.replace('太阳能发电机', '') + '玻璃电缆'),
               ('lang/en_us.lang', lambda en: en.replace(' Solar Panel', '') + ' Glass Fibre Cable')):
    path = os.path.join(A, fn)
    s = io.open(path, encoding='utf-8').read()
    inserted = 0
    for k, name in enumerate(ORDER):
        if name in KEEP:
            continue
        cid = cable_id(name)
        key = 'tile.solarpower.%s.name=' % cid
        if key in s:
            continue
        prev_cid = cable_id(ORDER[k - 1])
        prev_key = 'tile.solarpower.%s.name=' % prev_cid
        i = s.index(prev_key)
        e = s.index('\n', i) + 1
        pname = (ZHL if 'zh_cn' in fn else ENL)[panel_of(name)['pid']]
        line = key + (mk(pname))
        s = s[:e] + line + '\n' + s[e:]
        inserted += 1
    io.open(path, 'w', encoding='utf-8', newline='\n').write(s)
    print(fn, inserted, 'lines inserted')
print('DONE')
