package com.thelongtravail.data;

import com.thelongtravail.TravailAspect;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class LongTravailData {
    private static final String ROOT = "LongTravail";
    private static final String INITIALIZED = "Initialized";
    private static final String VERSION = "Version";
    private static final String JOURNEY_ID = "JourneyId";
    private static final String OWNER_ID = "OwnerId";
    private static final String WITNESSES = "Witnesses";
    private static final String FORCED_WITNESSES = "ForcedWitnesses";
    private static final String FORCED_MALICES = "ForcedMalices";
    private static final String REQUIREMENTS = "Requirements";
    private static final String REQUIREMENTS_REVISION = "RequirementsRevision";
    private static final int DATA_VERSION = 2;

    public static void initialize(ItemStack stack, ServerPlayer player) {
        tryInitialize(stack, player);
    }

    /** Transactional generation: failure leaves every byte of the original stack untouched. */
    public static boolean tryInitialize(ItemStack stack, ServerPlayer player) {
        if (isInitialized(stack)) return true; // Version 1 journeys retain their progress and witnesses.
        RequirementPools.Snapshot pools = RequirementPools.current(player);
        if (!pools.valid()) return false;
        CompoundTag root = root(stack).copy();
        root.putInt(VERSION, DATA_VERSION);
        root.putUUID(JOURNEY_ID, UUID.randomUUID());
        root.putUUID(OWNER_ID, player.getUUID());
        root.putInt(WITNESSES, 0);

        RandomSource random = player.getRandom();
        CompoundTag requirements = new CompoundTag();
        for (TravailAspect aspect : TravailAspect.values()) {
            CompoundTag aspectTag = new CompoundTag();
            var selected = pools.aspects().get(aspect);
            aspectTag.put("Biomes", draw(selected.biomes(), random));
            aspectTag.put("Structures", draw(selected.structures(), random));
            aspectTag.putString("BiomesStatus", selected.biomes().status().name());
            aspectTag.putString("StructuresStatus", selected.structures().status().name());
            requirements.put(aspect.id(), aspectTag);
        }
        root.put(REQUIREMENTS, requirements);
        root.putLong(REQUIREMENTS_REVISION, 1L);
        root.putBoolean(INITIALIZED, true);
        updateCompletions(root);
        saveRoot(stack, root);
        return true;
    }

    /** Build off-slot first: a generation failure must not erase the equipped diary. */
    public static ItemStack refreshedCopy(ItemStack original, ServerPlayer player) {
        ItemStack refreshed = original.copy();
        if (refreshed.hasTag()) refreshed.getTag().remove(ROOT);
        return tryInitialize(refreshed, player) ? refreshed : ItemStack.EMPTY;
    }

    public static boolean isInitialized(ItemStack stack) {
        return stack.hasTag() && stack.getTag().contains(ROOT, Tag.TAG_COMPOUND)
                && stack.getTag().getCompound(ROOT).getBoolean(INITIALIZED);
    }

    public static boolean hasWitness(ItemStack stack, TravailAspect aspect) {
        CompoundTag root = root(stack);
        if ((root.getInt(FORCED_MALICES) & aspect.mask()) != 0) return false;
        if ((root.getInt(FORCED_WITNESSES) & aspect.mask()) != 0) return true;
        return (root.getInt(WITNESSES) & aspect.mask()) != 0;
    }

    public static boolean visitBiome(ItemStack stack, ResourceLocation biome) {
        return removeRequirement(stack, "Biomes", biome);
    }

    public static boolean visitStructure(ItemStack stack, ResourceLocation structure) {
        return removeRequirement(stack, "Structures", structure);
    }

    /** Constant-size snapshot. NBT references are compared by identity, never by deep equality. */
    public record RequirementIdentity(ItemStack stack, Tag root, Tag requirements, UUID journey, long revision) {
        @Override public boolean equals(Object other) {
            return other instanceof RequirementIdentity key && stack == key.stack && root == key.root
                    && requirements == key.requirements && revision == key.revision && java.util.Objects.equals(journey, key.journey);
        }
        @Override public int hashCode() {
            return java.util.Objects.hash(System.identityHashCode(stack), System.identityHashCode(root),
                    System.identityHashCode(requirements), journey, revision);
        }
    }
    public static RequirementIdentity queryIdentity(ItemStack stack) {
        CompoundTag root = stack.hasTag() && stack.getTag().contains(ROOT, Tag.TAG_COMPOUND)
                ? stack.getTag().getCompound(ROOT) : null;
        return new RequirementIdentity(stack, root, root == null ? null : root.get(REQUIREMENTS),
                root != null && root.hasUUID(JOURNEY_ID) ? root.getUUID(JOURNEY_ID) : null,
                root == null ? 0 : root.getLong(REQUIREMENTS_REVISION));
    }
    /** External integrations editing requirements in-place must call this once after their changes. */
    public static void requirementsChanged(ItemStack stack) {
        if (!isInitialized(stack)) return;
        CompoundTag root = root(stack);
        updateCompletions(root);
        bumpRequirementsRevision(root);
        saveRoot(stack, root);
    }
    private static void bumpRequirementsRevision(CompoundTag root) {
        root.putLong(REQUIREMENTS_REVISION, root.getLong(REQUIREMENTS_REVISION) + 1L);
    }
    public static boolean hasRemainingBiomes(ItemStack stack) {
        CompoundTag requirements = root(stack).getCompound(REQUIREMENTS);
        for (TravailAspect aspect : TravailAspect.values())
            if (!requirements.getCompound(aspect.id()).getList("Biomes", Tag.TAG_STRING).isEmpty()) return true;
        return false;
    }

    public static List<ResourceLocation> remainingStructures(ItemStack stack) {
        java.util.Set<ResourceLocation> result = new java.util.LinkedHashSet<>();
        CompoundTag requirements = root(stack).getCompound(REQUIREMENTS);
        for (TravailAspect aspect : TravailAspect.values()) {
            ListTag list = requirements.getCompound(aspect.id()).getList("Structures", Tag.TAG_STRING);
            for (int i = 0; i < list.size(); i++) {
                ResourceLocation id = ResourceLocation.tryParse(list.getString(i));
                if (id != null) result.add(id);
            }
        }
        return List.copyOf(result);
    }

    public static List<RequirementStatus> requirementsForDisplay(ItemStack stack, TravailAspect aspect, boolean structures) {
        List<RequirementStatus> result = new ArrayList<>();
        CompoundTag aspectTag = root(stack).getCompound(REQUIREMENTS).getCompound(aspect.id());
        String listName = structures ? "Structures" : "Biomes";
        for (boolean completed : List.of(false, true)) {
            String key = completed ? "Completed" + listName : listName;
            ListTag list = aspectTag.getList(key, Tag.TAG_STRING);
            for (int i = 0; i < list.size(); i++) {
                ResourceLocation id = ResourceLocation.tryParse(list.getString(i));
                if (id != null && result.stream().noneMatch(entry -> entry.id().equals(id))) {
                    result.add(new RequirementStatus(id, completed));
                }
            }
        }
        return result;
    }

    public static void setWitness(ItemStack stack, TravailAspect aspect, boolean witness) {
        CompoundTag root = root(stack);
        int forcedWitnesses = root.getInt(FORCED_WITNESSES);
        int forcedMalices = root.getInt(FORCED_MALICES);
        if (witness) {
            forcedWitnesses |= aspect.mask();
            forcedMalices &= ~aspect.mask();
        } else {
            forcedMalices |= aspect.mask();
            forcedWitnesses &= ~aspect.mask();
        }
        root.putInt(FORCED_WITNESSES, forcedWitnesses);
        root.putInt(FORCED_MALICES, forcedMalices);
        saveRoot(stack, root);
    }

    public static void clearForcedWitnessState(ItemStack stack, TravailAspect aspect) {
        CompoundTag root = root(stack);
        if (aspect == null) {
            root.remove(FORCED_WITNESSES);
            root.remove(FORCED_MALICES);
        } else {
            root.putInt(FORCED_WITNESSES, root.getInt(FORCED_WITNESSES) & ~aspect.mask());
            root.putInt(FORCED_MALICES, root.getInt(FORCED_MALICES) & ~aspect.mask());
        }
        saveRoot(stack, root);
    }

    public record RequirementStatus(ResourceLocation id, boolean completed) {}

    public static long getLong(ItemStack stack, String key) {
        return root(stack).getLong(key);
    }

    public static void putLong(ItemStack stack, String key, long value) {
        CompoundTag root = root(stack);
        root.putLong(key, value);
        saveRoot(stack, root);
    }

    private static boolean removeRequirement(ItemStack stack, String listName, ResourceLocation target) {
        if (!isInitialized(stack)) return false;
        CompoundTag root = root(stack);
        CompoundTag requirements = root.getCompound(REQUIREMENTS);
        boolean changed = false;
        for (TravailAspect aspect : TravailAspect.values()) {
            CompoundTag aspectTag = requirements.getCompound(aspect.id());
            ListTag list = aspectTag.getList(listName, Tag.TAG_STRING);
            ListTag completed = aspectTag.getList("Completed" + listName, Tag.TAG_STRING);
            for (int i = list.size() - 1; i >= 0; i--) {
                if (target.toString().equals(list.getString(i))) {
                    if (!completed.contains(list.get(i))) completed.add(list.get(i).copy());
                    list.remove(i);
                    changed = true;
                }
            }
            aspectTag.put(listName, list);
            aspectTag.put("Completed" + listName, completed);
            requirements.put(aspect.id(), aspectTag);
        }
        if (changed) {
            root.put(REQUIREMENTS, requirements);
            updateCompletions(root);
            bumpRequirementsRevision(root);
            saveRoot(stack, root);
        }
        return changed;
    }

    private static void updateCompletions(CompoundTag root) {
        int witnesses = root.getInt(WITNESSES);
        CompoundTag requirements = root.getCompound(REQUIREMENTS);
        for (TravailAspect aspect : TravailAspect.values()) {
            if (!requirements.contains(aspect.id(), Tag.TAG_COMPOUND)) continue;
            CompoundTag aspectTag = requirements.getCompound(aspect.id());
            boolean valid = root.getInt(VERSION) < 2 || (validStatus(aspectTag.getString("BiomesStatus"))
                    && validStatus(aspectTag.getString("StructuresStatus")));
            if (valid && aspectTag.contains("Biomes", Tag.TAG_LIST) && aspectTag.contains("Structures", Tag.TAG_LIST)
                    && aspectTag.getList("Biomes", Tag.TAG_STRING).isEmpty()
                    && aspectTag.getList("Structures", Tag.TAG_STRING).isEmpty()) {
                witnesses |= aspect.mask();
            }
        }
        root.putInt(WITNESSES, witnesses);
    }

    private static boolean validStatus(String status) {
        return "READY".equals(status) || "DISABLED".equals(status);
    }

    private static ListTag draw(RequirementPools.Pool pool, RandomSource random) {
        ListTag result = new ListTag();
        for (WeightedEntry entry : WeightedPicker.withoutReplacement(pool.entries(), pool.count(), WeightedEntry::weight, random)) {
            result.add(StringTag.valueOf(entry.id().toString()));
        }
        return result;
    }

    private static CompoundTag root(ItemStack stack) {
        return stack.hasTag() ? stack.getTag().getCompound(ROOT) : new CompoundTag();
    }

    private static void saveRoot(ItemStack stack, CompoundTag root) {
        stack.getOrCreateTag().put(ROOT, root);
    }

    private LongTravailData() {}
}
