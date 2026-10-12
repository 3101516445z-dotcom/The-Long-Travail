package com.thelongtravail.mixin;

import com.thelongtravail.client.TimeStopClient;
import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ParticleEngine.class)
public abstract class TimeStopParticleMixin {
    @Inject(method = "tickParticle", at = @At("HEAD"), cancellable = true)
    private void travail$particle(Particle p, CallbackInfo ci) {
        if (TimeStopClient.fields.isEmpty() || !TimeStopClient.valid(Minecraft.getInstance().level))
            return;
        // 仅启动提示继续播放，不豁免世界中的其他末地烛粒子。
        if (com.thelongtravail.client.TimeStopCue.exempt(p)) return;
        var a = (TimeStopParticleAccessor) p;
        double x = a.travail$x(), y = a.travail$y(), z = a.travail$z();
        for (var field : TimeStopClient.fields) {
            double dx = field.center().x - x;
            double dy = field.center().y - y;
            double dz = field.center().z - z;
            if (dx * dx + dy * dy + dz * dz <= field.radius() * field.radius()) {
                a.travail$oldX(x);
                a.travail$oldY(y);
                a.travail$oldZ(z);
                ci.cancel();
                return;
            }
        }
    }
}
