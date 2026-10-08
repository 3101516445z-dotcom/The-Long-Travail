package com.thelongtravail.valley;

import com.thelongtravail.TheLongTravail;
import com.thelongtravail.TravailAspect;
import com.thelongtravail.config.AzraelConfig;
import com.thelongtravail.data.LongTravailData;
import com.thelongtravail.helper.TravailCurios;
import com.thelongtravail.registry.ModRegistry;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import top.theillusivec4.curios.api.CuriosApi;
import java.util.*;

@Mod.EventBusSubscriber(modid = TheLongTravail.MODID)
public final class AzraelState {
    private static final class State { boolean armed; final boolean witness; State(boolean witness) { this.witness = witness; } }
    private record Request(ServerPlayer player, UUID victim, State state, boolean self, net.minecraft.resources.ResourceKey<net.minecraft.world.level.Level> dimension) {}
    private static final Map<ServerPlayer, State> STATES = new WeakHashMap<>();
    private static final ArrayDeque<Request> REQUESTS = new ArrayDeque<>();
    public static boolean equipped(Player player) {
        if (TravailCurios.stack(player).isEmpty()) return false;
        return CuriosApi.getCuriosInventory(player).map(inv -> inv.findCurios(ModRegistry.AZRAEL.get()).stream()
                .anyMatch(r -> !r.slotContext().cosmetic() && r.slotContext().identifier().equals("head"))).orElse(false);
    }
    private static State state(ServerPlayer player) {
        if (!player.isAlive() || player.isSpectator() || !equipped(player)) { forget(player); return null; }
        boolean witness = LongTravailData.hasWitness(TravailCurios.stack(player), TravailAspect.DEEP_VALLEY);
        State state = STATES.get(player);
        if (state == null || state.witness != witness) { forget(player); state = new State(witness); STATES.put(player, state); }
        return state;
    }
    public static void received(ServerPlayer player, DamageSource source) {
        if (AzraelExecution.active() || source.getEntity() == null || source.getEntity() == player) return;
        State state = state(player);
        if (state != null && !state.witness) state.armed = true;
    }
    // 0=未触发，1=目标，2=自己。始终只消耗一个随机数。
    public static int witnessOutcome(double roll, double target, double self) {
        double sum = Math.max(1, target + self);
        return roll < target / sum ? 1 : roll < (target + self) / sum ? 2 : 0;
    }
    // 伤害确认后，即使目标已死亡，也先消耗机会并进行随机判定，再由执行队列跳过死亡目标。
    public static void damageDealt(ServerPlayer player, LivingEntity victim) {
        if (AzraelExecution.active() || player == victim
                || victim.isSpectator() || victim instanceof Player p && (!player.canHarmPlayer(p) || !player.server.isPvpAllowed())) return;
        State state = state(player);
        if (state == null) return;
        int outcome;
        if (state.witness) outcome = witnessOutcome(player.getRandom().nextDouble(), AzraelConfig.TARGET.get(), AzraelConfig.SELF.get());
        else {
            if (!state.armed) return;
            state.armed = false;
            outcome = player.getRandom().nextDouble() < AzraelConfig.MALICE.get() ? 1 : 0;
        }
        if (outcome != 0) REQUESTS.add(new Request(player, victim.getUUID(), state, outcome == 2, player.level().dimension()));
    }
    public static void forget(ServerPlayer player) {
        STATES.remove(player);
        REQUESTS.removeIf(request -> request.player == player);
    }
    public static void drain(MinecraftServer server) {
        int count = REQUESTS.size();
        while (count-- > 0 && !REQUESTS.isEmpty()) {
            Request request = REQUESTS.removeFirst();
            ServerPlayer player = request.player;
            if (player.server != server || player.hasDisconnected() || player.level().dimension() != request.dimension
                    || state(player) != request.state) continue;
            var entity = request.self ? player : player.serverLevel().getEntity(request.victim);
            if (entity instanceof LivingEntity victim && victim.isAlive() && !victim.isRemoved()) AzraelExecution.execute(player, victim);
        }
    }
    @SubscribeEvent public static void playerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase == TickEvent.Phase.END && event.player instanceof ServerPlayer player) state(player);
    }
    @SubscribeEvent public static void serverTick(TickEvent.ServerTickEvent event) {
        if (event.phase == TickEvent.Phase.END) drain(event.getServer());
    }
    @SubscribeEvent public static void logout(PlayerEvent.PlayerLoggedOutEvent e) { if (e.getEntity() instanceof ServerPlayer p) forget(p); }
    @SubscribeEvent public static void dimension(PlayerEvent.PlayerChangedDimensionEvent e) { if (e.getEntity() instanceof ServerPlayer p) forget(p); }
    @SubscribeEvent public static void clone(PlayerEvent.Clone e) { if (e.getOriginal() instanceof ServerPlayer p) forget(p); }
    @SubscribeEvent public static void stopped(ServerStoppedEvent e) { STATES.clear(); REQUESTS.clear(); AzraelExecution.clearDiagnostics(); }
    private AzraelState() {}
}
