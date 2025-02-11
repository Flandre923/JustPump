package flandre923.justpump.helper;


import flandre923.justpump.helper.data.FluidResult;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;

public class FluidFillScanner extends FluidScanner {
    private BlockPos startPos;
    private BlockPos endPos;
    private List<BlockPos> scanPositions = new ArrayList<>();
    private int currentIndex = 0;
    private ArrayList<BlockPos> resPos;
    private boolean isComplete;


    public FluidFillScanner(Level level, BlockPos startPos, BlockPos endPos) {
        super(level);
        this.resPos = new ArrayList<>();
        this.startPos = startPos;
        this.endPos = endPos;
        isComplete = false;
    }

    @Override
    public void startScan() {
        if (isScanning) return;

        // 生成扫描区域的所有位置
        scanPositions.clear();
        BlockPos.betweenClosedStream(startPos, endPos).forEach(pos ->
                scanPositions.add(pos.immutable())
        );

        isScanning = true;
        isComplete = false;
        currentIndex = 0;
    }

    @Override
    public void tick() {
        if (!isScanning || level == null) return;

        int processed = 0;
        while (currentIndex < scanPositions.size() && processed < PER_TICK_COUNT) {
            BlockPos pos = scanPositions.get(currentIndex);

            // 检查方块是否可替换
            BlockState state = level.getBlockState(pos);
            if (state.canBeReplaced()) {
                resPos.add(pos);
            }

            currentIndex++;
            processed++;
        }

        // 扫描完成处理
        if (currentIndex >= scanPositions.size()) {
            result = new FluidResult(false, resPos);
            completeScan();
        }
    }

    @Override
    public void reset() {
        isScanning = false;
        scanPositions.clear();
        currentIndex = 0;
        result = null;
    }

    @Override
    public boolean isComplete() {
        return !isScanning && result != null && isComplete;
    }

}
