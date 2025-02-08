package com.example.menu;

import com.example.blockentitiy.PumpBlockEntity;
import com.example.blockentitiy.PumpMode;
import com.example.network.SyncAreaDataPayload;
import com.example.reg.MenuTypeRegister;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.neoforged.neoforge.network.PacketDistributor;

import javax.annotation.Nullable;

public class PumpMenu extends AbstractContainerMenu {
    private final BlockPos pos;
    private final Player player;
    @Nullable
    private final PumpBlockEntity blockEntity;

    // 客户端使用的构造方法
    public PumpMenu(int containerId, Inventory playerInventory, BlockPos pos) {
        this(containerId, playerInventory.player, pos,
                playerInventory.player.level().getBlockEntity(pos) instanceof PumpBlockEntity be ? be : null);
        //addPlayerInventory(playerInventory);
    }
    // 服务端使用的构造方法
    public PumpMenu(int containerId, Player player, BlockPos pos, PumpBlockEntity blockEntity) {
        super(MenuTypeRegister.PUMP_MENU.get(), containerId);
        this.pos = pos;
        this.player = player;
        this.blockEntity = blockEntity;
    }
    private void addPlayerInventory(Inventory playerInventory) {
        // 添加玩家主物品栏
        for (int row = 0; row < 3; ++row) {
            for (int col = 0; col < 9; ++col) {
                this.addSlot(new Slot(
                        playerInventory,
                        col + row * 9 + 9,
                        8 + col * 18,
                        84 + row * 18
                ));
            }
        }

        // 添加快捷栏
        for (int col = 0; col < 9; ++col) {
            this.addSlot(new Slot(
                    playerInventory,
                    col,
                    8 + col * 18,
                    142
            ));
        }
    }


    public PumpBlockEntity getBlockEntity() {
        if(!player.level().isClientSide)
        {
            if (blockEntity != null) {
                return blockEntity;
            }
            // 客户端回退逻辑
            Level level = player.level();
            if (level.getBlockEntity(pos) instanceof PumpBlockEntity be) {
                return be;
            }
            throw new IllegalStateException("Block entity not found at " + pos);
        }
        return blockEntity;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int i) {
        return null;
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }
    private boolean hasSentData = false;

    @Override
    public void broadcastChanges() {
        super.broadcastChanges();
        if (!hasSentData &&
                (blockEntity.getPumpMode() == PumpMode.EXTRACTING_RANGE || blockEntity.getPumpMode() == PumpMode.FILLING) &&
                !player.level().isClientSide)
        {
            blockEntity.getRangeParameters().ifPresent(params -> {
                PacketDistributor.sendToPlayer(
                        (ServerPlayer)player,
                        new SyncAreaDataPayload(params.first(), params.second())
                );
                hasSentData = true; // 标记已发送
            });
        }
    }
}
