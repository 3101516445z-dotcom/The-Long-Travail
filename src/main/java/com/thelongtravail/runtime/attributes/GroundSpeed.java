package com.thelongtravail.runtime.attributes;

import com.thelongtravail.abyss.rain.RainState;
import com.thelongtravail.combat.EffectCombatRules;
import com.thelongtravail.registry.ModAttributes;
import com.thelongtravail.runtime.effects.StiffState;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.player.Player;

import java.util.UUID;

public final class GroundSpeed {
    public interface PropulsionRead {
        boolean travail$deferLimit();
    }

    public static void update(LivingEntity entity, UUID source, double bonus, String name) {
        var pool = entity.getAttribute(ModAttributes.GROUND_SPEED_BONUS.get());
        if (pool == null) return;
        var previous = pool.getModifier(source);
        if (previous != null && previous.getAmount() == bonus) return;
        if (previous != null) pool.removeModifier(source);
        if (bonus != 0)
            pool.addTransientModifier(
                    new AttributeModifier(
                            source, name, bonus, AttributeModifier.Operation.ADDITION));
    }

    public static float apply(LivingEntity entity, float original) {
        if (StiffState.active(entity)) return 0;
        var pool = entity.getAttribute(ModAttributes.GROUND_SPEED_BONUS.get());
        double bonus = pool == null ? 0 : pool.getValue();
        if (entity instanceof Player player) bonus += RainState.speed(player) - 1.0;
        // 推进路径在 moveRelative 统一应用限速，读取输入速度时跳过这一层。
        float limit =
                entity instanceof PropulsionRead read && read.travail$deferLimit()
                        ? 1
                        : limit(entity);
        return (float) (original * Math.max(0, 1 + bonus) * limit);
    }

    public static float limit(LivingEntity entity) {
        float altitude = entity instanceof Player player ? AltitudePenalty.factor(player) : 1;
        return EffectCombatRules.revival(entity) ? altitude * .5F : altitude;
    }

    private GroundSpeed() {}
}
