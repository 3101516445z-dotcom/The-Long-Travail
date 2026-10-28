package com.thelongtravail.farreach;

import com.thelongtravail.api.TravailStateApi;
import com.thelongtravail.config.FarReachItemsConfig;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.*;

public final class FarReachCombat {
    public static void ignite(net.minecraft.world.entity.LivingEntity target, DamageSource source) {
        if (!com.thelongtravail.config.ModeEffectRules.ICARUS_FIRE.applies(target)
                || target.level().isClientSide
                || target.fireImmune()
                || FarReachItemsConfig.IGNITE_SECONDS.get() == 0) return;
        if (source.getEntity() instanceof ServerPlayer player
                && player != target
                && FarReachEquipment.equipped(player, false))
            target.setSecondsOnFire(FarReachItemsConfig.IGNITE_SECONDS.get());
    }

    public static double bonus(ServerPlayer p) {
        if (!FarReachEquipment.equipped(p, false)) return 0;
        if (TravailStateApi.hasFarReachWitness(p)) return IcarusCombat.witnessBonus(p);
        if (!TravailStateApi.hasFarReachMalice(p)) return 0;
        double sun = IcarusEnvironment.sunlight(p.level().getDayTime());
        return sun == 0 ? 0 : IcarusCombat.maliceBonus(p, sun);
    }

    public static boolean immune(ServerPlayer p, DamageSource source) {
        if (!FarReachEquipment.equipped(p, false)) return false;
        if (IcarusCombat.baseImmune(source)) return true;
        return TravailStateApi.hasFarReachWitness(p) && IcarusCombat.witnessImmune(source);
    }

    private FarReachCombat() {}
}
