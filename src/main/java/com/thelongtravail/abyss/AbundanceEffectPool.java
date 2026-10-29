package com.thelongtravail.abyss;

import com.thelongtravail.common.RuntimeFailures;
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

    private record Snapshot(List<Entry> entries, boolean dataErrors, boolean rebuildFailed) {}

    private static volatile Snapshot snapshot = new Snapshot(List.of(), false, false);

    public static boolean hasDataErrors() {
        return snapshot.dataErrors();
    }

    public static String emptyMessage() {
        Snapshot current = snapshot;
        return current.rebuildFailed()
                ? "message.the_long_travail.abundance.failed"
                : current.dataErrors()
                        ? "message.the_long_travail.abundance.data_error"
                        : "message.the_long_travail.abundance.empty";
    }

    public static List<Entry> build(
            Collection<MobEffect> effects,
            Collection<Potion> potions,
            Set<MobEffect> excluded,
            int fallbackTicks) {
        return buildSnapshot(effects, potions, excluded, fallbackTicks).entries();
    }

    private static Snapshot buildSnapshot(
            Collection<MobEffect> effects,
            Collection<Potion> potions,
            Set<MobEffect> excluded,
            int fallbackTicks) {
        Map<MobEffect, Map<Integer, SortedSet<Integer>>> samples = new HashMap<>();
        Set<MobEffect> broken = new HashSet<>();
        boolean errors = false;
        for (Potion potion : potions) {
            List<MobEffectInstance> instances;
            try {
                instances = new ArrayList<>(Objects.requireNonNull(potion).getEffects());
            } catch (RuntimeException failure) {
                errors = true;
                RuntimeFailures.report(
                        "abundance potion "
                                + (potion == null
                                        ? "null"
                                        : ForgeRegistries.POTIONS.getKey(potion)),
                        failure);
                continue;
            }
            for (MobEffectInstance instance : instances) {
                MobEffect effect = null;
                try {
                    effect = Objects.requireNonNull(Objects.requireNonNull(instance).getEffect());
                    boolean instant;
                    try {
                        instant = effect.isInstantenous();
                    } catch (RuntimeException failure) {
                        broken.add(effect);
                        throw failure;
                    }
                    int duration = instant ? 1 : instance.getDuration();
                    if (duration != -1 && duration <= 0 || instance.getAmplifier() < 0) continue;
                    samples.computeIfAbsent(effect, unused -> new TreeMap<>())
                            .computeIfAbsent(instance.getAmplifier(), unused -> new TreeSet<>())
                            .add(duration);
                } catch (RuntimeException failure) {
                    errors = true;
                    RuntimeFailures.report(
                            "abundance potion sample "
                                    + (effect == null
                                            ? "null"
                                            : ForgeRegistries.MOB_EFFECTS.getKey(effect)),
                            failure);
                }
            }
        }
        List<Entry> result = new ArrayList<>();
        for (MobEffect effect : effects) {
            try {
                Objects.requireNonNull(effect);
                if (excluded.contains(effect) || broken.contains(effect)) continue;
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
                                                                    : Math.max(
                                                                            1, fallbackTicks))))));
                } else {
                    result.add(
                            new Entry(
                                    effect,
                                    levels.entrySet().stream()
                                            .map(
                                                    e ->
                                                            new Level(
                                                                    e.getKey(),
                                                                    List.copyOf(e.getValue())))
                                            .toList()));
                }
            } catch (RuntimeException failure) {
                errors = true;
                RuntimeFailures.report(
                        "abundance effect "
                                + (effect == null
                                        ? "null"
                                        : ForgeRegistries.MOB_EFFECTS.getKey(effect)),
                        failure);
            }
        }
        return new Snapshot(List.copyOf(result), errors, false);
    }

    public static void reload() {
        try {
            reloadPool();
        } catch (RuntimeException failure) {
            snapshot = new Snapshot(List.of(), false, true);
            RuntimeFailures.report("abundance pool rebuild", failure);
        }
    }

    private static void reloadPool() {
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
        snapshot =
                buildSnapshot(
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
        return sample(snapshot.entries(), random);
    }

    public static List<String> ids() {
        return snapshot.entries().stream()
                .map(e -> ForgeRegistries.MOB_EFFECTS.getKey(e.effect()).toString())
                .toList();
    }

    public static void clear() {
        snapshot = new Snapshot(List.of(), false, false);
    }

    private AbundanceEffectPool() {}
}
