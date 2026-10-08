package com.thelongtravail.client;

import com.thelongtravail.config.TravailConfig;
import com.thelongtravail.config.TravailClientConfig;
import com.thelongtravail.network.TooltipConfigSync;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;

public final class VisualDeprivationClient {
    private static final VisualDeprivationTimeline TIMELINE = new VisualDeprivationTimeline();
    private static VisualDeprivationStyle from, target;
    private static double styleStart;
    private static long revision = Long.MIN_VALUE;
    private static Object level, owner;
    private static float phase, previousPhase;

    public static void accept(int remaining, int total) { accept(remaining, total, false); }
    public static void progress(int remaining) {
        var mc = Minecraft.getInstance();
        if (mc.player != null && mc.player == owner && mc.level == level && mc.player.isAlive())
            TIMELINE.progress(mc.player, remaining);
    }
    public static void accept(int remaining, int total, boolean immediate) {
        var minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null || !player.isAlive()) { reset(); return; }
        if (owner != player || level != minecraft.level) reset();
        owner = player; level = minecraft.level;
        boolean wasVisible = TIMELINE.sample(minecraft.getFrameTime()) > 0 || TIMELINE.remaining() > 0;
        if (remaining <= 0 && (immediate || !wasVisible)) { reset(); return; }
        TIMELINE.accept(player, player.tickCount, remaining, total,
                integer("deepValley.visualDeprivationFadeInTicks", TravailConfig.VALLEY_VISUAL_DEPRIVATION_FADE_IN_TICKS.get()),
                integer("deepValley.visualDeprivationFadeOutTicks", TravailConfig.VALLEY_VISUAL_DEPRIVATION_FADE_OUT_TICKS.get()),
                integer("visual.refreshBlendTicks", TravailConfig.VISUAL_REFRESH_TICKS.get()),
                integer("visual.clearFadeOutTicks", TravailConfig.VISUAL_CLEAR_TICKS.get()), immediate);
        if (!wasVisible && remaining > 0) phase = previousPhase = 0;
        updateStyle();
    }
    public static void tick() {
        var mc = Minecraft.getInstance();
        if (mc.player == null || !mc.player.isAlive() || mc.level != level || mc.player != owner) { reset(); level = mc.level; owner = mc.player; return; }
        if (mc.isPaused() || com.thelongtravail.boundless.TimeStopManager.frozen(mc.player)) return;
        TIMELINE.tick(mc.player); updateStyle();
        previousPhase = phase;
        if (target != null) phase += (float) (Math.PI * 2 / (Math.max(0.1F, style(0).pulsePeriod()) * 20));
        if (phase > Math.PI * 2) { phase -= (float) (Math.PI * 2); previousPhase -= (float) (Math.PI * 2); }
    }
    private static void updateStyle() {
        long next = TooltipConfigSync.revision();
        if (target != null && revision == next) return;
        var current = target == null ? null : style(0);
        target = readStyle(); from = current == null ? target : current;
        styleStart = TIMELINE.time(0); revision = next;
    }
    public static VisualDeprivationStyle style(float partial) {
        if (target == null) updateStyle();
        return from.blend(target, (float) ((TIMELINE.time(partial) - styleStart) / 10));
    }
    public static float pulse(float partial, VisualDeprivationStyle style) {
        return style.pulse(previousPhase + (phase - previousPhase) * partial,
                TravailClientConfig.REDUCE_VISUAL_MOTION.get());
    }
    public static void reset() {
        TIMELINE.reset(); from = target = null; revision = Long.MIN_VALUE; level = owner = null; phase = previousPhase = 0;
    }
    public static float envelope(LocalPlayer player, float partialTick) {
        if (player == null || !player.isAlive() || player != Minecraft.getInstance().player) return 0;
        return TIMELINE.sample(partialTick);
    }
    private static float decimal(String key, double fallback) { return (float) TooltipConfigSync.decimal(key, fallback); }
    private static int integer(String key, int fallback) { return TooltipConfigSync.integer(key, fallback); }
    private static VisualDeprivationStyle readStyle() {
        return new VisualDeprivationStyle(
                decimal("visual.worldDarkening", TravailConfig.VISUAL_DARKENING.get()),
                decimal("visual.peripheralOpacity", TravailConfig.VISUAL_OPACITY.get()),
                decimal("visual.clearRadius", TravailConfig.VISUAL_RADIUS.get()),
                decimal("visual.edgeSoftness", TravailConfig.VISUAL_SOFTNESS.get()),
                decimal("visual.distanceVeilEnabled", TravailConfig.VISUAL_DISTANCE_ENABLED.get() ? 1 : 0),
                decimal("visual.distanceVeilStart", TravailConfig.VISUAL_DISTANCE_START.get()),
                decimal("visual.distanceVeilEnd", TravailConfig.VISUAL_DISTANCE_END.get()),
                decimal("visual.pulsePeriodSeconds", TravailConfig.VISUAL_PULSE_PERIOD.get()),
                decimal("visual.pulseDepth", TravailConfig.VISUAL_PULSE_DEPTH.get()),
                decimal("visual.pulseAffectsRadius", TravailConfig.VISUAL_PULSE_RADIUS.get() ? 1 : 0));
    }
    private VisualDeprivationClient() {}
}
