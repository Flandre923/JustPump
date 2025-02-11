package flandre923.justpump.network;

import flandre923.justpump.JustPump;
import flandre923.justpump.blockentitiy.PumpBlockEntity;
import flandre923.justpump.blockentitiy.PumpMode;
import flandre923.justpump.client.ClientAreaOffsetHelper;
import flandre923.justpump.screen.PumpScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record ScanAreaPayload(BlockPos pos, PumpMode mode, int xRadius,
                              int yExtend, int zRadius, int xOffset, int yOffset, int zOffset) implements CustomPacketPayload {

    public static final Type<ScanAreaPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(JustPump.MODID, "pump_scan"));

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


    public static void handleScanPacket(ScanAreaPayload packet, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            ctx.enqueueWork(()->{
                if (ctx.player().level().getBlockEntity(packet.pos()) instanceof PumpBlockEntity be) {
                    // 只在模式匹配时更新
                    if (be.getPumpMode() == packet.mode()) {
                        if (!ctx.player().level().isClientSide){
                            be.handleArea(
                                    packet.mode(),
                                    packet.xRadius(), packet.yExtend(), packet.zRadius(),
                                    packet.xOffset(), packet.yOffset(), packet.zOffset()
                            );

                        }
                        if (ctx.player().level().isClientSide) {

                            ClientAreaOffsetHelper.setArea(new BlockPos(packet.xRadius(), packet.yExtend(), packet.zRadius()));
                            ClientAreaOffsetHelper.setOffset(new BlockPos(packet.xOffset(), packet.yOffset(), packet.zOffset()));

                            if(Minecraft.getInstance().screen instanceof PumpScreen screen){
                                screen.updateInputFields(
                                        new BlockPos(packet.xRadius(), packet.yExtend(), packet.zRadius()),
                                        new BlockPos(packet.xOffset(), packet.yOffset(), packet.zOffset())
                                );
                            }
                        }
                    }
                }
            });
        });
    }
}
