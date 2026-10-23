package com.thelongtravail.boundless;

import com.thelongtravail.*;
import com.thelongtravail.api.TravailStateApi;
import com.thelongtravail.compat.maid.MaidAccessories;
import com.thelongtravail.config.BoundlessItemsConfig;
import com.thelongtravail.helper.*;
import com.thelongtravail.registry.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.*;
import net.minecraft.world.item.*;
import net.minecraft.world.phys.*;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import java.util.*;

@Mod.EventBusSubscriber(modid = TheLongTravail.MODID)
public final class LorentzFantasy {
    public static final ResourceKey<DamageType> VOID =
            ResourceKey.create(
                    Registries.DAMAGE_TYPE,
                    new ResourceLocation(TheLongTravail.MODID, "lorentz_void"));
    private static final UUID CRITICAL = UUID.fromString("1c32e413-3b0f-41c0-9edf-69b584d01f02");
    private static final String MARK = "TravailLorentz",
            PHASE = "TravailLorentzPhase",
            HITS = "TravailLorentzHits",
            AGE = "TravailLorentzAge";

    private record Impact(AbstractArrow arrow, LivingEntity target, boolean headshot) {}

    private static final ThreadLocal<Impact> IMPACT = new ThreadLocal<>();
    private static final Set<AbstractArrow> ARROWS = Collections.newSetFromMap(new WeakHashMap<>());
    private static final Map<LivingEntity, Map<UUID, Long>> COOLDOWNS = new WeakHashMap<>();

    public static boolean secondary(DamageSource s) {
        return s.is(VOID);
    }

    public static ItemStack equipped(LivingEntity e) {
        if (!e.isAlive() || e.isRemoved()) return ItemStack.EMPTY;
        if (e instanceof ServerPlayer p) {
            if (p.isSpectator() || TravailCurios.stack(p).isEmpty()) return ItemStack.EMPTY;
            return top.theillusivec4.curios.api.CuriosApi.getCuriosInventory(p)
                    .resolve()
                    .flatMap(
                            h ->
                                    h.findCurios(ModRegistry.LORENTZ_FANTASY.get()).stream()
                                            .filter(
                                                    r ->
                                                            !r.slotContext().cosmetic()
                                                                    && r.slotContext()
                                                                            .identifier()
                                                                            .equals("bracelet"))
                                            .findFirst())
                    .map(r -> r.stack())
                    .orElse(ItemStack.EMPTY);
        }
        return MaidAccessories.lorentzStack(e);
    }

    public static int phase(LivingEntity e) {
        if (equipped(e).isEmpty()) return 0;
        if (e instanceof ServerPlayer p)
            return switch (TravailStateApi.state(p, TravailAspect.BOUNDLESS)) {
                case MALICE -> 1;
                case WITNESS -> 2;
                default -> 0;
            };
        return MaidAccessories.lorentzPhase(e);
    }

    public static void refresh(LivingEntity e) {
        var a = e.getAttribute(ModAttributes.CRITICAL_CHANCE.get());
        if (a == null) return;
        double amount = phase(e) == 1 ? BoundlessItemsConfig.get("lorentz.critical") : 0;
        var old = a.getModifier(CRITICAL);
        if (old != null && old.getAmount() == amount) return;
        if (old != null) a.removeModifier(CRITICAL);
        if (amount > 0)
            a.addTransientModifier(
                    new AttributeModifier(
                            CRITICAL,
                            "Lorentz Malice critical",
                            amount,
                            AttributeModifier.Operation.ADDITION));
    }

    public static boolean eligibleArrow(AbstractArrow a) {
        return !(a instanceof ThrownTrident);
    }

    public static void initialize(AbstractArrow a) {
        if (a.level().isClientSide
                || !eligibleArrow(a)
                || a.getPersistentData().getBoolean(MARK)
                || !(a.getOwner() instanceof LivingEntity shooter)) return;
        ItemStack stack = equipped(shooter);
        if (stack.isEmpty()) return;
        var tag = a.getPersistentData();
        tag.putBoolean(MARK, true);
        tag.putInt(PHASE, phase(shooter));
        if (!stack.hasTag() || !stack.getTag().getBoolean("LorentzGravityDisabled"))
            a.setNoGravity(true);
        a.setPierceLevel(
                (byte)
                        Math.min(
                                127,
                                Math.max(0, a.getPierceLevel())
                                        + (int) BoundlessItemsConfig.get("lorentz.pierce")));
        if (shooter instanceof ServerPlayer p && (p.getAbilities().flying || p.isFallFlying()))
            tag.putBoolean("LongTravailFlyingProjectile", true);
        ARROWS.add(a);
    }

    @SubscribeEvent
    public static void join(EntityJoinLevelEvent e) {
        if (!e.getLevel().isClientSide && e.getEntity() instanceof AbstractArrow a) {
            if (e.loadedFromDisk()) {
                if (a.getPersistentData().getBoolean(MARK)) ARROWS.add(a);
            } else initialize(a);
        }
    }

    @SubscribeEvent
    public static void playerTick(TickEvent.PlayerTickEvent e) {
        if (e.phase == TickEvent.Phase.END && e.player instanceof ServerPlayer p) refresh(p);
    }

    @SubscribeEvent
    public static void tick(TickEvent.ServerTickEvent e) {
        if (e.phase != TickEvent.Phase.END) return;
        ARROWS.removeIf(
                a -> {
                    if (a.isRemoved()) return true;
                    if (a.isNoGravity()
                            && !((com.thelongtravail.mixin.LorentzArrowAccessor) a)
                                    .travail$inGround()
                            && !TimeStopManager.frozen(a)) {
                        int age = a.getPersistentData().getInt(AGE) + 1;
                        a.getPersistentData().putInt(AGE, age);
                        if (age >= BoundlessItemsConfig.ticks("lorentz.arrowLifetime")) {
                            a.discard();
                            return true;
                        }
                    }
                    return false;
                });
        if (e.getServer().getTickCount() % 200 == 0)
            COOLDOWNS
                    .values()
                    .forEach(
                            m ->
                                    m.entrySet()
                                            .removeIf(
                                                    x ->
                                                            x.getValue()
                                                                    <= e.getServer()
                                                                            .overworld()
                                                                            .getGameTime()));
    }

    @SubscribeEvent
    public static void stop(ServerStoppedEvent e) {
        ARROWS.clear();
        COOLDOWNS.clear();
        IMPACT.remove();
    }

    public static double bonus(LivingEntity target, DamageSource source) {
        if (secondary(source)
                || !source.is(DamageTypeTags.IS_PROJECTILE)
                || !(source.getEntity() instanceof LivingEntity shooter)
                || !enemy(shooter, target)
                || phase(shooter) != 1) return 0;
        double bonus = BoundlessItemsConfig.get("lorentz.damage");
        Impact i = IMPACT.get();
        if (i != null
                && i.target == target
                && i.arrow == source.getDirectEntity()
                && i.headshot
                && i.arrow.getPersistentData().getInt(PHASE) == 1)
            bonus += BoundlessItemsConfig.get("lorentz.headshot");
        return bonus;
    }

    public static boolean enemy(LivingEntity shooter, LivingEntity target) {
        if (shooter == target
                || !target.isAlive()
                || target.isRemoved()
                || shooter.isAlliedTo(target)
                || target.isAlliedTo(shooter)) return false;
        UUID ownerId =
                shooter instanceof ServerPlayer p
                        ? p.getUUID()
                        : MaidAccessories.lorentzOwner(shooter);
        ServerPlayer owner =
                shooter instanceof ServerPlayer p
                        ? p
                        : shooter.getServer() == null || ownerId == null
                                ? null
                                : shooter.getServer().getPlayerList().getPlayer(ownerId);
        if (target instanceof Player p)
            return !p.isSpectator()
                    && MaidAccessories.canHarm(shooter, p)
                    && (owner == null
                            || !com.thelongtravail.compat.ftbteams.FtbTeamsCompat.lanternTeammates(
                                    owner, p));
        if (target instanceof net.minecraft.world.entity.TamableAnimal pet
                && pet.getOwnerUUID() != null) {
            UUID petOwner =
                    shooter instanceof ServerPlayer p
                            ? p.getUUID()
                            : MaidAccessories.lorentzOwner(shooter);
            if (pet.getOwnerUUID().equals(petOwner)) return false;
        }
        return true;
    }

    public static boolean headshot(AbstractArrow arrow, LivingEntity target) {
        double height = Math.max(1e-6, target.getBbHeight());
        double eye = target.getEyeY();
        var id =
                net.minecraftforge.registries.ForgeRegistries.ENTITY_TYPES.getKey(target.getType());
        if (id != null
                && com.thelongtravail.config.LorentzGeometryConfig.FIX
                        .get()
                        .contains(id.toString())) {
            double corrected = Math.min(height, target.getType().getDimensions().height);
            eye =
                    target.getBoundingBox().minY
                            + Math.max(0, Math.min(1, target.getEyeHeight() / height)) * corrected;
            height = corrected;
        }
        Vec3 from = arrow.position(), to = from.add(arrow.getDeltaMovement());
        Vec3 hit = target.getBoundingBox().inflate(.3).clip(from, to).orElse(from);
        return hit.y >= eye - height * BoundlessItemsConfig.get("lorentz.headTolerance")
                && hit.y <= target.getBoundingBox().minY + height + .3;
    }

    private static LivingEntity livingTarget(Entity entity) {
        return entity instanceof LivingEntity living
                ? living
                : entity instanceof net.minecraftforge.entity.PartEntity<?> part
                                && part.getParent() instanceof LivingEntity living
                        ? living
                        : null;
    }

    public static boolean impact(
            AbstractArrow arrow,
            Entity entity,
            DamageSource source,
            float amount,
            java.util.function.BooleanSupplier original) {
        LivingEntity target = livingTarget(entity);
        if (target == null
                || !arrow.getPersistentData().getBoolean(MARK)
                || !(arrow.getOwner() instanceof LivingEntity shooter)
                || !enemy(shooter, target)) return original.getAsBoolean();
        var tag = arrow.getPersistentData();
        var hits = tag.getList(HITS, net.minecraft.nbt.Tag.TAG_STRING);
        String id = target.getStringUUID();
        boolean seen = false;
        for (int n = 0; n < hits.size(); n++) if (id.equals(hits.getString(n))) seen = true;
        if (seen) return original.getAsBoolean();
        hits.add(net.minecraft.nbt.StringTag.valueOf(id));
        tag.put(HITS, hits);
        Impact previous = IMPACT.get();
        IMPACT.set(new Impact(arrow, target, headshot(arrow, target)));
        double health = target.getHealth();
        boolean witness = tag.getInt(PHASE) == 2 && phase(shooter) == 2;
        boolean blocked =
                shooter instanceof ServerPlayer p
                        && com.thelongtravail.config.ModeEffectRules.diaryApplies(p)
                        && TravailStateApi.hasBoundlessMalice(p)
                        && tag.getBoolean("LongTravailFlyingProjectile");
        boolean result;
        try {
            result = original.getAsBoolean();
        } finally {
            if (previous == null) IMPACT.remove();
            else IMPACT.set(previous);
        }
        if (witness
                && !blocked
                && phase(shooter) == 2
                && enemy(shooter, target)
                && Double.isFinite(health)
                && health > 0) {
            long now = shooter.getServer().overworld().getGameTime();
            UUID caster = MaidAccessories.casterId(shooter);
            var cooldowns = COOLDOWNS.computeIfAbsent(target, t -> new HashMap<>());
            long until = cooldowns.getOrDefault(caster, Long.MIN_VALUE);
            if (now >= until) {
                cooldowns.put(caster, now + BoundlessItemsConfig.ticks("lorentz.interval"));
                hurtVoid(
                        shooter,
                        target,
                        entity,
                        health * BoundlessItemsConfig.get("lorentz.voidRatio"));
            }
        }
        return result;
    }

    public static void hurtVoid(LivingEntity shooter, LivingEntity target, double amount) {
        hurtVoid(shooter, target, target, amount);
    }

    // 生命值采样、冷却和受伤状态恢复均以父实体为准。
    // 实际伤害仍传给原命中部位，保留该部位的阶段和受击规则。
    private static void hurtVoid(
            LivingEntity shooter, LivingEntity target, Entity hitEntity, double amount) {
        if (!(amount > 0)
                || !Double.isFinite(amount)
                || !enemy(shooter, target)
                || shooter.level() != target.level()
                || hitEntity.isRemoved()
                || hitEntity.level() != target.level()
                || livingTarget(hitEntity) != target) return;
        DamageSource source =
                new DamageSource(
                        shooter.level()
                                .registryAccess()
                                .registryOrThrow(Registries.DAMAGE_TYPE)
                                .getHolderOrThrow(VOID),
                        shooter);
        int invul = target.invulnerableTime;
        var state = (com.thelongtravail.mixin.DamageReplayStateAccessor) target;
        float last = state.travail$lastHurt();
        try {
            hitEntity.hurt(source, (float) Math.min(Float.MAX_VALUE, amount));
        } finally {
            target.invulnerableTime = invul;
            state.travail$lastHurt(last);
        }
    }

    private LorentzFantasy() {}
}
