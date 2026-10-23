package com.thelongtravail.compat.mixin.apotheosis;

import com.thelongtravail.compat.apotheosis.AffixNameStyle;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;

// 神化使用优先级 500；此注入须在其生成完整名称后执行。
@Mixin(value = ItemStack.class, priority = 400)
public abstract class AffixNameMixin {
    @WrapMethod(method = "getHoverName")
    private Component travail$affixName(Operation<Component> original) {
        return AffixNameStyle.decorate((ItemStack) (Object) this, original.call());
    }
}
