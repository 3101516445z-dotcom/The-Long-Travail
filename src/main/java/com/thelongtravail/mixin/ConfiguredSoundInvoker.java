package com.thelongtravail.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.gen.Invoker;

// 仅供客户端使用的可选适配器，插件会先检查 Sounds 的类结构再应用。
@Pseudo
@Mixin(targets = "dev.imb11.sounds.api.config.ConfiguredSound", remap = false)
public interface ConfiguredSoundInvoker {
    @Invoker(value = "playSound", remap = false)
    void travail$playSound();
}
