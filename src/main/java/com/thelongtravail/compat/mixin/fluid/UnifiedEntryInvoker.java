package com.thelongtravail.compat.mixin.fluid;

import com.huanghuang.rsintegration.storage.disk.rs.UnifiedDiskManager;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

import java.util.UUID;

@org.spongepowered.asm.mixin.Pseudo
@Mixin(value = UnifiedDiskManager.Entry.class, remap = false)
public interface UnifiedEntryInvoker {
    @Invoker("<init>")
    static UnifiedDiskManager.Entry travail$create(UUID id) {
        throw new AssertionError();
    }
}
