package com.thelongtravail.network;

import com.thelongtravail.TheLongTravail;
import com.thelongtravail.data.HotPathMetrics.Counter;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.EntityLeaveLevelEvent;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import java.util.*;

// 主线程维护；归属不缓存，仍在每个同步批次重新解析。
@Mod.EventBusSubscriber(modid = TheLongTravail.MODID)
public final class ProjectileIndex {
    private static final long AUDIT_INTERVAL = 200;
    private static final Map<ServerLevel, State> WORLDS = new IdentityHashMap<>();
    private static final class State {
        final Set<Projectile> entities = Collections.newSetFromMap(new IdentityHashMap<>());
        long audited = Long.MIN_VALUE;
    }
    public static List<Projectile> snapshot(ServerLevel level) {
        State state = WORLDS.computeIfAbsent(level, ignored -> new State());
        long now = level.getGameTime();
        if (state.audited == Long.MIN_VALUE || now < state.audited || now - state.audited >= AUDIT_INTERVAL) {
            state.entities.clear();
            for (var entity : level.getAllEntities()) {
                Counter.SYNC_ENTITY_VISITS.add(1);
                if (entity instanceof Projectile projectile) state.entities.add(projectile);
            }
            state.audited = now;
        }
        state.entities.removeIf(entity -> {
            Counter.PROJECTILE_INDEX_CHECKS.add(1);
            // JoinLevelEvent 发生在真正加入实体管理器之前；排除后续监听器取消的生成。
            return entity.isRemoved() || entity.level() != level || level.getEntity(entity.getUUID()) != entity;
        });
        return List.copyOf(state.entities);
    }
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void joined(EntityJoinLevelEvent event) {
        if (event.getLevel() instanceof ServerLevel level && event.getEntity() instanceof Projectile projectile) {
            State state = WORLDS.get(level);
            if (state != null) state.entities.add(projectile);
        }
    }
    @SubscribeEvent
    public static void left(EntityLeaveLevelEvent event) {
        if (event.getLevel() instanceof ServerLevel level) {
            State state = WORLDS.get(level);
            if (state != null) state.entities.remove(event.getEntity());
        }
    }
    @SubscribeEvent public static void unloaded(LevelEvent.Unload event) { WORLDS.remove(event.getLevel()); }
    @SubscribeEvent public static void stopped(ServerStoppedEvent event) { clear(); }
    public static void clear() { WORLDS.clear(); }
    private ProjectileIndex() {}
}
