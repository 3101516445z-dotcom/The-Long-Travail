package com.thelongtravail.mixin;
import com.thelongtravail.abyss.PrecipitationScope;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.world.level.Level;
import net.minecraft.core.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
@Mixin(Level.class)
public abstract class RainLevelMixin {
    @WrapMethod(method="isRainingAt")
    private boolean travail$rainAt(BlockPos pos,Operation<Boolean> original) {
        try(var scope=new PrecipitationScope((Level)(Object)this)) { return original.call(pos); }
    }
}
