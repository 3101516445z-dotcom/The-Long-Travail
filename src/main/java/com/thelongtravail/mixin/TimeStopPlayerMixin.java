package com.thelongtravail.mixin;
import com.thelongtravail.boundless.*;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(ServerPlayer.class)
public abstract class TimeStopPlayerMixin {
    @Inject(method={"tick","doTick"},at=@At("HEAD"),cancellable=true)
    private void travail$freezePlayer(CallbackInfo ci){ServerPlayer p=(ServerPlayer)(Object)this;if(TimeStopManager.frozen(p)){FrozenEntityClock.tick(p);p.setOldPosAndRot();ci.cancel();}}
}
