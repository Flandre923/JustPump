package com.example.blockentitiy.help;

import net.minecraft.nbt.CompoundTag;

public class SimpleSwitch {
    private boolean isActive;

    // 初始化默认状态
    public SimpleSwitch() {
        this(false);
    }

    public SimpleSwitch(boolean initialState) {
        this.isActive = initialState;
    }

    // 查询状态
    public boolean isActive() {
        return this.isActive;
    }

    // 设置状态
    public void setActive(boolean active) {
        this.isActive = active;
    }

    // 切换状态
    public void toggle() {
        this.isActive = !this.isActive;
    }

    // 序列化到NBT
    public CompoundTag toNBT() {
        CompoundTag tag = new CompoundTag();
        tag.putBoolean("active", this.isActive);
        return tag;
    }

    // 从NBT反序列化
    public void fromNBT(CompoundTag tag) {
        this.isActive = tag.getBoolean("active");
    }
}
