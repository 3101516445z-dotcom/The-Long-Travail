package com.thelongtravail.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.thelongtravail.underworld.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.damagesource.DamageSource;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ServerPlayer.class)
public abstract class BookServerPlayerMixin implements SpawnProtectionAccess {
    @Shadow private int spawnInvulnerableTime;
    @Override public void travail$protection(int ticks){spawnInvulnerableTime=ticks;}
    @WrapMethod(method="hurt")
    private boolean travail$hit(DamageSource s,float amount,Operation<Boolean> original) {
        try(var h=BookRevival.hit((ServerPlayer)(Object)this,s,amount)){return original.call(s,amount);}
    }
    @WrapMethod(method="die")
    private void travail$death(DamageSource source,Operation<Void> original) {
        ServerPlayer p=(ServerPlayer)(Object)this;
        try(var scope=BookRevival.beginDeath(p,source)){original.call(source);BookRevival.finished(p);}
        catch(RuntimeException e){throw e;}catch(Exception e){throw new IllegalStateException(e);}
    }
    // 神秘遗物的末地重生锚也读取此入口；仅此次苏生隐藏绑定位置，避免消耗锚。
    @Inject(method="getRespawnPosition",at=@At("HEAD"),cancellable=true)
    private void travail$ignoreAnchor(CallbackInfoReturnable<BlockPos> cir) {
        var scope=BookRevival.respawn();if(scope!=null&&scope.old()==(Object)this)cir.setReturnValue(null);
    }
}
