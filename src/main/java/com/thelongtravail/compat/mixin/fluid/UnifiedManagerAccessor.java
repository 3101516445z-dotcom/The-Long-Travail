package com.thelongtravail.compat.mixin.fluid;

import com.huanghuang.rsintegration.storage.disk.persistence.DiskFileStore;
import com.huanghuang.rsintegration.storage.disk.rs.UnifiedDiskManager;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Map;
import java.util.UUID;

@org.spongepowered.asm.mixin.Pseudo
@Mixin(value = UnifiedDiskManager.class, remap = false)
public interface UnifiedManagerAccessor {
    @Accessor("files")
    DiskFileStore travail$files();

    @Accessor("entries")
    Map<UUID, UnifiedDiskManager.Entry> travail$entries();

    @Accessor("activeBackups")
    int travail$activeBackups();
}
