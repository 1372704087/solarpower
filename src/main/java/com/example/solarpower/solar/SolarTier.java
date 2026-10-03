package com.example.solarpower.solar;

import com.example.solarpower.energy.EuFormat;
import com.example.solarpower.energy.EuTier;
import com.example.solarpower.util.ChatCodes;

import java.math.BigDecimal;
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

    BASIC("solar_panel", 1, 5, 1L, 3L, 2L, 1L, 16L, 1L, 0x9FB0C0),
    ADVANCED("advanced_solar_panel", 1, 7, 1L, 3L, 2L, 1L, 16L, 1L, 0xE0B45C),
    HYBRID("hybrid_solar_panel", 2, 9, 1L, 3L, 2L, 1L, 16L, 1L, 0x4FD1C5),
    PERFECT("perfect_solar_panel", 3, 11, 1L, 3L, 2L, 1L, 16L, 1L, 0x7ED957),
    QUANTUM("quantum_solar_panel", 4, 13, 1L, 3L, 2L, 1L, 16L, 1L, 0x4EA8FF),
    SPECTRAL("spectral_solar_panel", 5, 15, 2L, 5L, 5L, 2L, 32L, 1L, 0xB07CFF),
    PROTON("proton_solar_panel", 6, 18, 2L, 5L, 5L, 2L, 32L, 1L, 0xFF7BAE),
    SINGULAR("singular_solar_panel", 7, 21, 2L, 5L, 5L, 2L, 32L, 1L, 0xC9D4E0),
    DIFFRACTION("diffraction_solar_panel", 8, 24, 2L, 5L, 5L, 2L, 32L, 1L, 0xFF5C5C),
    PHOTONIC("photonic_solar_panel", 9, 27, 2L, 5L, 5L, 2L, 32L, 1L, 0xFFE066),
    NEUTRON("neutron_solar_panel", 10, 31, 3L, 7L, 3L, 1L, 64L, 1L, 0x8FA6C9),
    BARYON("baryon_solar_panel", 11, 35, 3L, 7L, 3L, 1L, 64L, 1L, 0xC98A5A),
    HADRON("hadron_solar_panel", 12, 39, 3L, 7L, 3L, 1L, 64L, 1L, 0x59D2FF),
    GRAVITON("graviton_solar_panel", 13, 43, 3L, 7L, 3L, 1L, 64L, 1L, 0x7F6BFF),
    QUARK("quark_solar_panel", 14, 47, 3L, 7L, 3L, 1L, 64L, 1L, 0xF0F4FF),
    FUSION("fusion_solar_panel", 15, 55, 1L, 2L, 7L, 2L, 128L, 1L, 0xFF9440),
    ANTIMATTER("antimatter_solar_panel", 16, 63, 1L, 2L, 7L, 2L, 128L, 1L, 0xF03CE0),
    ZERO_POINT("zero_point_solar_panel", 17, 71, 1L, 2L, 7L, 2L, 128L, 1L, 0xC8FF3E),
    DARK_ENERGY("dark_energy_solar_panel", 18, 79, 1L, 2L, 7L, 2L, 128L, 1L, 0x8C9EFF),
    COSMIC_STRING("cosmic_string_solar_panel", 19, 87, 1L, 2L, 7L, 2L, 128L, 1L, 0x4DE8B0),
    HYPERDIMENSIONAL("hyperdimensional_solar_panel", 20, 95, 1L, 2L, 7L, 2L, 128L, 1L, 0xFF8C69),
    OMNIVERSAL("omniversal_solar_panel", 21, 111, 4L, 7L, 4L, 1L, 256L, 1L, 0x304FFE),
    VOID("void_solar_panel", 22, 127, 4L, 7L, 4L, 1L, 256L, 1L, 0x6A4CFF),
    SINGULARITY("singularity_solar_panel", 23, 143, 4L, 7L, 4L, 1L, 256L, 1L, 0xFF3B6B),
    STAR_CORE("star_core_solar_panel", 24, 159, 4L, 7L, 4L, 1L, 256L, 1L, 0x00E5A0),
    GENESIS("genesis_solar_panel", 25, 175, 4L, 7L, 4L, 1L, 256L, 1L, 0xFFD24A),
    VACUUM_DECAY("vacuum_decay_solar_panel", 26, 191, 4L, 7L, 4L, 1L, 256L, 1L, 0x9B5CFF),
    SUPERSTRING("superstring_solar_panel", 27, 207, 4L, 7L, 4L, 1L, 256L, 1L, 0x39D0FF),
    BRANE("brane_solar_panel", 28, 223, 4L, 7L, 4L, 1L, 256L, 1L, 0xFF7A1A),
    HEAT_DEATH("heat_death_solar_panel", 29, 239, 4L, 7L, 4L, 1L, 256L, 1L, 0xE8E8FF),
    ZEROING("zeroing_solar_panel", 30, 255, 4L, 7L, 4L, 1L, 256L, 1L, 0x00C2A8),
    PARADOX("paradox_solar_panel", 31, 271, 4L, 7L, 4L, 1L, 256L, 1L, 0xFF2E88),
    ETERNITY("eternity_solar_panel", 32, 303, 4L, 7L, 9L, 2L, 512L, 1L, 0x7C4DFF),
    TRANSCENDENT("transcendent_solar_panel", 33, 335, 4L, 7L, 9L, 2L, 512L, 1L, 0xFFB300),
    MYRIAD("myriad_solar_panel", 34, 367, 4L, 7L, 9L, 2L, 512L, 1L, 0x00B0FF),
    DEMIURGE("demiurge_solar_panel", 35, 399, 4L, 7L, 9L, 2L, 512L, 1L, 0xF44336),
    APOTHEOSIS("apotheosis_solar_panel", 36, 431, 4L, 7L, 9L, 2L, 512L, 1L, 0xFFEB3B),
    BOUNDLESS("boundless_solar_panel", 37, 463, 4L, 7L, 9L, 2L, 512L, 1L, 0x00E676),
    ABSOLUTE_INFINITY("absolute_infinity_solar_panel", 38, 495, 4L, 7L, 9L, 2L, 512L, 1L, 0xD500F9),
    ABYSS("abyss_solar_panel", 39, 527, 4L, 7L, 9L, 2L, 512L, 1L, 0xFFFFFF),
    ANNIHILATION("annihilation_solar_panel", 40, 591, 2L, 3L, 5L, 1L, 1024L, 1L, 0xFF1744),
    CHAOS("chaos_solar_panel", 41, 655, 2L, 3L, 5L, 1L, 1024L, 1L, 0x9E00FF),
    ORDER("order_solar_panel", 42, 719, 2L, 3L, 5L, 1L, 1024L, 1L, 0x00E5FF),
    SANDS_OF_TIME("sands_of_time_solar_panel", 43, 783, 2L, 3L, 5L, 1L, 1024L, 1L, 0xFFC400),
    VOID_RIFT("void_rift_solar_panel", 44, 847, 2L, 3L, 5L, 1L, 1024L, 1L, 0x6200EA),
    ARBITER("arbiter_solar_panel", 45, 911, 2L, 3L, 5L, 1L, 1024L, 1L, 0x00FF7F),
    PRIMORDIAL("primordial_solar_panel", 46, 975, 2L, 3L, 5L, 1L, 1024L, 1L, 0xFF6D00),
    FINAL_LAW("final_law_solar_panel", 47, 1039, 2L, 3L, 5L, 1L, 1024L, 1L, 0xAEEA00),
    CONVERGENCE("convergence_solar_panel", 48, 1167, 2L, 3L, 6L, 1L, 2048L, 1L, 0x00B8D4),
    SUPREME_ONE("supreme_one_solar_panel", 49, 1295, 2L, 3L, 6L, 1L, 2048L, 1L, 0xE040FB),
    GODHEAD("godhead_solar_panel", 50, 1423, 2L, 3L, 6L, 1L, 2048L, 1L, 0xFFF176),
    NEGENTROPY("negentropy_solar_panel", 51, 1679, 2L, 3L, 8L, 1L, 8192L, 1L, 0x18FFFF),
    ENDGAME("endgame_solar_panel", 52, 1935, 2L, 3L, 8L, 1L, 8192L, 1L, 0xFF4081),
    THE_ABSOLUTE("the_absolute_solar_panel", 53, 2191, 2L, 3L, 8L, 1L, 8192L, 1L, 0xB388FF),
    SUPREME_VOID("supreme_void_solar_panel", 54, 2447, 2L, 3L, 8L, 1L, 8192L, 1L, 0x64FFDA),
    AWAKENING("awakening_solar_panel", 55, 2703, 2L, 3L, 8L, 1L, 8192L, 1L, 0x82B1FF),
    CREATOR("creator_solar_panel", 56, 2959, 2L, 3L, 8L, 1L, 8192L, 1L, 0xFFAB40),
    STAR_SEA("star_sea_solar_panel", 57, 162959, 4L, 5L, 16L, 1L, 16384L, 1L, 0x69F0AE),
    SUPREME_PRINCIPLE("supreme_principle_solar_panel", 58, 619935, 4L, 5L, 64L, 1L, 32768L, 1L, 0xFF6E40),
    FINALE("finale_solar_panel", 59, 2120560, 4L, 5L, 1024L, 1L, 65536L, 1L, 0xB20000),
    FINALITY("finality_solar_panel", 60, 5600241, 4L, 5L, 65536L, 1L, 131072L, 1L, 0xB2FF59),
    DIVINE_LIGHT("divine_light_solar_panel", 61, 5874866, 6L, 7L, 131072L, 1L, 2147483648L, 1L, 0x966919),
    FIRMAMENT("firmament_solar_panel", 62, 6232777, 6L, 7L, 262144L, 1L, 17179869184L, 1L, 0x193782),
    PANTHEON("pantheon_solar_panel", 63, 6689310, 6L, 7L, 524288L, 1L, 137438953472L, 1L, 0x4B1978),
    MANDATE_OF_HEAVEN("mandate_of_heaven_solar_panel", 64, 7260977, 6L, 7L, 2097152L, 1L, 1099511627776L, 1L, 0xFFF0C8),
    APOCALYPSE("apocalypse_solar_panel", 65, 7990097, 6L, 7L, 16777216L, 1L, 8796093022208L, 1L, 0x460F0A),
    HEAVENS_END("heavens_end_solar_panel", 66, 8990097, 6L, 7L, 268435456L, 1L, 140737488355328L, 1L, 0x732DA5);

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
    /** 容量的紧凑镜像：能量管比例用，见 {@link #capacityMirror()}。 */
    private BigDecimal capacityMirror;

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
        if (this.capacityScientific == null) {
            String sci = EuFormat.scientificApprox(this.capacityEu);
            // scientificApprox 在数值 < 10^6 时返回 null（基础档容量仅数百 EU），
            // 必须先判空再取用，否则悬停能量行会 NPE。
            this.capacityScientific = sci == null ? "" : sci;
        }
        return this.capacityScientific.isEmpty() ? null : this.capacityScientific;
    }

    /**
     * 容量的紧凑镜像（保留前 18 位有效数字）——供界面算能量管比例。
     * <p>界面侧拿到的蓄电量只有 4 位有效数字，若直接与精确容量做
     * {@code BigDecimal.divide}/{@code compareTo}，JDK 会把两边补到同一 scale，
     * 把本档容量（最高 2^8990144，约 270 万位）完整物化出来：实测一次除法 240 ms、
     * 一次比较 500 ms，而界面上这两步每帧都要做。压缩到 18 位后两者同量级，
     * 比较与除法退化成 long 运算（实测 0.0006 ms），且比例误差 < 1e-15。
     */
    public BigDecimal capacityMirror() {
        if (this.capacityMirror == null) {
            this.capacityMirror = EuFormat.compactMirror(this.capacityEu, 18);
        }
        return this.capacityMirror;
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
