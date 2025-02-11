package flandre923.justpump.reg;

import flandre923.justpump.JustPump;
import flandre923.justpump.blockentitiy.PumpBlockEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public class BlockEntityRegister {

    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, JustPump.MODID);


    public static final Supplier<BlockEntityType<PumpBlockEntity>> PUMP_BE =
            BLOCK_ENTITIES.register("pump_block_entity",()->
                    BlockEntityType.Builder.of(
                            PumpBlockEntity::new,
                            BlockRegister.PUMP_BLOCK.get()
                    ).build(null));

    public static void register(IEventBus eventBus) {
        BLOCK_ENTITIES.register(eventBus);
    }


}
