package com.example.solarpower.block;

import com.example.solarpower.SolarPower;
import com.example.solarpower.client.GuiHandler;
import com.example.solarpower.solar.SolarTier;
import com.example.solarpower.tileentity.StorageBoxTile;

import net.minecraft.block.BlockContainer;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyDirection;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumBlockRenderType;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.NonNullList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

import java.math.BigInteger;
import java.util.Base64;

import javax.annotation.Nullable;

/**
 * 储电盒方块（1.12.2）。每个档位一个实例，数值由 {@link SolarTier} 决定。
 * <p>带水平 {@link #FACING}：放置时正面（能量芯/输出面）朝向玩家，翻面只改
 * 状态不重建 TE（{@code StorageBoxTile#shouldRefresh}）。
 * 比较器按存量比例输出 0~15；拆机掉落物保留电量（放置时读回）。
 */
public class StorageBoxBlock extends BlockContainer {

    public static final PropertyDirection FACING =
            PropertyDirection.create("facing", EnumFacing.Plane.HORIZONTAL);

    private final SolarTier tier;

    public StorageBoxBlock(SolarTier tier) {
        super(Material.IRON);
        this.tier = tier;
        setUnlocalizedName(SolarPower.MODID + "." + tier.id() + "_storage_box");
        setHardness(3.0F);
        setSoundType(SoundType.METAL);
        setCreativeTab(SolarPower.CREATIVE_TAB);
        setDefaultState(this.blockState.getBaseState().withProperty(FACING, EnumFacing.NORTH));
    }

    public SolarTier tier() {
        return this.tier;
    }

    /** BlockContainer 默认用 TESR 渲染（INVISIBLE），储电盒走方块模型。 */
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
        return new StorageBoxTile();
    }

    /** 放置时正面朝向玩家（= 玩家水平朝向的反方向）。 */
    @Override
    public IBlockState getStateForPlacement(World world, BlockPos pos, EnumFacing facing,
                                            float hitX, float hitY, float hitZ, int meta,
                                            EntityLivingBase placer, EnumHand hand) {
        return this.getDefaultState().withProperty(FACING,
                placer.getHorizontalFacing().getOpposite());
    }

    @Override
    public void onBlockPlacedBy(World world, BlockPos pos, IBlockState state,
                                EntityLivingBase placer, ItemStack stack) {
        TileEntity te = world.getTileEntity(pos);
        if (te instanceof StorageBoxTile && stack.hasTagCompound()
                && stack.getTagCompound().hasKey(StorageBoxTile.TAG_ENERGY_B64)) {
            try {
                ((StorageBoxTile) te).restoreEnergy(new BigInteger(Base64.getDecoder()
                        .decode(stack.getTagCompound().getString(StorageBoxTile.TAG_ENERGY_B64))));
            } catch (NumberFormatException ignored) {
                // 坏档按空盒处理
            }
        }
    }

    /** 拆机掉落物带电量 NBT（再次放置时由 {@link #onBlockPlacedBy} 读回）。 */
    @Override
    public void getDrops(NonNullList<ItemStack> drops, IBlockAccess world,
                         BlockPos pos, IBlockState state, int fortune) {
        ItemStack stack = new ItemStack(Item.getItemFromBlock(this), 1);
        TileEntity te = world.getTileEntity(pos);
        if (te instanceof StorageBoxTile) {
            BigInteger stored = ((StorageBoxTile) te).getStoredEu();
            if (stored.signum() > 0) {
                NBTTagCompound tag = new NBTTagCompound();
                tag.setString(StorageBoxTile.TAG_ENERGY_B64,
                        Base64.getEncoder().encodeToString(stored.toByteArray()));
                stack.setTagCompound(tag);
            }
        }
        drops.add(stack);
    }

    @Override
    public boolean onBlockActivated(World worldIn, BlockPos pos, IBlockState state,
                                    EntityPlayer playerIn, EnumHand hand, EnumFacing facing,
                                    float hitX, float hitY, float hitZ) {
        if (worldIn.isRemote) {
            return true;
        }
        TileEntity te = worldIn.getTileEntity(pos);
        if (te instanceof StorageBoxTile) {
            playerIn.openGui(SolarPower.instance, GuiHandler.GUI_STORAGE_BOX,
                    worldIn, pos.getX(), pos.getY(), pos.getZ());
            return true;
        }
        return false;
    }

    @Override
    public void breakBlock(World world, BlockPos pos, IBlockState state) {
        world.updateComparatorOutputLevel(pos, this);
        super.breakBlock(world, pos, state);
    }

    // ------------------------------------------------------------------
    // facing 状态
    // ------------------------------------------------------------------

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
    // 比较器：输出存量比例 0~15
    // ------------------------------------------------------------------

    @Override
    public boolean hasComparatorInputOverride(IBlockState state) {
        return true;
    }

    @Override
    public int getComparatorInputOverride(IBlockState state, World world, BlockPos pos) {
        TileEntity te = world.getTileEntity(pos);
        return te instanceof StorageBoxTile ? ((StorageBoxTile) te).comparatorLevel() : 0;
    }
}
