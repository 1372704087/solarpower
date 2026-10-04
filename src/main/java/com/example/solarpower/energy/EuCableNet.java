package com.example.solarpower.energy;

import com.example.solarpower.tileentity.GlassCableTile;
import com.example.solarpower.tileentity.SolarPanelTile;
import com.example.solarpower.tileentity.StorageBoxTile;
import com.example.solarpower.tileentity.TransformerTile;
import com.example.solarpower.solar.GlassCableTier;

import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.math.BigInteger;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 简化版 EU 电缆网络（1.12.2）：把相连的玻璃电缆聚成一张网，网内相邻的
 * 太阳能板（源）向相邻的 {@link IEuEnergy} 接收端（汇）输电。
 * <p>规则（对齐 IU 的简化版）：
 * <ul>
 *     <li>整个网络每 tick 传输上限 = 电缆等级的 capacityPerTick；</li>
 *     <li>线损按网络内电缆根数 × 每根线损一次性扣除（不做逐包路径计算）；</li>
 *     <li>电压包等级 = 源（面板）的电压等级，超压的汇会自行拒收。</li>
 * </ul>
 * <p>所有 EU 运算用 {@link BigInteger}，不设数值上限。
 * <p>除面板自行推流（{@link #push}）外，另提供 {@link #drain} 与 {@link #fill} 两个桥接入口：
 * 玻璃电缆把它们暴露成 Forge Energy，外部 FE 线缆（Mekanism、其它 FE 机器）就能接到本模组电网上
 * 取电或送电。桥接时的电压包按最低等级（{@link EuTier#LV}）标记，避免汇被误判为超压。
 */
public final class EuCableNet {

    private static final int MAX_NET_SIZE = 4096;

    /** 网络结构缓存：同一 game tick 内按 anchor 命中，避免每块面板各 BFS 一遍。
     *  <p>只在服务端主线程使用（{@code push}/{@code drain}/{@code fill} 都已被 isRemote 拦住），
     *  因此不需要同步。tick 变化即整表失效——网络结构可能因放置/拆除电缆而改变。 */
    private static World NET_CACHE_WORLD;
    private static long NET_CACHE_TICK = Long.MIN_VALUE;
    private static final Map<BlockPos, Net> NET_CACHE_ANCHOR = new HashMap<>();

    private EuCableNet() {
    }

    /**
     * 面板推流入口：扫描 {@code panelPos} 四周的相邻玻璃电缆，**每个相邻电缆面各推流一次**。
     *
     * <p>面板可能同时贴着同一张网的多根电缆（最多 6 个面）。此时该网每 tick 会收到
     * 多次推流，本板向该网的实际推送量可达「单面最大输出 × 相邻面数（最多 6）」——
     * 这是刻意保留的原始行为（放电按面数放大），不做按网络归并。
     *
     * <p>代价：每个面都要重做一遍百万位大数运算，实测顶档（2^8990097）一个面约 1.6 ms，
     * 六个面就是约 10 ms。若日后要把「面数放大」当 bug 修掉，需给网络快照加一个
     * 「结构身份」编号（{@link Net} 由 {@link Net#resolveSinks} 派生出新对象，
     * 不能用引用相等判同一张网），据此在本循环里归并成每网一次。
     *
     * <p>同 tick 内首次调用会 BFS 整张网，之后同网络的面板直接命中缓存。
     *
     * @param packetTier 电压包等级（源面板的电压等级）
     * @param maxOutput  单个相邻面每 tick 的最大输出（面板档位的「最大输出」），null 表示不限
     * @return 本次实际结算的面数（相邻的电缆面、且该网有汇）
     */
    public static int push(World world, BlockPos panelPos, IEuEnergy source, EuTier packetTier,
                           BigInteger maxOutput) {
        if (world.isRemote) {
            return 0;
        }
        int settled = 0;
        for (EnumFacing dir : EnumFacing.values()) {
            BlockPos side = panelPos.offset(dir);
            if (!(world.getTileEntity(side) instanceof GlassCableTile)) {
                continue;
            }
            Net net = collect(world, side, packetTier);
            if (net.isEmpty() || net.sinks().isEmpty()) {
                continue;
            }
            settle(world, net, source, maxOutput);
            settled++;
        }
        return settled;
    }

    /**
     * 储电盒推流入口：IU 语义的「正面输出」——只扫 {@code facing} 方向上相邻的电缆，
     * 每 tick 至多向那一张网推流一次（不像面板那样按相邻面数放大）。
     * 推流时把储电盒自己从汇里排除，避免刚推出去的电又被自己吸回来
     * （盒子贴着自己的输出面时既是源也是汇候选）。
     *
     * @return 本次是否实际结算（正面相邻电缆、且该网有其它汇）
     */
    public static int pushFromFacing(World world, BlockPos boxPos, IEuEnergy source,
                                     EuTier packetTier, BigInteger maxOutput, EnumFacing facing) {
        if (world.isRemote) {
            return 0;
        }
        BlockPos side = boxPos.offset(facing);
        if (!(world.getTileEntity(side) instanceof GlassCableTile)) {
            return 0;
        }
        Net net = collect(world, side, packetTier);
        if (net.isEmpty() || net.sinks().isEmpty()) {
            return 0;
        }
        settle(world, net, source, maxOutput, boxPos);
        return 1;
    }

    /**
     * 把源储存中的 EU 推进单张网络（一次调用 = 一个相邻面的推流）。
     *
     * @param maxOutput 单个相邻面每 tick 的最大输出，null 表示不限
     * @return 实际从源扣走并送达汇的 EU（线损部分留在源内，未扣）
     */
    private static BigInteger settle(World world, Net net, IEuEnergy source, BigInteger maxOutput) {
        return settle(world, net, source, maxOutput, null);
    }

    /** 同上；{@code excludeSink} 非空时该坐标不参与汇分配（储电盒推流时排除自己）。 */
    private static BigInteger settle(World world, Net net, IEuEnergy source, BigInteger maxOutput,
                                     BlockPos excludeSink) {
        if (net.isEmpty() || net.sinks().isEmpty()) {
            return BigInteger.ZERO;
        }
        // 先用「网络上限、源存量、面板最大输出」这三个只需廉价比较的值定出上限，
        // min 可交换，取最小值的顺序不影响结果；但源为空/上限为 0 时可就此返回，
        // 省掉整张网的空余量扫描（该项要对每个汇做一次大数相减）。
        BigInteger limit = net.tier().capacityPerTick().min(source.getStoredEu());
        if (maxOutput != null) {
            limit = limit.min(maxOutput);
        }
        if (limit.signum() <= 0) {
            return BigInteger.ZERO;
        }
        BigInteger budget = limit.min(demandOf(world, net, limit, excludeSink));
        if (budget.signum() <= 0) {
            return BigInteger.ZERO;
        }
        // 先派发、后扣源：源存量的净变化在两种写法下都等于 -sent。
        // 旧写法是「先按 budget 抽满，再把没送出去的 moved-sent 退回」——那个退回量
        // 往往只是个线损级的小数，却要走一次全宽相减去求它、再走一次全宽相加才加得回去，
        // 在百万位的数上每次都是零点几毫秒。这里直接只扣实际送出的部分。
        // 源存量必 ≥ budget ≥ sent，故这一步不可能抽空。
        // 语义注记：线损部分因此留在源蓄电内（旧写法是烧掉）。稳态下源蓄电恒满，
        // 两种写法的对外行为等价；若要恢复「线损烧掉」，在这里补一次 extractEu(loss)。
        BigInteger sent = fillSinks(world, net, budget.subtract(lossOf(net, budget)), excludeSink);
        if (sent.signum() > 0) {
            source.extractEu(sent, false);
        }
        return sent;
    }

    /**
     * 桥接出电：从 anchor 网络相邻的源（太阳能板）抽 {@code amount} EU。
     *
     * @return 扣线损后实际送出的 EU
     */
    public static BigInteger drain(World world, BlockPos anchor, BigInteger amount, boolean simulate) {
        if (world.isRemote || amount == null || amount.signum() <= 0) {
            return BigInteger.ZERO;
        }
        Net net = collect(world, anchor, EuTier.LV);
        BigInteger available = storedOf(world, net);
        if (net.isEmpty() || available.signum() <= 0) {
            return BigInteger.ZERO;
        }
        BigInteger budget = net.tier().capacityPerTick().min(available).min(amount);
        if (budget.signum() <= 0) {
            return BigInteger.ZERO;
        }
        BigInteger loss = lossOf(net, budget);
        if (simulate) {
            return budget.subtract(loss);
        }
        BigInteger moved = extractSources(world, net, budget);
        return moved.subtract(loss.min(moved));
    }

    /**
     * 桥接进电：把 {@code amount} EU 送进 anchor 网络相邻的汇。
     *
     * @return 扣线损后实际送达的 EU
     */
    public static BigInteger fill(World world, BlockPos anchor, BigInteger amount, boolean simulate) {
        if (world.isRemote || amount == null || amount.signum() <= 0) {
            return BigInteger.ZERO;
        }
        Net net = collect(world, anchor, EuTier.LV);
        if (net.isEmpty() || net.sinks().isEmpty()) {
            return BigInteger.ZERO;
        }
        // 同 push：先用常量与请求量定出上限，再拿它去扫汇空余（见 demandOf 的说明）。
        BigInteger limit = net.tier().capacityPerTick().min(amount);
        if (limit.signum() <= 0) {
            return BigInteger.ZERO;
        }
        BigInteger budget = limit.min(demandOf(world, net, limit, null));
        if (budget.signum() <= 0) {
            return BigInteger.ZERO;
        }
        BigInteger delivered = budget.subtract(lossOf(net, budget));
        return simulate ? delivered : fillSinks(world, net, delivered, null);
    }

    /** 网络相邻源（太阳能板）的存量之和，夹到本档每 tick 上限；供电缆对外报告"可抽取量"。 */
    public static BigInteger storedEu(World world, BlockPos anchor) {
        if (world.isRemote) {
            return BigInteger.ZERO;
        }
        Net net = collect(world, anchor, EuTier.LV);
        return net.isEmpty() ? BigInteger.ZERO : storedOf(world, net).min(net.tier().capacityPerTick());
    }

    /** 从 anchor 出发 BFS 收集整个电缆网络（成员坐标 + 邻接源/汇）。
     *  <p>结果按 {@code world.getTotalWorldTime()} 缓存，并把整张网络的**每个成员坐标**
     *  都登记为 key：同一 tick 内同网络的面板无论从哪根电缆接入都命中同一份结果。
     *  原先每块面板各自 BFS 一遍，N 块面板就是 N 遍；现在是每网络每 tick 一遍。
     *  <p>BFS 部分与原实现逐字等价；源/汇扫描仍按「逐个电缆扫 6 邻」进行，
     *  因为同一块面板可能邻接多根电缆，必须按其各自的去重集合统计。 */
    private static Net collect(World world, BlockPos anchor, EuTier packetTier) {
        long now = world.getTotalWorldTime();
        if (NET_CACHE_WORLD == world && NET_CACHE_TICK == now) {
            Net cached = NET_CACHE_ANCHOR.get(anchor);
            if (cached != null) {
                // 缓存里只存「结构」（成员、源、汇候选），电压包等级与本次调用相关：
                // 汇是否收电依赖 packetTier，必须按本次等级重新过滤，不能复用上一调用方的结果。
                return cached.resolveSinks(world, packetTier);
            }
        } else {
            // 进入新 tick：整表失效（网络结构可能因放置/拆除而变）
            NET_CACHE_ANCHOR.clear();
            NET_CACHE_WORLD = world;
            NET_CACHE_TICK = now;
        }

        GlassCableTile anchorCable = cableAt(world, anchor);
        if (anchorCable == null) {
            Net empty = Net.EMPTY.resolveSinks(world, packetTier);
            NET_CACHE_ANCHOR.put(anchor, Net.EMPTY);
            return empty;
        }
        GlassCableTier tier = anchorCable.tier();

        // --- 第一遍：BFS 收集电缆成员（与原实现逐字等价，包括 MAX_NET_SIZE 的计法） ---
        List<BlockPos> cables = new ArrayList<>();
        Set<BlockPos> seen = new HashSet<>();
        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        queue.add(anchor);
        seen.add(anchor);
        while (!queue.isEmpty() && seen.size() <= MAX_NET_SIZE) {
            BlockPos pos = queue.poll();
            cables.add(pos);
            for (EnumFacing dir : EnumFacing.values()) {
                BlockPos next = pos.offset(dir);
                if (seen.add(next) && cableAt(world, next) != null) {
                    queue.add(next);
                }
            }
        }

        // --- 第二遍：扫描每个电缆的非电缆邻居，分类为源 / 汇候选 ---
        List<BlockPos> sources = new ArrayList<>();
        Set<BlockPos> sourceSeen = new HashSet<>();
        Set<BlockPos> sinkSeen = new HashSet<>();
        List<BlockPos> sinkCandidates = new ArrayList<>();
        for (BlockPos cable : cables) {
            for (EnumFacing dir : EnumFacing.values()) {
                BlockPos side = cable.offset(dir);
                if (cableAt(world, side) != null) {
                    continue;
                }
                TileEntity be = world.getTileEntity(side);
                if (be instanceof SolarPanelTile) {
                    if (sourceSeen.add(side)) {
                        sources.add(side);
                    }
                    continue;
                }
                // 储电盒：正面（facing）朝向这根电缆时按源计（IU 正面输出语义）；
                // 同时它始终暴露 IEuEnergy，落到下面的汇候选分支（五面进电）。
                if (be instanceof StorageBoxTile
                        && ((StorageBoxTile) be).facing() == dir.getOpposite()
                        && sourceSeen.add(side)) {
                    sources.add(side);
                }
                // 变压器：任一面都可作为抽取源（FE 桥 drain 用；它自身转按 receiveEu 分流进池）
                if (be instanceof TransformerTile && sourceSeen.add(side)) {
                    sources.add(side);
                }
                if (be instanceof IEuEnergy && sinkSeen.add(side)) {
                    sinkCandidates.add(side);
                }
            }
        }

        // 缓存结构（成员/源/汇候选）；汇的实际收电力每次都按调用方给的 packetTier 重判
        Net base = new Net(tier, cables, sources, sinkCandidates);
        // 整张网络的每个成员都指向同一份结构：这样同网络的面板无论从哪根电缆接入都能命中
        for (BlockPos cable : cables) {
            NET_CACHE_ANCHOR.put(cable, base);
        }
        return base.resolveSinks(world, packetTier);
    }

    /** 网络相邻汇的空余容量之和。
     *  <p>{@code limit} 是本次调用已经确定的上限：一旦累计空余达到它，
     *  {@code min(limit, demand)} 就必然是 {@code limit}，无须再扫其余汇。
     *  <p>单块汇的空余走 {@link IEuEnergy#getRoomEu()}，自带缓存的实现可避免
     *  每 tick 反复对同一个百万位数做相减。 */
    private static BigInteger demandOf(World world, Net net, BigInteger limit, BlockPos excludeSink) {
        BigInteger demand = BigInteger.ZERO;
        for (BlockPos pos : net.sinks()) {
            if (pos.equals(excludeSink)) {
                continue;
            }
            IEuEnergy energy = energyAt(world, pos);
            if (energy != null) {
                demand = demand.add(energy.getRoomEu());
                if (demand.compareTo(limit) >= 0) {
                    return limit;
                }
            }
        }
        return demand;
    }

    /** 网络相邻源的存量之和（太阳能板 + 储电盒 + 变压器双池）。 */
    private static BigInteger storedOf(World world, Net net) {
        BigInteger total = BigInteger.ZERO;
        for (BlockPos pos : net.sources()) {
            TileEntity be = world.getTileEntity(pos);
            if (be instanceof SolarPanelTile) {
                total = total.add(((SolarPanelTile) be).getEnergy().getStoredEu());
            } else if (be instanceof StorageBoxTile) {
                total = total.add(((StorageBoxTile) be).getStoredEu());
            } else if (be instanceof TransformerTile) {
                total = total.add(((TransformerTile) be).getStoredEu());
            }
        }
        return total;
    }

    /** 线损：网络内电缆根数 × 每根线损，夹到不超过 {@code amount}。 */
    private static BigInteger lossOf(Net net, BigInteger amount) {
        if (amount.signum() <= 0) {
            return BigInteger.ZERO;
        }
        long lossAmount = Math.round(net.cables().size() * net.tier().lossPerBlock());
        return BigInteger.valueOf(lossAmount).min(amount);
    }

    /** 按汇平均分配 {@code amount}，返回实际被接收的 EU。
     *  <p>份额用 {@link #shareOf}：只有 1 个汇时该份额根本用不到（唯一一次循环落到
     *  "最后一个汇"分支），直接从除法里省掉；2/4/8 个汇改走移位。
     *  <p>单个汇的空余走 {@link IEuEnergy#getRoomEu()}，与 {@code receiveEu} 内部的
     *  夹取共用同一份缓存，不额外多算一次大数相减。 */
    private static BigInteger fillSinks(World world, Net net, BigInteger amount, BlockPos excludeSink) {
        int count = 0;
        for (BlockPos pos : net.sinks()) {
            if (!pos.equals(excludeSink)) {
                count++;
            }
        }
        if (count == 0 || amount.signum() <= 0) {
            return BigInteger.ZERO;
        }
        BigInteger perSink = count == 1 ? amount : shareOf(amount, count);
        BigInteger sent = BigInteger.ZERO;
        int index = 0;
        for (int i = 0; i < net.sinks().size(); i++) {
            BlockPos pos = net.sinks().get(i);
            if (pos.equals(excludeSink)) {
                continue;
            }
            IEuEnergy energy = energyAt(world, pos);
            if (energy != null) {
                BigInteger room = energy.getRoomEu();
                // 「最后一个有效汇拿余额」的语义保持不变：index 只数未被排除的汇
                BigInteger share = index == count - 1 ? amount.subtract(sent).min(room)
                        : perSink.min(room);
                if (share.signum() > 0) {
                    sent = sent.add(energy.receiveEu(share, net.packetTier(), false));
                }
            }
            index++;
        }
        return sent;
    }

    /** 按源平均抽取 {@code amount}，返回实际抽出的 EU（太阳能板走其内部储能，储电盒直接抽）。 */
    private static BigInteger extractSources(World world, Net net, BigInteger amount) {
        int count = net.sources().size();
        if (count == 0 || amount.signum() <= 0) {
            return BigInteger.ZERO;
        }
        BigInteger perSource = count == 1 ? amount : shareOf(amount, count);
        BigInteger taken = BigInteger.ZERO;
        for (int i = 0; i < count; i++) {
            TileEntity be = world.getTileEntity(net.sources().get(i));
            BigInteger share = i == count - 1 ? amount.subtract(taken) : perSource;
            if (be instanceof SolarPanelTile) {
                taken = taken.add(((SolarPanelTile) be).getEnergy().extractEu(share, false));
            } else if (be instanceof StorageBoxTile) {
                taken = taken.add(((StorageBoxTile) be).extractEu(share, false));
            } else if (be instanceof TransformerTile) {
                taken = taken.add(((TransformerTile) be).extractEu(share, false));
            }
        }
        return taken;
    }

    /**
     * 均分份额 {@code amount / count}（向下取整）。
     * <p>调用方已保证 {@code amount} 为正，故 2/4/8 用算术右移与整除逐位等价，
     * 而 {@link BigInteger#shiftRight} 只按字长搬位、不做除法循环：本机实测顶档
     * （1.1 MB 的数）一次 {@code divide(3)} 要 1.27 ms，一次移位只要 0.13 ms。
     * <p>另外 {@code divide(ONE)} 在 JDK 8 里**并不会**被短路（实测 1.02 ms），
     * 所以只有一个汇/源时必须在上层直接跳过除法。
     */
    private static BigInteger shareOf(BigInteger amount, int count) {
        switch (count) {
            case 2:
                return amount.shiftRight(1);
            case 4:
                return amount.shiftRight(2);
            case 8:
                return amount.shiftRight(3);
            default:
                return amount.divide(BigInteger.valueOf(count));
        }
    }

    private static GlassCableTile cableAt(World world, BlockPos pos) {
        TileEntity be = world.getTileEntity(pos);
        return be instanceof GlassCableTile ? (GlassCableTile) be : null;
    }

    private static IEuEnergy energyAt(World world, BlockPos pos) {
        TileEntity be = world.getTileEntity(pos);
        return be instanceof IEuEnergy ? (IEuEnergy) be : null;
    }

    /** 一张电缆网络的结构快照：等级、成员电缆坐标、邻接源坐标与邻接能量方块（汇候选）。
     *  <p>汇候选是「所有暴露 IEuEnergy 的邻接方块」，是否真能收电取决于电压包等级，
     *  由 {@link #resolveSinks} 按当次调用的 tier 过滤——因此本对象可跨不同 packetTier 复用。 */
    public static final class Net {

        static final Net EMPTY = new Net(null, new ArrayList<>(), new ArrayList<>(), new ArrayList<>());

        private final GlassCableTier tier;
        private final List<BlockPos> cables;
        private final List<BlockPos> sources;
        private final List<BlockPos> sinkCandidates;
        /** 本次调用实际可收电的汇（由 resolveSinks 按 packetTier 过滤得出）。 */
        private final EuTier packetTier;
        private final List<BlockPos> sinks;

        /** 结构快照（尚无 packetTier/sinks，由 {@link #resolveSinks} 派生）。 */
        Net(GlassCableTier tier, List<BlockPos> cables, List<BlockPos> sources, List<BlockPos> sinkCandidates) {
            this(tier, EuTier.LV, cables, sources, sinkCandidates, java.util.Collections.emptyList());
        }

        private Net(GlassCableTier tier, EuTier packetTier, List<BlockPos> cables,
                    List<BlockPos> sources, List<BlockPos> sinkCandidates, List<BlockPos> sinks) {
            this.tier = tier;
            this.packetTier = packetTier;
            this.cables = cables;
            this.sources = sources;
            this.sinkCandidates = sinkCandidates;
            this.sinks = sinks;
        }

        boolean isEmpty() {
            return this.cables.isEmpty();
        }

        /** 按电压包等级过滤汇候选，产出本次调用可用的汇集合。
         *  <p>ECJ 的 {@code receiveEu} 判定与空容量计算都要读 world，故需传入 world。 */
        Net resolveSinks(World world, EuTier packetTier) {
            if (this.sinkCandidates.isEmpty()) {
                return new Net(this.tier, packetTier, this.cables, this.sources, this.sinkCandidates, java.util.Collections.emptyList());
            }
            List<BlockPos> sinks = new ArrayList<>(this.sinkCandidates.size());
            for (BlockPos pos : this.sinkCandidates) {
                IEuEnergy energy = energyAt(world, pos);
                if (energy != null && energy.receiveEu(BigInteger.ONE, packetTier, true).signum() > 0) {
                    sinks.add(pos);
                }
            }
            return new Net(this.tier, packetTier, this.cables, this.sources, this.sinkCandidates, sinks);
        }

        public GlassCableTier tier() {
            return this.tier;
        }

        EuTier packetTier() {
            return this.packetTier;
        }

        public List<BlockPos> cables() {
            return this.cables;
        }

        List<BlockPos> sources() {
            return this.sources;
        }

        public List<BlockPos> sinks() {
            return this.sinks;
        }
    }
}