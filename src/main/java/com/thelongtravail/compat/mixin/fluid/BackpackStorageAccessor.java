package com.thelongtravail.compat.mixin.fluid;

import net.minecraft.nbt.CompoundTag;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Map;
import java.util.UUID;

@org.spongepowered.asm.mixin.Pseudo
@Mixin(targets = "net.p3pp3rf1y.sophisticatedbackpacks.backpack.BackpackStorage", remap = false)
public interface BackpackStorageAccessor {
    @Accessor("backpackContents")
    Map<UUID, CompoundTag> travail$contents();
}
