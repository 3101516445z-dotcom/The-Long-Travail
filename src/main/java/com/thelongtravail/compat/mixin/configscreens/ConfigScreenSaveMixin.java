package com.thelongtravail.compat.mixin.configscreens;

import com.thelongtravail.compat.configscreens.ConfigScreenAccess;

import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "fuzs.forgeconfigscreens.client.gui.screens.ConfigScreen$Main", remap = false)
public abstract class ConfigScreenSaveMixin {
    @Inject(method = "lambda$init$0", at = @At("HEAD"), cancellable = true, remap = false)
    private void travail$save(Button button, CallbackInfo ci) {
        var session = ((ConfigScreenAccess) this).travail$configSession();
        if (session != null) {
            ci.cancel();
            session.save((Screen) (Object) this);
        }
    }
}
