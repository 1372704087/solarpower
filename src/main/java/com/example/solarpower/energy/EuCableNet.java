package com.example.solarpower.energy;

import com.example.solarpower.tileentity.GlassCableTile;
import com.example.solarpower.tileentity.SolarPanelTile;
import com.example.solarpower.solar.GlassCableTier;

import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.math.BigInteger;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
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

    private EuCableNet() {
    }

    /**
     * 面板推流入口：把源储存中的 EU 推进 anchor 所在网络。
     *
     * @param packetTier 电压包等级（源面板的电压等级）
     * @return 实际从源送出的 EU（线损未扣）
     */
    public static BigInteger push(World world, BlockPos anchor, IEuEnergy source, EuTier packetTier) {
        if (world.isRemote) {
            return BigInteger.ZERO;
        }
        Net net = collect(world, anchor, packetTier);
        if (net.isEmpty() || net.sinks().isEmpty()) {
            return BigInteger.ZERO;
        }
        BigInteger budget = net.tier().capacityPerTick().min(demandOf(world, net)).min(source.getStoredEu());
        if (budget.signum() <= 0) {
            return BigInteger.ZERO;
        }
        BigInteger moved = source.extractEu(budget, false);
        BigInteger sent = fillSinks(world, net, moved.subtract(lossOf(net, moved)));
        // 没送出去的部分按源自身电压退回
        if (sent.compareTo(moved) < 0) {
            source.receiveEu(moved.subtract(sent), packetTier, false);
        }
        return moved;
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
        BigInteger budget = net.tier().capacityPerTick().min(demandOf(world, net)).min(amount);
        if (budget.signum() <= 0) {
            return BigInteger.ZERO;
        }
        BigInteger delivered = budget.subtract(lossOf(net, budget));
        return simulate ? delivered : fillSinks(world, net, delivered);
    }

    /** 网络相邻源（太阳能板）的存量之和，夹到本档每 tick 上限；供电缆对外报告"可抽取量"。 */
    public static BigInteger storedEu(World world, BlockPos anchor) {
        if (world.isRemote) {
            return BigInteger.ZERO;
        }
        Net net = collect(world, anchor, EuTier.LV);
        return net.isEmpty() ? BigInteger.ZERO : storedOf(world, net).min(net.tier().capacityPerTick());
    }

    /** 从 anchor 出发 BFS 收集整个电缆网络（成员坐标 + 邻接源/汇）。 */
    private static Net collect(World world, BlockPos anchor, EuTier packetTier) {
        if (cableAt(world, anchor) == null) {
            return Net.EMPTY;
        }
        GlassCableTier tier = cableAt(world, anchor).tier();
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
        // 源：与网络相邻的太阳能板（推流方）；汇：与网络相邻、暴露 IEuEnergy 且能收该电压包的非面板方块
        List<BlockPos> sources = new ArrayList<>();
        Set<BlockPos> sourceSeen = new HashSet<>();
        List<BlockPos> sinks = new ArrayList<>();
        Set<BlockPos> sinkSeen = new HashSet<>();
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
                if (sinkSeen.add(side) && be instanceof IEuEnergy) {
                    IEuEnergy energy = (IEuEnergy) be;
                    if (energy.receiveEu(BigInteger.ONE, packetTier, true).signum() > 0) {
                        sinks.add(side);
                    }
                }
            }
        }
        return new Net(tier, packetTier, cables, sources, sinks);
    }

    /** 网络相邻汇的空余容量之和。 */
    private static BigInteger demandOf(World world, Net net) {
        BigInteger demand = BigInteger.ZERO;
        for (BlockPos pos : net.sinks()) {
            IEuEnergy energy = energyAt(world, pos);
            if (energy != null) {
                demand = demand.add(energy.getCapacityEu().subtract(energy.getStoredEu()).max(BigInteger.ZERO));
            }
        }
        return demand;
    }

    /** 网络相邻源的存量之和。 */
    private static BigInteger storedOf(World world, Net net) {
        BigInteger total = BigInteger.ZERO;
        for (BlockPos pos : net.sources()) {
            SolarPanelTile panel = panelAt(world, pos);
            if (panel != null) {
                total = total.add(panel.getEnergy().getStoredEu());
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

    /** 按汇平均分配 {@code amount}，返回实际被接收的 EU。 */
    private static BigInteger fillSinks(World world, Net net, BigInteger amount) {
        int count = net.sinks().size();
        if (count == 0 || amount.signum() <= 0) {
            return BigInteger.ZERO;
        }
        BigInteger perSink = amount.divide(BigInteger.valueOf(count));
        BigInteger sent = BigInteger.ZERO;
        for (int i = 0; i < count; i++) {
            IEuEnergy energy = energyAt(world, net.sinks().get(i));
            if (energy == null) {
                continue;
            }
            BigInteger room = energy.getCapacityEu().subtract(energy.getStoredEu());
            BigInteger share = i == count - 1 ? amount.subtract(sent).min(room) : perSink.min(room);
            if (share.signum() > 0) {
                sent = sent.add(energy.receiveEu(share, net.packetTier(), false));
            }
        }
        return sent;
    }

    /** 按源平均抽取 {@code amount}，返回实际抽出的 EU。 */
    private static BigInteger extractSources(World world, Net net, BigInteger amount) {
        int count = net.sources().size();
        if (count == 0 || amount.signum() <= 0) {
            return BigInteger.ZERO;
        }
        BigInteger perSource = amount.divide(BigInteger.valueOf(count));
        BigInteger taken = BigInteger.ZERO;
        for (int i = 0; i < count; i++) {
            SolarPanelTile panel = panelAt(world, net.sources().get(i));
            if (panel == null) {
                continue;
            }
            BigInteger share = i == count - 1 ? amount.subtract(taken) : perSource;
            taken = taken.add(panel.getEnergy().extractEu(share, false));
        }
        return taken;
    }

    private static GlassCableTile cableAt(World world, BlockPos pos) {
        TileEntity be = world.getTileEntity(pos);
        return be instanceof GlassCableTile ? (GlassCableTile) be : null;
    }

    private static IEuEnergy energyAt(World world, BlockPos pos) {
        TileEntity be = world.getTileEntity(pos);
        return be instanceof IEuEnergy ? (IEuEnergy) be : null;
    }

    private static SolarPanelTile panelAt(World world, BlockPos pos) {
        TileEntity be = world.getTileEntity(pos);
        return be instanceof SolarPanelTile ? (SolarPanelTile) be : null;
    }

    /** 一张电缆网络：等级、电压包等级、成员电缆坐标与邻接源/汇坐标。 */
    public static final class Net {

        static final Net EMPTY = new Net(null, EuTier.LV, new ArrayList<>(), new ArrayList<>(), new ArrayList<>());

        private final GlassCableTier tier;
        private final EuTier packetTier;
        private final List<BlockPos> cables;
        private final List<BlockPos> sources;
        private final List<BlockPos> sinks;

        Net(GlassCableTier tier, EuTier packetTier, List<BlockPos> cables, List<BlockPos> sources, List<BlockPos> sinks) {
            this.tier = tier;
            this.packetTier = packetTier;
            this.cables = cables;
            this.sources = sources;
            this.sinks = sinks;
        }

        boolean isEmpty() {
            return this.cables.isEmpty();
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