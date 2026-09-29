package com.example.solarpower.inventory;

import com.example.solarpower.tileentity.SolarPanelTile;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.IContainerListener;

import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/** 太阳能板界面容器：无物品槽，同步「蓄电量」（高低 32 位）/「发电量」/「昼夜」。 */
public class SolarPanelContainer extends Container {

    public static final int ID_STORED_LO = 0;
    public static final int ID_STORED_HI = 1;
    public static final int ID_GENERATING = 2;
    public static final int ID_NIGHT = 3;

    private final SolarPanelTile tile;

    // 服务端上次推送 / 客户端当前值
    private long lastStored = -1L;
    private long lastGenerating = -1L;
    private int lastNight = -1;

    /** 客户端镜像（服务端也可读，仅用于显示）。 */
    public long stored;
    public long generating;
    public boolean night;

    public SolarPanelContainer(SolarPanelTile tile) {
        this.tile = tile;
    }

    public SolarPanelTile getTile() {
        return this.tile;
    }

    @Override
    public void detectAndSendChanges() {
        super.detectAndSendChanges();
        long stored = this.tile.getEnergy().getStoredEu();
        long generating = this.tile.getGenerating();
        int night = this.tile.isNight() ? 1 : 0;
        for (IContainerListener crafting : this.listeners) {
            if (this.lastStored != stored) {
                crafting.sendWindowProperty(this, ID_STORED_LO, (int) (stored & 0xFFFF_FFFFL));
                crafting.sendWindowProperty(this, ID_STORED_HI, (int) (stored >>> 32));
            }
            if (this.lastGenerating != generating) {
                crafting.sendWindowProperty(this, ID_GENERATING, (int) generating);
            }
            if (this.lastNight != night) {
                crafting.sendWindowProperty(this, ID_NIGHT, night);
            }
        }
        this.lastStored = stored;
        this.lastGenerating = generating;
        this.lastNight = night;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void updateProgressBar(int id, int value) {
        switch (id) {
            case ID_STORED_LO:
                this.stored = (this.stored & ~0xFFFF_FFFFL) | (value & 0xFFFF_FFFFL);
                break;
            case ID_STORED_HI:
                this.stored = (this.stored & 0xFFFF_FFFFL) | ((long) value << 32);
                break;
            case ID_GENERATING:
                this.generating = value;
                break;
            case ID_NIGHT:
                this.night = value != 0;
                break;
        }
    }

    @Override
    public boolean canInteractWith(EntityPlayer player) {
        return !this.tile.isInvalid()
                && player.getDistanceSq(this.tile.getPos().getX() + 0.5D, this.tile.getPos().getY() + 0.5D,
                        this.tile.getPos().getZ() + 0.5D) <= 64.0D;
    }
}
