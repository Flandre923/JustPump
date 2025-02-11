package com.example.network;

import com.example.ExampleMod;
import com.example.client.ClientProxy;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.stream.Stream;

public record SyncRangeDisplayPacket(BlockPos pos,boolean isActive) implements CustomPacketPayload {
    public static Type<SyncRangeDisplayPacket> TYPE  = new Type(ResourceLocation.fromNamespaceAndPath(ExampleMod.MODID,"sync_range_display_packet"));

    public static StreamCodec<FriendlyByteBuf, SyncRangeDisplayPacket>    STREAM_CODEC =
            CustomPacketPayload.codec(SyncRangeDisplayPacket::write,SyncRangeDisplayPacket::new);

    public SyncRangeDisplayPacket(FriendlyByteBuf buf)
    {
        this(buf.readBlockPos(),buf.readBoolean());
    }
    public void write(FriendlyByteBuf buf)
    {
        buf.writeBlockPos(pos);
        buf.writeBoolean(isActive);
    }
    public static void handleClient(SyncRangeDisplayPacket packet, IPayloadContext ctx) {
        ctx.enqueueWork(()->{
            ClientProxy.handleRangeDisplaySync(packet);
        });
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
