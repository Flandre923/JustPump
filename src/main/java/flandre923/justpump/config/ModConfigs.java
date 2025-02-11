package flandre923.justpump.config;

import flandre923.justpump.JustPump;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.common.ModConfigSpec;

@EventBusSubscriber(modid = JustPump.MODID,bus = EventBusSubscriber.Bus.MOD)
public class ModConfigs {

    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    // 每个tick处理的方块数量
    private static final ModConfigSpec.IntValue BLOCKS_PER_TICK = BUILDER
            .comment("Number of blocks processed per tick")
            .defineInRange("blocksPerTick", 4096, 1, Integer.MAX_VALUE);

    // 泵的流体容量（单位：mB，1桶=1000mB）
    private static final ModConfigSpec.IntValue PUMP_CAPACITY = BUILDER
            .comment("Pump fluid storage capacity in mB (1 bucket = 1000mB)")
            .defineInRange("pumpCapacity", 16 , 1, 64);

    // 视为无限流体的阈值（单位：mB）
    private static final ModConfigSpec.IntValue INFINITE_THRESHOLD = BUILDER
            .comment("Fluid amount above this threshold will be considered infinite")
            .defineInRange("infiniteThreshold", 10000, 0, Integer.MAX_VALUE);

    public static final ModConfigSpec SPEC = BUILDER.build();

    // 配置值实例
    public static int blocksPerTick;
    public static int pumpCapacity;
    public static int infiniteThreshold;

    @SubscribeEvent
    static void onLoad(final ModConfigEvent event) {
        // 从配置加载值到静态变量
        blocksPerTick = BLOCKS_PER_TICK.get();
        pumpCapacity = PUMP_CAPACITY.get();
        infiniteThreshold = INFINITE_THRESHOLD.get();
    }

}
