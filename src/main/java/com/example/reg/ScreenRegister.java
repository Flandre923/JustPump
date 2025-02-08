package com.example.reg;

import com.example.ExampleMod;
import com.example.screen.PumpScreen;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

@EventBusSubscriber(modid = ExampleMod.MODID,bus= EventBusSubscriber.Bus.MOD,value = Dist.CLIENT)
public class ScreenRegister {

    @SubscribeEvent
    public static void onClient(RegisterMenuScreensEvent event)
    {
        event.register(MenuTypeRegister.PUMP_MENU.get(), PumpScreen::new);
    }
}
