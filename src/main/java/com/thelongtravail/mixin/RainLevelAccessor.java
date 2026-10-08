package com.thelongtravail.mixin;

import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

// getThunderLevel 已乘以降雨强度，快照和原版数据包需要读取未经处理的字段值。
@Mixin(Level.class)
public interface RainLevelAccessor {
    @Accessor("thunderLevel") float travail$rawThunderLevel();
}
