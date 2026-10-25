package com.thelongtravail.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.thelongtravail.item.NameColoring;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import org.spongepowered.asm.mixin.Mixin;

/** 包裹神化（500）及已有词缀适配（400），在完整名称返回后应用施色。 */
@Mixin(value = ItemStack.class, priority = 300)
public abstract class ColoringNameMixin {
    @WrapMethod(method = "getHoverName")
    private Component travail$coloringName(Operation<Component> original) {
        return NameColoring.decorate((ItemStack) (Object) this, original.call());
    }
}
