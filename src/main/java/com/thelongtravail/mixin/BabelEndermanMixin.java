package com.thelongtravail.mixin;

import com.thelongtravail.farreach.BabelProtection;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.EnderMan;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(EnderMan.class)
public abstract class BabelEndermanMixin {
    @Inject(method = "isLookingAtMe", at = @At("HEAD"), cancellable = true)
    private void travail$look(Player player, CallbackInfoReturnable<Boolean> cir) {
        if (BabelProtection.protects((EnderMan) (Object) this, player)) cir.setReturnValue(false);
    }

    @WrapMethod(method = "setTarget")
    private void travail$target(LivingEntity target, Operation<Void> original) {
        EnderMan self = (EnderMan) (Object) this;
        if (BabelProtection.protects(self, target)) {
            if (BabelProtection.protects(self, self.getTarget())) original.call((LivingEntity) null);
            return;
        }
        original.call(target);
        if (target != null && self.getTarget() == null) original.call((LivingEntity) null);
    }
}
