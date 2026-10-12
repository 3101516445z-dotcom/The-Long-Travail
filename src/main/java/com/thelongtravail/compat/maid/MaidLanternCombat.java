package com.thelongtravail.compat.maid;

import com.thelongtravail.valley.*;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;

import com.thelongtravail.registry.ModRegistry;
import com.thelongtravail.boundless.TimeStopExemptions;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.damagesource.DamageSource;
import java.util.*;

// 仅服务端运行；关系和状态不写入物品或玩家存档。
public final class MaidLanternCombat {
    public static final double RANGE_SQR = 64 * 64;
    private static final Map<UUID, State> STATES = new HashMap<>();

    private static final class State {
        final Map<UUID, Long> enemies = new HashMap<>();
        List<LivingEntity> nearby = List.of();
        long scan = Long.MIN_VALUE, start = -1, last = -1;
    }

    public static boolean equipped(EntityMaid p) {
        return !MaidAccessoryRuntime.equipped(p, ModRegistry.SWORD_AND_LANTERN.get()).isEmpty();
    }

    private static boolean friendly(EntityMaid p, LivingEntity e) {
        if (e == p || p.isAlliedTo(e) || e.isAlliedTo(p)) return true;
        var master = MaidAccessoryRuntime.owner(p);
        if (master == null) return false;
        if (e instanceof Player other && SwordLanternState.teammates(master, other)) return true;
        var id = TimeStopExemptions.owner(e);
        if (master.getUUID().equals(id)) return true;
        var other = id == null ? null : p.getServer().getPlayerList().getPlayer(id);
        return other != null && SwordLanternState.teammates(master, other);
    }

    private static boolean nearby(EntityMaid p, LivingEntity e) {
        return e != p
                && e.isAlive()
                && !e.isRemoved()
                && !e.isSpectator()
                && e.level() == p.level()
                && p.distanceToSqr(e) <= RANGE_SQR;
    }

    private static boolean eligible(EntityMaid p, LivingEntity e) {
        return nearby(p, e)
                && e.isAttackable()
                && !friendly(p, e)
                && (!(e instanceof Player other)
                        || !other.isCreative() && MaidAccessoryRuntime.canHarm(p, other));
    }

    private static State update(EntityMaid p) {
        State s = STATES.computeIfAbsent(p.getUUID(), id -> new State());
        long now = p.level().getGameTime();
        if (s.last >= 0 && now - s.last >= SwordLanternConfig.ticks(SwordLanternConfig.TIMEOUT)) {
            s.start = -1;
            s.last = -1;
        }
        s.enemies
                .entrySet()
                .removeIf(
                        entry -> {
                            Entity e =
                                    ((net.minecraft.server.level.ServerLevel) p.level())
                                            .getEntity(entry.getKey());
                            return now - entry.getValue()
                                            >= SwordLanternConfig.ticks(SwordLanternConfig.MEMORY)
                                    || !(e instanceof LivingEntity living)
                                    || !eligible(p, living);
                        });
        if (s.scan == Long.MIN_VALUE || now - s.scan >= 10) {
            s.nearby =
                    p.level()
                            .getEntitiesOfClass(
                                    LivingEntity.class,
                                    p.getBoundingBox().inflate(64),
                                    e -> nearby(p, e));
            s.scan = now;
        }
        return s;
    }

    private static boolean enemy(EntityMaid p, State s, LivingEntity e, LivingEntity pending) {
        return eligible(p, e)
                && (e == pending
                        || e instanceof Mob mob && mob.getTarget() == p
                        || s.enemies.containsKey(e.getUUID()));
    }

    public static double bonus(EntityMaid p, LivingEntity pending) {
        if (!equipped(p)) {
            forget(p);
            return 0;
        }
        State s = update(p);
        Set<UUID> seen = new HashSet<>();
        int enemies = 0;
        boolean undead = false, alone = true;
        List<LivingEntity> candidates = new ArrayList<>(s.nearby);
        // 玩家关系及到达范围即时检查，不等待下一次环境扫描。
        candidates.addAll(((net.minecraft.server.level.ServerLevel) p.level()).players());
        for (UUID id : s.enemies.keySet()) {
            Entity e = ((net.minecraft.server.level.ServerLevel) p.level()).getEntity(id);
            if (e instanceof LivingEntity living) candidates.add(living);
        }
        if (pending != null) candidates.add(pending);
        for (LivingEntity e : candidates) {
            if (!seen.add(e.getUUID()) || !nearby(p, e)) continue;
            boolean hostile = enemy(p, s, e, pending);
            if (hostile) {
                enemies++;
                undead |=
                        (e == pending
                                        ? com.thelongtravail.underworld.UnderworldItems.attackType(
                                                p, e)
                                        : e.getMobType())
                                == MobType.UNDEAD;
            }
            if (e instanceof Player && !hostile
                    || !(e instanceof Player)
                            && Objects.equals(p.getOwnerUUID(), TimeStopExemptions.owner(e))
                            && p.getOwnerUUID() != null) alone = false;
        }
        int conditions =
                (enemies > 10 ? 1 : 0)
                        + (undead ? 1 : 0)
                        + (alone ? 1 : 0)
                        + (s.start >= 0
                                        && p.level().getGameTime() - s.start
                                                > SwordLanternConfig.ticks(
                                                        SwordLanternConfig.DURATION)
                                ? 1
                                : 0)
                        + (p.getHealth() < p.getMaxHealth() * SwordLanternConfig.HEALTH.get()
                                ? 1
                                : 0);
        return conditions * SwordLanternConfig.BONUS.get();
    }

    public static void confirmed(LivingEntity victim, EntityMaid attacker) {
        if (attacker != null && attacker != victim) record(attacker, victim);
    }

    public static void received(LivingEntity victim, DamageSource source) {
        if (victim instanceof EntityMaid maid
                && source.getEntity() instanceof LivingEntity cause
                && cause != maid) record(maid, cause);
    }

    private static void record(EntityMaid p, LivingEntity other) {
        // 致死一击也刷新久战，但死亡目标不会继续留在有效敌人集合中。
        if (!equipped(p)
                || other == p
                || other.level() != p.level()
                || p.distanceToSqr(other) > RANGE_SQR
                || other.isSpectator()
                || friendly(p, other)
                || other instanceof Player target
                        && (target.isCreative() || !MaidAccessoryRuntime.canHarm(p, target)))
            return;
        State s = update(p);
        long now = p.level().getGameTime();
        if (s.start < 0) s.start = now;
        s.last = now;
        s.enemies.put(other.getUUID(), now);
    }

    public static void tick(EntityMaid p) {
        if (equipped(p)) update(p);
        else forget(p);
    }

    public static void forget(EntityMaid p) {
        STATES.remove(p.getUUID());
    }

    public static void clear() {
        STATES.clear();
    }

    private MaidLanternCombat() {}
}
