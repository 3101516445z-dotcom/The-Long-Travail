package com.thelongtravail.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(net.minecraft.world.level.storage.loot.entries.CompositeEntryBase.class)
public interface ExecutionLootCompositeAccessor {
    @Accessor("children")
    net.minecraft.world.level.storage.loot.entries.LootPoolEntryContainer[] travail$children();
}
