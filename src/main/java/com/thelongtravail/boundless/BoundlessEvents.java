package com.thelongtravail.boundless;
import com.thelongtravail.TheLongTravail;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.*;
import net.minecraftforge.event.server.*;
import net.minecraftforge.eventbus.api.*;
import net.minecraftforge.fml.common.Mod;
@Mod.EventBusSubscriber(modid=TheLongTravail.MODID)
public final class BoundlessEvents {
    @SubscribeEvent public static void tick(TickEvent.ServerTickEvent e){
        for(var l:e.getServer().getAllLevels()){TimeStopManager.get(l).tick(e.phase==TickEvent.Phase.END);FrozenBlockEvents.get(l).tick(l);}
        if(e.phase==TickEvent.Phase.START)for(ServerPlayer p:e.getServer().getPlayerList().getPlayers()){
            if(!TimeStopManager.frozen(p)){StarVoiceState.tick(p);DreamState.tick(p);}
            if(e.getServer().getTickCount()%20==0)com.thelongtravail.network.BoundlessPacket.sendDream(p,DreamState.active(p));
            if(p.containerMenu!=p.inventoryMenu&&FrozenContainers.frozen(p.containerMenu))p.closeContainer();
        }
    }
    @SubscribeEvent(priority=EventPriority.HIGHEST) public static void interact(PlayerInteractEvent e){
        boolean block=e instanceof PlayerInteractEvent.LeftClickBlock||e instanceof PlayerInteractEvent.RightClickBlock;
        if(e.isCancelable()&&(TimeStopManager.frozen(e.getEntity())||block&&TimeStopManager.frozen(e.getLevel(),e.getPos())))e.setCanceled(true);
    }
    @SubscribeEvent public static void spawned(net.minecraftforge.event.entity.EntityJoinLevelEvent e){
        if(!(e.getLevel() instanceof net.minecraft.server.level.ServerLevel level)||!TimeStopManager.active(level))return;
        var entity=e.getEntity();
        if(entity instanceof net.minecraft.world.entity.projectile.Projectile
                ||entity instanceof net.minecraft.world.entity.OwnableEntity
                ||entity instanceof net.minecraft.world.entity.animal.allay.Allay
                ||entity instanceof net.minecraft.world.entity.projectile.EvokerFangs
                ||entity instanceof net.minecraft.world.entity.LightningBolt
                ||entity instanceof net.minecraft.world.entity.player.Player
                ||TimeStopManager.fields(level).stream().anyMatch(f->f.allies.contains(entity.getUUID())))
            TimeStopManager.get(level).dirty=true;
    }
    @SubscribeEvent public static void login(PlayerEvent.PlayerLoggedInEvent e){if(e.getEntity() instanceof ServerPlayer p){com.thelongtravail.network.TimeStopSync.forget(p);TimeStopManager.login(p);DreamState.finish(p);com.thelongtravail.network.TimeStopPacket.send(p.serverLevel());}}
    @SubscribeEvent public static void respawn(PlayerEvent.PlayerRespawnEvent e){if(e.getEntity() instanceof ServerPlayer p)com.thelongtravail.network.BoundlessPacket.sendDream(p,DreamState.active(p));}
    @SubscribeEvent public static void logout(PlayerEvent.PlayerLoggedOutEvent e){if(e.getEntity() instanceof ServerPlayer p){com.thelongtravail.network.TimeStopSync.forget(p);DreamState.finish(p);}}
    @SubscribeEvent public static void dimension(PlayerEvent.PlayerChangedDimensionEvent e){if(e.getEntity() instanceof ServerPlayer p){com.thelongtravail.network.TimeStopSync.forget(p);DreamState.finish(p,p.isAlive());com.thelongtravail.network.TimeStopPacket.send(p.serverLevel());}}
    @SubscribeEvent public static void clone(PlayerEvent.Clone e){for(String key:new String[]{DreamState.COOLDOWN,StarVoiceState.COOLDOWN,StarVoiceState.INTERVAL})e.getEntity().getPersistentData().putInt(key,e.getOriginal().getPersistentData().getInt(key));}
    @SubscribeEvent public static void stopping(ServerStoppingEvent e){for(ServerPlayer p:e.getServer().getPlayerList().getPlayers())DreamState.finish(p);TimeStopManager.stopping(e.getServer());for(var l:e.getServer().getAllLevels())FrozenBlockEvents.get(l).tick(l);}
    @SubscribeEvent public static void stopped(ServerStoppedEvent e){TimeStopManager.clear();FrozenEntityClock.clear();}
    private BoundlessEvents(){}
}
