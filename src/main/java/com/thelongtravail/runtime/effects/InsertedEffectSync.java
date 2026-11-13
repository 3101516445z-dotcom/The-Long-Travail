package com.thelongtravail.runtime.effects;

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

    public static void synchronize(LivingEntity target, MobEffectInstance effect) {
        var data = target.getPersistentData();
        var marked = data.getCompound(KEY);
        marked.putBoolean(ForgeRegistries.MOB_EFFECTS.getKey(effect.getEffect()).toString(), true);
        data.put(KEY, marked);
        EffectInsertion.synchronize(target, effect);
        TravailNetwork.sendInsertedEffect(target, packet(target, effect));
    }

    public static void updated(LivingEntity target, MobEffectInstance effect) {
        if (target.level().isClientSide || !marked(target, effect)) return;
        TravailNetwork.sendInsertedEffect(target, packet(target, effect));
    }

    public static void removed(LivingEntity target, MobEffectInstance effect) {
        if (!(target.level() instanceof net.minecraft.server.level.ServerLevel level)
                || !marked(target, effect)) return;
        var marked = target.getPersistentData().getCompound(KEY);
        marked.remove(ForgeRegistries.MOB_EFFECTS.getKey(effect.getEffect()).toString());
        if (marked.isEmpty()) target.getPersistentData().remove(KEY);
        level.getChunkSource()
                .broadcastAndSend(
                        target,
                        new net.minecraft.network.protocol.game.ClientboundRemoveMobEffectPacket(
                                target.getId(), effect.getEffect()));
    }

    private static boolean marked(LivingEntity target, MobEffectInstance effect) {
        return target.getPersistentData().contains(KEY)
                && target.getPersistentData()
                        .getCompound(KEY)
                        .contains(
                                ForgeRegistries.MOB_EFFECTS.getKey(effect.getEffect()).toString());
    }

    @SubscribeEvent
    public static void tracking(PlayerEvent.StartTracking event) {
        if (!(event.getEntity() instanceof ServerPlayer player)
                || !(event.getTarget() instanceof LivingEntity target)) return;
        var marked = target.getPersistentData().getCompound(KEY);
        for (var effect : target.getActiveEffects()) {
            String id = ForgeRegistries.MOB_EFFECTS.getKey(effect.getEffect()).toString();
            if (marked.contains(id))
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
