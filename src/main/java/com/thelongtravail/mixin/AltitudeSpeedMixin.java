package com.thelongtravail.mixin;

import com.thelongtravail.data.AltitudePenalty;
import com.thelongtravail.data.StiffState;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;

// 步行和游泳会读取 Player#getSpeed；在返回值处缩放，避免将高空惩罚写入属性表，
// 同时保留其他模组的速度加成。鞘翅移动另由 AltitudeGlidingMixin 处理。
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
        // 在整个方法返回后组合速度，覆盖其他模组注入的提前返回路径。
        // 僵硬、无垠和駅仍在同一处应用，各自仅计算一次。
        if (StiffState.active(player)) return 0.0F;
        float factor = AltitudePenalty.factor(player);
        factor *= com.thelongtravail.abyss.RainState.speed(player);
        return original * factor;
    }
}
