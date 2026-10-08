package com.thelongtravail.abyss;

import com.thelongtravail.TheLongTravail;
import com.thelongtravail.config.RainConfig;
import com.thelongtravail.network.*;
import net.minecraft.network.protocol.game.ClientboundGameEventPacket;
import net.minecraft.server.level.*;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import java.util.*;

@Mod.EventBusSubscriber(modid=TheLongTravail.MODID)
public final class RainEvents {
    private static final Map<ServerPlayer, Progress> PLAYERS = new WeakHashMap<>();
    private static final class Progress { int heal, food; long castTick = Long.MIN_VALUE, configRevision = -1; RainPacket last; }
    @SubscribeEvent public static void tick(TickEvent.PlayerTickEvent e) {
        if (e.phase!=TickEvent.Phase.END || !(e.player instanceof ServerPlayer p)) return;
        int cooldown=RainState.cooldown(p);
        if(cooldown>0) p.getPersistentData().putInt(RainState.COOLDOWN_KEY,cooldown-1);
        Progress progress=PLAYERS.computeIfAbsent(p, unused->new Progress());
        boolean eligible = p.isAlive() && !p.isSpectator();
        boolean active = eligible && RainState.passive(p);
        Boolean observedPassive = eligible ? active : null;
        if(active) {
            if(++progress.heal >= RainConfig.ticks(RainConfig.HEAL_SECONDS.get())) {
                progress.heal=0;
                if(RainConfig.HEAL.get()>0 && p.getHealth()<p.getMaxHealth()) {
                    p.heal(RainConfig.HEAL.get().floatValue());
                    observedPassive = null; // 回血回调可能换装备、改天气，不能跨回调复用。
                }
            }
            if(++progress.food >= RainConfig.ticks(RainConfig.FOOD_SECONDS.get())) {
                progress.food=0; recoverFood(p); observedPassive = null;
            }
        } else { progress.heal=progress.food=0; }
        sync(p, progress, false, observedPassive);
    }
    public static void recoverFood(ServerPlayer p) {
        var food=p.getFoodData(); int amount=RainConfig.FOOD.get(); float saturation=RainConfig.SATURATION.get().floatValue();
        if(amount>0) food.eat(amount, saturation/(2*amount));
        else if(saturation>0) food.setSaturation(Math.min(food.getFoodLevel(),food.getSaturationLevel()+saturation));
    }
    public static void cast(ServerPlayer p) {
        if(!RainConfig.SKILL_ENABLED.get()) return;
        if(!p.isAlive() || p.isSpectator() || p.containerMenu!=p.inventoryMenu || !RainState.equipped(p)) return;
        Progress progress=PLAYERS.computeIfAbsent(p, unused->new Progress());
        long tick=p.serverLevel().getGameTime();
        if(progress.castTick==tick) return; // 即使冷却为零，每刻也只接受一次请求。
        progress.castTick=tick;
        if(!p.level().dimension().equals(Level.OVERWORLD)) { message(p,"dimension"); return; }
        if(RainState.cooldown(p)>0) { message(p,"cooldown"); return; }
        var weather=RainWeather.get(p.serverLevel());
        if(weather.active() && RainConfig.REPEAT.get()==RainConfig.Repeat.REJECT) { message(p,"ongoing"); return; }
        boolean natural=weather.naturalRain(p.serverLevel());
        weather.start(p.serverLevel(), RainConfig.ticks(RainConfig.DURATION.get()));
        p.getPersistentData().putInt(RainState.COOLDOWN_KEY,RainConfig.ticks(RainConfig.COOLDOWN.get()));
        message(p,natural?"natural":"success");
    }
    private static void message(ServerPlayer p,String key) { TravailNetwork.sendRainMessage(p,new RainMessagePacket("message.the_long_travail.rain."+key)); }
    private static void sync(ServerPlayer p, Progress s, boolean force) {
        sync(p, s, force, null);
    }
    private static void sync(ServerPlayer p, Progress s, boolean force, Boolean observedPassive) {
        var config = RainTooltips.serverSnapshot(p.server.getTickCount());
        boolean weather = RainWeather.active(p.level()), dry = RainConfig.DRY_RAIN.get(), snow = RainConfig.SNOW_RAIN.get();
        float speed = RainState.speed(p);
        boolean fire = RainConfig.FIRE.get() && (observedPassive != null ? observedPassive : RainState.passive(p));
        var last = s.last;
        if (!force && last != null && s.configRevision == config.revision() && last.playerId() == p.getId()
                && last.weather() == weather && last.dry() == dry && last.snow() == snow
                && Float.compare(last.speed(), speed) == 0 && last.fire() == fire) return;
        RainPacket packet = new RainPacket(p.getId(), weather, dry, snow, speed, fire, config.eki(), config.tokaido(), config.slots());
        s.last = packet; s.configRevision = config.revision(); TravailNetwork.sendRain(p, packet);
    }
    // 由配置重载入口转交到服务端线程。
    public static void configReloaded(net.minecraft.server.MinecraftServer server) {
        RainTooltips.invalidateServer();
        for (ServerPlayer p : server.getPlayerList().getPlayers()) sync(p, PLAYERS.computeIfAbsent(p, unused -> new Progress()), true);
    }
    public static void syncWorld(ServerLevel level) {
        for(ServerPlayer p:level.players()) {
            // 设置天气标志后恢复当前强度，避免仅发送开始或停止事件导致客户端强度突变。
            p.connection.send(new ClientboundGameEventPacket(level.getLevelData().isRaining()?ClientboundGameEventPacket.START_RAINING:ClientboundGameEventPacket.STOP_RAINING,0));
            p.connection.send(new ClientboundGameEventPacket(ClientboundGameEventPacket.RAIN_LEVEL_CHANGE,level.getRainLevel(1)));
            p.connection.send(new ClientboundGameEventPacket(ClientboundGameEventPacket.THUNDER_LEVEL_CHANGE,RainWeather.rawThunderLevel(level)));
            sync(p,PLAYERS.computeIfAbsent(p,unused->new Progress()),true);
        }
    }
    @SubscribeEvent public static void login(PlayerEvent.PlayerLoggedInEvent e) { if(e.getEntity() instanceof ServerPlayer p) sync(p,PLAYERS.computeIfAbsent(p,u->new Progress()),true); }
    @SubscribeEvent public static void dimension(PlayerEvent.PlayerChangedDimensionEvent e) { if(e.getEntity() instanceof ServerPlayer p) { RainState.forget(p); sync(p,PLAYERS.computeIfAbsent(p,u->new Progress()),true); } }
    @SubscribeEvent public static void clone(PlayerEvent.Clone e) { e.getEntity().getPersistentData().putInt(RainState.COOLDOWN_KEY,RainState.cooldown(e.getOriginal())); }
    @SubscribeEvent public static void logout(PlayerEvent.PlayerLoggedOutEvent e) { if(e.getEntity() instanceof ServerPlayer p) { PLAYERS.remove(p); RainState.forget(p); } }
    @SubscribeEvent public static void stop(ServerStoppedEvent e) { PLAYERS.clear(); RainState.clear(); RainTooltips.invalidateServer(); }
    private RainEvents() {}
}
