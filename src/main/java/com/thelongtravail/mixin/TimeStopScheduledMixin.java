package com.thelongtravail.mixin;
import com.thelongtravail.boundless.*;
import net.minecraft.world.ticks.*;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;
@Mixin(LevelTicks.class)
public abstract class TimeStopScheduledMixin<T> implements ScheduledTickOwner {
    @org.spongepowered.asm.mixin.Unique private ServerLevel travail$world;
    @Override public void travail$bindTickWorld(ServerLevel level){travail$world=level;}
    @com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation(method="runCollectedTicks",at=@At(value="INVOKE",target="Ljava/util/function/BiConsumer;accept(Ljava/lang/Object;Ljava/lang/Object;)V"))
    private void travail$execute(java.util.function.BiConsumer<?,?> callback,Object pos,Object type,com.llamalad7.mixinextras.injector.wrapoperation.Operation<Void> original,
            @com.llamalad7.mixinextras.sugar.Local ScheduledTick<T> task){
        ServerLevel l=TimeStopManager.active(travail$world)?travail$level():null;
        if(l==null||!FrozenBlockTasks.get(l).hold(l,task,l.getFluidTicks()==(Object)this))original.call(callback,pos,type);
    }
    private ServerLevel travail$level(){return travail$world;}
    @Inject(method={"schedule","scheduleForThisTick"},at=@At("HEAD"),cancellable=true)
    private void travail$hold(ScheduledTick<T> t,CallbackInfo ci){if(!TimeStopManager.active(travail$world))return;ServerLevel l=travail$level();if(l!=null&&FrozenBlockTasks.get(l).hold(l,t,l.getFluidTicks()==(Object)this))ci.cancel();}
    @Inject(method="hasScheduledTick",at=@At("RETURN"),cancellable=true)
    private void travail$pending(BlockPos p,T type,CallbackInfoReturnable<Boolean> ci){if(ci.getReturnValue())return;ServerLevel l=travail$level();if(l!=null&&FrozenBlockTasks.get(l).held(p,type,l.getFluidTicks()==(Object)this))ci.setReturnValue(true);}
}
