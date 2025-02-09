package com.example.network;

import com.example.ExampleMod;
import com.example.blockentitiy.PumpBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record ToggleRangePayload (BlockPos pos,int mode) implements CustomPacketPayload {
    // mode 1
    public static int TOGGLE = 1; //
    public static int CLOSE  = 2;

    public static final Type<ToggleRangePayload> TYPE = new Type(ResourceLocation.fromNamespaceAndPath(ExampleMod.MODID, "toggle_range"));
    public static StreamCodec<FriendlyByteBuf,ToggleRangePayload> STREAM_CODEC
            = CustomPacketPayload.codec(ToggleRangePayload::write, ToggleRangePayload::new);

    public ToggleRangePayload(FriendlyByteBuf buf) {
        this(buf.readBlockPos(),buf.readInt());
    }

    public void write(FriendlyByteBuf buf)
    {
        buf.writeBlockPos(pos);
        buf.writeInt(mode);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(ToggleRangePayload payload, IPayloadContext ctx)
    {
        ctx.enqueueWork(()->{
            BlockEntity blockEntity = ctx.player().level().getBlockEntity(payload.pos);
            if(blockEntity instanceof PumpBlockEntity pump)
            {
                if(payload.mode == TOGGLE)
                    pump.switchAreaDisplay();
                else if(payload.mode == CLOSE)
                    pump.closeDisplay();
            }
        });
    }
}
