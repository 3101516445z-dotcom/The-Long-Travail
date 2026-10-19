package com.thelongtravail.mixin;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.*;
import net.minecraft.world.level.storage.loot.entries.LootPoolSingletonContainer;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.function.*;

@Mixin(LootPoolSingletonContainer.class)
public interface LootSingletonAccess {
    @Accessor("weight")
    int travail$weight();

    @Accessor("compositeFunction")
    BiFunction<ItemStack, LootContext, ItemStack> travail$function();
}
