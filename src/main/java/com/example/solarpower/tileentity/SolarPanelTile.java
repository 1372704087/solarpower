package com.example.solarpower.tileentity;

import com.example.solarpower.energy.EuCableNet;
import com.example.solarpower.energy.IEuEnergy;
import com.example.solarpower.energy.EuTier;
import com.example.solarpower.solar.SolarTier;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ITickable;
import net.minecraft.util.math.BlockPos;

import javax.annotation.Nullable;

/**
 * 太阳能板方块实体（1.12.2，全档位共用）。
 * 判定与 NeoForge 版一致：有天空光的维度 + 上方无遮挡才发电；白天按档位，
 * 夜晚（月光）为白天的一半（基础档不发电）；雨天/雷暴 ×0.65。
 */
public class SolarPanelTile extends TileEntity implements ITickable {

    private static final double RAIN_COEFFICIENT = 0.65D;
    private static final String TAG_ENERGY = "Energy";

    private final TileEnergy storage = new TileEnergy();
    private long generating;
    private SolarTier resolvedTier;

    /** 档位由所在方块的实例决定（1.12 的 TileEntity 构造器不带状态，懒解析）。 */
    public SolarTier getTier() {
        if (this.resolvedTier == null && this.hasWorld()) {
            if (this.world.getBlockState(this.pos).getBlock() instanceof com.example.solarpower.block.SolarPanelBlock) {
                this.resolvedTier = ((com.example.solarpower.block.SolarPanelBlock) this.world
                        .getBlockState(this.pos).getBlock()).tier();
            }
        }
        return this.resolvedTier == null ? SolarTier.BASIC : this.resolvedTier;
    }

    public IEuEnergy getEnergy() {
        return this.storage;
    }

    public long getGenerating() {
        return this.generating;
    }

    public boolean isNight() {
        return this.hasWorld() && !this.world.isDaytime();
    }

    @Override
    public void update() {
        if (this.world.isRemote) {
            return;
        }
        SolarTier tier = this.getTier();
        long generated = this.computeGeneration(tier);
        this.generating = generated;
        if (generated > 0L) {
            this.storage.receiveEu(generated, tier.voltage(), false);
        }
        if (this.storage.getStoredEu() > 0L) {
            this.pushToCables();
        }
    }

    private long computeGeneration(SolarTier tier) {
        if (!this.world.provider.hasSkyLight()) {
            return 0L; // 下界/末地这类没有天空光照的维度
        }
        if (!this.world.canSeeSky(this.pos.up())) {
            return 0L; // 上方被方块遮挡
        }
        boolean raining = this.world.isRaining() || this.world.isThundering();
        long base = this.world.isDaytime() ? tier.generationEu() : tier.nightGenerationEu();
        return raining ? (long) (base * RAIN_COEFFICIENT) : base;
    }

    private void pushToCables() {
        for (EnumFacing dir : EnumFacing.values()) {
            BlockPos side = this.pos.offset(dir);
            if (this.world.getTileEntity(side) instanceof GlassCableTile) {
                EuCableNet.push(this.world, side, this.storage, this.getTier().voltage());
            }
        }
    }

    @Override
    public void readFromNBT(NBTTagCompound compound) {
        super.readFromNBT(compound);
        this.storage.setStored(compound.getLong(TAG_ENERGY));
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound compound) {
        super.writeToNBT(compound);
        compound.setLong(TAG_ENERGY, this.storage.getStoredEu());
        return compound;
    }

    /** 储能容器：容量/电压跟随懒解析的档位。 */
    private final class TileEnergy implements IEuEnergy {

        private long stored;

        @Override
        public long getStoredEu() {
            return this.stored;
        }

        @Override
        public long getCapacityEu() {
            return SolarPanelTile.this.getTier().capacityEu();
        }

        @Override
        public EuTier getTier() {
            return SolarPanelTile.this.getTier().voltage();
        }

        @Override
        public long receiveEu(long amount, EuTier tier, boolean simulate) {
            if (amount <= 0L || !SolarPanelTile.this.getTier().voltage().accepts(tier)) {
                return 0L;
            }
            long accepted = Math.min(amount, getCapacityEu() - this.stored);
            if (accepted <= 0L) {
                return 0L;
            }
            if (!simulate) {
                this.stored += accepted;
                markDirty();
            }
            return accepted;
        }

        @Override
        public long extractEu(long amount, boolean simulate) {
            long extracted = Math.min(Math.max(0L, amount), this.stored);
            if (extracted > 0L && !simulate) {
                this.stored -= extracted;
                markDirty();
            }
            return extracted;
        }

        void setStored(long value) {
            this.stored = Math.max(0L, Math.min(value, this.getCapacityEu()));
            markDirty();
        }
    }
}
