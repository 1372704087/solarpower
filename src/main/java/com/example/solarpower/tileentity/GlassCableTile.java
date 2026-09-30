package com.example.solarpower.tileentity;

import com.example.solarpower.compat.Ic2Compat;
import com.example.solarpower.energy.EuCableNet;
import com.example.solarpower.energy.FeConvert;
import com.example.solarpower.solar.GlassCableTier;

import ic2.api.energy.EnergyNet;
import ic2.api.energy.event.EnergyTileLoadEvent;
import ic2.api.energy.event.EnergyTileUnloadEvent;
import ic2.api.energy.tile.IEnergyAcceptor;
import ic2.api.energy.tile.IEnergyConductor;
import ic2.api.energy.tile.IEnergyEmitter;

import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.energy.CapabilityEnergy;
import net.minecraftforge.energy.IEnergyStorage;
import net.minecraftforge.fml.common.Optional;

import javax.annotation.Nullable;

/**
 * 玻璃电缆方块实体（1.12.2，全等级共用）。等级由方块决定，电缆不储能。
 * <p>IC2 兼容是软依赖：装了 IC2 时本电缆同时是 IC2 的 {@code IEnergyConductor}，
 * IC2 的电网可以直接穿过它（线损取本档的 EU/格）；没装时该接口与方法会被 Forge 的
 * {@code @Optional} 剥离。本模组自身的输电仍走 {@code EuCableNet}，两套网各自独立结算。
 * <p>Forge Energy 桥接：本电缆对外暴露 {@link IEnergyStorage}，把 FE 请求转给所在
 * {@code EuCableNet} 网络——抽电＝从相连面板取，进电＝送给相连的汇。这样 Mekanism
 * 的万能线缆（以及其它 FE 机器）可以直接接在玻璃电缆上，无需任何额外依赖。
 */
@Optional.Interface(iface = "ic2.api.energy.tile.IEnergyConductor", modid = "ic2")
public class GlassCableTile extends TileEntity implements IEnergyConductor, IEnergyStorage {

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

    // ------------------------------------------------------------------
    // IC2 导体（软依赖）：装了 IC2 才注册进 IC2 电网，IC2 的电流可穿过本电缆。
    // ------------------------------------------------------------------

    /** 是否已注册进 IC2 电网。 */
    private boolean ic2Registered;

    @Override
    public void onLoad() {
        super.onLoad();
        if (Ic2Compat.LOADED) {
            loadIc2();
        }
    }

    @Override
    public void invalidate() {
        if (Ic2Compat.LOADED) {
            unloadIc2();
        }
        super.invalidate();
    }

    @Override
    public void onChunkUnload() {
        if (Ic2Compat.LOADED) {
            unloadIc2();
        }
        super.onChunkUnload();
    }

    @Optional.Method(modid = "ic2")
    private void loadIc2() {
        if (!this.ic2Registered && !this.world.isRemote && EnergyNet.instance != null) {
            MinecraftForge.EVENT_BUS.post(new EnergyTileLoadEvent(this));
            this.ic2Registered = true;
        }
    }

    @Optional.Method(modid = "ic2")
    private void unloadIc2() {
        if (this.ic2Registered) {
            MinecraftForge.EVENT_BUS.post(new EnergyTileUnloadEvent(this));
            this.ic2Registered = false;
        }
    }

    /** 与任何 IC2 能量方块互连（IC2 原版线缆也是连一切）。 */
    @Optional.Method(modid = "ic2")
    @Override
    public boolean acceptsEnergyFrom(IEnergyEmitter emitter, EnumFacing side) {
        return true;
    }

    @Optional.Method(modid = "ic2")
    @Override
    public boolean emitsEnergyTo(IEnergyAcceptor acceptor, EnumFacing side) {
        return true;
    }

    /** IC2 侧的线损：直接取本档的 EU/格。 */
    @Optional.Method(modid = "ic2")
    @Override
    public double getConductionLoss() {
        return this.tier().lossPerBlock();
    }

    // 玻璃电缆视为"不熔断"：IC2 只在超过下列阈值时才会震击/烧毁。
    // 取 +∞ / MAX_VALUE，等价于永不熔断；本档的每 tick 传输上限仍由 EuCableNet 负责。

    @Optional.Method(modid = "ic2")
    @Override
    public double getInsulationEnergyAbsorption() {
        return Double.POSITIVE_INFINITY;
    }

    @Optional.Method(modid = "ic2")
    @Override
    public double getInsulationBreakdownEnergy() {
        return Double.MAX_VALUE;
    }

    @Optional.Method(modid = "ic2")
    @Override
    public double getConductorBreakdownEnergy() {
        return Double.MAX_VALUE;
    }

    @Optional.Method(modid = "ic2")
    @Override
    public void removeInsulation() {
        // 永不触发（绝缘吸收为 +∞）
    }

    @Optional.Method(modid = "ic2")
    @Override
    public void removeConductor() {
        // 永不触发（熔断阈值为 MAX_VALUE）
    }

    // ------------------------------------------------------------------
    // Forge Energy 桥接（无依赖）：电缆本身不储能，FE 请求全部转给所在 EuCableNet 网络。
    // 抽电＝从相连面板取（已扣线损），进电＝送给相连的汇。Mekanism 万能线缆能直接接上来。
    // ------------------------------------------------------------------

    @Override
    public boolean hasCapability(Capability<?> capability, @Nullable EnumFacing facing) {
        return capability == CapabilityEnergy.ENERGY || super.hasCapability(capability, facing);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T getCapability(Capability<T> capability, @Nullable EnumFacing facing) {
        if (capability == CapabilityEnergy.ENERGY) {
            return (T) this;
        }
        return super.getCapability(capability, facing);
    }

    /** 网络当前可抽出的 FE（来源于相连的太阳能板）。 */
    @Override
    public int getEnergyStored() {
        return this.hasWorld() ? FeConvert.toFe(EuCableNet.storedEu(this.world, this.pos)) : 0;
    }

    /** 本档电缆每 tick 的传输上限折算成 FE，作为"管道容量"对外报告。 */
    @Override
    public int getMaxEnergyStored() {
        return FeConvert.toFe(this.tier().capacityPerTick());
    }

    @Override
    public boolean canExtract() {
        return true;
    }

    @Override
    public boolean canReceive() {
        return true;
    }

    @Override
    public int extractEnergy(int maxExtract, boolean simulate) {
        if (!this.hasWorld() || this.world.isRemote) {
            return 0;
        }
        return FeConvert.toFe(EuCableNet.drain(this.world, this.pos, FeConvert.toEu(maxExtract), simulate));
    }

    @Override
    public int receiveEnergy(int maxReceive, boolean simulate) {
        if (!this.hasWorld() || this.world.isRemote) {
            return 0;
        }
        return FeConvert.toFe(EuCableNet.fill(this.world, this.pos, FeConvert.toEu(maxReceive), simulate));
    }
}