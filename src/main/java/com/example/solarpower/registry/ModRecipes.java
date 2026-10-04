package com.example.solarpower.registry;

import com.example.solarpower.SolarPower;
import com.example.solarpower.energy.EuTier;
import com.example.solarpower.solar.GlassCableTier;
import com.example.solarpower.solar.MtRecipes;
import com.example.solarpower.solar.SolarTier;

import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;

import net.minecraftforge.fml.common.registry.ForgeRegistries;
import net.minecraftforge.oredict.OreDictionary;
import net.minecraftforge.oredict.ShapedOreRecipe;
import net.minecraftforge.oredict.ShapelessOreRecipe;

/**
 * 工作台配方：分子重组仪本体 + 太阳能合成链。
 * <p>链路与图案照搬 Advanced Solar Panels 的经典硬编码默认配方；原配方里的 IC2 原料
 * 按下表映射成原版物品（与官方 1.20.1 移植版「原版可映射子集」的思路一致）：
 * <ul>
 *     <li>ic2:iron_plate → 铁锭；ic2:alloy（高级合金）→ 铁锭；ic2:carbon_plate → 煤炭块；</li>
 *     <li>ic2:reinforced_glass → 玻璃；ic2:thick_neutron_reflector → 黑曜石；</li>
 *     <li>ic2:advanced_machine → 铁块；ic2:ev_transformer → 红石块；ic2:advanced_circuit → 金锭；</li>
 *     <li>ic2:iridium（矿辞铱）→ 本模组的铱锭；铀锭走矿辞 {@code ingotUranium}（装 IC2 时兼容其铀）。</li>
 * </ul>
 */
public final class ModRecipes {

    /** 在 init 阶段调用（物品已注册完毕）。 */
    public static void register() {
        MtRecipes.register();
        registerOredict();
        registerCrafting();
        registerSspComponents();
        registerPanelChain();
        registerCableRecipes();
        registerTransformerRecipes();
    }

    /**
     * 变压器合成（65 档，桥接电压 T↔T+1）：上下铁锭 + 中列「电缆(T)/铁块/电缆(T)」。
     * 电缆取绑定面板 iuTier == T 的首条玻璃电缆（低压/中压/高压……逐档可用）。
     */
    private static void registerTransformerRecipes() {
        for (EuTier tier : EuTier.values()) {
            if (tier.iuTier() > 65) {
                continue;
            }
            GlassCableTier cable = cableForIuTier(tier.iuTier());
            add(new ItemStack(ModBlocks.transformer(tier)), " I ", "CBC", " I ",
                    'I', Items.IRON_INGOT, 'B', Blocks.IRON_BLOCK,
                    'C', ModItems.cableItem(cable));
        }
    }

    /** 绑定面板 iuTier == 给定电压档的首条玻璃电缆。 */
    private static GlassCableTier cableForIuTier(int iuTier) {
        for (GlassCableTier tier : GlassCableTier.values()) {
            if (tier.partTier().iuTier() == iuTier) {
                return tier;
            }
        }
        throw new IllegalStateException("no glass cable for iuTier " + iuTier);
    }

    /**
     * 电缆合成（IC2 玻纤电缆风格，用户指定图案，产出 ×6）。
     * <p>钻石粉/银粉均走 IC2 矿辞（dustDiamond / dustSilver，需装 IC2，用户指定）。
     */
    private static void registerCableRecipes() {
        add(new ItemStack(ModItems.cableItem(GlassCableTier.BASIC), 6), "GGG", "DSD", "GGG",
                'G', Blocks.GLASS, 'D', "dustDiamond", 'S', "dustSilver");
        // 高级电缆（用户指定）：中列玻璃/高级小块/玻璃，两侧基础电缆
        add(new ItemStack(ModItems.cableItem(GlassCableTier.ADVANCED)), " G ", "BPB", " G ",
                'G', Blocks.GLASS, 'B', ModItems.cableItem(GlassCableTier.BASIC),
                'P', ModItems.sunnariumPartItem(SolarTier.ADVANCED));
        // 混合档起批量生成同款十字升级链（用户指定规律）：
        // 中列玻璃/本档小块/玻璃，两侧各 1 条上一档电缆，产出 ×1（到天终档止，永夜未定）
        GlassCableTier[] cables = GlassCableTier.values();
        for (int i = 2; i < cables.length; i++) {
            if (cables[i] == GlassCableTier.ETERNAL_NIGHT) {
                continue;
            }
            add(new ItemStack(ModItems.cableItem(cables[i])), " G ", "BPB", " G ",
                    'G', Blocks.GLASS, 'B', ModItems.cableItem(cables[i - 1]),
                    'P', ModItems.sunnariumPartItem(cables[i].partTier()));
        }
    }

    /** 把本模组铀锭登记进矿辞，辐光铀配方即可同时吃 IC2 / 本模组的铀。 */
    private static void registerOredict() {
        OreDictionary.registerOre("ingotUranium", ModItems.crafting("uranium_ingot"));
    }

    private static void registerCrafting() {
        ItemStack part = stack("sunnarium_part");
        ItemStack sunnarium = stack("sunnarium");
        ItemStack enrichedSunnarium = stack("enriched_sunnarium");
        ItemStack iridium = stack("iridium_ingot");
        ItemStack iridiumPlate = stack("iridium_iron_plate");
        ItemStack reinforcedPlate = stack("reinforced_iridium_iron_plate");
        ItemStack irradiantUranium = stack("irradiant_uranium");
        ItemStack pane = stack("irradiant_glass_pane");
        ItemStack mtCore = stack("mt_core");

        // 阳炎：9 阳炎碎片；富集阳炎：8 辐光铀抱 1 阳炎
        add(sunnarium, "SSS", "SSS", "SSS", 'S', part);
        add(enrichedSunnarium, "UUU", "USU", "UUU", 'U', irradiantUranium, 'S', sunnarium);
        // 阳炎合金：8 铱抱 1 阳炎；富集合金：4 富集阳炎抱 1 阳炎合金
        add(stack("sunnarium_alloy"), "III", "ISI", "III", 'I', iridium, 'S', sunnarium);
        add(stack("enriched_sunnarium_alloy"), " S ", "SAS", " S ",
                'S', enrichedSunnarium, 'A', stack("sunnarium_alloy"));
        // 铱铁板：8 铁抱 1 铱；强化铱铁板：4 铁角 + 4 煤炭块边 + 铱铁板心
        add(iridiumPlate, "III", "IPI", "III", 'I', Items.IRON_INGOT, 'P', iridium);
        add(reinforcedPlate, "ACA", "CIC", "ACA",
                'A', Items.IRON_INGOT, 'C', Blocks.COAL_BLOCK, 'I', iridiumPlate);
        // 辐光铀：4 萤石粉抱 1 铀（矿辞）；辐光玻璃板：玻璃排 + 辐光铀 + 萤石粉
        add(irradiantUranium, " G ", "GUG", " G ",
                'G', Items.GLOWSTONE_DUST, 'U', "ingotUranium");
        add(pane, "GGG", "UDU", "GGG",
                'G', Blocks.GLASS, 'U', irradiantUranium, 'D', Items.GLOWSTONE_DUST);
        // 辐光强化板：红石/阳炎碎片/黄色染料/钻石 + 强化铱铁板
        add(stack("irradiant_reinforced_plate"), "RSR", "LIL", "RDR",
                'R', Items.REDSTONE, 'S', part, 'L', "dyeYellow",
                'I', reinforcedPlate, 'D', Items.DIAMOND);
        // 分子重组核心：4 辐光玻璃板 + 2 黑曜石；量子核心：4 富集合金 + 4 下界之星 + 末影之眼
        add(mtCore, "PRP", "P P", "PRP", 'P', pane, 'R', Blocks.OBSIDIAN);
        add(stack("quantum_core"), "ANA", "NEN", "ANA",
                'A', stack("enriched_sunnarium_alloy"), 'N', Items.NETHER_STAR,
                'E', Items.ENDER_EYE);
        // 分子重组仪：4 铁块角 + 2 红石块 + 2 金锭 + 分子重组核心
        add(new ItemStack(ModBlocks.molecularTransformer()), "MTM", "CcC", "MTM",
                'M', Blocks.IRON_BLOCK, 'T', Blocks.REDSTONE_BLOCK,
                'C', Items.GOLD_INGOT, 'c', mtCore);
    }

    /**
     * SSP 光谱组件族，配方沿用 Super Solar Panels 的默认配方（原料按本模组惯例原版化：
     * ic2 强化玻璃 → 玻璃）。太阳光分解器在 SSP 里正是光谱档面板的核心材料。
     */
    private static void registerSspComponents() {
        // 绿色组件：1 光辉玻璃板（SSP 原配方为无序合成）
        addShapeless(stack("greencomponent"), stack("irradiant_glass_pane"));
        // 蓝色组件：8 玻璃抱青染料；红色组件：8 玻璃抱红石
        add(stack("bluecomponent"), "AAA", "ABA", "AAA",
                'A', Blocks.GLASS, 'B', "dyeBlue");
        add(stack("redcomponent"), "AAA", "ABA", "AAA",
                'A', Blocks.GLASS, 'B', Items.REDSTONE);
        // 太阳光分解器：红/绿/蓝三列
        add(stack("solarsplitter"), "ABC", "ABC", "ABC",
                'A', stack("redcomponent"), 'B', stack("greencomponent"),
                'C', stack("bluecomponent"));
    }

    /**
     * 「XX玻璃板」「XX阳光化合物」「XX小块阳光化合物」「XX阳光合金」「XX核心」材料链
     * 与面板合成（IU 风格：每级面板吃本级玻璃板与本级核心，全部工作台配方；
     * 升级关系与本体的小块→化合物→合金一一对应）。
     * <ul>
     *     <li>基础玻璃板：玻璃排抱萤石粉——辉光玻璃板的无铀弱化版；</li>
     *     <li>小块阶梯：N 档小块 = 1 富集阳光化合物 + 1 上一档小块（基础档小块本体来自分子重组仪，
     *     富集阳光化合物本体不动，仍是整条阶梯的通用增幅材料）；</li>
     *     <li>化合物阶梯：N 档化合物 = 9 张 N 档小块（与本体"9 小块 → 1 化合物"同图案）；</li>
     *     <li>合金阶梯：N 档合金 = 8 铱锭抱 1 N 档化合物（与本体的"8 铱抱化合物"同构）；</li>
     *     <li>核心阶梯：高级核心 = 1 富集阳光化合物 + 1 本档合金（入门竖排）；其余 N 档核心 =
     *     上下 2 张本档合金 + 左右 2 张本档化合物 + 中心 1 枚上一级核心（自增长，
     *     量子核心除外）；</li>
     *     <li>其余每级玻璃板：1 上一档阳光合金 + 1 上一级玻璃板（线性成本，仿 IU 光子玻璃阶梯）；</li>
     *     <li>光谱玻璃板：额外要求 1 太阳光分解器（SSP 里光谱档正是由它核心合成）；</li>
     *     <li>基础面板：铁锭四角 + 玻璃边 + 基础玻璃板芯；</li>
     *     <li>其余每级面板：4 核心四角 + 4 本级玻璃板边 + 上一级面板芯
     *     （同 IU 的"恒常玻璃 + 激发核 + 上一级面板"）。</li>
     * </ul>
     */
    private static void registerPanelChain() {
        SolarTier[] tiers = SolarTier.values();

        add(new ItemStack(ModItems.paneItem(tiers[0])), "GGG", "GDG", "GGG",
                'G', Blocks.GLASS, 'D', Items.GLOWSTONE_DUST);
        for (int i = 1; i < tiers.length; i++) {
            add(new ItemStack(ModItems.sunnariumPartItem(tiers[i])), "F", "P",
                    'F', stack("enriched_sunnarium"), 'P', ModItems.sunnariumPartItem(tiers[i - 1]));
            add(new ItemStack(ModItems.sunnariumItem(tiers[i])), "SSS", "SSS", "SSS",
                    'S', ModItems.sunnariumPartItem(tiers[i]));
            add(new ItemStack(ModItems.sunnariumAlloyItem(tiers[i])), "III", "ICI", "III",
                    'I', stack("iridium_ingot"), 'C', ModItems.sunnariumItem(tiers[i]));
            // 量子核心沿用 ASP 官方配方（富集合金 + 下界之星 + 末影之眼），不走通用核心配方；
            // 高级核心 = 上下本档合金 + 左右本档化合物 + 中心钻石（无上一级核心，钻石作引信）；
            // 其余每档 = 同图案，中心为上一级核心（用户指定 3×3 图案）
            if (tiers[i] == SolarTier.QUANTUM) {
                continue;
            }
            if (i == 1) {
                add(new ItemStack(ModItems.coreItem(tiers[i])), " A ", "CDC", " A ",
                        'A', ModItems.sunnariumAlloyItem(tiers[i]),
                        'C', ModItems.sunnariumItem(tiers[i]), 'D', Items.DIAMOND);
            } else {
                add(new ItemStack(ModItems.coreItem(tiers[i])), " A ", "CKC", " A ",
                        'A', ModItems.sunnariumAlloyItem(tiers[i]),
                        'C', ModItems.sunnariumItem(tiers[i]),
                        'K', ModItems.coreItem(tiers[i - 1]));
            }
        }
        for (int i = 1; i < tiers.length; i++) {
            SolarTier prev = tiers[i - 1];
            if (tiers[i] == SolarTier.SPECTRAL) {
                add(new ItemStack(ModItems.paneItem(tiers[i])), "U", "S", "P",
                        'U', ModItems.sunnariumAlloyItem(prev), 'S', stack("solarsplitter"),
                        'P', ModItems.paneItem(prev));
            } else {
                add(new ItemStack(ModItems.paneItem(tiers[i])), "U", "P",
                        'U', ModItems.sunnariumAlloyItem(prev), 'P', ModItems.paneItem(prev));
            }
            add(new ItemStack(ModBlocks.panel(tiers[i])), "KPK", "KCK", "KPK",
                    'K', ModItems.coreItem(tiers[i]), 'P', ModItems.paneItem(tiers[i]),
                    'C', ModItems.panelItem(prev));
        }

        add(new ItemStack(ModBlocks.panel(tiers[0])), "IGI", "GPG", "IGI",
                'I', Items.IRON_INGOT, 'G', Blocks.GLASS, 'P', ModItems.paneItem(tiers[0]));
    }

    /** 按 ASP 合成链注册名取物品；玻璃板材料不在该表里，需直接用 {@link ModItems#paneItem}。 */
    private static ItemStack stack(String id) {
        net.minecraft.item.Item item = ModItems.crafting(id);
        if (item == null) {
            throw new IllegalStateException("Unknown crafting item: " + id);
        }
        return new ItemStack(item);
    }

    private static void add(ItemStack result, Object... recipe) {
        String name = result.getItem().getRegistryName().getResourcePath();
        ResourceLocation key = new ResourceLocation(SolarPower.MODID, name);
        ShapedOreRecipe shaped = new ShapedOreRecipe(key, result, recipe);
        shaped.setRegistryName(key);
        ForgeRegistries.RECIPES.register(shaped);
    }

    private static void addShapeless(ItemStack result, Object... recipe) {
        String name = result.getItem().getRegistryName().getResourcePath();
        ResourceLocation key = new ResourceLocation(SolarPower.MODID, name);
        ShapelessOreRecipe shapeless = new ShapelessOreRecipe(key, result, recipe);
        shapeless.setRegistryName(key);
        ForgeRegistries.RECIPES.register(shapeless);
    }

    private ModRecipes() {
    }
}
