package com.thelongtravail.client;
import java.util.*;
public final class EffectSoundPolicy {
    public enum Reason { GAIN, CLEAR, MALICE_CLEAR, EXPIRE }
    private record Mark(String id, boolean gain, Reason reason, long serial, long batch, long tick) {}
    private record Pending(String id, boolean gain, java.util.function.BooleanSupplier play, long tick) {}
    private record Cleared(long tick, long batch, boolean malice) {}
    private final ArrayDeque<Mark> marks = new ArrayDeque<>();
    private final ArrayDeque<Pending> pending = new ArrayDeque<>();
    private final Map<String, Cleared> cleared = new LinkedHashMap<>();
    private final Map<String, Long> warnings = new LinkedHashMap<>();
    private final Map<Long, Long> batches = new LinkedHashMap<>();
    private final java.util.function.BiConsumer<String, String> diagnostic;
    private long tick;
    public EffectSoundPolicy(java.util.function.BiConsumer<String, String> diagnostic) { this.diagnostic = diagnostic; }
    public void clear() { marks.clear(); pending.clear(); cleared.clear(); warnings.clear(); batches.clear(); tick = 0; }
    public void mark(String id, Reason reason, long serial, long batch) {
        if (marks.stream().anyMatch(m -> m.serial == serial)) return;
        if (marks.size() >= 512) marks.removeFirst();
        marks.addLast(new Mark(id, reason == Reason.GAIN, reason, serial, batch, tick));
        if (reason == Reason.CLEAR || reason == Reason.MALICE_CLEAR) {
            trim(cleared, 256); cleared.put(id, new Cleared(tick, batch, reason == Reason.MALICE_CLEAR));
        }
    }
    // 回调仅在实际调用播放时返回 true。
    public void notification(String id, boolean gain, java.util.function.BooleanSupplier play) {
        if (pending.size() >= 512) {
            var overflow = pending.removeFirst();
            if (overflow.play.getAsBoolean()) diagnostic.accept("overflow-play", overflow.id);
        }
        pending.addLast(new Pending(id, gain, play, tick));
    }
    public void advance() {
        tick++;
        // 为跟随原版效果包到达的原因包预留两个客户端刻。
        while (!pending.isEmpty() && tick - pending.peekFirst().tick >= 2) decide(pending.removeFirst());
        marks.removeIf(m -> tick - m.tick > 10);
        cleared.entrySet().removeIf(e -> tick - e.getValue().tick > 40);
        warnings.entrySet().removeIf(e -> tick - e.getValue() >= 200);
        batches.entrySet().removeIf(e -> tick - e.getValue() > 40);
    }
    private void decide(Pending event) {
        var iterator = marks.iterator();
        while (iterator.hasNext()) {
            var mark = iterator.next();
            if (mark.id.equals(event.id) && mark.gain == event.gain && Math.abs(mark.tick - event.tick) <= 10) {
                iterator.remove(); diagnostic.accept("quiet-" + mark.reason, event.id); return;
            }
        }
        Cleared recent = cleared.get(event.id);
        if (event.gain && recent != null && event.tick >= recent.tick - 2 && event.tick - recent.tick <= 40) {
            Long warned = warnings.get(event.id);
            if (warned != null && tick - warned < 200) { diagnostic.accept("quiet-repeat", event.id); return; }
            // 负面警告逐效果保留；恶意清除的有益效果可批量合并通知。
            if (recent.malice && batches.containsKey(recent.batch)) { diagnostic.accept("quiet-batch", event.id); return; }
            trim(warnings, 256); warnings.put(event.id, tick);
            if (recent.malice) { trim(batches, 256); batches.put(recent.batch, tick); }
        }
        if (event.play.getAsBoolean()) diagnostic.accept(event.gain ? "play-gain" : "play-loss", event.id);
    }
    private static <K,V> void trim(Map<K,V> map, int limit) {
        if (map.size() >= limit) map.remove(map.keySet().iterator().next());
    }
}
