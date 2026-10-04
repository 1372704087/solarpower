package com.example.solarpower.inventory;

import com.example.solarpower.tileentity.TransformerTile;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.IContainerListener;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;

import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.math.BigDecimal;

import javax.annotation.Nonnull;

/**
 * 变压器界面容器：无机器槽位，同步「低压池/高压池」显示分量。
 * <p>池存量是 BigInteger，同步「首 4 位有效数字 + 十进制指数」各两个 int
 * （同太阳能板/储电盒容器）。
 */
public class TransformerContainer extends Container {

    public static final int ID_LOW_MANTISSA = 0;
    public static final int ID_LOW_EXP = 1;
    public static final int ID_HIGH_MANTISSA = 2;
    public static final int ID_HIGH_EXP = 3;

    private final TransformerTile tile;

    // 服务端上次推送
    private int lastLowMantissa = -1;
    private int lastLowExp = -1;
    private int lastHighMantissa = -1;
    private int lastHighExp = -1;

    /** 客户端镜像（约 4 位有效数字；服务端权威数值仍是精确的 BigInteger）。 */
    public BigDecimal lowStored = BigDecimal.ZERO;
    public BigDecimal highStored = BigDecimal.ZERO;

    // 客户端拼装用的原始分量
    private int lowMantissa;
    private int lowExp;
    private int highMantissa;
    private int highExp;

    public TransformerContainer(InventoryPlayer playerInv, TransformerTile tile) {
        this.tile = tile;
        // 玩家背包 3×9 + 热栏（坐标与 textures/gui/transformer.png 的槽位严格一致，ySize=190）
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlotToContainer(new Slot(playerInv, col + row * 9 + 9,
                        8 + col * 18, 113 + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            this.addSlotToContainer(new Slot(playerInv, col, 8 + col * 18, 165));
        }
    }

    public TransformerTile getTile() {
        return this.tile;
    }

    @Override
    public void detectAndSendChanges() {
        super.detectAndSendChanges();
        int[] lowParts = this.tile.getLowDisplayParts();
        int[] highParts = this.tile.getHighDisplayParts();
        for (IContainerListener listener : this.listeners) {
            if (this.lastLowMantissa != lowParts[0] || this.lastLowExp != lowParts[1]) {
                listener.sendWindowProperty(this, ID_LOW_MANTISSA, lowParts[0]);
                listener.sendWindowProperty(this, ID_LOW_EXP, lowParts[1]);
            }
            if (this.lastHighMantissa != highParts[0] || this.lastHighExp != highParts[1]) {
                listener.sendWindowProperty(this, ID_HIGH_MANTISSA, highParts[0]);
                listener.sendWindowProperty(this, ID_HIGH_EXP, highParts[1]);
            }
        }
        this.lastLowMantissa = lowParts[0];
        this.lastLowExp = lowParts[1];
        this.lastHighMantissa = highParts[0];
        this.lastHighExp = highParts[1];
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void updateProgressBar(int id, int value) {
        switch (id) {
            case ID_LOW_MANTISSA:
                this.lowMantissa = value;
                this.lowStored = rebuild(this.lowMantissa, this.lowExp);
                break;
            case ID_LOW_EXP:
                this.lowExp = value;
                this.lowStored = rebuild(this.lowMantissa, this.lowExp);
                break;
            case ID_HIGH_MANTISSA:
                this.highMantissa = value;
                this.highStored = rebuild(this.highMantissa, this.highExp);
                break;
            case ID_HIGH_EXP:
                this.highExp = value;
                this.highStored = rebuild(this.highMantissa, this.highExp);
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

    /** 没有机器槽位，shift-click 不转移。 */
    @Nonnull
    @Override
    public ItemStack transferStackInSlot(EntityPlayer player, int index) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean canInteractWith(EntityPlayer player) {
        return !this.tile.isInvalid()
                && player.getDistanceSq(this.tile.getPos().getX() + 0.5D, this.tile.getPos().getY() + 0.5D,
                        this.tile.getPos().getZ() + 0.5D) <= 64.0D;
    }
}
