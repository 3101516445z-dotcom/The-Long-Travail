package com.thelongtravail.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.thelongtravail.TheLongTravail;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterShadersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.io.IOException;

/** Scoped text materials: retain the active font's atlas, layout and render states. */
@Mod.EventBusSubscriber(modid = TheLongTravail.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class DiaryFontEffects {
    private static ShaderInstance shader;
    private static long resourceGeneration;
    public static long resourceGeneration() { return resourceGeneration; }
    private static final boolean[] loggedMaterials = new boolean[3];
    private static final long EPOCH = System.nanoTime();

    @SubscribeEvent
    public static void registerShaders(RegisterShadersEvent event) throws IOException {
        event.registerShader(new ShaderInstance(event.getResourceProvider(),
                new ResourceLocation(TheLongTravail.MODID, "diary_font"), DefaultVertexFormat.POSITION_COLOR_TEX),
                loaded -> {
                    shader = loaded;
                    resourceGeneration++;
                    DiaryDialogue.clear();
                    java.util.Arrays.fill(loggedMaterials, false);
                    TheLongTravail.LOGGER.info("Diary font shader loaded");
                });
    }

    private static final ThreadLocal<MaterialProbe> PROBE = ThreadLocal.withInitial(MaterialProbe::new);
    private static final class MaterialProbe implements net.minecraft.util.FormattedCharSink {
        int material; boolean mixed, busy;
        public boolean accept(int index, net.minecraft.network.chat.Style style, int codePoint) {
            if (Character.isWhitespace(codePoint)) return true;
            String marker = style.getInsertion();
            int current = "the_long_travail:name".equals(marker) ? 0
                    : marker != null && marker.startsWith("the_long_travail:prose:") ? 1 : -1;
            if (current < 0 || (material >= 0 && material != current)) { mixed = true; return false; }
            material = current; return true;
        }
    }
    public static int material(FormattedCharSequence text) {
        if (text instanceof DiaryText.Unmarked) return -1;
        MaterialProbe probe = PROBE.get();
        if (probe.busy) probe = new MaterialProbe();
        probe.busy = true; probe.material = -1; probe.mixed = false;
        try { text.accept(probe); return probe.mixed ? -1 : probe.material; }
        finally { probe.busy = false; }
    }

    public static FormattedCharSequence unmarked(FormattedCharSequence text) {
        return DiaryText.lazyUnmarked(text);
    }

    public static float sweepWidth(FormattedCharSequence text) {
        float[] width = {1};
        text.accept((index, style, codePoint) -> {
            String marker = style.getInsertion();
            if (marker != null && marker.startsWith("the_long_travail:prose:")) {
                try { width[0] = Math.max(1, Float.parseFloat(marker.substring(marker.lastIndexOf(':') + 1))); }
                catch (NumberFormatException ignored) { }
                return false;
            }
            return true;
        });
        return width[0];
    }

    public static MultiBufferSource buffers(MultiBufferSource original, int material, float originX, float sweepWidth) {
        return new MaterialBuffers(original, material, originX, sweepWidth);
    }

    /** Most text uses one atlas. Allocate a map only when this draw actually uses several types. */
    private static final class MaterialBuffers extends DrawScopedCache<RenderType, RenderType> implements MultiBufferSource {
        private final MultiBufferSource original;
        private final int material;
        private final float originX, sweepWidth;

        private MaterialBuffers(MultiBufferSource original, int material, float originX, float sweepWidth) {
            this.original = original; this.material = material;
            this.originX = originX; this.sweepWidth = sweepWidth;
        }

        public com.mojang.blaze3d.vertex.VertexConsumer getBuffer(RenderType type) {
            return original.getBuffer(value(type));
        }

        protected RenderType create(RenderType type) {
            return new MaterialType(type, material, originX, sweepWidth);
        }
    }

    private static final class MaterialType extends RenderType {
        private MaterialType(RenderType original, int material, float originX, float sweepWidth) {
            super("travail_font", original.format(), original.mode(), original.bufferSize(),
                    false, false, () -> setup(original, material, originX, sweepWidth), original::clearRenderState);
        }
    }

    private static void setup(RenderType original, int material, float originX, float sweepWidth) {
        original.setupRenderState();
        if (shader == null) return;
        ShaderInstance originalShader = RenderSystem.getShader();
        String name = originalShader == null ? "" : originalShader.getName();
        // Modern UI's SDF fill uses a distance field; bitmap/intensity atlases use alpha/red.
        // Keep outlines and non-text buffers on their original pipeline.
        if (name.contains("stroke") || name.contains("background")) return;
        int atlas = name.contains("sdf") ? 2 : name.contains("intensity") ? 1 : 0;
        if (!loggedMaterials[material]) {
            loggedMaterials[material] = true;
            TheLongTravail.LOGGER.info("Diary text material active: {}, original shader: {}, atlas: {}",
                    material == 0 ? "name" : material == 1 ? "prose" : "rippling halo", name, atlas);
        }
        RenderSystem.setShader(() -> shader);
        shader.safeGetUniform("Seconds").set((System.nanoTime() - EPOCH) / 1_000_000_000F % 3600F);
        shader.safeGetUniform("Material").set((float) material);
        shader.safeGetUniform("AtlasMode").set((float) atlas);
        shader.safeGetUniform("OriginX").set(originX);
        shader.safeGetUniform("SweepWidth").set(sweepWidth);
    }

    private DiaryFontEffects() {}
}
