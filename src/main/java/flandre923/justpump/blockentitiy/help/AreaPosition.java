package flandre923.justpump.blockentitiy.help;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;

public class AreaPosition {
    private BlockPos currentArea;
    private BlockPos currentOffset;
    private PositionChangeCallback callback;

    public AreaPosition() {
        this(new BlockPos(10, 10, 10), BlockPos.ZERO);
    }

    public AreaPosition(BlockPos area, BlockPos offset) {
        this.currentArea = area;
        this.currentOffset = offset;
    }

    // 回调接口
    public interface PositionChangeCallback {
        void onAreaChanged(BlockPos newArea);
        void onOffsetChanged(BlockPos newOffset);
    }

    // 设置回调
    public void setCallback(PositionChangeCallback callback) {
        this.callback = callback;
    }

    // 区域坐标相关方法
    public BlockPos getCurrentArea() {
        return currentArea;
    }

    public void setCurrentArea(BlockPos newArea) {
        this.currentArea = newArea;
        if (callback != null) {
            callback.onAreaChanged(newArea);
        }
    }

    // 偏移量相关方法
    public BlockPos getCurrentOffset() {
        return currentOffset;
    }

    public void setCurrentOffset(BlockPos newOffset) {
        this.currentOffset = newOffset;
        if (callback != null) {
            callback.onOffsetChanged(newOffset);
        }
    }

    // 保存到NBT
    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putIntArray("currentArea", new int[]{currentArea.getX(), currentArea.getY(), currentArea.getZ()});
        tag.putIntArray("currentOffset", new int[]{currentOffset.getX(), currentOffset.getY(), currentOffset.getZ()});
        return tag;
    }

    // 从NBT加载
    public void load(CompoundTag tag) {
        int[] areaPos = tag.getIntArray("currentArea");
        int[] offsetPos = tag.getIntArray("currentOffset");
        
        if (areaPos.length == 3) {
            setCurrentArea(new BlockPos(areaPos[0], areaPos[1], areaPos[2]));
        }
        if (offsetPos.length == 3) {
            setCurrentOffset(new BlockPos(offsetPos[0], offsetPos[1], offsetPos[2]));
        }
    }
}
