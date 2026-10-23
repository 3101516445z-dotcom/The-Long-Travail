package com.thelongtravail.mixin;

import com.thelongtravail.farreach.BabelProtection;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.world.entity.*;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Mob.class)
public abstract class BabelMobMixin {
    @Shadow private LivingEntity target;

    @WrapMethod(method = "setTarget")
    private void travail$target(LivingEntity next, Operation<Void> original) {
        Mob self = (Mob) (Object) this;
        if (BabelProtection.protects(self, next)) {
            if (BabelProtection.protects(self, target)) original.call((LivingEntity) null);
            if (BabelProtection.protects(self, target)) {
                target = null;
                self.getNavigation().stop();
            }
            return;
        }
        original.call(next);
        if (BabelProtection.protects(self, target)) target = null;
    }

    @Redirect(
            method = "setTarget",
            at =
                    @At(
                            value = "FIELD",
                            target =
                                    "Lnet/minecraft/world/entity/Mob;target:Lnet/minecraft/world/entity/LivingEntity;",
                            opcode = 181))
    private void travail$commit(Mob self, LivingEntity next) {
        if (!BabelProtection.protects(self, next)) target = next;
        else if (BabelProtection.protects(self, target)) target = null;
    }

    @Inject(method = "serverAiStep", at = @At("HEAD"))
    private void travail$clean(CallbackInfo ci) {
        BabelProtection.clean((Mob) (Object) this);
    }
}
