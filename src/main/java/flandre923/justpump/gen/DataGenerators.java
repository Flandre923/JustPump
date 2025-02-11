package flandre923.justpump.gen;

import flandre923.justpump.JustPump;
import net.minecraft.data.DataGenerator;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import net.neoforged.neoforge.data.event.GatherDataEvent;

@EventBusSubscriber(modid = JustPump.MODID,bus = EventBusSubscriber.Bus.MOD)
public class DataGenerators {
    @SubscribeEvent
    public static void gatherData(GatherDataEvent event)
    {
        DataGenerator generator = event.getGenerator();
        ExistingFileHelper helper = event.getExistingFileHelper();
        generator.addProvider(event.includeClient(), new ModBlockStateProvider(generator.getPackOutput(), JustPump.MODID, helper));
        generator.addProvider(event.includeClient(),new ModItemProvider(generator.getPackOutput(), JustPump.MODID,helper));
    }
}
