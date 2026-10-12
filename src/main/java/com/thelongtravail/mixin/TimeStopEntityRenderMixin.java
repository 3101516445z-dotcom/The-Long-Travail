package com.thelongtravail.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.mojang.blaze3d.vertex.PoseStack;
import com.thelongtravail.boundless.TimeStopManager;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(EntityRenderDispatcher.class)
public abstract class TimeStopEntityRenderMixin {
    @WrapMethod(method = "render")
    private void travail$freezeAnimation(
            Entity entity,
            double x,
            double y,
            double z,
            float yaw,
            float partialTick,
            PoseStack pose,
            MultiBufferSource buffers,
            int light,
            Operation<Void> original) {
        // 冻结在最后完成的游戏刻姿态，避免 partialTick 每刻从零重播旧动画区间。
        // 只影响本次实体渲染；不改实体动画数据、全局时钟或玩家视角。
        float animationTick = TimeStopManager.frozen(entity) ? 1.0F : partialTick;
        original.call(entity, x, y, z, yaw, animationTick, pose, buffers, light);
    }
}
