package com.example.helper.data;

import com.example.blockentitiy.PumpMode;
import com.example.helper.Helper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.fluids.FluidType;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

public class FluidResult {
    private static final String FLUID_KEY = "Fluid";
    private static final String INFINITE_KEY = "Infinite";
    private static final String POSITIONS_KEY = "Positions";
    private static final String POS_X_KEY = "x";
    private static final String POS_Y_KEY = "y";
    private static final String POS_Z_KEY = "z";

    private final boolean isInfinite;
    private final List<BlockPos> positions;

    public FluidResult(boolean isInfinite, List<BlockPos> positions) {
        this.isInfinite = isInfinite;
        this.positions = new CopyOnWriteArrayList<>(positions);
    }

    // 新增反序列化构造函数
    public FluidResult(CompoundTag tag) {
        Fluid fluid = BuiltInRegistries.FLUID.get(ResourceLocation.parse(tag.getString(FLUID_KEY)));
        this.isInfinite = tag.getBoolean(INFINITE_KEY);

        ListTag posList = tag.getList(POSITIONS_KEY, Tag.TAG_COMPOUND);
        List<BlockPos> loadedPositions = new CopyOnWriteArrayList<>();
        for (int i = 0; i < posList.size(); i++) {
            CompoundTag posTag = posList.getCompound(i);
            loadedPositions.add(new BlockPos(
                posTag.getInt(POS_X_KEY),
                posTag.getInt(POS_Y_KEY),
                posTag.getInt(POS_Z_KEY)
            ));
        }
        this.positions = isInfinite ? Collections.emptyList() : loadedPositions;
    }

    public void save(CompoundTag tag) {
        tag.putBoolean(INFINITE_KEY, isInfinite);

        ListTag posList = new ListTag();
        if (!isInfinite) {
            for (BlockPos pos : positions) {
                CompoundTag posTag = new CompoundTag();
                posTag.putInt(POS_X_KEY, pos.getX());
                posTag.putInt(POS_Y_KEY, pos.getY());
                posTag.putInt(POS_Z_KEY, pos.getZ());
                posList.add(posTag);
            }
        }
        tag.put(POSITIONS_KEY, posList);
    }


    public List<BlockPos> getFluidPositions(int max) {
        return positions.stream()
                .filter(pos -> !usedPositions.contains(pos))
                .limit(max)
                .collect(Collectors.toList());
    }

    public void updateAfterDrain(int drained) {
        if (!isInfinite) {
            synchronized (positions) {
                int actualDrained = Math.min(drained / FluidType.BUCKET_VOLUME, positions.size());
                positions.subList(0, actualDrained).clear();
            }
        }
    }

    public String debugReport() {
        return String.format("""
            Fluid Cluster Scan Completed!
            Status: %s
            Source Blocks: %d
            First Position: %s
            Last Position: %s
            """,
            isInfinite ? "INFINITE" : "FINITE",
            positions.size(),
            isInfinite ? "N/A" : Helper.posToString(positions.get(0)),
            isInfinite ? "N/A" : Helper.posToString(positions.get(positions.size()-1))
        );
    }

    public PumpMode getMode() {
        return PumpMode.EXTRACTING_AUTO;
    }

    public boolean isInfinite() {
        return isInfinite;
    }

    public int getSourceCount() {
        return positions.size();
    }

    public List<BlockPos> getAllPositions() {
        return Collections.unmodifiableList(positions);
    }

    private final Set<BlockPos> usedPositions = ConcurrentHashMap.newKeySet();

    public void markPositionUsed(BlockPos pos) {
        usedPositions.add(pos);
    }

}
