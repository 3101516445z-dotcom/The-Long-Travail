package com.thelongtravail.mixin;

import com.thelongtravail.client.LanternLightingClient;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LevelRenderer.class)
public abstract class LanternLevelLightMixin {
    @Inject(method="getLightColor(Lnet/minecraft/world/level/BlockAndTintGetter;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/core/BlockPos;)I",at=@At("RETURN"),cancellable=true)
    private static void travail$lanternLight(BlockAndTintGetter level,BlockState state,BlockPos pos,CallbackInfoReturnable<Integer> cir) {
        if(!state.isSolidRender(level,pos))cir.setReturnValue(LanternLightingClient.light(pos.getX()+.5,pos.getY()+.5,pos.getZ()+.5,cir.getReturnValueI()));
    }
}
