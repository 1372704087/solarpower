# -*- coding: utf-8 -*-
# 一次性迁移脚本（2026-10-03）：按用户的新发电量规格重写 SolarTier.java。
# 规格要点：发电量 = 2^genExp 逐档爬梯（分类等级步进 ×4/×8/×16/×32/×64/×128/×512/×2048/×8192，
# 最终/神话档为独立大步进）；最大输出 = 发电量 × 输出倍数；蓄电 = 发电量 × 存储倍数；
# 夜间 = 发电量 × 夜间比例（向下取整）。档位的 iuTier 与主体色从旧文件原样保留。
from __future__ import annotations
import io, re, os

C1 = ((1, 3), (2, 1), (16, 1))
C2 = ((2, 5), (5, 2), (32, 1))
C3 = ((3, 7), (3, 1), (64, 1))
C4 = ((1, 2), (7, 2), (128, 1))
C5 = ((4, 7), (4, 1), (256, 1))
C6 = ((4, 7), (9, 2), (512, 1))
C7 = ((2, 3), (5, 1), (1024, 1))
C8 = ((2, 3), (6, 1), (2048, 1))
C9 = ((2, 3), (8, 1), (8192, 1))

SPEC: dict[str, tuple] = {}
for n, e in [('BASIC', 5), ('ADVANCED', 7), ('HYBRID', 9), ('PERFECT', 11), ('QUANTUM', 13)]:
    SPEC[n] = (e, C1)
for n, e in [('SPECTRAL', 15), ('PROTON', 18), ('SINGULAR', 21), ('DIFFRACTION', 24), ('PHOTONIC', 27)]:
    SPEC[n] = (e, C2)
for n, e in [('NEUTRON', 31), ('BARYON', 35), ('HADRON', 39), ('GRAVITON', 43), ('QUARK', 47)]:
    SPEC[n] = (e, C3)
for n, e in [('FUSION', 55), ('ANTIMATTER', 63), ('ZERO_POINT', 71), ('DARK_ENERGY', 79),
             ('COSMIC_STRING', 87), ('HYPERDIMENSIONAL', 95)]:
    SPEC[n] = (e, C4)
for n, e in [('OMNIVERSAL', 111), ('VOID', 127), ('SINGULARITY', 143), ('STAR_CORE', 159),
             ('GENESIS', 175), ('VACUUM_DECAY', 191), ('SUPERSTRING', 207), ('BRANE', 223),
             ('HEAT_DEATH', 239), ('ZEROING', 255), ('PARADOX', 271)]:
    SPEC[n] = (e, C5)
for n, e in [('ETERNITY', 303), ('TRANSCENDENT', 335), ('MYRIAD', 367), ('DEMIURGE', 399),
             ('APOTHEOSIS', 431), ('BOUNDLESS', 463), ('ABSOLUTE_INFINITY', 495), ('ABYSS', 527)]:
    SPEC[n] = (e, C6)
for n, e in [('ANNIHILATION', 591), ('CHAOS', 655), ('ORDER', 719), ('SANDS_OF_TIME', 783),
             ('VOID_RIFT', 847), ('ARBITER', 911), ('PRIMORDIAL', 975), ('FINAL_LAW', 1039)]:
    SPEC[n] = (e, C7)
# 分类8：归一 ×512 入门，太一/神格 ×2048（神格之后的分类边界见交付说明）
for n, e in [('CONVERGENCE', 1167), ('SUPREME_ONE', 1295), ('GODHEAD', 1423)]:
    SPEC[n] = (e, C8)
# 分类9：逆熵 ×8192 起到创世者
for n, e in [('NEGENTROPY', 1679), ('ENDGAME', 1935), ('THE_ABSOLUTE', 2191), ('SUPREME_VOID', 2447),
             ('AWAKENING', 2703), ('CREATOR', 2959)]:
    SPEC[n] = (e, C9)
# 最终（独立倍率）
SPEC['STAR_SEA'] = (162959, ((4, 5), (16, 1), (16384, 1)))
SPEC['SUPREME_PRINCIPLE'] = (619935, ((4, 5), (64, 1), (32768, 1)))
SPEC['FINALE'] = (2120560, ((4, 5), (1024, 1), (65536, 1)))
SPEC['FINALITY'] = (5600241, ((4, 5), (65536, 1), (131072, 1)))
# 神话（独立倍率）
SPEC['DIVINE_LIGHT'] = (5874866, ((6, 7), (131072, 1), (2147483648, 1)))
SPEC['FIRMAMENT'] = (6232777, ((6, 7), (262144, 1), (17179869184, 1)))
SPEC['PANTHEON'] = (6689310, ((6, 7), (524288, 1), (137438953472, 1)))
SPEC['MANDATE_OF_HEAVEN'] = (7260977, ((6, 7), (2097152, 1), (1099511627776, 1)))
SPEC['APOCALYPSE'] = (7990097, ((6, 7), (16777216, 1), (8796093022208, 1)))
SPEC['HEAVENS_END'] = (8990097, ((6, 7), (268435456, 1), (140737488355328, 1)))

BASE = os.path.dirname(os.path.abspath(__file__))
SRC = os.path.normpath(os.path.join(BASE, '..', 'src', 'main', 'java', 'com', 'example',
                                    'solarpower', 'solar', 'SolarTier.java'))
old = io.open(SRC, encoding='utf-8').read()
starts = [m for m in re.finditer(r'^    ([A-Z_]+)\("([a-z_]+)",\s*(\d+),', old, re.M)]
tiers = []
for i, m in enumerate(starts):
    end = starts[i + 1].start() if i + 1 < len(starts) else len(old)
    accent = re.search(r'0x([0-9A-Fa-f]{6})', old[m.start():end]).group(0)
    tiers.append((m.group(1), m.group(2), int(m.group(3)), accent))
assert len(tiers) == 67, len(tiers)

lines = []
for k, (name, cid, iu, accent) in enumerate(tiers):
    e, mults = SPEC[name]
    (nn, nd), (on, od), (sn, sd) = mults
    tail = ');' if k == len(tiers) - 1 else '),'
    fmt = '    %s("%s", %d, %d, %dL, %dL, %dL, %dL, %dL, %dL, %s' + tail
    lines.append(fmt % (name, cid, iu, e, nn, nd, on, od, sn, sd, accent))
consts = '\n'.join(lines)

header = '''package com.example.solarpower.solar;

import com.example.solarpower.energy.EuFormat;
import com.example.solarpower.energy.EuTier;
import com.example.solarpower.util.ChatCodes;

import java.math.BigInteger;

/**
 * 全部太阳能面板档位（2026-10-03 用户重设计：全套数值按 2 的幂爬梯）。
 * <p>每档由 4 个量定义，其中 3 个从发电量派生：
 * <ul>
 *     <li>发电量 = 2^genExp（EU/t），前档 × 当前倍数逐级累积；</li>
 *     <li>最大输出 = 发电量 × 输出倍数——本板每 tick 向电缆网络推送的上限
 *     （白天先蓄后发可短时超发电量放电，夜间靠蓄电同样最多推到该值）；</li>
 *     <li>蓄电上限 = 发电量 × 存储倍数；</li>
 *     <li>夜间发电 = 发电量 × 夜间比例（向下取整）。</li>
 * </ul>
 * 分类等级（组内共享夜间比例与倍率；括号内为发电量步进）：
 * <ol>
 *     <li>基础~量子（×4）：夜 1/3、输出 ×2、存储 ×16；</li>
 *     <li>光谱~光子（光谱 ×4 后 ×8）：夜 2/5、输出 ×2.5、存储 ×32；</li>
 *     <li>中子~夸克（×16）：夜 3/7、输出 ×3、存储 ×64；</li>
 *     <li>聚变~超维（聚变 ×16 后 ×32）：夜 1/2、输出 ×3.5、存储 ×128；</li>
 *     <li>全能宇宙~悖论（全能宇宙 ×32 后 ×64）：夜 4/7、输出 ×4、存储 ×256；</li>
 *     <li>永恒~归墟（永恒 ×64 后 ×128）：夜 4/7、输出 ×4.5、存储 ×512；</li>
 *     <li>湮灭~终律（湮灭 ×128 后 ×512）：夜 2/3、输出 ×5、存储 ×1024；</li>
 *     <li>归一~神格（归一 ×512 后 ×2048）：夜 2/3、输出 ×6、存储 ×2048；</li>
 *     <li>逆熵~创世者（逆熵 ×8192 起）：夜 2/3、输出 ×8、存储 ×8192；</li>
 *     <li>最终（星海~终末）：夜 4/5，输出 ×16/×64/×1024/×65536，存储 ×16384/×32768/×65536/×131072；</li>
 *     <li>神话（神光~天终）：夜 6/7，输出 ×131072 递增到 ×268435456，存储 ×2^31 递增到 ×2^47。</li>
 * </ol>
 * 电压等级 iuTier 与主体色沿用旧版。发电量用 {@link BigInteger} 承载（天终 = 2^985 EU/t）。
 */
public enum SolarTier {

'''

footer = '''
    private final String id;
    private final int iuTier;
    /** 发电量指数：发电量 = 2^genExp（EU/t）。 */
    private final int genExp;
    private final long nightNum;
    private final long nightDen;
    private final long outNum;
    private final long outDen;
    private final long storeNum;
    private final long storeDen;
    private final int accentColor;

    private final BigInteger generationEu;
    private final BigInteger maxOutputEu;
    private final BigInteger capacityEu;
    /** 夜间发电量惰性缓存：tooltip 每帧取用，须返回同一实例以便上层按身份缓存。 */
    private BigInteger nightGenerationEu;
    /** 容量与发电量的缩写串惰性缓存：档位常量，界面每帧取用，避免反复做 O(n²) 转换。 */
    private String capacityLabel;
    private String capacityScientific;
    private String generationLabel;
    private String generationAnnotated;
    private String nightGenerationAnnotated;
    private String capacityAnnotated;
    private String maxOutputAnnotated;

    SolarTier(String id, int iuTier, int genExp, long nightNum, long nightDen,
              long outNum, long outDen, long storeNum, long storeDen, int accentColor) {
        this.id = id;
        this.iuTier = iuTier;
        this.genExp = genExp;
        this.nightNum = nightNum;
        this.nightDen = nightDen;
        this.outNum = outNum;
        this.outDen = outDen;
        this.storeNum = storeNum;
        this.storeDen = storeDen;
        this.accentColor = accentColor;
        this.generationEu = BigInteger.ONE.shiftLeft(genExp);
        this.maxOutputEu = this.generationEu
                .multiply(BigInteger.valueOf(outNum)).divide(BigInteger.valueOf(outDen));
        this.capacityEu = this.generationEu
                .multiply(BigInteger.valueOf(storeNum)).divide(BigInteger.valueOf(storeDen));
    }

    /** 注册名（同时也是贴图与模型的文件名）。 */
    public String id() {
        return this.id;
    }

    /** IU 里的 tier 编号（1 起）。 */
    public int iuTier() {
        return this.iuTier;
    }

    /** 满日照下的发电量（EU/t）。 */
    public BigInteger generationEu() {
        return this.generationEu;
    }

    /** 夜间（月光）发电量（EU/t）：本档夜间比例 × 发电量，向下取整且不足 1 归零。 */
    public BigInteger nightGenerationEu() {
        if (this.nightGenerationEu == null) {
            BigInteger night = this.generationEu
                    .multiply(BigInteger.valueOf(this.nightNum))
                    .divide(BigInteger.valueOf(this.nightDen));
            this.nightGenerationEu = night.signum() < 1 ? BigInteger.ZERO : night;
        }
        return this.nightGenerationEu;
    }

    /** 最大输出（EU/t）：本板每 tick 向网络推送的 EU 上限（可用蓄电超发）。 */
    public BigInteger maxOutputEu() {
        return this.maxOutputEu;
    }

    /** 内部缓存上限（EU）。 */
    public BigInteger capacityEu() {
        return this.capacityEu;
    }

    /** 容量的已格式化缩写（档位常量，惰性缓存一次）。 */
    public String capacityLabel() {
        if (this.capacityLabel == null) {
            this.capacityLabel = EuFormat.formatEu(this.capacityEu);
        }
        return this.capacityLabel;
    }

    /** 容量的科学计数法约数，小于 10^6 时为 null（档位常量，惰性缓存一次）。 */
    public String capacityScientific() {
        String sci = EuFormat.scientificApprox(this.capacityEu);
        return sci.isEmpty() ? null : sci;
    }

    /** 白天发电量的已格式化缩写（档位常量，惰性缓存一次）。 */
    public String generationLabel() {
        if (this.generationLabel == null) {
            this.generationLabel = EuFormat.formatEu(this.generationEu);
        }
        return this.generationLabel;
    }

    /** 白天发电量的「缩写 + 科学计数法注记」（工具提示用，档位常量，惰性缓存一次）。 */
    public String generationAnnotated() {
        if (this.generationAnnotated == null) {
            this.generationAnnotated = EuFormat.annotated(this.generationEu);
        }
        return this.generationAnnotated;
    }

    /** 夜间发电量的「缩写 + 科学计数法注记」（工具提示用，档位常量，惰性缓存一次）。 */
    public String nightGenerationAnnotated() {
        if (this.nightGenerationAnnotated == null) {
            this.nightGenerationAnnotated = EuFormat.annotated(nightGenerationEu());
        }
        return this.nightGenerationAnnotated;
    }

    /** 最大输出的「缩写 + 科学计数法注记」（工具提示用，档位常量，惰性缓存一次）。 */
    public String maxOutputAnnotated() {
        if (this.maxOutputAnnotated == null) {
            this.maxOutputAnnotated = EuFormat.annotated(this.maxOutputEu);
        }
        return this.maxOutputAnnotated;
    }

    /** 容量的「缩写 + 科学计数法注记」（工具提示用，档位常量，惰性缓存一次）。 */
    public String capacityAnnotated() {
        if (this.capacityAnnotated == null) {
            this.capacityAnnotated = EuFormat.annotated(this.capacityEu);
        }
        return this.capacityAnnotated;
    }

    /** 电压等级，与 IU 的 tier 编号一一对应。 */
    public EuTier voltage() {
        return EuTier.byIuTier(this.iuTier);
    }

    /** 方块/物品的翻译键（1.12.2 使用 tile. 前缀）。 */
    public String translationKey() {
        return "tile.solarpower." + this.id;
    }

    /** 对应「XX玻璃板」面板材料的注册名（基础档特例；其余为去掉 _solar_panel 后缀加 _glass_pane）。 */
    public String paneId() {
        if (this == BASIC) {
            return "basic_glass_pane";
        }
        return id().substring(0, id().length() - "_solar_panel".length()) + "_glass_pane";
    }

    /** 对应「XX小块阳光化合物」材料的注册名（基础档即小块阳光化合物本体的注册名）。 */
    public String sunnariumPartId() {
        if (this == BASIC) {
            return "sunnarium_part";
        }
        return id().substring(0, id().length() - "_solar_panel".length()) + "_sunnarium_part";
    }

    /** 对应「XX阳光化合物」材料的注册名（基础档即阳光化合物本体的注册名）。 */
    public String sunnariumId() {
        if (this == BASIC) {
            return "sunnarium";
        }
        return id().substring(0, id().length() - "_solar_panel".length()) + "_sunnarium";
    }

    /** 对应「XX阳光合金」材料的注册名（基础档即阳光合金本体的注册名）。 */
    public String sunnariumAlloyId() {
        if (this == BASIC) {
            return "sunnarium_alloy";
        }
        return id().substring(0, id().length() - "_solar_panel".length()) + "_sunnarium_alloy";
    }

    /** 对应「XX核心」材料的注册名（仿 IU 激发核，2 档起才存在，基础档无本体）。 */
    public String coreId() {
        if (this == BASIC) {
            return "basic_core";
        }
        return id().substring(0, id().length() - "_solar_panel".length()) + "_core";
    }

    /** 档位主题色（0xRRGGBB），GUI 电池片着色用。 */
    public int accentColor() {
        return this.accentColor;
    }

    /** 与主题色最接近的原版 16 色代码（§ 前缀），用于物品名等只能用原版色板的地方。 */
    public String accentCode() {
        return ChatCodes.nearest(this.accentColor);
    }
}
'''

io.open(SRC, 'w', encoding='utf-8', newline='\n').write(header + consts + '\n' + footer)

# 自校验：重读生成的常量，检查整除与步进
s = io.open(SRC, encoding='utf-8').read()
rows = re.findall(r'^    ([A-Z_]+)\("[a-z_0-9]+", (\d+), (\d+), (\d+)L, (\d+)L, (\d+)L, (\d+)L, (\d+)L, (\d+)L, 0x[0-9A-Fa-f]{6}\)[,;]', s, re.M)
assert len(rows) == 67, len(rows)
prev = 0
sys_path = os.path.normpath(os.path.join(BASE, '..', 'src'))
for r in rows:
    name = r[0]
    e = int(r[2])
    nn, nd, on, od, sn, sd = map(int, r[3:9])
    g = 2 ** e
    assert g * on % od == 0, name
    assert g * sn % sd == 0, name
    step = (g // prev) if prev else 0
    if prev:
        assert step & (step - 1) == 0, (name, step)   # 步进必须是 2 的幂
    prev = g
print('SolarTier.java rewritten: 67 tiers, all steps are powers of two')
