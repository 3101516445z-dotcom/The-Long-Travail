package com.thelongtravail.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(net.minecraft.world.level.storage.loot.entries.LootItem.class)
public interface ExecutionLootItemAccessor {
    @Accessor("item")
    net.minecraft.world.item.Item travail$item();
}
