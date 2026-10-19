package com.thelongtravail.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(net.minecraft.world.level.storage.loot.entries.LootTableReference.class)
public interface ExecutionLootReferenceAccessor {
    @Accessor("name")
    net.minecraft.resources.ResourceLocation travail$name();
}
