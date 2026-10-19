package com.thelongtravail.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.thelongtravail.boundless.BoundlessAcquisition;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.*;
import org.spongepowered.asm.mixin.*;

@Mixin(ServerPlayerGameMode.class)
public abstract class PrinceBlockBreakMixin {
    @Shadow @Final protected ServerPlayer player;

    @WrapMethod(method = "destroyBlock")
    private boolean travail$flower(BlockPos pos, Operation<Boolean> original) {
        var state = player.level().getBlockState(pos);
        boolean result = original.call(pos);
        BoundlessAcquisition.broken(player, pos, state, result);
        return result;
    }
}
