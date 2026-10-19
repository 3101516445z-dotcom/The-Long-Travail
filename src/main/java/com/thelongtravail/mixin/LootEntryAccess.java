package com.thelongtravail.mixin;

import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.entries.LootPoolEntryContainer;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.function.Predicate;

@Mixin(LootPoolEntryContainer.class)
public interface LootEntryAccess {
    @Accessor("compositeCondition")
    Predicate<LootContext> travail$condition();
}
