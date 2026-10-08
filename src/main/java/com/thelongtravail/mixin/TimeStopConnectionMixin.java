package com.thelongtravail.mixin;
import com.thelongtravail.boundless.TimeStopManager;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.protocol.game.*;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(ServerGamePacketListenerImpl.class)
public abstract class TimeStopConnectionMixin {
    @Shadow public ServerPlayer player;
    @Shadow private int aboveGroundTickCount;
    @Shadow private int aboveGroundVehicleTickCount;
    @Shadow private boolean clientIsFloating;
    @Shadow private boolean clientVehicleIsFloating;
    @ModifyVariable(method="handleMovePlayer",at=@At("HEAD"),argsOnly=true)
    private ServerboundMovePlayerPacket travail$entryPacket(ServerboundMovePlayerPacket packet){
        net.minecraft.network.protocol.PacketUtils.ensureRunningOnSameThread(packet,(ServerGamePacketListenerImpl)(Object)this,player.serverLevel());
        if(!packet.hasPosition()||TimeStopManager.frozen(player))return packet;
        var desired=new net.minecraft.world.phys.Vec3(packet.getX(player.getX()),packet.getY(player.getY()),packet.getZ(player.getZ()));
        var delta=desired.subtract(player.position());var allowed=TimeStopManager.clip(player,delta);
        if(allowed.equals(delta))return packet;
        var pos=player.position().add(allowed);
        return new ServerboundMovePlayerPacket.PosRot(pos.x,pos.y,pos.z,packet.getYRot(player.getYRot()),packet.getXRot(player.getXRot()),packet.isOnGround());
    }
    @Inject(method="tick",at=@At("HEAD"))
    private void travail$keepConnection(CallbackInfo ci){if(TimeStopManager.frozen(player)||TimeStopManager.frozen(player.getRootVehicle())){aboveGroundTickCount=0;aboveGroundVehicleTickCount=0;clientIsFloating=false;clientVehicleIsFloating=false;}}
    @Inject(method="handleMovePlayer",at=@At("HEAD"),cancellable=true)
    private void travail$move(ServerboundMovePlayerPacket p,CallbackInfo ci){net.minecraft.network.protocol.PacketUtils.ensureRunningOnSameThread(p,(ServerGamePacketListenerImpl)(Object)this,player.serverLevel());if(TimeStopManager.frozen(player))ci.cancel();}
    @Inject(method="handleMoveVehicle",at=@At("HEAD"),cancellable=true)
    private void travail$vehicle(ServerboundMoveVehiclePacket p,CallbackInfo ci){net.minecraft.network.protocol.PacketUtils.ensureRunningOnSameThread(p,(ServerGamePacketListenerImpl)(Object)this,player.serverLevel());if(TimeStopManager.frozen(player)||player.getVehicle()!=null&&TimeStopManager.frozen(player.getVehicle()))ci.cancel();}
    @Inject(method="handleUseItem",at=@At("HEAD"),cancellable=true)
    private void travail$use(ServerboundUseItemPacket p,CallbackInfo ci){net.minecraft.network.protocol.PacketUtils.ensureRunningOnSameThread(p,(ServerGamePacketListenerImpl)(Object)this,player.serverLevel());if(TimeStopManager.frozen(player))ci.cancel();}
    @Inject(method="handleUseItemOn",at=@At("HEAD"),cancellable=true)
    private void travail$block(ServerboundUseItemOnPacket p,CallbackInfo ci){net.minecraft.network.protocol.PacketUtils.ensureRunningOnSameThread(p,(ServerGamePacketListenerImpl)(Object)this,player.serverLevel());if(TimeStopManager.frozen(player)||TimeStopManager.frozen(player.level(),p.getHitResult().getBlockPos())||TimeStopManager.frozen(player.level(),p.getHitResult().getBlockPos().relative(p.getHitResult().getDirection()))){player.connection.send(new ClientboundBlockUpdatePacket(player.level(),p.getHitResult().getBlockPos()));ci.cancel();}}
    @Inject(method="handlePlayerAction",at=@At("HEAD"),cancellable=true)
    private void travail$action(ServerboundPlayerActionPacket p,CallbackInfo ci){net.minecraft.network.protocol.PacketUtils.ensureRunningOnSameThread(p,(ServerGamePacketListenerImpl)(Object)this,player.serverLevel());if(TimeStopManager.frozen(player)||TimeStopManager.frozen(player.level(),p.getPos())){player.connection.send(new ClientboundBlockUpdatePacket(player.level(),p.getPos()));ci.cancel();}}
    @Inject(method="handleInteract",at=@At("HEAD"),cancellable=true)
    private void travail$interact(ServerboundInteractPacket p,CallbackInfo ci){
        net.minecraft.network.protocol.PacketUtils.ensureRunningOnSameThread(p,(ServerGamePacketListenerImpl)(Object)this,player.serverLevel());
        if(TimeStopManager.frozen(player)){ci.cancel();return;}
        var target=p.getTarget(player.serverLevel());if(target==null||!TimeStopManager.frozen(target))return;
        boolean[] attack={false};p.dispatch(new ServerboundInteractPacket.Handler(){
            public void onInteraction(net.minecraft.world.InteractionHand hand){}
            public void onInteraction(net.minecraft.world.InteractionHand hand,net.minecraft.world.phys.Vec3 pos){}
            public void onAttack(){attack[0]=true;}
        });
        if(!attack[0]){player.containerMenu.sendAllDataToRemote();ci.cancel();}
    }
    @Inject(method="handleContainerClick",at=@At("HEAD"),cancellable=true)
    private void travail$container(ServerboundContainerClickPacket p,CallbackInfo ci){net.minecraft.network.protocol.PacketUtils.ensureRunningOnSameThread(p,(ServerGamePacketListenerImpl)(Object)this,player.serverLevel());if(TimeStopManager.frozen(player)||player.containerMenu!=player.inventoryMenu&&com.thelongtravail.boundless.FrozenContainers.frozen(player.containerMenu)){player.containerMenu.sendAllDataToRemote();ci.cancel();}}
    @Inject(method="handleContainerButtonClick",at=@At("HEAD"),cancellable=true)
    private void travail$button(ServerboundContainerButtonClickPacket p,CallbackInfo ci){net.minecraft.network.protocol.PacketUtils.ensureRunningOnSameThread(p,(ServerGamePacketListenerImpl)(Object)this,player.serverLevel());if(TimeStopManager.frozen(player)||player.containerMenu!=player.inventoryMenu&&com.thelongtravail.boundless.FrozenContainers.frozen(player.containerMenu))ci.cancel();}
    @Inject(method="handlePlayerCommand",at=@At("HEAD"),cancellable=true)
    private void travail$command(ServerboundPlayerCommandPacket p,CallbackInfo ci){net.minecraft.network.protocol.PacketUtils.ensureRunningOnSameThread(p,(ServerGamePacketListenerImpl)(Object)this,player.serverLevel());if(TimeStopManager.frozen(player))ci.cancel();}
}
