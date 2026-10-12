package com.thelongtravail.boundless;

import com.thelongtravail.api.TravailStateApi;
import com.thelongtravail.config.BoundlessItemsConfig;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;

public final class StarVoiceState {
    public static final String RECEIVED_INTERVAL = "LongTravailStarReceivedInterval";
    public static final String COOLDOWN = "LongTravailStarCooldown",
            INTERVAL = "LongTravailStarInterval";

    public static void received(LivingEntity victim, DamageSource source) {
        if (victim instanceof ServerPlayer p) {
            var cause = source.getEntity() != null ? source.getEntity() : source.getDirectEntity();
            // 发射器等投射物没有射手，但仍有实体来源；缺少射手信息时也排除可追溯的自有投射物。
            boolean self =
                    cause == p
                            || source.getEntity() == null
                                    && cause != null
                                    && p.getUUID().equals(TimeStopExemptions.owner(cause));
            if (cause != null && !self) roll(p, false);
        }
    }

    public static void confirmed(LivingEntity victim, DamageSource source) {
        if (source.getEntity() instanceof ServerPlayer p && p != victim && !victim.isSpectator())
            roll(p, true);
    }

    private static void roll(ServerPlayer p, boolean attacking) {
        if (!p.isAlive()
                || p.isSpectator()
                || TimeStopManager.frozen(p)
                || !BoundlessEquipment.equipped(p, false)
                || TimeStopManager.locked(p.getUUID())) return;
        boolean witness = TravailStateApi.hasBoundlessWitness(p);
        if (attacking != witness) return;
        if (witness
                && (p.getPersistentData().getInt(COOLDOWN) > 0
                        || p.getPersistentData().getInt(INTERVAL) > 0)) return;
        if (!witness && p.getPersistentData().getInt(RECEIVED_INTERVAL) > 0) return;
        p.getPersistentData()
                .putInt(
                        witness ? INTERVAL : RECEIVED_INTERVAL,
                        BoundlessItemsConfig.ticks(
                                witness ? "star.witnessInterval" : "star.maliceInterval"));
        if (p.getRandom().nextDouble()
                < BoundlessItemsConfig.get(witness ? "star.witnessChance" : "star.maliceChance"))
            TimeStopManager.get(p.serverLevel()).start(p, witness);
    }

    public static void tick(ServerPlayer p) {
        if (!TimeStopManager.locked(p.getUUID())) decrement(p, COOLDOWN);
        decrement(p, INTERVAL);
        decrement(p, RECEIVED_INTERVAL);
    }

    public static void decrement(ServerPlayer p, String key) {
        int c = p.getPersistentData().getInt(key);
        if (c > 0) p.getPersistentData().putInt(key, c - 1);
    }

    private StarVoiceState() {}
}
