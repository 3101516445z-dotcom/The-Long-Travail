package com.thelongtravail.mixin;

import com.thelongtravail.data.AltitudePenalty;
import com.thelongtravail.data.StiffState;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 步行和游泳会读取 Player#getSpeed；在返回值处缩放，避免将高空惩罚写入属性表，
 * 同时保留其他模组的速度加成。鞘翅移动另由 AltitudeGlidingMixin 处理。
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
        // 两项修正必须放在同一回调中，否则任一可取消的 RETURN 注入
        // 都可能提前返回，使另一回调的规则无法应用。
        if (StiffState.active(player)) {
            cir.setReturnValue(0.0F);
            return;
        }
        float factor = AltitudePenalty.factor(player);
        factor *= com.thelongtravail.abyss.RainState.speed(player);
        if (factor != 1.0F) cir.setReturnValue(cir.getReturnValueF() * factor);
    }
}
