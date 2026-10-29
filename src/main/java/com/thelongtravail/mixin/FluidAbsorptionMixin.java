package com.thelongtravail.mixin;

import com.thelongtravail.helper.TrueDamage;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(Player.class)
public abstract class FluidAbsorptionMixin {
    @Redirect(
            method = "actuallyHurt",
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Lnet/minecraft/world/entity/player/Player;getAbsorptionAmount()F",
                            ordinal = 0))
    private float theLongTravail$fluidBypassesAbsorption(
            Player player, DamageSource source, float amount) {
        return (source.is(TrueDamage.FLUID)
                        || source.is(com.thelongtravail.combat.DirectStrikeDamage.TYPE))
                ? 0F
                : player.getAbsorptionAmount();
    }
}
