package com.thelongtravail.valley;

import com.thelongtravail.network.ExecutionTerminationPacket;
import java.util.*;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;

public final class ExecutionReceipts {
    private static final class Receipts {
        final Map<Integer, Receipt> identities = new HashMap<>();
        Iterator<Map.Entry<Integer, Receipt>> cursor;
    }

    private record Receipt(UUID uuid, ExecutionLease lease) {}

    private final Map<Level, Receipts> worlds = new WeakHashMap<>();

    public void remember(Level world, ExecutionTerminationPacket packet) {
        if (!world.dimension().location().equals(packet.dimension())
                || world.getGameTime() >= packet.expires()
                || packet.expires() > packet.limit()) return;
        var receipts = worlds.computeIfAbsent(world, ignored -> new Receipts());
        var previous = receipts.identities.get(packet.id());
        if (previous != null && packet.uuid().equals(previous.uuid())) {
            previous.lease().update(packet.expires());
            return;
        }
        receipts.identities.put(
                packet.id(),
                new Receipt(packet.uuid(), new ExecutionLease(packet.expires(), packet.limit())));
        receipts.cursor = null;
    }

    public boolean matches(Entity entity) {
        return matches(entity.level(), entity.getId(), entity.getUUID());
    }

    public boolean matches(Level world, int id, UUID uuid) {
        return lease(world, id, uuid) != null;
    }

    public ExecutionLease lease(Level world, int id, UUID uuid) {
        var receipts = worlds.get(world);
        var receipt = receipts == null ? null : receipts.identities.get(id);
        return receipt != null
                        && uuid.equals(receipt.uuid())
                        && !receipt.lease().expired(world.getGameTime())
                ? receipt.lease()
                : null;
    }

    public int probe(Level world, java.util.function.Consumer<Entity> found) {
        var receipts = worlds.get(world);
        if (receipts == null || receipts.identities.isEmpty()) return 0;
        if (receipts.cursor == null) receipts.cursor = receipts.identities.entrySet().iterator();
        int checked = 0;
        while (checked < 64 && receipts.cursor.hasNext()) {
            var identity = receipts.cursor.next();
            checked++;
            if (identity.getValue().lease().expired(world.getGameTime())) {
                receipts.cursor.remove();
                continue;
            }
            var entity = world.getEntity(identity.getKey());
            if (entity != null && entity.getUUID().equals(identity.getValue().uuid()))
                found.accept(entity);
        }
        if (!receipts.cursor.hasNext()) receipts.cursor = null;
        if (receipts.identities.isEmpty()) worlds.remove(world);
        return checked;
    }

    public void forget(Object world) {
        worlds.remove(world);
    }
}
