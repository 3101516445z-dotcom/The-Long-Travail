package com.thelongtravail.network;

import com.thelongtravail.boundless.TimeStopExemptions;
import com.thelongtravail.boundless.TimeStopManager;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.AABB;
import java.util.*;

// 每批共享实体归属计算；协议仍使用完整快照，客户端无需合并增量。
public final class TimeStopSync {
    private static final Map<ServerPlayer, TimeStopPacket> LAST = new WeakHashMap<>();
    public static void forget(ServerPlayer player) { LAST.remove(player); }
    public static void clear() { LAST.clear(); }

    public static void send(ServerLevel level) {
        if (level.players().isEmpty()) return;
        int distance = level.getServer().getPlayerList().getViewDistance() * 16 + 256;
        var needed = new LinkedHashSet<TimeStopManager.Field>();
        var visible = new LinkedHashMap<ServerPlayer, List<TimeStopManager.Field>>();
        var fields = TimeStopManager.fields(level);
        for (var player : level.players()) {
            var list = new ArrayList<TimeStopManager.Field>();
            for (var field : fields) if (field.center.distanceToSqr(player.position()) <
                    (distance + field.radius) * (distance + field.radius)) list.add(field);
            visible.put(player, list);
            needed.addAll(list);
        }
        // 即使射手不在客户端视野内，快速投射物仍需得到服务端豁免确认。
        List<Projectile> projectiles = needed.isEmpty() ? List.of() : ProjectileIndex.snapshot(level);
        var owners = new IdentityHashMap<Entity, UUID>();
        var views = new IdentityHashMap<TimeStopManager.Field, TimeStopPacket.View>();
        for (var field : needed) {
            var allies = new HashSet<>(field.allies);
            for (Entity entity : level.getEntities((Entity)null, new AABB(field.center, field.center).inflate(field.radius + 16)))
                include(entity, field, allies, owners);
            for (Entity entity : projectiles) include(entity, field, allies, owners);
            views.put(field, new TimeStopPacket.View(field.id, field.center, field.radius, Set.copyOf(allies)));
        }
        visible.forEach((player, list) -> {
            var packet = new TimeStopPacket(level.dimension().location(), list.stream().map(views::get).toList());
            if (player.connection != null && !packet.equals(LAST.get(player))) {
                com.thelongtravail.data.HotPathMetrics.Counter.SYNC_PACKETS.add(1);
                TravailNetwork.sendTimeStop(player, packet);
                LAST.put(player, packet);
            }
        });
    }

    private static void include(Entity entity, TimeStopManager.Field field, Set<UUID> allies, Map<Entity, UUID> owners) {
        if (allies.size() >= TimeStopPacket.MAX_ALLIES || allies.contains(entity.getUUID())) return;
        if (!owners.containsKey(entity)) {
            com.thelongtravail.data.HotPathMetrics.Counter.OWNER_LOOKUPS.add(1);
            owners.put(entity, TimeStopExemptions.owner(entity));
        }
        UUID owner = owners.get(entity);
        if (owner != null && field.allies.contains(owner)) allies.add(entity.getUUID());
    }
    private TimeStopSync() {}
}
