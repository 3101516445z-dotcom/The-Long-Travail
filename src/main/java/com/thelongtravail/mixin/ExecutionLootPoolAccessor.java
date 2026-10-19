package com.thelongtravail.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(net.minecraft.world.level.storage.loot.LootPool.class)
public interface ExecutionLootPoolAccessor {
    @Accessor("entries")
    net.minecraft.world.level.storage.loot.entries.LootPoolEntryContainer[] travail$entries();
}
