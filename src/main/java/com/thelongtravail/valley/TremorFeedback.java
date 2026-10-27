package com.thelongtravail.valley;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import java.util.EnumMap;
import java.util.Map;
import java.util.WeakHashMap;

// 服务端按使用者及提示类型限频，不保存离线玩家。
public enum TremorFeedback {
    FAILURE("failure"),
    BLACKLISTED("blacklisted");
    private static final Map<ServerPlayer, EnumMap<TremorFeedback, Integer>> LAST =
            new WeakHashMap<>();
    private final String key;

    TremorFeedback(String suffix) {
        key = "message.the_long_travail.high_frequency_tremor." + suffix;
    }

    public void send(ServerPlayer player) {
        var times = LAST.computeIfAbsent(player, ignored -> new EnumMap<>(TremorFeedback.class));
        int now = player.tickCount;
        Integer previous = times.get(this);
        if (previous != null && now >= previous && (long) now - previous < 40) return;
        times.put(this, now);
        player.displayClientMessage(Component.translatable(key), true);
    }
}
