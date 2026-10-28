package com.thelongtravail.underworld;

import com.thelongtravail.TravailAspect;
import com.thelongtravail.api.TravailStateApi;
import com.thelongtravail.combat.DamageIdentity;
import com.thelongtravail.combat.SunBladeActors;
import com.thelongtravail.common.AttributeModifierSet;
import com.thelongtravail.compat.maid.MaidAccessories;
import com.thelongtravail.config.SunBladeConfig;
import com.thelongtravail.helper.AccessoryEquipRules;
import com.thelongtravail.registry.ModAttributes;
import com.thelongtravail.registry.ModRegistry;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashMap;
import java.util.Map;

@Mod.EventBusSubscriber(modid = "the_long_travail")
public final class Kagotsurube {
    private static final ResourceLocation ATTRIBUTES =
            new ResourceLocation("the_long_travail", "kagotsurube");
    private static final Map<LivingEntity, Map<Attribute, AttributeModifierSet.Modifier>> APPLIED =
            new java.util.WeakHashMap<>();

    public static boolean equipped(LivingEntity entity) {
        return entity != null
                && entity.isAlive()
                && !entity.isSpectator()
                && (entity instanceof ServerPlayer actor
                        ? AccessoryEquipRules.hasEquipped(
                                actor, "belt", ModRegistry.KAGOTSURUBE.get())
                        : MaidAccessories.equipped(entity, ModRegistry.KAGOTSURUBE.get()));
    }

    public static DamageSource convert(LivingEntity target, DamageSource source) {
        LivingEntity actor = SunBladeActors.attacker(source);
        if (actor != null
                && equipped(actor)
                && SunBladeActors.phase(actor, TravailAspect.UNDERWORLD)
                        == TravailStateApi.AspectState.WITNESS)
            return DamageIdentity.convert(target, source);
        return source;
    }

    public static void confirmed(LivingEntity target, DamageSource source) {
        LivingEntity actor = SunBladeActors.attacker(source);
        if (actor == null
                || actor == target
                || !equipped(actor)
                || !target.isAlive()
                || target.isSpectator()
                || target instanceof net.minecraft.world.entity.player.Player p && p.isCreative())
            return;
        int added = SunBladeConfig.ticks("kagotsurube.witherSeconds");
        if (added <= 0) return;
        MobEffectInstance old = target.getEffect(MobEffects.WITHER);
        if (old == null)
            target.addEffect(new MobEffectInstance(MobEffects.WITHER, added, 0), actor);
        else if (!old.isInfiniteDuration()) {
            // 原地更新 NBT 副本以保留等级、隐藏效果、显示标记；绕过 addEffect 的整段时长再放大。
            var data = old.save(new net.minecraft.nbt.CompoundTag());
            data.putInt(
                    "Duration",
                    (int) Math.min(Integer.MAX_VALUE, (long) old.getDuration() + added));
            MobEffectInstance updated = MobEffectInstance.load(data);
            if (updated != null) target.forceAddEffect(updated, actor);
        }
    }

    public static void refresh(LivingEntity entity) {
        if (entity.level().isClientSide) return;
        Map<Attribute, AttributeModifierSet.Modifier> wanted = new HashMap<>();
        if (equipped(entity)
                && SunBladeActors.phase(entity, TravailAspect.UNDERWORLD)
                        == TravailStateApi.AspectState.MALICE) {
            wanted.put(
                    ModAttributes.INSIGHT.get(),
                    new AttributeModifierSet.Modifier(
                            SunBladeConfig.value("kagotsurube.insight"),
                            AttributeModifier.Operation.ADDITION));
            wanted.put(
                    ModAttributes.OVERLORD.get(),
                    new AttributeModifierSet.Modifier(
                            SunBladeConfig.value("kagotsurube.overlord"),
                            AttributeModifier.Operation.ADDITION));
        }
        if (wanted.isEmpty() && !APPLIED.containsKey(entity)) return;
        if (!wanted.equals(APPLIED.get(entity)) || entity.tickCount % 20 == 0) {
            AttributeModifierSet.reconcile(entity, ATTRIBUTES, wanted);
            if (wanted.isEmpty()) APPLIED.remove(entity);
            else APPLIED.put(entity, wanted);
        }
    }

    public static void clear(LivingEntity entity) {
        if (entity.level().isClientSide) return;
        AttributeModifierSet.reconcile(entity, ATTRIBUTES, Map.of());
        APPLIED.remove(entity);
    }

    @SubscribeEvent
    public static void tick(TickEvent.PlayerTickEvent event) {
        if (event.phase == TickEvent.Phase.END && event.player instanceof ServerPlayer)
            refresh(event.player);
    }

    private Kagotsurube() {}
}
