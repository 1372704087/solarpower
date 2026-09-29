package com.example.solarpower.block;

import com.example.solarpower.SolarPower;
import com.example.solarpower.solar.GlassCableTier;
import com.example.solarpower.tileentity.GlassCableTile;

import net.minecraft.block.BlockContainer;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumBlockRenderType;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

import net.minecraftforge.common.property.ExtendedBlockState;
import net.minecraftforge.common.property.IExtendedBlockState;
import net.minecraftforge.common.property.IUnlistedProperty;

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
            if (connectsTo(world.getBlockState(pos.offset(dir)))) {
                mask |= 1 << dir.getIndex();
            }
        }
        return mask;
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

    @Nullable
    @Override
    public TileEntity createNewTileEntity(World world, int meta) {
        return new GlassCableTile();
    }
}
