package com.thelongtravail.mixin;
import net.minecraft.world.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
@Mixin(CompoundContainer.class)
public interface TimeStopCompoundAccessor {
    @Accessor("container1") Container travail$first();
    @Accessor("container2") Container travail$second();
}
