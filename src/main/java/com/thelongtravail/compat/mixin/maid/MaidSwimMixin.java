package com.thelongtravail.compat.mixin.maid;

import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.thelongtravail.compat.maid.MaidAccessoryRuntime;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

// 女仆主动游泳分支绕过了原版移动方法中的 Forge 游泳倍率，此处仅补上花卉加成。
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
