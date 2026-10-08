package com.thelongtravail.mixin;
import net.minecraft.world.ticks.*;
import it.unimi.dsi.fastutil.longs.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
@Mixin(LevelTicks.class)
public interface TimeStopTicksAccessor<T> {
    @Accessor("allContainers") Long2ObjectMap<LevelChunkTicks<T>> travail$containers();
    @Accessor("nextTickForContainer") Long2LongMap travail$nextTicks();
}
