package com.example.solarpower.energy;

import com.example.solarpower.tileentity.GlassCableTile;
import com.example.solarpower.tileentity.SolarPanelTile;
import com.example.solarpower.solar.GlassCableTier;

import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

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
    public static long push(World world, BlockPos anchor, IEuEnergy source, EuTier packetTier) {
        if (world.isRemote) {
            return 0L;
        }
        Net net = collect(world, anchor, packetTier);
        if (net.isEmpty() || net.sinks().isEmpty()) {
            return 0L;
        }
        long demand = 0L;
        for (BlockPos sink : net.sinks()) {
            IEuEnergy energy = energyAt(world, sink);
            if (energy != null) {
                demand += Math.max(0L, energy.getCapacityEu() - energy.getStoredEu());
            }
        }
        long budget = Math.min(net.tier().capacityPerTick(), demand);
        budget = Math.min(budget, source.getStoredEu());
        if (budget <= 0L) {
            return 0L;
        }
        long moved = source.extractEu(budget, false);
        // 线损：电缆根数 × 每根线损，从传输总量中扣除
        long loss = Math.min(moved, Math.round(net.cables().size() * net.tier().lossPerBlock()));
        long delivered = moved - loss;
        long perSink = delivered / net.sinks().size();
        long sent = 0L;
        for (int i = 0; i < net.sinks().size(); i++) {
            IEuEnergy energy = energyAt(world, net.sinks().get(i));
            if (energy == null) {
                continue;
            }
            long share = Math.min(perSink, energy.getCapacityEu() - energy.getStoredEu());
            if (i == net.sinks().size() - 1) {
                share = Math.min(delivered - sent, energy.getCapacityEu() - energy.getStoredEu());
            }
            if (share > 0L) {
                energy.receiveEu(share, packetTier, false);
                sent += share;
            }
        }
        // 没送出去的部分按源自身电压退回
        if (sent < moved) {
            source.receiveEu(moved - sent, packetTier, false);
        }
        return moved;
    }

    /** 从 anchor 出发 BFS 收集整个电缆网络（成员坐标 + 邻接汇）。 */
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
        // 汇：与网络相邻、暴露 IEuEnergy 且能接收该电压包的方块实体（太阳能板自身除外）
        List<BlockPos> sinks = new ArrayList<>();
        Set<BlockPos> sinkSeen = new HashSet<>();
        for (BlockPos cable : cables) {
            for (EnumFacing dir : EnumFacing.values()) {
                BlockPos side = cable.offset(dir);
                if (cableAt(world, side) != null || !sinkSeen.add(side)) {
                    continue;
                }
                TileEntity be = world.getTileEntity(side);
                if (be instanceof SolarPanelTile) {
                    continue;
                }
                if (be instanceof IEuEnergy) {
                    IEuEnergy energy = (IEuEnergy) be;
                    if (energy.receiveEu(1L, packetTier, true) > 0L) {
                        sinks.add(side);
                    }
                }
            }
        }
        return new Net(tier, cables, sinks);
    }

    private static GlassCableTile cableAt(World world, BlockPos pos) {
        TileEntity be = world.getTileEntity(pos);
        return be instanceof GlassCableTile ? (GlassCableTile) be : null;
    }

    private static IEuEnergy energyAt(World world, BlockPos pos) {
        TileEntity be = world.getTileEntity(pos);
        return be instanceof IEuEnergy ? (IEuEnergy) be : null;
    }

    /** 一张电缆网络：等级、成员电缆坐标与邻接汇坐标。 */
    public static final class Net {

        static final Net EMPTY = new Net(null, new ArrayList<>(), new ArrayList<>());

        private final GlassCableTier tier;
        private final List<BlockPos> cables;
        private final List<BlockPos> sinks;

        Net(GlassCableTier tier, List<BlockPos> cables, List<BlockPos> sinks) {
            this.tier = tier;
            this.cables = cables;
            this.sinks = sinks;
        }

        boolean isEmpty() {
            return this.cables.isEmpty();
        }

        public GlassCableTier tier() {
            return this.tier;
        }

        public List<BlockPos> cables() {
            return this.cables;
        }

        public List<BlockPos> sinks() {
            return this.sinks;
        }
    }
}
