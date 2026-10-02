package com.thelongtravail.client;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterShadersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import java.io.IOException;

/** 在 GUI 前执行一次 alpha 混合的世界渲染，不复制帧缓冲，也不依赖深度。 */
@Mod.EventBusSubscriber(modid = "the_long_travail", value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class VisualDeprivationRenderer {
    private static ShaderInstance shader;
    @SubscribeEvent
    public static void register(RegisterShadersEvent event) throws IOException {
        event.registerShader(new ShaderInstance(event.getResourceProvider(),
                new ResourceLocation("the_long_travail", "visual_deprivation"), DefaultVertexFormat.POSITION), value -> shader = value);
    }
    public static void render(float partial) {
        var mc = Minecraft.getInstance();
        float envelope = VisualDeprivationClient.envelope(mc.player, partial);
        if (shader == null || envelope <= 0) return;
        var style = VisualDeprivationClient.style(partial);
        float pulse = VisualDeprivationClient.pulse(partial, style);
        float radius = style.radius() * (1 + (1 - pulse) * style.pulseRadius());
        shader.safeGetUniform("Veil").set(envelope * pulse, style.darkening(), style.opacity(), radius);
        shader.safeGetUniform("Softness").set(style.softness());
        var previous = RenderSystem.getShader();
        RenderSystem.disableDepthTest(); RenderSystem.depthMask(false); RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc(); RenderSystem.disableCull();
        try {
            RenderSystem.setShader(() -> shader);
            var buffer = Tesselator.getInstance().getBuilder();
            buffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION);
            buffer.vertex(-1, -1, 0).endVertex(); buffer.vertex(1, -1, 0).endVertex();
            buffer.vertex(1, 1, 0).endVertex(); buffer.vertex(-1, 1, 0).endVertex();
            BufferUploader.drawWithShader(buffer.end());
        } finally {
            RenderSystem.setShader(() -> previous); RenderSystem.enableCull();
            RenderSystem.disableBlend(); RenderSystem.depthMask(true); RenderSystem.enableDepthTest();
        }
    }
    private VisualDeprivationRenderer() {}
}
