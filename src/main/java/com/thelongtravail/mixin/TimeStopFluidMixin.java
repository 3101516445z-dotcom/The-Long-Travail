package com.thelongtravail.mixin;
import com.thelongtravail.boundless.*;
import net.minecraft.core.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;
@Mixin(FlowingFluid.class)
public abstract class TimeStopFluidMixin {
    @Inject(method="spreadTo",at=@At("HEAD"),cancellable=true)
    private void travail$spread(LevelAccessor l,BlockPos p,BlockState state,Direction direction,FluidState fluid,CallbackInfo ci){
        if(l instanceof ServerLevel s&&TimeStopManager.frozen(s,p)){FrozenBlockTasks.get(s).fluidBoundary(p.relative(direction.getOpposite()),p);ci.cancel();}
    }
    @Inject(method="canSpreadTo",at=@At("HEAD"),cancellable=true)
    private void travail$check(BlockGetter l,BlockPos from,BlockState state,Direction direction,BlockPos to,BlockState target,FluidState fluid,Fluid type,CallbackInfoReturnable<Boolean> ci){
        if(l instanceof ServerLevel s&&(TimeStopManager.frozen(s,from)||TimeStopManager.frozen(s,to))){FrozenBlockTasks.get(s).fluidBoundary(from,to);ci.setReturnValue(false);}
    }
}
