package com.example.solarpower.block;

import com.example.solarpower.SolarPower;
import com.example.solarpower.client.GuiHandler;
import com.example.solarpower.solar.SolarTier;
import com.example.solarpower.tileentity.SolarPanelTile;

import net.minecraft.block.BlockContainer;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumBlockRenderType;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import javax.annotation.Nullable;

/** 太阳能板方块（1.12.2）。每个档位一个实例，数值由 {@link SolarTier} 决定。 */
public class SolarPanelBlock extends BlockContainer {

    private final SolarTier tier;

    public SolarPanelBlock(SolarTier tier) {
        super(Material.IRON);
        this.tier = tier;
        setUnlocalizedName(SolarPower.MODID + "." + tier.id());
        setHardness(2.0F);
        setSoundType(SoundType.METAL);
        setCreativeTab(SolarPower.CREATIVE_TAB);
    }

    public SolarTier tier() {
        return this.tier;
    }

    /** BlockContainer 默认用 TESR 渲染（INVISIBLE），面板走方块模型。 */
    @Override
    public EnumBlockRenderType getRenderType(IBlockState state) {
        return EnumBlockRenderType.MODEL;
    }

    @Override
    public boolean hasTileEntity(IBlockState state) {
        return true;
    }

    @Override
    public boolean onBlockActivated(World worldIn, BlockPos pos, IBlockState state,
                                    EntityPlayer playerIn, EnumHand hand, EnumFacing facing,
                                    float hitX, float hitY, float hitZ) {
        if (worldIn.isRemote) {
            return true;
        }
        TileEntity te = worldIn.getTileEntity(pos);
        if (te instanceof SolarPanelTile) {
            playerIn.openGui(SolarPower.instance, GuiHandler.GUI_SOLAR_PANEL,
                    worldIn, pos.getX(), pos.getY(), pos.getZ());
            return true;
        }
        return false;
    }

    @Nullable
    @Override
    public TileEntity createNewTileEntity(World world, int meta) {
        return new SolarPanelTile();
    }
}
