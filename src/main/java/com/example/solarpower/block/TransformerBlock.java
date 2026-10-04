package com.example.solarpower.block;

import com.example.solarpower.SolarPower;
import com.example.solarpower.energy.EuTier;
import com.example.solarpower.tileentity.TransformerTile;

import net.minecraft.block.BlockContainer;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyDirection;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumBlockRenderType;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import javax.annotation.Nullable;

/**
 * 变压器方块（1.12.2，65 档，桥接电压 T↔T+1）。
 * 带水平 {@link #FACING}：正面（能量芯）= 高压侧（T+1），其余五面 = 低压侧（T）。
 * 放置时正面朝向玩家。转换逻辑全在 {@link TransformerTile}；无 GUI、无模式开关
 * （IC2 语义：按进电方向自动升/降压）。
 */
public class TransformerBlock extends BlockContainer {

    public static final PropertyDirection FACING =
            PropertyDirection.create("facing", EnumFacing.Plane.HORIZONTAL);

    private final EuTier tier;

    public TransformerBlock(EuTier tier) {
        super(Material.IRON);
        this.tier = tier;
        setUnlocalizedName(SolarPower.MODID + "." + tier.name().toLowerCase() + "_transformer");
        setHardness(3.0F);
        setSoundType(SoundType.METAL);
        setCreativeTab(SolarPower.CREATIVE_TAB);
        setDefaultState(this.blockState.getBaseState().withProperty(FACING, EnumFacing.NORTH));
    }

    /** 低压侧档位（桥接 tier ↔ tier+1）。 */
    public EuTier tier() {
        return this.tier;
    }

    @Override
    public EnumBlockRenderType getRenderType(IBlockState state) {
        return EnumBlockRenderType.MODEL;
    }

    @Override
    public boolean hasTileEntity(IBlockState state) {
        return true;
    }

    @Nullable
    @Override
    public TileEntity createNewTileEntity(World world, int meta) {
        return new TransformerTile();
    }

    /** 放置时正面（高压侧）朝向玩家。 */
    @Override
    public IBlockState getStateForPlacement(World world, BlockPos pos, EnumFacing facing,
                                            float hitX, float hitY, float hitZ, int meta,
                                            EntityLivingBase placer, EnumHand hand) {
        return this.getDefaultState().withProperty(FACING,
                placer.getHorizontalFacing().getOpposite());
    }

    @Override
    public IBlockState getStateFromMeta(int meta) {
        return this.getDefaultState().withProperty(FACING, EnumFacing.getHorizontal(meta & 3));
    }

    @Override
    public int getMetaFromState(IBlockState state) {
        return state.getValue(FACING).getHorizontalIndex();
    }

    @Override
    protected BlockStateContainer createBlockState() {
        return new BlockStateContainer(this, FACING);
    }

    // ------------------------------------------------------------------
    // 比较器：两池合计占用比例 0~15
    // ------------------------------------------------------------------

    @Override
    public boolean hasComparatorInputOverride(IBlockState state) {
        return true;
    }

    @Override
    public int getComparatorInputOverride(IBlockState state, World world, BlockPos pos) {
        TileEntity te = world.getTileEntity(pos);
        return te instanceof TransformerTile ? ((TransformerTile) te).comparatorLevel() : 0;
    }
}
