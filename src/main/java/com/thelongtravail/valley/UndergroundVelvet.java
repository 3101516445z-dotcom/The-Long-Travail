package com.thelongtravail.valley;

import com.thelongtravail.api.TravailStateApi;
import com.thelongtravail.common.*;
import com.thelongtravail.config.DeepValleyItemsConfig;
import com.thelongtravail.loot.WeightedLootSampler;
import com.thelongtravail.registry.ModRegistry;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.*;
import net.minecraft.world.level.storage.loot.*;
import net.minecraft.world.level.storage.loot.parameters.*;

import java.util.*;

public final class UndergroundVelvet {
    private static final TagKey<Item> ORES = TagKey.create(Registries.ITEM, new ResourceLocation("forge", "ores"));
    private static final TagKey<Item> INGOTS = TagKey.create(Registries.ITEM, new ResourceLocation("forge", "ingots"));
    private static long nextWarning;

    public static boolean use(ServerPlayer player, ItemStack stack) {
        if (player.getCooldowns().isOnCooldown(stack.getItem())
                || player.isSpectator()
                || com.thelongtravail.boundless.TimeStopManager.frozen(player)) return false;
        boolean ore = player.getRandom().nextDouble() < DeepValleyItemsConfig.get("underground_velvet.oreChance");
        ItemStack base =
                TagItemSampler.choose(
                        ore ? ORES : INGOTS,
                        ore ? DeepValleyItemsConfig.EXTRA_ORES.get() : DeepValleyItemsConfig.EXTRA_INGOTS.get(),
                        player.getRandom(),
                        8);
        if (base.isEmpty()) return failed(player);
        var state = TravailStateApi.snapshot(player);
        List<ItemStack> rewards = new ArrayList<>();
        rewards.add(base);
        if (state.hasMalice(com.thelongtravail.TravailAspect.DEEP_VALLEY)) {
            double mine = DeepValleyItemsConfig.get("underground_velvet.mineshaftChance"),
                    city = DeepValleyItemsConfig.get("underground_velvet.cityChance");
            double divisor = Math.max(1, mine + city), roll = player.getRandom().nextDouble();
            String table =
                    roll < mine / divisor
                            ? "abandoned_mineshaft"
                            : roll < (mine + city) / divisor ? "ancient_city" : null;
            if (table != null)
                try {
                    LootParams params =
                            new LootParams.Builder(player.serverLevel())
                                    .withParameter(LootContextParams.ORIGIN, player.position())
                                    .withParameter(LootContextParams.THIS_ENTITY, player)
                                    .withLuck(0)
                                    .create(LootContextParamSets.CHEST);
                    LootContext context = new LootContext.Builder(params).create(null);
                    var result =
                            WeightedLootSampler.sample(
                                    new ResourceLocation("minecraft", "chests/" + table),
                                    player.server.getLootData()::getLootTable,
                                    context,
                                    table.equals("ancient_city") ? 2 : 4,
                                    1,
                                    item -> item == ModRegistry.UNDERGROUND_VELVET.get());
                    if (result.skipped() > 0)
                        warn(player, "Skipped unsupported loot entries: " + result.skipped(), null);
                    if (!result.complete()) return failed(player);
                    rewards.addAll(result.items());
                } catch (RuntimeException failure) {
                    warn(player, "Unable to sample Underground Velvet loot", failure);
                    return failed(player);
                }
        }
        if (!player.getAbilities().instabuild) stack.shrink(1);
        player.getCooldowns()
                .addCooldown(
                        ModRegistry.UNDERGROUND_VELVET.get(),
                        (int) Math.ceil(DeepValleyItemsConfig.get("underground_velvet.cooldownTicks")));
        for (ItemStack reward : rewards) ItemDelivery.give(player, reward);
        if (state.hasWitness(com.thelongtravail.TravailAspect.DEEP_VALLEY)) player.giveExperienceLevels(15);
        return true;
    }

    private static boolean failed(ServerPlayer player) {
        player.displayClientMessage(
                net.minecraft.network.chat.Component.translatable("message.the_long_travail.underground_velvet.failed"),
                true);
        return false;
    }

    private static void warn(ServerPlayer p, String text, Exception failure) {
        long now = p.server.overworld().getGameTime();
        if (now < nextWarning && now >= nextWarning - 1200) return;
        nextWarning = now + 1200;
        com.thelongtravail.TheLongTravail.LOGGER.warn(text, failure);
    }

    private UndergroundVelvet() {}
}
