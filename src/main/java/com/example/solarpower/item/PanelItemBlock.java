package com.example.solarpower.item;

import com.example.solarpower.block.SolarPanelBlock;
import com.example.solarpower.energy.EuFormat;
import com.example.solarpower.solar.SolarTier;

import net.minecraft.block.Block;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.List;

import javax.annotation.Nullable;

/** 太阳能板物品：tooltip 显示能量等级、昼夜发电量与蓄电上限（IU 风格）。 */
public class PanelItemBlock extends ItemBlock {

    public PanelItemBlock(Block block) {
        super(block);
    }

    /** 物品名按档位主题色显示（原版 16 色板里最接近的一档）。 */
    @SideOnly(Side.CLIENT)
    @Override
    public String getItemStackDisplayName(ItemStack stack) {
        SolarTier tier = ((SolarPanelBlock) this.getBlock()).tier();
        return tier.accentCode() + I18n.format(tier.translationKey() + ".name");
    }

    /** 参数含客户端类（ITooltipFlag），仅客户端覆写。 */
    @SideOnly(Side.CLIENT)
    @Override
    public void addInformation(ItemStack stack, @Nullable World world, List<String> tooltip,
                               ITooltipFlag flag) {
        SolarTier tier = ((SolarPanelBlock) this.getBlock()).tier();
        tooltip.add(I18n.format("tooltip.solarpower.energy_tier", tier.iuTier(), tier.voltage().name()));
        tooltip.add(I18n.format("tooltip.solarpower.day_gen", EuFormat.annotated(tier.generationEu())));
        tooltip.add(I18n.format("tooltip.solarpower.night_gen", EuFormat.annotated(tier.nightGenerationEu())));
        tooltip.add(I18n.format("tooltip.solarpower.capacity", EuFormat.annotated(tier.capacityEu())));
    }
}
