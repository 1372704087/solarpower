package com.example.solarpower.inventory;

import com.example.solarpower.tileentity.StorageBoxTile;

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
 * 储电盒界面容器：无机器槽位，只同步「蓄电量」显示分量。
 * <p>存量是 BigInteger（可远超 double/窗口属性上限），同步「首 4 位有效数字 +
 * 十进制指数」两个 int，客户端用 {@link BigDecimal} 精确重建（同太阳能板容器）。
 */
public class StorageBoxContainer extends Container {

    public static final int ID_STORED_MANTISSA = 0;
    public static final int ID_STORED_EXP = 1;

    private final StorageBoxTile tile;

    // 服务端上次推送
    private int lastStoredMantissa = -1;
    private int lastStoredExp = -1;

    /** 客户端镜像（约 4 位有效数字；服务端权威数值仍是精确的 BigInteger）。 */
    public BigDecimal stored = BigDecimal.ZERO;

    // 客户端拼装用的原始分量
    private int storedMantissa;
    private int storedExp;

    public StorageBoxContainer(InventoryPlayer playerInv, StorageBoxTile tile) {
        this.tile = tile;
        // 玩家背包 3×9 + 热栏（坐标与 textures/gui/storage_box.png 的槽位严格一致）
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlotToContainer(new Slot(playerInv, col + row * 9 + 9,
                        8 + col * 18, 84 + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            this.addSlotToContainer(new Slot(playerInv, col, 8 + col * 18, 142));
        }
    }

    public StorageBoxTile getTile() {
        return this.tile;
    }

    @Override
    public void detectAndSendChanges() {
        super.detectAndSendChanges();
        int[] parts = this.tile.getStoredDisplayParts();
        for (IContainerListener listener : this.listeners) {
            if (this.lastStoredMantissa != parts[0] || this.lastStoredExp != parts[1]) {
                listener.sendWindowProperty(this, ID_STORED_MANTISSA, parts[0]);
                listener.sendWindowProperty(this, ID_STORED_EXP, parts[1]);
            }
        }
        this.lastStoredMantissa = parts[0];
        this.lastStoredExp = parts[1];
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void updateProgressBar(int id, int value) {
        if (id == ID_STORED_MANTISSA) {
            this.storedMantissa = value;
            this.stored = rebuild(this.storedMantissa, this.storedExp);
        } else if (id == ID_STORED_EXP) {
            this.storedExp = value;
            this.stored = rebuild(this.storedMantissa, this.storedExp);
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
