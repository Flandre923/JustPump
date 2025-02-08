package com.example.blockentitiy;

import com.example.ExampleMod;
import com.mojang.serialization.Codec;
import net.minecraft.network.chat.Component;

public enum PumpMode {
    EXTRACTING_RANGE("range"),
    EXTRACTING_AUTO("auto"),
    FILLING("filling");

    private final String name;

    PumpMode(String name)
    {
        this.name = name;
    }

    public static final Codec<PumpMode> CODEC = Codec.STRING.xmap(
            name -> valueOf(name.toUpperCase()),
            mode -> mode.name().toLowerCase()
    );

    public Component getDisplayName()
    {
        return Component.translatable("mode." + ExampleMod.MODID +"." + name);
    }

    public PumpMode next()
    {
        return values()[(ordinal() +1)%values().length];
    }

    public boolean isExtracting() {
        return this == EXTRACTING_RANGE || this == EXTRACTING_AUTO;
    }
}
