package com.thelongtravail.mixin;
import com.thelongtravail.boundless.*;
import com.llamalad7.mixinextras.injector.wrapoperation.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.*;
import net.minecraft.world.level.*;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(ServerLevel.class)
public abstract class TimeStopWorldMixin {
    @Inject(method="<init>",at=@At("RETURN"))
    private void travail$bindSchedulers(CallbackInfo ci){
        ServerLevel level=(ServerLevel)(Object)this;
        ((ScheduledTickOwner)level.getBlockTicks()).travail$bindTickWorld(level);
        ((ScheduledTickOwner)level.getFluidTicks()).travail$bindTickWorld(level);
    }
    @Shadow private void tickPassenger(Entity vehicle,Entity passenger){throw new AssertionError();}
    @Inject(method="tickNonPassenger",at=@At("HEAD"),cancellable=true)
    private void travail$freezeEntity(Entity e,CallbackInfo ci){if(TimeStopManager.frozen(e)){FrozenEntityClock.tick(e);e.setOldPosAndRot();for(Entity child:e.getPassengers())tickPassenger(e,child);ci.cancel();}else FrozenEntityClock.normal(e);}
    @Inject(method="tickPassenger",at=@At("HEAD"),cancellable=true)
    private void travail$freezePassenger(Entity vehicle,Entity e,CallbackInfo ci){if(TimeStopManager.frozen(e)){FrozenEntityClock.tick(e);e.setOldPosAndRot();for(Entity child:e.getPassengers())tickPassenger(e,child);ci.cancel();}else FrozenEntityClock.normal(e);}
    @WrapOperation(method="tickChunk",at=@At(value="INVOKE",target="Lnet/minecraft/world/level/block/state/BlockState;randomTick(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/core/BlockPos;Lnet/minecraft/util/RandomSource;)V"))
    private void travail$randomBlock(net.minecraft.world.level.block.state.BlockState s,ServerLevel l,net.minecraft.core.BlockPos p,net.minecraft.util.RandomSource r,Operation<Void> original){if(!TimeStopManager.frozen(l,p))original.call(s,l,p,r);}
    @WrapOperation(method="tickChunk",at=@At(value="INVOKE",target="Lnet/minecraft/world/level/material/FluidState;randomTick(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/util/RandomSource;)V"))
    private void travail$randomFluid(net.minecraft.world.level.material.FluidState s,Level l,net.minecraft.core.BlockPos p,net.minecraft.util.RandomSource r,Operation<Void> original){if(!TimeStopManager.frozen(l,p))original.call(s,l,p,r);}
    @Inject(method="doBlockEvent",at=@At("HEAD"),cancellable=true)
    private void travail$blockEvent(net.minecraft.world.level.BlockEventData e,org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable<Boolean> ci){
        ServerLevel l=(ServerLevel)(Object)this;if(TimeStopManager.frozen(l,e.pos())){FrozenBlockEvents.get(l).hold(e);ci.setReturnValue(false);}
    }
}
