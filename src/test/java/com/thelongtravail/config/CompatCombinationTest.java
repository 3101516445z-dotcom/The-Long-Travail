package com.thelongtravail.config;

import com.electronwill.nightconfig.core.CommentedConfig;
import com.google.gson.GsonBuilder;
import com.thelongtravail.TravailAspect;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;

public final class CompatCombinationTest {
    public record Descriptor(
            String family,
            String theme,
            String namespace,
            String pool,
            String marker,
            List<String> raw,
            List<String> entries) {}

    private static long checks;
    private static int scenarios;

    private static Object value(Object record, String name) throws Exception {
        var method = record.getClass().getDeclaredMethod(name);
        method.setAccessible(true);
        return method.invoke(record);
    }

    private static List<?> presets(Class<?> type) throws Exception {
        var field = type.getDeclaredField("PRESETS");
        field.setAccessible(true);
        return (List<?>) field.get(null);
    }

    public static List<Descriptor> descriptors() throws Exception {
        var all = new ArrayList<Descriptor>();
        for (String family : List.of("biome", "structure")) {
            Class<?> type =
                    family.equals("biome")
                            ? BiomeCompatDefaults.class
                            : StructureCompatDefaults.class;
            for (Object p : presets(type)) {
                var function = (Function<TravailAspect, List<String>>) value(p, family + "s");
                for (var aspect : TravailAspect.values()) {
                    var raw = function.apply(aspect);
                    if (raw.isEmpty()) continue;
                    all.add(
                            new Descriptor(
                                    family,
                                    aspect.id(),
                                    (String) value(p, "namespace"),
                                    "travail.requirements." + family + "s",
                                    (String) value(p, "marker"),
                                    raw,
                                    raw.stream()
                                            .map(
                                                    id ->
                                                            id
                                                                    + "|@"
                                                                    + family
                                                                    + "."
                                                                    + id.replace(':', '.')
                                                                    + "|1")
                                            .toList()));
                }
            }
        }
        for (Object p : presets(RewardCompatDefaults.class)) {
            String ns = (String) value(p, "namespace");
            for (boolean fish : List.of(true, false)) {
                var raw = (List<String>) value(p, fish ? "fishingItems" : "phantomDrops");
                all.add(
                        new Descriptor(
                                "reward",
                                fish ? "underworld" : "boundless",
                                ns,
                                fish
                                        ? RewardCompatDefaults.FISH_POOL
                                        : RewardCompatDefaults.PHANTOM_POOL,
                                "travail.witness.compatInitialization."
                                        + ns
                                        + (fish ? ".fishingItemsAdded" : ".phantomDropsAdded"),
                                raw,
                                raw));
            }
        }
        for (Object p : presets(FishingEntityCompatDefaults.class)) {
            String ns = (String) value(p, "namespace");
            var raw = (List<String>) value(p, "entries");
            all.add(
                    new Descriptor(
                            "entity",
                            "underworld",
                            ns,
                            FishingEntityCompatDefaults.POOL,
                            "travail.witness.compatInitialization." + ns + ".fishingEntitiesAdded",
                            raw,
                            raw));
        }
        for (Object p : presets(EffectCompatDefaults.class)) {
            String ns = (String) value(p, "namespace");
            for (boolean far : List.of(true, false)) {
                var raw = (List<String>) value(p, far ? "farReach" : "deepValley");
                all.add(
                        new Descriptor(
                                "effect",
                                far ? "far_reach" : "deep_valley",
                                ns,
                                EffectCompatDefaults.POOL,
                                "travail.witness.compatInitialization."
                                        + ns
                                        + ".positiveEffectsAdded",
                                raw,
                                raw));
            }
        }
        for (Object p : presets(GoldenAgeCompatDefaults.class)) {
            var pool = (GoldenAgeCompatDefaults.Pool) value(p, "pool");
            var raw = (List<String>) value(p, "entries");
            String ns = (String) value(p, "mod");
            all.add(
                    new Descriptor(
                            pool == GoldenAgeCompatDefaults.Pool.FOOD ? "food" : "enchantment",
                            "far_reach",
                            ns,
                            pool.path(),
                            pool.marker(ns),
                            raw,
                            raw));
        }
        return all;
    }

    private static String id(String entry) {
        return entry.split("\\|", -1)[0].trim();
    }

    private static void expect(boolean value, String label) {
        if (!value) throw new AssertionError("scenario=" + scenarios + " " + label);
        checks++;
    }

    private static boolean allowed(Set<String> selected) {
        return !(selected.contains("youkaisfeasts") && selected.contains("youkaishomecoming"));
    }

    private static boolean apply(
            CommentedConfig c, Descriptor d, boolean present, Predicate<String> exists) {
        return switch (d.family()) {
            case "biome" -> BiomeCompatDefaults.apply(c, d.raw(), d.marker(), present, exists);
            case "structure" ->
                    StructureCompatDefaults.apply(c, d.raw(), d.marker(), present, exists);
            case "reward" ->
                    RewardCompatDefaults.apply(c, d.pool(), d.raw(), d.marker(), present, exists);
            case "entity" ->
                    FishingEntityCompatDefaults.apply(c, d.raw(), d.marker(), present, exists);
            case "effect" ->
                    EffectCompatDefaults.apply(
                            c,
                            d.theme().equals("far_reach")
                                    ? TravailAspect.FAR_REACH
                                    : TravailAspect.DEEP_VALLEY,
                            new EffectCompatDefaults.Preset(
                                    d.namespace(), d.namespace(), d.raw(), d.raw()),
                            present,
                            exists);
            default ->
                    GoldenAgeCompatDefaults.add(
                            c,
                            d.family().equals("food")
                                    ? GoldenAgeCompatDefaults.Pool.FOOD
                                    : GoldenAgeCompatDefaults.Pool.ENCHANTMENT,
                            d.namespace(),
                            d.raw(),
                            present,
                            exists);
        };
    }

    private static void round(
            List<Descriptor> ds,
            Map<String, CommentedConfig> actual,
            Map<String, CommentedConfig> model,
            Set<String> selected,
            Predicate<String> exists) {
        for (Descriptor d : ds) {
            var want = model.get(d.theme());
            boolean change =
                    selected.contains(d.namespace()) && !Boolean.TRUE.equals(want.get(d.marker()));
            if (change) {
                var list = new ArrayList<>(want.<List<String>>get(d.pool()));
                for (String candidate : d.entries()) {
                    if (exists.test(id(candidate))
                            && list.stream().noneMatch(old -> id(old).equals(id(candidate))))
                        list.add(candidate);
                }
                want.set(d.pool(), list);
                want.set(d.marker(), true);
            }
            expect(
                    apply(actual.get(d.theme()), d, selected.contains(d.namespace()), exists)
                            == change,
                    "write decision " + d.marker());
        }
        for (String theme : actual.keySet())
            expect(actual.get(theme).equals(model.get(theme)), "whole file " + theme);
    }

    private static void scenario(List<Descriptor> ds, Set<String> selected, int mode) {
        expect(allowed(selected), "mutually exclusive sources");
        scenarios++;
        var actual = new LinkedHashMap<String, CommentedConfig>();
        var model = new LinkedHashMap<String, CommentedConfig>();
        for (Descriptor d : ds) {
            var c = actual.computeIfAbsent(d.theme(), key -> CommentedConfig.inMemory());
            c.set("untouched.note", "保留手动配置");
            c.set("untouched.chance", 0.37);
            if (!c.contains(d.pool())) c.set(d.pool(), new ArrayList<String>());
            if (mode > 0) {
                var entries = new ArrayList<>(c.<List<String>>get(d.pool()));
                String target = id(d.entries().get(0));
                String manual =
                        switch (d.family()) {
                            case "biome", "structure" -> target + "|玩家自定名称|77";
                            case "reward" -> target + "|77|2|7";
                            case "effect" -> target + "|7|77";
                            case "entity" -> target + "|77";
                            default -> target;
                        };
                if (entries.stream().noneMatch(old -> id(old).equals(id(manual))))
                    entries.add(manual);
                c.set(d.pool(), entries);
            }
            c.set(d.marker(), mode == 2 && Math.floorMod(d.marker().hashCode(), 3) == 0);
        }
        actual.forEach((theme, c) -> model.put(theme, copy(c)));
        Predicate<String> exists = id -> mode != 2 || Math.floorMod(id.hashCode(), 3) != 0;
        round(ds, actual, model, selected, exists);
        round(ds, actual, model, selected, exists);
        for (Descriptor d : ds) {
            actual.get(d.theme()).set(d.pool(), List.of());
            model.get(d.theme()).set(d.pool(), List.of());
        }
        round(ds, actual, model, selected, id -> true);
        for (Descriptor d : ds)
            if (Math.floorMod(d.marker().hashCode(), 2) == 0) {
                actual.get(d.theme()).remove(d.marker());
                model.get(d.theme()).remove(d.marker());
            }
        round(ds, actual, model, selected, id -> true);
        var complement = new HashSet<String>();
        for (Descriptor d : ds)
            if (!selected.contains(d.namespace())) complement.add(d.namespace());
        complement.remove(
                selected.contains("youkaisfeasts") ? "youkaishomecoming" : "youkaisfeasts");
        round(ds, actual, model, complement, id -> true);
        round(ds, actual, model, selected, id -> true);
    }

    private static CommentedConfig copy(CommentedConfig source) {
        var target = CommentedConfig.inMemory();
        source.valueMap()
                .forEach(
                        (key, value) ->
                                target.set(
                                        List.of(key),
                                        value instanceof CommentedConfig child
                                                ? copy(child)
                                                : value instanceof List<?> list
                                                        ? new ArrayList<>(list)
                                                        : value));
        return target;
    }

    public static void main(String[] args) throws Exception {
        long start = System.nanoTime();
        var ds = descriptors();
        var stats = new LinkedHashMap<String, Integer>();
        for (String family :
                List.of("biome", "structure", "reward", "entity", "effect", "golden")) {
            var subset =
                    ds.stream()
                            .filter(
                                    d ->
                                            family.equals("golden")
                                                    ? Set.of("food", "enchantment")
                                                            .contains(d.family())
                                                    : d.family().equals(family))
                            .toList();
            var sources = subset.stream().map(Descriptor::namespace).distinct().toList();
            int count = 0;
            for (int mask = 0; mask < (1 << sources.size()); mask++) {
                var selected = new HashSet<String>();
                for (int i = 0; i < sources.size(); i++)
                    if ((mask & (1 << i)) != 0) selected.add(sources.get(i));
                if (!allowed(selected)) continue;
                scenario(subset, selected, mask % 3);
                count++;
            }
            stats.put(family, count);
        }
        var sources = ds.stream().map(Descriptor::namespace).distinct().sorted().toList();
        for (String source : sources) scenario(ds, Set.of(source), 1);
        scenario(ds, Set.of(), 0);
        for (String excluded : List.of("youkaisfeasts", "youkaishomecoming")) {
            var full = new HashSet<>(sources);
            full.remove(excluded);
            for (int mode = 0; mode < 3; mode++) scenario(ds, full, mode);
        }
        var random = new Random(20261024L);
        for (int n = 0; n < 1500; n++) {
            var selected = new HashSet<String>();
            for (String source : sources) if (random.nextBoolean()) selected.add(source);
            if (!allowed(selected))
                selected.remove(n % 2 == 0 ? "youkaisfeasts" : "youkaishomecoming");
            var shuffled = new ArrayList<>(ds);
            Collections.shuffle(shuffled, random);
            scenario(shuffled, selected, n % 3);
        }
        var pressure = CommentedConfig.inMemory();
        var large = new ArrayList<String>();
        for (int i = 0; i < 20000; i++) large.add("manual:entry_" + i + "|77");
        pressure.set(FishingEntityCompatDefaults.POOL, large);
        var entities =
                ds.stream()
                        .filter(
                                d ->
                                        d.family().equals("entity")
                                                && !d.namespace().equals("youkaisfeasts"))
                        .toList();
        for (int cycle = 0; cycle < 50; cycle++) {
            for (Descriptor d : entities) {
                pressure.remove(d.marker());
                apply(pressure, d, true, id -> true);
            }
            expect(
                    pressure.<List<String>>get(FishingEntityCompatDefaults.POOL)
                            .subList(0, 20000)
                            .equals(large),
                    "Large manual pool retained");
            expect(
                    pressure.<List<String>>get(FishingEntityCompatDefaults.POOL).size() == 20010,
                    "No growth on repeated reset");
        }
        var output = Path.of(args[0]);
        Files.createDirectories(output);
        var gson = new GsonBuilder().setPrettyPrinting().create();
        Files.writeString(output.resolve("descriptors.json"), gson.toJson(ds));
        String report =
                "COMPAT_COMBINATIONS_PASS scenarios="
                        + scenarios
                        + " checks="
                        + checks
                        + " sources="
                        + sources.size()
                        + " subsets="
                        + stats
                        + " seed=20261024 largePool=20000 resetCycles=50 elapsedMs="
                        + (System.nanoTime() - start) / 1000000;
        Files.writeString(output.resolve("result.txt"), report);
        System.out.println(report);
    }

    private CompatCombinationTest() {}
}
