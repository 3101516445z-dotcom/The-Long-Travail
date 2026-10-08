package com.thelongtravail.mixin;
import net.minecraft.client.particle.Particle;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
@Mixin(Particle.class)
public interface TimeStopParticleAccessor {
    @Accessor("xo") void travail$oldX(double value);
    @Accessor("yo") void travail$oldY(double value);
    @Accessor("zo") void travail$oldZ(double value);
    @Accessor("x") double travail$x();
    @Accessor("y") double travail$y();
    @Accessor("z") double travail$z();
}
