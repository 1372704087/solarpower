package com.example.solarpower.energy;

import java.math.BigInteger;

/**
 * EU ↔ Forge Energy（FE）换算：1 EU = 4 FE。
 * <p>FE 是 int，超出上限时夹到 {@link Integer#MAX_VALUE}（对齐"超上限只传上限"的规则）。
 * <p>换算率与 IC2 的 FE 兑换一致，因此 1 EU = 4 FE = 10 J（Mekanism 默认 1 FE = 2.5 J）。
 */
public final class FeConvert {

    /** 1 EU 折合的 FE 数。 */
    public static final int FE_PER_EU = 4;

    private static final BigInteger MAX_FE = BigInteger.valueOf(Integer.MAX_VALUE);

    private FeConvert() {
    }

    /** BigInteger 的 EU → int 的 FE；非正数按 0，超出 int 上限则夹到上限。 */
    public static int toFe(BigInteger eu) {
        if (eu == null || eu.signum() <= 0) {
            return 0;
        }
        BigInteger fe = eu.multiply(BigInteger.valueOf(FE_PER_EU));
        return fe.compareTo(MAX_FE) >= 0 ? Integer.MAX_VALUE : fe.intValue();
    }

    /** int 的 FE → BigInteger 的 EU；不足 1 EU 的零头舍去。 */
    public static BigInteger toEu(int fe) {
        return fe <= 0 ? BigInteger.ZERO : BigInteger.valueOf(fe / FE_PER_EU);
    }
}