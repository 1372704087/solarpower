package com.example.solarpower.energy;

/**
 * EU 能量容器的统一接口，对应 IC2 的 {@code IEnergySink}/{@code IEnergySource} 的简化版。
 * <p>蓄电量用 {@code long}：IU 的高阶太阳能板缓存可达 2.5×10¹² EU，超出 int 范围。
 */
public interface IEuEnergy {

    /** 当前蓄电量（EU）。 */
    long getStoredEu();

    /** 蓄电上限（EU）。 */
    long getCapacityEu();

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
    long receiveEu(long amount, EuTier tier, boolean simulate);

    /**
     * 抽取 EU。
     *
     * @param amount   请求抽取量
     * @param simulate 为 true 时只计算不实际扣除
     * @return 实际抽取的 EU
     */
    long extractEu(long amount, boolean simulate);
}