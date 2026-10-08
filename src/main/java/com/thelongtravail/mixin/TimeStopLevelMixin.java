package com.thelongtravail.mixin;
import com.thelongtravail.boundless.*;
import com.llamalad7.mixinextras.injector.wrapoperation.*;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.entity.TickingBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(Level.class)
public abstract class TimeStopLevelMixin {
    @WrapOperation(method="tickBlockEntities",at=@At(value="INVOKE",target="Lnet/minecraft/world/level/block/entity/TickingBlockEntity;tick()V"))
    private void travail$blockEntity(TickingBlockEntity t,Operation<Void> original){if(!TimeStopManager.frozen((Level)(Object)this,t.getPos()))original.call(t);}
    @Inject(method="setBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;II)Z",at=@At("HEAD"),cancellable=true)
    private void travail$protectBlock(BlockPos p,BlockState state,int flags,int recursion,CallbackInfoReturnable<Boolean> ci){if(TimeStopManager.frozen((Level)(Object)this,p)&&!com.thelongtravail.valley.LanternLight.permitted((Level)(Object)this,p,state))ci.setReturnValue(false);}
    @Inject(method="destroyBlock",at=@At("HEAD"),cancellable=true)
    private void travail$protectDestroy(BlockPos p,boolean drop,net.minecraft.world.entity.Entity source,int recursion,CallbackInfoReturnable<Boolean> ci){if(TimeStopManager.frozen((Level)(Object)this,p))ci.setReturnValue(false);}
}
