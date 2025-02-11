package flandre923.justpump.network;

import flandre923.justpump.JustPump;
import flandre923.justpump.blockentitiy.PumpBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record ToggleRangePayload (BlockPos pos,int mode) implements CustomPacketPayload {
    // mode 1
    public static int TOGGLE = 1; //
    public static int CLOSE  = 2;

    public static final Type<ToggleRangePayload> TYPE = new Type(ResourceLocation.fromNamespaceAndPath(JustPump.MODID, "toggle_range"));
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
                syncToClient(ctx.player().level(),payload.pos);
            }
        });
    }

    public static void syncToClient(Level level, BlockPos pos) {
        if (level instanceof ServerLevel serverLevel) {
            serverLevel.getPlayers(player ->
                    player.distanceToSqr(pos.getX(),pos.getY(),pos.getZ()) < 64
            ).forEach(player -> {
                if(level.getBlockEntity(pos) instanceof PumpBlockEntity be){
                    PacketDistributor.sendToPlayer(player,new SyncRangeDisplayPacket(
                            pos,be.getAreaDisplay().isActive()
                            ));
                }
            });
        }
    }
}
