package com.thelongtravail.farreach;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.nbt.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.animal.*;
import net.minecraft.world.entity.ambient.AmbientCreature;
import net.minecraft.world.entity.monster.*;
import net.minecraft.world.entity.monster.hoglin.Hoglin;
import net.minecraft.world.entity.monster.piglin.*;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import java.util.*;

public final class BabelProtection {
    private static long nextMalformedUuidWarning;
    private static final List<MemoryModuleType<?>> BLOCKED_MEMORIES =
            List.of(
                    MemoryModuleType.ATTACK_TARGET,
                    MemoryModuleType.ANGRY_AT,
                    MemoryModuleType.HURT_BY_ENTITY,
                    MemoryModuleType.HURT_BY);
    public static final String BROKEN = "TravailBabelProvoked";
    public static final TagKey<EntityType<?>> INCLUDE =
            TagKey.create(
                    Registries.ENTITY_TYPE,
                    new ResourceLocation("the_long_travail", "babel_include"));
    public static final TagKey<EntityType<?>> EXCLUDE =
            TagKey.create(
                    Registries.ENTITY_TYPE,
                    new ResourceLocation("the_long_travail", "babel_exclude"));

    public static boolean eligible(Entity e) {
        if (!(e instanceof Mob) || e.getType().is(EXCLUDE)) return false;
        if (e.getType().is(INCLUDE)) return true;
        if (e instanceof PiglinBrute || e instanceof Hoglin) return false;
        return e instanceof NeutralMob
                || e instanceof Piglin
                || (!(e instanceof Enemy)
                        && (e instanceof Animal
                                || e instanceof AbstractVillager
                                || e instanceof AbstractGolem
                                || e instanceof WaterAnimal
                                || e instanceof AmbientCreature));
    }

    public static boolean provoked(LivingEntity mob, UUID player) {
        ListTag list = mob.getPersistentData().getList(BROKEN, Tag.TAG_INT_ARRAY);
        for (Tag tag : list) {
            if (!(tag instanceof IntArrayTag array) || array.getAsIntArray().length != 4) {
                long now = System.nanoTime();
                if (nextMalformedUuidWarning == 0 || now - nextMalformedUuidWarning >= 0) {
                    nextMalformedUuidWarning = now + 60_000_000_000L;
                    com.thelongtravail.TheLongTravail.LOGGER.warn(
                            "Ignoring malformed Babel provocation UUID on entity {}",
                            mob.getUUID());
                }
                continue;
            }
            if (NbtUtils.loadUUID(tag).equals(player)) return true;
        }
        return false;
    }

    public static void damaged(LivingEntity mob, DamageSource source) {
        if (mob.level().isClientSide || !eligible(mob)) return;
        ServerPlayer player = com.thelongtravail.flourishing.Affection.attacker(source.getEntity());
        if (player == null)
            player = com.thelongtravail.flourishing.Affection.attacker(source.getDirectEntity());
        if (player == null || provoked(mob, player.getUUID())) return;
        ListTag list = mob.getPersistentData().getList(BROKEN, Tag.TAG_INT_ARRAY);
        list.add(NbtUtils.createUUID(player.getUUID()));
        mob.getPersistentData().put(BROKEN, list);
    }

    public static boolean protects(LivingEntity mob, Entity target) {
        return target instanceof ServerPlayer player
                && !mob.level().isClientSide
                && eligible(mob)
                && !provoked(mob, player.getUUID())
                && BabelCarrying.carrying(player);
    }

    public static boolean memoryBlocked(
            LivingEntity owner, MemoryModuleType<?> type, Object value) {
        if (owner == null || !eligible(owner)) return false;
        if (type == MemoryModuleType.ATTACK_TARGET || type == MemoryModuleType.HURT_BY_ENTITY)
            return value instanceof Entity e && protects(owner, e);
        if (type == MemoryModuleType.ANGRY_AT && value instanceof UUID uuid)
            return protects(owner, owner.level().getPlayerByUUID(uuid));
        if (type == MemoryModuleType.HURT_BY && value instanceof DamageSource source)
            return protects(owner, source.getEntity());
        return false;
    }

    public static void cleanBrain(LivingEntity owner, Brain<?> brain) {
        if (brain == null) return;
        for (MemoryModuleType<?> key : BLOCKED_MEMORIES)
            if (brain.hasMemoryValue(key)
                    && memoryBlocked(owner, key, brain.getMemory(key).orElse(null)))
                brain.eraseMemory(key);
    }

    public static void clean(Mob mob) {
        if (mob.level().isClientSide || !eligible(mob)) return;
        if (protects(mob, mob.getTarget())) {
            mob.setTarget(null);
            mob.getNavigation().stop();
        }
        if (protects(mob, mob.getLastHurtByMob())) mob.setLastHurtByMob(null);
        if (protects(mob, mob.getLastHurtMob())) mob.setLastHurtMob(null);
        if (mob instanceof NeutralMob n
                && n.getPersistentAngerTarget() != null
                && protects(mob, mob.level().getPlayerByUUID(n.getPersistentAngerTarget()))) {
            n.setPersistentAngerTarget(null);
            n.setRemainingPersistentAngerTime(0);
        }
        cleanBrain(mob, mob.getBrain());
    }

    private BabelProtection() {}
}
