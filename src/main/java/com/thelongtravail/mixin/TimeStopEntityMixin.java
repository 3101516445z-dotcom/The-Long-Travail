package com.thelongtravail.mixin;
import com.thelongtravail.boundless.TimeStopManager;
import net.minecraft.world.entity.*;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(Entity.class)
public abstract class TimeStopEntityMixin implements com.thelongtravail.boundless.FrozenEntityClock.Clock {
    @org.spongepowered.asm.mixin.Unique private long travail$clockTick;
    @org.spongepowered.asm.mixin.Unique private boolean travail$clockSet;
    @Override public boolean travail$markClock(long tick){
        if(travail$clockSet&&travail$clockTick==tick)return false;
        travail$clockSet=true;travail$clockTick=tick;return true;
    }

    @org.spongepowered.asm.mixin.Unique private Vec3 travail$entryVelocity;
    @org.spongepowered.asm.mixin.Unique private long travail$entryTick;
    @com.llamalad7.mixinextras.injector.ModifyReturnValue(method="getDeltaMovement",at=@At("RETURN"))
    private Vec3 travail$projectileTrajectory(Vec3 velocity){
        Entity e=(Entity)(Object)this;
        if(!(e instanceof net.minecraft.world.entity.projectile.Projectile))return velocity;
        if(TimeStopManager.frozen(e)){travail$entryVelocity=null;return velocity;}
        Vec3 allowed=TimeStopManager.clip(e,velocity);
        if(!allowed.equals(velocity)){travail$entryVelocity=velocity;travail$entryTick=e.level().getGameTime();}
        else travail$entryVelocity=null;
        return allowed;
    }
    @Inject(method="setDeltaMovement(Lnet/minecraft/world/phys/Vec3;)V",at=@At("HEAD"),cancellable=true)
    private void travail$preserveProjectile(Vec3 velocity,CallbackInfo ci){
        if(travail$entryVelocity!=null&&travail$entryTick==((Entity)(Object)this).level().getGameTime())ci.cancel();
    }
    @org.spongepowered.asm.mixin.Unique private boolean travail$positionGuard;
    @Inject(method="setPos(DDD)V",at=@At("HEAD"),cancellable=true)
    private void travail$position(double x,double y,double z,CallbackInfo ci){
        Entity e=(Entity)(Object)this;
        if(travail$positionGuard||!e.isAddedToWorld())return;
        if(!e.level().isClientSide&&!TimeStopManager.active(e.level()))return;
        Vec3 delta=new Vec3(x,y,z).subtract(e.position());
        Vec3 allowed=TimeStopManager.clip(e,delta);
        if(allowed.equals(delta))return;
        travail$positionGuard=true;
        try{Vec3 p=e.position().add(allowed);e.setPos(p.x,p.y,p.z);}finally{travail$positionGuard=false;}
        ci.cancel();
    }
    @Inject(method="turn",at=@At("HEAD"),cancellable=true)
    private void travail$rotation(double yaw,double pitch,CallbackInfo ci){if(TimeStopManager.frozen((Entity)(Object)this))ci.cancel();}
    @Inject(method="move",at=@At("HEAD"),cancellable=true)
    private void travail$freezeMove(MoverType type,Vec3 delta,CallbackInfo ci){if(TimeStopManager.frozen((Entity)(Object)this))ci.cancel();}
    @com.llamalad7.mixinextras.injector.ModifyReturnValue(method="collide",at=@At("RETURN"))
    private Vec3 travail$clipEntry(Vec3 delta){return TimeStopManager.clip((Entity)(Object)this,delta);}
    @Inject(method="push(Lnet/minecraft/world/entity/Entity;)V",at=@At("HEAD"),cancellable=true)
    private void travail$freezePush(Entity other,CallbackInfo ci){if(TimeStopManager.frozen((Entity)(Object)this)||TimeStopManager.frozen(other))ci.cancel();}
}
