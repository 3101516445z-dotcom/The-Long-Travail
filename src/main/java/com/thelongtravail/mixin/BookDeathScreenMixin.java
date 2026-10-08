package com.thelongtravail.mixin;

import com.thelongtravail.client.UnderworldClient;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LocalPlayer.class)
public abstract class BookDeathScreenMixin {
    // 战斗数据包和 Minecraft.setScreen(null) 均经过此处，重连时也不例外。
    @Inject(method="shouldShowDeathScreen",at=@At("RETURN"),cancellable=true)
    private void travail$choice(CallbackInfoReturnable<Boolean> ci){
        if(UnderworldClient.available())ci.setReturnValue(true);
    }
}
