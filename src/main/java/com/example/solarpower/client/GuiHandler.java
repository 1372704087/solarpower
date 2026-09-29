package com.example.solarpower.client;

import com.example.solarpower.SolarPower;
import com.example.solarpower.inventory.SolarPanelContainer;
import com.example.solarpower.tileentity.SolarPanelTile;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.network.IGuiHandler;

import javax.annotation.Nullable;

/** GUI 处理器：服务端返回容器，客户端返回界面。 */
public class GuiHandler implements IGuiHandler {

    public static final int GUI_SOLAR_PANEL = 0;

    @Nullable
    @Override
    public Object getServerGuiElement(int id, EntityPlayer player, World world, int x, int y, int z) {
        if (id == GUI_SOLAR_PANEL
                && world.getTileEntity(new BlockPos(x, y, z)) instanceof SolarPanelTile) {
            return new SolarPanelContainer((SolarPanelTile) world.getTileEntity(new BlockPos(x, y, z)));
        }
        return null;
    }

    @Nullable
    @Override
    public Object getClientGuiElement(int id, EntityPlayer player, World world, int x, int y, int z) {
        if (id == GUI_SOLAR_PANEL
                && world.getTileEntity(new BlockPos(x, y, z)) instanceof SolarPanelTile) {
            return new GuiSolarPanel(new SolarPanelContainer(
                    (SolarPanelTile) world.getTileEntity(new BlockPos(x, y, z))));
        }
        return null;
    }
}
