package com.example.client;

import com.example.network.SyncAreaDataPayload;
import com.example.screen.PumpScreen;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class ClientEvents {
    public static void handleAreaDataSync(SyncAreaDataPayload payload) {
        Minecraft.getInstance().execute(() -> {
            if (Minecraft.getInstance().screen instanceof PumpScreen screen) {
                screen.updateInputFields(payload.area(), payload.offset());
            }
        });
    }
}
