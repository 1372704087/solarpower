package com.example.solarpower.solar;

import com.example.solarpower.registry.ModItems;

import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.EnumDyeColor;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 分子重组机配方表。机制照搬 Advanced Solar Panels：单个输入按配方定义的总 EU 量
 * 逐步注入能量，注满后转换成产出（每份配方消耗 1 个输入）。
 * <p>数值沿用 ASP 1.12.2 的经典默认配方集（原版可映射子集，与官方 1.20.1 移植版一致）；
 * 「火药 → 铀锭」是原 IC2 压缩机路线（铀矿 → 铀锭）的原版化替身，
 * 保证不装 IC2 时富集链（辐光铀）也有来源。
 */
public final class MtRecipes {

    /** 一条转换配方：输入（按物品+meta 匹配，忽略 NBT 与数量）、产出、所需总 EU。 */
    public static final class MtRecipe {

        public final ItemStack input;
        public final ItemStack output;
        public final int totalEu;

        MtRecipe(ItemStack input, ItemStack output, int totalEu) {
            this.input = input;
            this.output = output;
            this.totalEu = totalEu;
        }

        public boolean matches(ItemStack stack) {
            return !stack.isEmpty()
                    && stack.getItem() == this.input.getItem()
                    && stack.getMetadata() == this.input.getMetadata();
        }
    }

    private static final List<MtRecipe> RECIPES = new ArrayList<>();

    /** 注册经典默认配方集（在物品注册之后调用）。 */
    public static void register() {
        if (!RECIPES.isEmpty()) {
            return;
        }
        // ASP 经典默认值
        add(new ItemStack(Items.SKULL, 1, 1), Items.NETHER_STAR, 250_000_000); // 凋灵骷髅头 → 下界之星
        add(new ItemStack(Items.COAL), Items.DIAMOND, 9_000_000);              // 煤炭 → 钻石
        add(new ItemStack(Items.GLOWSTONE_DUST), ModItems.crafting("sunnarium_part"), 1_000_000);
        add(new ItemStack(Blocks.GLOWSTONE), ModItems.crafting("sunnarium"), 9_000_000);
        add(new ItemStack(Items.GOLD_INGOT), ModItems.crafting("iridium_ingot"), 9_000_000);
        add(new ItemStack(Blocks.NETHERRACK), new ItemStack(Items.GUNPOWDER, 2), 70_000);
        add(new ItemStack(Blocks.SAND), new ItemStack(Blocks.GRAVEL), 50_000);
        add(new ItemStack(Blocks.DIRT), Items.CLAY_BALL, 50_000);
        add(new ItemStack(Items.COAL, 1, 1), Items.COAL, 60_000);              // 木炭 → 煤炭
        add(new ItemStack(Items.GUNPOWDER), ModItems.crafting("uranium_ingot"), 2_000_000); // 铀的原版化来源
        // 染色羊毛 → 对应矿物块（经典彩蛋配方）
        add(wool(EnumDyeColor.YELLOW), new ItemStack(Blocks.GLOWSTONE), 500_000);
        add(wool(EnumDyeColor.BLUE), new ItemStack(Blocks.LAPIS_BLOCK), 500_000);
        add(wool(EnumDyeColor.RED), new ItemStack(Blocks.REDSTONE_BLOCK), 500_000);
    }

    private static void add(ItemStack input, Item output, int totalEu) {
        add(input, new ItemStack(output), totalEu);
    }

    private static void add(ItemStack input, ItemStack output, int totalEu) {
        RECIPES.add(new MtRecipe(input, output, totalEu));
    }

    /** 指定颜色的羊毛。 */
    private static ItemStack wool(EnumDyeColor color) {
        return new ItemStack(Blocks.WOOL, 1, color.getMetadata());
    }

    /** 按输入物品查找配方，无匹配返回 {@code null}。 */
    public static MtRecipe find(ItemStack stack) {
        for (MtRecipe recipe : RECIPES) {
            if (recipe.matches(stack)) {
                return recipe;
            }
        }
        return null;
    }

    public static List<MtRecipe> all() {
        return Collections.unmodifiableList(RECIPES);
    }

    private MtRecipes() {
    }
}
