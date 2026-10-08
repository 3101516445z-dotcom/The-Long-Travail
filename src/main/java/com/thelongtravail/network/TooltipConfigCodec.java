package com.thelongtravail.network;

import net.minecraft.network.FriendlyByteBuf;
import java.nio.charset.StandardCharsets;
import java.util.*;

// 分配内存和发送前均检查固定的协议限制。
public final class TooltipConfigCodec {
    public static final int MAX_VALUES = 512, MAX_POOLS = 32, MAX_ENTRIES = 2048,
            MAX_TOTAL_ENTRIES = 8192, MAX_KEY = 128, MAX_ENTRY = 1024, MAX_BYTES = 1_048_576;
    public record Snapshot(Map<String, Double> values, Map<String, List<String>> pools) {
        public Snapshot {
            values = Map.copyOf(values);
            Map<String, List<String>> copy = new LinkedHashMap<>();
            pools.forEach((key, entries) -> copy.put(key, List.copyOf(entries)));
            pools = Map.copyOf(copy);
            validate(values, pools);
        }
    }
    private static int size(int value, int maximum, String label) {
        if (value < 0 || value > maximum) throw new IllegalArgumentException("Tooltip config " + label + " outside 0.." + maximum + ": " + value);
        return value;
    }
    private static int stringBytes(String text, int maximum, String label) {
        size(text.length(), maximum, label);
        return 5 + text.getBytes(StandardCharsets.UTF_8).length;
    }
    public static void validate(Map<String, Double> values, Map<String, List<String>> pools) {
        size(values.size(), MAX_VALUES, "value count"); size(pools.size(), MAX_POOLS, "pool count");
        int total = 0, bytes = 10;
        for (var entry : values.entrySet()) {
            bytes += stringBytes(entry.getKey(), MAX_KEY, "key") + 8;
            if (!Double.isFinite(entry.getValue())) throw new IllegalArgumentException("Non-finite tooltip config: " + entry.getKey());
        }
        for (var pool : pools.entrySet()) {
            bytes += stringBytes(pool.getKey(), MAX_KEY, "pool key") + 5;
            size(pool.getValue().size(), MAX_ENTRIES, pool.getKey() + " entries");
            total = size(total + pool.getValue().size(), MAX_TOTAL_ENTRIES, "total entries");
            for (String entry : pool.getValue()) {
                bytes += stringBytes(entry, MAX_ENTRY, pool.getKey() + " entry length");
                size(bytes, MAX_BYTES, "encoded byte budget at " + pool.getKey());
            }
        }
        size(bytes, MAX_BYTES, "encoded byte budget");
    }
    public static void encode(Snapshot snapshot, FriendlyByteBuf buffer) {
        buffer.writeVarInt(snapshot.values().size());
        snapshot.values().forEach((key, value) -> { buffer.writeUtf(key, MAX_KEY); buffer.writeDouble(value); });
        buffer.writeVarInt(snapshot.pools().size());
        snapshot.pools().forEach((key, entries) -> {
            buffer.writeUtf(key, MAX_KEY); buffer.writeVarInt(entries.size());
            entries.forEach(entry -> buffer.writeUtf(entry, MAX_ENTRY));
        });
    }
    public static Snapshot decode(FriendlyByteBuf buffer) {
        size(buffer.readableBytes(), MAX_BYTES, "payload bytes");
        int count = size(buffer.readVarInt(), MAX_VALUES, "value count");
        Map<String, Double> values = new LinkedHashMap<>();
        for (int i = 0; i < count; i++) {
            String key = buffer.readUtf(MAX_KEY); double value = buffer.readDouble();
            if (!Double.isFinite(value) || values.putIfAbsent(key, value) != null)
                throw new IllegalArgumentException("Invalid/duplicate tooltip config: " + key);
        }
        int poolCount = size(buffer.readVarInt(), MAX_POOLS, "pool count"), total = 0;
        Map<String, List<String>> pools = new LinkedHashMap<>();
        for (int i = 0; i < poolCount; i++) {
            String key = buffer.readUtf(MAX_KEY);
            if (pools.containsKey(key)) throw new IllegalArgumentException("Duplicate tooltip pool: " + key);
            int entries = size(buffer.readVarInt(), MAX_ENTRIES, key + " entries");
            total = size(total + entries, MAX_TOTAL_ENTRIES, "total entries");
            List<String> list = new ArrayList<>(entries);
            for (int j = 0; j < entries; j++) list.add(buffer.readUtf(MAX_ENTRY));
            pools.put(key, list);
        }
        if (buffer.isReadable()) throw new IllegalArgumentException("Trailing tooltip config bytes");
        return new Snapshot(values, pools);
    }
    private TooltipConfigCodec() {}
}
