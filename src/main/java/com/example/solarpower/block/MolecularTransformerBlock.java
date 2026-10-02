package com.example.solarpower.block;

import com.example.solarpower.SolarPower;
import com.example.solarpower.client.GuiHandler;
import com.example.solarpower.tileentity.MolecularTransformerTile;

import net.minecraft.block.BlockContainer;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.InventoryHelper;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumBlockRenderType;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import javax.annotation.Nullable;

/**
 * 分子重组仪方块（1.12.2）。机制照搬 Advanced Solar Panels：
 * 接受 EU 注入，把输入物品按配方总量转换成产出；静态 JSON 模型渲染（同 IU，
 * active 两态共用同一模型），
 * 比较器输出合成进度（0~15）。
 */
public class MolecularTransformerBlock extends BlockContainer {

    public static final PropertyBool ACTIVE = PropertyBool.create("active");

    public MolecularTransformerBlock() {
        super(Material.IRON);
        setUnlocalizedName(SolarPower.MODID + ".molecular_transformer");
        setHardness(3.0F);
        setSoundType(SoundType.METAL);
        setCreativeTab(SolarPower.CREATIVE_TAB);
        setDefaultState(this.blockState.getBaseState().withProperty(ACTIVE, false));
    }

    @Override
    public EnumBlockRenderType getRenderType(IBlockState state) {
        return EnumBlockRenderType.MODEL;
    }

    /** 模型为悬空小件组合、未填满整格：必须声明非不透明，否则相邻方块朝向本机的面
     *  会被面剔除当成“被遮住”而消失，形成透视空洞（玻璃电缆同款覆写）。 */
    @Override
    public boolean isOpaqueCube(IBlockState state) {
        return false;
    }

    @Override
    public boolean isFullCube(IBlockState state) {
        return false;
    }

    @Override
    public boolean hasTileEntity(IBlockState state) {
        return true;
    }

    @Nullable
    @Override
    public TileEntity createNewTileEntity(World world, int meta) {
        return new MolecularTransformerTile();
    }

    @Override
    public boolean onBlockActivated(World worldIn, BlockPos pos, IBlockState state,
                                    EntityPlayer playerIn, EnumHand hand, EnumFacing facing,
                                    float hitX, float hitY, float hitZ) {
        if (worldIn.isRemote) {
            return true;
        }
        TileEntity te = worldIn.getTileEntity(pos);
        if (te instanceof MolecularTransformerTile) {
            playerIn.openGui(SolarPower.instance, GuiHandler.GUI_MOLECULAR_TRANSFORMER,
                    worldIn, pos.getX(), pos.getY(), pos.getZ());
            return true;
        }
        return false;
    }

    @Override
    public void breakBlock(World world, BlockPos pos, IBlockState state) {
        TileEntity te = world.getTileEntity(pos);
        if (te instanceof MolecularTransformerTile) {
            for (ItemStack stack : ((MolecularTransformerTile) te).dropContents()) {
                InventoryHelper.spawnItemStack(world, pos.getX(), pos.getY(), pos.getZ(), stack);
            }
            world.updateComparatorOutputLevel(pos, this);
        }
        super.breakBlock(world, pos, state);
    }

    // ------------------------------------------------------------------
    // active 状态（保留状态位，两态共用同一模型，同 IU）
    // ------------------------------------------------------------------

    @Override
    public IBlockState getStateFromMeta(int meta) {
        return this.getDefaultState().withProperty(ACTIVE, meta == 1);
    }

    @Override
    public int getMetaFromState(IBlockState state) {
        return state.getValue(ACTIVE) ? 1 : 0;
    }

    @Override
    protected BlockStateContainer createBlockState() {
        return new BlockStateContainer(this, ACTIVE);
    }

    // ------------------------------------------------------------------
    // 比较器：输出进度 0~15（机制照搬 ASP）
    // ------------------------------------------------------------------

    @Override
    public boolean hasComparatorInputOverride(IBlockState state) {
        return true;
    }

    @Override
    public int getComparatorInputOverride(IBlockState state, World world, BlockPos pos) {
        TileEntity te = world.getTileEntity(pos);
        return te instanceof MolecularTransformerTile
                ? ((MolecularTransformerTile) te).comparatorLevel() : 0;
    }
}
