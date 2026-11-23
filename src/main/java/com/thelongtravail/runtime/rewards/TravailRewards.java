package com.thelongtravail.runtime.rewards;


import com.thelongtravail.TravailAspect;
import com.thelongtravail.combat.DirectDeath;
import com.thelongtravail.common.weighted.WeightedTable;
import com.thelongtravail.config.TravailConfig;
import com.thelongtravail.equipment.TravailCurios;
import com.thelongtravail.journey.LongTravailData;
import com.thelongtravail.runtime.pools.RewardEntry;
import com.thelongtravail.runtime.pools.RuntimePools;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.monster.Phantom;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.player.ItemFishedEvent;
import net.minecraftforge.registries.ForgeRegistries;

public final class TravailRewards {

    public static void onFishing(ItemFishedEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        ItemStack travail = TravailCurios.stack(player);
        if (travail.isEmpty() || !LongTravailData.hasWitness(travail, TravailAspect.ABYSS)) return;
        if (player.getRandom().nextDouble() < TravailConfig.FISH_SPECIAL_CHANCE.get())
            spawnFishingReward(player, event);
    }

    public static void killNearbyPhantoms(ServerPlayer player) {
        int range = TravailConfig.PHANTOM_RANGE.get();
        double rangeSquared = (double) range * range;
        int attempts = 0;
        RewardDelivery queue = RewardDelivery.get(player.server);
        for (Phantom phantom :
                player.serverLevel()
                        .getEntitiesOfClass(
                                Phantom.class,
                                new net.minecraft.world.phys.AABB(
                                                player.position(), player.position())
                                        .inflate(range),
                                candidate ->
                                        candidate.isAlive()
                                                && candidate.distanceToSqr(player)
                                                        <= rangeSquared)) {
            if (!phantom.isAlive()) continue;
            if (attempts++ >= TravailConfig.PHANTOM_MAX_KILLS.get()) break;
            try (var reservation = queue.reserve()) {
                if (reservation == null) {
                    queue.warnFull(player.server);
                    break;
                }
                if (DirectDeath.tryPlayerKill(phantom, player))
                    queueReward(
                            reservation,
                            player.serverLevel(),
                            phantom.position(),
                            RuntimePools.current().phantom(),
                            player.getRandom());
            }
        }
    }

    static void spawnFishingReward(ServerPlayer player, ItemFishedEvent event) {
        Vec3 position = event.getHookEntity().position();
        spawnRewardAt(
                player.serverLevel(), position, RuntimePools.current().fish(), player.getRandom());
    }

    static void spawnRewardAt(
            ServerLevel level,
            Vec3 position,
            WeightedTable<RewardEntry> configured,
            RandomSource random) {
        RewardDelivery queue = RewardDelivery.get(level.getServer());
        try (var reservation = queue.reserve()) {
            if (reservation == null) {
                queue.warnFull(level.getServer());
                return;
            }
            queueReward(reservation, level, position, configured, random);
        }
    }

    static void queueReward(
            RewardDelivery.Reservation reservation,
            ServerLevel level,
            Vec3 position,
            WeightedTable<RewardEntry> configured,
            RandomSource random) {
        RewardEntry selected = configured.choose(random);
        if (selected == null) return;
        Item item = ForgeRegistries.ITEMS.getValue(selected.id());
        if (item == null) return;
        int count =
                selected.minCount() + random.nextInt(selected.maxCount() - selected.minCount() + 1);
        reservation.submit(level, position, new ItemStack(item), count);
    }

    private TravailRewards() {}
}
