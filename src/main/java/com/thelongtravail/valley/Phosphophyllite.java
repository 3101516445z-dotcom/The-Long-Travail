package com.thelongtravail.valley;

import com.thelongtravail.api.TravailStateApi;
import com.thelongtravail.compat.arsnouveau.ArsNouveauAttributeRules;
import com.thelongtravail.common.*;
import com.thelongtravail.config.*;
import com.thelongtravail.helper.AccessoryEquipRules;
import com.thelongtravail.registry.ModRegistry;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.*;

@Mod.EventBusSubscriber(modid = "the_long_travail")
public final class Phosphophyllite {
    private static final ResourceLocation SOURCE =
            new ResourceLocation("the_long_travail", "phosphophyllite");
    private static final Set<String> COMPAT_EXCLUSIONS =
            ArsNouveauAttributeRules.phosphophylliteExclusions();

    private record Applied(int phase, double amount, Set<String> excluded) {}

    private static List<? extends String> excludedValues = List.of();
    private static Set<String> excludedSnapshot = COMPAT_EXCLUSIONS;

    private static Set<String> exclusions() {
        var values = DeepValleyItemsConfig.EXCLUDED_ATTRIBUTES.get();
        if (!excludedValues.equals(values)) {
            excludedValues = List.copyOf(values);
            Set<String> merged = new HashSet<>(values);
            merged.addAll(COMPAT_EXCLUSIONS);
            excludedSnapshot = Set.copyOf(merged);
        }
        return excludedSnapshot;
    }

    private static final Map<LivingEntity, Applied> APPLIED = new WeakHashMap<>();
    private static final Set<LivingEntity> HEALING =
            Collections.newSetFromMap(new IdentityHashMap<>());

    public static int phase(LivingEntity entity) {
        if (!entity.isAlive() || entity.isSpectator()) return 0;
        if (entity instanceof ServerPlayer p) {
            if (!AccessoryEquipRules.hasEquipped(p, "curio", ModRegistry.PHOSPHOPHYLLITE.get()))
                return 0;
            return TravailStateApi.hasDeepValleyWitness(p)
                    ? 2
                    : TravailStateApi.hasDeepValleyMalice(p) ? 1 : 0;
        }
        return com.thelongtravail.compat.maid.MaidAccessories.phosphophyllitePhase(entity);
    }

    public static double damageBonus(LivingEntity entity) {
        if (!ModeEffectRules.PHOSPHOPHYLLITE_DAMAGE.applies(entity)) return 0;
        int phase = phase(entity);
        return phase == 0
                ? 0
                : DeepValleyItemsConfig.get(
                        "phosphophyllite." + (phase == 2 ? "witnessDamage" : "maliceDamage"));
    }

    public static void tick(LivingEntity entity) {
        int phase = phase(entity);
        double amount =
                phase == 0
                        ? 0
                        : DeepValleyItemsConfig.get(
                                "phosphophyllite."
                                        + (phase == 2 ? "witnessAttributes" : "maliceAttributes"));
        Set<String> excluded = exclusions();
        Applied old = APPLIED.get(entity);
        boolean changed =
                old == null
                        || old.phase != phase
                        || Double.compare(old.amount, amount) != 0
                        || !old.excluded.equals(excluded);
        if (changed || phase != 0 && entity.tickCount % 20 == 0) {
            AttributeModifierSet.reconcile(entity, SOURCE, amount, excluded);
            if (changed) APPLIED.put(entity, new Applied(phase, amount, excluded));
        }
    }

    public static void received(LivingEntity entity) {
        if (phase(entity) != 2 || !HEALING.add(entity)) return;
        try {
            if (entity.getRandom().nextDouble()
                    < DeepValleyItemsConfig.get("phosphophyllite.healChance"))
                OverflowHealing.heal(
                        entity,
                        (float)
                                Math.min(
                                        Float.MAX_VALUE,
                                        entity.getMaxHealth()
                                                * DeepValleyItemsConfig.get(
                                                        "phosphophyllite.healFraction")));
        } finally {
            HEALING.remove(entity);
        }
    }

    @SubscribeEvent
    public static void tick(TickEvent.PlayerTickEvent event) {
        if (event.phase == TickEvent.Phase.END && event.player instanceof ServerPlayer p) tick(p);
    }

    @SubscribeEvent
    public static void stopped(net.minecraftforge.event.server.ServerStoppedEvent event) {
        APPLIED.clear();
        HEALING.clear();
        excludedValues = List.of();
        excludedSnapshot = COMPAT_EXCLUSIONS;
    }

    private Phosphophyllite() {}
}
