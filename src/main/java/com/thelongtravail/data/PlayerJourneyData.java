package com.thelongtravail.data;

import com.thelongtravail.TheLongTravail;
import com.thelongtravail.TravailAspect;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

public final class PlayerJourneyData {
    private static final String VISITED_BIOMES = "visited_biomes";
    private static final String REVEALED_ASPECTS = "revealed_aspects";

    public static int revealedMask(Player player) {
        return getModTag(player).getInt(REVEALED_ASPECTS) & 63;
    }

    public static boolean reveal(ServerPlayer player, TravailAspect aspect) {
        CompoundTag mod = getModTag(player);
        if (!reveal(mod, aspect)) return false;
        saveModTag(player, mod);
        return true;
    }

    static boolean reveal(CompoundTag mod, TravailAspect aspect) {
        int previous = mod.getInt(REVEALED_ASPECTS);
        if ((previous & aspect.mask()) != 0) return false;
        mod.putInt(REVEALED_ASPECTS, previous | aspect.mask());
        return true;
    }

    public static void copyTo(Player original, Player replacement) {
        saveModTag(replacement, getModTag(original).copy());
    }

    public static boolean discoverBiome(ServerPlayer player, ResourceLocation biome) {
        CompoundTag mod = getModTag(player);
        CompoundTag visited = mod.getCompound(VISITED_BIOMES);
        String key = biome.toString();
        if (visited.contains(key)) return false;
        visited.putBoolean(key, true);
        mod.put(VISITED_BIOMES, visited);
        saveModTag(player, mod);
        return true;
    }

    public static int discoveredBiomeCount(ServerPlayer player) {
        return getModTag(player).getCompound(VISITED_BIOMES).getAllKeys().size();
    }

    private static CompoundTag getModTag(Player player) {
        CompoundTag persisted = player.getPersistentData().getCompound(Player.PERSISTED_NBT_TAG);
        return persisted.getCompound(TheLongTravail.MODID);
    }

    private static void saveModTag(Player player, CompoundTag mod) {
        CompoundTag persistentData = player.getPersistentData();
        CompoundTag persisted = persistentData.getCompound(Player.PERSISTED_NBT_TAG);
        persisted.put(TheLongTravail.MODID, mod);
        persistentData.put(Player.PERSISTED_NBT_TAG, persisted);
    }

    private PlayerJourneyData() {}
}
