package com.example.solarpower.tileentity;

import com.example.solarpower.energy.EuTier;
import com.example.solarpower.energy.FeConvert;
import com.example.solarpower.energy.IEuEnergy;
import com.example.solarpower.registry.ModBlocks;
import com.example.solarpower.block.MolecularTransformerBlock;
import com.example.solarpower.compat.Ic2Compat;
import com.example.solarpower.solar.MtRecipes;

import ic2.api.energy.EnergyNet;
import ic2.api.energy.event.EnergyTileLoadEvent;
import ic2.api.energy.event.EnergyTileUnloadEvent;
import ic2.api.energy.tile.IEnergyAcceptor;
import ic2.api.energy.tile.IEnergyEmitter;
import ic2.api.energy.tile.IEnergySink;

import net.minecraft.block.state.IBlockState;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ITickable;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.energy.CapabilityEnergy;
import net.minecraftforge.energy.IEnergyStorage;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.fml.common.Optional;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

/**
 * 分子重组仪方块实体（1.12.2）。机制照搬 Advanced Solar Panels 的 TileEntityMolecularTransformer：
 * <ul>
 *     <li>单输入/单输出；输入必须有配方才收；开始转换时先扣 1 个输入并锁定产出；</li>
 *     <li>每 tick 把缓存里的 EU 注入当前任务，直到注满配方的总 EU，产出进入输出槽；</li>
 *     <li>缓存上限 2^31 EU（14 级单包电压上限，用户指定）；受电等级放宽到最高级 59
 *         （见 {@link #SINK_TIER}），ASP 原值为 14 级 + 缓存 100k EU；</li>
 * </ul>
 * <p>能量入口三通道（与太阳能板一致）：IC2 电网（软依赖，{@code @Optional}）、
 * 本模组玻璃电缆网络（暴露 {@link IEuEnergy}）、Forge Energy（1 EU = 4 FE，只进不出）。
 * 工作进度走方块 active 状态与比较器（0~15）。
 */
@Optional.InterfaceList({
        @Optional.Interface(iface = "ic2.api.energy.tile.IEnergySink", modid = "ic2")
})
public class MolecularTransformerTile extends TileEntity
        implements ITickable, IEuEnergy, IEnergySink, IEnergyStorage {

    /**
     * 内部缓存上限（EU）：14 级电压上限（QUARK，2^31 ≈ 21.5 亿，用户指定）。
     * ASP 原值为 100,000 EU，已按用户要求放大。
     */
    public static final BigInteger CAPACITY_EU = BigInteger.ONE.shiftLeft(31);
    /**
     * 受电电压等级。ASP 原值为 14（QUARK，2^31 EU/packet），本模组放宽到最高级 59
     * （2^121 EU/packet，与 {@link EuTier} 顶格一致）：60 档面板全部可直喂，
     * 无需降压变压器——对原机制的一处有意放宽；此值只影响进包电压。
     */
    public static final int SINK_TIER = 59;

    private static final String TAG_ENERGY = "Energy";
    private static final String TAG_INPUT = "Input";
    private static final String TAG_OUTPUT = "Output";
    private static final String TAG_JOB = "Job";
    private static final String TAG_TOTAL = "Total";
    private static final String TAG_USED = "Used";
    /** NBT 字符串标签类型（{@link NBTTagCompound#hasKey(String, int)} 用）。 */
    private static final int NBT_STRING = 8;

    private final ItemStackHandler input = new ItemStackHandler(1) {
        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            // 只收有配方的输入（自动化管道同样受此约束）
            return MtRecipes.find(stack) == null ? stack : super.insertItem(slot, stack, simulate);
        }
    };
    private final ItemStackHandler output = new ItemStackHandler(1) {
        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            return stack;   // 只出不进
        }
    };

    /** 当前任务的产出预锁定（输入已扣、能量未注满），照搬 ASP 的 currentOutput。 */
    private ItemStack jobOutput = ItemStack.EMPTY;
    private int totalEu;
    private int energyUsed;
    private boolean working;

    private final MtEnergy storage = new MtEnergy();

    // ------------------------------------------------------------------
    // 数据访问（容器/GUI 同步用）
    // ------------------------------------------------------------------

    public ItemStackHandler getInput() {
        return this.input;
    }

    public ItemStackHandler getOutput() {
        return this.output;
    }

    public IEuEnergy getEnergy() {
        return this.storage;
    }

    // IEuEnergy 直接由方块实体暴露：玻璃电缆网络按此接口识别受电端（汇）

    @Override
    public BigInteger getStoredEu() {
        return this.storage.getStoredEu();
    }

    @Override
    public BigInteger getCapacityEu() {
        return this.storage.getCapacityEu();
    }

    @Override
    public EuTier getTier() {
        return this.storage.getTier();
    }

    @Override
    public BigInteger receiveEu(BigInteger amount, EuTier tier, boolean simulate) {
        return this.storage.receiveEu(amount, tier, simulate);
    }

    @Override
    public BigInteger extractEu(BigInteger amount, boolean simulate) {
        return this.storage.extractEu(amount, simulate);
    }

    public int getTotalEu() {
        return this.totalEu;
    }

    public int getEnergyUsed() {
        return this.energyUsed;
    }

    public boolean isWorking() {
        return this.working;
    }

    /** 比较器输出 0~15（照搬 ASP 公式）。 */
    public int comparatorLevel() {
        return this.totalEu <= 0 ? 0 : this.energyUsed * 15 / this.totalEu;
    }

    /** 拆机时掉落的物品（输入槽 + 输出槽）。 */
    public List<ItemStack> dropContents() {
        List<ItemStack> drops = new ArrayList<>();
        for (ItemStackHandler handler : new ItemStackHandler[]{this.input, this.output}) {
            for (int i = 0; i < handler.getSlots(); i++) {
                ItemStack stack = handler.getStackInSlot(i);
                if (!stack.isEmpty()) {
                    drops.add(stack.copy());
                    handler.setStackInSlot(i, ItemStack.EMPTY);
                }
            }
        }
        return drops;
    }

    // ------------------------------------------------------------------
    // 工作循环
    // ------------------------------------------------------------------

    /**
     * Forge 对模组 TE 的默认实现是「状态一变就重建 TE」，而本机每 tick 都可能切换
     * active 属性——不覆写的话每次开工都会拆掉 TE：任务和槽内物品全丢、界面被关闭。
     * 这里改为只有方块本体被替换时才重建。
     */
    @Override
    public boolean shouldRefresh(World world, BlockPos pos, IBlockState oldState, IBlockState newState) {
        return oldState.getBlock() != newState.getBlock();
    }

    @Override
    public void update() {
        if (this.world.isRemote) {
            return;
        }
        boolean active = false;

        // 空闲且有输入：锁定配方、扣除 1 个输入
        if (this.jobOutput.isEmpty() && !this.input.getStackInSlot(0).isEmpty()) {
            MtRecipes.MtRecipe recipe = MtRecipes.find(this.input.getStackInSlot(0));
            if (recipe != null && canInsertOutput(recipe.output)) {
                this.jobOutput = recipe.output.copy();
                this.totalEu = recipe.totalEu;
                this.energyUsed = 0;
                this.input.extractItem(0, 1, false);
                markDirty();
                this.world.updateComparatorOutputLevel(this.pos, ModBlocks.molecularTransformer());
            }
        }

        // 有任务：把缓存里的 EU 尽量注入，注满则出料
        if (!this.jobOutput.isEmpty()) {
            BigInteger need = BigInteger.valueOf(this.totalEu - this.energyUsed);
            BigInteger took = this.storage.extractEu(need.min(this.storage.getStoredEu()), false);
            if (took.signum() > 0) {
                this.energyUsed += took.intValueExact();
                active = true;
                markDirty();
            }
            if (this.energyUsed >= this.totalEu) {
                // 出料：直接写入输出槽（insertItem 被「只出不进」覆写拒绝，insertItemStacked 会全数弹回）；
                // 输出槽被占/塞满时保留任务下 tick 重试，绝不吞物品
                ItemStack produced = this.jobOutput.copy();
                ItemStack existing = this.output.getStackInSlot(0);
                boolean stored;
                if (existing.isEmpty()) {
                    this.output.setStackInSlot(0, produced);
                    stored = true;
                } else if (existing.getItem() == produced.getItem()
                        && existing.getMetadata() == produced.getMetadata()
                        && ItemStack.areItemStackTagsEqual(existing, produced)
                        && existing.getCount() + produced.getCount() <= existing.getMaxStackSize()) {
                    existing.grow(produced.getCount());
                    this.output.setStackInSlot(0, existing);
                    stored = true;
                } else {
                    stored = false;
                }
                if (stored) {
                    this.jobOutput = ItemStack.EMPTY;
                    this.totalEu = 0;
                    this.energyUsed = 0;
                }
                markDirty();
                this.world.updateComparatorOutputLevel(this.pos, ModBlocks.molecularTransformer());
            }
        }

        this.working = active;
        setActive(active);
    }

    private boolean canInsertOutput(ItemStack stack) {
        ItemStack existing = this.output.getStackInSlot(0);
        if (existing.isEmpty()) {
            return true;
        }
        return existing.getItem() == stack.getItem()
                && existing.getMetadata() == stack.getMetadata()
                && ItemStack.areItemStackTagsEqual(existing, stack)
                && existing.getCount() + stack.getCount() <= existing.getMaxStackSize();
    }

    private void setActive(boolean active) {
        if (this.world.getTileEntity(this.pos) != this) {
            return;
        }
        IBlockState state = this.world.getBlockState(this.pos);
        if (state.getBlock() == ModBlocks.molecularTransformer()
                && state.getValue(MolecularTransformerBlock.ACTIVE) != active) {
            this.world.setBlockState(this.pos, state.withProperty(MolecularTransformerBlock.ACTIVE, active), 3);
        }
    }

    // ------------------------------------------------------------------
    // NBT
    // ------------------------------------------------------------------

    @Override
    public void readFromNBT(NBTTagCompound compound) {
        super.readFromNBT(compound);
        if (compound.hasKey(TAG_ENERGY, NBT_STRING)) {
            try {
                this.storage.setStored(new BigInteger(compound.getString(TAG_ENERGY)));
            } catch (NumberFormatException e) {
                this.storage.setStored(BigInteger.ZERO);
            }
        } else {
            this.storage.setStored(BigInteger.valueOf(compound.getLong(TAG_ENERGY)));
        }
        this.input.deserializeNBT(compound.getCompoundTag(TAG_INPUT));
        this.output.deserializeNBT(compound.getCompoundTag(TAG_OUTPUT));
        this.jobOutput = compound.hasKey(TAG_JOB) ? new ItemStack(compound.getCompoundTag(TAG_JOB)) : ItemStack.EMPTY;
        this.totalEu = compound.getInteger(TAG_TOTAL);
        this.energyUsed = compound.getInteger(TAG_USED);
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound compound) {
        super.writeToNBT(compound);
        compound.setString(TAG_ENERGY, this.storage.getStoredEu().toString());
        compound.setTag(TAG_INPUT, this.input.serializeNBT());
        compound.setTag(TAG_OUTPUT, this.output.serializeNBT());
        if (!this.jobOutput.isEmpty()) {
            compound.setTag(TAG_JOB, this.jobOutput.writeToNBT(new NBTTagCompound()));
        }
        compound.setInteger(TAG_TOTAL, this.totalEu);
        compound.setInteger(TAG_USED, this.energyUsed);
        return compound;
    }

    // ------------------------------------------------------------------
    // IC2 兼容（软依赖）：与太阳能板同一套注册/受电模式，只做受电端（sink）
    // ------------------------------------------------------------------

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

    /** IC2 侧单 tick 请求上限 = 14 级单包电压。 */
    private BigInteger ic2PacketCeiling() {
        return EuTier.byIuTier(SINK_TIER).maxVoltage();
    }

    @Optional.Method(modid = "ic2")
    @Override
    public double getDemandedEnergy() {
        BigInteger space = this.storage.getCapacityEu().subtract(this.storage.getStoredEu());
        return ic2FromEu(space.max(BigInteger.ZERO).min(this.ic2PacketCeiling()));
    }

    @Optional.Method(modid = "ic2")
    @Override
    public int getSinkTier() {
        return SINK_TIER;
    }

    @Optional.Method(modid = "ic2")
    @Override
    public double injectEnergy(EnumFacing directionFrom, double amount, double voltage) {
        BigInteger requested = euFromIc2(amount);
        EuTier incoming = EuTier.byVoltage(euFromIc2(voltage));
        BigInteger accepted = this.storage.receiveEu(requested, incoming, false);
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
    // Forge Energy：只进不出（本机是纯用电器），1 EU = 4 FE
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
        return false;
    }

    @Override
    public boolean canReceive() {
        return true;
    }

    @Override
    public int extractEnergy(int maxExtract, boolean simulate) {
        return 0;
    }

    @Override
    public int receiveEnergy(int maxReceive, boolean simulate) {
        // FE 无电压概念，按最低等级注入（不会超过 14 级上限）
        return FeConvert.toFe(this.storage.receiveEu(FeConvert.toEu(maxReceive), EuTier.LV, simulate));
    }

    // ------------------------------------------------------------------
    // 储能容器：固定容量与等级
    // ------------------------------------------------------------------

    private final class MtEnergy implements IEuEnergy {

        private BigInteger stored = BigInteger.ZERO;

        @Override
        public BigInteger getStoredEu() {
            return this.stored;
        }

        @Override
        public BigInteger getCapacityEu() {
            return CAPACITY_EU;
        }

        @Override
        public EuTier getTier() {
            return EuTier.byIuTier(SINK_TIER);
        }

        @Override
        public BigInteger receiveEu(BigInteger amount, EuTier tier, boolean simulate) {
            if (amount == null || amount.signum() <= 0 || !getTier().accepts(tier)) {
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
