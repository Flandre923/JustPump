package flandre923.justpump.blockentitiy.visiable;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;

public class VisiableHelper {
    public static void placeHollowCube(Level level, Block block, BlockPos start, BlockPos end) {
        // 确定立方体边界
        int minX = Math.min(start.getX(), end.getX());
        int maxX = Math.max(start.getX(), end.getX());
        int minY = Math.min(start.getY(), end.getY());
        int maxY = Math.max(start.getY(), end.getY());
        int minZ = Math.min(start.getZ(), end.getZ());
        int maxZ = Math.max(start.getZ(), end.getZ());

        // 遍历所有外壳位置
        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    // 检查是否在外壳上
                    if (isOnSurface(x, y, z, minX, maxX, minY, maxY, minZ, maxZ)) {
                        BlockPos pos = new BlockPos(x, y, z);

                        // 检查当前位置是否可放置
                        if (canPlaceBlock(level, pos)) {
                            level.setBlock(pos, block.defaultBlockState(),
                                    Block.UPDATE_ALL | Block.UPDATE_CLIENTS);
                        }
                    }
                }
            }
        }
    }

    public static void removeHollowCube(Level level, Block targetBlock, BlockPos start, BlockPos end) {
        // 计算立方体边界
        int minX = Math.min(start.getX(), end.getX());
        int maxX = Math.max(start.getX(), end.getX());
        int minY = Math.min(start.getY(), end.getY());
        int maxY = Math.max(start.getY(), end.getY());
        int minZ = Math.min(start.getZ(), end.getZ());
        int maxZ = Math.max(start.getZ(), end.getZ());

        // 遍历所有外壳位置
        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    // 检查是否在外壳上
                    if (isOnSurface(x, y, z, minX, maxX, minY, maxY, minZ, maxZ)) {
                        BlockPos pos = new BlockPos(x, y, z);

                        // 检查当前方块是否匹配目标类型
                        if (isMatchingBlock(level, pos, targetBlock)) {
                            level.setBlock(pos, Blocks.AIR.defaultBlockState(),
                                    Block.UPDATE_ALL | Block.UPDATE_CLIENTS);
                        }
                    }
                }
            }
        }
    }

    private static boolean isMatchingBlock(Level level, BlockPos pos, Block targetBlock) {
        BlockState currentState = level.getBlockState(pos);
        return currentState.getBlock() == targetBlock;
    }

    private static boolean isOnSurface(int x, int y, int z,
                                       int minX, int maxX,
                                       int minY, int maxY,
                                       int minZ, int maxZ) {
        // 检查是否在六个面上
        return x == minX || x == maxX ||
                y == minY || y == maxY ||
                z == minZ || z == maxZ;
    }

    private static boolean canPlaceBlock(Level level, BlockPos pos) {
        // 获取当前方块状态
        BlockState currentState = level.getBlockState(pos);
        FluidState fluidState = currentState.getFluidState();

        // 排除流体源方块
        if (fluidState.isSource()) {
            return false;
        }
        // 允许替换空气、可替换植物、液体等
        return currentState.isAir() ||
                currentState.canBeReplaced();
    }
}
