package com.thelongtravail.mixin;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.thelongtravail.farreach.GoldenAgeActions;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
@Mixin(ServerPlayer.class)
public abstract class GoldenAgePlayerDropMixin {
    @WrapMethod(method="drop(Z)Z")
    private boolean travail$manualDrop(boolean all,Operation<Boolean> original){try(var scope=GoldenAgeActions.manual((ServerPlayer)(Object)this)){return original.call(all);}}
}
