package com.thelongtravail.data;

import com.thelongtravail.TravailAspect;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

public class RevelationProgressTest {
    static void check(boolean value, String message) { if (!value) throw new AssertionError(message); }
    public static void main(String[] args) throws Exception {
        CompoundTag playerData = new CompoundTag();
        CompoundTag visited = new CompoundTag();
        visited.putBoolean("minecraft:plains", true);
        playerData.put("visited_biomes", visited);
        int expectedMask = 0;
        for (var aspect : TravailAspect.values()) {
            check(PlayerJourneyData.reveal(playerData, aspect), "First use unlocks");
            expectedMask |= aspect.mask();
            check(playerData.getInt("revealed_aspects") == expectedMask, "Only matching mist is revealed");
            check(!PlayerJourneyData.reveal(playerData, aspect), "Duplicate use must not consume");
            check(playerData.getCompound("visited_biomes").getBoolean("minecraft:plains"), "Preserve exploration data");
        }
        ByteArrayOutputStream saved = new ByteArrayOutputStream();
        NbtIo.writeCompressed(playerData, saved);
        CompoundTag reloaded = NbtIo.readCompressed(new ByteArrayInputStream(saved.toByteArray()));
        check(reloaded.equals(playerData), "Unlocks survive serialized player saves");
        CompoundTag respawnCopy = reloaded.copy();
        for (var aspect : TravailAspect.values())
            check(!PlayerJourneyData.reveal(respawnCopy, aspect), "Respawn copy retains all six unlocks");
        respawnCopy.putInt("revealed_aspects", 0);
        check(reloaded.getInt("revealed_aspects") == 63, "Clone data must not alias original player data");
        check(PlayerJourneyData.reveal(new CompoundTag(), TravailAspect.ABYSS), "Different player starts locked");
        System.out.println("PASS: six independent unlocks, duplicates, NBT save/reload, clone copy, player isolation, biome preservation.");
    }
}
