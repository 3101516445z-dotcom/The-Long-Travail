package com.thelongtravail.runtime.effects;

import com.thelongtravail.common.RuntimeFailures;
import com.thelongtravail.network.InsertedEffectPacket;
import com.thelongtravail.network.TravailNetwork;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

// 仅同步显式植入工具确认的效果；记录随实体存档保留，供后加入的追踪者读取。
@Mod.EventBusSubscriber(modid = "the_long_travail")
public final class InsertedEffectSync {
    private static final String KEY = "the_long_travail:inserted_effects";

    static boolean isMarked(LivingEntity target, net.minecraft.world.effect.MobEffect effect) {
        var id = ForgeRegistries.MOB_EFFECTS.getKey(effect);
        return id != null && target.getPersistentData().getCompound(KEY).contains(id.toString());
    }

    // 失败恢复专用：先恢复追踪标记，再补发最终状态；不再次执行效果回调。
    static void restore(
            LivingEntity target,
            net.minecraft.world.effect.MobEffect effect,
            @javax.annotation.Nullable MobEffectInstance previous,
            boolean wasMarked) {
        var id = ForgeRegistries.MOB_EFFECTS.getKey(effect);
        if (id == null) return;
        var data = target.getPersistentData();
        var marked = data.getCompound(KEY);
        if (wasMarked) marked.putBoolean(id.toString(), true);
        else marked.remove(id.toString());
        if (marked.isEmpty()) data.remove(KEY);
        else data.put(KEY, marked);
        if (previous != null) {
            EffectInsertion.synchronize(target, previous);
            if (wasMarked) TravailNetwork.sendInsertedEffect(target, packet(target, previous));
        } else if (target.level() instanceof net.minecraft.server.level.ServerLevel level) {
            level.getChunkSource()
                    .broadcastAndSend(
                            target,
                            new net.minecraft.network.protocol.game
                                    .ClientboundRemoveMobEffectPacket(target.getId(), effect));
        }
    }

    public static void synchronize(LivingEntity target, MobEffectInstance effect) {
        String id = registeredId(effect);
        if (id == null) return;
        var data = target.getPersistentData();
        var marked = data.getCompound(KEY);
        marked.putBoolean(id, true);
        data.put(KEY, marked);
        EffectInsertion.synchronize(target, effect);
        TravailNetwork.sendInsertedEffect(target, packet(target, effect));
    }

    public static void updated(LivingEntity target, MobEffectInstance effect) {
        if (target.level().isClientSide || !marked(target, effect)) return;
        TravailNetwork.sendInsertedEffect(target, packet(target, effect));
    }

    public static void removed(LivingEntity target, MobEffectInstance effect) {
        if (!(target.level() instanceof net.minecraft.server.level.ServerLevel level)) return;
        var marked = target.getPersistentData().getCompound(KEY);
        if (marked.isEmpty()) return;
        String id = registeredId(effect);
        if (id == null || !marked.contains(id)) return;
        marked.remove(id);
        if (marked.isEmpty()) target.getPersistentData().remove(KEY);
        level.getChunkSource()
                .broadcastAndSend(
                        target,
                        new net.minecraft.network.protocol.game.ClientboundRemoveMobEffectPacket(
                                target.getId(), effect.getEffect()));
    }

    private static boolean marked(LivingEntity target, MobEffectInstance effect) {
        var marked = target.getPersistentData().getCompound(KEY);
        if (marked.isEmpty()) return false;
        String id = registeredId(effect);
        return id != null && marked.contains(id);
    }

    @javax.annotation.Nullable
    private static String registeredId(MobEffectInstance effect) {
        var id = ForgeRegistries.MOB_EFFECTS.getKey(effect.getEffect());
        if (id != null) return id.toString();
        RuntimeFailures.report(
                "inserted effect sync",
                new IllegalArgumentException(
                        "Cannot synchronize unregistered effect: "
                                + effect.getEffect().getClass().getName()));
        return null;
    }

    @SubscribeEvent
    public static void tracking(PlayerEvent.StartTracking event) {
        if (!(event.getEntity() instanceof ServerPlayer player)
                || !(event.getTarget() instanceof LivingEntity target)) return;
        var marked = target.getPersistentData().getCompound(KEY);
        if (marked.isEmpty()) return;
        for (var effect : target.getActiveEffects()) {
            String id = registeredId(effect);
            if (id != null && marked.contains(id))
                TravailNetwork.sendInsertedEffect(player, packet(target, effect));
        }
    }

    private static InsertedEffectPacket packet(LivingEntity target, MobEffectInstance effect) {
        return new InsertedEffectPacket(
                target.level().dimension().location(),
                target.getId(),
                target.getUUID(),
                effect.save(new CompoundTag()));
    }

    private InsertedEffectSync() {}
}
