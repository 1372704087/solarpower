package com.example.solarpower.energy;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;

import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.Map;

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
    /** 词头可命名的最大千进制组（10 个标准 + 999 个扩展 = 10^3030 量级封顶）。 */
    private static final int MAX_PREFIX_GROUP = (EU_UNITS.length - 1) + 999;
    /** 万位数以上的 BigInteger 转字符串是二次方开销，而 tooltip/GUI 每帧重取，
     * 按对象身份缓存格式化结果（调用方传的是档位常量字段，实例稳定）。
     * <p>注意：实时变化的存量每 tick 都会产生新对象，身份缓存对其无效——
     * 这类调用点应由持有方（如 {@code SolarPanelTile.TileEnergy}）自己做按引用惰性缓存。 */
    private static final Map<BigInteger, String> SCI_CACHE =
            Collections.synchronizedMap(new IdentityHashMap<BigInteger, String>());
    private static final Map<BigInteger, String> ANNOTATED_CACHE =
            Collections.synchronizedMap(new IdentityHashMap<BigInteger, String>());
    private static final Map<BigInteger, String> FORMAT_CACHE =
            Collections.synchronizedMap(new IdentityHashMap<BigInteger, String>());
    /** 10 的幂缓存：decimalExponent/displayParts 每 tick 重取。
     *  <p>淘汰策略是「只清最小的一半」而不是整表 clear —— 整表清空会让最大的那些幂
     *  （同时也是最贵的）被反复重建，形成周期性卡顿。 */
    private static final Map<Integer, BigInteger> POW10_CACHE =
            Collections.synchronizedMap(new HashMap<Integer, BigInteger>());
    private static final int POW10_CACHE_LIMIT = 1024;

    private static BigInteger pow10(int e) {
        if (e <= 1) {
            return BigInteger.TEN.pow(Math.max(0, e));
        }
        Integer key = e;
        BigInteger hit = POW10_CACHE.get(key);
        if (hit == null) {
            hit = BigInteger.TEN.pow(e);
            if (POW10_CACHE.size() > POW10_CACHE_LIMIT) {
                // 只淘汰较小的一半：更大的幂更贵，清掉它们只会让接下来的调用重建最贵的部分
                int median = POW10_CACHE.size() / 2;
                Iterator<Integer> it = POW10_CACHE.keySet().iterator();
                while (it.hasNext() && median > 0) {
                    if (it.next() < e) {
                        it.remove();
                        median--;
                    }
                }
            }
            POW10_CACHE.put(key, hit);
        }
        return hit;
    }

    public static String formatEu(BigInteger value) {
        if (value == null || value.signum() <= 0) {
            return "0";
        }
        String hit = FORMAT_CACHE.get(value);
        if (hit == null) {
            hit = uncachedFormat(value);
            if (FORMAT_CACHE.size() > 4096) {
                FORMAT_CACHE.clear();
            }
            FORMAT_CACHE.put(value, hit);
        }
        return hit;
    }

    /**
     * 词头 + 科学计数法约数注记，如 {@code 69.43DcNnOq (≈6.94e925)}；
     * 数值小于 10^6 时无需注记，返回纯词头形式。
     */
    public static String annotated(BigInteger value) {
        if (value == null) {
            return formatEu((BigDecimal) null);
        }
        String hit = ANNOTATED_CACHE.get(value);
        if (hit == null) {
            hit = uncachedAnnotated(value);
            if (ANNOTATED_CACHE.size() > 4096) {
                ANNOTATED_CACHE.clear();
            }
            ANNOTATED_CACHE.put(value, hit);
        }
        return hit;
    }

    private static String uncachedAnnotated(BigInteger value) {
        String sci = scientificApprox(value);
        String fmt = formatEu(value);
        if (fmt.equals(sci)) {
            return fmt; // 超出词头范围时 formatEu 退回的正是科学计数法，勿重复注记
        }
        return fmt + (sci == null ? "" : " (≈" + sci + ")");
    }

    /** 约 3 位有效数字的科学计数法，如 {@code 6.94e925}；小于 10^6 时返回 null（无需注记）。 */
    public static String scientificApprox(BigInteger value) {
        if (value == null || value.signum() <= 0) {
            return null;
        }
        String hit = SCI_CACHE.get(value);
        if (hit != null) {
            return hit.isEmpty() ? null : hit;
        }
        hit = uncachedSci(value);
        if (SCI_CACHE.size() > 4096) {
            SCI_CACHE.clear();
        }
        SCI_CACHE.put(value, hit == null ? "" : hit);
        return hit;
    }

    /** {@link #scientificApprox(BigInteger)} 的无缓存实现。 */
    private static String uncachedSci(BigInteger v) {
        int e = decimalExponent(v);
        if (e < 6) {
            return null;
        }
        // 只取前 3 位有效数字：大数除以 10^(e-2)，商固定为 3 位，避免平方级 toString
        String head = v.divide(pow10(e - 2)).toString();
        return head.charAt(0) + "." + head.substring(1) + "e" + e;
    }

    /** long 的十进制位数（精确：log10 在 10 的整数幂上会因浮点误差少 1）。 */
    private static int digitsOf(long v) {
        return Long.toString(v).length();
    }

    /**
     * 巨数的「紧凑镜像」：只保留前 n 位有效数字（高位截断），数值量级不变，O(1) 构造。
     * <p>用途：界面侧的镜像值只有 4 位有效数字，而档位常量是精确巨数——最高档
     * 2^8990097 约 270 万位十进制。直接拿两者做 {@link BigDecimal#compareTo} 或
     * {@link BigDecimal#divide} 时，JDK 会把两边补到同一 scale，把巨数完整物化出来：
     * 实测一次 240 ms（divide）/ 约 500 ms（compareTo），界面每帧都做 ⇒ 面板完全卡死。
     * 把巨数也压成同量级的紧凑镜像后，比较与除法都退化成 long 运算（实测 0.0006 ms）。
     *
     * @param n 保留的有效位数，1..18（受 long 限制）
     */
    public static BigDecimal compactMirror(BigInteger value, int n) {
        if (value == null || value.signum() <= 0) {
            return BigDecimal.ZERO;
        }
        int want = Math.max(1, Math.min(n, 18));
        int e = decimalExponent(value);
        if (e + 1 < want) {
            want = e + 1;               // 数值本身不足 want 位：此时镜像即精确值
        }
        // 取前 want 位十进制数字必须走真正的十进制除法：用移位取高位得到的是「2 的幂」缩放，
        // 换算成十进制还需要乘回 2^k，直接取它的十进制数字会得到错误结果。
        // 这里商的位宽只有 want（≤ 18）位，JDK 会走 Knuth 除法（商的字数为 1），
        // 代价约 O(被除数位宽) —— 每个档位只算一次并缓存。
        long mantissa = value.divide(pow10(e - want + 1)).longValue();
        return BigDecimal.valueOf(mantissa).scaleByPowerOfTen(e - digitsOf(mantissa) + 1);
    }

    /** 大数 → {首 4 位有效数字, 十进制指数}，供容器窗口属性同步（每 tick 一次，持有方按引用缓存）。
     *  <p>直接做一次精确整除：商的位宽只有 4 位，JDK 会走 Knuth 除法（商的字数为 1），
     *  代价是 O(被除数位宽)，不是平方级 —— 实测 270 万位约 0.7 ms。
     *  <p>曾试过「取最高 60 位做估计 + 线性校正」以避免这次除法，但 {@code shiftRight}
     *  得到的是「2 的幂」缩放，换算成十进制还差一个因子：实测估计值偏高 5.6 倍，
     *  远超 ±8 的校正上限，于是每次都白做 8 次巨数乘法再落到精确除法兜底 —— 整体反而慢约 4 倍。 */
    public static int[] displayParts(BigInteger value) {
        if (value == null || value.signum() <= 0) {
            return new int[]{0, 0};
        }
        int e = decimalExponent(value);
        int want = Math.min(4, e + 1);
        return new int[]{value.divide(pow10(e - want + 1)).intValue(), e};
    }

    /**
     * 十进制指数 e（10^e ≤ v &lt; 10^(e+1)）。
     * <p>先用 bitLength 做 O(1) 估算（log10(2) ≈ 0.30103），再用至多两次比较校正。
     * 关键在于校正必须是「单次比较 + 单次移位」而不是 divide/multiply 循环：
     * 对 n 位大数，循环里每一步都是 O(n²) 的全量乘除，而移位取位宽无关的常数时间比例。
     */
    private static int decimalExponent(BigInteger v) {
        int bits = v.bitLength();
        if (bits <= 64) {
            // 小数值直接走 toString：此时 toString 可忽略，且能确保 ±1 完全正确
            return v.toString().length() - 1;
        }
        int e = (int) ((long) (bits - 1) * 30103L / 100000L);
        // 估算误差至多 ±1（0.30103 略小于真实 log10(2)），单次比较即可校正
        BigInteger pow = pow10(e);
        int cmp = pow.compareTo(v);
        if (cmp > 0) {
            return e - 1;
        }
        if (pow.multiply(BigInteger.TEN).compareTo(v) <= 0) {
            return e + 1;
        }
        return e;
    }

    /** {@link #formatEu(BigInteger)} 的无缓存实现：词头路径只碰前几位数字。 */
    private static String uncachedFormat(BigInteger v) {
        int e = decimalExponent(v);
        if (e < 3) {
            // 不足 1000：原样输出（此规模的 toString 可忽略不计）
            return v.compareTo(BigInteger.valueOf(100L)) >= 0
                    ? v.toString()
                    : new BigDecimal(v).setScale(2, RoundingMode.DOWN).toPlainString();
        }
        int group = e / 3;
        if (group >= MAX_PREFIX_GROUP) {
            return scientificApprox(v);
        }
        int lead = e % 3 + 1;                    // 缩位后整数部分位数（1..3）
        int want = lead + 2;                     // 整数位 + 2 位小数
        String digits = v.divide(pow10(e - want + 1)).toString();
        String coeff = lead == 3
                ? digits.substring(0, 3)
                : digits.substring(0, lead) + "." + digits.substring(lead, lead + 2);
        return coeff + prefixFor(group);
    }

    /** {@link #scientificApprox(BigInteger)} 的 BigDecimal 版（负 scale 也正确）。 */
    public static String scientificApprox(BigDecimal value) {
        if (value == null || value.signum() <= 0) {
            return null;
        }
        // 取前 3 位有效数字，不足则补 0 —— 与 BigInteger 版语义一致：
        // 1e3000 的 toString() 是 "1" 后跟 3000 个 0，前 3 位同样是 "100"，故显示 1.00e3000。
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
        // 先规范化：stripTrailingZeros 之后 precision() 才是「真有效位数」。
        // 例：BigDecimal.valueOf(1000).scaleByPowerOfTen(3000) 的 precision 是 4，
        // 但真有效位数只有 1 —— 直接拿 precision 推组号会把 10^3000 算成 990 组而非 1000 组，
        // 于是同一块面板会因数值来源不同而显示不同的词头。
        BigDecimal v = value.stripTrailingZeros();
        // 十进制指数直接取「precision - scale - 1」，**不要**先把 scale 归零：
        // 归零会把「4 位尾数 × 10^k」物化成完整的十进制整数。本模组最高档发电量是
        // 2^8990097（约 270 万位），一次 setScale(0) 实测 246 ms，紧随其后的 setScale(2)
        // 再花 243 ms —— 界面每帧调用两次 formatEu，合计 1.2 s/帧，面板完全无法使用。
        // 不归零时后续 movePointLeft 只搬动 scale（O(1)），尾数始终是紧凑的 4 位数。
        int exponent = v.precision() - v.scale() - 1;
        if (exponent < 0) {
            // < 1 的值不在 EU 的取值域内（能量恒为非负整数），但真出现时直接原样输出，
            // 不参与词头换算 —— 避免组号算出负数导致数组越界。
            return v.toPlainString();
        }
        // 组序号 = 十进制指数 / 3，O(1) 得出，不做「反复除以 1000」的循环：
        // GUI 每帧都会调它，而搬动 scale 在大 precision 上是 O(n)，循环推到 T 级就是 O(n²)。
        int group = exponent / 3;
        if (group >= MAX_PREFIX_GROUP) {
            // 超出扩展词头命名范围（≥10^3031）：词头写不出来，退回纯科学计数法
            return scientificApprox(value);
        }
        BigDecimal scaled = v.movePointLeft(group * 3);
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
        int k = Math.min(group, MAX_PREFIX_GROUP) - (EU_UNITS.length - 1); // 1 = 10^33（unquetta）
        return EXT_HUNDREDS[Math.min(k / 100, EXT_HUNDREDS.length - 1)]
                + EXT_TENS[Math.min(k / 10 % 10, EXT_TENS.length - 1)]
                + EXT_UNITS[Math.min(k % 10, EXT_UNITS.length - 1)] + "q";
    }

    private EuFormat() {
    }
}
