package com.example.solarpower.tileentity;

import com.example.solarpower.solar.GlassCableTier;

import net.minecraft.tileentity.TileEntity;

import javax.annotation.Nullable;

/** 玻璃电缆方块实体（1.12.2，全等级共用）。等级由方块决定，电缆不储能。 */
public class GlassCableTile extends TileEntity {

    @Nullable
    private GlassCableTier resolvedTier;

    /** 等级由所在方块的实例决定（懒解析）。 */
    public GlassCableTier tier() {
        if (this.resolvedTier == null && this.hasWorld()) {
            if (this.world.getBlockState(this.pos).getBlock() instanceof com.example.solarpower.block.GlassCableBlock) {
                this.resolvedTier = ((com.example.solarpower.block.GlassCableBlock) this.world
                        .getBlockState(this.pos).getBlock()).tier();
            }
        }
        return this.resolvedTier == null ? GlassCableTier.BASIC : this.resolvedTier;
    }
}
