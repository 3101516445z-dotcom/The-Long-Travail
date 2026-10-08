package com.thelongtravail.mixin;
import com.thelongtravail.abyss.*;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(ServerLevel.class)
public abstract class RainServerMixin {
    @org.spongepowered.asm.mixin.Unique private boolean travail$freezeWeatherCycle;
    @Inject(method="advanceWeatherCycle",at=@At("HEAD"))
    private void travail$weather(CallbackInfo ci) {
        ServerLevel level=(ServerLevel)(Object)this;
        travail$freezeWeatherCycle = level.dimension().equals(Level.OVERWORLD) && RainWeather.get(level).advance(level);
    }
    @com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation(method="advanceWeatherCycle",at=@At(value="INVOKE",target="Lnet/minecraft/world/level/GameRules;getBoolean(Lnet/minecraft/world/level/GameRules$Key;)Z"))
    private boolean travail$pauseNaturalCycle(net.minecraft.world.level.GameRules rules,
            net.minecraft.world.level.GameRules.Key<net.minecraft.world.level.GameRules.BooleanValue> key, Operation<Boolean> original) {
        return !(travail$freezeWeatherCycle && key == net.minecraft.world.level.GameRules.RULE_WEATHER_CYCLE) && original.call(rules,key);
    }
    @Inject(method="resetWeatherCycle",at=@At("HEAD"),cancellable=true)
    private void travail$sleep(CallbackInfo ci) { if(RainWeather.active((ServerLevel)(Object)this)) ci.cancel(); }
    @WrapMethod(method="tickChunk")
    private void travail$precipitation(LevelChunk chunk,int speed,Operation<Void> original) {
        try(var scope=new PrecipitationScope((ServerLevel)(Object)this)) { original.call(chunk,speed); }
    }
}
