package com.thelongtravail.compat.maid;

import com.thelongtravail.registry.ModRegistry;
import net.minecraft.world.entity.*;
import net.minecraft.world.item.*;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.server.MinecraftServer;
import java.util.*;

// 未安装车万女仆时，通用代码不会加载女仆接口类。
public final class MaidAccessories {
    // 运行时注册与客户端说明共用此列表，此处不引用女仆模组的类。
    public static List<Item> items() {
        return List.of(
                ModRegistry.SPRING_GAME.get(),
                ModRegistry.SWORD_AND_LANTERN.get(),
                ModRegistry.AZRAEL.get(),
                ModRegistry.THOUSAND_YEARS.get(),
                ModRegistry.STAR_VOICE.get(),
                ModRegistry.ICARUS.get(),
                ModRegistry.TOKAIDO.get(),
                ModRegistry.EKI.get());
    }

    private static boolean loaded() {
        return net.minecraftforge.fml.ModList.get().isLoaded("touhou_little_maid");
    }

    public static boolean isMaid(Entity e) {
        return loaded() && MaidAccessoryRuntime.isMaid(e);
    }

    public static void init() {
        if (loaded()) MaidAccessoryRuntime.init();
    }

    public static double bonus(LivingEntity victim, DamageSource source) {
        return loaded() ? MaidAccessoryRuntime.bonus(victim, source) : 0;
    }

    public static double witherBonus(LivingEntity victim, DamageSource source) {
        return loaded() ? MaidAccessoryRuntime.witherBonus(victim, source) : 0;
    }

    public static boolean immune(LivingEntity victim, DamageSource source, float amount) {
        return loaded() && MaidAccessoryRuntime.immune(victim, source, amount);
    }

    public static float reduce(
            LivingEntity victim, DamageSource source, float amount, boolean poison) {
        return loaded() ? MaidAccessoryRuntime.reduce(victim, source, amount, poison) : amount;
    }

    public static void received(LivingEntity victim, DamageSource source) {
        if (loaded()) MaidAccessoryRuntime.received(victim, source);
    }

    public static void retaliate(LivingEntity victim, DamageSource source) {
        if (loaded()) MaidAccessoryRuntime.retaliate(victim, source);
    }

    public static void confirmed(
            LivingEntity victim, DamageSource source, boolean transfer, boolean rose) {
        if (loaded()) MaidAccessoryRuntime.confirmed(victim, source, transfer, rose);
    }

    public static void successful(LivingEntity victim, DamageSource source, boolean transfer) {
        if (loaded()) MaidAccessoryRuntime.successful(victim, source, transfer);
    }

    public static boolean seesUndead(LivingEntity e) {
        return loaded() && MaidAccessoryRuntime.seesUndead(e);
    }

    public static MobEffectInstance extend(
            LivingEntity target, MobEffectInstance effect, Entity source) {
        return loaded() ? MaidAccessoryRuntime.extend(target, effect, source) : effect;
    }

    public static UUID casterId(LivingEntity e) {
        return isMaid(e) ? MaidAccessoryRuntime.identity(e) : e.getUUID();
    }

    public static void starCooldown(LivingEntity e, int ticks) {
        if (isMaid(e)) MaidAccessoryRuntime.starCooldown(e, ticks);
    }

    public static boolean completeCooldown(MinecraftServer server, UUID id, int ticks) {
        return loaded() && MaidAccessoryLedger.complete(server, id, ticks);
    }

    public static boolean lantern(Entity e) {
        return loaded() && MaidAccessoryRuntime.lantern(e);
    }

    public static boolean canHarm(
            LivingEntity attacker, net.minecraft.world.entity.player.Player other) {
        if (attacker instanceof net.minecraft.server.level.ServerPlayer p)
            return p.server.isPvpAllowed() && p.canHarmPlayer(other);
        return loaded() && MaidAccessoryRuntime.canHarm(attacker, other);
    }

    private MaidAccessories() {}
}
