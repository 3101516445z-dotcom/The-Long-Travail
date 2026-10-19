package com.thelongtravail.boundless;

import com.thelongtravail.TheLongTravail;
import com.thelongtravail.api.CarryingApi;
import com.thelongtravail.config.BoundlessItemsConfig;
import com.thelongtravail.registry.ModRegistry;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraftforge.common.ForgeMod;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.*;
import net.minecraftforge.fml.common.Mod;
import java.util.*;

@Mod.EventBusSubscriber(modid = TheLongTravail.MODID)
public final class LittlePrince {
    private static final UUID STEP = UUID.fromString("1c32e413-3b0f-41c0-9edf-69b584d01f01");
    private static final Map<ServerPlayer, Boolean> GRANTS = new WeakHashMap<>();
    private static final Map<ServerPlayer, Boolean> PREVIOUS = new WeakHashMap<>();
    private static final String OWNED = "TravailPrinceFlightOwned";

    // 先撤销本模组授予的飞行能力，再让其他游戏刻监听器判断各自的飞行来源。
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void start(TickEvent.PlayerTickEvent e) {
        if (e.phase != TickEvent.Phase.START || !(e.player instanceof ServerPlayer p)) return;
        PREVIOUS.put(p, p.getAbilities().mayfly);
        if (Boolean.TRUE.equals(GRANTS.remove(p)) && !p.isCreative() && !p.isSpectator())
            p.getAbilities().mayfly = false;
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void end(TickEvent.PlayerTickEvent e) {
        if (e.phase == TickEvent.Phase.END && e.player instanceof ServerPlayer p) refresh(p);
    }

    public static void refresh(ServerPlayer p) {
        var carrying = CarryingApi.query(p, ModRegistry.LITTLE_PRINCE.get());
        boolean active =
                carrying == CarryingApi.CarryingResult.PRESENT
                        || carrying == CarryingApi.CarryingResult.UNKNOWN && p.getPersistentData().getBoolean(OWNED);
        boolean previous = PREVIOUS.getOrDefault(p, p.getAbilities().mayfly);
        boolean own = active && !p.getAbilities().mayfly;
        if (active) p.getAbilities().mayfly = true;
        GRANTS.put(p, own);
        p.getPersistentData().putBoolean(OWNED, own);
        boolean stop = !p.getAbilities().mayfly && p.getAbilities().flying;
        if (stop) p.getAbilities().flying = false;
        if (previous != p.getAbilities().mayfly || stop) p.onUpdateAbilities();
        PREVIOUS.remove(p);
        var attr = p.getAttribute(ForgeMod.STEP_HEIGHT_ADDITION.get());
        if (attr != null) {
            var old = attr.getModifier(STEP);
            // 计算高度时排除本模组的修饰符，避免上一刻的加成反过来影响本次判定。
            double current = old == null ? 0 : old.getAmount();
            double base = attr.getBaseValue(), factor = 1, total = 1;
            for (var modifier : attr.getModifiers()) {
                if (modifier.getId().equals(STEP)) continue;
                switch (modifier.getOperation()) {
                    case ADDITION -> base += modifier.getAmount();
                    case MULTIPLY_BASE -> factor += modifier.getAmount();
                    case MULTIPLY_TOTAL -> total *= 1 + modifier.getAmount();
                }
            }
            double multiplier = factor * total;
            // 服务端玩家的基础跨步高度为 1.0，客户端移动使用 0.6；同步补充值须以客户端为基准。
            double without = .6D + base * multiplier;
            double amount =
                    active && multiplier > 0
                            ? Math.max(0, (BoundlessItemsConfig.get("prince.stepHeight") - without) / multiplier)
                            : 0;
            if (current == amount) return;
            if (old != null) attr.removeModifier(STEP);
            if (amount > 0)
                attr.addTransientModifier(
                        new AttributeModifier(
                                STEP, "Little Prince minimum step", amount, AttributeModifier.Operation.ADDITION));
        }
    }

    @SubscribeEvent
    public static void login(PlayerEvent.PlayerLoggedInEvent e) {
        if (e.getEntity() instanceof ServerPlayer p && p.getPersistentData().getBoolean(OWNED)) {
            if (!p.isCreative() && !p.isSpectator()) {
                p.getAbilities().mayfly = false;
                p.getAbilities().flying = false;
                p.onUpdateAbilities();
            }
            p.getPersistentData().remove(OWNED);
        }
    }

    @SubscribeEvent
    public static void logout(PlayerEvent.PlayerLoggedOutEvent e) {
        if (e.getEntity() instanceof ServerPlayer p) {
            GRANTS.remove(p);
            PREVIOUS.remove(p);
            CarryingApi.forget(p);
        }
    }

    @SubscribeEvent
    public static void stopped(ServerStoppedEvent e) {
        GRANTS.clear();
        PREVIOUS.clear();
        CarryingApi.clear();
    }

    private LittlePrince() {}
}
