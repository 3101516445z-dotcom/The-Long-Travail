package com.thelongtravail.client;

/** Pure animation state: logical expiry and a short visual release are separate. */
public final class VisualDeprivationTimeline {
    private Object owner;
    private double clock, lastSample, episodeStart, riseStart, riseLength, end, fallLength;
    private float riseFrom;
    private int total;
    private long startTick;
    private boolean releasing;

    public void accept(Object player, long tick, int remaining, int duration) {
        accept(player, tick, remaining, duration, 20, 20, 5, 0, true);
    }
    public void accept(Object player, long tick, int remaining, int duration,
                       float fadeIn, float fadeOut, float refresh, float clear, boolean immediate) {
        syncOwner(player);
        if (player == null) { reset(); return; }
        double now = Math.max(clock, lastSample);
        float current = value(now);
        if (remaining <= 0) {
            if (immediate || clear <= 0 || current <= 0) { reset(); return; }
            if (releasing) return;
            releasing = true; total = 0; riseFrom = current;
            riseStart = now; riseLength = Math.max(0, clear); end = now;
            return;
        }
        boolean continuation = current > 0 || (!releasing && end > now);
        if (!continuation) { episodeStart = now; startTick = tick; }
        total = Math.max(remaining, duration);
        double incoming = Math.max(0, fadeIn), outgoing = Math.max(0, fadeOut);
        double scale = incoming + outgoing > total ? total / (incoming + outgoing) : 1;
        incoming *= scale; outgoing *= scale;
        boolean wasReleasing = releasing;
        double previousRiseEnd = riseStart + riseLength;
        releasing = false; end = now + remaining;
        riseFrom = current; riseStart = now;
        riseLength = continuation && !wasReleasing && now < previousRiseEnd
                ? previousRiseEnd - now : continuation ? Math.max(0, refresh) : incoming;
        double sum = riseLength + outgoing;
        if (sum > remaining) { riseLength *= remaining / sum; outgoing *= remaining / sum; }
        fallLength = outgoing;
    }
    public void tick(Object player) { if (!syncOwner(player)) clock++; }
    private boolean syncOwner(Object player) {
        if (owner == player) return false;
        reset(); owner = player; return true;
    }
    public float sample(float partialTick) {
        lastSample = clock + Math.max(0, Math.min(1, partialTick));
        return value(lastSample);
    }
    private float value(double time) {
        if (owner == null) return 0;
        if (releasing) return riseFrom * (1 - smooth((time - riseStart) / Math.max(0.0001, riseLength)));
        if (time >= end) return 0;
        float in = riseLength <= 0 ? 1 : smooth((time - riseStart) / riseLength);
        float out = fallLength <= 0 ? 1 : smooth((end - time) / fallLength);
        return Math.min(riseFrom + (1 - riseFrom) * in, out);
    }
    public static float smooth(double value) {
        double x = Math.max(0, Math.min(1, value));
        return (float) (x * x * (3 - 2 * x));
    }
    public double time(float partialTick) { return clock + partialTick; }
    public void reset() {
        owner = null; clock = lastSample = episodeStart = riseStart = riseLength = end = fallLength = 0;
        total = 0; startTick = 0; riseFrom = 0; releasing = false;
    }
    public int remaining() { return releasing ? 0 : (int) Math.max(0, Math.min(Integer.MAX_VALUE, Math.ceil(end - clock))); }
    public int total() { return total; }
    public long startTick() { return startTick; }
}
