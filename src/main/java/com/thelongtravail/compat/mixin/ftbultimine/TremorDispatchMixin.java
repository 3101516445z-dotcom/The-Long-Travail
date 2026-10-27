package com.thelongtravail.compat.mixin.ftbultimine;

import com.thelongtravail.compat.ftbultimine.TremorUltimine;
import com.thelongtravail.valley.HighFrequencyTremorActions;
import com.thelongtravail.valley.PlayerBlockBreaker;

import net.minecraft.core.*;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;

import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = "dev.ftb.mods.ftbultimine.FTBUltimine", remap = false)
public abstract class TremorDispatchMixin {
    @Inject(method = "blockRightClick", at = @At("HEAD"), cancellable = true, remap = false)
    private void travail$defer(
            Player player,
            InteractionHand hand,
            BlockPos pos,
            Direction face,
            CallbackInfoReturnable<Object> cir) {
        if (HighFrequencyTremorActions.eligible(player, hand) && !TremorUltimine.dispatching())
            cir.setReturnValue(TremorUltimine.pass());
    }

    @Inject(method = "blockBroken", at = @At("HEAD"), cancellable = true, remap = false)
    private void travail$noRecursion(CallbackInfoReturnable<Object> cir) {
        if (PlayerBlockBreaker.active()) cir.setReturnValue(TremorUltimine.pass());
    }
}
