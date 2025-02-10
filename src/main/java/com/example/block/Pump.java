package com.example.block;

import com.example.blockentitiy.PumpBlockEntity;
import com.example.blockentitiy.visiable.VisiableHelper;
import com.example.network.ModeUpdatePayload;
import com.example.network.ScanStartPayload;
import com.example.reg.BlockEntityRegister;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.network.PacketDistributor;

import javax.annotation.Nullable;

public class Pump extends BaseEntityBlock {
    public Pump() {
        super(Properties.ofFullCopy(Blocks.IRON_BLOCK));
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if(!level.isClientSide && player instanceof ServerPlayer serverPlayer)
        {
            BlockEntity be = level.getBlockEntity(pos);
            if(be instanceof PumpBlockEntity menuProvider)
            {
                serverPlayer.openMenu(menuProvider,buf->buf.writeBlockPos(pos));
                PacketDistributor.sendToPlayer(serverPlayer,new ModeUpdatePayload(pos,menuProvider.getPumpMode()));
                return InteractionResult.CONSUME;
            }
        }
        return InteractionResult.SUCCESS;
    }


    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if(!state.is(newState.getBlock()))
        {
            BlockEntity be = level.getBlockEntity(pos);
            if(be instanceof PumpBlockEntity pump)
            {
                level.updateNeighborsAt(pos,this);
                if(pump.getAreaDisplay().isActive())
                    pump.closeDisplay();

            }
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return null;
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> blockEntityType) {
        return createFurnaceTicker(level, blockEntityType, BlockEntityRegister.PUMP_BE.get());
    }

    @Nullable
    protected static <T extends BlockEntity> BlockEntityTicker<T> createFurnaceTicker(Level level, BlockEntityType<T> serverType, BlockEntityType<? extends PumpBlockEntity> clientType) {
        return level.isClientSide ? null : createTickerHelper(serverType, clientType, PumpBlockEntity::serverTick);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos blockPos, BlockState blockState) {
        return new PumpBlockEntity(blockPos,blockState);
    }
}
