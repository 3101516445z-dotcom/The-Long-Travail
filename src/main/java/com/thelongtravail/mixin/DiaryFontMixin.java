package com.thelongtravail.mixin;

import com.thelongtravail.client.DiaryFontEffects;
import com.thelongtravail.client.DiaryText;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** 先于可选字体替换执行；递归调用不再携带材质标记。 */
@Mixin(value = Font.class, priority = 3000)
public abstract class DiaryFontMixin {
    @org.spongepowered.asm.mixin.Unique
    private static final float[][] travail$haloOffsets = travail$createOffsets();
    @org.spongepowered.asm.mixin.Unique
    private static float[][] travail$createOffsets() {
        float[][] offsets = new float[16][2];
        for (int ring = 0; ring < 2; ring++) for (int sample = 0; sample < 8; sample++) {
            double angle = (sample + ring * 0.5) * Math.PI / 4;
            float radius = ring == 0 ? 0.55F : 1.1F;
            offsets[ring * 8 + sample][0] = radius * (float) Math.cos(angle);
            offsets[ring * 8 + sample][1] = radius * (float) Math.sin(angle);
        }
        return offsets;
    }
    @Inject(method = "drawInBatch(Lnet/minecraft/network/chat/Component;FFIZLorg/joml/Matrix4f;Lnet/minecraft/client/renderer/MultiBufferSource;Lnet/minecraft/client/gui/Font$DisplayMode;II)I", at = @At("HEAD"), cancellable = true)
    private void travail$component(Component text, float x, float y, int color, boolean shadow,
                                   Matrix4f pose, MultiBufferSource buffers, Font.DisplayMode mode,
                                   int background, int light, CallbackInfoReturnable<Integer> cir) {
        travail$draw(text.getVisualOrderText(), x, y, color, shadow, pose, buffers, mode, background, light, cir);
    }

    @Inject(method = "drawInBatch(Lnet/minecraft/util/FormattedCharSequence;FFIZLorg/joml/Matrix4f;Lnet/minecraft/client/renderer/MultiBufferSource;Lnet/minecraft/client/gui/Font$DisplayMode;II)I", at = @At("HEAD"), cancellable = true)
    private void travail$sequence(FormattedCharSequence text, float x, float y, int color, boolean shadow,
                                  Matrix4f pose, MultiBufferSource buffers, Font.DisplayMode mode,
                                  int background, int light, CallbackInfoReturnable<Integer> cir) {
        travail$draw(text, x, y, color, shadow, pose, buffers, mode, background, light, cir);
    }

    private void travail$draw(FormattedCharSequence text, float x, float y, int color, boolean shadow,
                              Matrix4f pose, MultiBufferSource buffers, Font.DisplayMode mode,
                              int background, int light, CallbackInfoReturnable<Integer> cir) {
        int material = DiaryFontEffects.material(text);
        if (material < 0) return;
        FormattedCharSequence cleanText = material == 0 ? DiaryText.prepare(text) : DiaryFontEffects.unmarked(text);
        var quality = com.thelongtravail.config.TravailClientConfig.NAME_HALO_QUALITY.get();
        if (material == 0 && quality.samplesPerRing > 0) {
            int inputAlpha = color >>> 24;
            if (inputAlpha < 4) inputAlpha = 255; // 遵循 Font 对隐式不透明颜色的约定。
            MultiBufferSource haloBuffers = DiaryFontEffects.buffers(buffers, 2, x, 1);
            // 对称柔光核合批至同一渲染类型；主字形覆盖内部，着色器仅使外围光晕产生波动。
            for (int ring = 0; ring < 2; ring++) {
                float opacity = quality.opacity(ring == 0 ? 0.075F : 0.035F);
                int haloAlpha = Math.round(inputAlpha * opacity);
                if (haloAlpha < 4) continue;
                int haloColor = (haloAlpha << 24) | (color & 0xFFFFFF);
                for (int sample = 0; sample < 8; sample += 8 / quality.samplesPerRing) {
                    int offset = ring * 8 + (sample + (ring == 0 ? 0 : (8 / quality.samplesPerRing) / 2)) % 8;
                    Matrix4f haloPose = new Matrix4f(pose).translate(
                            travail$haloOffsets[offset][0], travail$haloOffsets[offset][1], 0);
                    ((Font) (Object) this).drawInBatch(cleanText, x, y, haloColor, false,
                            haloPose, haloBuffers, mode, 0, light);
                }
            }
        }
        cir.setReturnValue(((Font) (Object) this).drawInBatch(cleanText, x, y,
                color, shadow, pose, DiaryFontEffects.buffers(buffers, material, x,
                        material == 0 ? 1 : DiaryFontEffects.sweepWidth(text)), mode, background, light));
    }
}
