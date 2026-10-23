package com.thelongtravail.compat.mixin.quark;

import com.thelongtravail.registry.ModRegistry;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.violetmoon.quark.base.Quark;
import org.violetmoon.quark.content.tools.module.ColorRunesModule;

@Pseudo
@Mixin(targets = "org.violetmoon.quark.content.tools.module.ColorRunesModule", remap = false)
public abstract class DiaryRuneRecipeMixin {
    @Inject(method = "canHaveRune", at = @At("HEAD"), cancellable = true)
    private static void travail$diary(ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
        if (stack.is(ModRegistry.LONG_TRAVAIL.get())
                && Quark.ZETA.modules.isEnabled(ColorRunesModule.class)) cir.setReturnValue(true);
    }
}
