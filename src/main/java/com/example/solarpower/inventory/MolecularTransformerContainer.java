package com.example.solarpower.inventory;

import com.example.solarpower.solar.MtRecipes;
import com.example.solarpower.tileentity.MolecularTransformerTile;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.IContainerListener;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;

import net.minecraftforge.items.SlotItemHandler;

/**
 * 分子重组机界面容器：输入/输出槽 + 背包，同步「缓存电量 / 配方总 EU / 已注入 EU」。
 * <p>窗口属性只能传 short，因此各数值拆成低/高 16 位两个属性发送（数值上限
 * 2.5 亿 EU，两个 16 位足够）；客户端重建后仅供显示，服务端数值是权威值。
 */
public class MolecularTransformerContainer extends Container {

    public static final int ID_ENERGY_LO = 0;
    public static final int ID_ENERGY_HI = 1;
    public static final int ID_TOTAL_LO = 2;
    public static final int ID_TOTAL_HI = 3;
    public static final int ID_USED_LO = 4;
    public static final int ID_USED_HI = 5;
    public static final int ID_WORKING = 6;

    /** 机器槽位数量（输入 + 输出），玩家背包槽位从该下标开始。 */
    public static final int MACHINE_SLOTS = 2;
    /** 全部槽位数量（机器 2 + 背包 27 + 快捷栏 9）。 */
    public static final int TOTAL_SLOTS = 38;

    private final MolecularTransformerTile tile;

    // 服务端上次推送
    private int lastEnergyLo = -1;
    private int lastEnergyHi = -1;
    private int lastTotalLo = -1;
    private int lastTotalHi = -1;
    private int lastUsedLo = -1;
    private int lastUsedHi = -1;
    private int lastWorking = -1;

    // 客户端镜像（仅供显示）。存电量上限 2^31，int 装不下满值，用 long 承载
    private long energy;
    private int total;
    private int used;
    private boolean working;

    public MolecularTransformerContainer(InventoryPlayer playerInv, MolecularTransformerTile tile) {
        this.tile = tile;
        // 槽位坐标 = SSP 底图实测网格（逐像素测得）：
        // 槽框边 x=17+c*21（框边 1px），内腔（物品显示区）x=18+c*21..33，16px；步长 21。
        // 物品格坐标 = 内腔左上：背包 (18+col*21, 98/119/140)，快捷栏 y=165（框顶边 164,内腔 165 起）；机器槽 (20,27)/(20,68)。
        // 注意：不是框角+1（框角 17 处是 1px 装饰边，内腔从 18 起）。
        this.addSlotToContainer(new SlotItemHandler(tile.getInput(), 0, 20, 27) {
            @Override
            public boolean isItemValid(ItemStack stack) {
                return MtRecipes.find(stack) != null;
            }
        });
        this.addSlotToContainer(new SlotItemHandler(tile.getOutput(), 0, 20, 68) {
            @Override
            public boolean isItemValid(ItemStack stack) {
                return false;
            }
        });
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlotToContainer(new Slot(playerInv, col + row * 9 + 9, 18 + col * 21, 98 + row * 21));
            }
        }
        for (int col = 0; col < 9; col++) {
            // 热栏框顶边在 164，内腔从 165 起
            this.addSlotToContainer(new Slot(playerInv, col, 18 + col * 21, 165));
        }
    }

    public MolecularTransformerTile getTile() {
        return this.tile;
    }

    /** 缓存电量（客户端镜像），上限 2^31。 */
    public long displayedEnergy() {
        return this.energy;
    }

    /** 缓存上限（EU）。 */
    public long displayedCapacity() {
        return MolecularTransformerTile.CAPACITY_EU.longValue();
    }

    /** 当前配方总 EU（客户端镜像）。 */
    public int displayedTotal() {
        return this.total;
    }

    /** 已注入 EU（客户端镜像）。 */
    public int displayedUsed() {
        return this.used;
    }

    public boolean displayedWorking() {
        return this.working;
    }

    @Override
    public void detectAndSendChanges() {
        super.detectAndSendChanges();
        long stored = this.tile.getEnergy().getStoredEu().longValueExact();
        int total = this.tile.getTotalEu();
        int used = this.tile.getEnergyUsed();
        int working = this.tile.isWorking() ? 1 : 0;
        for (IContainerListener listener : this.listeners) {
            if (this.lastEnergyLo != (int) (stored & 0xFFFF) || this.lastEnergyHi != (int) ((stored >>> 16) & 0xFFFF)) {
                listener.sendWindowProperty(this, ID_ENERGY_LO, (int) (stored & 0xFFFF));
                listener.sendWindowProperty(this, ID_ENERGY_HI, (int) ((stored >>> 16) & 0xFFFF));
            }
            if (this.lastTotalLo != (total & 0xFFFF) || this.lastTotalHi != ((total >>> 16) & 0xFFFF)) {
                listener.sendWindowProperty(this, ID_TOTAL_LO, total & 0xFFFF);
                listener.sendWindowProperty(this, ID_TOTAL_HI, (total >>> 16) & 0xFFFF);
            }
            if (this.lastUsedLo != (used & 0xFFFF) || this.lastUsedHi != ((used >>> 16) & 0xFFFF)) {
                listener.sendWindowProperty(this, ID_USED_LO, used & 0xFFFF);
                listener.sendWindowProperty(this, ID_USED_HI, (used >>> 16) & 0xFFFF);
            }
            if (this.lastWorking != working) {
                listener.sendWindowProperty(this, ID_WORKING, working);
            }
        }
        this.lastEnergyLo = (int) (stored & 0xFFFF);
        this.lastEnergyHi = (int) ((stored >>> 16) & 0xFFFF);
        this.lastTotalLo = total & 0xFFFF;
        this.lastTotalHi = (total >>> 16) & 0xFFFF;
        this.lastUsedLo = used & 0xFFFF;
        this.lastUsedHi = (used >>> 16) & 0xFFFF;
        this.lastWorking = working;
    }

    @Override
    public void updateProgressBar(int id, int value) {
        switch (id) {
            case ID_ENERGY_LO:
                this.energy = (this.energy & ~0xFFFFL) | (value & 0xFFFFL);
                break;
            case ID_ENERGY_HI:
                this.energy = (this.energy & 0xFFFFL) | ((value & 0xFFFFL) << 16);
                break;
            case ID_TOTAL_LO:
                this.total = (this.total & ~0xFFFF) | (value & 0xFFFF);
                break;
            case ID_TOTAL_HI:
                this.total = (this.total & 0xFFFF) | ((value & 0xFFFF) << 16);
                break;
            case ID_USED_LO:
                this.used = (this.used & ~0xFFFF) | (value & 0xFFFF);
                break;
            case ID_USED_HI:
                this.used = (this.used & 0xFFFF) | ((value & 0xFFFF) << 16);
                break;
            case ID_WORKING:
                this.working = value != 0;
                break;
            default:
                break;
        }
    }

    @Override
    public ItemStack transferStackInSlot(EntityPlayer player, int index) {
        ItemStack remaining = ItemStack.EMPTY;
        Slot slot = this.inventorySlots.get(index);
        if (slot != null && slot.getHasStack()) {
            ItemStack current = slot.getStack();
            remaining = current.copy();
            if (index < MACHINE_SLOTS) {
                // 机器槽 → 背包
                if (!this.mergeItemStack(current, MACHINE_SLOTS, TOTAL_SLOTS, true)) {
                    return ItemStack.EMPTY;
                }
            } else if (MtRecipes.find(current) != null) {
                // 背包 → 输入槽（有配方的物品优先）
                if (!this.mergeItemStack(current, 0, 1, false)) {
                    return ItemStack.EMPTY;
                }
            } else if (index < TOTAL_SLOTS - 9) {
                // 主背包 → 快捷栏
                if (!this.mergeItemStack(current, TOTAL_SLOTS - 9, TOTAL_SLOTS, false)) {
                    return ItemStack.EMPTY;
                }
            } else if (!this.mergeItemStack(current, MACHINE_SLOTS, TOTAL_SLOTS - 9, false)) {
                // 快捷栏 → 主背包
                return ItemStack.EMPTY;
            }
            if (current.isEmpty()) {
                slot.putStack(ItemStack.EMPTY);
            } else {
                slot.onSlotChanged();
            }
            if (current.getCount() == remaining.getCount()) {
                return ItemStack.EMPTY;
            }
            slot.onTake(player, current);
        }
        return remaining;
    }

    @Override
    public boolean canInteractWith(EntityPlayer player) {
        return !this.tile.isInvalid()
                && player.getDistanceSq(this.tile.getPos().getX() + 0.5D, this.tile.getPos().getY() + 0.5D,
                        this.tile.getPos().getZ() + 0.5D) <= 64.0D;
    }
}
