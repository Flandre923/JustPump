package com.example.helper;

import it.unimi.dsi.fastutil.Pair;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.material.Fluid;
import org.lwjgl.system.linux.Stat;

public class Helper {
    public static String getFluidName(Fluid fluid) {
        return fluid.builtInRegistryHolder().key().location().toString();
    }

    public static String posToString(BlockPos pos) {
        return String.format("[X:%d Y:%d Z:%d]", pos.getX(), pos.getY(), pos.getZ());
    }

    public static Pair<BlockPos, BlockPos> calculateScanRange(
            BlockPos basePos,
            BlockPos area,    // (x半径, y延伸, z半径)
            BlockPos offset    // (x偏移, y偏移, z偏移)
    ) {
        // 分解参数分量
        int xRadius = area.getX();
        int yExtend = area.getY();
        int zRadius = area.getZ();

        int xOffset = offset.getX();
        int yOffset = offset.getY();
        int zOffset = offset.getZ();

        // 计算中心点
        BlockPos center = basePos.offset(xOffset, yOffset, zOffset);

        // 计算扫描范围
        BlockPos start = center.offset(
                -xRadius,       // X轴负向扩展
                yOffset,        // 保持原始Y偏移
                -zRadius        // Z轴负向扩展
        );

        BlockPos end = center.offset(
                xRadius,        // X轴正向扩展
                -yExtend,       // Y轴负向延伸（向下探测）
                zRadius        // Z轴正向扩展
        );

        return Pair.of(start, end);
    }
}
