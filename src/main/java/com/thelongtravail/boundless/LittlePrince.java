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
    private static final double STEP_MARGIN = 0.2D;
    private static final double STEP_EPSILON = 1.0E-6D;

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void end(TickEvent.PlayerTickEvent e) {
        if (e.phase == TickEvent.Phase.END && e.player instanceof ServerPlayer p) refresh(p);
    }

    public static void refresh(ServerPlayer p) {
        var carrying = CarryingApi.query(p, ModRegistry.LITTLE_PRINCE.get());
        boolean active = LittlePrinceFlight.update(p, carrying);
        var attr = p.getAttribute(ForgeMod.STEP_HEIGHT_ADDITION.get());
        if (attr != null) {
            var old = attr.getModifier(STEP);
            // 计算高度时排除本模组的修饰符，避免上一刻的加成反过来影响本次判定。
            double current = old == null ? 0 : old.getAmount();
            double factor = 1, total = 1;
            for (var modifier : attr.getModifiers()) {
                if (modifier.getId().equals(STEP)) continue;
                switch (modifier.getOperation()) {
                    case MULTIPLY_BASE -> factor += modifier.getAmount();
                    case MULTIPLY_TOTAL -> total *= 1 + modifier.getAmount();
                }
            }
            double multiplier = factor * total;
            double without = p.getStepHeight() - current * multiplier;
            double target = BoundlessItemsConfig.get("prince.stepHeight");
            if (target > 0) target += STEP_MARGIN;
            double amount =
                    active && multiplier > 0 ? Math.max(0, (target - without) / multiplier) : 0;
            if (Math.abs(current - amount) < STEP_EPSILON) return;
            if (old != null) attr.removeModifier(STEP);
            if (amount > 0)
                attr.addTransientModifier(
                        new AttributeModifier(
                                STEP,
                                "Little Prince minimum step",
                                amount,
                                AttributeModifier.Operation.ADDITION));
        }
    }

    @SubscribeEvent
    public static void login(PlayerEvent.PlayerLoggedInEvent e) {
        if (e.getEntity() instanceof ServerPlayer p) LittlePrinceFlight.resync(p);
    }

    @SubscribeEvent
    public static void dimension(PlayerEvent.PlayerChangedDimensionEvent e) {
        if (e.getEntity() instanceof ServerPlayer p) LittlePrinceFlight.resync(p);
    }

    @SubscribeEvent
    public static void respawn(PlayerEvent.PlayerRespawnEvent e) {
        if (e.getEntity() instanceof ServerPlayer p) LittlePrinceFlight.resync(p);
    }

    @SubscribeEvent
    public static void clone(PlayerEvent.Clone e) {
        if (e.getOriginal() instanceof ServerPlayer p) {
            LittlePrinceFlight.forget(p);
            CarryingApi.forget(p);
        }
    }

    @SubscribeEvent
    public static void logout(PlayerEvent.PlayerLoggedOutEvent e) {
        if (e.getEntity() instanceof ServerPlayer p) {
            LittlePrinceFlight.forget(p);
            CarryingApi.forget(p);
        }
    }

    @SubscribeEvent
    public static void stopped(ServerStoppedEvent e) {
        LittlePrinceFlight.clear();
        CarryingApi.clear();
    }

    private LittlePrince() {}
}
