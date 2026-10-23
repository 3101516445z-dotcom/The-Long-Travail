package com.thelongtravail.data;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

public final class TravailAdvancements {
    private static final ResourceLocation ROOT = new ResourceLocation("the_long_travail", "root");

    private TravailAdvancements() {}

    public static void onDiaryEquipped(ServerPlayer player) {
        var advancement = player.server.getAdvancements().getAdvancement(ROOT);
        if (advancement != null
                && !player.getAdvancements().getOrStartProgress(advancement).isDone()) {
            player.getAdvancements().award(advancement, "wear_long_travail");
        }
    }
}
