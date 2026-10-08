package com.thelongtravail.mixin;
import com.mojang.blaze3d.platform.InputConstants;
import com.thelongtravail.client.RainClient;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(MouseHandler.class)
public abstract class RainMouseMixin {
    @Inject(method="onPress",at=@At("HEAD"))
    private void travail$mouse(long window,int button,int action,int modifiers,CallbackInfo ci) {
        if(window==Minecraft.getInstance().getWindow().getWindow()) RainClient.press(InputConstants.Type.MOUSE.getOrCreate(button),action);
    }
}
