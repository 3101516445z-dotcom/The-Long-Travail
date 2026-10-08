package com.thelongtravail.boundless;
import net.minecraft.nbt.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.BlockEventData;
import net.minecraft.world.level.saveddata.SavedData;
import java.util.*;
public final class FrozenBlockEvents extends SavedData {
    private final FrozenCoverage coverage = new FrozenCoverage();
    private final Set<Long> waitingChunks = new HashSet<>();
    private record Held(BlockEventData event, Set<BlockPos> gates) {}
    private final Map<BlockEventData,Set<BlockPos>> gates = new HashMap<>();
    private final IndexedFrozenQueue<BlockEventData,Held> events = new IndexedFrozenQueue<>(held -> {
        Set<Long> chunks = new HashSet<>();
        chunks.add(net.minecraft.world.level.ChunkPos.asLong(held.event.pos()));
        for (BlockPos pos : held.gates) chunks.add(net.minecraft.world.level.ChunkPos.asLong(pos));
        return chunks;
    });
    public static FrozenBlockEvents get(ServerLevel l){return l.getDataStorage().computeIfAbsent(FrozenBlockEvents::load,FrozenBlockEvents::new,"travail_frozen_events");}
    public void hold(BlockEventData event) {
        if (!events.containsKey(event)) {
            events.put(event, new Held(event, Set.of()));
            setDirty();
        }
    }
    public void hold(BlockEventData event, Collection<BlockPos> positions) {
        hold(event);
        Set<BlockPos> combined = gates.computeIfAbsent(event, ignored -> new HashSet<>());
        combined.addAll(positions);
        events.put(event, new Held(event, Set.copyOf(combined)));
        setDirty();
    }
    public void tick(ServerLevel level) {
        if (events.isEmpty()) return;
        events.changed(coverage.changed(TimeStopManager.get(level)));
        Set<Long> loaded = new HashSet<>();
        waitingChunks.removeIf(chunk -> {
            if (!level.hasChunkAt(new BlockPos(net.minecraft.world.level.ChunkPos.getX(chunk)<<4, 0,
                    net.minecraft.world.level.ChunkPos.getZ(chunk)<<4))) return false;
            loaded.add(chunk);
            return true;
        });
        events.changed(loaded);
        var ready = events.release(held -> {
            var event = held.event;
            if (TimeStopManager.frozen(level, event.pos())) return false;
            for (BlockPos gate : held.gates) if (TimeStopManager.frozen(level, gate)) return false;
            if (level.hasChunkAt(event.pos())) return true;
            waitingChunks.add(net.minecraft.world.level.ChunkPos.asLong(event.pos()));
            return false;
        });
        if (!ready.isEmpty()) setDirty();
        for (Held held : ready) {
            var event = held.event;
            gates.remove(event);
            level.blockEvent(event.pos(), event.block(), event.paramA(), event.paramB());
        }
    }
    @Override public CompoundTag save(CompoundTag root) {
        ListTag list = new ListTag();
        for (Held held : events.values()) {
            var event = held.event;
            CompoundTag tag = new CompoundTag();
            tag.putLong("pos", event.pos().asLong());
            tag.putString("block", BuiltInRegistries.BLOCK.getKey(event.block()).toString());
            tag.putInt("a", event.paramA());
            tag.putInt("b", event.paramB());
            ListTag positions = new ListTag();
            for (BlockPos position : held.gates) positions.add(LongTag.valueOf(position.asLong()));
            tag.put("gates", positions);
            list.add(tag);
        }
        root.put("events", list);
        return root;
    }
    private static FrozenBlockEvents load(CompoundTag root) {
        var out = new FrozenBlockEvents();
        for (Tag raw : root.getList("events", Tag.TAG_COMPOUND)) {
            CompoundTag tag = (CompoundTag)raw;
            BlockEventData event = new BlockEventData(BlockPos.of(tag.getLong("pos")),
                    BuiltInRegistries.BLOCK.get(new ResourceLocation(tag.getString("block"))), tag.getInt("a"), tag.getInt("b"));
            out.hold(event);
            Set<BlockPos> positions = new HashSet<>();
            for (Tag position : tag.getList("gates", Tag.TAG_LONG)) positions.add(BlockPos.of(((LongTag)position).getAsLong()));
            if (!positions.isEmpty()) out.hold(event, positions);
        }
        return out;
    }
}
