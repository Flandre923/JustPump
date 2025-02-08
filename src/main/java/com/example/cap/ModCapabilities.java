package com.example.cap;

import com.example.ExampleMod;
import com.example.reg.BlockEntityRegister;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.capabilities.BlockCapability;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

import static com.example.ExampleMod.MODID;
@EventBusSubscriber(modid = MODID,bus = EventBusSubscriber.Bus.MOD)
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
