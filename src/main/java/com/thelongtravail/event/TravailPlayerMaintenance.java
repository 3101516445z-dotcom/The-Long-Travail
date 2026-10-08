package com.thelongtravail.event;

import com.thelongtravail.TravailAspect;
import com.thelongtravail.config.TravailConfig;
import com.thelongtravail.data.*;
import com.thelongtravail.helper.TravailCurios;
import com.thelongtravail.network.TooltipConfigSync;
import com.thelongtravail.registry.ModRegistry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.player.PlayerXpEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;


final class TravailPlayerMaintenance {
    private static final String STARTER_ITEM_RECEIVED = "LongTravailStarterItemReceived";
    static void giveStarterItem(ServerPlayer player) {
        if (!TravailConfig.GIVE_STARTER_ITEM.get()) return;

        CompoundTag persistentData = player.getPersistentData();
        CompoundTag persisted = persistentData.getCompound(Player.PERSISTED_NBT_TAG);
        if (persisted.getBoolean(STARTER_ITEM_RECEIVED)) return;

        ItemStack example = new ItemStack(ModRegistry.LONG_TRAVAIL.get());
        boolean alreadyOwnsOne = player.getInventory().contains(example) || !TravailCurios.stack(player).isEmpty();
        if (!alreadyOwnsOne) {
            ItemStack gift = example.copy();
            if (!player.addItem(gift)) player.drop(gift, false);
        }

        persisted.putBoolean(STARTER_ITEM_RECEIVED, true);
        persistentData.put(Player.PERSISTED_NBT_TAG, persisted);
    }

    static void updateAltitudePenalty(ServerPlayer player, ItemStack travail) {
        boolean active = !LongTravailData.hasWitness(travail, TravailAspect.BOUNDLESS)
                && player.getY() > TravailConfig.BOUNDLESS_HEIGHT_THRESHOLD.get();
        double reduction = TravailConfig.BOUNDLESS_SPEED_REDUCTION.get();
        boolean penaltyActive = active && reduction > 0.0D;
        // 步行和游泳惩罚由 AltitudeSpeedMixin 在读取速度时应用，此处不写入移速属性。
        if (penaltyActive && player.getAbilities().flying) reduceFlyingSpeed(player, reduction); else restoreFlyingSpeed(player);
        // 鞘翅位移由 AltitudeGlidingMixin 在控制端缩放。
        // 此处不能衰减保存的速度，否则会累积衰减且无法覆盖本地预测。
    }

    static void reduceFlyingSpeed(ServerPlayer player, double reduction) {
        CompoundTag data = player.getPersistentData();
        float current = player.getAbilities().getFlyingSpeed();
        float baseline = data.getBoolean("LongTravailFlightSlowed")
                ? data.getFloat("LongTravailOriginalFlySpeed") : current;
        // 旧存档只有基准值，没有上次应用的值。
        float last = data.contains("LongTravailLastFlySpeed") ? data.getFloat("LongTravailLastFlySpeed") : current;
        FlightSpeedAdjustment.Result adjustment = FlightSpeedAdjustment.apply(current, baseline, last, reduction);
        data.putBoolean("LongTravailFlightSlowed", true);
        data.putFloat("LongTravailOriginalFlySpeed", adjustment.baseline());
        data.putFloat("LongTravailLastFlySpeed", adjustment.applied());
        if (!FlightSpeedAdjustment.same(current, adjustment.applied())) {
            player.getAbilities().setFlyingSpeed(adjustment.applied());
            player.onUpdateAbilities();
        }
    }

    static void restoreFlyingSpeed(ServerPlayer player) {
        CompoundTag data = player.getPersistentData();
        if (!data.getBoolean("LongTravailFlightSlowed")) return;
        float current = player.getAbilities().getFlyingSpeed();
        float restored = FlightSpeedAdjustment.restore(current, data.getFloat("LongTravailOriginalFlySpeed"),
                data.contains("LongTravailLastFlySpeed") ? data.getFloat("LongTravailLastFlySpeed") : current);
        data.remove("LongTravailOriginalFlySpeed");
        data.remove("LongTravailLastFlySpeed");
        data.remove("LongTravailFlightSlowed");
        if (!FlightSpeedAdjustment.same(current, restored)) {
            player.getAbilities().setFlyingSpeed(restored);
            player.onUpdateAbilities();
        }
    }

    static void updateExtraSlots(ServerPlayer player, ItemStack travail) {
        TravailCurios.syncExtraSlots(player, LongTravailData.hasWitness(travail, TravailAspect.BOUNDLESS)
                ? TravailConfig.BOUNDLESS_EXTRA_CURIO_SLOTS.get() : 0);
    }

    static void removeExtraSlots(ServerPlayer player) {
        TravailCurios.syncExtraSlots(player, 0);
    }

    static void clampExperience(ServerPlayer player) {
        int cap = TravailConfig.UNDERWORLD_EXPERIENCE_LEVEL_CAP.get();
        if (player.experienceLevel < cap) return;
        player.experienceLevel = cap;
        player.experienceProgress = 0.0F;
        player.totalExperience = experienceAtLevel(cap);
    }

    static int experienceAtLevel(int level) {
        if (level <= 0) return 0;
        double total;
        if (level <= 16) total = level * level + 6.0D * level;
        else if (level <= 31) total = 2.5D * level * level - 40.5D * level + 360.0D;
        else total = 4.5D * level * level - 162.5D * level + 2220.0D;
        return total >= Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) Math.max(0.0D, Math.floor(total));
    }

    static void onBreakSpeed(PlayerEvent.BreakSpeed event) {
        // 客户端预测和服务端破坏方块必须使用相同的惩罚系数。
        Player player = event.getEntity();
        ItemStack travail = TravailCurios.stack(player);
        if (!travail.isEmpty() && !LongTravailData.hasWitness(travail, TravailAspect.ABYSS)) {
            double reduction = TravailConfig.ABYSS_MINING_REDUCTION.get();
            if (player.level().isClientSide)
                reduction = TooltipConfigSync.decimal("abyss.miningReduction", reduction);
            // 在普通装备加成之后应用，保留其他模组已计算的速度。
            float multiplier = (float) (1.0D - Math.max(0.0D, Math.min(1.0D, reduction)));
            event.setNewSpeed(event.getNewSpeed() * multiplier);
        }
    }

    static void onExperienceChange(PlayerXpEvent.XpChange event) {
        int cap = TravailConfig.UNDERWORLD_EXPERIENCE_LEVEL_CAP.get();
        if (!(event.getEntity() instanceof ServerPlayer player) || event.getAmount() <= 0 || player.experienceLevel < cap) return;
        ItemStack travail = TravailCurios.stack(player);
        if (!travail.isEmpty() && !LongTravailData.hasWitness(travail, TravailAspect.UNDERWORLD)) event.setAmount(0);
    }

    static void onExperienceLevelChange(PlayerXpEvent.LevelChange event) {
        int cap = TravailConfig.UNDERWORLD_EXPERIENCE_LEVEL_CAP.get();
        if (!(event.getEntity() instanceof ServerPlayer player) || event.getLevels() <= 0 || player.experienceLevel < cap) return;
        ItemStack travail = TravailCurios.stack(player);
        if (!travail.isEmpty() && !LongTravailData.hasWitness(travail, TravailAspect.UNDERWORLD)) event.setLevels(0);
    }

    private TravailPlayerMaintenance() {}
}
