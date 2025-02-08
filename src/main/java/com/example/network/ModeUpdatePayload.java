package com.example.network;

import com.example.blockentitiy.PumpBlockEntity;
import com.example.blockentitiy.PumpMode;
import com.example.screen.PumpScreen;
import io.netty.buffer.ByteBuf;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record ModeUpdatePayload(BlockPos pos, PumpMode mode) implements CustomPacketPayload {
    public static final Type<ModeUpdatePayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath("examplemod", "mode_update"));

    public static final StreamCodec<ByteBuf, ModeUpdatePayload> STREAM_CODEC =
            StreamCodec.composite(
                    BlockPos.STREAM_CODEC,
                    ModeUpdatePayload::pos,
                    ByteBufCodecs.fromCodec(PumpMode.CODEC),
                    ModeUpdatePayload::mode,
                    ModeUpdatePayload::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }


    public static void handleClientSide(ModeUpdatePayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (Minecraft.getInstance().level != null) {
                BlockEntity be = Minecraft.getInstance().level.getBlockEntity(payload.pos());
                if (be instanceof PumpBlockEntity pump) {
                    pump.setPumpMode(payload.mode());
                    // 请求界面重绘
                    if (Minecraft.getInstance().screen instanceof PumpScreen screen) {
                        screen.updateDisplay();
                    }
                }
            }
        });
    }


    public static void handleServerSide(ModeUpdatePayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            ServerPlayer player =(ServerPlayer) context.player();
            if (player != null) {
                player.getServer().execute(() -> {
                    if (player.level().getBlockEntity(payload.pos()) instanceof PumpBlockEntity pump) {
                        // 切换模式
                        pump.switchMode();
                        // 发送同步包给所有客户端
                        PacketDistributor.sendToPlayersTrackingChunk(
                                player.serverLevel(),
                                new ChunkPos(payload.pos()),
                                new ModeUpdatePayload(payload.pos(), pump.getPumpMode())
                        );
                    }
                });
            }
        }).exceptionally(e -> {
            context.disconnect(Component.literal("Mode change failed: " + e.getMessage()));
            return null;
        });
    }
}