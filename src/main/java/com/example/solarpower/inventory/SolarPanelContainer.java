package com.example.solarpower.inventory;

import com.example.solarpower.energy.EuFormat;
import com.example.solarpower.tileentity.SolarPanelTile;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.IContainerListener;

import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.math.BigDecimal;
import java.math.BigInteger;

/**
 * 太阳能板界面容器：无物品槽，同步「蓄电量」/「发电量」/「昼夜」。
 * <p>蓄电量与发电量是 BigInteger（可远超 double 上限），窗口属性只能传 short，
 * 因此同步「首 4 位有效数字 + 十进制指数」两个 int，客户端用 {@link BigDecimal}
 * 精确重建（约 4 位有效数字，足够界面显示与能量管比例）；服务端权威数值仍是精确的 BigInteger。
 */
public class SolarPanelContainer extends Container {

    public static final int ID_STORED_MANTISSA = 0;
    public static final int ID_STORED_EXP = 1;
    public static final int ID_GEN_MANTISSA = 2;
    public static final int ID_GEN_EXP = 3;
    public static final int ID_NIGHT = 4;

    private final SolarPanelTile tile;

    // 服务端上次推送
    private int lastStoredMantissa = -1;
    private int lastStoredExp = -1;
    private int lastGenMantissa = -1;
    private int lastGenExp = -1;
    private int lastNight = -1;

    /** 客户端镜像（由首 4 位有效数字精确重建，仅约 4 位有效数字；服务端仅用于显示）。 */
    public BigDecimal stored = BigDecimal.ZERO;
    public BigDecimal generating = BigDecimal.ZERO;
    public boolean night;

    // 客户端拼装用的原始分量
    private int storedMantissa;
    private int storedExp;
    private int genMantissa;
    private int genExp;

    public SolarPanelContainer(SolarPanelTile tile) {
        this.tile = tile;
    }

    public SolarPanelTile getTile() {
        return this.tile;
    }

    @Override
    public void detectAndSendChanges() {
        super.detectAndSendChanges();
        int[] storedParts = this.tile.getStoredDisplayParts();
        int[] genParts = this.tile.getGeneratingDisplayParts();
        int night = this.tile.isNight() ? 1 : 0;
        for (IContainerListener listener : this.listeners) {
            if (this.lastStoredMantissa != storedParts[0] || this.lastStoredExp != storedParts[1]) {
                listener.sendWindowProperty(this, ID_STORED_MANTISSA, storedParts[0]);
                listener.sendWindowProperty(this, ID_STORED_EXP, storedParts[1]);
            }
            if (this.lastGenMantissa != genParts[0] || this.lastGenExp != genParts[1]) {
                listener.sendWindowProperty(this, ID_GEN_MANTISSA, genParts[0]);
                listener.sendWindowProperty(this, ID_GEN_EXP, genParts[1]);
            }
            if (this.lastNight != night) {
                listener.sendWindowProperty(this, ID_NIGHT, night);
            }
        }
        this.lastStoredMantissa = storedParts[0];
        this.lastStoredExp = storedParts[1];
        this.lastGenMantissa = genParts[0];
        this.lastGenExp = genParts[1];
        this.lastNight = night;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void updateProgressBar(int id, int value) {
        switch (id) {
            case ID_STORED_MANTISSA:
                this.storedMantissa = value;
                this.stored = rebuild(this.storedMantissa, this.storedExp);
                break;
            case ID_STORED_EXP:
                this.storedExp = value;
                this.stored = rebuild(this.storedMantissa, this.storedExp);
                break;
            case ID_GEN_MANTISSA:
                this.genMantissa = value;
                this.generating = rebuild(this.genMantissa, this.genExp);
                break;
            case ID_GEN_EXP:
                this.genExp = value;
                this.generating = rebuild(this.genMantissa, this.genExp);
                break;
            case ID_NIGHT:
                this.night = value != 0;
                break;
            default:
                break;
        }
    }

    /** 由「首 4 位有效数字 + 十进制指数」精确重建显示值。 */
    private static BigDecimal rebuild(int mantissa, int exponent) {
        if (mantissa <= 0) {
            return BigDecimal.ZERO;
        }
        int digits = (int) Math.log10(mantissa) + 1;
        return BigDecimal.valueOf(mantissa).scaleByPowerOfTen(exponent - (digits - 1));
    }

    @Override
    public boolean canInteractWith(EntityPlayer player) {
        return !this.tile.isInvalid()
                && player.getDistanceSq(this.tile.getPos().getX() + 0.5D, this.tile.getPos().getY() + 0.5D,
                        this.tile.getPos().getZ() + 0.5D) <= 64.0D;
    }
}