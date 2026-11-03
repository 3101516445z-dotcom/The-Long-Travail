package travail.smoke;

import com.mojang.serialization.Codec;
import com.thelongtravail.config.TravailConfig;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.storage.loot.*;
import net.minecraft.world.level.storage.loot.parameters.*;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.ForgeInternalHandler;
import net.minecraftforge.common.loot.IGlobalLootModifier;
import java.util.*;
import java.util.function.BiConsumer;

/** Exercises the real transformed LootTable and Forge modifier chain in the isolated server. */
final class ArchaeologyLootCases {
    static void run(ServerLevel level, BiConsumer<Boolean, String> check) {
        var manager = modifierManager();
        boolean previous = TravailConfig.PROTECT_ARCHAEOLOGY_LOOT.get();
        Object saved = null;
        java.lang.reflect.Field field = null;
        long started = System.nanoTime();
        int rolls = 0;
        try {
            check.accept(previous, "archaeology protection defaults to enabled");
            field = manager.getClass().getDeclaredField("registeredLootModifiers");
            field.setAccessible(true);
            saved = field.get(manager);
            var params =
                    new LootParams.Builder(level)
                            .withParameter(LootContextParams.ORIGIN, Vec3.ZERO)
                            .create(LootContextParamSets.ARCHAEOLOGY);
            var custom = level.getServer().getLootData().getLootTable(id("buried_custom"));
            var ordinary =
                    level.getServer().getLootData().getLootTable(id("archaeology/path_only"));
            check.accept(
                    custom.getParamSet() == LootContextParamSets.ARCHAEOLOGY,
                    "custom archaeology type loaded");
            check.accept(
                    ordinary.getParamSet() == LootContextParamSets.CHEST,
                    "path alone is not archaeology");
            var injected =
                    LootTable.lootTable()
                            .setParamSet(LootContextParamSets.ARCHAEOLOGY)
                            .withPool(
                                    LootPool.lootPool()
                                            .name("external_table_injection")
                                            .setRolls(
                                                    net.minecraft.world.level.storage.loot.providers
                                                            .number.ConstantValue.exactly(1))
                                            .add(
                                                    net.minecraft.world.level.storage.loot.entries
                                                            .LootItem.lootTableItem(
                                                            Items.GOLD_INGOT)))
                            .build();
            var probes = List.of(new Probe(0), new Probe(1), new Probe(2), new Probe(3));
            var chain = new LinkedHashMap<ResourceLocation, IGlobalLootModifier>();
            for (int i = 0; i < probes.size(); i++) chain.put(id("probe_" + i), probes.get(i));
            field.set(manager, chain);
            for (int i = 0; i < 100000; i++) {
                long seed = 20261102L + i;
                TravailConfig.PROTECT_ARCHAEOLOGY_LOOT.set(true);
                var kept = custom.getRandomItems(params, seed);
                check.accept(
                        kept.size() == 1
                                && kept.get(0).is(Items.EMERALD)
                                && kept.get(0).getCount() == 3
                                && kept.get(0).getTag().getBoolean("table_injection"),
                        "nested table and table functions preserved");
                var added = injected.getRandomItems(params, seed);
                check.accept(
                        added.size() == 1 && added.get(0).is(Items.GOLD_INGOT),
                        "table pool injection preserved");
                var chest = ordinary.getRandomItems(params, seed);
                check.accept(isModified(chest), "non archaeology runs full modifier chain");
                TravailConfig.PROTECT_ARCHAEOLOGY_LOOT.set(false);
                check.accept(
                        isModified(custom.getRandomItems(params, seed)),
                        "disabled restores modifier chain");
                rolls += 4;
            }
            for (Probe probe : probes)
                check.accept(
                        probe.calls == 200000,
                        "protected rolls never enter modifier chain: " + probe.calls);
            var randomProbe = new Probe(4);
            for (String name :
                    List.of(
                            "desert_pyramid",
                            "desert_well",
                            "ocean_ruin_cold",
                            "ocean_ruin_warm",
                            "trail_ruins_common",
                            "trail_ruins_rare")) {
                var table =
                        level.getServer()
                                .getLootData()
                                .getLootTable(
                                        new ResourceLocation("minecraft", "archaeology/" + name));
                for (int i = 0; i < 10000; i++) {
                    long seed = 123456L + i;
                    var baselineContext =
                            new LootContext.Builder(params)
                                    .withOptionalRandomSeed(seed)
                                    .create(null);
                    var protectedContext =
                            new LootContext.Builder(params)
                                    .withOptionalRandomSeed(seed)
                                    .create(null);
                    field.set(manager, Map.of());
                    TravailConfig.PROTECT_ARCHAEOLOGY_LOOT.set(false);
                    var expected = new ArrayList<ItemStack>();
                    table.getRandomItems(baselineContext, expected::add);
                    field.set(manager, Map.of(id("random_replacement"), randomProbe));
                    TravailConfig.PROTECT_ARCHAEOLOGY_LOOT.set(true);
                    var actual = new ArrayList<ItemStack>();
                    table.getRandomItems(protectedContext, actual::add);
                    check.accept(expected.size() == actual.size(), "same seed output size " + name);
                    for (int j = 0; j < expected.size(); j++)
                        check.accept(
                                ItemStack.matches(expected.get(j), actual.get(j)),
                                "same seed exact item " + name);
                    check.accept(
                            baselineContext.getRandom().nextLong()
                                    == protectedContext.getRandom().nextLong(),
                            "modifier bypass preserves random state " + name);
                    rolls += 2;
                }
            }
            check.accept(randomProbe.calls == 0, "random replacement never executed");
            System.out.println(
                    "ARCHAEOLOGY_PRESSURE PASS rolls="
                            + rolls
                            + " elapsedMs="
                            + (System.nanoTime() - started) / 1000000);
        } catch (ReflectiveOperationException error) {
            throw new AssertionError(error);
        } finally {
            TravailConfig.PROTECT_ARCHAEOLOGY_LOOT.set(previous);
            if (field != null && saved != null) {
                try {
                    field.set(manager, saved);
                } catch (IllegalAccessException error) {
                    throw new AssertionError(error);
                }
            }
        }
    }

    private static Object modifierManager() {
        try {
            var method = ForgeInternalHandler.class.getDeclaredMethod("getLootModifierManager");
            method.setAccessible(true);
            return method.invoke(null);
        } catch (ReflectiveOperationException error) {
            throw new AssertionError(error);
        }
    }

    private static boolean isModified(List<ItemStack> loot) {
        return loot.size() == 1
                && loot.get(0).is(Items.APPLE)
                && loot.get(0).getTag().getBoolean("post_processed");
    }

    private static ResourceLocation id(String path) {
        return new ResourceLocation("travail_smoke", path);
    }

    private static final class Probe implements IGlobalLootModifier {
        private final int mode;
        private int calls;

        Probe(int mode) {
            this.mode = mode;
        }

        public ObjectArrayList<ItemStack> apply(
                ObjectArrayList<ItemStack> loot, LootContext context) {
            calls++;
            switch (mode) {
                case 0 -> {
                    loot.clear();
                    loot.add(new ItemStack(Items.STICK));
                }
                case 1 -> loot.add(new ItemStack(Items.APPLE));
                case 2 ->
                        loot.forEach(
                                stack -> stack.getOrCreateTag().putBoolean("post_processed", true));
                case 3 -> loot.removeIf(stack -> stack.is(Items.STICK));
                case 4 -> {
                    context.getRandom().nextLong();
                    loot.clear();
                    loot.add(new ItemStack(Items.BARRIER));
                }
                default -> throw new AssertionError(mode);
            }
            return loot;
        }

        public Codec<? extends IGlobalLootModifier> codec() {
            throw new UnsupportedOperationException("in-memory test fixture");
        }
    }
}
