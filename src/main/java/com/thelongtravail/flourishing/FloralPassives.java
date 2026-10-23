package com.thelongtravail.flourishing;

import com.thelongtravail.mixin.EffectDurationAccessor;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.ForgeMod;
import java.util.UUID;

public final class FloralPassives {
    private static final UUID GRAVITY = UUID.fromString("035e4f41-a19a-4815-8f8a-72789ff1e101"),
            KNOCKBACK = UUID.fromString("035e4f41-a19a-4815-8f8a-72789ff1e102"),
            BLOCK_REACH = UUID.fromString("035e4f41-a19a-4815-8f8a-72789ff1e103"),
            ENTITY_REACH = UUID.fromString("035e4f41-a19a-4815-8f8a-72789ff1e104"),
            TOUGHNESS = UUID.fromString("035e4f41-a19a-4815-8f8a-72789ff1e105");

    public static void tick(LivingEntity entity, ItemStack crown) {
        if (!entity.isAlive() || entity.isSpectator()) crown = ItemStack.EMPTY;
        modifier(
                entity,
                ForgeMod.ENTITY_GRAVITY.get(),
                GRAVITY,
                -FloralCombat.value(crown, Flower.CHERRY_LEAVES),
                AttributeModifier.Operation.MULTIPLY_TOTAL);
        modifier(
                entity,
                Attributes.KNOCKBACK_RESISTANCE,
                KNOCKBACK,
                FloralCombat.value(crown, Flower.FLOWERING_AZALEA),
                AttributeModifier.Operation.ADDITION);
        modifier(
                entity,
                Attributes.ARMOR_TOUGHNESS,
                TOUGHNESS,
                FloralCombat.value(crown, Flower.FLOWERING_AZALEA_LEAVES),
                AttributeModifier.Operation.ADDITION);
        double reach = FloralCombat.value(crown, Flower.CHORUS_FLOWER);
        modifier(
                entity,
                ForgeMod.BLOCK_REACH.get(),
                BLOCK_REACH,
                reach,
                AttributeModifier.Operation.ADDITION);
        modifier(
                entity,
                ForgeMod.ENTITY_REACH.get(),
                ENTITY_REACH,
                reach,
                AttributeModifier.Operation.ADDITION);
    }

    private static void modifier(
            LivingEntity entity,
            Attribute attribute,
            UUID id,
            double amount,
            AttributeModifier.Operation operation) {
        var instance = entity.getAttribute(attribute);
        if (instance == null) return;
        var old = instance.getModifier(id);
        if (old != null && old.getAmount() == amount && old.getOperation() == operation) return;
        if (old != null) instance.removeModifier(id);
        if (amount != 0)
            instance.addTransientModifier(
                    new AttributeModifier(id, "Spring flower", amount, operation));
    }

    public static MobEffectInstance shorten(LivingEntity target, MobEffectInstance incoming) {
        if (target.level().isClientSide
                || !target.isAlive()
                || target.isSpectator()
                || incoming.getEffect().getCategory() != MobEffectCategory.HARMFUL
                || incoming.getEffect().isInstantenous()) return incoming;
        ItemStack crown =
                target instanceof ServerPlayer player
                        ? Affection.equipped(player, true)
                        : com.thelongtravail.compat.maid.MaidAccessories.springCrown(target);
        double reduction = FloralCombat.value(crown, Flower.SPORE_BLOSSOM);
        if (reduction <= 0) return incoming;
        return copyShortened(incoming, 1 - reduction);
    }

    private static MobEffectInstance copyShortened(MobEffectInstance incoming, double multiplier) {
        var copy = new MobEffectInstance(incoming);
        if (!incoming.isInfiniteDuration() && incoming.getDuration() > 0)
            ((EffectDurationAccessor) copy)
                    .travail$duration((int) Math.ceil(incoming.getDuration() * multiplier));
        var hidden = ((EffectDurationAccessor) incoming).travail$hiddenEffect();
        if (hidden != null)
            ((EffectDurationAccessor) copy).travail$hiddenEffect(copyShortened(hidden, multiplier));
        return copy;
    }

    private FloralPassives() {}
}
