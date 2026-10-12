package com.thelongtravail.data;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

// 完成状态由原版进度系统保存；调用方须先确认玩家已佩戴苦旅。
public final class TravailAdvancements {
    private static final ResourceLocation ROOT = new ResourceLocation("the_long_travail", "root");

    private TravailAdvancements() {}

    public static void onDiaryEquipped(ServerPlayer player) {
        var advancement = player.server.getAdvancements().getAdvancement(ROOT);
        // 数据包可能移除此进度，不跨资源重载缓存进度对象。
        if (advancement != null
                && !player.getAdvancements().getOrStartProgress(advancement).isDone()) {
            player.getAdvancements().award(advancement, "wear_long_travail");
        }
    }
}
