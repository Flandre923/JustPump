package com.example.network;

import com.example.ExampleMod;
import com.example.blockentitiy.PumpBlockEntity;
import com.example.client.ClientEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

// SyncAreaDataPayload.java
public record SyncAreaDataPayload(BlockPos area, BlockPos offset) implements CustomPacketPayload {
    public static final Type TYPE = new Type(ResourceLocation.fromNamespaceAndPath(ExampleMod.MODID,"sync_area_data"));

    public static StreamCodec<FriendlyByteBuf,SyncAreaDataPayload> STREAM_CODEC =
            CustomPacketPayload.codec(SyncAreaDataPayload::write, SyncAreaDataPayload::new);

    public SyncAreaDataPayload(FriendlyByteBuf buf) {
        this(buf.readBlockPos(), buf.readBlockPos());
    }

    public void write(FriendlyByteBuf buf) {
        buf.writeBlockPos(area());
        buf.writeBlockPos(offset());
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handleScanPacket(SyncAreaDataPayload payload, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            ClientEvents.handleAreaDataSync(payload);
        });
    }
}
