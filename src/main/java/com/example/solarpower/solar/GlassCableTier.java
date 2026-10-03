package com.example.solarpower.solar;

import java.math.BigInteger;

/**
 * 玻璃电缆等级（2026-10-03 补全：与太阳能面板一一对应，共 57 档，含无面板的永夜）。
 * <p>输电上限 = 对应面板「最大输出」的 4 倍——一根电缆保证满载带动 4 根同级面板；
 * 末档永夜电缆没有同名面板，容量取最强发电机（天终）蓄电上限的 64 倍
 * （满仓放电也带得走）。容量用 {@link BigInteger} 承载，不设上限。
 * <p>线损（EU/格）为手工调校的装饰性数值。
 */
public enum GlassCableTier {

    BASIC("glass_cable", 0.06D, SolarTier.BASIC, 0x9FB0C0),
    ADVANCED("advanced_glass_cable", 0.07D, SolarTier.ADVANCED, 0xE0B45C),
    HYBRID("glass_cable_2", 0.07D, SolarTier.HYBRID, 0x4FD1C5),
    ULTIMATE_HYBRID("ultimate_hybrid_glass_cable", 0.08D, SolarTier.PERFECT, 0x7ED957),
    QUANTUM("glass_cable_4", 0.09D, SolarTier.QUANTUM, 0x4EA8FF),
    SPECTRAL("glass_cable_5", 0.10D, SolarTier.SPECTRAL, 0xB07CFF),
    PROTON("glass_cable_6", 0.11D, SolarTier.PROTON, 0xFF7BAE),
    SINGULAR("glass_cable_7", 0.12D, SolarTier.SINGULAR, 0xC9D4E0),
    DIFFRACTION("glass_cable_8", 0.15D, SolarTier.DIFFRACTION, 0xFF5C5C),
    PHOTONIC("glass_cable_9", 0.18D, SolarTier.PHOTONIC, 0xFFE066),
    NEUTRON("glass_cable_10", 0.20D, SolarTier.NEUTRON, 0x8FA6C9),
    BARYON("baryon_glass_cable", 0.21D, SolarTier.BARYON, 0xC98A5A),
    HADRON("hadron_glass_cable", 0.22D, SolarTier.HADRON, 0x59D2FF),
    GRAVITON("graviton_glass_cable", 0.23D, SolarTier.GRAVITON, 0x7F6BFF),
    QUARK("glass_cable_11", 0.25D, SolarTier.QUARK, 0xF0F4FF),
    FUSION("glass_cable_12", 0.30D, SolarTier.FUSION, 0xFF9440),
    ANTIMATTER("antimatter_glass_cable", 0.30D, SolarTier.ANTIMATTER, 0xF03CE0),
    ZERO_POINT("zero_point_glass_cable", 0.31D, SolarTier.ZERO_POINT, 0xC8FF3E),
    DARK_ENERGY("dark_energy_glass_cable", 0.31D, SolarTier.DARK_ENERGY, 0x8C9EFF),
    COSMIC_STRING("cosmic_string_glass_cable", 0.32D, SolarTier.COSMIC_STRING, 0x4DE8B0),
    HYPERDIMENSIONAL("hyperdimensional_glass_cable", 0.32D, SolarTier.HYPERDIMENSIONAL, 0xFF8C69),
    OMNIVERSAL("glass_cable_13", 0.32D, SolarTier.OMNIVERSAL, 0x304FFE),
    VOID("void_glass_cable", 0.32D, SolarTier.VOID, 0x6A4CFF),
    SINGULARITY("singularity_glass_cable", 0.32D, SolarTier.SINGULARITY, 0xFF3B6B),
    STAR_CORE("star_core_glass_cable", 0.33D, SolarTier.STAR_CORE, 0x00E5A0),
    GENESIS("genesis_glass_cable", 0.33D, SolarTier.GENESIS, 0xFFD24A),
    VACUUM_DECAY("glass_cable_14", 0.34D, SolarTier.VACUUM_DECAY, 0x9B5CFF),
    SUPERSTRING("superstring_glass_cable", 0.33D, SolarTier.SUPERSTRING, 0x39D0FF),
    BRANE("brane_glass_cable", 0.33D, SolarTier.BRANE, 0xFF7A1A),
    HEAT_DEATH("heat_death_glass_cable", 0.34D, SolarTier.HEAT_DEATH, 0xE8E8FF),
    ZEROING("zeroing_glass_cable", 0.34D, SolarTier.ZEROING, 0x00C2A8),
    PARADOX("glass_cable_15", 0.36D, SolarTier.PARADOX, 0xFF2E88),
    ETERNITY("eternity_glass_cable", 0.36D, SolarTier.ETERNITY, 0x7C4DFF),
    TRANSCENDENT("transcendent_glass_cable", 0.36D, SolarTier.TRANSCENDENT, 0xFFB300),
    MYRIAD("myriad_glass_cable", 0.37D, SolarTier.MYRIAD, 0x00B0FF),
    DEMIURGE("demiurge_glass_cable", 0.37D, SolarTier.DEMIURGE, 0xF44336),
    APOTHEOSIS("glass_cable_16", 0.38D, SolarTier.APOTHEOSIS, 0xFFEB3B),
    BOUNDLESS("boundless_glass_cable", 0.38D, SolarTier.BOUNDLESS, 0x00E676),
    ABSOLUTE_INFINITY("absolute_infinity_glass_cable", 0.39D, SolarTier.ABSOLUTE_INFINITY, 0xD500F9),
    ABYSS("abyss_glass_cable", 0.39D, SolarTier.ABYSS, 0xFFFFFF),
    ANNIHILATION("annihilation_glass_cable", 0.39D, SolarTier.ANNIHILATION, 0xFF1744),
    CHAOS("glass_cable_17", 0.40D, SolarTier.CHAOS, 0x9E00FF),
    ORDER("order_glass_cable", 0.41D, SolarTier.ORDER, 0x00E5FF),
    SANDS_OF_TIME("sands_of_time_glass_cable", 0.41D, SolarTier.SANDS_OF_TIME, 0xFFC400),
    VOID_RIFT("glass_cable_18", 0.42D, SolarTier.VOID_RIFT, 0x6200EA),
    ARBITER("arbiter_glass_cable", 0.42D, SolarTier.ARBITER, 0x00FF7F),
    PRIMORDIAL("primordial_glass_cable", 0.43D, SolarTier.PRIMORDIAL, 0xFF6D00),
    FINAL_LAW("final_law_glass_cable", 0.43D, SolarTier.FINAL_LAW, 0xAEEA00),
    CONVERGENCE("convergence_glass_cable", 0.45D, SolarTier.CONVERGENCE, 0x00B8D4),
    SUPREME_ONE("supreme_one_glass_cable", 0.45D, SolarTier.SUPREME_ONE, 0xE040FB),
    GODHEAD("godhead_glass_cable", 0.46D, SolarTier.GODHEAD, 0xFFF176),
    NEGENTROPY("glass_cable_19", 0.44D, SolarTier.NEGENTROPY, 0x18FFFF),
    ENDGAME("endgame_glass_cable", 0.45D, SolarTier.ENDGAME, 0xFF4081),
    THE_ABSOLUTE("the_absolute_glass_cable", 0.46D, SolarTier.THE_ABSOLUTE, 0xB388FF),
    SUPREME_VOID("supreme_void_glass_cable", 0.46D, SolarTier.SUPREME_VOID, 0x64FFDA),
    AWAKENING("glass_cable_20", 0.46D, SolarTier.AWAKENING, 0x82B1FF),
    CREATOR("glass_cable_21", 0.48D, SolarTier.CREATOR, 0xFFAB40),
    STAR_SEA("glass_cable_22", 0.50D, SolarTier.STAR_SEA, 0x69F0AE),
    SUPREME_PRINCIPLE("glass_cable_23", 0.52D, SolarTier.SUPREME_PRINCIPLE, 0xFF6E40),
    FINALE("glass_cable_24", 0.54D, SolarTier.FINALE, 0xB20000),
    FINALITY("glass_cable_25", 0.56D, SolarTier.FINALITY, 0xB2FF59),
    DIVINE_LIGHT("glass_cable_26", 0.58D, SolarTier.DIVINE_LIGHT, 0x966919),
    FIRMAMENT("glass_cable_27", 0.60D, SolarTier.FIRMAMENT, 0x193782),
    PANTHEON("glass_cable_28", 0.62D, SolarTier.PANTHEON, 0x4B1978),
    MANDATE_OF_HEAVEN("glass_cable_29", 0.64D, SolarTier.MANDATE_OF_HEAVEN, 0xFFF0C8),
    APOCALYPSE("glass_cable_30", 0.66D, SolarTier.APOCALYPSE, 0x460F0A),
    HEAVENS_END("glass_cable_31", 0.68D, SolarTier.HEAVENS_END, 0x732DA5),
    ETERNAL_NIGHT("glass_cable_32", 0.70D, SolarTier.HEAVENS_END, 64L, 0x2B2B2B);

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
