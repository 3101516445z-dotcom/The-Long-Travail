package com.thelongtravail.mixin;

import com.thelongtravail.client.TravailClientEvents;
import net.minecraft.client.gui.GuiGraphics;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// 先于提示框替换模组执行，因为它们可能绕过 Forge 提示框事件。
@Mixin(value = GuiGraphics.class, priority = 2000)
public abstract class GuiGraphicsTooltipMixin {
    @Inject(method = "renderTooltipInternal", at = @At("HEAD"), cancellable = true)
    private void theLongTravail$hideBackgroundTooltip(CallbackInfo ci) {
        if (TravailClientEvents.suppressBackgroundTooltip()) ci.cancel();
    }
}
