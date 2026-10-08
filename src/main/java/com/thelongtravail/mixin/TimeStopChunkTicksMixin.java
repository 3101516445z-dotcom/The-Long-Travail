package com.thelongtravail.mixin;

import com.thelongtravail.boundless.TickContainerRevision;
import net.minecraft.world.ticks.LevelChunkTicks;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LevelChunkTicks.class)
public abstract class TimeStopChunkTicksMixin implements TickContainerRevision {
    @Unique private long travail$revision;
    @Override public long travail$tickRevision() { return travail$revision; }
    @Inject(method={"scheduleUnchecked", "unpack", "removeIf"}, at=@At("TAIL"))
    private void travail$changed(CallbackInfo ci) { travail$revision++; }
}
