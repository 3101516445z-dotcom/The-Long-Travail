package com.thelongtravail.mixin;
import com.thelongtravail.boundless.*;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.redstone.CollectingNeighborUpdater;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(CollectingNeighborUpdater.class)
public abstract class TimeStopNeighborMixin {
    @Shadow @Final private Level level;
    @Inject(method="neighborChanged(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/Block;Lnet/minecraft/core/BlockPos;)V",at=@At("HEAD"),cancellable=true)
    private void travail$neighbor(BlockPos p,Block block,BlockPos from,CallbackInfo ci){if(level instanceof ServerLevel s&&TimeStopManager.frozen(level,p)){FrozenBlockTasks.get(s).neighbor(s,p,block,from);ci.cancel();}}
    @Inject(method="neighborChanged(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/Block;Lnet/minecraft/core/BlockPos;Z)V",at=@At("HEAD"),cancellable=true)
    private void travail$neighborState(BlockState state,BlockPos p,Block block,BlockPos from,boolean moving,CallbackInfo ci){travail$neighbor(p,block,from,ci);}
    @Inject(method="shapeUpdate",at=@At("HEAD"),cancellable=true)
    private void travail$shape(net.minecraft.core.Direction face,BlockState state,BlockPos p,BlockPos from,int flags,int recursion,CallbackInfo ci){if(level instanceof ServerLevel s&&TimeStopManager.frozen(level,p)){FrozenBlockTasks.get(s).shape(face,p,from,flags,recursion);ci.cancel();}}
}
