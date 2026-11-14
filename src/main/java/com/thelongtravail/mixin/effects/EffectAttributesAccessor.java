package com.thelongtravail.mixin.effects;

import net.minecraft.world.entity.ai.attributes.*;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Map;

// 仅供显式效果植入的失败恢复读取；不拦截正常属性更新。
@Mixin(AttributeMap.class)
public interface EffectAttributesAccessor {
    @Accessor("attributes")
    Map<Attribute, AttributeInstance> travail$instances();

    @Accessor("supplier")
    AttributeSupplier travail$supplier();
}
