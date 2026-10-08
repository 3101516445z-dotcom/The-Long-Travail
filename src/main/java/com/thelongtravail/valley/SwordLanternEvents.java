package com.thelongtravail.valley;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.*;
import net.minecraftforge.event.*;
import net.minecraftforge.event.entity.living.*;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.*;
import net.minecraftforge.eventbus.api.*;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid="the_long_travail")
public final class SwordLanternEvents {
    private static boolean immune(MobEffect e){return e==MobEffects.BLINDNESS||e==MobEffects.DARKNESS||e==MobEffects.CONFUSION;}
    @SubscribeEvent(priority=EventPriority.LOWEST) public static void effect(MobEffectEvent.Applicable e){
        if(e.getEntity() instanceof ServerPlayer p&&immune(e.getEffectInstance().getEffect())&&SwordLanternState.equipped(p))e.setResult(Event.Result.DENY);
    }
    @SubscribeEvent public static void tick(TickEvent.ServerTickEvent e){
        if(e.phase!=TickEvent.Phase.END)return;
        for(ServerPlayer p:e.getServer().getPlayerList().getPlayers()){
            SwordLanternState.tick(p);
            LanternSync.tick(p);
            if(!SwordLanternState.equipped(p))continue;
            for(MobEffect effect:new MobEffect[]{MobEffects.BLINDNESS,MobEffects.DARKNESS,MobEffects.CONFUSION})
                if(p.hasEffect(effect))com.thelongtravail.data.EffectChanges.remove(p,effect,false);
        }
        LanternLight.tick();
    }
    private static void reset(ServerPlayer p){SwordLanternState.forget(p);LanternSync.forget(p);}
    @SubscribeEvent public static void tracking(PlayerEvent.StartTracking e){if(e.getEntity() instanceof ServerPlayer viewer && e.getTarget() instanceof ServerPlayer target)LanternSync.tracking(viewer,target);}
    @SubscribeEvent public static void logout(PlayerEvent.PlayerLoggedOutEvent e){if(e.getEntity() instanceof ServerPlayer p)reset(p);}
    @SubscribeEvent public static void dimension(PlayerEvent.PlayerChangedDimensionEvent e){if(e.getEntity() instanceof ServerPlayer p)reset(p);}
    @SubscribeEvent public static void death(LivingDeathEvent e){if(e.getEntity() instanceof ServerPlayer p)reset(p);}
    @SubscribeEvent public static void stop(ServerStoppingEvent e){LanternLight.stop();LanternSync.clear();SwordLanternState.clear();}
    private SwordLanternEvents(){}
}
