package com.thelongtravail.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.*;
import com.thelongtravail.underworld.BookRevival;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.*;
import net.minecraft.server.players.PlayerList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(PlayerList.class)
public abstract class BookPlayerListMixin {
    @WrapOperation(method="respawn",at=@At(value="INVOKE",target="Lnet/minecraft/server/level/ServerLevel;noCollision(Lnet/minecraft/world/entity/Entity;)Z"))
    private boolean travail$beforePlacement(ServerLevel level,net.minecraft.world.entity.Entity entity,Operation<Boolean> original) {
        var s=BookRevival.respawn();
        if(s==null||!entity.getUUID().equals(s.old().getUUID()))return original.call(level,entity);
        if(entity instanceof ServerPlayer p)p.setRespawnPosition(s.spawnDimension(),s.spawnPosition(),s.spawnAngle(),s.spawnForced(),false);
        entity.moveTo(s.position().x,s.position().y,s.position().z,s.old().getYRot(),s.old().getXRot());
        return true; // 搜索已检查实际方块碰撞；原版向上推挤不能覆盖最近位置。
    }
    @WrapOperation(method="respawn",at=@At(value="INVOKE",target="Lnet/minecraft/server/MinecraftServer;overworld()Lnet/minecraft/server/level/ServerLevel;"))
    private ServerLevel travail$dimension(MinecraftServer server,Operation<ServerLevel> original,ServerPlayer old,boolean keep) {
        var s=BookRevival.respawn();return s==null||s.old()!=old?original.call(server):s.level();
    }
    @WrapOperation(method="respawn",at=@At(value="INVOKE",target="Lnet/minecraft/server/level/ServerPlayer;moveTo(DDDFF)V"))
    private void travail$position(ServerPlayer p,double x,double y,double z,float yaw,float pitch,Operation<Void> original) {
        var s=BookRevival.respawn();
        if(s==null||!p.getUUID().equals(s.old().getUUID()))original.call(p,x,y,z,yaw,pitch);
        else original.call(p,s.position().x,s.position().y,s.position().z,s.old().getYRot(),s.old().getXRot());
    }
}
