package flandre923.justpump.reg;

import flandre923.justpump.JustPump;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ItemRegister {
    public static final DeferredRegister.Items ITEMS =
            DeferredRegister.createItems(JustPump.MODID);


    public static final DeferredItem<BlockItem> PUMP_ITEM = ITEMS.register(
            "pump_block",
            () -> new BlockItem(BlockRegister.PUMP_BLOCK.get(), new Item.Properties())
    );


    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }
}
