package com.thelongtravail.combat;

import com.thelongtravail.TheLongTravail;
import net.minecraft.core.Holder;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import java.lang.ref.WeakReference;
import java.util.*;
import com.thelongtravail.diagnostics.RuntimeDiagnostics;
import static com.thelongtravail.diagnostics.RuntimeDiagnostics.Metric.*;

@Mod.EventBusSubscriber(modid = TheLongTravail.MODID)
public final class DamageReplay {
    private static final int MAX_PENDING = 4096;

    public interface Policy {
        boolean valid(LivingEntity attacker, LivingEntity target);

        boolean continueChain(LivingEntity attacker, LivingEntity target);
    }

    private record Task(
            WeakReference<LivingEntity> attacker,
            WeakReference<LivingEntity> target,
            WeakReference<Entity> direct,
            Holder<DamageType> type,
            Vec3 position,
            float amount,
            ServerLevel level,
            int readyTick,
            long queuedNanos,
            Policy policy) {}

    private static final Map<MinecraftServer, ArrayDeque<Task>> QUEUES = new WeakHashMap<>();
    private static final ThreadLocal<Frame> CURRENT = new ThreadLocal<>();

    public static final class Frame {
        public final LivingEntity target;
        public final DamageSource source;
        public final float amount;
        private final Policy policy;
        private final Frame previous;
        private boolean cancelled;
        private boolean claimed;
        public boolean confirmed;

        Frame(LivingEntity target, DamageSource source, float amount, Policy policy) {
            this.target = target;
            this.source = source;
            this.amount = amount;
            this.policy = policy;
            this.previous = CURRENT.get();
        }
    }

    public static Frame claim(LivingEntity target, DamageSource source) {
        Frame f = CURRENT.get();
        if (f == null || f.claimed || f.target != target || f.source != source) return null;
        f.claimed = true;
        return f;
    }

    public static boolean active(LivingEntity target, DamageSource source) {
        var context = com.thelongtravail.helper.CombatContext.current(target, source);
        return context != null && context.isReplay();
    }

    // 取消指定来源的旧追加链；正在结算的伤害不回滚，但不再延续该链。
    public static void cancel(LivingEntity attacker, Policy policy) {
        if (!(attacker.level() instanceof ServerLevel level)) return;
        var queue = QUEUES.get(level.getServer());
        if (queue != null) queue.removeIf(t -> t.attacker.get() == attacker && t.policy == policy);
        for (Frame frame = CURRENT.get(); frame != null; frame = frame.previous)
            if (frame.source.getEntity() == attacker && frame.policy == policy)
                frame.cancelled = true;
    }

    public static void enqueue(
            LivingEntity attacker,
            LivingEntity target,
            DamageSource source,
            float amount,
            Policy policy) {
        if (!(amount > 0) || !Float.isFinite(amount)) return;
        if (!(target.level() instanceof ServerLevel level)
                || attacker.level() != level
                || !target.isAlive()) return;
        var queue = QUEUES.computeIfAbsent(level.getServer(), s -> new ArrayDeque<>());
        if (queue.size() >= MAX_PENDING) {
            RuntimeDiagnostics.observe(REPLAY_CAPACITY_DROP, 1);
            return;
        }
        queue.addLast(
                new Task(
                        new WeakReference<>(attacker),
                        new WeakReference<>(target),
                        new WeakReference<>(source.getDirectEntity()),
                        source.typeHolder(),
                        source.sourcePositionRaw(),
                        amount,
                        level,
                        level.getServer().getTickCount() + 1,
                        RuntimeDiagnostics.start(),
                        policy));
        RuntimeDiagnostics.observe(REPLAY_DEPTH, queue.size());
    }

    @SubscribeEvent
    public static void stopped(ServerStoppedEvent event) {
        QUEUES.remove(event.getServer());
    }

    @SubscribeEvent
    public static void tick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        var queue = QUEUES.get(event.getServer());
        if (queue == null || queue.isEmpty()) return;
        Task t = queue.peekFirst();
        if (t.readyTick > event.getServer().getTickCount()) return;
        queue.removeFirst();
        RuntimeDiagnostics.observe(REPLAY_DEPTH, queue.size());
        RuntimeDiagnostics.observe(
                REPLAY_WAIT_TICKS, (long) event.getServer().getTickCount() - t.readyTick + 1);
        RuntimeDiagnostics.end(REPLAY_WAIT_NS, t.queuedNanos);
        LivingEntity attacker = t.attacker.get(), target = t.target.get();
        if (attacker == null
                || target == null
                || attacker.isRemoved()
                || target.isRemoved()
                || !attacker.isAlive()
                || !target.isAlive()
                || attacker.level() != t.level
                || target.level() != t.level
                || t.level.getEntity(attacker.getUUID()) != attacker
                || t.level.getEntity(target.getUUID()) != target
                || !t.policy.valid(attacker, target)) return;
        if (target instanceof net.minecraft.world.entity.player.Player player
                && !com.thelongtravail.compat.maid.MaidAccessories.canHarm(attacker, player))
            return;
        DamageSource source = new DamageSource(t.type, t.direct.get(), attacker, t.position);
        Frame previous = CURRENT.get(), frame = new Frame(target, source, t.amount, t.policy);
        int invulnerable = target.invulnerableTime;
        var state = (com.thelongtravail.mixin.DamageReplayStateAccessor) target;
        float lastHurt = state.travail$lastHurt();
        CURRENT.set(frame);
        try {
            target.invulnerableTime = 0;
            target.hurt(source, t.amount);
        } finally {
            target.invulnerableTime = invulnerable;
            state.travail$lastHurt(lastHurt);
            if (previous == null) CURRENT.remove();
            else CURRENT.set(previous);
        }
        if (!frame.cancelled
                && frame.confirmed
                && target.isAlive()
                && t.policy.valid(attacker, target)
                && t.policy.continueChain(attacker, target))
            enqueue(attacker, target, source, t.amount, t.policy);
    }

    private DamageReplay() {}
}
