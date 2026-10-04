package com.example.solarpower.tileentity;

import com.example.solarpower.block.StorageBoxBlock;
import com.example.solarpower.energy.EuCableNet;
import com.example.solarpower.energy.EuFormat;
import com.example.solarpower.energy.FeConvert;
import com.example.solarpower.energy.IEuEnergy;
import com.example.solarpower.energy.EuTier;
import com.example.solarpower.solar.SolarTier;

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
 * 储电盒方块实体（1.12.2，全档位共用，档位绑定 {@link SolarTier}）。
 * <p>语义对齐 IU 储能仓：五面进电（对电缆网络自动作为汇）、正面出电
 * （每 tick 主动向正面相邻电缆推流，上限 = 本档面板最大输出）。
 * 容量 = 本档面板缓冲 ×64；电压包等级 = 本档面板电压，超压包由
 * {@code receiveEu} 内的电压校验拒收。
 * <p>存档用 Base64 大端补码（百万位大数 {@code toString} 是平方级开销，禁用）；
 * 读档时档位未解析，存量在首个 tick 才夹回容量（同太阳能板）。
 * <p>正面朝向存在方块状态里，翻面只变状态不重建 TE
 * （{@link #shouldRefresh} 只在方块本体被替换时返回 true）。
 */
public class StorageBoxTile extends TileEntity implements ITickable, IEuEnergy, IEnergyStorage {

    /** 容量 = 面板缓冲 ×64（shiftLeft(6)）。 */
    private static final int CAPACITY_SHIFT = 6;

    public static final String TAG_ENERGY = "Energy";
    public static final String TAG_ENERGY_B64 = TAG_ENERGY + "_b64";
    /** NBT 字符串标签类型（{@link NBTTagCompound#hasKey(String, int)} 用）。 */
    private static final int NBT_STRING = 8;

    private BigInteger stored = BigInteger.ZERO;
    private SolarTier resolvedTier;

    /** 容量缓存：按档位实例失效（shiftLeft(6) 在百万位数上虽是线性开销，也不必每 tick 重算）。 */
    private SolarTier capacityForTier;
    private BigInteger capacityCache;

    /** 剩余容量缓存（电缆网络每 tick 反复询问；按「存量实例 + 容量实例」失效，同太阳能板）。 */
    private BigInteger roomForStored;
    private BigInteger roomForCapacity;
    private BigInteger roomCache = BigInteger.ZERO;

    /** 存量显示分量缓存（容器每秒取用 20 次，stored 每 tick 至多变一次）。 */
    private BigInteger partsFor;
    private int[] partsCache;

    /** 比较器读数缓存（stored×16/容量 向下取整，夹到 0~15）。 */
    private BigInteger comparatorFor;
    private BigInteger comparatorForCapacity;
    private int comparatorCache;

    /** 档位由所在方块的实例决定（1.12 的 TileEntity 构造器不带状态，懒解析）。 */
    public SolarTier getSolarTier() {
        if (this.resolvedTier == null && this.hasWorld()) {
            if (this.world.getBlockState(this.pos).getBlock() instanceof StorageBoxBlock) {
                this.resolvedTier = ((StorageBoxBlock) this.world
                        .getBlockState(this.pos).getBlock()).tier();
            }
        }
        return this.resolvedTier == null ? SolarTier.BASIC : this.resolvedTier;
    }

    /** 正面（能量芯/输出面）朝向，读方块状态；TE 未挂世界时回落北。 */
    public EnumFacing facing() {
        if (this.hasWorld()
                && this.world.getBlockState(this.pos).getBlock() instanceof StorageBoxBlock) {
            return this.world.getBlockState(this.pos).getValue(StorageBoxBlock.FACING);
        }
        return EnumFacing.NORTH;
    }

    /** 容量 = 面板缓冲 ×64（按档位实例缓存）。 */
    public BigInteger capacityEu() {
        SolarTier tier = getSolarTier();
        if (this.capacityForTier != tier) {
            this.capacityForTier = tier;
            this.capacityCache = tier.capacityEu().shiftLeft(CAPACITY_SHIFT);
        }
        return this.capacityCache;
    }

    /** 拆机保留电量：从物品 NBT 读回（{@code StorageBoxBlock#getDrops} 写入）。 */
    public void restoreEnergy(BigInteger value) {
        this.stored = value == null || value.signum() < 0 ? BigInteger.ZERO : value;
        markDirty();
    }

    /** 存量的显示分量（首 4 位有效数字 + 十进制指数，惰性缓存）。 */
    public int[] getStoredDisplayParts() {
        if (this.partsFor != this.stored) {
            this.partsFor = this.stored;
            this.partsCache = EuFormat.displayParts(this.stored);
        }
        return this.partsCache;
    }

    /** 比较器输出 0~15：存量×16 ÷ 容量 向下取整（移位代替 ×16，除法按存量实例缓存）。 */
    public int comparatorLevel() {
        BigInteger capacity = capacityEu();
        if (this.comparatorFor != this.stored || this.comparatorForCapacity != capacity) {
            this.comparatorFor = this.stored;
            this.comparatorForCapacity = capacity;
            this.comparatorCache = capacity.signum() <= 0 ? 0
                    : this.stored.shiftLeft(4).divide(capacity).intValueExact();
        }
        return Math.max(0, Math.min(15, this.comparatorCache));
    }

    // ------------------------------------------------------------------
    // 能量
    // ------------------------------------------------------------------

    @Override
    public void update() {
        if (this.world.isRemote) {
            return;
        }
        // 读档时档位未解析，存量在首个 tick 才夹回容量（同太阳能板）
        BigInteger capacity = capacityEu();
        if (this.stored.compareTo(capacity) > 0) {
            this.stored = capacity;
            markDirty();
        }
        if (this.stored.signum() > 0) {
            // IU 语义：仅正面出电，每个 tick 至多向正面相邻的那张电缆网推流一次
            EuCableNet.pushFromFacing(this.world, this.pos, this, getSolarTier().voltage(),
                    getSolarTier().maxOutputEu(), facing());
        }
    }

    @Override
    public BigInteger getStoredEu() {
        return this.stored;
    }

    @Override
    public BigInteger getCapacityEu() {
        return capacityEu();
    }

    @Override
    public BigInteger getRoomEu() {
        BigInteger capacity = capacityEu();
        if (this.roomForCapacity == capacity && this.roomForStored == this.stored) {
            return this.roomCache;
        }
        this.roomForCapacity = capacity;
        this.roomForStored = this.stored;
        this.roomCache = capacity.subtract(this.stored).max(BigInteger.ZERO);
        return this.roomCache;
    }

    @Override
    public EuTier getTier() {
        return getSolarTier().voltage();
    }

    @Override
    public BigInteger receiveEu(BigInteger amount, EuTier tier, boolean simulate) {
        if (amount == null || amount.signum() <= 0
                || !getTier().accepts(tier)) {
            return BigInteger.ZERO;
        }
        BigInteger accepted = amount.min(getRoomEu());
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

    // ------------------------------------------------------------------
    // Forge Energy 桥（同太阳能板：1 EU = 4 FE，双向）
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
        return FeConvert.toFe(this.stored);
    }

    @Override
    public int getMaxEnergyStored() {
        return FeConvert.toFe(capacityEu());
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
        return FeConvert.toFe(receiveEu(FeConvert.toEu(maxReceive), getTier(), simulate));
    }

    // ------------------------------------------------------------------
    // 存档 / 翻面
    // ------------------------------------------------------------------

    /**
     * Forge 对模组 TE 的默认实现是「状态一变就重建 TE」，而翻面只改 facing 属性——
     * 不覆写的话每次换朝向都会拆掉 TE、清空存量。这里改为只有方块本体被替换时才重建
     * （分子重组仪同款覆写）。
     */
    @Override
    public boolean shouldRefresh(World world, BlockPos pos, IBlockState oldState,
                                 IBlockState newState) {
        return oldState.getBlock() != newState.getBlock();
    }

    @Override
    public void readFromNBT(NBTTagCompound compound) {
        super.readFromNBT(compound);
        BigInteger value;
        if (compound.hasKey(TAG_ENERGY_B64, NBT_STRING)) {
            value = new BigInteger(Base64.getDecoder().decode(compound.getString(TAG_ENERGY_B64)));
        } else if (compound.hasKey(TAG_ENERGY, NBT_STRING)) {
            try {
                value = new BigInteger(compound.getString(TAG_ENERGY));
            } catch (NumberFormatException e) {
                value = BigInteger.ZERO;
            }
        } else {
            value = BigInteger.valueOf(compound.getLong(TAG_ENERGY));
        }
        // 只做符号保护：读档时档位未解析，容量钳制推迟到首个 update()
        this.stored = value == null || value.signum() < 0 ? BigInteger.ZERO : value;
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound compound) {
        super.writeToNBT(compound);
        compound.setString(TAG_ENERGY_B64,
                Base64.getEncoder().encodeToString(this.stored.toByteArray()));
        return compound;
    }
}
