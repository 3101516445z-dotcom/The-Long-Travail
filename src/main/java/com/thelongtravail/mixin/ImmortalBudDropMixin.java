package com.thelongtravail.mixin;

import com.thelongtravail.flourishing.ImmortalBud;

import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.item.ItemEntity;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ItemEntity.class)
public abstract class ImmortalBudDropMixin {
    @Shadow private int age;

    @Inject(method = "tick", at = @At("HEAD"))
    private void travail$preserveDrop(CallbackInfo ci) {
        if (age >= 0 && ImmortalBud.protectsDrop(((ItemEntity) (Object) this).getItem())) {
            // 保留正常拾取与合并行为，避免使用无限寿命哨兵值。
            age = -1;
        }
    }

    @Inject(method = "hurt", at = @At("HEAD"), cancellable = true)
    private void travail$protectDrop(
            DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        if ((source.is(DamageTypeTags.IS_FIRE) || source.is(DamageTypeTags.IS_EXPLOSION))
                && ImmortalBud.protectsDrop(((ItemEntity) (Object) this).getItem())) {
            cir.setReturnValue(false);
        }
    }
}
