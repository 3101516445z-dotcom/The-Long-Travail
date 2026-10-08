package com.thelongtravail.mixin;

import com.thelongtravail.client.DiaryFontEffects;
import com.thelongtravail.client.DiaryText;
import com.thelongtravail.client.NameHaloKernel;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

// 先于可选字体替换执行；递归调用不再携带材质标记。
@Mixin(value = Font.class, priority = 3000)
public abstract class DiaryFontMixin {
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
        var quality = (material >= 3 ? com.thelongtravail.config.TravailClientConfig.ASPECT_NAME_HALO_QUALITY
                : com.thelongtravail.config.TravailClientConfig.NAME_HALO_QUALITY).get();
        NameHaloKernel kernel = NameHaloKernel.of(quality);
        boolean halo = (material == 0 || material >= 3) && kernel.samplesPerRing > 0
                && (kernel.alpha(color, 0) >= 4 || kernel.alpha(color, 1) >= 4);
        FormattedCharSequence cleanText = DiaryText.forDraw(text, halo);
        if (halo) {
            MultiBufferSource haloBuffers = DiaryFontEffects.buffers(buffers, material == 0 ? 2 : material + 6, x, 1);
            Matrix4f haloPose = new Matrix4f();
            // 对称柔光核合批至同一渲染类型；主字形覆盖内部，着色器仅使外围光晕产生波动。
            for (int ring = 0; ring < 2; ring++) {
                int haloAlpha = kernel.alpha(color, ring);
                if (haloAlpha < 4) continue;
                int haloColor = (haloAlpha << 24) | (color & 0xFFFFFF);
                for (int sample = 0; sample < kernel.samplesPerRing; sample++) {
                    haloPose.set(pose).translate(kernel.x(ring, sample), kernel.y(ring, sample), 0);
                    ((Font) (Object) this).drawInBatch(cleanText, x, y, haloColor, false,
                            haloPose, haloBuffers, mode, 0, light);
                }
            }
        }
        cir.setReturnValue(((Font) (Object) this).drawInBatch(cleanText, x, y,
                color, shadow, pose, DiaryFontEffects.buffers(buffers, material, x,
                        material == 1 ? DiaryFontEffects.sweepWidth(text) : 1), mode, background, light));
    }
}
