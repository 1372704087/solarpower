package com.example.solarpower.solar;

import com.example.solarpower.energy.EuTier;

/**
 * IU（Industrial Upgrade）里全部常规太阳能板的档位。
 * <p>数值取自 IU 1.21.1-3.4.0.11 的 {@code EnumSolarPanels}（Config 默认值）：
 * 白天发电量（EU/t）、内部缓存（EU）与电压等级 tier。
 * <p>夜间（月光）发电量按 IU 规则为白天的一半，不足 1 EU/t 的档位（基础）夜晚不发电；
 * 不包含 IU 的月亮相位/太阳系数/日夜玻璃与模块加成系统。
 * <p>IU 里名为 admin（衍射）的档位其实是正常升级链上的一环（tier 8），因此保留。
 * <p>SI 系列（太拉～昆它，tier 15~21）为超出 IU 14 级的扩展档，沿用链上既有的
 * 规律外推：白天发电量每级 ×4（中子级起规律），容量每级 ×10（引力子级起规律），
 * 昆它容量封顶到 {@link Long#MAX_VALUE}（与电压等级 MAX 封顶呼应）。
 */
public enum SolarTier {

    BASIC("solar_panel", 1, 1L, 128L, 0x9FB0C0),
    ADVANCED("advanced_solar_panel", 1, 5L, 3200L, 0xE0B45C),
    HYBRID("hybrid_solar_panel", 2, 15L, 20000L, 0x4FD1C5),
    PERFECT("perfect_solar_panel", 3, 45L, 200000L, 0x7ED957),
    QUANTUM("quantum_solar_panel", 4, 135L, 1000000L, 0x4EA8FF),
    SPECTRAL("spectral_solar_panel", 5, 405L, 5000000L, 0xB07CFF),
    PROTON("proton_solar_panel", 6, 1215L, 50000000L, 0xFF7BAE),
    SINGULAR("singular_solar_panel", 7, 3645L, 1000000000L, 0xC9D4E0),
    DIFFRACTION("diffraction_solar_panel", 8, 10935L, 1500000000L, 0xFF5C5C),
    PHOTONIC("photonic_solar_panel", 9, 32805L, 5000000000L, 0xFFE066),
    NEUTRON("neutron_solar_panel", 10, 131220L, 6500000000L, 0x8FA6C9),
    BARYON("baryon_solar_panel", 11, 524880L, 10000000000L, 0xC98A5A),
    HADRON("hadron_solar_panel", 12, 2099520L, 25000000000L, 0x59D2FF),
    GRAVITON("graviton_solar_panel", 13, 8398080L, 250000000000L, 0x7F6BFF),
    QUARK("quark_solar_panel", 14, 33592320L, 2500000000000L, 0xF0F4FF),
    TERA("tera_solar_panel", 15, 134369280L, 25000000000000L, 0xFF9440),
    PETA("peta_solar_panel", 16, 537477120L, 250000000000000L, 0xF03CE0),
    EXA("exa_solar_panel", 17, 2149908480L, 2500000000000000L, 0xC8FF3E),
    ZETTA("zetta_solar_panel", 18, 8599633920L, 25000000000000000L, 0x8C9EFF),
    YOTTA("yotta_solar_panel", 19, 34398535680L, 250000000000000000L, 0x4DE8B0),
    RONNA("ronna_solar_panel", 20, 137594142720L, 2500000000000000000L, 0xFF8C69),
    QUETTA("quetta_solar_panel", 21, 550376570880L, Long.MAX_VALUE, 0x304FFE);

    private final String id;
    private final int iuTier;
    private final long generationEu;
    private final long capacityEu;
    private final int accentColor;

    SolarTier(String id, int iuTier, long generationEu, long capacityEu, int accentColor) {
        this.id = id;
        this.iuTier = iuTier;
        this.generationEu = generationEu;
        this.capacityEu = capacityEu;
        this.accentColor = accentColor;
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
    public long generationEu() {
        return this.generationEu;
    }

    /** 夜间（月光）发电量（EU/t）：IU 规则为白天的一半，向下取整且不足 1 归零。 */
    public long nightGenerationEu() {
        long night = this.generationEu / 2L;
        return night < 1L ? 0L : night;
    }

    /** 内部缓存上限（EU）。 */
    public long capacityEu() {
        return this.capacityEu;
    }

    /** 界面里该档位的强调色（0xRRGGBB）。 */
    public int accentColor() {
        return this.accentColor;
    }

    /** 电压等级，由 IU 的 tier 编号换算。 */
    public EuTier voltage() {
        return EuTier.byIuTier(this.iuTier);
    }

    /** 方块/物品的翻译键（1.12.2 使用 tile. 前缀）。 */
    public String translationKey() {
        return "tile.solarpower." + this.id;
    }
}