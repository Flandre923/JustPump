// SyncScanStatePacket.java
package com.example.network;

import com.example.ExampleMod;
import com.example.client.ClientProxy;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record SyncScanStatePacket(BlockPos pos, boolean scanning, boolean scanComplete) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<SyncScanStatePacket> TYPE = 
        new Type<>(ResourceLocation.fromNamespaceAndPath(ExampleMod.MODID, "sync_scan_state"));

    public static final StreamCodec<FriendlyByteBuf, SyncScanStatePacket> STREAM_CODEC =
            StreamCodec.composite(
                    BlockPos.STREAM_CODEC,
                    SyncScanStatePacket::pos,
                    ByteBufCodecs.BOOL,
                    SyncScanStatePacket::scanning,
                    ByteBufCodecs.BOOL,
                    SyncScanStatePacket::scanComplete,
                    SyncScanStatePacket::new
            );

    public static void handleClient(SyncScanStatePacket packet, IPayloadContext ctx) {
        ctx.enqueueWork(() -> ClientProxy.handleScanStateSync(packet));
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}