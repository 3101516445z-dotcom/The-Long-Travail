package com.thelongtravail.flourishing;

import com.thelongtravail.TheLongTravail;
import com.thelongtravail.config.*;
import com.thelongtravail.registry.ModRegistry;
import com.thelongtravail.mixin.EffectDurationAccessor;
import com.thelongtravail.loot.LootItemCollector;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.*;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.GameRules;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.living.MobEffectEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import java.util.UUID;

@Mod.EventBusSubscriber(modid = TheLongTravail.MODID)
public final class Mistletoe {
    private record Expiry(
            int rank,
            float maxHealth,
            net.minecraft.resources.ResourceLocation table,
            net.minecraft.world.phys.Vec3 position) {}

    private static final java.util.Map<LivingEntity, Expiry> PENDING = new java.util.WeakHashMap<>();
    private static final ThreadLocal<LivingEntity> SETTLING = new ThreadLocal<>();
    private static final UUID SPEED = UUID.fromString("4e429a52-f0ae-41db-8bc8-e54e3689ada1");

    @SubscribeEvent
    public static void interact(net.minecraftforge.event.entity.player.PlayerInteractEvent.EntityInteract event) {
        if (!event.getItemStack().is(ModRegistry.MISTLETOE.get())) return;
        Entity target = event.getTarget();
        if (target instanceof net.minecraftforge.entity.PartEntity<?> part) target = part.getParent();
        if (!(target instanceof LivingEntity living)) return;
        InteractionResult result = apply(event.getItemStack(), event.getEntity(), living);
        if (result != InteractionResult.PASS) {
            event.setCancellationResult(result);
            event.setCanceled(true);
        }
    }

    public static int level(LivingEntity entity) {
        var effect = entity.getEffect(ModRegistry.MISTLETOE_EFFECT.get());
        return effect == null
                ? 0
                : Math.max(0, Math.min(FlourishingGrowthConfig.MAX_LEVEL.get(), effect.getAmplifier() + 1));
    }

    public static InteractionResult apply(ItemStack stack, Player player, LivingEntity target) {
        if (target instanceof Player) {
            if (!player.level().isClientSide)
                player.displayClientMessage(
                        Component.translatable("message.the_long_travail.mistletoe.player_target"), true);
            return InteractionResult.FAIL;
        }
        if (!target.isAlive() || target.isSpectator() || target instanceof ArmorStand) return InteractionResult.PASS;
        if (player.level().isClientSide) return InteractionResult.SUCCESS;
        int next = Math.min(FlourishingGrowthConfig.MAX_LEVEL.get(), level(target) + 1);
        var old = target.getEffect(ModRegistry.MISTLETOE_EFFECT.get());
        int added = FlourishingItemsConfig.ticks(FlourishingGrowthConfig.mistletoe("durationSeconds"));
        int duration =
                old != null && old.isInfiniteDuration()
                        ? -1
                        : (int)
                                Math.min(
                                        Integer.MAX_VALUE,
                                        (long) added + (old == null ? 0 : Math.max(0, old.getDuration())));
        var effect = new MobEffectInstance(ModRegistry.MISTLETOE_EFFECT.get(), duration, next - 1);
        if (!target.addEffect(effect, player)) {
            player.displayClientMessage(Component.translatable("message.the_long_travail.mistletoe.rejected"), true);
            return InteractionResult.FAIL;
        }
        var actual = target.getEffect(effect.getEffect());
        if (actual == null) return InteractionResult.FAIL;
        ((EffectDurationAccessor) actual).travail$amplifier(next - 1);
        ((EffectDurationAccessor) actual).travail$duration(duration);
        ((EffectDurationAccessor) actual).travail$hiddenEffect(null);
        syncSpeed(target);
        ((ServerLevel) target.level())
                .getChunkSource()
                .broadcastAndSend(
                        target,
                        new net.minecraft.network.protocol.game.ClientboundUpdateMobEffectPacket(
                                target.getId(), actual));
        if (!player.getAbilities().instabuild) stack.shrink(1);
        return InteractionResult.CONSUME;
    }

    public static double bonus(LivingEntity entity) {
        return level(entity) * FlourishingGrowthConfig.mistletoe("damageBonusPerLevel");
    }

    public static double reduction(LivingEntity entity) {
        return SETTLING.get() == entity
                ? 0
                : Math.min(1, level(entity) * FlourishingGrowthConfig.mistletoe("damageReductionPerLevel"));
    }

    private static void syncSpeed(LivingEntity entity) {
        setSpeed(entity, SPEED, level(entity) * FlourishingGrowthConfig.mistletoe("speedBonusPerLevel"), "Mistletoe");
    }

    public static void setSpeed(LivingEntity entity, UUID id, double value, String name) {
        var attribute = entity.getAttribute(Attributes.MOVEMENT_SPEED);
        if (attribute == null) return;
        var old = attribute.getModifier(id);
        if (old != null && old.getAmount() == value) return;
        if (old != null) attribute.removeModifier(id);
        if (value > 0)
            attribute.addTransientModifier(
                    new AttributeModifier(id, name, value, AttributeModifier.Operation.MULTIPLY_TOTAL));
    }

    @SubscribeEvent
    public static void tick(LivingEvent.LivingTickEvent event) {
        if (!event.getEntity().level().isClientSide) syncSpeed(event.getEntity());
    }

    @SubscribeEvent
    public static void expired(MobEffectEvent.Expired event) {
        var effect = event.getEffectInstance();
        var entity = event.getEntity();
        if (effect == null
                || effect.getEffect() != ModRegistry.MISTLETOE_EFFECT.get()
                || !entity.isAlive()
                || !(entity.level() instanceof ServerLevel level)) return;
        // 这里只记录到期信息；等原版迭代器完成删除后，再在 tickEffects 末尾结算，避免在 Expired 事件中修改效果映射。
        PENDING.putIfAbsent(
                entity,
                new Expiry(
                        Math.min(FlourishingGrowthConfig.MAX_LEVEL.get(), effect.getAmplifier() + 1),
                        entity.getMaxHealth(),
                        lootTable(entity),
                        entity.position()));
    }

    private static net.minecraft.resources.ResourceLocation lootTable(LivingEntity entity) {
        if (entity instanceof Player) return null;
        try {
            return entity.getLootTable();
        } catch (RuntimeException failure) {
            TheLongTravail.LOGGER.warn("Mistletoe loot lookup failed for {}", entity.getUUID(), failure);
            return null;
        }
    }

    public static void abort(LivingEntity entity) {
        PENDING.remove(entity);
    }

    public static void afterEffects(LivingEntity entity) {
        Expiry expiry = PENDING.remove(entity);
        if (expiry == null || !entity.isAlive() || !(entity.level() instanceof ServerLevel level)) return;
        int rank = expiry.rank();
        var table = expiry.table();
        var position = expiry.position();
        syncSpeed(entity);
        float damage =
                (float)
                        Math.min(
                                Float.MAX_VALUE,
                                expiry.maxHealth() * rank * FlourishingGrowthConfig.mistletoe("expiryDamagePerLevel"));
        boolean reward =
                entity.getRandom().nextDouble()
                        < Math.min(1, rank * FlourishingGrowthConfig.mistletoe("lootChancePerLevel"));
        if (damage > 0) {
            LivingEntity previous = SETTLING.get();
            SETTLING.set(entity);
            try {
                entity.hurt(entity.damageSources().magic(), damage);
            } finally {
                if (previous == null) SETTLING.remove();
                else SETTLING.set(previous);
            }
        }
        if (!reward || table == null || !level.getGameRules().getBoolean(GameRules.RULE_DOMOBLOOT)) return;
        try {
            var result = LootItemCollector.collectCached(table, level.getServer().getLootData());
            for (var item : result.items()) {
                var drop =
                        new net.minecraft.world.entity.item.ItemEntity(
                                level, position.x, position.y, position.z, new ItemStack(item));
                drop.setDefaultPickUpDelay();
                level.addFreshEntity(drop);
            }
        } catch (RuntimeException failure) {
            TheLongTravail.LOGGER.warn(
                    "Mistletoe loot generation failed; not retried for {}", entity.getUUID(), failure);
        }
    }

    private Mistletoe() {}
}
