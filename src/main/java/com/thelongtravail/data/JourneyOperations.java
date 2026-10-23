package com.thelongtravail.data;

import com.thelongtravail.TravailAspect;
import net.minecraft.nbt.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import java.util.*;

public final class JourneyOperations {
    public enum Status {
        SUCCESS,
        NO_CHANGE,
        NO_DIARY,
        UNINITIALIZED,
        INVALID_DATA,
        INVALID_POOL,
        NO_CONDITIONS,
        NO_RECORDS,
        STALE,
        BUSY
    }

    public enum Kind {
        BIOME,
        STRUCTURE
    }

    public enum Override {
        NATURAL,
        MALICE,
        WITNESS
    }

    public record RequirementRef(Kind kind, ResourceLocation id) {
        public RequirementRef {
            Objects.requireNonNull(kind);
            Objects.requireNonNull(id);
        }

        String listName() {
            return kind == Kind.BIOME ? "Biomes" : "Structures";
        }
    }

    public static CompoundTag generate(RequirementPools.AspectPools pools, RandomSource random) {
        CompoundTag theme = new CompoundTag();
        theme.putInt("ThemeFormat", 1);
        theme.putUUID("Epoch", UUID.randomUUID());
        theme.putInt("ConditionVersion", 2);
        theme.put("Biomes", draw(pools.biomes(), random));
        theme.put("Structures", draw(pools.structures(), random));
        theme.putString("BiomesStatus", pools.biomes().status().name());
        theme.putString("StructuresStatus", pools.structures().status().name());
        return theme;
    }

    private static ListTag draw(RequirementPools.Pool pool, RandomSource random) {
        ListTag list = new ListTag();
        for (WeightedEntry entry :
                WeightedPicker.withoutReplacement(
                        pool.entries(), pool.count(), WeightedEntry::weight, random))
            list.add(StringTag.valueOf(entry.id().toString()));
        return list;
    }

    public static void stateChanged(CompoundTag root) {
        root.putLong("StateRevision", root.getLong("StateRevision") + 1);
    }

    public static void requirementsChanged(CompoundTag root) {
        root.putLong("RequirementsRevision", root.getLong("RequirementsRevision") + 1);
        stateChanged(root);
    }

    public static void updateCompletion(CompoundTag root, TravailAspect aspect) {
        if (!JourneyState.validConditions(root, aspect)) return;
        CompoundTag theme = root.getCompound("Requirements").getCompound(aspect.id());
        if (theme.getList("Biomes", Tag.TAG_STRING).isEmpty()
                && theme.getList("Structures", Tag.TAG_STRING).isEmpty())
            root.putInt("Witnesses", root.getInt("Witnesses") | aspect.mask());
    }

    public static Status complete(CompoundTag root, TravailAspect aspect, RequirementRef target) {
        if (!root.getBoolean("Initialized")) return Status.UNINITIALIZED;
        if (!JourneyState.validConditions(root, aspect)) return Status.INVALID_DATA;
        CompoundTag theme = root.getCompound("Requirements").getCompound(aspect.id());
        ListTag list = theme.getList(target.listName(), Tag.TAG_STRING);
        Tag entry = StringTag.valueOf(target.id().toString());
        if (!list.contains(entry)) return Status.NO_CHANGE;
        ListTag done = theme.getList("Completed" + target.listName(), Tag.TAG_STRING);
        if (!done.contains(entry)) done.add(entry.copy());
        list.removeIf(entry::equals);
        theme.put("Completed" + target.listName(), done);
        updateCompletion(root, aspect);
        requirementsChanged(root);
        return Status.SUCCESS;
    }

    public static Status completeRandom(
            CompoundTag root, TravailAspect aspect, RandomSource random) {
        if (!root.getBoolean("Initialized")) return Status.UNINITIALIZED;
        if (!JourneyState.validConditions(root, aspect)) return Status.INVALID_DATA;
        var theme = root.getCompound("Requirements").getCompound(aspect.id());
        Set<RequirementRef> entries = new LinkedHashSet<>();
        for (Kind kind : Kind.values()) {
            String name = kind == Kind.BIOME ? "Biomes" : "Structures";
            ListTag remaining = theme.getList(name, Tag.TAG_STRING),
                    completed = theme.getList("Completed" + name, Tag.TAG_STRING);
            for (int i = 0; i < remaining.size(); i++) {
                if (completed.contains(remaining.get(i))) return Status.INVALID_DATA;
                entries.add(new RequirementRef(kind, new ResourceLocation(remaining.getString(i))));
            }
        }
        if (entries.isEmpty()) return Status.NO_RECORDS;
        return complete(root, aspect, new ArrayList<>(entries).get(random.nextInt(entries.size())));
    }

    public static Status resetAspect(CompoundTag root, TravailAspect aspect, ServerPlayer player) {
        if (!root.getBoolean("Initialized")) return Status.UNINITIALIZED;
        if (JourneyState.identity(root, aspect) == null) return Status.INVALID_DATA;
        return resetAspect(
                root,
                aspect,
                RequirementPools.current(player).aspects().get(aspect),
                player.getRandom());
    }

    public static Status resetAspect(
            CompoundTag root,
            TravailAspect aspect,
            RequirementPools.AspectPools pools,
            RandomSource random) {
        if (!root.getBoolean("Initialized")) return Status.UNINITIALIZED;
        if (JourneyState.identity(root, aspect) == null) return Status.INVALID_DATA;
        if (pools.biomes().status() == RequirementPools.Status.INVALID
                || pools.structures().status() == RequirementPools.Status.INVALID)
            return Status.INVALID_POOL;
        if (pools.biomes().count() + pools.structures().count() == 0) return Status.NO_CONDITIONS;
        CompoundTag generated = generate(pools, random);
        root.getCompound("Requirements").put(aspect.id(), generated);
        for (String field : new String[] {"Witnesses", "ForcedWitnesses", "ForcedMalices"})
            root.putInt(field, root.getInt(field) & ~aspect.mask());
        if (aspect == TravailAspect.UNDERWORLD) root.remove("UnderworldBookWitness");
        requirementsChanged(root);
        return Status.SUCCESS;
    }

    public static Status grantWitness(
            CompoundTag root, JourneyState.AspectJourneyKey expected, ResourceLocation source) {
        if (expected == null || source == null) return Status.INVALID_DATA;
        if (!expected.equals(JourneyState.identity(root, expected.aspect()))) return Status.STALE;
        CompoundTag theme = root.getCompound("Requirements").getCompound(expected.aspect().id());
        ListTag sources = theme.getList("WitnessSources", Tag.TAG_STRING);
        Tag entry = StringTag.valueOf(source.toString());
        boolean book =
                expected.aspect() == TravailAspect.UNDERWORLD
                        && source.equals(JourneyState.BOOK_SOURCE);
        boolean changed =
                (root.getInt("Witnesses") & expected.aspect().mask()) == 0
                        || !sources.contains(entry)
                        || book && !root.getBoolean("UnderworldBookWitness");
        if (!changed) return Status.NO_CHANGE;
        if (!sources.contains(entry)) sources.add(entry);
        theme.put("WitnessSources", sources);
        root.putInt("Witnesses", root.getInt("Witnesses") | expected.aspect().mask());
        if (book) root.putBoolean("UnderworldBookWitness", true);
        stateChanged(root);
        return Status.SUCCESS;
    }

    public static Status override(
            CompoundTag root, Collection<TravailAspect> aspects, Override value) {
        if (value == null || aspects == null || aspects.stream().anyMatch(Objects::isNull))
            return Status.INVALID_DATA;
        if (!JourneyState.supported(root)) return Status.INVALID_DATA;
        int beforeWitness = root.getInt("ForcedWitnesses"),
                beforeMalice = root.getInt("ForcedMalices");
        int witnesses = beforeWitness, malices = beforeMalice;
        for (TravailAspect aspect : aspects) {
            if (root.getBoolean("Initialized") && JourneyState.identity(root, aspect) == null)
                return Status.INVALID_DATA;
            witnesses &= ~aspect.mask();
            malices &= ~aspect.mask();
            if (value == Override.WITNESS) witnesses |= aspect.mask();
            if (value == Override.MALICE) malices |= aspect.mask();
        }
        if (witnesses == beforeWitness && malices == beforeMalice) return Status.NO_CHANGE;
        root.putInt("ForcedWitnesses", witnesses);
        root.putInt("ForcedMalices", malices);
        stateChanged(root);
        return Status.SUCCESS;
    }

    private JourneyOperations() {}
}
