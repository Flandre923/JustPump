package flandre923.justpump.reg;

import flandre923.justpump.JustPump;
import flandre923.justpump.menu.PumpMenu;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class MenuTypeRegister {

    public static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(Registries.MENU, JustPump.MODID);

    // 使用新式注册方法
    public static final DeferredHolder<MenuType<?>, MenuType<PumpMenu>> PUMP_MENU =
            MENUS.register("pump_menu", () ->
                    IMenuTypeExtension.create(
                            (containerId, inv,buffer) -> new PumpMenu(
                                    containerId,
                                    inv,
                                    buffer.readBlockPos()
                            )
                    )
            );

    public static void register(IEventBus eventBus)
    {
        MENUS.register(eventBus);
    }

}
