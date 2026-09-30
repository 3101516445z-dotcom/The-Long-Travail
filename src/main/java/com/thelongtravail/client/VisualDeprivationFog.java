package com.thelongtravail.client;
import net.minecraft.client.Minecraft;
import net.minecraftforge.client.event.ViewportEvent;

/** Optional distance adapter; never widens existing fluid or other-mod fog. */
public final class VisualDeprivationFog {
    public static void renderFog(ViewportEvent.RenderFog event) {
        var player = Minecraft.getInstance().player;
        if (player == null || event.getCamera().getEntity() != player) return;
        float partial = (float) event.getPartialTick();
        float envelope = VisualDeprivationClient.envelope(player, partial);
        if (envelope <= 0) return;
        var style = VisualDeprivationClient.style(partial);
        float blend = envelope * style.distanceEnabled() * VisualDeprivationClient.pulse(partial, style);
        if (blend <= 0) return;
        float far = event.getFarPlaneDistance(), near = event.getNearPlaneDistance();
        float targetFar = Math.min(far, style.distanceEnd());
        float targetNear = Math.min(near, Math.min(style.distanceStart(), targetFar * .95F));
        event.setFarPlaneDistance(mix(far, targetFar, blend));
        event.setNearPlaneDistance(mix(near, targetNear, blend));
        event.setCanceled(true);
    }
    public static void color(ViewportEvent.ComputeFogColor event) {
        var player = Minecraft.getInstance().player;
        if (player == null || event.getCamera().getEntity() != player) return;
        float partial = (float) event.getPartialTick();
        float envelope = VisualDeprivationClient.envelope(player, partial);
        if (envelope <= 0) return;
        var style = VisualDeprivationClient.style(partial);
        float remaining = 1 - envelope * style.distanceEnabled() * VisualDeprivationClient.pulse(partial, style);
        event.setRed(event.getRed() * remaining);
        event.setGreen(event.getGreen() * remaining);
        event.setBlue(event.getBlue() * remaining);
    }
    private static float mix(float from, float to, float amount) {
        return from > 0 && to > 0 ? (float) Math.exp(Math.log(from) * (1 - amount) + Math.log(to) * amount)
                : from + (to - from) * amount;
    }
    private VisualDeprivationFog() {}
}
