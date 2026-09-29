package com.example.solarpower.energy;

/**
 * 仿 IC2 / IU 的电压等级（tier）。
 * 每一级电压是上一级的 4 倍，最高一级封顶到 {@link Integer#MAX_VALUE}。
 * 机器只接收不高于自身等级的电压；与 IC2 一样超压被拒绝（IC2 里机器会直接爆炸，这里简化为拒收以免炸家）。
 */
public enum EuTier {
    LV(32),
    MV(128),
    HV(512),
    EV(2048),
    IV(8192),
    LUV(32768),
    ZPM(131072),
    UV(524288),
    UHV(2097152),
    UEV(8388608),
    UIV(33554432),
    UMV(134217728),
    UXV(536870912),
    MAX(Integer.MAX_VALUE);

    private final int maxVoltage;

    EuTier(int maxVoltage) {
        this.maxVoltage = maxVoltage;
    }

    /** 该等级允许的最大电压（EU/packet）。 */
    public int maxVoltage() {
        return this.maxVoltage;
    }

    /** 本等级能否接受来自 {@code incoming} 等级的电压包。 */
    public boolean accepts(EuTier incoming) {
        return incoming.maxVoltage <= this.maxVoltage;
    }

    /** 本等级在 IU 里对应的 tier 编号（从 1 开始）。 */
    public int iuTier() {
        return ordinal() + 1;
    }

    /** 取 IU 编号对应的电压等级，超出范围时夹到两端。 */
    public static EuTier byIuTier(int iuTier) {
        int index = Math.max(1, Math.min(values().length, iuTier)) - 1;
        return values()[index];
    }
}