package com.thelongtravail.data;
import com.thelongtravail.TheLongTravail;
import com.thelongtravail.network.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.*;
import net.minecraftforge.registries.ForgeRegistries;
import java.util.*;
/** 状态归服务端线程管理；外部添加或移除效果时放弃对该效果的所有权。 */
public final class EffectChanges {
    private static final Map<ServerPlayer, Map<MobEffect, MobEffectInstance>> OWNED = new WeakHashMap<>();
    private static final Map<ServerPlayer, Diagnostic> DIAGNOSTICS = new WeakHashMap<>();
    private record Context(ServerPlayer player, MobEffect effect) {}
    private static final ThreadLocal<Context> CONTEXT = new ThreadLocal<>();
    private static long serial;
    private static final Map<ServerPlayer, Map<MobEffect, MobEffectInstance>> EXPIRING_MALICE = new WeakHashMap<>();
    private static final ResourceLocation CONTROL = new ResourceLocation("the_long_travail", "control");
    private static final class Diagnostic { long until, next; Map<String, Integer> counts = new LinkedHashMap<>(); }
    public static int diagnose(ServerPlayer player) {
        Diagnostic diagnostic = new Diagnostic();
        long now = player.level().getGameTime(); diagnostic.until = now + 600; diagnostic.next = now + 100;
        DIAGNOSTICS.put(player, diagnostic);
        notice(player, CONTROL, EffectNotice.Kind.DIAGNOSE);
        player.sendSystemMessage(net.minecraft.network.chat.Component.literal("Long Travail: effect diagnostics enabled for 30 seconds (server/client logs)."));
        return 1;
    }
    private static void count(ServerPlayer player, String event, MobEffect effect) {
        var d = DIAGNOSTICS.get(player); if (d == null) return;
        String key = event + ":" + ForgeRegistries.MOB_EFFECTS.getKey(effect);
        if (d.counts.size() >= 256 && !d.counts.containsKey(key)) key = "other";
        d.counts.merge(key, 1, Integer::sum);
    }
    private static void notice(ServerPlayer player, ResourceLocation id, EffectNotice.Kind kind) {
        if (id != null) TravailNetwork.sendEffectNotice(player, new EffectNotice(id, kind, ++serial, player.level().getGameTime()));
    }
    public static boolean add(ServerPlayer player, MobEffectInstance incoming) {
        MobEffect effect = incoming.getEffect(); var before = player.getEffect(effect);
        var owned = OWNED.get(player); boolean wasOwned = owned != null && owned.get(effect) == before && before != null;
        Context old = CONTEXT.get(); CONTEXT.set(new Context(player, effect)); boolean added;
        try { added = player.addEffect(incoming); } finally { if (old == null) CONTEXT.remove(); else CONTEXT.set(old); }
        var after = player.getEffect(effect);
        if (added && after != null) {
            count(player, "automatic-add", effect);
            // 不能仅因隐藏效果链发生变化，就接管已存在的无关效果。
            if (before == null || wasOwned) {
                var map = OWNED.computeIfAbsent(player, p -> new HashMap<>());
                if (map.size() < 256 || map.containsKey(effect)) map.put(effect, after);
            }
            if (before == null) notice(player, ForgeRegistries.MOB_EFFECTS.getKey(effect), EffectNotice.Kind.GAIN);
        }
        return added;
    }
    public static boolean remove(ServerPlayer player, MobEffect effect, boolean malice) {
        boolean present = player.hasEffect(effect);
        Context old = CONTEXT.get(); CONTEXT.set(new Context(player, effect)); boolean removed;
        try { removed = malice ? forceRemove(player, effect) : player.removeEffect(effect); } finally { if (old == null) CONTEXT.remove(); else CONTEXT.set(old); }
        boolean gone = present && removed && !player.hasEffect(effect);
        count(player, gone ? (malice ? "malice-remove" : "automatic-clear") : "remove-failed", effect);
        if (gone) {
            externalChange(player, effect);
            notice(player, ForgeRegistries.MOB_EFFECTS.getKey(effect), malice ? EffectNotice.Kind.MALICE_CLEAR : EffectNotice.Kind.CLEAR);
        }
        return gone;
    }
    /** 先直接移除一次，失败则将剩余时间设为一刻，不循环重试。 */
    private static boolean forceRemove(ServerPlayer player, MobEffect effect) {
        if (!player.hasEffect(effect)) return false;
        MobEffectInstance removed = player.removeEffectNoUpdate(effect);
        if (removed != null) {
            ((com.thelongtravail.mixin.EffectRemovalInvoker) player).travail$onEffectRemoved(removed);
        }
        MobEffectInstance remaining = player.getEffect(effect);
        if (remaining == null) return true;
        // 直接修改当前实例，包括无限持续效果，并丢弃隐藏的后备效果，
        // 防止当前效果到期后由较弱或更持久的效果接替。
        var access = (com.thelongtravail.mixin.EffectDurationAccessor) remaining;
        access.travail$hiddenEffect(null);
        access.travail$duration(1);
        player.serverLevel().getChunkSource().broadcastAndSend(player,
                new net.minecraft.network.protocol.game.ClientboundUpdateMobEffectPacket(player.getId(), remaining));
        EXPIRING_MALICE.computeIfAbsent(player, ignored -> new HashMap<>()).put(effect, remaining);
        var owned = OWNED.get(player);
        if (owned != null) { owned.remove(effect); if (owned.isEmpty()) OWNED.remove(player); }
        count(player, "malice-expiry-fallback", effect);
        return false; // 安排到期不代表已成功移除。
    }
    private static void checkMaliceExpiry(ServerPlayer player) {
        var pending = EXPIRING_MALICE.get(player);
        if (pending == null) return;
        pending.entrySet().removeIf(entry -> {
            var current = player.getEffect(entry.getKey());
            if (current == entry.getValue() && current.getDuration() <= 1) return false;
            if (current == null && entry.getValue().getDuration() == 0) {
                count(player, "malice-remove", entry.getKey());
                notice(player, ForgeRegistries.MOB_EFFECTS.getKey(entry.getKey()), EffectNotice.Kind.MALICE_CLEAR);
            }
            return true;
        });
        if (pending.isEmpty()) EXPIRING_MALICE.remove(player);
    }
    public static void externalChange(ServerPlayer player, MobEffect effect) {
        Context context = CONTEXT.get();
        if (context != null && context.player == player && context.effect == effect) return;
        var owned = OWNED.get(player); if (owned != null) { owned.remove(effect); if (owned.isEmpty()) OWNED.remove(player); }
    }
    public static void tick(ServerPlayer player) {
        checkMaliceExpiry(player);
        var owned = OWNED.get(player);
        if (owned != null) {
            var iterator = owned.entrySet().iterator();
            while (iterator.hasNext()) {
                var entry = iterator.next(); var current = player.getEffect(entry.getKey());
                if (current != entry.getValue()) {
                    if (current == null && entry.getValue().getDuration() == 0) {
                        notice(player, ForgeRegistries.MOB_EFFECTS.getKey(entry.getKey()), EffectNotice.Kind.EXPIRE);
                        count(player, "automatic-expire", entry.getKey());
                    }
                    iterator.remove();
                }
            }
            if (owned.isEmpty()) OWNED.remove(player);
        }
        var d = DIAGNOSTICS.get(player); if (d == null) return;
        long now = player.level().getGameTime();
        if (now >= d.next) {
            TheLongTravail.LOGGER.info("Travail effect diagnostic [server {}]: {}", player.getUUID(), d.counts);
            d.counts.clear(); d.next = now + 100;
        }
        if (now >= d.until) DIAGNOSTICS.remove(player);
    }
    public static void unequipped(ServerPlayer player) {
        if (OWNED.remove(player) != null) notice(player, CONTROL, EffectNotice.Kind.RESET);
    }
    public static void forget(ServerPlayer player) {
        OWNED.remove(player); DIAGNOSTICS.remove(player); EXPIRING_MALICE.remove(player);
        notice(player, CONTROL, EffectNotice.Kind.RESET);
    }
    private EffectChanges() {}
}
