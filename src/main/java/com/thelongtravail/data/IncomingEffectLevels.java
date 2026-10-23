package com.thelongtravail.data;

import com.thelongtravail.TravailAspect;
import com.thelongtravail.abyss.MidnightLafite;
import com.thelongtravail.config.TravailConfig;
import com.thelongtravail.helper.TravailCurios;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.*;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.registries.ForgeRegistries;

public final class IncomingEffectLevels {
    public static int adjusted(LivingEntity entity, MobEffect effect, int original) {
        if (entity.level().isClientSide
                || effect == com.thelongtravail.registry.ModRegistry.MISTLETOE_EFFECT.get())
            return original;
        int bonus = MidnightLafite.levels(entity, effect);
        var id = ForgeRegistries.MOB_EFFECTS.getKey(effect);
        if (entity instanceof ServerPlayer player
                && effect.getCategory() == MobEffectCategory.BENEFICIAL) {
            var diary = TravailCurios.stack(player);
            if (!diary.isEmpty()
                    && LongTravailData.hasWitness(diary, TravailAspect.FAR_REACH)
                    && (id == null || !RuntimePools.current().levelBonusBlacklist().contains(id)))
                bonus += TravailConfig.FAR_WITNESS_LEVEL_BONUS.get();
        }
        if (bonus == 0) return original;
        int cap =
                id == null
                        ? TravailConfig.FAR_WITNESS_FALLBACK_MAX_LEVEL.get()
                        : RuntimePools.current()
                                .maxLevels()
                                .getOrDefault(
                                        id, TravailConfig.FAR_WITNESS_FALLBACK_MAX_LEVEL.get());
        return Math.max(original, (int) Math.min((long) original + bonus, (long) cap - 1));
    }

    private IncomingEffectLevels() {}
}
