package com.example.helper;

import com.example.helper.data.FluidResult;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.material.FluidState;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

public class CuboidFluidScanner extends FluidScanner{
    private static final int MAX_BLOCKS_PER_TICK = 2048; // 每tick处理2048个方块

    // 扫描范围边界
    private final int minX, minY, minZ;
    private final int maxX, maxY, maxZ;


    // 扫描状态
    private final Queue<BlockPos> scanQueue = new ConcurrentLinkedQueue<>();
    private final Set<BlockPos> scanned = ConcurrentHashMap.newKeySet();
    private final List<BlockPos> foundPositions = new ArrayList<>();
    private boolean isCompleted = false;


    public CuboidFluidScanner(Level level, BlockPos corner1, BlockPos corner2,BlockPos area,BlockPos offset) {
        super(level);
        // 计算立方体边界
        this.minX = Math.min(corner1.getX(), corner2.getX());
        this.minY = Math.min(corner1.getY(), corner2.getY());
        this.minZ = Math.min(corner1.getZ(), corner2.getZ());
        this.maxX = Math.max(corner1.getX(), corner2.getX());
        this.maxY = Math.max(corner1.getY(), corner2.getY());
        this.maxZ = Math.max(corner1.getZ(), corner2.getZ());
        // 初始化扫描队列（按层扫描）
        for (int y = minY; y <= maxY; y++) {
            for (int x = minX; x <= maxX; x++) {
                for (int z = minZ; z <= maxZ; z++) {
                    scanQueue.add(new BlockPos(x, y, z));
                }
            }
        }
    }

    @Override
    public void startScan() {
        if (!isScanning) {
            isScanning = true;
        }
    }

    @Override
    public void tick() {
        if (!isScanning) return;

        int processed = 0;
        while (!scanQueue.isEmpty() && processed < MAX_BLOCKS_PER_TICK) {
            BlockPos pos = scanQueue.poll();
            processBlock(pos);
            processed++;
        }

        if (scanQueue.isEmpty()) {
            completeScan();
        }
    }

    private void processBlock(BlockPos pos) {
        if (scanned.add(pos)) {
            FluidState fluidState = level.getFluidState(pos);
            if (fluidState.isSource() && !fluidState.isEmpty()) {
                foundPositions.add(pos.immutable());
            }
        }
    }

    @Override
    protected void completeScan() {
        isCompleted = true;
        isScanning = false;
        this.result = new FluidResult(
                false, // 范围扫描始终标记为有限
                new ArrayList<>(foundPositions)
        );

        if (listener != null) {
            listener.onScanComplete(result);
        }
    }

    @Override
    public void reset() {
        scanQueue.clear();
        scanned.clear();
        foundPositions.clear();
        isScanning = false;
        isCompleted = false;
        result = null;

        // 重新初始化队列
        for (int y = minY; y <= maxY; y++) {
            for (int x = minX; x <= maxX; x++) {
                for (int z = minZ; z <= maxZ; z++) {
                    scanQueue.add(new BlockPos(x, y, z));
                }
            }
        }
    }

    @Override
    public boolean isScanning() {
        return isScanning && !isCompleted;
    }

    @Override
    public boolean isComplete() {
        return !this.isScanning;
    }

}