package com.thelongtravail.data;

import com.thelongtravail.TravailAspect;
import net.minecraft.nbt.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import java.util.UUID;

public final class JourneyState {
    public static final ResourceLocation BOOK_SOURCE =
            new ResourceLocation("the_long_travail", "book_of_the_dead");

    public enum Format {
        LEGACY,
        CURRENT,
        UNINITIALIZED,
        CORRUPT,
        UNSUPPORTED
    }

    public record AspectJourneyKey(UUID journeyId, TravailAspect aspect, UUID epoch) {
        public AspectJourneyKey {
            java.util.Objects.requireNonNull(journeyId);
            java.util.Objects.requireNonNull(aspect);
            java.util.Objects.requireNonNull(epoch);
        }
    }

    public static CompoundTag root(ItemStack stack) {
        return stack.hasTag() ? stack.getTag().getCompound("LongTravail") : new CompoundTag();
    }

    public static boolean supported(CompoundTag root) {
        for (String field : new String[] {"Witnesses", "ForcedWitnesses", "ForcedMalices"})
            if (root.contains(field) && !root.contains(field, Tag.TAG_INT)) return false;
        for (String field : new String[] {"StateRevision", "RequirementsRevision"})
            if (root.contains(field) && !root.contains(field, Tag.TAG_LONG)) return false;
        if (root.contains("Initialized") && !root.contains("Initialized", Tag.TAG_BYTE))
            return false;
        return (!root.contains("Version") || root.contains("Version", Tag.TAG_INT))
                && root.getInt("Version") >= 0
                && root.getInt("Version") <= 2;
    }

    public static boolean validStorage(ItemStack stack) {
        return !stack.hasTag()
                || !stack.getTag().contains("LongTravail")
                || stack.getTag().contains("LongTravail", Tag.TAG_COMPOUND);
    }

    public static boolean validLists(CompoundTag theme) {
        for (String name :
                new String[] {"Biomes", "Structures", "CompletedBiomes", "CompletedStructures"}) {
            if (!theme.contains(name) && name.startsWith("Completed")) continue;
            if (!theme.contains(name, Tag.TAG_LIST)) return false;
            ListTag list = (ListTag) theme.get(name);
            if (!list.isEmpty() && list.getElementType() != Tag.TAG_STRING) return false;
            var unique = new java.util.HashSet<String>();
            for (int i = 0; i < list.size(); i++) {
                String stored = list.getString(i);
                ResourceLocation id = ResourceLocation.tryParse(stored);
                if (id == null || !id.toString().equals(stored) || !unique.add(stored))
                    return false;
            }
        }
        for (String name : new String[] {"Biomes", "Structures"})
            for (Tag entry : theme.getList(name, Tag.TAG_STRING))
                if (theme.getList("Completed" + name, Tag.TAG_STRING).contains(entry)) return false;
        return true;
    }

    public static Format format(CompoundTag root, TravailAspect aspect) {
        if (!supported(root)) return Format.UNSUPPORTED;
        if (!root.getBoolean("Initialized")) return Format.UNINITIALIZED;
        if (!root.hasUUID("JourneyId") || !root.contains("Requirements", Tag.TAG_COMPOUND))
            return Format.CORRUPT;
        CompoundTag requirements = root.getCompound("Requirements");
        if (!requirements.contains(aspect.id(), Tag.TAG_COMPOUND)) return Format.CORRUPT;
        CompoundTag theme = requirements.getCompound(aspect.id());
        if (!validLists(theme)) return Format.CORRUPT;
        if (theme.contains("ThemeFormat")) {
            if (!theme.contains("ThemeFormat", Tag.TAG_INT)) return Format.CORRUPT;
            if (theme.getInt("ThemeFormat") != 1) return Format.UNSUPPORTED;
            if (!theme.hasUUID("Epoch")
                    || !theme.contains("ConditionVersion", Tag.TAG_INT)
                    || theme.getInt("ConditionVersion") != 2) return Format.CORRUPT;
            if (!validStatus(theme.getString("BiomesStatus"))
                    || !validStatus(theme.getString("StructuresStatus"))) return Format.CORRUPT;
            return validSources(theme) ? Format.CURRENT : Format.CORRUPT;
        }
        if (theme.contains("Epoch") || theme.contains("ConditionVersion")) return Format.CORRUPT;
        if (root.getInt("Version") >= 2
                && (!validStatus(theme.getString("BiomesStatus"))
                        || !validStatus(theme.getString("StructuresStatus"))))
            return Format.CORRUPT;
        return validSources(theme) ? Format.LEGACY : Format.CORRUPT;
    }

    private static boolean validSources(CompoundTag theme) {
        if (!theme.contains("WitnessSources")) return true;
        if (!theme.contains("WitnessSources", Tag.TAG_LIST)) return false;
        ListTag sources = (ListTag) theme.get("WitnessSources");
        if (!sources.isEmpty() && sources.getElementType() != Tag.TAG_STRING) return false;
        for (int i = 0; i < sources.size(); i++)
            if (ResourceLocation.tryParse(sources.getString(i)) == null) return false;
        return true;
    }

    public static AspectJourneyKey identity(CompoundTag root, TravailAspect aspect) {
        Format format = format(root, aspect);
        if (format != Format.LEGACY && format != Format.CURRENT) return null;
        UUID journey = root.getUUID("JourneyId");
        return new AspectJourneyKey(
                journey,
                aspect,
                format == Format.LEGACY
                        ? journey
                        : root.getCompound("Requirements")
                                .getCompound(aspect.id())
                                .getUUID("Epoch"));
    }

    public static AspectJourneyKey identity(ItemStack stack, TravailAspect aspect) {
        return identity(root(stack), aspect);
    }

    public static boolean validConditions(CompoundTag root, TravailAspect aspect) {
        Format format = format(root, aspect);
        if (format != Format.LEGACY && format != Format.CURRENT) return false;
        CompoundTag theme = root.getCompound("Requirements").getCompound(aspect.id());
        if (!validLists(theme)) return false;
        return format == Format.LEGACY && root.getInt("Version") < 2
                || validStatus(theme.getString("BiomesStatus"))
                        && validStatus(theme.getString("StructuresStatus"));
    }

    private static boolean validStatus(String status) {
        return status.equals("READY") || status.equals("DISABLED");
    }

    public static boolean hasWitness(CompoundTag root, TravailAspect aspect) {
        int mask = aspect.mask();
        return (root.getInt("ForcedMalices") & mask) == 0
                && ((root.getInt("ForcedWitnesses") | root.getInt("Witnesses")) & mask) != 0;
    }

    public static int effectiveMask(CompoundTag root) {
        return (root.getInt("Witnesses") | root.getInt("ForcedWitnesses"))
                & ~root.getInt("ForcedMalices")
                & 63;
    }

    public static boolean hasSource(
            ItemStack diary, TravailAspect aspect, ResourceLocation source) {
        CompoundTag root = root(diary);
        return root.getCompound("Requirements")
                        .getCompound(aspect.id())
                        .getList("WitnessSources", Tag.TAG_STRING)
                        .contains(StringTag.valueOf(source.toString()))
                || aspect == TravailAspect.UNDERWORLD
                        && source.equals(BOOK_SOURCE)
                        && root.getBoolean("UnderworldBookWitness");
    }

    public static long stateRevision(ItemStack diary) {
        return root(diary).getLong("StateRevision");
    }

    private JourneyState() {}
}
