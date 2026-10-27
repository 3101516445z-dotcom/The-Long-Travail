package com.thelongtravail.compat.mixin.ftbultimine;

import com.thelongtravail.compat.ftbultimine.TremorUltimine;

import net.minecraft.core.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;

import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = "dev.ftb.mods.ftbultimine.utils.forge.PlatformMethodsImpl", remap = false)
public abstract class TremorPlatformMixin {
    @Inject(method = "blockRightClick", at = @At("HEAD"), cancellable = true, remap = false)
    private static void travail$batch(
            @Coerce Object context,
            ServerPlayer player,
            InteractionHand hand,
            BlockPos pos,
            Direction face,
            @Coerce Object data,
            CallbackInfoReturnable<Integer> cir) {
        if (TremorUltimine.dispatching()) cir.setReturnValue(TremorUltimine.batch(player, data));
    }
}
