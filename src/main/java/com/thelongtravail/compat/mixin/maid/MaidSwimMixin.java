package com.thelongtravail.compat.mixin.maid;

import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.thelongtravail.compat.maid.MaidAccessoryRuntime;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(EntityMaid.class)
public abstract class MaidSwimMixin {
    @ModifyArg(
            method = "travel",
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Lcom/github/tartaricacid/touhoulittlemaid/entity/passive/EntityMaid;moveRelative(FLnet/minecraft/world/phys/Vec3;)V"),
            index = 0)
    private float travail$swim(float speed) {
        return speed * (float) (1 + MaidAccessoryRuntime.swimBonus((EntityMaid) (Object) this));
    }
}
