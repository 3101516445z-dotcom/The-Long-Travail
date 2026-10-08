package com.thelongtravail.mixin;
import com.thelongtravail.abyss.*;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(Biome.class)
public abstract class RainBiomeMixin {
    @Inject(method="hasPrecipitation",at=@At("RETURN"),cancellable=true)
    private void travail$dry(CallbackInfoReturnable<Boolean> cir) {
        Level level=PrecipitationScope.level();
        if(level!=null && RainWeather.dry(level)) cir.setReturnValue(true);
    }
    @Inject(method="getPrecipitationAt",at=@At("RETURN"),cancellable=true)
    private void travail$type(BlockPos pos,CallbackInfoReturnable<Biome.Precipitation> cir) {
        Level level=PrecipitationScope.level(); if(level==null) return;
        if(cir.getReturnValue()==Biome.Precipitation.NONE && RainWeather.dry(level)
                || cir.getReturnValue()==Biome.Precipitation.SNOW && RainWeather.snow(level)) cir.setReturnValue(Biome.Precipitation.RAIN);
    }
    @Inject(method="shouldSnow",at=@At("HEAD"),cancellable=true)
    private void travail$snow(LevelReader reader,BlockPos pos,CallbackInfoReturnable<Boolean> cir) {
        if(reader instanceof Level level && RainWeather.snow(level)) cir.setReturnValue(false);
    }
    @Inject(method="shouldFreeze(Lnet/minecraft/world/level/LevelReader;Lnet/minecraft/core/BlockPos;Z)Z",at=@At("HEAD"),cancellable=true)
    private void travail$ice(LevelReader reader,BlockPos pos,boolean edge,CallbackInfoReturnable<Boolean> cir) {
        if(reader instanceof Level level && RainWeather.snow(level)) cir.setReturnValue(false);
    }
}
