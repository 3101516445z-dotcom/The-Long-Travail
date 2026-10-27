package com.thelongtravail.abyss;

import com.thelongtravail.TheLongTravail;
import com.thelongtravail.config.AbyssItemsConfig;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.*;

// 按效果、等级和持续时间分层保存去重后的不可变抽样表。
public final class AbundanceEffectPool {
    public record Level(int amplifier, List<Integer> durations) {
        public Level {
            durations = List.copyOf(durations);
        }
    }

    public record Entry(MobEffect effect, List<Level> levels) {
        public Entry {
            levels = List.copyOf(levels);
        }
    }

    private static List<Entry> entries = List.of();

    public static List<Entry> build(
            Collection<MobEffect> effects,
            Collection<Potion> potions,
            Set<MobEffect> excluded,
            int fallbackTicks) {
        Map<MobEffect, Map<Integer, SortedSet<Integer>>> samples = new HashMap<>();
        for (Potion potion : potions) {
            for (MobEffectInstance effect : potion.getEffects()) {
                int duration = effect.getEffect().isInstantenous() ? 1 : effect.getDuration();
                if (duration != -1 && duration <= 0 || effect.getAmplifier() < 0) continue;
                samples.computeIfAbsent(effect.getEffect(), unused -> new TreeMap<>())
                        .computeIfAbsent(effect.getAmplifier(), unused -> new TreeSet<>())
                        .add(duration);
            }
        }
        List<Entry> result = new ArrayList<>();
        for (MobEffect effect : effects) {
            if (excluded.contains(effect)) continue;
            var levels = samples.get(effect);
            if (levels == null || levels.isEmpty()) {
                result.add(
                        new Entry(
                                effect,
                                List.of(
                                        new Level(
                                                0,
                                                List.of(
                                                        effect.isInstantenous()
                                                                ? 1
                                                                : fallbackTicks)))));
            } else {
                result.add(
                        new Entry(
                                effect,
                                levels.entrySet().stream()
                                        .map(e -> new Level(e.getKey(), List.copyOf(e.getValue())))
                                        .toList()));
            }
        }
        return List.copyOf(result);
    }

    public static void reload() {
        Set<MobEffect> excluded = new HashSet<>();
        for (String text : AbyssItemsConfig.ABUNDANCE_BLACKLIST.get()) {
            ResourceLocation id = ResourceLocation.tryParse(text);
            if (id != null && ForgeRegistries.MOB_EFFECTS.containsKey(id))
                excluded.add(ForgeRegistries.MOB_EFFECTS.getValue(id));
            else
                TheLongTravail.LOGGER.warn(
                        "Unknown liquefied abundance effect blacklist ID: {}", text);
        }
        var whitelist = AbyssItemsConfig.ABUNDANCE_WHITELIST.get();
        Set<MobEffect> allowed = new HashSet<>();
        for (String text : new LinkedHashSet<>(whitelist)) {
            ResourceLocation id = ResourceLocation.tryParse(text);
            if (id != null && text.contains(":") && ForgeRegistries.MOB_EFFECTS.containsKey(id))
                allowed.add(ForgeRegistries.MOB_EFFECTS.getValue(id));
            else
                TheLongTravail.LOGGER.warn(
                        "Unknown liquefied abundance effect whitelist ID: {}", text);
        }
        var effects =
                ForgeRegistries.MOB_EFFECTS.getValues().stream()
                        .filter(effect -> whitelist.isEmpty() || allowed.contains(effect))
                        .sorted(
                                Comparator.comparing(
                                        e -> ForgeRegistries.MOB_EFFECTS.getKey(e).toString()))
                        .toList();
        entries =
                build(
                        effects,
                        ForgeRegistries.POTIONS.getValues(),
                        excluded,
                        ticks(AbyssItemsConfig.ABUNDANCE_DURATION.get()));
    }

    public static int ticks(double seconds) {
        return Math.max(1, (int) Math.ceil(seconds * 20));
    }

    public static MobEffectInstance sample(List<Entry> pool, RandomSource random) {
        if (pool.isEmpty()) return null;
        Entry entry = pool.get(random.nextInt(pool.size()));
        Level level = entry.levels().get(random.nextInt(entry.levels().size()));
        int duration = level.durations().get(random.nextInt(level.durations().size()));
        return new MobEffectInstance(entry.effect(), duration, level.amplifier());
    }

    public static MobEffectInstance sample(RandomSource random) {
        return sample(entries, random);
    }

    public static List<String> ids() {
        return entries.stream()
                .map(e -> ForgeRegistries.MOB_EFFECTS.getKey(e.effect()).toString())
                .toList();
    }

    public static void clear() {
        entries = List.of();
    }

    private AbundanceEffectPool() {}
}
