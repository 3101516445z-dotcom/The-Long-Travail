package com.thelongtravail.compat.maid;

import net.minecraft.nbt.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import java.util.*;

public final class MaidAccessoryLedger extends SavedData {
    private final Map<UUID, CompoundTag> entries = new HashMap<>();

    public static MaidAccessoryLedger get(MinecraftServer server) {
        return server.overworld()
                .getDataStorage()
                .computeIfAbsent(
                        MaidAccessoryLedger::load,
                        MaidAccessoryLedger::new,
                        "travail_maid_accessories");
    }

    public CompoundTag find(UUID id) {
        return entries.get(id);
    }

    public CompoundTag entry(UUID id) {
        return entries.computeIfAbsent(
                id,
                k -> {
                    setDirty();
                    return new CompoundTag();
                });
    }

    public static boolean complete(MinecraftServer server, UUID id, int ticks) {
        var ledger = get(server);
        if (!ledger.entries.containsKey(id)) return false;
        var tag = ledger.entries.get(id);
        tag.putInt("StarCooldown", Math.max(ticks, tag.getInt("StarCooldown")));
        ledger.setDirty();
        return true;
    }

    public static MaidAccessoryLedger load(CompoundTag root) {
        var ledger = new MaidAccessoryLedger();
        for (Tag value : root.getList("Entries", Tag.TAG_COMPOUND)) {
            var tag = (CompoundTag) value;
            if (tag.hasUUID("Id")) ledger.entries.put(tag.getUUID("Id"), tag.getCompound("State"));
        }
        return ledger;
    }

    @Override
    public CompoundTag save(CompoundTag root) {
        ListTag list = new ListTag();
        entries.forEach(
                (id, state) -> {
                    var tag = new CompoundTag();
                    tag.putUUID("Id", id);
                    tag.put("State", state.copy());
                    list.add(tag);
                });
        root.put("Entries", list);
        return root;
    }
}
