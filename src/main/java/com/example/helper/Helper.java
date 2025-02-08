package com.example.helper;

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

}
