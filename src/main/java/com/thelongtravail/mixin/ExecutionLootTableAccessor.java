package com.thelongtravail.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(net.minecraft.world.level.storage.loot.LootTable.class)
public interface ExecutionLootTableAccessor {
    @Accessor("pools")
    java.util.List<net.minecraft.world.level.storage.loot.LootPool> travail$pools();
}
