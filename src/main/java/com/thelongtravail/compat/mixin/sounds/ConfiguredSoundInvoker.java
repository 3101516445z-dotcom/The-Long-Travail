package com.thelongtravail.compat.mixin.sounds;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.gen.Invoker;

@Pseudo
@Mixin(targets = "dev.imb11.sounds.api.config.ConfiguredSound", remap = false)
public interface ConfiguredSoundInvoker {
    @Invoker(value = "playSound", remap = false)
    void travail$playSound();
}
