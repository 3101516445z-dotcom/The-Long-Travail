package com.thelongtravail.compat.mixin.jei;

import com.thelongtravail.client.TravailClientEvents;
import com.thelongtravail.client.catalog.CatalogSession;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "mezz.jei.gui.events.GuiEventHandler", remap = false)
public abstract class JeiReadingOverlayMixin {
    @Inject(
            method = {"onDrawBackgroundPost", "onDrawForeground", "onDrawScreenPost"},
            at = @At("HEAD"),
            cancellable = true,
            remap = false,
            require = 0)
    private void theLongTravail$hideJeiWhileReading(CallbackInfo ci) {
        if (TravailClientEvents.isReading() || CatalogSession.active() != null) ci.cancel();
    }
}
