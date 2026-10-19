package com.thelongtravail.flourishing;

import com.thelongtravail.*;
import com.thelongtravail.config.*;
import com.thelongtravail.data.LongTravailData;
import com.thelongtravail.helper.TravailCurios;
import com.thelongtravail.registry.ModRegistry;
import com.thelongtravail.growth.GrowthScheduler;
import com.thelongtravail.combat.DamageReplay;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.entity.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.effect.*;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import top.theillusivec4.curios.api.CuriosApi;
import java.util.UUID;

@Mod.EventBusSubscriber(modid = TheLongTravail.MODID)
public final class EverlastingFlower {
    public static final TagKey<Biome> BIOMES =
            TagKey.create(Registries.BIOME, new ResourceLocation(TheLongTravail.MODID, "flower_biomes"));
    private static final ResourceLocation GROWTH_SOURCE =
            ResourceLocation.fromNamespaceAndPath(TheLongTravail.MODID, "everlasting_flower");
    private static final DamageReplay.Policy REPLAY =
            new DamageReplay.Policy() {
                public boolean valid(LivingEntity attacker, LivingEntity target) {
                    return phase(attacker) == 2;
                }

                public boolean continueChain(LivingEntity attacker, LivingEntity target) {
                    return attacker.getRandom().nextDouble() < FlourishingGrowthConfig.flower("witness.repeatChance");
                }
            };
    private static final UUID SPEED = UUID.fromString("4e429a52-f0ae-41db-8bc8-e54e3689ada2");

    public static ItemStack equipped(LivingEntity entity) {
        if (!entity.isAlive() || entity.isSpectator()) return ItemStack.EMPTY;
        if (com.thelongtravail.compat.maid.MaidAccessories.isMaid(entity))
            return com.thelongtravail.compat.maid.MaidAccessories.flowerStack(entity);
        if (!(entity instanceof net.minecraft.world.entity.player.Player)) return ItemStack.EMPTY;
        return CuriosApi.getCuriosInventory(entity)
                .map(
                        inv ->
                                inv.findCurios(ModRegistry.EVERLASTING_FLOWER.get()).stream()
                                        .filter(r -> !r.slotContext().cosmetic())
                                        .map(r -> r.stack())
                                        .findFirst()
                                        .orElse(ItemStack.EMPTY))
                .orElse(ItemStack.EMPTY);
    }

    public static int phase(LivingEntity entity) {
        if (equipped(entity).isEmpty()) return 0;
        if (!(entity instanceof ServerPlayer player))
            return com.thelongtravail.compat.maid.MaidAccessories.flowerPhase(entity);
        var diary = TravailCurios.stack(player);
        return diary.isEmpty() ? 0 : LongTravailData.hasWitness(diary, TravailAspect.FLOURISHING) ? 2 : 1;
    }

    public static boolean growthEnabled(ItemStack stack) {
        return stack.hasTag() && stack.getTag().contains("TravailGrowth")
                ? stack.getTag().getBoolean("TravailGrowth")
                : com.thelongtravail.network.TooltipConfigSync.decimal(
                                "floral.growthDefault", FlourishingGrowthConfig.DEFAULT_GROWTH.get() ? 1 : 0)
                        > 0;
    }

    public static boolean biome(LivingEntity entity) {
        return entity.level().getBiome(entity.blockPosition()).is(BIOMES);
    }

    public static double bonus(LivingEntity entity) {
        return phase(entity) == 2 && biome(entity) ? FlourishingGrowthConfig.flower("witness.damageBonus") : 0;
    }

    public static double reduction(LivingEntity entity) {
        return phase(entity) == 1 && biome(entity) ? FlourishingGrowthConfig.flower("malice.damageReduction") : 0;
    }

    public static void update(LivingEntity entity) {
        if (entity.level().isClientSide) return;
        var stack = equipped(entity);
        Mistletoe.setSpeed(
                entity,
                SPEED,
                !stack.isEmpty() && phase(entity) == 1 && biome(entity)
                        ? FlourishingGrowthConfig.flower("malice.speedBonus")
                        : 0,
                "Everlasting flower");
        if (!stack.isEmpty() && FlourishingGrowthConfig.GROWTH.get() && growthEnabled(stack))
            GrowthScheduler.submit(
                    GROWTH_SOURCE,
                    entity,
                    new GrowthScheduler.Policy(
                            FlourishingGrowthConfig.RADIUS.get(),
                            FlourishingGrowthConfig.HEIGHT.get(),
                            FlourishingGrowthConfig.INTERVAL.get()));
        else GrowthScheduler.remove(GROWTH_SOURCE, entity);
    }

    @SubscribeEvent
    public static void tick(LivingEvent.LivingTickEvent event) {
        LivingEntity entity = event.getEntity();
        if (entity instanceof ServerPlayer || com.thelongtravail.compat.maid.MaidAccessories.isMaid(entity))
            update(entity);
    }

    public static void successful(LivingEntity target, DamageSource source, float amount, boolean replay) {
        if (!(source.getEntity() instanceof LivingEntity attacker) || attacker == target || !attacker.isAlive()) return;
        if (replay) return;
        int phase = phase(attacker);
        if (phase == 1
                && attacker.getRandom().nextDouble() < FlourishingGrowthConfig.flower("malice.resistanceChance")) {
            int amplifier = FlourishingGrowthConfig.RESISTANCE_LEVEL.get() - 1;
            var old = attacker.getEffect(MobEffects.DAMAGE_RESISTANCE);
            if (old == null || old.getAmplifier() <= amplifier)
                attacker.addEffect(
                        new MobEffectInstance(
                                MobEffects.DAMAGE_RESISTANCE,
                                FlourishingItemsConfig.ticks(
                                        FlourishingGrowthConfig.flower("malice.resistanceSeconds")),
                                amplifier));
        } else if (phase == 2
                && amount > 0
                && Float.isFinite(amount)
                && attacker.getRandom().nextDouble() < FlourishingGrowthConfig.flower("witness.repeatChance"))
            DamageReplay.enqueue(attacker, target, source, amount, REPLAY);
    }

    private EverlastingFlower() {}
}
