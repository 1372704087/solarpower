package com.example.solarpower.energy;

import java.math.BigInteger;

/**
 * EU 能量容器的统一接口，对应 IC2 的 {@code IEnergySink}/{@code IEnergySource} 的简化版。
 * <p>蓄电量用 {@link BigInteger}：不设数值上限，可承载任意大的 EU，不再受 long/int 封顶限制。
 */
public interface IEuEnergy {

    /** 当前蓄电量（EU）。 */
    BigInteger getStoredEu();

    /** 蓄电上限（EU）。 */
    BigInteger getCapacityEu();

    /**
     * 还能注入多少 EU（容量 − 存量，非负）。
     * <p>等价于 {@code getCapacityEu().subtract(getStoredEu()).max(ZERO)}，但电缆网络
     * 每 tick 会对同一容器反复询问该值：百万位大数的相减是 O(带宽) 且要分配一块新数组，
     * 而稳态下这个值往往整 tick 不变。自带缓存的实现可覆盖本方法把重复计算吃掉
     * （见 {@code SolarPanelTile.TileEnergy}）。
     */
    default BigInteger getRoomEu() {
        return getCapacityEu().subtract(getStoredEu()).max(BigInteger.ZERO);
    }

    /** 该容器的电压等级。 */
    EuTier getTier();

    /**
     * 注入 EU。
     *
     * @param amount   请求注入量
     * @param tier     输入电压等级，超过本容器等级时拒收
     * @param simulate 为 true 时只计算不实际写入
     * @return 实际接收的 EU
     */
    BigInteger receiveEu(BigInteger amount, EuTier tier, boolean simulate);

    /**
     * 抽取 EU。
     *
     * @param amount   请求抽取量
     * @param simulate 为 true 时只计算不实际扣除
     * @return 实际抽取的 EU
     */
    BigInteger extractEu(BigInteger amount, boolean simulate);
}