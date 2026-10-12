package com.thelongtravail.boundless;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.projectile.Projectile;
import java.util.*;

public final class TimeStopExemptions {
    public static Set<UUID> snapshot(ServerPlayer p) {
        Set<UUID> out = new HashSet<>();
        out.add(p.getUUID());
        for (ServerPlayer other : p.server.getPlayerList().getPlayers()) {
            boolean same = p.getTeam() != null && p.getTeam().isAlliedTo(other.getTeam());
            if (!same)
                same =
                        com.thelongtravail.compat.ftbteams.FtbTeamsCompat.timeStopTeammates(
                                p, other);
            if (same) out.add(other.getUUID());
        }
        com.thelongtravail.compat.ftbteams.FtbTeamsCompat.addTimeStopMembers(p, out);
        return Set.copyOf(out);
    }

    public static UUID owner(Entity e) {
        // 高频普通实体和链首终点不需要循环检测集合；真实归属链仍保留 UUID 去重和 16 层上限。
        if (e == null) return null;
        if (e instanceof net.minecraft.world.entity.player.Player) return e.getUUID();
        if (e instanceof net.minecraft.world.entity.animal.allay.Allay allay)
            return allay.getBrain()
                    .getMemory(net.minecraft.world.entity.ai.memory.MemoryModuleType.LIKED_PLAYER)
                    .orElse(null);
        if (!(e instanceof OwnableEntity
                || e instanceof Projectile
                || e instanceof net.minecraft.world.entity.projectile.EvokerFangs
                || e instanceof net.minecraft.world.entity.LightningBolt)) return null;
        Set<UUID> seen = new HashSet<>();
        for (int depth = 0; e != null && depth < 16 && seen.add(e.getUUID()); depth++) {
            if (e instanceof net.minecraft.world.entity.player.Player) return e.getUUID();
            if (e instanceof net.minecraft.world.entity.animal.allay.Allay allay)
                return allay.getBrain()
                        .getMemory(
                                net.minecraft.world.entity.ai.memory.MemoryModuleType.LIKED_PLAYER)
                        .orElse(null);
            if (e instanceof net.minecraft.world.entity.projectile.EvokerFangs fangs) {
                e = fangs.getOwner();
                continue;
            }
            if (e instanceof net.minecraft.world.entity.LightningBolt bolt) {
                e = bolt.getCause();
                continue;
            }
            if (e instanceof Projectile shot) {
                Entity next = shot.getOwner();
                if (next == null)
                    return ((com.thelongtravail.mixin.TimeStopProjectileAccessor) (Object) shot)
                            .travail$ownerUUID();
                e = next;
                continue;
            }
            if (!(e instanceof OwnableEntity owned)) return null;
            Entity next = owned.getOwner();
            if (next == null) return owned.getOwnerUUID();
            e = next;
        }
        return null;
    }

    public static boolean exempt(Entity e, Set<UUID> allies) {
        if (!com.thelongtravail.config.ModeEffectRules.STAR_FREEZE.applies(e)) return true;
        if (allies.contains(e.getUUID())) return true;
        UUID owner = owner(e);
        return owner != null && allies.contains(owner);
    }

    private TimeStopExemptions() {}
}
