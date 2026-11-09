package com.thelongtravail.integration;

import com.thelongtravail.api.TravailStateApi.AspectState;

import net.minecraft.server.MinecraftServer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.*;
import net.minecraft.world.item.*;

import java.util.*;

// 主体只使用原版类型；默认行为保留无女仆时的查询、伤害和玩家 PVP 语义。
public final class MaidIntegration {
    public static boolean equipped(
            LivingEntity entity,
            net.minecraftforge.registries.RegistryObject<net.minecraft.world.item.Item> item) {
        return item.isPresent() && equipped(entity, item.get());
    }

    public interface Equipment {
        default boolean equipped(LivingEntity entity, Item item) {
            return false;
        }

        default AspectState phase(LivingEntity entity, com.thelongtravail.TravailAspect aspect) {
            return AspectState.INACTIVE;
        }

        default boolean quixote(LivingEntity e) {
            return false;
        }

        default int quixotePhase(LivingEntity e) {
            return 0;
        }

        default UUID quixoteOwner(LivingEntity e) {
            return null;
        }

        default int phosphophyllitePhase(LivingEntity entity) {
            return 0;
        }

        default ItemStack lorentzStack(LivingEntity e) {
            return ItemStack.EMPTY;
        }

        default int lorentzPhase(LivingEntity e) {
            return 0;
        }

        default UUID lorentzOwner(LivingEntity e) {
            return null;
        }

        default boolean lafite(LivingEntity entity) {
            return false;
        }

        default int lafitePhase(LivingEntity entity) {
            return 0;
        }

        default ItemStack flowerStack(LivingEntity entity) {
            return ItemStack.EMPTY;
        }

        default int flowerPhase(LivingEntity entity) {
            return 0;
        }

        default int relicPhase(LivingEntity e) {
            return 0;
        }

        default ItemStack relicStack(LivingEntity e, boolean banner) {
            return ItemStack.EMPTY;
        }

        default boolean isMaid(Entity e) {
            return false;
        }

        default ItemStack springCrown(LivingEntity entity) {
            return ItemStack.EMPTY;
        }

        default boolean seesUndead(LivingEntity e) {
            return false;
        }

        default boolean lantern(Entity e) {
            return false;
        }
    }

    public interface Combat {
        default LivingEntity damageActor(DamageSource source) {
            return null;
        }

        default LivingEntity quixoteAttacker(DamageSource source) {
            return null;
        }

        default double bonus(LivingEntity victim, DamageSource source) {
            return 0;
        }

        default double witherBonus(LivingEntity victim, DamageSource source) {
            return 0;
        }

        default boolean immune(LivingEntity victim, DamageSource source, float amount) {
            return false;
        }

        default double remainingMultiplier(
                LivingEntity victim, DamageSource source, boolean poison) {
            return 1;
        }

        default void received(LivingEntity victim, DamageSource source) {}

        default void retaliate(LivingEntity victim, DamageSource source) {}

        default void confirmed(LivingEntity victim, DamageSource source, boolean rose) {}

        default void successful(LivingEntity victim, DamageSource source) {}

        default MobEffectInstance extend(
                LivingEntity target, MobEffectInstance effect, Entity source) {
            return effect;
        }

        default boolean canHarm(
                LivingEntity attacker, net.minecraft.world.entity.player.Player other) {
            if (attacker instanceof net.minecraft.server.level.ServerPlayer p)
                return p.server.isPvpAllowed() && p.canHarmPlayer(other);
            return false;
        }
    }

    public interface Lifecycle {
        default int quixoteCooldown(LivingEntity e) {
            return 0;
        }

        default void quixoteCooldown(LivingEntity e, int ticks) {}

        default UUID casterId(LivingEntity e) {
            return e.getUUID();
        }

        default void starCooldown(LivingEntity e, int ticks) {}

        default boolean completeCooldown(MinecraftServer server, UUID id, int ticks) {
            return false;
        }
    }

    public interface Provider extends Equipment, Combat, Lifecycle {}

    private static Provider provider = new Provider() {};

    public static void install(Provider value) {
        provider = java.util.Objects.requireNonNull(value);
    }

    public static boolean equipped(LivingEntity entity, Item item) {
        return provider.equipped(entity, item);
    }

    public static AspectState phase(LivingEntity entity, com.thelongtravail.TravailAspect aspect) {
        return provider.phase(entity, aspect);
    }

    public static LivingEntity damageActor(DamageSource source) {
        return provider.damageActor(source);
    }

    public static LivingEntity quixoteAttacker(DamageSource source) {
        return provider.quixoteAttacker(source);
    }

    public static boolean quixote(LivingEntity e) {
        return provider.quixote(e);
    }

    public static int quixotePhase(LivingEntity e) {
        return provider.quixotePhase(e);
    }

    public static UUID quixoteOwner(LivingEntity e) {
        return provider.quixoteOwner(e);
    }

    public static int quixoteCooldown(LivingEntity e) {
        return provider.quixoteCooldown(e);
    }

    public static void quixoteCooldown(LivingEntity e, int ticks) {
        provider.quixoteCooldown(e, ticks);
    }

    public static int phosphophyllitePhase(LivingEntity entity) {
        return provider.phosphophyllitePhase(entity);
    }

    public static ItemStack lorentzStack(LivingEntity e) {
        return provider.lorentzStack(e);
    }

    public static int lorentzPhase(LivingEntity e) {
        return provider.lorentzPhase(e);
    }

    public static UUID lorentzOwner(LivingEntity e) {
        return provider.lorentzOwner(e);
    }

    public static boolean lafite(LivingEntity entity) {
        return provider.lafite(entity);
    }

    public static int lafitePhase(LivingEntity entity) {
        return provider.lafitePhase(entity);
    }

    public static ItemStack flowerStack(LivingEntity entity) {
        return provider.flowerStack(entity);
    }

    public static int flowerPhase(LivingEntity entity) {
        return provider.flowerPhase(entity);
    }

    public static int relicPhase(LivingEntity e) {
        return provider.relicPhase(e);
    }

    public static ItemStack relicStack(LivingEntity e, boolean banner) {
        return provider.relicStack(e, banner);
    }

    public static boolean isMaid(Entity e) {
        return provider.isMaid(e);
    }

    public static ItemStack springCrown(LivingEntity entity) {
        return provider.springCrown(entity);
    }

    public static double bonus(LivingEntity victim, DamageSource source) {
        return provider.bonus(victim, source);
    }

    public static double witherBonus(LivingEntity victim, DamageSource source) {
        return provider.witherBonus(victim, source);
    }

    public static boolean immune(LivingEntity victim, DamageSource source, float amount) {
        return provider.immune(victim, source, amount);
    }

    public static double remainingMultiplier(
            LivingEntity victim, DamageSource source, boolean poison) {
        return provider.remainingMultiplier(victim, source, poison);
    }

    public static void received(LivingEntity victim, DamageSource source) {
        provider.received(victim, source);
    }

    public static void retaliate(LivingEntity victim, DamageSource source) {
        provider.retaliate(victim, source);
    }

    public static void confirmed(LivingEntity victim, DamageSource source, boolean rose) {
        provider.confirmed(victim, source, rose);
    }

    public static void successful(LivingEntity victim, DamageSource source) {
        provider.successful(victim, source);
    }

    public static boolean seesUndead(LivingEntity e) {
        return provider.seesUndead(e);
    }

    public static MobEffectInstance extend(
            LivingEntity target, MobEffectInstance effect, Entity source) {
        return provider.extend(target, effect, source);
    }

    public static UUID casterId(LivingEntity e) {
        return provider.casterId(e);
    }

    public static void starCooldown(LivingEntity e, int ticks) {
        provider.starCooldown(e, ticks);
    }

    public static boolean completeCooldown(MinecraftServer server, UUID id, int ticks) {
        return provider.completeCooldown(server, id, ticks);
    }

    public static boolean lantern(Entity e) {
        return provider.lantern(e);
    }

    public static boolean canHarm(
            LivingEntity attacker, net.minecraft.world.entity.player.Player other) {
        return provider.canHarm(attacker, other);
    }

    private MaidIntegration() {}
}
