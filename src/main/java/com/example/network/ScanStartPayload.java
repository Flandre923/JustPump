package com.example.network;

import com.example.ExampleMod;
import com.example.blockentitiy.PumpBlockEntity;
import com.example.blockentitiy.PumpMode;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.stream.Stream;

public record ScanStartPayload (PumpMode mode,BlockPos pos) implements CustomPacketPayload {
    public static final Type<ScanStartPayload> TYPE = new Type(ResourceLocation.fromNamespaceAndPath(ExampleMod.MODID, "scan_start"));

    public static final StreamCodec STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.fromCodec(PumpMode.CODEC),
                    ScanStartPayload::mode,
                    BlockPos.STREAM_CODEC,
                    ScanStartPayload::pos,
                    ScanStartPayload::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }


    public static void handleScanStartPayload(ScanStartPayload payload, IPayloadContext ctx){
        ctx.enqueueWork(()->{
            if (ctx.player().level().getBlockEntity(payload.pos()) instanceof PumpBlockEntity be) {
                be.handleScan(payload.mode);
            }
        });
    }

}
