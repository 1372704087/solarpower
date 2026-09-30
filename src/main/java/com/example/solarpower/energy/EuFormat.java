package com.example.solarpower.energy;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;

/**
 * EU 数值的界面缩写：千进制缩位 + 国际单位制词头（标准到 Q，之后用扩展词头），
 * 不用科学计数法。公共类，GUI 与物品 tooltip 共用。
 * <p>例如 {@code 1.35DcNnSeq EU/t}（1.35 × 10^921）。
 */
public final class EuFormat {

    /** 千进制标准词头，索引 0 为空、1 为 k……10 为 Q（10^30，官方词头到 Q 为止）。 */
    private static final String[] EU_UNITS = {"", "k", "M", "G", "T", "P", "E", "Z", "Y", "R", "Q"};
    /** Q 之后的扩展词头（unquetta/duoquetta…），按「百位+十位+个位」组合，末尾补 q。 */
    private static final String[] EXT_UNITS = {"", "U", "D", "T", "Q", "P", "S", "Se", "O", "N"};
    private static final String[] EXT_TENS = {"", "Dc", "Vg", "Tg", "Qg", "Pg", "Sg", "Se", "Og", "Nn"};
    private static final String[] EXT_HUNDREDS = {"", "C", "Dc", "Tc", "Qc", "Pc", "Sc", "Sec", "Oc", "Nc"};
    private static final BigDecimal THOUSAND = BigDecimal.valueOf(1_000L);
    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100L);

    public static String formatEu(BigInteger value) {
        return formatEu(value == null ? null : new BigDecimal(value));
    }

    /**
     * 词头 + 科学计数法约数注记，如 {@code 69.43DcNnOq (≈6.94e925)}；
     * 数值小于 10^6 时无需注记，返回纯词头形式。
     */
    public static String annotated(BigInteger value) {
        String sci = scientificApprox(value);
        return formatEu(value) + (sci == null ? "" : " (≈" + sci + ")");
    }

    /** 约 3 位有效数字的科学计数法，如 {@code 6.94e925}；小于 10^6 时返回 null（无需注记）。 */
    public static String scientificApprox(BigInteger value) {
        if (value == null || value.signum() <= 0) {
            return null;
        }
        String digits = value.toString();
        if (digits.length() <= 6) {
            return null;
        }
        return digits.charAt(0) + "." + digits.substring(1, 3) + "e" + (digits.length() - 1);
    }

    /** {@link #scientificApprox(BigInteger)} 的 BigDecimal 版（负 scale 也正确）。 */
    public static String scientificApprox(BigDecimal value) {
        if (value == null || value.signum() <= 0) {
            return null;
        }
        String unscaled = value.unscaledValue().toString();
        int exponent = unscaled.length() - 1 - value.scale();
        if (exponent < 6) {
            return null;
        }
        String head = (unscaled + "000").substring(0, 3);
        return head.charAt(0) + "." + head.substring(1) + "e" + exponent;
    }

    public static String formatEu(BigDecimal value) {
        if (value == null || value.signum() <= 0) {
            return "0";
        }
        BigDecimal scaled = value;
        int group = 0;
        while (scaled.compareTo(THOUSAND) >= 0) {
            scaled = scaled.movePointLeft(3);
            group++;
        }
        String coeff = scaled.compareTo(HUNDRED) >= 0
                ? scaled.setScale(0, RoundingMode.DOWN).toPlainString()
                : scaled.setScale(2, RoundingMode.DOWN).toPlainString();
        return coeff + prefixFor(group);
    }

    /** 千进制组序号 → 词头：0 空白；1..10 标准 SI（k..Q）；11 起扩展（unquetta…缩写）。 */
    private static String prefixFor(int group) {
        if (group < EU_UNITS.length) {
            return EU_UNITS[group];
        }
        int k = group - (EU_UNITS.length - 1); // 1 = 10^33（unquetta）
        return EXT_HUNDREDS[k / 100] + EXT_TENS[k / 10 % 10] + EXT_UNITS[k % 10] + "q";
    }

    private EuFormat() {
    }
}
