package com.thelongtravail.compat.fluid;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;

public final class PerfectFluidBackpacks {
    public static boolean hasReferences(Tag data) {
        return FluidCopyGraph.hasReferences(data);
    }

    public static void reserveIds(Tag data) {
        if (data instanceof CompoundTag compound) FluidCopyGraph.reserveIds(compound);
    }

    private PerfectFluidBackpacks() {}
}
