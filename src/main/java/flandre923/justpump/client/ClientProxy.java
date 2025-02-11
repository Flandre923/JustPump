package flandre923.justpump.client;

import flandre923.justpump.blockentitiy.PumpBlockEntity;
import flandre923.justpump.network.SyncAreaDataPayload;
import flandre923.justpump.network.SyncRangeDisplayPacket;
import flandre923.justpump.network.SyncScanStatePacket;
import flandre923.justpump.screen.PumpScreen;
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


    public static void handleScanStateSync(SyncScanStatePacket packet) {
        Minecraft minecraft = Minecraft.getInstance();
        ClientAreaOffsetHelper.setIsScanning(packet.scanning());
        ClientAreaOffsetHelper.setIsScanComplete(packet.scanComplete());
        if (minecraft.screen instanceof PumpScreen screen
                && screen.getMenu().getBlockEntity().getBlockPos().equals(packet.pos())) {
            screen.updateDisplay();  // 新增界面强制刷新
        }
    }


    public static void handleAreaDataSync(SyncAreaDataPayload payload) {
        Minecraft.getInstance().execute(() -> {
            if (Minecraft.getInstance().screen instanceof PumpScreen screen) {
                screen.updateInputFields(payload.area(), payload.offset());
            }
        });
    }

}
