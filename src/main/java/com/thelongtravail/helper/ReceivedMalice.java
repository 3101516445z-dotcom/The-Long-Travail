package com.thelongtravail.helper;
import com.thelongtravail.config.TravailConfig;
import com.thelongtravail.event.TravailEvents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import java.util.Map;
import java.util.WeakHashMap;

/** 服务端线程状态，由嵌套伤害共用；触发效果前先预留冷却。 */
public final class ReceivedMalice {
    private static final Map<ServerPlayer, Long> LAST = new WeakHashMap<>();
    public static void forget(ServerPlayer player) { LAST.remove(player); }
    public static void clear() { LAST.clear(); }
    public static void trigger(ServerPlayer player, DamageSource source) {
        if (TravailCurios.stack(player).isEmpty()) return;
        String id = source.typeHolder().unwrapKey().map(key -> key.location().toString()).orElse("");
        if (TravailConfig.RECEIVED_EXCLUSIONS.get().contains(id)) return;
        long now = player.level().getGameTime();
        Long last = LAST.get(player);
        int cooldown = TravailConfig.RECEIVED_COOLDOWN.get();
        if (last != null && now >= last && now - last < cooldown) return;
        LAST.put(player, now);
        TravailEvents.processBeingAttacked(player);
    }
    private ReceivedMalice() {}
}
