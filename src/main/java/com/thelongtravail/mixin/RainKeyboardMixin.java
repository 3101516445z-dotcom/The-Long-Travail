package com.thelongtravail.mixin;
import com.mojang.blaze3d.platform.InputConstants;
import com.thelongtravail.client.RainClient;
import net.minecraft.client.KeyboardHandler;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(KeyboardHandler.class)
public abstract class RainKeyboardMixin {
    @Inject(method="keyPress",at=@At("HEAD"),cancellable=true)
    private void travail$key(long window,int key,int scan,int action,int modifiers,CallbackInfo ci) {
        if(window==Minecraft.getInstance().getWindow().getWindow() && com.thelongtravail.client.BoundlessClient.press(InputConstants.getKey(key,scan),action)) { ci.cancel(); return; }
        if(window==Minecraft.getInstance().getWindow().getWindow()) RainClient.press(InputConstants.getKey(key,scan),action);
    }
}
