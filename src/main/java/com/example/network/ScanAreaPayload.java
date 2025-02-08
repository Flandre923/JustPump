package com.example.network;

import com.example.ExampleMod;
import com.example.blockentitiy.PumpBlockEntity;
import com.example.blockentitiy.PumpMode;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record ScanAreaPayload(BlockPos pos, PumpMode mode, int xRadius,
                              int yExtend, int zRadius, int xOffset, int yOffset, int zOffset) implements CustomPacketPayload {

    public static final Type<ScanAreaPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(ExampleMod.MODID, "pump_scan"));

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
    public static final StreamCodec<FriendlyByteBuf, ScanAreaPayload> STREAM_CODEC =
            CustomPacketPayload.codec(ScanAreaPayload::write, ScanAreaPayload::new);

    // 反序列化构造函数
    private ScanAreaPayload(FriendlyByteBuf buf) {
        this(
                buf.readBlockPos(),
                buf.readEnum(PumpMode.class),
                buf.readVarInt(),
                buf.readVarInt(),
                buf.readVarInt(),
                buf.readVarInt(),
                buf.readVarInt(),
                buf.readVarInt()
        );
    }
    // 序列化方法
    private void write(FriendlyByteBuf buf) {
        buf.writeBlockPos(pos);
        buf.writeEnum(mode);
        buf.writeVarInt(xRadius);
        buf.writeVarInt(yExtend);
        buf.writeVarInt(zRadius);
        buf.writeVarInt(xOffset);
        buf.writeVarInt(yOffset);
        buf.writeVarInt(zOffset);
    }


    public static void handleScanPacket(ScanAreaPayload payload, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            if (ctx.player().level().getBlockEntity(payload.pos()) instanceof PumpBlockEntity be) {
                be.handleArea(payload.mode, payload.xRadius(), payload.yExtend(), payload.zRadius(),
                        payload.xOffset(), payload.yOffset(), payload.zOffset());
            }
        });
    }
}
