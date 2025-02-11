package com.example.client;

import com.example.blockentitiy.PumpBlockEntity;
import com.example.network.SyncRangeDisplayPacket;
import com.example.screen.PumpScreen;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
// 客户端处理类
public class ClientProxy {
    public static void handleRangeDisplaySync(SyncRangeDisplayPacket packet) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level.getBlockEntity(packet.pos()) instanceof PumpBlockEntity pump) {
                pump.getAreaDisplay().setActive(packet.isActive());
            if (Minecraft.getInstance().screen instanceof PumpScreen screen) {
                screen.updateDisplay();
            }
        }
    }
}