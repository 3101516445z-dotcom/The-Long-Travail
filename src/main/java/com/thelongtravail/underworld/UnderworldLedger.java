package com.thelongtravail.underworld;

import com.thelongtravail.TravailAspect;
import com.thelongtravail.data.JourneyState;
import com.thelongtravail.data.JourneyOperations;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.nbt.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.saveddata.SavedData;
import java.util.*;

public final class UnderworldLedger extends SavedData {
    public static final int MAX_NBT_DEPTH = 64;
    public static final String BOOK_ID = "TravailBookId";
    private final Set<UUID> spent = new HashSet<>(), witnesses = new HashSet<>();
    private final Map<JourneyState.AspectJourneyKey, Set<ResourceLocation>> themeWitnesses = new HashMap<>();
    private final ListTag retainedThemeRecords = new ListTag(), retainedLegacyRecords = new ListTag();
    private final CompoundTag quarantined = new CompoundTag();
    private boolean consumptionReliable = true;
    public final Map<UUID, CompoundTag> deaths = new HashMap<>();
    public final Set<String> graves = new HashSet<>();

    public static UnderworldLedger get(MinecraftServer server) {
        return server.overworld()
                .getDataStorage()
                .computeIfAbsent(UnderworldLedger::load, UnderworldLedger::new, "the_long_travail_underworld");
    }

    public static UUID identity(ItemStack s) {
        CompoundTag n = s.getOrCreateTag();
        if (!n.hasUUID(BOOK_ID)) n.putUUID(BOOK_ID, UUID.randomUUID());
        return n.getUUID(BOOK_ID);
    }

    public boolean consumed(ItemStack s) {
        return s.hasTag() && s.getTag().hasUUID(BOOK_ID) && spent.contains(s.getTag().getUUID(BOOK_ID));
    }

    public boolean spent(UUID id) {
        return spent.contains(id);
    }

    // 无法从损坏的消耗记录还原物品身份；保留有效记录，并暂停需要核对消耗资格的极限模式苏生。
    public boolean canRevive(boolean hardcore, UUID book) {
        return !hardcore || consumptionReliable && !spent(book);
    }

    public boolean hasChanges() {
        return !spent.isEmpty() || !witnesses.isEmpty() || !themeWitnesses.isEmpty();
    }

    public void consume(UUID id) {
        spent.add(id);
        setDirty();
    }

    public void witness(UUID id) {
        if (witnesses.add(id)) setDirty();
    }

    public void witness(JourneyState.AspectJourneyKey key, ResourceLocation source) {
        Objects.requireNonNull(key);
        Objects.requireNonNull(source);
        if (themeWitnesses.computeIfAbsent(key, ignored -> new HashSet<>()).add(source)) setDirty();
    }

    public void reconcile(ItemStack s) {
        if (s.isEmpty()) return;
        if (consumed(s)) {
            s.setCount(0);
            return;
        }
        if (s.hasTag()) reconcileTag(s.getTag(), 0);
    }

    public void reconcileTag(Tag tag, int depth) {
        if (depth > MAX_NBT_DEPTH || tag == null || !hasChanges()) return;
        try {
            reconcileSynchronous(tag, depth);
        } catch (ConcurrentModificationException changed) {
            // 共享标签导致键集变化时，从当前根重新校正；已完成的校正是幂等的。
            if (depth != 0) throw changed;
            var cursor = new NbtReconcileCursor(() -> tag);
            while (cursor.step(this)) {}
        }
    }

    private void reconcileSynchronous(Tag tag, int depth) {
        if (depth > MAX_NBT_DEPTH || tag == null) return;
        if (!(tag instanceof CompoundTag)
                && (!(tag instanceof ListTag list)
                        || list.getElementType() != Tag.TAG_COMPOUND && list.getElementType() != Tag.TAG_LIST)) return;
        com.thelongtravail.data.HotPathMetrics.Counter.NBT_SYNC_NODES.add(1);
        reconcileNode(tag);
        if (depth == MAX_NBT_DEPTH) return;
        if (tag instanceof CompoundTag c) for (String key : c.getAllKeys()) reconcileSynchronous(c.get(key), depth + 1);
        else if (tag instanceof ListTag list) for (Tag child : list) reconcileSynchronous(child, depth + 1);
    }

    public void reconcileNode(Tag tag) {
        if (tag instanceof CompoundTag c) {
            if (c.getString("id").equals("the_long_travail:book_of_the_dead")) {
                CompoundTag n = c.getCompound("tag");
                if (n.hasUUID(BOOK_ID) && spent.contains(n.getUUID(BOOK_ID))) c.putByte("Count", (byte) 0);
            }
            if (c.contains("LongTravail", Tag.TAG_COMPOUND)) {
                CompoundTag root = c.getCompound("LongTravail");
                for (TravailAspect aspect : TravailAspect.values()) {
                    var key = JourneyState.identity(root, aspect);
                    if (key == null) continue;
                    if (aspect == TravailAspect.UNDERWORLD
                            && JourneyState.format(root, aspect) == JourneyState.Format.LEGACY
                            && witnesses.contains(key.journeyId()))
                        JourneyOperations.grantWitness(root, key, JourneyState.BOOK_SOURCE);
                    for (ResourceLocation source : themeWitnesses.getOrDefault(key, Set.of()))
                        JourneyOperations.grantWitness(root, key, source);
                }
            }
        }
    }

    public void recordSize() {
        com.thelongtravail.data.HotPathMetrics.Counter.STORAGE_SPENT.set(spent.size());
        com.thelongtravail.data.HotPathMetrics.Counter.STORAGE_WITNESSES.set(
                witnesses.size() + themeWitnesses.values().stream().mapToInt(Set::size).sum());
        com.thelongtravail.data.HotPathMetrics.Counter.STORAGE_GRAVES.set(graves.size());
    }

    public static UnderworldLedger load(CompoundTag n) {
        UnderworldLedger d = new UnderworldLedger();
        d.quarantined.merge(n.getCompound("QuarantinedLedger").copy());
        d.consumptionReliable = !d.quarantined.contains("Spent");
        for (Tag t : d.records(n, "Spent")) {
            try {
                if (!(t instanceof StringTag)) throw new IllegalArgumentException("Expected UUID string");
                d.spent.add(UUID.fromString(t.getAsString()));
            } catch (IllegalArgumentException invalid) {
                d.quarantine("Spent", t);
            }
        }
        for (Tag t : d.records(n, "Witnesses")) {
            if (!(t instanceof StringTag)) {
                d.quarantine("Witnesses", t);
                continue;
            }
            try {
                d.witnesses.add(UUID.fromString(t.getAsString()));
            } catch (IllegalArgumentException invalid) {
                d.retainedLegacyRecords.add(t.copy());
                com.thelongtravail.TheLongTravail.LOGGER.warn("Ignoring malformed legacy witness: {}", t);
            }
        }
        for (Tag t : d.records(n, "ThemeWitnesses")) {
            if (!(t instanceof CompoundTag entry)) {
                d.quarantine("ThemeWitnesses", t);
                continue;
            }
            TravailAspect aspect =
                    Arrays.stream(TravailAspect.values())
                            .filter(a -> a.id().equals(entry.getString("Aspect")))
                            .findFirst()
                            .orElse(null);
            ResourceLocation source = ResourceLocation.tryParse(entry.getString("Source"));
            if (entry.contains("RecordFormat", Tag.TAG_INT)
                    && entry.getInt("RecordFormat") == 1
                    && entry.hasUUID("JourneyId")
                    && entry.hasUUID("Epoch")
                    && aspect != null
                    && source != null)
                d.themeWitnesses
                        .computeIfAbsent(
                                new JourneyState.AspectJourneyKey(
                                        entry.getUUID("JourneyId"), aspect, entry.getUUID("Epoch")),
                                ignored -> new HashSet<>())
                        .add(source);
            else {
                d.retainedThemeRecords.add(entry.copy());
                com.thelongtravail.TheLongTravail.LOGGER.warn(
                        "Preserving inactive malformed/unsupported theme witness: {}", entry);
            }
        }
        for (Tag t : d.records(n, "Graves")) {
            if (t instanceof StringTag && validGrave(t.getAsString())) d.graves.add(t.getAsString());
            else d.quarantine("Graves", t);
        }
        for (Tag t : d.records(n, "Deaths")) {
            if (t instanceof CompoundTag c && validDeath(c)) d.deaths.put(c.getUUID("Player"), c.copy());
            else d.quarantine("Deaths", t);
        }
        if (!d.consumptionReliable)
            com.thelongtravail.TheLongTravail.LOGGER.warn(
                    "Hardcore Book of the Dead revival is paused: repair the quarantined Spent ledger records before clearing their quarantine");
        return d;
    }

    private ListTag records(CompoundTag root, String key) {
        Tag raw = root.get(key);
        if (raw == null) return new ListTag();
        if (raw instanceof ListTag list) return list;
        quarantine(key, raw);
        return new ListTag();
    }

    private void quarantine(String key, Tag raw) {
        ListTag entries = quarantined.getList(key, Tag.TAG_COMPOUND);
        CompoundTag entry = new CompoundTag();
        entry.put("Raw", raw.copy());
        entries.add(entry);
        quarantined.put(key, entries);
        if (key.equals("Spent")) consumptionReliable = false;
        if (entries.size() == 1)
            com.thelongtravail.TheLongTravail.LOGGER.warn(
                    "Preserving malformed underworld {} records in QuarantinedLedger; invalid records are inactive",
                    key);
    }

    private static boolean validGrave(String value) {
        int separator = value.lastIndexOf('|');
        if (separator <= 0 || ResourceLocation.tryParse(value.substring(0, separator)) == null) return false;
        try {
            Long.parseLong(value.substring(separator + 1));
            return true;
        } catch (NumberFormatException invalid) {
            return false;
        }
    }

    private static boolean validDeath(CompoundTag entry) {
        if (!entry.hasUUID("Player")
                || !entry.hasUUID("Death")
                || !entry.hasUUID("Book")
                || !entry.contains("Dimension", Tag.TAG_STRING)
                || entry.getString("Dimension").isEmpty()
                || ResourceLocation.tryParse(entry.getString("Dimension")) == null) return false;
        for (String key : List.of("X", "Y", "Z"))
            if (!entry.contains(key, Tag.TAG_ANY_NUMERIC) || !Double.isFinite(entry.getDouble(key))) return false;
        for (String key : List.of("Yaw", "Pitch"))
            if (!entry.contains(key, Tag.TAG_ANY_NUMERIC) || !Float.isFinite(entry.getFloat(key))) return false;
        for (String key : List.of("Width", "Height"))
            if (entry.contains(key)
                    && (!entry.contains(key, Tag.TAG_ANY_NUMERIC)
                            || !Float.isFinite(entry.getFloat(key))
                            || entry.getFloat(key) <= 0)) return false;
        return entry.contains("Hardcore", Tag.TAG_BYTE)
                && (!entry.contains("Ready") || entry.contains("Ready", Tag.TAG_BYTE));
    }

    @Override
    public CompoundTag save(CompoundTag n) {
        ListTag a = new ListTag(), b = new ListTag(), c = new ListTag(), g = new ListTag();
        spent.forEach(id -> a.add(StringTag.valueOf(id.toString())));
        witnesses.forEach(id -> b.add(StringTag.valueOf(id.toString())));
        deaths.values().forEach(v -> c.add(v.copy()));
        graves.forEach(v -> g.add(StringTag.valueOf(v)));
        ListTag themes = new ListTag();
        for (var row : themeWitnesses.entrySet())
            for (ResourceLocation source : row.getValue()) {
                var key = row.getKey();
                CompoundTag entry = new CompoundTag();
                entry.putInt("RecordFormat", 1);
                entry.putUUID("JourneyId", key.journeyId());
                entry.putString("Aspect", key.aspect().id());
                entry.putUUID("Epoch", key.epoch());
                entry.putString("Source", source.toString());
                themes.add(entry);
            }
        retainedThemeRecords.forEach(t -> themes.add(t.copy()));
        retainedLegacyRecords.forEach(t -> b.add(t.copy()));
        if (!quarantined.isEmpty()) n.put("QuarantinedLedger", quarantined.copy());
        n.put("ThemeWitnesses", themes);
        n.put("Spent", a);
        n.put("Witnesses", b);
        n.put("Deaths", c);
        n.put("Graves", g);
        return n;
    }
}
