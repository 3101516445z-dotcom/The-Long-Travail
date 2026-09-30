package com.thelongtravail.data;
import java.util.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.*;
/** Failed removals rotate to the next round instead of monopolizing the budget. */
public final class CleanseRotation {
    private static final Map<ServerPlayer, ArrayDeque<MobEffect>> QUEUES = new WeakHashMap<>();
    public static void forget(ServerPlayer player) { QUEUES.remove(player); }
    public static int remove(ServerPlayer player, int actions) {
        return remove(player, actions, () -> true);
    }
    public static int remove(ServerPlayer player, int actions, java.util.function.BooleanSupplier acquire) {
        if (actions <= 0) return 0;
        Set<MobEffect> active = new HashSet<>();
        for (var instance : player.getActiveEffects()) if (instance.getEffect().getCategory() == MobEffectCategory.HARMFUL) active.add(instance.getEffect());
        var queue = QUEUES.computeIfAbsent(player, p -> new ArrayDeque<>());
        queue.removeIf(e -> !active.contains(e));
        active.removeAll(new HashSet<>(queue));
        var fresh = new ArrayList<>(active);
        while (!fresh.isEmpty()) {
            int index = player.getRandom().nextInt(fresh.size());
            queue.addLast(fresh.get(index)); fresh.set(index, fresh.get(fresh.size() - 1)); fresh.remove(fresh.size() - 1);
        }
        int budget = Math.min(queue.size(), Math.min(64, Math.max(32, actions))), removed = 0;
        for (int i = 0; i < budget && removed < actions; i++) {
            if (!acquire.getAsBoolean()) break;
            var effect = queue.removeFirst();
            if (EffectChanges.remove(player, effect, false)) removed++;
            else if (player.hasEffect(effect)) queue.addLast(effect);
        }
        if (queue.isEmpty()) QUEUES.remove(player);
        return removed;
    }
    private CleanseRotation() {}
}
