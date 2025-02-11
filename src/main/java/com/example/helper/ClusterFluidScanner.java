package com.example.helper;

import com.example.helper.data.FluidResult;
import com.google.common.collect.Queues;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class ClusterFluidScanner extends FluidScanner{
    private static final int MAX_BLOCKS_PER_TICK = 512;
    private static final int MAX_FINITE_VOLUME = 10000;

    private final BlockPos startPos;
    private final Queue<BlockPos> scanQueue = Queues.newConcurrentLinkedQueue();
    private final Set<BlockPos> scanned = ConcurrentHashMap.newKeySet();
    private FluidState targetFluid;
    private Set<BlockPos> foundPositions = new LinkedHashSet<>();
    private boolean isInfinite = false;
    private boolean isCompleted = false;


    public ClusterFluidScanner(Level level, BlockPos startPos) {
        super(level);
        this.startPos = startPos;
        initialize();
    }

    public void startScan() {
        if (!isScanning) {
            isScanning = true;
            initialize();
        }
    }

    private void initialize() {
        // 垂直扫描找到第一个流体源
        BlockPos.MutableBlockPos checkPos = startPos.mutable();
        while (checkPos.getY() >= level.getMinBuildHeight()) {
            FluidState state = level.getFluidState(checkPos);
            if (!state.isEmpty()) {
                targetFluid = state;
                scanQueue.add(checkPos.immutable());
                scanned.add(checkPos.immutable());
                return;
            }
            checkPos.move(Direction.DOWN);
        }
        isCompleted = true;
        if (listener != null) {
            listener.onScanComplete(new FluidResult(false, Collections.emptyList()));
        }
    }

    public void tick() {
        if(!isScanning) return;

        int processed = 0;
        while (!scanQueue.isEmpty() && processed < MAX_BLOCKS_PER_TICK) {
            BlockPos pos = scanQueue.poll();
            processBlock(pos);
            processed++;
            if (foundPositions.size() > MAX_FINITE_VOLUME) {
                markAsInfinite();
                break;
            }
        }

        if (scanQueue.isEmpty()) {
//            generateResult();
            completeScan();
        }
    }

    private void processBlock(BlockPos pos) {
        if (isSource(pos)) { // 仅添加源头到结果列表
            foundPositions.add(pos);
        }
        // 无论是否源头都进行扩散扫描
        checkForNeighbors(pos);
    }

    private void checkForNeighbors(BlockPos pos) {
        for (Direction dir : Direction.values()) {
            BlockPos neighborPos = pos.relative(dir);

            // 使用isFluidAny代替isValidFluid
            if (isFluidAny(neighborPos) && !scanned.contains(neighborPos)) {
                scanned.add(neighborPos);
                scanQueue.add(neighborPos);
            }
        }

        // 特殊处理向下扫描
        BlockPos downPos = pos.below();
        if (isFluidAny(downPos) && !scanned.contains(downPos)) {
            scanned.add(downPos);
            scanQueue.add(downPos);
        }
    }
    private boolean isSource(BlockPos pos) {
        return level.getFluidState(pos).isSource();
    }


    private boolean isFluidAny(BlockPos pos) {
        if (pos.getY() < level.getMinBuildHeight()) return false;

        FluidState state = level.getFluidState(pos);
        return !state.isEmpty() &&
                state.getType().getFluidType() == targetFluid.getType().getFluidType();
    }


    private void markAsInfinite() {
        isInfinite = true;
        scanQueue.clear();
        isCompleted = true;
    }

    protected void completeScan() {
        isCompleted = true;
        // 转换为ArrayList保持顺序
        List<BlockPos> uniquePositions = new ArrayList<>(foundPositions);
        this.result = new FluidResult(
                isInfinite,
                uniquePositions
        );
        if (listener != null) {
            listener.onScanComplete(result);
        }
    }
    public Optional<FluidResult> getResult() {
        if (!isCompleted) return Optional.empty();
        return Optional.of(this.result);
    }

    public boolean isScanning() {
        return !isCompleted && !isInfinite;
    }
    public void reset() {
        scanQueue.clear();
        scanned.clear();
        foundPositions.clear();
        targetFluid = null;
        isInfinite = false;
        isCompleted = false;
        result = null;
        isScanning = false; // 重置父类中的扫描状态
    }

    @Override
    public boolean isComplete() {
        return  isCompleted;
    }


}

