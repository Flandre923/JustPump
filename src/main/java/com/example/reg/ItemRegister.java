package com.example.reg;

import com.example.ExampleMod;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ItemRegister {
    public static final DeferredRegister.Items ITEMS =
            DeferredRegister.createItems(ExampleMod.MODID);


    public static final DeferredItem<BlockItem> PUMP_ITEM = ITEMS.register(
            "pump",
            () -> new BlockItem(BlockRegister.PUMP_BLOCK.get(), new Item.Properties())
    );


    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }
}
