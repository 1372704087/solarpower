package com.example.solarpower.tileentity;

import com.example.solarpower.block.TransformerBlock;
import com.example.solarpower.energy.EuCableNet;
import com.example.solarpower.energy.EuFormat;
import com.example.solarpower.energy.FeConvert;
import com.example.solarpower.energy.IEuEnergy;
import com.example.solarpower.energy.EuTier;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.block.state.IBlockState;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ITickable;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.energy.CapabilityEnergy;
import net.minecraftforge.energy.IEnergyStorage;

import net.minecraftforge.common.capabilities.Capability;

import java.math.BigInteger;
import java.util.Base64;

/**
 * 变压器方块实体（1.12.2，65 档，桥接电压 T↔T+1）。IC2 语义的双向转换：
 * 正面（能量芯面）= 高压侧（T+1），其余五面 = 低压侧（T）。不设模式开关——
 * 电从哪一侧进来，就从另一侧按对方电压输出（高压进→低压出 = 降压，反之 = 升压）。
 * <p>实现上按进电电压分流到两个内部缓冲池：低压包进低压池、高压包进高压池；
 * 每 tick 双向各自向相邻电缆推流一次，方向吞吐上限 = 低压档电压 2^(2T+3)
 * （IC2 变压器「每 tick 一个本档电压包」的连续化）。池容量 = 吞吐 ×8，只作
 * 缓冲不做储能（储电请用储电盒）。
 * <p>存储量极小（顶档也只有 2^133 量级），无 BigInteger 性能问题。
 */
public class TransformerTile extends TileEntity implements ITickable, IEuEnergy, IEnergyStorage {

    /** 池容量 = 吞吐 ×8。 */
    private static final int POOL_CAPACITY_TICKS = 8;

    private static final String TAG_LOW_B64 = "Low_b64";
    private static final String TAG_HIGH_B64 = "High_b64";

    private BigInteger lowStored = BigInteger.ZERO;
    private BigInteger highStored = BigInteger.ZERO;
    private EuTier resolvedLowTier;

    /** 低/高压档与吞吐（按档位实例缓存）。 */
    private EuTier lowTierCache;
    private EuTier highTierCache;
    private BigInteger rateCache;

    private BigInteger roomForLow;
    private BigInteger roomForHigh;
    private BigInteger roomCache = BigInteger.ZERO;

    private BigInteger comparatorFor;
    private BigInteger comparatorForHigh;
    private int comparatorCache;

    /** 池存量显示分量缓存（容器每秒取用 20 次，池存量每 tick 至多变一次）。 */
    private BigInteger lowPartsFor;
    private int[] lowPartsCache;
    private BigInteger highPartsFor;
    private int[] highPartsCache;

    /** 低压池存量的显示分量（首 4 位有效数字 + 十进制指数，惰性缓存）。 */
    public int[] getLowDisplayParts() {
        if (this.lowPartsFor != this.lowStored) {
            this.lowPartsFor = this.lowStored;
            this.lowPartsCache = EuFormat.displayParts(this.lowStored);
        }
        return this.lowPartsCache;
    }

    /** 高压池存量的显示分量（同上）。 */
    public int[] getHighDisplayParts() {
        if (this.highPartsFor != this.highStored) {
            this.highPartsFor = this.highStored;
            this.highPartsCache = EuFormat.displayParts(this.highStored);
        }
        return this.highPartsCache;
    }

    /** 档位由所在方块的实例决定（懒解析，回落 LV）。 */
    public EuTier lowTier() {
        if (this.lowTierCache == null && this.hasWorld()) {
            if (this.world.getBlockState(this.pos).getBlock() instanceof TransformerBlock) {
                this.lowTierCache = ((TransformerBlock) this.world
                        .getBlockState(this.pos).getBlock()).tier();
            }
        }
        return this.lowTierCache == null ? EuTier.LV : this.lowTierCache;
    }

    public EuTier highTier() {
        if (this.highTierCache == null) {
            this.highTierCache = EuTier.byIuTier(lowTier().iuTier() + 1);
        }
        return this.highTierCache;
    }

    /** 每 tick 每方向的转换吞吐上限 = 低压档电压 2^(2T+3)。 */
    public BigInteger rate() {
        if (this.rateCache == null) {
            this.rateCache = lowTier().maxVoltage();
        }
        return this.rateCache;
    }

    /** 池容量 = 吞吐 ×8（缓冲，不做储能）。 */
    public BigInteger poolCapacity() {
        return rate().shiftLeft(3);   // 吞吐 ×8
    }

    /** 正面（高压侧）朝向，读方块状态。 */
    public EnumFacing facing() {
        if (this.hasWorld()
                && this.world.getBlockState(this.pos).getBlock() instanceof TransformerBlock) {
            return this.world.getBlockState(this.pos).getValue(TransformerBlock.FACING);
        }
        return EnumFacing.NORTH;
    }

    public BigInteger getLowStored() {
        return this.lowStored;
    }

    public BigInteger getHighStored() {
        return this.highStored;
    }

    // ------------------------------------------------------------------
    // 能量
    // ------------------------------------------------------------------

    @Override
    public void update() {
        if (this.world.isRemote) {
            return;
        }
        clampPools();
        // 高压池：从正面（唯一高压面）输出
        if (this.highStored.signum() > 0) {
            EuCableNet.pushFromFacing(this.world, this.pos, this, highTier(),
                    rate(), facing());
        }
        // 低压池：从其余五面输出（IU 变压器低压侧全面放电）
        for (EnumFacing dir : EnumFacing.values()) {
            if (dir == facing()) {
                continue;
            }
            if (this.lowStored.signum() <= 0) {
                break;
            }
            EuCableNet.pushFromFacing(this.world, this.pos, this, lowTier(),
                    rate(), dir);
        }
    }

    private void clampPools() {
        BigInteger cap = poolCapacity();
        if (this.lowStored.compareTo(cap) > 0) {
            this.lowStored = cap;
            markDirty();
        }
        if (this.highStored.compareTo(cap) > 0) {
            this.highStored = cap;
            markDirty();
        }
    }

    @Override
    public BigInteger getStoredEu() {
        return this.lowStored.add(this.highStored);
    }

    @Override
    public BigInteger getCapacityEu() {
        return poolCapacity().shiftLeft(1);   // 两池合计
    }

    @Override
    public BigInteger getRoomEu() {
        BigInteger cap = poolCapacity();
        if (this.roomForLow != null && this.roomForLow == cap) {
            return this.roomCache;
        }
        return computeRoom(cap);
    }

    private BigInteger computeRoom(BigInteger cap) {
        BigInteger room = cap.subtract(this.lowStored).max(BigInteger.ZERO)
                .add(cap.subtract(this.highStored).max(BigInteger.ZERO));
        this.roomForLow = cap;
        this.roomForHigh = cap;
        this.roomCache = room;
        return room;
    }

    @Override
    public EuTier getTier() {
        return highTier();
    }

    /**
     * 按来包电压分流：低压包（≤T）进低压池、高压包（≤T+1）进高压池，更高拒收。
     * 面无关——电网按来方电压即可正确路由，等价于 IC2 的「哪侧进电走哪侧池」。
     */
    @Override
    public BigInteger receiveEu(BigInteger amount, EuTier tier, boolean simulate) {
        if (amount == null || amount.signum() <= 0) {
            return BigInteger.ZERO;
        }
        BigInteger cap = poolCapacity();
        BigInteger target;
        if (lowTier().accepts(tier)) {
            target = this.lowStored;
        } else if (highTier().accepts(tier)) {
            target = this.highStored;
        } else {
            return BigInteger.ZERO;
        }
        BigInteger accepted = amount.min(cap.subtract(target).max(BigInteger.ZERO));
        if (accepted.signum() <= 0) {
            return BigInteger.ZERO;
        }
        if (!simulate) {
            if (lowTier().accepts(tier)) {
                this.lowStored = this.lowStored.add(accepted);
            } else {
                this.highStored = this.highStored.add(accepted);
            }
            markDirty();
        }
        return accepted;
    }

    /** 通用抽取：先低压池后高压池（FE 桥与电网 drain 用）。 */
    @Override
    public BigInteger extractEu(BigInteger amount, boolean simulate) {
        if (amount == null || amount.signum() <= 0) {
            return BigInteger.ZERO;
        }
        BigInteger fromLow = amount.min(this.lowStored);
        BigInteger fromHigh = amount.subtract(fromLow).min(this.highStored);
        BigInteger extracted = fromLow.add(fromHigh);
        if (extracted.signum() <= 0) {
            return BigInteger.ZERO;
        }
        if (!simulate) {
            this.lowStored = this.lowStored.subtract(fromLow);
            this.highStored = this.highStored.subtract(fromHigh);
            markDirty();
        }
        return extracted;
    }

    /** 比较器：两池合计占两池总容量的比例 0~15。 */
    public int comparatorLevel() {
        BigInteger cap = poolCapacity();
        if (this.comparatorFor != this.lowStored || this.comparatorForHigh != this.highStored) {
            this.comparatorFor = this.lowStored;
            this.comparatorForHigh = this.highStored;
            BigInteger total = this.lowStored.add(this.highStored);
            this.comparatorCache = total.shiftLeft(4).divide(cap.shiftLeft(1)).intValueExact();
        }
        return Math.max(0, Math.min(15, this.comparatorCache));
    }

    // ------------------------------------------------------------------
    // Forge Energy 桥（挂低压池，1 EU = 4 FE）
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
        return FeConvert.toFe(this.lowStored);
    }

    @Override
    public int getMaxEnergyStored() {
        return FeConvert.toFe(poolCapacity());
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
        return FeConvert.toFe(extractEu(FeConvert.toEu(maxExtract), simulate));
    }

    @Override
    public int receiveEnergy(int maxReceive, boolean simulate) {
        return FeConvert.toFe(receiveEu(FeConvert.toEu(maxReceive), lowTier(), simulate));
    }

    // ------------------------------------------------------------------
    // 存档 / 翻面
    // ------------------------------------------------------------------

    @Override
    public boolean shouldRefresh(World world, BlockPos pos, IBlockState oldState,
                                 IBlockState newState) {
        return oldState.getBlock() != newState.getBlock();
    }

    @Override
    public void readFromNBT(NBTTagCompound compound) {
        super.readFromNBT(compound);
        this.lowStored = readB64(compound, TAG_LOW_B64);
        this.highStored = readB64(compound, TAG_HIGH_B64);
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound compound) {
        super.writeToNBT(compound);
        compound.setString(TAG_LOW_B64,
                Base64.getEncoder().encodeToString(this.lowStored.toByteArray()));
        compound.setString(TAG_HIGH_B64,
                Base64.getEncoder().encodeToString(this.highStored.toByteArray()));
        return compound;
    }

    private static BigInteger readB64(NBTTagCompound compound, String key) {
        if (!compound.hasKey(key)) {
            return BigInteger.ZERO;
        }
        return new BigInteger(Base64.getDecoder().decode(compound.getString(key)));
    }
}
