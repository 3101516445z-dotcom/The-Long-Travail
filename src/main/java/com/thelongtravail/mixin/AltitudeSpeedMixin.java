package com.thelongtravail.mixin;

import com.thelongtravail.data.AltitudePenalty;
import com.thelongtravail.data.StiffState;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;

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
        if (!travail$altitudeValid
                || travail$altitudeTick != player.tickCount
                || travail$altitudeRevision != revision
                || travail$altitudeLevel != player.level()) {
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

    @WrapMethod(method = "getSpeed")
    private float travail$applyAltitudeSlow(Operation<Float> operation) {
        float original = operation.call();
        Player player = (Player) (Object) this;
        if (StiffState.active(player)) return 0.0F;
        float factor = AltitudePenalty.factor(player);
        factor *= com.thelongtravail.abyss.RainState.speed(player);
        return original * factor;
    }
}
