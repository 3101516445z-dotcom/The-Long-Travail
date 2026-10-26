package com.thelongtravail.mixin;

import com.thelongtravail.flourishing.ImmortalBud;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ItemStack.class)
public abstract class ImmortalBudHurtMixin {
    @Inject(method = "hurt", at = @At("HEAD"), cancellable = true)
    private void travail$preventBreak(
            int amount,
            RandomSource random,
            ServerPlayer player,
            CallbackInfoReturnable<Boolean> cir) {
        // hurt() 根据局部计算值判断破损，仅阻止损伤写入不足以保护物品。
        if (amount > 0 && ImmortalBud.marked((ItemStack) (Object) this)) cir.setReturnValue(false);
    }
}
