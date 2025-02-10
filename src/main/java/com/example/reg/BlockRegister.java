package com.example.reg;

import com.example.ExampleMod;
import com.example.block.Pump;
import net.minecraft.world.level.block.Block;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

public class BlockRegister {
    public static final DeferredRegister.Blocks BLOCKS =
            DeferredRegister.createBlocks(ExampleMod.MODID);

    public static final DeferredBlock<Block> PUMP_BLOCK =
            BLOCKS.registerBlock("pump_block",(properties -> new Pump()));

    public static final DeferredBlock<Block> VISIABLE_BLOCK =
            BLOCKS.registerBlock("visiable_block",(properties -> new Block(properties.strength(0.1f).noOcclusion())));
    public static void register(IEventBus eventBus) {
        BLOCKS.register(eventBus);
    }

}
