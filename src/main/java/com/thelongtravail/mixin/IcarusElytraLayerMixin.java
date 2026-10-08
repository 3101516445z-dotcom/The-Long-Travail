package com.thelongtravail.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.thelongtravail.farreach.IcarusFlight;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.ElytraLayer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// 伊卡洛斯模型沿用原版动画，同时只显示一对翅膀。
@Mixin(ElytraLayer.class)
public abstract class IcarusElytraLayerMixin<T extends LivingEntity, M extends EntityModel<T>> extends RenderLayer<T, M> {
    @Unique private com.thelongtravail.client.IcarusWingModel<T> travail$wingModel;
    @Unique private static final ResourceLocation TRAVAIL_ICARUS = new ResourceLocation("the_long_travail", "textures/entity/icarus_elytra.png");
    protected IcarusElytraLayerMixin(RenderLayerParent<T, M> parent) { super(parent); }
    @Inject(method = "render(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;ILnet/minecraft/world/entity/LivingEntity;FFFFFF)V", at = @At("HEAD"), cancellable = true)
    private void travail$icarusWings(PoseStack pose, MultiBufferSource buffers, int light, T entity,
            float limbSwing, float limbAmount, float partial, float age, float yaw, float pitch, CallbackInfo ci) {
        var stack = IcarusFlight.visibleStack(entity);
        if (stack.isEmpty()) return;
        if (travail$wingModel == null) travail$wingModel = new com.thelongtravail.client.IcarusWingModel<>(TRAVAIL_ICARUS);
        pose.pushPose();
        pose.translate(0, 0, 0.125F);
        getParentModel().copyPropertiesTo(travail$wingModel);
        travail$wingModel.setupAnim(entity, limbSwing, limbAmount, age, yaw, pitch);
        var consumer = ItemRenderer.getArmorFoilBuffer(buffers, RenderType.armorCutoutNoCull(TRAVAIL_ICARUS), false, stack.hasFoil());
        travail$wingModel.renderToBuffer(pose, consumer, light, OverlayTexture.NO_OVERLAY, 1, 1, 1, 1);
        pose.popPose();
        ci.cancel();
    }
}
