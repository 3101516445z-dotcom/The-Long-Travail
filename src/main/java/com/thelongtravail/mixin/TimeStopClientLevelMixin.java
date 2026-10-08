package com.thelongtravail.mixin;
import com.thelongtravail.boundless.TimeStopManager;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(ClientLevel.class)
public abstract class TimeStopClientLevelMixin {
    @Inject(method="doAnimateTick",at=@At(value="INVOKE",target="Lnet/minecraft/client/multiplayer/ClientLevel;getBlockState(Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/level/block/state/BlockState;",ordinal=0),cancellable=true)
    private void travail$ambient(int x,int y,int z,int range,net.minecraft.util.RandomSource random,net.minecraft.world.level.block.Block marker,net.minecraft.core.BlockPos.MutableBlockPos pos,CallbackInfo ci){
        if(TimeStopManager.frozen((ClientLevel)(Object)this,pos))ci.cancel();
    }
    @Shadow private void tickPassenger(Entity vehicle,Entity passenger){throw new AssertionError();}
    @Inject(method="tickNonPassenger",at=@At("HEAD"),cancellable=true)
    private void travail$entity(Entity e,CallbackInfo ci){if(TimeStopManager.frozen(e)){e.setOldPosAndRot();for(Entity child:e.getPassengers())tickPassenger(e,child);ci.cancel();}}
    @Inject(method="tickPassenger",at=@At("HEAD"),cancellable=true)
    private void travail$passenger(Entity vehicle,Entity e,CallbackInfo ci){if(TimeStopManager.frozen(e)){e.setOldPosAndRot();for(Entity child:e.getPassengers())tickPassenger(e,child);ci.cancel();}}
}
