package com.example.solarpower.energy;

import java.math.BigInteger;

/**
 * 仿 IC2 / IU 的电压等级（tier）。共 59 级，与太阳能板档位一一对应。
 * <p>每一级电压是上一级的 4 倍，第 n 级上限电压 = 2^(2n+3)（LV = 2^5 = 32，
 * QUETTA = 2^45 ≈ 35.18T）。1~21 级沿用 IC2/IU 的经典命名，22~59 级超出 IU
 * 原有范围，直接以级数命名（L22~L59）。
 * <p>上限电压用 {@link BigInteger} 承载（L32 起超出 long），取消数值封顶；
 * 机器只接收不高于自身等级的电压，超压拒收（IC2 里会爆炸，这里简化为拒收以免炸家）。
 */
public enum EuTier {
    LV(1), MV(2), HV(3), EV(4), IV(5), LUV(6), ZPM(7), UV(8), UHV(9), UEV(10),
    UIV(11), UMV(12), UXV(13), QUARK(14), TERA(15), PETA(16), EXA(17), ZETTA(18),
    YOTTA(19), RONNA(20), QUETTA(21),
    L22(22), L23(23), L24(24), L25(25), L26(26), L27(27), L28(28), L29(29), L30(30),
    L31(31), L32(32), L33(33), L34(34), L35(35), L36(36), L37(37), L38(38), L39(39),
    L40(40), L41(41), L42(42), L43(43), L44(44), L45(45), L46(46), L47(47), L48(48),
    L49(49), L50(50), L51(51), L52(52), L53(53), L54(54), L55(55), L56(56), L57(57),
    L58(58), L59(59);

    private final int iuTier;
    private final BigInteger maxVoltage;

    EuTier(int iuTier) {
        this.iuTier = iuTier;
        this.maxVoltage = BigInteger.ONE.shiftLeft(2 * iuTier + 3);
    }

    /** 该等级允许的最大电压（EU/packet）。 */
    public BigInteger maxVoltage() {
        return this.maxVoltage;
    }

    /** 本等级能否接受来自 {@code incoming} 等级的电压包。 */
    public boolean accepts(EuTier incoming) {
        return incoming.maxVoltage.compareTo(this.maxVoltage) <= 0;
    }

    /** 本等级在 IU 里对应的 tier 编号（从 1 开始）。 */
    public int iuTier() {
        return this.iuTier;
    }

    /** 取 IU 编号对应的电压等级，超出范围时夹到两端。 */
    public static EuTier byIuTier(int iuTier) {
        int index = Math.max(1, Math.min(values().length, iuTier)) - 1;
        return values()[index];
    }

    /** 取能容纳 {@code voltage} 电压包的最低等级；非法值按最低级，超出最高级则夹到最高级。 */
    public static EuTier byVoltage(BigInteger voltage) {
        if (voltage == null || voltage.signum() <= 0) {
            return LV;
        }
        for (EuTier tier : values()) {
            if (tier.maxVoltage.compareTo(voltage) >= 0) {
                return tier;
            }
        }
        return values()[values().length - 1];
    }
}
