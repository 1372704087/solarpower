package com.example.solarpower.block;

import com.example.solarpower.SolarPower;
import com.example.solarpower.compat.Ic2Compat;
import com.example.solarpower.solar.GlassCableTier;
import com.example.solarpower.tileentity.GlassCableTile;

import ic2.api.energy.tile.IEnergyConductor;
import ic2.api.energy.tile.IEnergySink;
import ic2.api.energy.tile.IEnergySource;

import net.minecraft.block.BlockContainer;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumBlockRenderType;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

import net.minecraftforge.common.property.ExtendedBlockState;
import net.minecraftforge.common.property.IExtendedBlockState;
import net.minecraftforge.common.property.IUnlistedProperty;
import net.minecraftforge.energy.CapabilityEnergy;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;

/**
 * 玻璃电缆方块（1.12.2）。等级决定线损与容量。
 * <p>1.12.2 每个方块最多 16 个状态，因此连接信息不进 blockstate，
 * 而是通过 {@link #CONNECTIONS} 扩展状态在渲染时按邻居动态生成。
 */
public class GlassCableBlock extends BlockContainer {

    /** 六向连接掩码：bit = 1 << EnumFacing.getIndex()。 */
    public static final IUnlistedProperty<Integer> CONNECTIONS = new IUnlistedProperty<Integer>() {
        @Override
        public String getName() {
            return "connections";
        }

        @Override
        public boolean isValid(Integer value) {
            return value != null && value >= 0 && value < 64;
        }

        @Override
        public Class<Integer> getType() {
            return Integer.class;
        }

        @Override
        public String valueToString(Integer value) {
            return value.toString();
        }
    };

    private static final AxisAlignedBB CORE = new AxisAlignedBB(6 / 16D, 6 / 16D, 6 / 16D, 10 / 16D, 10 / 16D, 10 / 16D);
    private static final AxisAlignedBB ARM_NORTH = new AxisAlignedBB(6 / 16D, 6 / 16D, 0 / 16D, 10 / 16D, 10 / 16D, 6 / 16D);
    private static final AxisAlignedBB ARM_SOUTH = new AxisAlignedBB(6 / 16D, 6 / 16D, 10 / 16D, 10 / 16D, 10 / 16D, 16 / 16D);
    private static final AxisAlignedBB ARM_WEST = new AxisAlignedBB(0 / 16D, 6 / 16D, 6 / 16D, 6 / 16D, 10 / 16D, 10 / 16D);
    private static final AxisAlignedBB ARM_EAST = new AxisAlignedBB(10 / 16D, 6 / 16D, 6 / 16D, 16 / 16D, 10 / 16D, 10 / 16D);
    private static final AxisAlignedBB ARM_UP = new AxisAlignedBB(6 / 16D, 10 / 16D, 6 / 16D, 10 / 16D, 16 / 16D, 10 / 16D);
    private static final AxisAlignedBB ARM_DOWN = new AxisAlignedBB(6 / 16D, 0 / 16D, 6 / 16D, 10 / 16D, 6 / 16D, 10 / 16D);

    private final GlassCableTier tier;

    public GlassCableBlock(GlassCableTier tier) {
        super(Material.GLASS);
        this.tier = tier;
        setUnlocalizedName(SolarPower.MODID + "." + tier.id());
        setHardness(0.5F);
        setSoundType(SoundType.GLASS);
        // ItemBlock 构造时会拷贝 Block 的创造标签；不设会进原版「建筑方块」而不是工业太阳能
        setCreativeTab(SolarPower.CREATIVE_TAB);
    }

    public GlassCableTier tier() {
        return this.tier;
    }

    /** 电缆与相邻的电缆（任意等级）和太阳能板连接。 */
    public static boolean connectsTo(IBlockState neighbor) {
        return neighbor.getBlock() instanceof GlassCableBlock
                || neighbor.getBlock() instanceof SolarPanelBlock;
    }

    /** 计算某位置对六面的连接掩码。 */
    public static int maskFor(IBlockAccess world, BlockPos pos) {
        int mask = 0;
        for (EnumFacing dir : EnumFacing.values()) {
            BlockPos side = pos.offset(dir);
            if (connectsTo(world.getBlockState(side)) || connectsToEnergyDevice(world, side, dir)) {
                mask |= 1 << dir.getIndex();
            }
        }
        return mask;
    }

    /**
     * 邻居是否为可接驳的能量设备：暴露 Forge Energy 能力，或（装了 IC2 时）实现
     * IC2 电网接口。第三方机器不在 {@link #connectsTo} 白名单里，但能量上能接驳，
     * 视觉上同样伸出接入臂——否则就是"电通了臂不伸"。
     */
    private static boolean connectsToEnergyDevice(IBlockAccess world, BlockPos side, EnumFacing dir) {
        TileEntity te = world.getTileEntity(side);
        if (te == null) {
            return false;
        }
        // Ic2Compat.LOADED 短路：IC2 缺席时不会解析 ic2.* 类型
        if (Ic2Compat.LOADED && (te instanceof IEnergySink || te instanceof IEnergySource
                || te instanceof IEnergyConductor)) {
            return true;
        }
        return te.hasCapability(CapabilityEnergy.ENERGY, dir.getOpposite());
    }

    /** 邻居方块实体变化时刷新渲染，接入臂才能在放置/拆除机器时实时伸出或收回。 */
    @Override
    public void onNeighborChange(IBlockAccess world, BlockPos pos, BlockPos neighbor) {
        if (world instanceof World && ((World) world).isRemote) {
            ((World) world).markBlockRangeForRenderUpdate(pos, pos);
        }
    }

    @Override
    public IBlockState getExtendedState(IBlockState state, IBlockAccess world, BlockPos pos) {
        return ((IExtendedBlockState) state).withProperty(CONNECTIONS, maskFor(world, pos));
    }

    @Override
    public AxisAlignedBB getBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
        int mask = maskFor(source, pos);
        AxisAlignedBB box = CORE;
        if ((mask & (1 << EnumFacing.NORTH.getIndex())) != 0) box = box.union(ARM_NORTH);
        if ((mask & (1 << EnumFacing.SOUTH.getIndex())) != 0) box = box.union(ARM_SOUTH);
        if ((mask & (1 << EnumFacing.WEST.getIndex())) != 0) box = box.union(ARM_WEST);
        if ((mask & (1 << EnumFacing.EAST.getIndex())) != 0) box = box.union(ARM_EAST);
        if ((mask & (1 << EnumFacing.UP.getIndex())) != 0) box = box.union(ARM_UP);
        if ((mask & (1 << EnumFacing.DOWN.getIndex())) != 0) box = box.union(ARM_DOWN);
        return box;
    }

    @Override
    protected BlockStateContainer createBlockState() {
        return new ExtendedBlockState(this, new net.minecraft.block.properties.IProperty[0],
                new IUnlistedProperty[]{CONNECTIONS});
    }

    @Override
    public boolean isOpaqueCube(IBlockState state) {
        return false;
    }

    @Override
    public boolean isFullCube(IBlockState state) {
        return false;
    }

    @Override
    public EnumBlockRenderType getRenderType(IBlockState state) {
        return EnumBlockRenderType.MODEL;
    }

    /** 贴图带透明镂空（玻璃质感），走 CUTOUT 层做 alpha 测试。 */
    @SideOnly(Side.CLIENT)
    @Override
    public BlockRenderLayer getBlockLayer() {
        return BlockRenderLayer.CUTOUT;
    }

    @Nullable
    @Override
    public TileEntity createNewTileEntity(World world, int meta) {
        return new GlassCableTile();
    }
}
