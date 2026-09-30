package com.example.solarpower.solar;

import java.math.BigInteger;

/**
 * IU 玻璃电缆等级，数值取自 IU 1.21.1-3.4.0.11 的
 * {@code CableType}：线损（EU/格）与每 tick 最大传输量（EU）。
 * <p>传输量用 {@link BigInteger} 承载，取消 long 封顶。
 * <p>前 11 档（BASIC~INFINITY）沿用 IU 原始数值。
 * 扩展 13 档（FUSION~FINALITY）覆盖太阳能 12~59 档：每 5 档太阳能为一组，
 * 容量取该组最高太阳能的发电量；末尾 4 档（56~59）跨度过大（每档 ×10⁵⁰），
 * 各自单独一档电缆。
 */
public enum GlassCableTier {

    BASIC("glass_cable", 0.06D, 4_096L, 0xFFD957),
    HYBRID("glass_cable_2", 0.07D, 8_192L, 0xCB31F1),
    PERFECT("glass_cable_3", 0.08D, 32_768L, 0x959595),
    QUANTUM("glass_cable_4", 0.09D, 131_072L, 0xE34141),
    SPECTRAL("glass_cable_5", 0.10D, 524_288L, 0x30C730),
    PROTON("glass_cable_6", 0.11D, 2_097_152L, 0x462EBF),
    SINGULAR("glass_cable_7", 0.12D, 8_388_608L, 0xE0E0E0),
    DIFFRACTION("glass_cable_8", 0.15D, 33_554_432L, 0xFAF37D),
    PHOTONIC("glass_cable_9", 0.18D, 134_217_728L, 0x3CDBD7),
    NEUTRON("glass_cable_10", 0.20D, 536_870_912L, 0x6F5A25),
    INFINITY("glass_cable_11", 0.25D, 8_589_934_590L, 0x535353),

    // 每 5 档太阳能为一组，容量取该组最高太阳能的发电量
    FUSION("glass_cable_12", 0.30D, SolarTier.FUSION, 0xE87820),
    OMNIVERSAL("glass_cable_13", 0.32D, SolarTier.OMNIVERSAL, 0x8034D6),
    VACUUM_DECAY("glass_cable_14", 0.34D, SolarTier.VACUUM_DECAY, 0xAC2CDC),
    PARADOX("glass_cable_15", 0.36D, SolarTier.PARADOX, 0xE22C84),
    APOTHEOSIS("glass_cable_16", 0.38D, SolarTier.APOTHEOSIS, 0xF0D050),
    CHAOS("glass_cable_17", 0.40D, SolarTier.CHAOS, 0xA024D2),
    VOID_RIFT("glass_cable_18", 0.42D, SolarTier.VOID_RIFT, 0x3828AA),
    NEGENTROPY("glass_cable_19", 0.44D, SolarTier.NEGENTROPY, 0x2CCDD7),
    AWAKENING("glass_cable_20", 0.46D, SolarTier.AWAKENING, 0x3A96EB),
    // 末尾 4 档跨度过大，各自单独一档
    CREATOR("glass_cable_21", 0.48D, SolarTier.CREATOR, 0x34C448),
    STAR_SEA("glass_cable_22", 0.50D, SolarTier.STAR_SEA, 0x2A4ABE),
    ETERNAL_SILENCE("glass_cable_23", 0.52D, SolarTier.SUPREME_PRINCIPLE, 0x808694),
    FINALITY("glass_cable_24", 0.54D, SolarTier.THE_ALL, 0xDEC682);

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

    /** 扩展档：容量直接取对应太阳能档的发电量。 */
    GlassCableTier(String id, double lossPerBlock, SolarTier source, int midColor) {
        this.id = id;
        this.lossPerBlock = lossPerBlock;
        this.capacityPerTick = source.generationEu();
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
