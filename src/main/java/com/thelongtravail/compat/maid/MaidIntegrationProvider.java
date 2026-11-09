package com.thelongtravail.compat.maid;

import com.thelongtravail.api.TravailStateApi.AspectState;
import com.thelongtravail.integration.MaidIntegration;

import net.minecraft.server.MinecraftServer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.*;
import net.minecraft.world.item.*;

import java.util.*;

public final class MaidIntegrationProvider implements MaidIntegration.Provider {
    @Override
    public boolean equipped(LivingEntity entity, Item item) {
        return MaidAccessories.equipped(entity, item);
    }

    @Override
    public AspectState phase(LivingEntity entity, com.thelongtravail.TravailAspect aspect) {
        return MaidAccessories.phase(entity, aspect);
    }

    @Override
    public LivingEntity damageActor(DamageSource source) {
        return MaidAccessories.damageActor(source);
    }

    @Override
    public LivingEntity quixoteAttacker(DamageSource source) {
        return MaidAccessories.quixoteAttacker(source);
    }

    @Override
    public boolean quixote(LivingEntity e) {
        return MaidAccessories.quixote(e);
    }

    @Override
    public int quixotePhase(LivingEntity e) {
        return MaidAccessories.quixotePhase(e);
    }

    @Override
    public UUID quixoteOwner(LivingEntity e) {
        return MaidAccessories.quixoteOwner(e);
    }

    @Override
    public int quixoteCooldown(LivingEntity e) {
        return MaidAccessories.quixoteCooldown(e);
    }

    @Override
    public void quixoteCooldown(LivingEntity e, int ticks) {
        MaidAccessories.quixoteCooldown(e, ticks);
    }

    @Override
    public int phosphophyllitePhase(LivingEntity entity) {
        return MaidAccessories.phosphophyllitePhase(entity);
    }

    @Override
    public ItemStack lorentzStack(LivingEntity e) {
        return MaidAccessories.lorentzStack(e);
    }

    @Override
    public int lorentzPhase(LivingEntity e) {
        return MaidAccessories.lorentzPhase(e);
    }

    @Override
    public UUID lorentzOwner(LivingEntity e) {
        return MaidAccessories.lorentzOwner(e);
    }

    @Override
    public boolean lafite(LivingEntity entity) {
        return MaidAccessories.lafite(entity);
    }

    @Override
    public int lafitePhase(LivingEntity entity) {
        return MaidAccessories.lafitePhase(entity);
    }

    @Override
    public ItemStack flowerStack(LivingEntity entity) {
        return MaidAccessories.flowerStack(entity);
    }

    @Override
    public int flowerPhase(LivingEntity entity) {
        return MaidAccessories.flowerPhase(entity);
    }

    @Override
    public int relicPhase(LivingEntity e) {
        return MaidAccessories.relicPhase(e);
    }

    @Override
    public ItemStack relicStack(LivingEntity e, boolean banner) {
        return MaidAccessories.relicStack(e, banner);
    }

    @Override
    public boolean isMaid(Entity e) {
        return MaidAccessories.isMaid(e);
    }

    @Override
    public ItemStack springCrown(LivingEntity entity) {
        return MaidAccessories.springCrown(entity);
    }

    @Override
    public double bonus(LivingEntity victim, DamageSource source) {
        return MaidAccessories.bonus(victim, source);
    }

    @Override
    public double witherBonus(LivingEntity victim, DamageSource source) {
        return MaidAccessories.witherBonus(victim, source);
    }

    @Override
    public boolean immune(LivingEntity victim, DamageSource source, float amount) {
        return MaidAccessories.immune(victim, source, amount);
    }

    @Override
    public double remainingMultiplier(LivingEntity victim, DamageSource source, boolean poison) {
        return MaidAccessories.remainingMultiplier(victim, source, poison);
    }

    @Override
    public void received(LivingEntity victim, DamageSource source) {
        MaidAccessories.received(victim, source);
    }

    @Override
    public void retaliate(LivingEntity victim, DamageSource source) {
        MaidAccessories.retaliate(victim, source);
    }

    @Override
    public void confirmed(LivingEntity victim, DamageSource source, boolean rose) {
        MaidAccessories.confirmed(victim, source, rose);
    }

    @Override
    public void successful(LivingEntity victim, DamageSource source) {
        MaidAccessories.successful(victim, source);
    }

    @Override
    public boolean seesUndead(LivingEntity e) {
        return MaidAccessories.seesUndead(e);
    }

    @Override
    public MobEffectInstance extend(LivingEntity target, MobEffectInstance effect, Entity source) {
        return MaidAccessories.extend(target, effect, source);
    }

    @Override
    public UUID casterId(LivingEntity e) {
        return MaidAccessories.casterId(e);
    }

    @Override
    public void starCooldown(LivingEntity e, int ticks) {
        MaidAccessories.starCooldown(e, ticks);
    }

    @Override
    public boolean completeCooldown(MinecraftServer server, UUID id, int ticks) {
        return MaidAccessories.completeCooldown(server, id, ticks);
    }

    @Override
    public boolean lantern(Entity e) {
        return MaidAccessories.lantern(e);
    }

    @Override
    public boolean canHarm(LivingEntity attacker, net.minecraft.world.entity.player.Player other) {
        return MaidAccessories.canHarm(attacker, other);
    }
}
