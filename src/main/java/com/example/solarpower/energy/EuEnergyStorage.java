package com.example.solarpower.energy;

/** {@link IEuEnergy} 的基础实现，只做蓄电与容量/电压校验。 */
public class EuEnergyStorage implements IEuEnergy {

    private final long capacity;
    private final EuTier tier;
    private long stored;

    public EuEnergyStorage(long capacity, EuTier tier) {
        this.capacity = capacity;
        this.tier = tier;
    }

    @Override
    public long getStoredEu() {
        return this.stored;
    }

    @Override
    public long getCapacityEu() {
        return this.capacity;
    }

    @Override
    public EuTier getTier() {
        return this.tier;
    }

    @Override
    public long receiveEu(long amount, EuTier tier, boolean simulate) {
        if (amount <= 0L || !this.tier.accepts(tier)) {
            return 0L;
        }
        long accepted = Math.min(amount, this.capacity - this.stored);
        if (accepted <= 0L) {
            return 0L;
        }
        if (!simulate) {
            this.stored += accepted;
        }
        return accepted;
    }

    @Override
    public long extractEu(long amount, boolean simulate) {
        if (amount <= 0L) {
            return 0L;
        }
        long extracted = Math.min(amount, this.stored);
        if (extracted <= 0L) {
            return 0L;
        }
        if (!simulate) {
            this.stored -= extracted;
        }
        return extracted;
    }

    /** 供存档读回使用，数值会被夹在 [0, capacity]。 */
    public void setStoredEu(long value) {
        this.stored = Math.max(0L, Math.min(value, this.capacity));
    }
}