package com.example.network;

import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.DirectionalPayloadHandler;
import net.neoforged.neoforge.network.registration.HandlerThread;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

@EventBusSubscriber(bus = EventBusSubscriber.Bus.MOD)
public class NetWorkHandler {

    @SubscribeEvent
    public static void registerPayLoads(RegisterPayloadHandlersEvent event)
    {
        PayloadRegistrar registrar = event.registrar("1.0").executesOn(HandlerThread.MAIN);

        registrar.playBidirectional(
                ModeUpdatePayload.TYPE,
                ModeUpdatePayload.STREAM_CODEC,
                new DirectionalPayloadHandler<>(
                        ModeUpdatePayload::handleClientSide,
                        ModeUpdatePayload::handleServerSide
                )
        );

        registrar.playBidirectional(ScanAreaPayload.TYPE, ScanAreaPayload.STREAM_CODEC, ScanAreaPayload::handleScanPacket);
        registrar.playToClient(SyncAreaDataPayload.TYPE,SyncAreaDataPayload.STREAM_CODEC,SyncAreaDataPayload::handleScanPacket);
        registrar.playToServer(ScanStartPayload.TYPE,ScanStartPayload.STREAM_CODEC,ScanStartPayload::handleScanStartPayload);
        registrar.playToServer(ToggleRangePayload.TYPE,ToggleRangePayload.STREAM_CODEC,ToggleRangePayload::handle);
        registrar.playToClient(SyncRangeDisplayPacket.TYPE,SyncRangeDisplayPacket.STREAM_CODEC,SyncRangeDisplayPacket::handleClient);
        registrar.playToClient(SyncScanStatePacket.TYPE, SyncScanStatePacket.STREAM_CODEC, SyncScanStatePacket::handleClient);

    }
}
