package com.example.solarpower.item;

import com.example.solarpower.block.GlassCableBlock;
import com.example.solarpower.energy.EuFormat;
import com.example.solarpower.solar.GlassCableTier;

import net.minecraft.block.Block;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import com.example.solarpower.util.ChatCodes;

import java.util.List;

import javax.annotation.Nullable;

/** 玻璃电缆物品：tooltip 显示线损与每 tick 传输上限。 */
public class CableItemBlock extends ItemBlock {

    public CableItemBlock(Block block) {
        super(block);
    }

    /** 物品名按档位主体色显示（原版 16 色板里最接近的一档）。 */
    @SideOnly(Side.CLIENT)
    @Override
    public String getItemStackDisplayName(ItemStack stack) {
        GlassCableTier tier = ((GlassCableBlock) this.getBlock()).tier();
        return ChatCodes.nearest(tier.midColor()) + I18n.format(tier.translationKey() + ".name");
    }

    /** 参数含客户端类（ITooltipFlag），仅客户端覆写。 */
    @SideOnly(Side.CLIENT)
    @Override
    public void addInformation(ItemStack stack, @Nullable World world, List<String> tooltip,
                               ITooltipFlag flag) {
        GlassCableTier tier = ((GlassCableBlock) this.getBlock()).tier();
        tooltip.add(I18n.format("tooltip.solarpower.loss", tier.lossPerBlock()));
        tooltip.add(I18n.format("tooltip.solarpower.throughput", EuFormat.annotated(tier.capacityPerTick())));
    }
}
