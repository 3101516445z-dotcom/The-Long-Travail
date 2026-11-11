package com.thelongtravail.combat;

import com.thelongtravail.registry.ModRegistry;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

// 普通生物、玩家和女仆共用的效果规则，按原伤害来源解析攻击主体。
@Mod.EventBusSubscriber(modid = "the_long_travail")
public final class EffectCombatRules {
    public static LivingEntity actor(DamageSource source) {
        source = DamageIdentity.original(source);
        return source.getEntity() instanceof LivingEntity living
                ? living
                : com.thelongtravail.integration.MaidIntegration.damageActor(source);
    }

    public static boolean peaceful(LivingEntity entity) {
        return entity != null && entity.hasEffect(ModRegistry.AMONG_ALL_THINGS.get());
    }

    public static boolean mara(LivingEntity entity) {
        return entity != null && entity.hasEffect(ModRegistry.MARA_TRANSFORMATION.get());
    }

    public static boolean revival(LivingEntity entity) {
        return entity != null && entity.hasEffect(ModRegistry.REVIVAL.get());
    }

    public static boolean blocked(LivingEntity target, DamageSource source) {
        if (allowsKill(source)) return false;
        return peaceful(target) || peaceful(actor(source));
    }

    public static boolean allowsKill(DamageSource source) {
        return !com.thelongtravail.config.TravailConfig.AMONG_ALL_THINGS_IMMUNE_TO_KILL.get()
                && DamageIdentity.original(source)
                        .is(net.minecraft.world.damagesource.DamageTypes.GENERIC_KILL);
    }

    public static DamageSource convert(LivingEntity target, DamageSource source) {
        return mara(actor(source)) ? DamageIdentity.convert(target, source) : source;
    }

    public static double bonus(DamageSource source) {
        return mara(actor(source)) ? .5 : 0;
    }

    public static double remaining(DamageSource source) {
        return revival(actor(source)) ? .01 : 1;
    }

    public static void confirmed(LivingEntity target, DamageSource source) {
        LivingEntity actor = actor(source);
        if (!mara(actor) || !target.isAlive()) return;
        MobEffectInstance old = target.getEffect(MobEffects.WITHER);
        if (old == null || old.getAmplifier() <= 2)
            target.addEffect(new MobEffectInstance(MobEffects.WITHER, 100, 2), actor);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void attack(LivingAttackEvent event) {
        if (!event.getEntity().level().isClientSide
                && blocked(event.getEntity(), event.getSource()))
            event.setCanceled(true);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void hurt(LivingHurtEvent event) {
        if (event.getEntity().level().isClientSide) return;
        if (blocked(event.getEntity(), event.getSource())) {
            event.setCanceled(true);
            return;
        }
        // 直接发布 Forge 事件的来源可能没有 hurt 上下文；此时在事件入口补齐效果倍率。
        if (CombatContext.current(event.getEntity(), event.getSource()) == null) {
            float amount = CombatRules.multiply(event.getAmount(), 1 + bonus(event.getSource()));
            event.setAmount(CombatRules.multiply(amount, remaining(event.getSource())));
        }
    }

    private EffectCombatRules() {}
}
