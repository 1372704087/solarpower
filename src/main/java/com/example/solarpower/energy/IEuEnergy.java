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