package com.example.solarpower.energy;

import java.math.BigInteger;

/** {@link IEuEnergy} 的基础实现，只做蓄电与容量/电压校验。数值用 BigInteger，无上限。 */
public class EuEnergyStorage implements IEuEnergy {

    private final BigInteger capacity;
    private final EuTier tier;
    private BigInteger stored = BigInteger.ZERO;

    public EuEnergyStorage(BigInteger capacity, EuTier tier) {
        this.capacity = capacity;
        this.tier = tier;
    }

    @Override
    public BigInteger getStoredEu() {
        return this.stored;
    }

    @Override
    public BigInteger getCapacityEu() {
        return this.capacity;
    }

    @Override
    public EuTier getTier() {
        return this.tier;
    }

    @Override
    public BigInteger receiveEu(BigInteger amount, EuTier tier, boolean simulate) {
        if (amount == null || amount.signum() <= 0 || !this.tier.accepts(tier)) {
            return BigInteger.ZERO;
        }
        BigInteger accepted = amount.min(this.capacity.subtract(this.stored));
        if (accepted.signum() <= 0) {
            return BigInteger.ZERO;
        }
        if (!simulate) {
            this.stored = this.stored.add(accepted);
        }
        return accepted;
    }

    @Override
    public BigInteger extractEu(BigInteger amount, boolean simulate) {
        if (amount == null || amount.signum() <= 0) {
            return BigInteger.ZERO;
        }
        BigInteger extracted = amount.min(this.stored);
        if (extracted.signum() <= 0) {
            return BigInteger.ZERO;
        }
        if (!simulate) {
            this.stored = this.stored.subtract(extracted);
        }
        return extracted;
    }

    /** 供存档读回使用，数值会被夹在 [0, capacity]。 */
    public void setStoredEu(BigInteger value) {
        if (value == null || value.signum() < 0) {
            this.stored = BigInteger.ZERO;
        } else {
            this.stored = value.min(this.capacity);
        }
    }
}