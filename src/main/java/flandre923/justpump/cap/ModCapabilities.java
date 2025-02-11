package flandre923.justpump.cap;

import flandre923.justpump.reg.BlockEntityRegister;
import flandre923.justpump.JustPump;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;

@EventBusSubscriber(modid = JustPump.MODID,bus = EventBusSubscriber.Bus.MOD)
public class ModCapabilities {

    @SubscribeEvent
    public static void onRegisterCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(
                Capabilities.FluidHandler.BLOCK,
                BlockEntityRegister.PUMP_BE.get(),
                (be, direction) -> be.getFluidHandler(direction)
        );
    }
}
