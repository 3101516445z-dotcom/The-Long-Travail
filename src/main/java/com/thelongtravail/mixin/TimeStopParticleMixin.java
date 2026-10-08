package com.thelongtravail.mixin;
import com.thelongtravail.client.TimeStopClient;
import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.*;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(ParticleEngine.class)
public abstract class TimeStopParticleMixin {
    @Inject(method="tickParticle",at=@At("HEAD"),cancellable=true)
    private void travail$particle(Particle p,CallbackInfo ci){
        if(!TimeStopClient.valid(Minecraft.getInstance().level))return;
        var a=(TimeStopParticleAccessor)p;Vec3 pos=new Vec3(a.travail$x(),a.travail$y(),a.travail$z());
        if(TimeStopClient.fields.stream().anyMatch(f->f.contains(pos))){a.travail$oldX(pos.x);a.travail$oldY(pos.y);a.travail$oldZ(pos.z);ci.cancel();}
    }
}
