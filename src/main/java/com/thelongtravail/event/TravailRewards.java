package com.thelongtravail.event;

import com.thelongtravail.TravailAspect;
import com.thelongtravail.config.TravailConfig;
import com.thelongtravail.data.*;
import com.thelongtravail.helper.TravailCurios;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Phantom;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.player.ItemFishedEvent;
import net.minecraftforge.registries.ForgeRegistries;


// 钓鱼与幻翼奖励；沿用共享队列的预留、提交和释放顺序。
final class TravailRewards {

    static void onFishing(ItemFishedEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        ItemStack travail = TravailCurios.stack(player);
        if (travail.isEmpty() || !LongTravailData.hasWitness(travail, TravailAspect.UNDERWORLD)) return;
        double entityChance = TravailConfig.FISH_ENTITY_CHANCE.get();
        double itemChance = TravailConfig.FISH_SPECIAL_CHANCE.get();
        double total = entityChance + itemChance;
        if (total > 1.0D) {
            entityChance /= total;
            itemChance /= total;
        }
        double roll = player.getRandom().nextDouble();
        if (roll < entityChance) {
            spawnFishingEntity(player, event);
        } else if (roll < entityChance + itemChance) {
            spawnFishingReward(player, event);
        }
    }

    static void killNearbyPhantoms(ServerPlayer player) {
        int range = TravailConfig.PHANTOM_RANGE.get();
        double rangeSquared = (double) range * range;
        int attempts = 0;
        RewardDelivery queue = RewardDelivery.get(player.server);
        for (Phantom phantom : player.serverLevel().getEntitiesOfClass(Phantom.class,
                new net.minecraft.world.phys.AABB(player.position(), player.position()).inflate(range),
                candidate -> candidate.isAlive() && candidate.distanceToSqr(player) <= rangeSquared)) {
            if (!phantom.isAlive()) continue;
            if (attempts++ >= TravailConfig.PHANTOM_MAX_KILLS.get()) break;
            try (var reservation = queue.reserve()) {
                if (reservation == null) { queue.warnFull(player.server); break; }
                phantom.hurt(player.damageSources().playerAttack(player), Float.MAX_VALUE);
                if (!phantom.isAlive()) queueReward(reservation, player.serverLevel(), phantom.position(), RuntimePools.current().phantom(), player.getRandom());
            }
        }
    }

    static void spawnFishingReward(ServerPlayer player, ItemFishedEvent event) {
        Vec3 position = event.getHookEntity().position();
        spawnRewardAt(player.serverLevel(), position, RuntimePools.current().fish(), player.getRandom());
    }

    static void spawnFishingEntity(ServerPlayer player, ItemFishedEvent event) {
        WeightedEntry selected = RuntimePools.current().entities().choose(player.getRandom());
        if (selected == null) return;
        EntityType<?> type = ForgeRegistries.ENTITY_TYPES.getValue(selected.id());
        Entity entity = type == null ? null : type.create(player.serverLevel());
        if (entity != null) {
            Vec3 pos = event.getHookEntity().position();
            entity.moveTo(pos.x, pos.y, pos.z, player.getYRot(), 0.0F);
            player.serverLevel().addFreshEntity(entity);
        }
    }

    static void spawnRewardAt(ServerLevel level, Vec3 position, WeightedTable<RewardEntry> configured, RandomSource random) {
        RewardDelivery queue = RewardDelivery.get(level.getServer());
        try (var reservation = queue.reserve()) {
            if (reservation == null) { queue.warnFull(level.getServer()); return; }
            queueReward(reservation, level, position, configured, random);
        }
    }

    static void queueReward(RewardDelivery.Reservation reservation, ServerLevel level, Vec3 position,
                                    WeightedTable<RewardEntry> configured, RandomSource random) {
        RewardEntry selected = configured.choose(random);
        if (selected == null) return;
        Item item = ForgeRegistries.ITEMS.getValue(selected.id());
        if (item == null) return;
        int count = selected.minCount() + random.nextInt(selected.maxCount() - selected.minCount() + 1);
        reservation.submit(level, position, new ItemStack(item), count);
    }

    private TravailRewards() {}
}
