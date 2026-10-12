package com.thelongtravail.data;

import com.thelongtravail.TravailAspect;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
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
    private static final String REQUIREMENTS = "Requirements";
    private static final String REQUIREMENTS_REVISION = "RequirementsRevision";
    private static final int DATA_VERSION = 2;

    public static void initialize(ItemStack stack, ServerPlayer player) {
        tryInitialize(stack, player);
    }

    // 事务式生成：失败时原物品数据保持不变。
    public static boolean tryInitialize(ItemStack stack, ServerPlayer player) {
        if (!JourneyState.validStorage(stack)) return false;
        if (isInitialized(stack)) return JourneyState.supported(root(stack)); // 保留版本 1 旅程的进度和见证。
        if (!JourneyState.supported(root(stack))) return false;
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
            CompoundTag aspectTag = JourneyOperations.generate(pools.aspects().get(aspect), random);
            requirements.put(aspect.id(), aspectTag);
        }
        root.put(REQUIREMENTS, requirements);
        root.putLong(REQUIREMENTS_REVISION, 1L);
        root.putBoolean(INITIALIZED, true);
        updateCompletions(root);
        saveRoot(stack, root);
        return true;
    }

    // 先在槽位外生成，失败时不能清除已装备的日记。
    public static ItemStack refreshedCopy(ItemStack original, ServerPlayer player) {
        if (!JourneyState.validStorage(original)) return ItemStack.EMPTY;
        CompoundTag existing = root(original);
        if (!JourneyState.supported(existing)) return ItemStack.EMPTY;
        if (isInitialized(original))
            for (TravailAspect aspect : TravailAspect.values())
                if (JourneyState.identity(existing, aspect) == null) return ItemStack.EMPTY;
        ItemStack refreshed = original.copy();
        if (refreshed.hasTag()) refreshed.getTag().remove(ROOT);
        return tryInitialize(refreshed, player) ? refreshed : ItemStack.EMPTY;
    }

    public static boolean isInitialized(ItemStack stack) {
        return stack.hasTag()
                && stack.getTag().contains(ROOT, Tag.TAG_COMPOUND)
                && stack.getTag().getCompound(ROOT).getBoolean(INITIALIZED);
    }

    public static boolean hasWitness(ItemStack stack, TravailAspect aspect) {
        return JourneyState.hasWitness(root(stack), aspect);
    }

    public static boolean visitBiome(ItemStack stack, ResourceLocation biome) {
        return removeRequirement(stack, "Biomes", biome);
    }

    public static boolean visitStructure(ItemStack stack, ResourceLocation structure) {
        return removeRequirement(stack, "Structures", structure);
    }

    // 固定大小快照；NBT 引用按对象身份比较，不做深度比较。
    public record RequirementIdentity(
            ItemStack stack, Tag root, Tag requirements, UUID journey, long revision) {
        @Override
        public boolean equals(Object other) {
            return other instanceof RequirementIdentity key
                    && stack == key.stack
                    && root == key.root
                    && requirements == key.requirements
                    && revision == key.revision
                    && java.util.Objects.equals(journey, key.journey);
        }

        @Override
        public int hashCode() {
            return java.util.Objects.hash(
                    System.identityHashCode(stack),
                    System.identityHashCode(root),
                    System.identityHashCode(requirements),
                    journey,
                    revision);
        }
    }

    public static RequirementIdentity queryIdentity(ItemStack stack) {
        CompoundTag root =
                stack.hasTag() && stack.getTag().contains(ROOT, Tag.TAG_COMPOUND)
                        ? stack.getTag().getCompound(ROOT)
                        : null;
        return new RequirementIdentity(
                stack,
                root,
                root == null ? null : root.get(REQUIREMENTS),
                root != null && root.hasUUID(JOURNEY_ID) ? root.getUUID(JOURNEY_ID) : null,
                root == null ? 0 : root.getLong(REQUIREMENTS_REVISION));
    }

    // 外部集成原地修改需求后，必须调用一次以使缓存失效。
    public static void requirementsChanged(ItemStack stack) {
        if (!isInitialized(stack)) return;
        CompoundTag root = root(stack);
        updateCompletions(root);
        bumpRequirementsRevision(root);
        saveRoot(stack, root);
    }

    private static void bumpRequirementsRevision(CompoundTag root) {
        JourneyOperations.requirementsChanged(root);
    }

    public static boolean hasRemainingBiomes(ItemStack stack) {
        CompoundTag requirements = root(stack).getCompound(REQUIREMENTS);
        for (TravailAspect aspect : TravailAspect.values())
            if (!requirements.getCompound(aspect.id()).getList("Biomes", Tag.TAG_STRING).isEmpty())
                return true;
        return false;
    }

    public static List<ResourceLocation> remainingStructures(ItemStack stack) {
        return remainingRequirements(stack, "Structures");
    }

    public static List<ResourceLocation> remainingBiomes(ItemStack stack) {
        return remainingRequirements(stack, "Biomes");
    }

    private static List<ResourceLocation> remainingRequirements(ItemStack stack, String listName) {
        java.util.Set<ResourceLocation> result = new java.util.LinkedHashSet<>();
        CompoundTag requirements = root(stack).getCompound(REQUIREMENTS);
        for (TravailAspect aspect : TravailAspect.values()) {
            ListTag list = requirements.getCompound(aspect.id()).getList(listName, Tag.TAG_STRING);
            for (int i = 0; i < list.size(); i++) {
                ResourceLocation id = ResourceLocation.tryParse(list.getString(i));
                if (id != null) result.add(id);
            }
        }
        return List.copyOf(result);
    }

    public static List<RequirementStatus> requirementsForDisplay(
            ItemStack stack, TravailAspect aspect, boolean structures) {
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
        if (JourneyOperations.override(
                        root,
                        List.of(aspect),
                        witness
                                ? JourneyOperations.Override.WITNESS
                                : JourneyOperations.Override.MALICE)
                == JourneyOperations.Status.SUCCESS) saveRoot(stack, root);
    }

    public static void clearForcedWitnessState(ItemStack stack, TravailAspect aspect) {
        CompoundTag root = root(stack);
        if (JourneyOperations.override(
                        root,
                        aspect == null ? List.of(TravailAspect.values()) : List.of(aspect),
                        JourneyOperations.Override.NATURAL)
                == JourneyOperations.Status.SUCCESS) saveRoot(stack, root);
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

    private static boolean removeRequirement(
            ItemStack stack, String listName, ResourceLocation target) {
        if (!isInitialized(stack)) return false;
        CompoundTag root = root(stack);
        long revision = root.getLong(REQUIREMENTS_REVISION),
                stateRevision = root.getLong("StateRevision");
        var ref =
                new JourneyOperations.RequirementRef(
                        listName.equals("Biomes")
                                ? JourneyOperations.Kind.BIOME
                                : JourneyOperations.Kind.STRUCTURE,
                        target);
        boolean changed = false;
        for (TravailAspect aspect : TravailAspect.values())
            changed |=
                    JourneyOperations.complete(root, aspect, ref)
                            == JourneyOperations.Status.SUCCESS;
        if (changed) {
            root.putLong(REQUIREMENTS_REVISION, revision + 1);
            root.putLong("StateRevision", stateRevision + 1);
            saveRoot(stack, root);
        }
        return changed;
    }

    private static void updateCompletions(CompoundTag root) {
        for (TravailAspect aspect : TravailAspect.values())
            JourneyOperations.updateCompletion(root, aspect);
    }

    private static CompoundTag root(ItemStack stack) {
        return stack.hasTag() ? stack.getTag().getCompound(ROOT) : new CompoundTag();
    }

    private static void saveRoot(ItemStack stack, CompoundTag root) {
        stack.getOrCreateTag().put(ROOT, root);
    }

    private LongTravailData() {}
}
