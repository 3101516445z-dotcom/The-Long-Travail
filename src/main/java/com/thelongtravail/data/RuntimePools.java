package com.thelongtravail.data;

import com.thelongtravail.TheLongTravail;
import com.thelongtravail.config.TravailConfig;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraftforge.registries.ForgeRegistries;
import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;

/** 服务端线程缓存，在注册表可用后及配置重载后重建。 */
public final class RuntimePools {
    public record Snapshot(WeightedTable<EffectEntry> far, WeightedTable<EffectEntry> valley,
                           WeightedTable<RewardEntry> fish, WeightedTable<RewardEntry> phantom,
                           WeightedTable<WeightedEntry> entities, Map<ResourceLocation, Integer> maxLevels,
                           Set<ResourceLocation> levelBonusBlacklist) {}
    private static volatile Snapshot snapshot = new Snapshot(new WeightedTable<>(List.of(), EffectEntry::weight),
            new WeightedTable<>(List.of(), EffectEntry::weight), new WeightedTable<>(List.of(), RewardEntry::weight),
            new WeightedTable<>(List.of(), RewardEntry::weight), new WeightedTable<>(List.of(), WeightedEntry::weight), Map.of(), Set.of());
    public static Snapshot current() { return snapshot; }
    public static void reload() {
        AutomaticEffectBudget.diagnoseConfiguration();
        AltitudePenalty.configReloaded();
        RequirementPools.invalidate();
        var far = effects(TravailConfig.FAR_REACH_POSITIVE_EFFECTS.get());
        var valley = effects(TravailConfig.DEEP_VALLEY_POSITIVE_EFFECTS.get());
        Map<ResourceLocation, Integer> levels = new HashMap<>();
        far.forEach(e -> levels.merge(e.id(), e.maxLevel(), Math::max));
        valley.forEach(e -> levels.merge(e.id(), e.maxLevel(), Math::max));
        snapshot = new Snapshot(new WeightedTable<>(far, EffectEntry::weight), new WeightedTable<>(valley, EffectEntry::weight),
                new WeightedTable<>(rewards(TravailConfig.FISH_SPECIAL_ITEMS.get()), RewardEntry::weight),
                new WeightedTable<>(rewards(TravailConfig.PHANTOM_DROPS.get()), RewardEntry::weight),
                new WeightedTable<>(parse(TravailConfig.FISH_ENTITIES.get(), WeightedEntry::parse,
                        e -> ForgeRegistries.ENTITY_TYPES.containsKey(e.id()), WeightedEntry::id), WeightedEntry::weight), Map.copyOf(levels), levelBonusBlacklist());
        JourneyQueries.clear();
    }
    private static Set<ResourceLocation> levelBonusBlacklist() {
        Set<ResourceLocation> result = new HashSet<>();
        for (String value : TravailConfig.FAR_WITNESS_LEVEL_BLACKLIST.get()) {
            String text = value.trim();
            ResourceLocation id = text.contains(":") ? ResourceLocation.tryParse(text) : null;
            if (id == null) {
                TheLongTravail.LOGGER.warn("Ignoring invalid Far Reach level bonus blacklist ID: {}", value);
                continue;
            }
            result.add(id);
            if (!ForgeRegistries.MOB_EFFECTS.containsKey(id)) {
                TheLongTravail.LOGGER.warn("Far Reach level bonus blacklist effect is not installed: {}", id);
            }
        }
        return Set.copyOf(result);
    }
    private static List<EffectEntry> effects(List<? extends String> source) {
        return parse(source, text -> {
            EffectEntry entry = EffectEntry.parse(text);
            if (entry != null && Integer.parseInt(text.split("\\|")[1].trim()) > 256)
                TheLongTravail.LOGGER.warn("Capping effect level at 256: {}", text);
            return entry;
        }, e -> ForgeRegistries.MOB_EFFECTS.containsKey(e.id())
                && ForgeRegistries.MOB_EFFECTS.getValue(e.id()).getCategory() == MobEffectCategory.BENEFICIAL, EffectEntry::id);
    }
    private static List<RewardEntry> rewards(List<? extends String> source) {
        return parse(source, RewardEntry::parse, e -> ForgeRegistries.ITEMS.containsKey(e.id()), RewardEntry::id);
    }

    private static <T> List<T> parse(List<? extends String> source, Function<String, T> parser,
                                    Predicate<T> valid, Function<T, ResourceLocation> id) {
        Map<ResourceLocation, T> entries = new LinkedHashMap<>();
        for (String text : source) {
            T entry = parser.apply(text);
            if (entry == null || !valid.test(entry)) {
                TheLongTravail.LOGGER.warn("Ignoring invalid/unavailable random pool entry: {}", text);
            } else if (entries.putIfAbsent(id.apply(entry), entry) != null) {
                TheLongTravail.LOGGER.warn("Ignoring duplicate random pool target: {}", text);
            }
        }
        return List.copyOf(entries.values());
    }

    private RuntimePools() {}
}
