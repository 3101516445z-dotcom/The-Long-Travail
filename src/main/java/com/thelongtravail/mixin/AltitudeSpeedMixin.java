package com.thelongtravail.mixin;

import com.thelongtravail.data.AltitudePenalty;
import com.thelongtravail.registry.ModRegistry;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Boundless Malice high-altitude slowdown.
 *
 * Player#getSpeed is read by ordinary ground/swimming movement (LivingEntity#travel and
 * LivingEntity#getFrictionInfluencedSpeed both call it virtually), and it is where
 * LivingEntity#getAttributeValue(MOVEMENT_SPEED) lands. Scaling the returned value keeps the
 * penalty out of the attribute map entirely, so mods that neutralize movement speed penalties
 * cannot reach it, while their own speed bonuses still apply normally.
 * Elytra movement is handled separately by AltitudeGlidingMixin.
 */
@Mixin(Player.class)
public abstract class AltitudeSpeedMixin implements AltitudePenalty.Cache {
    @Unique private boolean travail$altitudeValid;
    @Unique private int travail$altitudeTick;
    @Unique private long travail$altitudeRevision;
    @Unique private Object travail$altitudeLevel;
    @Unique private float travail$altitudeValue;

    @Override
    public float travail$altitudeFactor() {
        Player player = (Player) (Object) this;
        long revision = AltitudePenalty.revision(player);
        if (!travail$altitudeValid || travail$altitudeTick != player.tickCount
                || travail$altitudeRevision != revision || travail$altitudeLevel != player.level()) {
            travail$altitudeValue = AltitudePenalty.compute(player);
            travail$altitudeTick = player.tickCount;
            travail$altitudeRevision = revision;
            travail$altitudeLevel = player.level();
            travail$altitudeValid = true;
        }
        return travail$altitudeValue;
    }

    @Override
    public void travail$invalidateAltitude() {
        travail$altitudeValid = false;
        travail$altitudeLevel = null;
    }

    @Inject(method = "getSpeed", at = @At("RETURN"), cancellable = true)
    private void travail$applyAltitudeSlow(CallbackInfoReturnable<Float> cir) {
        Player player = (Player) (Object) this;
        // Keep both corrections in one callback: either cancellable RETURN injection
        // could otherwise return before the other callback gets to apply its rule.
        if (player.hasEffect(ModRegistry.STIFF.get())) {
            cir.setReturnValue(0.0F);
            return;
        }
        float factor = AltitudePenalty.factor(player);
        if (factor < 1.0F) cir.setReturnValue(cir.getReturnValueF() * factor);
    }
}
