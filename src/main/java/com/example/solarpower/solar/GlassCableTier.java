package com.example.solarpower.solar;

/**
 * IU 玻璃电缆的 11 个等级，数值取自 IU 1.21.1-3.4.0.11 的
 * {@code CableType}：线损（EU/格）与每 tick 最大传输量（EU）。
 * <p>命名沿用 IU 对档位链物品的惯例，按太阳能板等级链前缀命名
 * （基础/混合/…/中子/无限）。
 */
public enum GlassCableTier {

    BASIC("glass_cable", 0.06D, 4_096L),
    HYBRID("glass_cable_2", 0.07D, 8_192L),
    PERFECT("glass_cable_3", 0.08D, 32_768L),
    QUANTUM("glass_cable_4", 0.09D, 131_072L),
    SPECTRAL("glass_cable_5", 0.10D, 524_288L),
    PROTON("glass_cable_6", 0.11D, 2_097_152L),
    SINGULAR("glass_cable_7", 0.12D, 8_388_608L),
    DIFFRACTION("glass_cable_8", 0.15D, 33_554_432L),
    PHOTONIC("glass_cable_9", 0.18D, 134_217_728L),
    NEUTRON("glass_cable_10", 0.20D, 536_870_912L),
    INFINITY("glass_cable_11", 0.25D, 8_589_934_590L);

    private final String id;
    private final double lossPerBlock;
    private final long capacityPerTick;

    GlassCableTier(String id, double lossPerBlock, long capacityPerTick) {
        this.id = id;
        this.lossPerBlock = lossPerBlock;
        this.capacityPerTick = capacityPerTick;
    }

    /** 注册名（同时也是贴图、模型与方块的文件名）。 */
    public String id() {
        return this.id;
    }

    /** 线损（EU/格）：每经过一根电缆，传输量扣除该数值。 */
    public double lossPerBlock() {
        return this.lossPerBlock;
    }

    /** 单根电缆每 tick 的最大传输量（EU）。 */
    public long capacityPerTick() {
        return this.capacityPerTick;
    }

    /** 方块/物品的翻译键（1.12.2 使用 tile. 前缀）。 */
    public String translationKey() {
        return "tile.solarpower." + this.id;
    }
}
