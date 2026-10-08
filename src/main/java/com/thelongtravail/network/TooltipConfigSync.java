package com.thelongtravail.network;

import com.thelongtravail.TravailAspect;
import com.thelongtravail.config.TravailConfig;
import com.thelongtravail.data.WeightedEntry;
import net.minecraft.resources.ResourceLocation;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class TooltipConfigSync {
    private static volatile Map<String, Double> clientValues = Map.of();
    private static volatile Map<String, List<String>> clientPools = Map.of();
    private static final Map<String, Map<ResourceLocation, String>> displayNames = new java.util.HashMap<>();
    private static long revision;

    public static long revision() { return revision; }

    public static void reset() {
        clientValues = Map.of();
        clientPools = Map.of();
        displayNames.clear();
        revision++;
    }

    public static double decimal(String key, double fallback) {
        return clientValues.getOrDefault(key, fallback);
    }

    public static List<? extends String> pool(String key, List<? extends String> fallback) { return clientPools.containsKey(key) ? clientPools.get(key) : fallback; }

    public static int integer(String key, int fallback) {
        return clientValues.containsKey(key) ? clientValues.get(key).intValue() : fallback;
    }

    static void accept(Map<String, Double> values, Map<String, List<String>> pools) {
        clientValues = Map.copyOf(values);
        clientPools = Map.copyOf(pools);
        displayNames.clear();
        revision++;
    }

    public static String requirementDisplayName(TravailAspect aspect, boolean structures, ResourceLocation id) {
        String key = aspect.id() + "." + (structures ? "structures" : "biomes");
        String configuredName = displayNames.computeIfAbsent(key, unused -> {
            List<? extends String> configured = clientPools.containsKey(key) ? clientPools.get(key)
                    : (structures ? TravailConfig.STRUCTURE_POOLS : TravailConfig.BIOME_POOLS).get(aspect).get();
            Map<ResourceLocation, String> names = new LinkedHashMap<>();
            for (String value : configured) {
                WeightedEntry entry = WeightedEntry.parse(value);
                if (entry != null && !entry.displayName().isBlank()) names.putIfAbsent(entry.id(), entry.displayName());
            }
            return Map.copyOf(names);
        }).getOrDefault(id, id.toString());
        // 缓存原始配置名，翻译在读取时进行，避免切换语言后残留旧文本。
        return com.thelongtravail.data.RequirementNames.resolve(structures, id, configuredName);
    }

    static Map<String, Double> serverValues() {
        Map<String, Double> values = new LinkedHashMap<>();
        values.put("underworldItem.wither", com.thelongtravail.config.UnderworldItemsConfig.WITHER_BONUS.get());
        values.put("underworldItem.kill", com.thelongtravail.config.UnderworldItemsConfig.KILL_THRESHOLD.get());
        values.put("underworldItem.protection", (double)com.thelongtravail.config.UnderworldItemsConfig.PROTECTION.get());
        values.put("swordLantern.memory", com.thelongtravail.valley.SwordLanternConfig.MEMORY.get());
        values.put("swordLantern.timeout", com.thelongtravail.valley.SwordLanternConfig.TIMEOUT.get());
        values.put("swordLantern.duration", com.thelongtravail.valley.SwordLanternConfig.DURATION.get());
        values.put("swordLantern.bonus", com.thelongtravail.valley.SwordLanternConfig.BONUS.get());
        values.put("swordLantern.health", com.thelongtravail.valley.SwordLanternConfig.HEALTH.get());
        values.put("farItem.icarus.igniteSeconds", (double)com.thelongtravail.config.FarReachItemsConfig.IGNITE_SECONDS.get());
        com.thelongtravail.config.FarReachItemsConfig.NUMBERS.forEach((k,v)->values.put("farItem."+k,v.get()));
        com.thelongtravail.config.BoundlessItemsConfig.NUMBERS.forEach((k, v) -> values.put("boundlessItem." + k, v.get()));
        put(values, "general.muteBeneficialEffectGainSounds", TravailConfig.MUTE_BENEFICIAL_EFFECT_GAIN_SOUNDS.get() ? 1 : 0);
        put(values, "flourishing.healingReduction", TravailConfig.FLOURISHING_HEALING_REDUCTION.get());
        put(values, "flourishing.healthThreshold", TravailConfig.FLOURISHING_HEALTH_THRESHOLD.get());
        put(values, "flourishing.damageMultiplier", TravailConfig.FLOURISHING_DAMAGE_MULTIPLIER.get());
        put(values, "flourishing.bonusPerBiome", TravailConfig.FLOURISHING_BONUS_PER_BIOME.get());
        put(values, "flourishing.maxHealingBonus", TravailConfig.MAX_HEALING_BONUS.get());
        put(values, "abyss.fluidDamage", TravailConfig.ABYSS_FLUID_DAMAGE.get());
        put(values, "abyss.fluidInterval", TravailConfig.ABYSS_FLUID_INTERVAL_SECONDS.get());
        put(values, "abyss.miningReduction", TravailConfig.ABYSS_MINING_REDUCTION.get());
        put(values, "abyss.environmentMultiplier", TravailConfig.ABYSS_ENVIRONMENT_MULTIPLIER.get());
        put(values, "abyss.cancelChance", TravailConfig.ABYSS_CANCEL_CHANCE.get());
        put(values, "farReach.clearAllChance", TravailConfig.FAR_ALL_CLEAR_CHANCE.get());
        put(values, "farReach.attackClearChance", TravailConfig.FAR_ATTACK_CLEAR_CHANCE.get());
        put(values, "farReach.attackCooldown", TravailConfig.FAR_ATTACK_COOLDOWN_SECONDS.get());
        put(values, "farReach.levelBonus", TravailConfig.FAR_WITNESS_LEVEL_BONUS.get());
        put(values, "farReach.interval", TravailConfig.FAR_WITNESS_INTERVAL_SECONDS.get());
        put(values, "farReach.actionCount", TravailConfig.FAR_WITNESS_ACTION_COUNT.get());
        put(values, "farReach.effectDuration", TravailConfig.FAR_WITNESS_POSITIVE_DURATION_SECONDS.get());
        put(values, "deepValley.visualDeprivationChance", TravailConfig.VALLEY_VISUAL_DEPRIVATION_CHANCE.get());
        put(values, "deepValley.visualDeprivationDuration", TravailConfig.VALLEY_VISUAL_DEPRIVATION_DURATION_SECONDS.get());
        put(values, "deepValley.visualDeprivationFadeInTicks", TravailConfig.VALLEY_VISUAL_DEPRIVATION_FADE_IN_TICKS.get());
        put(values, "deepValley.visualDeprivationFadeOutTicks", TravailConfig.VALLEY_VISUAL_DEPRIVATION_FADE_OUT_TICKS.get());
        put(values, "visual.worldDarkening", TravailConfig.VISUAL_DARKENING.get());
        put(values, "visual.peripheralOpacity", TravailConfig.VISUAL_OPACITY.get());
        put(values, "visual.clearRadius", TravailConfig.VISUAL_RADIUS.get());
        put(values, "visual.edgeSoftness", TravailConfig.VISUAL_SOFTNESS.get());
        put(values, "visual.distanceVeilStart", TravailConfig.VISUAL_DISTANCE_START.get());
        put(values, "visual.distanceVeilEnd", TravailConfig.VISUAL_DISTANCE_END.get());
        put(values, "visual.pulsePeriodSeconds", TravailConfig.VISUAL_PULSE_PERIOD.get());
        put(values, "visual.pulseDepth", TravailConfig.VISUAL_PULSE_DEPTH.get());
        put(values, "visual.distanceVeilEnabled", TravailConfig.VISUAL_DISTANCE_ENABLED.get() ? 1 : 0);
        put(values, "visual.pulseAffectsRadius", TravailConfig.VISUAL_PULSE_RADIUS.get() ? 1 : 0);
        put(values, "visual.refreshBlendTicks", TravailConfig.VISUAL_REFRESH_TICKS.get());
        put(values, "visual.clearFadeOutTicks", TravailConfig.VISUAL_CLEAR_TICKS.get());
        put(values, "deepValley.stiffChance", TravailConfig.VALLEY_STIFF_CHANCE.get());
        put(values, "deepValley.stiffDuration", TravailConfig.VALLEY_STIFF_DURATION_SECONDS.get());
        put(values, "deepValley.swiftnessDuration", TravailConfig.VALLEY_SWIFTNESS_DURATION_SECONDS.get());
        put(values, "deepValley.swiftnessLevel", TravailConfig.VALLEY_SWIFTNESS_LEVEL.get());
        put(values, "deepValley.randomEffectCount", TravailConfig.VALLEY_RANDOM_EFFECT_COUNT.get());
        put(values, "deepValley.randomEffectDuration", TravailConfig.VALLEY_RANDOM_EFFECT_DURATION_SECONDS.get());
        put(values, "deepValley.witnessAttackCooldown", TravailConfig.VALLEY_WITNESS_ATTACK_COOLDOWN_SECONDS.get());
        put(values, "underworld.damageReduction", TravailConfig.UNDERWORLD_DAMAGE_REDUCTION.get());
        put(values, "underworld.experienceCap", TravailConfig.UNDERWORLD_EXPERIENCE_LEVEL_CAP.get());
        put(values, "underworld.anvilCap", TravailConfig.UNDERWORLD_ANVIL_LEVEL_CAP.get());
        put(values, "underworld.fortuneBonus", TravailConfig.UNDERWORLD_FORTUNE_BONUS.get());
        put(values, "underworld.lootingBonus", TravailConfig.UNDERWORLD_LOOTING_BONUS.get());
        put(values, "underworld.fishingEntityChance", TravailConfig.FISH_ENTITY_CHANCE.get());
        put(values, "underworld.fishingItemChance", TravailConfig.FISH_SPECIAL_CHANCE.get());
        put(values, "boundless.heightThreshold", TravailConfig.BOUNDLESS_HEIGHT_THRESHOLD.get());
        put(values, "boundless.speedReduction", TravailConfig.BOUNDLESS_SPEED_REDUCTION.get());
        put(values, "boundless.extraSlots", TravailConfig.BOUNDLESS_EXTRA_CURIO_SLOTS.get());
        put(values, "boundless.phantomRange", TravailConfig.PHANTOM_RANGE.get());
        put(values, "boundless.phantomInterval", TravailConfig.PHANTOM_CHECK_INTERVAL_SECONDS.get());
        com.thelongtravail.config.FlourishingItemsConfig.NUMBERS.forEach((k,v)->values.put("floral."+k,v.get()));
        com.thelongtravail.config.FlourishingItemsConfig.ENABLED.forEach((k,v)->values.put("floral.enabled."+k.id,v.get()?1D:0D));
        values.put("floral.sharing",com.thelongtravail.config.FlourishingItemsConfig.SHARING.get()?1D:0D);
        values.put("azrael.malice", com.thelongtravail.config.AzraelConfig.MALICE.get());
        values.put("azrael.target", com.thelongtravail.config.AzraelConfig.targetChance());
        values.put("azrael.self", com.thelongtravail.config.AzraelConfig.selfChance());
        return values;
    }

    static Map<String, List<String>> serverPools() {
        Map<String, List<String>> pools = new LinkedHashMap<>();
        for (TravailAspect aspect : TravailAspect.values()) {
            pools.put(aspect.id() + ".biomes", List.copyOf(TravailConfig.BIOME_POOLS.get(aspect).get()));
            pools.put(aspect.id() + ".structures", List.copyOf(TravailConfig.STRUCTURE_POOLS.get(aspect).get()));
        }
        pools.put("floral.springSlots", List.copyOf(com.thelongtravail.config.FlourishingItemsConfig.SPRING_SLOTS.get()));
        pools.put("floral.affectionSlots", List.copyOf(com.thelongtravail.config.FlourishingItemsConfig.AFFECTION_SLOTS.get()));
        return pools;
    }

    private static void put(Map<String, Double> values, String key, Number value) {
        values.put(key, value.doubleValue());
    }

    private TooltipConfigSync() {}
}
