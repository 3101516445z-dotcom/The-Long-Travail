package com.thelongtravail.mixin;
import com.thelongtravail.boundless.*;
import net.minecraft.world.level.block.piston.*;
import net.minecraft.world.level.Level;
import net.minecraft.core.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(PistonBaseBlock.class)
public abstract class TimeStopPistonMixin {
    @Inject(method="triggerEvent",at=@At("HEAD"),cancellable=true)
    private void travail$event(net.minecraft.world.level.block.state.BlockState state,Level l,BlockPos p,int event,int data,CallbackInfoReturnable<Boolean> ci){
        Direction face=state.getValue(PistonBaseBlock.FACING);boolean extend=event==0;
        PistonStructureResolver resolver=new PistonStructureResolver(l,p,face,extend);resolver.resolve();
        boolean blocked=TimeStopManager.frozen(l,p)||TimeStopManager.frozen(l,p.relative(face));
        Direction move=extend?face:face.getOpposite();
        for(BlockPos q:resolver.getToPush())blocked|=TimeStopManager.frozen(l,q)||TimeStopManager.frozen(l,q.relative(move));
        for(BlockPos q:resolver.getToDestroy())blocked|=TimeStopManager.frozen(l,q);
        if(blocked){if(l instanceof net.minecraft.server.level.ServerLevel server){java.util.Set<BlockPos> gates=new java.util.HashSet<>();gates.add(p);gates.add(p.relative(face));gates.addAll(resolver.getToPush());for(BlockPos q:resolver.getToPush())gates.add(q.relative(move));gates.addAll(resolver.getToDestroy());FrozenBlockEvents.get(server).hold(new net.minecraft.world.level.BlockEventData(p,state.getBlock(),event,data),gates);}ci.setReturnValue(false);}
    }
    @Inject(method="moveBlocks",at=@At("HEAD"),cancellable=true)
    private void travail$move(Level l,BlockPos p,Direction face,boolean extend,CallbackInfoReturnable<Boolean> ci){
        PistonStructureResolver resolver=new PistonStructureResolver(l,p,face,extend);resolver.resolve();
        boolean blocked=TimeStopManager.frozen(l,p)||TimeStopManager.frozen(l,p.relative(face));
        Direction move=extend?face:face.getOpposite();
        for(BlockPos q:resolver.getToPush())blocked|=TimeStopManager.frozen(l,q)||TimeStopManager.frozen(l,q.relative(move));
        for(BlockPos q:resolver.getToDestroy())blocked|=TimeStopManager.frozen(l,q);
        if(blocked){if(l instanceof net.minecraft.server.level.ServerLevel s)FrozenBlockTasks.get(s).neighbor(s,p,l.getBlockState(p).getBlock(),p.relative(face));ci.setReturnValue(false);}
    }
}
