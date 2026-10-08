package com.thelongtravail.mixin;
import com.thelongtravail.boundless.TimeStopManager;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(LocalPlayer.class)
public abstract class TimeStopLocalPlayerMixin {
    @Inject(method="tick",at=@At("HEAD"),cancellable=true)
    private void travail$freeze(CallbackInfo ci){LocalPlayer p=(LocalPlayer)(Object)this;if(TimeStopManager.frozen(p)){p.setOldPosAndRot();ci.cancel();}}
}
