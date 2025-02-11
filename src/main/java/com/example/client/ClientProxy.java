package com.example.client;

import com.example.blockentitiy.PumpBlockEntity;
import com.example.network.SyncRangeDisplayPacket;
import com.example.network.SyncScanStatePacket;
import com.example.screen.PumpScreen;
import net.minecraft.advancements.critereon.UsedTotemTrigger;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
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


    public static void handleScanStateSync(SyncScanStatePacket packet) {
        Minecraft minecraft = Minecraft.getInstance();
        ClientAreaOffsetHelper.setIsScanning(packet.scanning());
        ClientAreaOffsetHelper.setIsScanComplete(packet.scanComplete());
        if (minecraft.screen instanceof PumpScreen screen
                && screen.getMenu().getBlockEntity().getBlockPos().equals(packet.pos())) {
            screen.updateDisplay();  // 新增界面强制刷新
        }
    }

}
