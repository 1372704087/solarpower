package com.example.solarpower.tileentity;

import com.example.solarpower.energy.EuCableNet;
import com.example.solarpower.energy.FeConvert;
import com.example.solarpower.energy.IEuEnergy;
import com.example.solarpower.energy.EuTier;
import com.example.solarpower.solar.SolarTier;
import com.example.solarpower.compat.Ic2Compat;

import ic2.api.energy.EnergyNet;
import ic2.api.energy.event.EnergyTileLoadEvent;
import ic2.api.energy.event.EnergyTileUnloadEvent;
import ic2.api.energy.tile.IEnergyAcceptor;
import ic2.api.energy.tile.IEnergyEmitter;
import ic2.api.energy.tile.IEnergySink;
import ic2.api.energy.tile.IEnergySource;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ITickable;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.energy.CapabilityEnergy;
import net.minecraftforge.energy.IEnergyStorage;
import net.minecraftforge.fml.common.Optional;

import java.math.BigDecimal;
import java.math.BigInteger;

/**
 * 太阳能板方块实体（1.12.2，全档位共用）。
 * 判定与 NeoForge 版一致：有天空光的维度 + 上方无遮挡才发电；白天按档位，
 * 夜晚（月光）为白天的一半（基础档不发电）；雨天/雷暴 ×0.65。
 * <p>发电量与蓄电量用 {@link BigInteger} 承载，不设数值上限。
 * <p>IC2 兼容是软依赖：装了 IC2 时本板同时作为 {@code IEnergySource} 与 {@code IEnergySink}
 * 注册进 IC2 电网（双向）；没装时这两个接口与相关方法会被 Forge 的 {@code @Optional} 剥离。
 * <p>Forge Energy 兼容无需依赖：本板对暴露 {@link IEnergyStorage}，Mekanism 的万能线缆
 * （以及其它 FE 机器）可以直接接上来取电或充电；换算 1 EU = 4 FE。
 */
@Optional.InterfaceList({
        @Optional.Interface(iface = "ic2.api.energy.tile.IEnergySource", modid = "ic2"),
        @Optional.Interface(iface = "ic2.api.energy.tile.IEnergySink", modid = "ic2")
})
public class SolarPanelTile extends TileEntity implements ITickable, IEnergySource, IEnergySink, IEnergyStorage {

    /** 雨天/雷暴系数 ×0.65，用整数 65/100 精确计算。 */
    private static final BigInteger RAIN_NUMERATOR = BigInteger.valueOf(65L);
    private static final BigInteger RAIN_DENOMINATOR = BigInteger.valueOf(100L);

    private static final String TAG_ENERGY = "Energy";
    /** NBT 字符串标签类型（{@link NBTTagCompound#hasKey(String, int)} 用）。 */
    private static final int NBT_STRING = 8;

    private final TileEnergy storage = new TileEnergy();
    private BigInteger generating = BigInteger.ZERO;
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

    public BigInteger getGenerating() {
        return this.generating;
    }

    public boolean isNight() {
        return this.hasWorld() && !this.world.isDaytime();
    }

    // ------------------------------------------------------------------
    // IC2 兼容（软依赖）：装了 IC2 才注册进 IC2 电网，双向供取电。
    // 未装时以下带 @Optional.Method 的方法会被 Forge 剥离，调用点由 Ic2Compat.LOADED 拦住，
    // 因此方法体里引用的 ic2.* 类型不会被 JVM 解析到。
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

    /** IC2 侧单 tick 传输上限 = 本档电压；IC2 的 tier 上限正是 2^(2t+3)，与本模组 tier 电压一致。 */
    private BigInteger ic2PacketCeiling() {
        return this.getTier().voltage().maxVoltage();
    }

    @Optional.Method(modid = "ic2")
    @Override
    public double getOfferedEnergy() {
        // 存量超过 IC2 单 tick 上限时只报上限："超上限就传上限的电"
        return ic2FromEu(this.storage.getStoredEu().min(this.ic2PacketCeiling()));
    }

    @Optional.Method(modid = "ic2")
    @Override
    public void drawEnergy(double amount) {
        this.storage.extractEu(euFromIc2(amount), false);
    }

    @Optional.Method(modid = "ic2")
    @Override
    public int getSourceTier() {
        return this.getTier().voltage().iuTier();
    }

    @Optional.Method(modid = "ic2")
    @Override
    public boolean emitsEnergyTo(IEnergyAcceptor receiver, EnumFacing side) {
        return true;
    }

    @Optional.Method(modid = "ic2")
    @Override
    public double getDemandedEnergy() {
        BigInteger space = this.storage.getCapacityEu().subtract(this.storage.getStoredEu());
        return ic2FromEu(space.min(this.ic2PacketCeiling()));
    }

    @Optional.Method(modid = "ic2")
    @Override
    public int getSinkTier() {
        return this.getTier().voltage().iuTier();
    }

    @Optional.Method(modid = "ic2")
    @Override
    public double injectEnergy(EnumFacing directionFrom, double amount, double voltage) {
        BigInteger requested = euFromIc2(amount);
        EuTier incoming = EuTier.byVoltage(euFromIc2(voltage));
        BigInteger accepted = this.storage.receiveEu(requested, incoming, false);
        // 返回未被接收的部分（超压或装满）
        return ic2FromEu(requested.subtract(accepted));
    }

    @Optional.Method(modid = "ic2")
    @Override
    public boolean acceptsEnergyFrom(IEnergyEmitter emitter, EnumFacing side) {
        return true;
    }

    /** IC2 的 double 电量转 BigInteger：非法值按 0，Infinity 夹到 double 上限。 */
    private static BigInteger euFromIc2(double amount) {
        if (!(amount > 0.0D)) {
            return BigInteger.ZERO;
        }
        if (Double.isInfinite(amount)) {
            amount = Double.MAX_VALUE;
        }
        return BigDecimal.valueOf(amount).toBigInteger();
    }

    /** BigInteger 电量转 IC2 的 double。 */
    private static double ic2FromEu(BigInteger amount) {
        return amount == null || amount.signum() <= 0 ? 0.0D : amount.doubleValue();
    }

    // ------------------------------------------------------------------
    // Forge Energy（无依赖）：直接暴露本板储能，Mekanism 万能线缆等 FE 线缆可双向接驳。
    // 换算 1 EU = 4 FE，FE 超 int 上限时只报上限（FeConvert 负责夹取）。
    // ------------------------------------------------------------------

    @Override
    public boolean hasCapability(Capability<?> capability, EnumFacing facing) {
        return capability == CapabilityEnergy.ENERGY || super.hasCapability(capability, facing);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T getCapability(Capability<T> capability, EnumFacing facing) {
        if (capability == CapabilityEnergy.ENERGY) {
            return (T) this;
        }
        return super.getCapability(capability, facing);
    }

    @Override
    public int getEnergyStored() {
        return FeConvert.toFe(this.storage.getStoredEu());
    }

    @Override
    public int getMaxEnergyStored() {
        return FeConvert.toFe(this.storage.getCapacityEu());
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
        return FeConvert.toFe(this.storage.extractEu(FeConvert.toEu(maxExtract), simulate));
    }

    @Override
    public int receiveEnergy(int maxReceive, boolean simulate) {
        // FE 无电压概念，按本板自身等级注入，因此不会被误判为超压
        return FeConvert.toFe(this.storage.receiveEu(FeConvert.toEu(maxReceive), this.getTier().voltage(), simulate));
    }

    @Override
    public void update() {
        if (this.world.isRemote) {
            return;
        }
        SolarTier tier = this.getTier();
        BigInteger generated = this.computeGeneration(tier);
        this.generating = generated;
        if (generated.signum() > 0) {
            this.storage.receiveEu(generated, tier.voltage(), false);
        }
        if (this.storage.getStoredEu().signum() > 0) {
            this.pushToCables();
        }
    }

    private BigInteger computeGeneration(SolarTier tier) {
        if (!this.world.provider.hasSkyLight()) {
            return BigInteger.ZERO; // 下界/末地这类没有天空光照的维度
        }
        if (!this.world.canSeeSky(this.pos.up())) {
            return BigInteger.ZERO; // 上方被方块遮挡
        }
        BigInteger base = this.world.isDaytime() ? tier.generationEu() : tier.nightGenerationEu();
        if (base.signum() <= 0) {
            return BigInteger.ZERO;
        }
        boolean raining = this.world.isRaining() || this.world.isThundering();
        return raining ? base.multiply(RAIN_NUMERATOR).divide(RAIN_DENOMINATOR) : base;
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
        BigInteger stored;
        if (compound.hasKey(TAG_ENERGY, NBT_STRING)) {
            try {
                stored = new BigInteger(compound.getString(TAG_ENERGY));
            } catch (NumberFormatException e) {
                stored = BigInteger.ZERO;
            }
        } else {
            // 兼容旧存档：此前以 long 保存
            stored = BigInteger.valueOf(compound.getLong(TAG_ENERGY));
        }
        this.storage.setStored(stored);
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound compound) {
        super.writeToNBT(compound);
        compound.setString(TAG_ENERGY, this.storage.getStoredEu().toString());
        return compound;
    }

    /** 储能容器：容量/电压跟随懒解析的档位。 */
    private final class TileEnergy implements IEuEnergy {

        private BigInteger stored = BigInteger.ZERO;

        @Override
        public BigInteger getStoredEu() {
            return this.stored;
        }

        @Override
        public BigInteger getCapacityEu() {
            return SolarPanelTile.this.getTier().capacityEu();
        }

        @Override
        public EuTier getTier() {
            return SolarPanelTile.this.getTier().voltage();
        }

        @Override
        public BigInteger receiveEu(BigInteger amount, EuTier tier, boolean simulate) {
            if (amount == null || amount.signum() <= 0
                    || !SolarPanelTile.this.getTier().voltage().accepts(tier)) {
                return BigInteger.ZERO;
            }
            BigInteger accepted = amount.min(getCapacityEu().subtract(this.stored));
            if (accepted.signum() <= 0) {
                return BigInteger.ZERO;
            }
            if (!simulate) {
                this.stored = this.stored.add(accepted);
                markDirty();
            }
            return accepted;
        }

        @Override
        public BigInteger extractEu(BigInteger amount, boolean simulate) {
            if (amount == null || amount.signum() <= 0) {
                return BigInteger.ZERO;
            }
            BigInteger extracted = amount.min(this.stored);
            if (extracted.signum() <= 0) {
                return BigInteger.ZERO;
            }
            if (!simulate) {
                this.stored = this.stored.subtract(extracted);
                markDirty();
            }
            return extracted;
        }

        void setStored(BigInteger value) {
            if (value == null || value.signum() < 0) {
                this.stored = BigInteger.ZERO;
            } else {
                this.stored = value.min(getCapacityEu());
            }
            markDirty();
        }
    }
}