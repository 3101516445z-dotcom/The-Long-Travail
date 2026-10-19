package com.thelongtravail.mixin;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.*;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.function.*;

@Mixin(LootPool.class)
public interface LootPoolAccess {
    @Accessor("compositeCondition")
    Predicate<LootContext> travail$condition();

    @Accessor("compositeFunction")
    BiFunction<ItemStack, LootContext, ItemStack> travail$function();
}
