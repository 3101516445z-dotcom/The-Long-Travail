package com.thelongtravail.network;

import net.minecraft.resources.ResourceLocation;

public record EffectNotice(ResourceLocation effect, Kind kind, long serial, long batch) {
    public enum Kind {
        GAIN,
        CLEAR,
        MALICE_CLEAR,
        EXPIRE,
        RESET,
        DIAGNOSE
    }

    public boolean gain() {
        return kind == Kind.GAIN;
    }

    public static java.util.function.Consumer<EffectNotice> receiver = notice -> {};
}
