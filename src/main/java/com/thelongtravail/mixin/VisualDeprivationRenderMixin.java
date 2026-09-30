package com.thelongtravail.mixin;
import com.thelongtravail.client.VisualDeprivationRenderer;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
public abstract class VisualDeprivationRenderMixin {
    @Inject(method = "render", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/pipeline/RenderTarget;bindWrite(Z)V", shift = At.Shift.AFTER))
    private void travail$veil(float partial, long time, boolean renderLevel, CallbackInfo ci) {
        VisualDeprivationRenderer.render(partial);
    }
}
