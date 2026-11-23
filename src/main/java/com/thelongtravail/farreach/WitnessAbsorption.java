package com.thelongtravail.farreach;

import com.thelongtravail.TravailAspect;
import com.thelongtravail.equipment.TravailCurios;
import com.thelongtravail.journey.LongTravailData;

import net.minecraft.server.level.ServerPlayer;

@net.minecraftforge.fml.common.Mod.EventBusSubscriber(modid = "the_long_travail")
public final class WitnessAbsorption {
    @net.minecraftforge.eventbus.api.SubscribeEvent
    public static void onHealthLoss(com.thelongtravail.combat.DamageHealthLossEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) grant(player, event.amount());
    }

    private static final String KEY = "LongTravailFarReachAbsorption";
    private static final float CAP = 40F;

    public static void grant(ServerPlayer player, double healthLost) {
        var diary = TravailCurios.stack(player);
        if (!player.isAlive()
                || diary.isEmpty()
                || !LongTravailData.hasWitness(diary, TravailAspect.FAR_REACH)) return;
        float total = player.getAbsorptionAmount();
        float owned = Math.min(total, player.getPersistentData().getFloat(KEY));
        float add = (float) Math.min(CAP - owned, Math.ceil(healthLost / 2));
        if (add <= 0) return;
        player.setAbsorptionAmount(total + add);
        save(player, owned + Math.min(add, Math.max(0, player.getAbsorptionAmount() - total)));
    }

    public static void spent(ServerPlayer player, float ownedBefore, float loss) {
        if (ownedBefore > 0 && loss > 0) save(player, Math.max(0, ownedBefore - loss));
    }

    public static float balance(ServerPlayer player) {
        return player.getPersistentData().getFloat(KEY);
    }

    public static void clamp(ServerPlayer player, float amount) {
        if (balance(player) > amount) save(player, Math.max(0, amount));
    }

    public static void reclaim(ServerPlayer player) {
        float owned = balance(player);
        if (owned <= 0) return;
        save(player, 0);
        player.setAbsorptionAmount(Math.max(0, player.getAbsorptionAmount() - owned));
    }

    private static void save(ServerPlayer player, float owned) {
        if (owned > 0) player.getPersistentData().putFloat(KEY, owned);
        else player.getPersistentData().remove(KEY);
    }

    private WitnessAbsorption() {}
}
