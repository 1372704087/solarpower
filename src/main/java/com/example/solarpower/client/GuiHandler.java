package com.example.solarpower.client;

import com.example.solarpower.inventory.MolecularTransformerContainer;
import com.example.solarpower.inventory.SolarPanelContainer;
import com.example.solarpower.inventory.StorageBoxContainer;
import com.example.solarpower.inventory.TransformerContainer;
import com.example.solarpower.tileentity.MolecularTransformerTile;
import com.example.solarpower.tileentity.SolarPanelTile;
import com.example.solarpower.tileentity.StorageBoxTile;
import com.example.solarpower.tileentity.TransformerTile;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.network.IGuiHandler;

import javax.annotation.Nullable;

/** GUI 处理器：服务端返回容器，客户端返回界面。 */
public class GuiHandler implements IGuiHandler {

    public static final int GUI_SOLAR_PANEL = 0;
    public static final int GUI_MOLECULAR_TRANSFORMER = 1;
    public static final int GUI_STORAGE_BOX = 2;
    public static final int GUI_TRANSFORMER = 3;

    @Nullable
    @Override
    public Object getServerGuiElement(int id, EntityPlayer player, World world, int x, int y, int z) {
        BlockPos pos = new BlockPos(x, y, z);
        if (id == GUI_SOLAR_PANEL
                && world.getTileEntity(pos) instanceof SolarPanelTile) {
            return new SolarPanelContainer((SolarPanelTile) world.getTileEntity(pos));
        }
        if (id == GUI_MOLECULAR_TRANSFORMER
                && world.getTileEntity(pos) instanceof MolecularTransformerTile) {
            return new MolecularTransformerContainer(player.inventory,
                    (MolecularTransformerTile) world.getTileEntity(pos));
        }
        if (id == GUI_STORAGE_BOX
                && world.getTileEntity(pos) instanceof StorageBoxTile) {
            return new StorageBoxContainer(player.inventory,
                    (StorageBoxTile) world.getTileEntity(pos));
        }
        if (id == GUI_TRANSFORMER
                && world.getTileEntity(pos) instanceof TransformerTile) {
            return new TransformerContainer(player.inventory,
                    (TransformerTile) world.getTileEntity(pos));
        }
        return null;
    }

    @Nullable
    @Override
    public Object getClientGuiElement(int id, EntityPlayer player, World world, int x, int y, int z) {
        BlockPos pos = new BlockPos(x, y, z);
        if (id == GUI_SOLAR_PANEL
                && world.getTileEntity(pos) instanceof SolarPanelTile) {
            return new GuiSolarPanel(new SolarPanelContainer((SolarPanelTile) world.getTileEntity(pos)));
        }
        if (id == GUI_MOLECULAR_TRANSFORMER
                && world.getTileEntity(pos) instanceof MolecularTransformerTile) {
            return new GuiMolecularTransformer(new MolecularTransformerContainer(player.inventory,
                    (MolecularTransformerTile) world.getTileEntity(pos)));
        }
        if (id == GUI_STORAGE_BOX
                && world.getTileEntity(pos) instanceof StorageBoxTile) {
            return new GuiStorageBox(new StorageBoxContainer(player.inventory,
                    (StorageBoxTile) world.getTileEntity(pos)));
        }
        if (id == GUI_TRANSFORMER
                && world.getTileEntity(pos) instanceof TransformerTile) {
            return new GuiTransformer(new TransformerContainer(player.inventory,
                    (TransformerTile) world.getTileEntity(pos)));
        }
        return null;
    }
}
