package com.example.solarpower.registry;

import com.example.solarpower.SolarPower;
import com.example.solarpower.solar.MtRecipes;

import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;

import net.minecraftforge.fml.common.registry.ForgeRegistries;
import net.minecraftforge.oredict.OreDictionary;
import net.minecraftforge.oredict.ShapedOreRecipe;

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

    private static ItemStack stack(String id) {
        return new ItemStack(ModItems.crafting(id));
    }

    private static void add(ItemStack result, Object... recipe) {
        String name = result.getItem().getRegistryName().getResourcePath();
        ResourceLocation key = new ResourceLocation(SolarPower.MODID, name);
        ShapedOreRecipe shaped = new ShapedOreRecipe(key, result, recipe);
        shaped.setRegistryName(key);
        ForgeRegistries.RECIPES.register(shaped);
    }

    private ModRecipes() {
    }
}
