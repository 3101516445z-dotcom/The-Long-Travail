package com.thelongtravail.event;

import com.thelongtravail.TravailAspect;
import com.thelongtravail.config.ModeEffectRules;
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
        boolean alreadyOwnsOne =
                player.getInventory().contains(example) || !TravailCurios.stack(player).isEmpty();
        if (!alreadyOwnsOne) {
            ItemStack gift = example.copy();
            if (!player.addItem(gift)) player.drop(gift, false);
        }

        persisted.putBoolean(STARTER_ITEM_RECEIVED, true);
        persistentData.put(Player.PERSISTED_NBT_TAG, persisted);
    }

    static void updateAltitudePenalty(ServerPlayer player, ItemStack travail) {
        // 只迁移可确认来源的旧记录；新惩罚在读取移动速度时计算，
        // 使其他模组仍能读取未经修改的能力速度。
        restoreFlyingSpeed(player);
    }

    static void restoreFlyingSpeed(ServerPlayer player) {
        CompoundTag data = player.getPersistentData();
        if (!data.getBoolean("LongTravailFlightSlowed")) return;
        float current = player.getAbilities().getFlyingSpeed();
        float restored =
                hasFlightRecord(data)
                        ? FlightSpeedAdjustment.restore(
                                current,
                                data.getFloat("LongTravailOriginalFlySpeed"),
                                data.getFloat("LongTravailLastFlySpeed"))
                        : current;
        data.remove("LongTravailOriginalFlySpeed");
        data.remove("LongTravailLastFlySpeed");
        data.remove("LongTravailFlightSlowed");
        if (!FlightSpeedAdjustment.same(current, restored)) {
            player.getAbilities().setFlyingSpeed(restored);
            player.onUpdateAbilities();
        }
    }

    private static boolean hasFlightRecord(CompoundTag data) {
        return data.getBoolean("LongTravailFlightSlowed")
                && data.contains("LongTravailOriginalFlySpeed", net.minecraft.nbt.Tag.TAG_FLOAT)
                && data.contains("LongTravailLastFlySpeed", net.minecraft.nbt.Tag.TAG_FLOAT)
                && Float.isFinite(data.getFloat("LongTravailOriginalFlySpeed"))
                && Float.isFinite(data.getFloat("LongTravailLastFlySpeed"));
    }

    static void updateExtraSlots(ServerPlayer player, ItemStack travail) {
        TravailCurios.syncExtraSlots(
                player,
                LongTravailData.hasWitness(travail, TravailAspect.BOUNDLESS)
                        ? TravailConfig.BOUNDLESS_EXTRA_CURIO_SLOTS.get()
                        : 0);
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
        return total >= Integer.MAX_VALUE
                ? Integer.MAX_VALUE
                : (int) Math.max(0.0D, Math.floor(total));
    }

    static void onBreakSpeed(PlayerEvent.BreakSpeed event) {
        Player player = event.getEntity();
        ItemStack travail = TravailCurios.stack(player);
        if (ModeEffectRules.diaryApplies(player)
                && !travail.isEmpty()
                && !LongTravailData.hasWitness(travail, TravailAspect.ABYSS)) {
            double reduction = TravailConfig.ABYSS_MINING_REDUCTION.get();
            if (player.level().isClientSide)
                reduction = TooltipConfigSync.decimal("abyss.miningReduction", reduction);
            float multiplier = (float) (1.0D - Math.max(0.0D, Math.min(1.0D, reduction)));
            event.setNewSpeed(event.getNewSpeed() * multiplier);
        }
    }

    static void onExperienceChange(PlayerXpEvent.XpChange event) {
        int cap = TravailConfig.UNDERWORLD_EXPERIENCE_LEVEL_CAP.get();
        if (!(event.getEntity() instanceof ServerPlayer player)
                || event.getAmount() <= 0
                || player.experienceLevel < cap) return;
        ItemStack travail = TravailCurios.stack(player);
        if (ModeEffectRules.diaryApplies(player)
                && !travail.isEmpty()
                && !LongTravailData.hasWitness(travail, TravailAspect.UNDERWORLD))
            event.setAmount(0);
    }

    static void onExperienceLevelChange(PlayerXpEvent.LevelChange event) {
        int cap = TravailConfig.UNDERWORLD_EXPERIENCE_LEVEL_CAP.get();
        if (!(event.getEntity() instanceof ServerPlayer player)
                || event.getLevels() <= 0
                || player.experienceLevel < cap) return;
        ItemStack travail = TravailCurios.stack(player);
        if (ModeEffectRules.diaryApplies(player)
                && !travail.isEmpty()
                && !LongTravailData.hasWitness(travail, TravailAspect.UNDERWORLD))
            event.setLevels(0);
    }

    private TravailPlayerMaintenance() {}
}
