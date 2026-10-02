package com.example.solarpower.solar;

import java.math.BigInteger;

/**
 * IU 玻璃电缆等级，数值取自 IU 1.21.1-3.4.0.11 的
 * {@code CableType}：线损（EU/格）与每 tick 最大传输量（EU）。
 * <p>传输量用 {@link BigInteger} 承载，取消 long 封顶。
 * <p>前 11 档（BASIC~QUARK）沿用 IU 原始数值（第 11 档 IU 原名 INFINITY，
 * 本模组改称 QUARK，把「无限」留给真正的顶档语义，避免中游档位占用）。
 * 扩展 20 档（FUSION~HEAVENS_END）覆盖太阳能 12~66 档：每 5 档太阳能为一组，
 * 容量取该组最高太阳能发电量的 10 倍；末尾 11 档（56~66）跨度过大（每档 ×10⁵⁰ 起），
 * 各自单独一档电缆。
 */
public enum GlassCableTier {

    BASIC("glass_cable", 0.06D, 4_096L, 0x9FB0C0),
    HYBRID("glass_cable_2", 0.07D, 8_192L, 0x4FD1C5),
    ULTIMATE_HYBRID("ultimate_hybrid_glass_cable", 0.08D, 32_768L, 0x7ED957),
    QUANTUM("glass_cable_4", 0.09D, 131_072L, 0x4EA8FF),
    SPECTRAL("glass_cable_5", 0.10D, 524_288L, 0xB07CFF),
    PROTON("glass_cable_6", 0.11D, 2_097_152L, 0xFF7BAE),
    SINGULAR("glass_cable_7", 0.12D, 8_388_608L, 0xC9D4E0),
    DIFFRACTION("glass_cable_8", 0.15D, 33_554_432L, 0xFF5C5C),
    PHOTONIC("glass_cable_9", 0.18D, 134_217_728L, 0xFFE066),
    NEUTRON("glass_cable_10", 0.20D, 536_870_912L, 0x8FA6C9),
    QUARK("glass_cable_11", 0.25D, 8_589_934_590L, 0xF0F4FF),

    // 每 5 档太阳能为一组，容量取该组最高太阳能发电量的 10 倍
    FUSION("glass_cable_12", 0.30D, SolarTier.FUSION, 0xFF9440),
    OMNIVERSAL("glass_cable_13", 0.32D, SolarTier.OMNIVERSAL, 0x304FFE),
    VACUUM_DECAY("glass_cable_14", 0.34D, SolarTier.VACUUM_DECAY, 0xFFD24A),
    PARADOX("glass_cable_15", 0.36D, SolarTier.PARADOX, 0x00C2A8),
    APOTHEOSIS("glass_cable_16", 0.38D, SolarTier.APOTHEOSIS, 0xFFEB3B),
    CHAOS("glass_cable_17", 0.40D, SolarTier.CHAOS, 0x9E00FF),
    VOID_RIFT("glass_cable_18", 0.42D, SolarTier.VOID_RIFT, 0x6200EA),
    NEGENTROPY("glass_cable_19", 0.44D, SolarTier.NEGENTROPY, 0x18FFFF),
    AWAKENING("glass_cable_20", 0.46D, SolarTier.AWAKENING, 0x82B1FF),
    // 末尾 4 档跨度过大，各自单独一档
    CREATOR("glass_cable_21", 0.48D, SolarTier.CREATOR, 0xFFAB40),
    STAR_SEA("glass_cable_22", 0.50D, SolarTier.STAR_SEA, 0x69F0AE),
    SUPREME_PRINCIPLE("glass_cable_23", 0.52D, SolarTier.SUPREME_PRINCIPLE, 0xFF6E40),
    FINALE("glass_cable_24", 0.54D, SolarTier.FINALE, 0xB20000),
    FINALITY("glass_cable_25", 0.56D, SolarTier.FINALITY, 0xB2FF59),
    // 神话档（62~67）：容量取对应面板发电量的 10 倍，主体色取该面板 3 色渐变的中间色
    DIVINE_LIGHT("glass_cable_26", 0.58D, SolarTier.DIVINE_LIGHT, 0xFFD66E),
    FIRMAMENT("glass_cable_27", 0.60D, SolarTier.FIRMAMENT, 0x64A5FF),
    PANTHEON("glass_cable_28", 0.62D, SolarTier.PANTHEON, 0xA064D2),
    MANDATE_OF_HEAVEN("glass_cable_29", 0.64D, SolarTier.MANDATE_OF_HEAVEN, 0xEBAF46),
    APOCALYPSE("glass_cable_30", 0.66D, SolarTier.APOCALYPSE, 0xE16919),
    HEAVENS_END("glass_cable_31", 0.68D, SolarTier.HEAVENS_END, 0x732DA5),
    // 永夜：不按发电量定容，直接自适应最高档面板的蓄电上限 ×10（满仓放电也带得走）
    ETERNAL_NIGHT("glass_cable_32", 0.70D, SolarTier.HEAVENS_END, true, 0x2B2B2B);

    private final String id;
    private final double lossPerBlock;
    private final BigInteger capacityPerTick;
    /** 主体色（与贴图管线 gen_cables_iu.py 的 mid 一致），用于物品名等显示。 */
    private final int midColor;

    GlassCableTier(String id, double lossPerBlock, long capacityPerTick, int midColor) {
        this.id = id;
        this.lossPerBlock = lossPerBlock;
        this.capacityPerTick = BigInteger.valueOf(capacityPerTick);
        this.midColor = midColor;
    }

    /** 扩展档：容量取对应太阳能组顶档发电量的 10 倍（发电量绑定太紧，满仓放电被电缆卡成瓶颈）。 */
    GlassCableTier(String id, double lossPerBlock, SolarTier source, int midColor) {
        this(id, lossPerBlock, source, false, midColor);
    }

    /** 扩展档：{@code byCapacity} 为真时容量取对应面板蓄电上限的 10 倍，否则取发电量的 10 倍。 */
    GlassCableTier(String id, double lossPerBlock, SolarTier source, boolean byCapacity, int midColor) {
        this.id = id;
        this.lossPerBlock = lossPerBlock;
        BigInteger base = byCapacity ? source.capacityEu() : source.generationEu();
        this.capacityPerTick = base.multiply(BigInteger.TEN);
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
}
