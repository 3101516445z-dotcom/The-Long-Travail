package com.thelongtravail.compat.mixin.embeddium;

import com.thelongtravail.client.LanternLightingClient;
import me.jellysquid.mods.sodium.client.model.quad.ModelQuadView;
import me.jellysquid.mods.sodium.client.model.light.data.QuadLightData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(
        targets = {
            "me.jellysquid.mods.sodium.client.model.light.smooth.SmoothLightPipeline",
            "me.jellysquid.mods.sodium.client.model.light.flat.FlatLightPipeline"
        },
        remap = false)
public abstract class LanternEmbeddiumLightMixin {
    @Inject(method = "calculate", at = @At("RETURN"), remap = false)
    private void travail$vertexLantern(
            ModelQuadView quad,
            BlockPos pos,
            QuadLightData out,
            Direction cullFace,
            Direction lightFace,
            boolean shade,
            CallbackInfo ci) {
        var lights = LanternLightingClient.lightingSnapshot();
        if (lights.isEmpty()) return;
        for (int i = 0; i < 4; i++)
            out.lm[i] =
                    lights.merge(
                            pos.getX() + quad.getX(i),
                            pos.getY() + quad.getY(i),
                            pos.getZ() + quad.getZ(i),
                            out.lm[i]);
    }
}
