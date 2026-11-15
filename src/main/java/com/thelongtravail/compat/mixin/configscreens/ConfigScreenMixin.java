package com.thelongtravail.compat.mixin.configscreens;

import com.thelongtravail.compat.configscreens.ConfigScreenAccess;
import com.thelongtravail.compat.configscreens.ConfigScreenSession;
import com.thelongtravail.config.storage.ManagedConfig;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraftforge.fml.config.ModConfig;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Map;

@Pseudo
@Mixin(targets = "fuzs.forgeconfigscreens.client.gui.screens.ConfigScreen", remap = false)
public abstract class ConfigScreenMixin implements ConfigScreenAccess {
    @Unique private ConfigScreenSession travail$configSession;

    public ConfigScreenSession travail$configSession() {
        return travail$configSession;
    }

    public void travail$configSession(ConfigScreenSession session) {
        travail$configSession = session;
    }

    @Inject(method = "create", at = @At("RETURN"), remap = false)
    private static void travail$capture(
            Screen parent,
            Component title,
            ModConfig config,
            Map<?, ?> values,
            CallbackInfoReturnable<Object> cir) {
        if (config instanceof ManagedConfig managed)
            ((ConfigScreenAccess) cir.getReturnValue())
                    .travail$configSession(new ConfigScreenSession(managed, parent, values));
    }
}
