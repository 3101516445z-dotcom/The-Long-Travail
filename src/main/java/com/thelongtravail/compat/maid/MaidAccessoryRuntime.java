package com.thelongtravail.compat.maid;

import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.github.tartaricacid.touhoulittlemaid.api.event.MaidAndItemTransformEvent;
import com.github.tartaricacid.touhoulittlemaid.network.NetworkHandler;
import com.github.tartaricacid.touhoulittlemaid.network.message.SyncBaubleMessage;
import com.thelongtravail.TravailAspect;
import com.thelongtravail.registry.ModRegistry;
import com.thelongtravail.helper.TravailCurios;
import com.thelongtravail.data.LongTravailData;
import com.thelongtravail.config.*;
import com.thelongtravail.flourishing.*;
import com.thelongtravail.abyss.RainState;
import com.thelongtravail.farreach.IcarusEnvironment;
import com.thelongtravail.boundless.*;
import com.thelongtravail.valley.*;
import net.minecraft.server.level.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.item.*;
import net.minecraft.world.effect.*;
import net.minecraft.world.damagesource.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.tags.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.*;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.*;
import net.minecraftforge.event.entity.living.*;
import net.minecraftforge.eventbus.api.*;
import net.minecraftforge.registries.ForgeRegistries;
import top.theillusivec4.curios.api.event.CurioEquipEvent;
import java.util.*;

// 仅从女仆原生饰品栏读取装备；每次重新查询主人所佩戴苦旅的状态，不修改或缓存该状态。
public final class MaidAccessoryRuntime {
    public enum Phase {
        INACTIVE,
        MALICE,
        WITNESS
    }

    private static final String ID = "TravailMaidIdentity";
    private static final Map<EntityMaid, State> STATES = new WeakHashMap<>();
    private static final Map<LivingEntity, Map<String, Long>> EFFECT_TIMES = new WeakHashMap<>();
    private static final ArrayDeque<Runnable> PENDING = new ArrayDeque<>();
    private static final UUID HEALTH = UUID.fromString("193ab8d0-46cc-4179-bb32-f0317ed65401"),
            ATTACK = UUID.fromString("193ab8d0-46cc-4179-bb32-f0317ed65402"),
            SWIM = UUID.fromString("193ab8d0-46cc-4179-bb32-f0317ed65403"),
            SPEED = UUID.fromString("193ab8d0-46cc-4179-bb32-f0317ed65404"),
            EKI_SPEED = UUID.fromString("193ab8d0-46cc-4179-bb32-f0317ed65405");

    private static final class State {
        int heal, rainHeal;
        long rose = Long.MIN_VALUE, kill = Long.MIN_VALUE;
        boolean armed;
        Phase azrael = Phase.INACTIVE;
        UUID owner;
        long generation;
        CompoundTag lamps;
    }

    public static void init() {
        MinecraftForge.EVENT_BUS.register(new MaidAccessoryRuntime());
    }

    public static List<Item> items() {
        return MaidAccessories.items();
    }

    public static boolean isMaid(Entity e) {
        return e instanceof EntityMaid;
    }

    public static void watch(EntityMaid m) {
        if (!m.level().isClientSide) STATES.computeIfAbsent(m, k -> new State());
    }

    public static void equipmentChanged(EntityMaid m) {
        equipmentChanged(m, new ItemStack(ModRegistry.AZRAEL.get()));
        equipmentChanged(m, new ItemStack(ModRegistry.SWORD_AND_LANTERN.get()));
    }

    public static void equipmentChanged(EntityMaid m, ItemStack changed) {
        watch(m);
        var s = STATES.get(m);
        if (s != null && changed.is(ModRegistry.AZRAEL.get())) {
            s.armed = false;
            s.generation++;
        }
        if (changed.is(ModRegistry.SWORD_AND_LANTERN.get())) MaidLanternCombat.forget(m);
    }

    public static ItemStack equipped(EntityMaid m, Item item) {
        if (!m.isAlive() || m.isRemoved()) return ItemStack.EMPTY;
        var slots = m.getMaidBauble();
        for (int i = 0; i < slots.getSlots(); i++) {
            var stack = slots.getStackInSlot(i);
            if (stack.is(item)) return stack;
        }
        return ItemStack.EMPTY;
    }

    private static boolean has(EntityMaid m, Item item) {
        return !equipped(m, item).isEmpty();
    }

    public static ServerPlayer owner(EntityMaid m) {
        var server = m.getServer();
        var id = m.getOwnerUUID();
        var p = server == null || id == null ? null : server.getPlayerList().getPlayer(id);
        return p == null || p.hasDisconnected() ? null : p;
    }

    public static Phase phase(EntityMaid m, TravailAspect aspect) {
        var p = owner(m);
        if (p == null) return Phase.INACTIVE;
        var diary = TravailCurios.stack(p);
        if (diary.isEmpty()) return Phase.INACTIVE;
        return LongTravailData.hasWitness(diary, aspect) ? Phase.WITNESS : Phase.MALICE;
    }

    public static UUID identity(LivingEntity e) {
        var tag = e.getPersistentData();
        if (!tag.hasUUID(ID)) tag.putUUID(ID, UUID.randomUUID());
        return tag.getUUID(ID);
    }

    private static CompoundTag ledger(EntityMaid m) {
        return MaidAccessoryLedger.get(m.getServer()).entry(identity(m));
    }

    public static void starCooldown(LivingEntity e, int ticks) {
        var m = (EntityMaid) e;
        var tag = ledger(m);
        tag.putInt("StarCooldown", Math.max(ticks, tag.getInt("StarCooldown")));
        MaidAccessoryLedger.get(m.getServer()).setDirty();
    }

    public static boolean canHarm(LivingEntity attacker, Player other) {
        if (!(attacker instanceof EntityMaid m)) return false;
        var p = owner(m);
        return p != null && p != other && p.server.isPvpAllowed() && p.canHarmPlayer(other);
    }

    public static boolean lantern(Entity e) {
        return e instanceof EntityMaid m
                && LanternSources.enabled(equipped(m, ModRegistry.SWORD_AND_LANTERN.get()));
    }

    public static EntityMaid attacker(Entity source) {
        Set<Entity> seen = Collections.newSetFromMap(new IdentityHashMap<>());
        while (source != null && seen.add(source)) {
            if (source instanceof EntityMaid m) return m;
            if (source instanceof Projectile projectile) source = projectile.getOwner();
            else return null;
        }
        return null;
    }

    private static EntityMaid attacker(DamageSource source) {
        var m = attacker(source.getEntity());
        return m == null ? attacker(source.getDirectEntity()) : m;
    }

    private static ItemStack crown(EntityMaid m) {
        return equipped(m, ModRegistry.SPRING_GAME.get());
    }

    public static double swimBonus(EntityMaid m) {
        return FloralCombat.value(crown(m), Flower.BLUE_ORCHID);
    }

    private static boolean sunny(EntityMaid m) {
        return m.level().isDay() && m.level().canSeeSky(m.blockPosition());
    }

    public static Phase eki(EntityMaid m) {
        return has(m, ModRegistry.EKI.get())
                        && RainState.weather(
                                m,
                                RainConfig.EKI_WEATHER.get(),
                                RainConfig.EKI_LOCAL.get(),
                                RainConfig.EKI_SKY.get())
                ? phase(m, TravailAspect.ABYSS)
                : Phase.INACTIVE;
    }

    private static boolean tokaido(EntityMaid m) {
        return has(m, ModRegistry.TOKAIDO.get())
                && RainState.weather(
                        m,
                        RainConfig.TOKAIDO_WEATHER.get(),
                        RainConfig.TOKAIDO_LOCAL.get(),
                        RainConfig.TOKAIDO_SKY.get());
    }

    public static double bonus(LivingEntity victim, DamageSource source) {
        var m = attacker(source);
        if (m == null || m == victim || m.level().isClientSide) return 0;
        var crown = crown(m);
        double value = FloralCombat.value(crown, Flower.POPPY);
        if (m.getHealth()
                < m.getMaxHealth()
                        * FlourishingItemsConfig.extra(Flower.RED_TULIP, "healthThreshold"))
            value += FloralCombat.value(crown, Flower.RED_TULIP);
        if (source.is(DamageTypeTags.IS_PROJECTILE))
            value += FloralCombat.value(crown, Flower.CORNFLOWER);
        if (sunny(m)) value += FloralCombat.value(crown, Flower.SUNFLOWER);
        if (eki(m) == Phase.MALICE) value += RainConfig.DAMAGE.get();
        if (has(m, ModRegistry.ICARUS.get())) {
            var phase = phase(m, TravailAspect.FAR_REACH);
            if (phase == Phase.WITNESS && IcarusEnvironment.openSky(m))
                value += FarReachItemsConfig.get("icarus.witnessBonus");
            if (phase == Phase.MALICE)
                value +=
                        IcarusEnvironment.sunlight(m.level().getDayTime())
                                * FarReachItemsConfig.get(
                                        IcarusEnvironment.sunny(m.level())
                                                        && IcarusEnvironment.openSky(m)
                                                ? "icarus.sunnyPeak"
                                                : "icarus.malicePeak");
        }
        return value + MaidLanternCombat.bonus(m, victim);
    }

    public static boolean seesUndead(LivingEntity e) {
        return e instanceof EntityMaid m
                && has(m, ModRegistry.THOUSAND_YEARS.get())
                && phase(m, TravailAspect.UNDERWORLD) == Phase.WITNESS;
    }

    public static double witherBonus(LivingEntity victim, DamageSource source) {
        var m = attacker(source);
        if (m == null
                || !has(m, ModRegistry.THOUSAND_YEARS.get())
                || phase(m, TravailAspect.UNDERWORLD) != Phase.MALICE) return 0;
        var wither = victim.getEffect(MobEffects.WITHER);
        return wither == null
                ? 0
                : ((double) wither.getAmplifier() + 1) * UnderworldItemsConfig.WITHER_BONUS.get();
    }

    public static boolean immune(LivingEntity victim, DamageSource source, float amount) {
        if (!(victim instanceof EntityMaid m) || m.level().isClientSide) return false;
        if (has(m, ModRegistry.ICARUS.get())) {
            if (source.is(DamageTypeTags.IS_FALL) || source.is(DamageTypes.FLY_INTO_WALL))
                return true;
            if (phase(m, TravailAspect.FAR_REACH) == Phase.WITNESS) {
                if (source.is(DamageTypes.MAGIC) || source.is(DamageTypes.INDIRECT_MAGIC))
                    return true;
                for (String entry : FarReachItemsConfig.EXTRA_IMMUNITIES.get()) {
                    boolean tag = entry.startsWith("#");
                    var id = ResourceLocation.tryParse(tag ? entry.substring(1) : entry);
                    if (id != null
                            && (tag
                                    ? source.is(TagKey.create(Registries.DAMAGE_TYPE, id))
                                    : source.typeHolder()
                                            .unwrapKey()
                                            .map(k -> k.location().equals(id))
                                            .orElse(false))) return true;
                }
            }
        }
        if (source.is(DamageTypeTags.IS_FIRE) && RainConfig.FIRE.get() && tokaido(m)) return true;
        return amount > 0
                && !source.is(DamageTypes.GENERIC_KILL)
                && eki(m) == Phase.WITNESS
                && !RainConfig.IMMUNITY_EXCLUSIONS
                        .get()
                        .contains(
                                source.typeHolder()
                                        .unwrapKey()
                                        .map(k -> k.location().toString())
                                        .orElse(""))
                && m.getRandom().nextDouble() < RainConfig.IMMUNITY.get();
    }

    public static float reduce(
            LivingEntity victim, DamageSource source, float amount, boolean poison) {
        if (!(victim instanceof EntityMaid m) || FloralCombat.rose(source)) return amount;
        var crown = crown(m);
        double multiplier = 1;
        if (poison) multiplier *= 1 - FloralCombat.value(crown, Flower.ALLIUM);
        if (source.is(DamageTypeTags.IS_FALL))
            multiplier *= 1 - FloralCombat.value(crown, Flower.AZURE_BLUET);
        if (source.is(DamageTypeTags.IS_FIRE))
            multiplier *= 1 - FloralCombat.value(crown, Flower.TORCHFLOWER);
        if (m.getHealth()
                < m.getMaxHealth()
                        * FlourishingItemsConfig.extra(Flower.WHITE_TULIP, "healthThreshold"))
            multiplier *= 1 - FloralCombat.value(crown, Flower.WHITE_TULIP);
        return FloralCombat.safe(amount * multiplier);
    }

    private static State state(EntityMaid m) {
        watch(m);
        var s = STATES.get(m);
        var phase =
                has(m, ModRegistry.AZRAEL.get())
                        ? phase(m, TravailAspect.DEEP_VALLEY)
                        : Phase.INACTIVE;
        boolean changedOwner = !Objects.equals(s.owner, m.getOwnerUUID());
        if (phase != s.azrael || changedOwner) {
            s.armed = false;
            s.azrael = phase;
            s.owner = m.getOwnerUUID();
            s.generation++;
        }
        if (changedOwner) MaidLanternCombat.forget(m);
        return s;
    }

    public static void received(LivingEntity victim, DamageSource source) {
        var cause = attacker(source);
        if (victim instanceof EntityMaid m) {
            Entity entity =
                    source.getEntity() != null ? source.getEntity() : source.getDirectEntity();
            if (entity != null && entity != m && cause != m) {
                var s = state(m);
                if (s.azrael == Phase.MALICE) s.armed = true;
                rollStar(m, false);
            }
        }
        MaidLanternCombat.received(victim, source);
    }

    public static void confirmed(
            LivingEntity victim, DamageSource source, boolean transfer, boolean rose) {
        var cause = attacker(source);
        if (cause != null && cause != victim && !victim.isSpectator()) {
            rollStar(cause, true);
            if (!transfer && !rose) {
                if (has(cause, ModRegistry.ICARUS.get())
                        && ModeEffectRules.ICARUS_FIRE.applies(victim)
                        && !victim.fireImmune()
                        && FarReachItemsConfig.IGNITE_SECONDS.get() > 0)
                    victim.setSecondsOnFire(FarReachItemsConfig.IGNITE_SECONDS.get());
                azrael(cause, victim);
            }
        }
        if (!transfer && !rose) MaidLanternCombat.confirmed(victim, cause);
    }

    private static void azrael(EntityMaid m, LivingEntity victim) {
        if (AzraelExecution.active() || victim instanceof Player p && !canHarm(m, p)) return;
        var s = state(m);
        int outcome = 0;
        if (s.azrael == Phase.WITNESS)
            outcome =
                    AzraelState.witnessOutcome(
                            m.getRandom().nextDouble(),
                            AzraelConfig.TARGET.get(),
                            AzraelConfig.SELF.get());
        else if (s.azrael == Phase.MALICE && s.armed) {
            s.armed = false;
            outcome = m.getRandom().nextDouble() < AzraelConfig.MALICE.get() ? 1 : 0;
        }
        if (outcome == 0) return;
        var target = outcome == 2 ? m : victim;
        long generation = s.generation;
        var level = m.level();
        PENDING.add(
                () -> {
                    if (m.isAlive()
                            && !m.isRemoved()
                            && m.level() == level
                            && target.level() == level
                            && state(m) == s
                            && s.generation == generation) AzraelExecution.execute(m, target);
                });
    }

    private static void rollStar(EntityMaid m, boolean attacking) {
        if (!m.isAlive()
                || m.isRemoved()
                || TimeStopManager.frozen(m)
                || !has(m, ModRegistry.STAR_VOICE.get())
                || TimeStopManager.locked(identity(m))) return;
        var phase = phase(m, TravailAspect.BOUNDLESS);
        if (phase == Phase.INACTIVE || attacking != (phase == Phase.WITNESS)) return;
        boolean witness = phase == Phase.WITNESS;
        var tag = ledger(m);
        if (witness && (tag.getInt("StarCooldown") > 0 || tag.getInt("StarInterval") > 0)) return;
        if (!witness && tag.getInt("StarReceivedInterval") > 0) return;
        tag.putInt(
                witness ? "StarInterval" : "StarReceivedInterval",
                BoundlessItemsConfig.ticks(
                        witness ? "star.witnessInterval" : "star.maliceInterval"));
        MaidAccessoryLedger.get(m.getServer()).setDirty();
        if (m.getRandom().nextDouble()
                >= BoundlessItemsConfig.get(witness ? "star.witnessChance" : "star.maliceChance"))
            return;
        var p = owner(m);
        if (p == null) return;
        var allies = new HashSet<>(TimeStopExemptions.snapshot(p));
        allies.add(m.getUUID());
        TimeStopManager.get((ServerLevel) m.level())
                .startCaster(
                        m,
                        witness,
                        m.level().getGameTime(),
                        m.position(),
                        BoundlessItemsConfig.get("star.radius"),
                        BoundlessItemsConfig.ticks(
                                witness ? "star.witnessDuration" : "star.maliceDuration"),
                        allies);
    }

    public static void successful(LivingEntity victim, DamageSource source, boolean transfer) {
        if (transfer || FloralCombat.rose(source)) return;
        var m = attacker(source);
        if (m != null && m != victim) {
            apply(m, victim, Flower.LILY_OF_THE_VALLEY, MobEffects.POISON);
            apply(m, victim, Flower.WITHER_ROSE, MobEffects.WITHER);
        }
    }

    public static void retaliate(LivingEntity victim, DamageSource source) {
        if (victim instanceof EntityMaid defender
                && source.getDirectEntity() instanceof LivingEntity other
                && other != defender
                && !source.is(DamageTypes.THORNS)
                && !source.is(DamageTypeTags.IS_PROJECTILE)
                && !source.is(DamageTypeTags.IS_EXPLOSION)) {
            var s = state(defender);
            long now = defender.level().getGameTime();
            if (FlowerData.active(crown(defender), Flower.ROSE_BUSH)
                    && ready(now, s.rose, Flower.ROSE_BUSH)
                    && ModeEffectRules.FLOWER_THORNS.applies(other)) {
                s.rose = now;
                var damage =
                        new DamageSource(
                                defender.level()
                                        .registryAccess()
                                        .registryOrThrow(Registries.DAMAGE_TYPE)
                                        .getHolderOrThrow(FloralCombat.ROSE),
                                defender);
                other.hurt(
                        damage,
                        FloralCombat.safe(
                                defender.getMaxHealth()
                                        * FlourishingItemsConfig.value(Flower.ROSE_BUSH)));
            }
        }
    }

    private static boolean ready(long now, long last, Flower flower) {
        return last == Long.MIN_VALUE
                || now - last
                        >= FlourishingItemsConfig.ticks(
                                FlourishingItemsConfig.extra(flower, "cooldownSeconds"));
    }

    private static void apply(EntityMaid m, LivingEntity victim, Flower flower, MobEffect effect) {
        if (!ModeEffectRules.FLOWER_EFFECTS.applies(victim) || !FlowerData.active(crown(m), flower))
            return;
        long now = m.level().getGameTime();
        var times = EFFECT_TIMES.computeIfAbsent(victim, k -> new HashMap<>());
        times.entrySet().removeIf(e -> e.getValue() <= now);
        String key = identity(m) + ":" + flower.id;
        if (times.containsKey(key) || times.size() >= 256) return;
        times.put(
                key,
                now
                        + FlourishingItemsConfig.ticks(
                                FlourishingItemsConfig.extra(flower, "cooldownSeconds")));
        victim.addEffect(
                new MobEffectInstance(
                        effect,
                        FlourishingItemsConfig.ticks(
                                FlourishingItemsConfig.extra(flower, "durationSeconds")),
                        (int) FlourishingItemsConfig.value(flower) - 1),
                m);
    }

    public static MobEffectInstance extend(
            LivingEntity target, MobEffectInstance effect, Entity source) {
        var m = attacker(source);
        if (m == null
                || target.level().isClientSide
                || !ModeEffectRules.FLOWER_EFFECTS.applies(target)
                || effect.isInfiniteDuration()) return effect;
        var type = effect.getEffect();
        if (type != MobEffects.POISON
                && type != MobEffects.WITHER
                && type != MobEffects.MOVEMENT_SLOWDOWN
                && type != MobEffects.WEAKNESS) return effect;
        double value = FloralCombat.value(crown(m), Flower.LILAC);
        if (value <= 0) return effect;
        var copy = new MobEffectInstance(effect);
        ((com.thelongtravail.mixin.EffectDurationAccessor) copy)
                .travail$duration(
                        (int)
                                Math.min(
                                        Integer.MAX_VALUE,
                                        Math.ceil(effect.getDuration() * (1 + value))));
        return copy;
    }

    private static void attribute(EntityMaid m, Attribute attribute, UUID id, double value) {
        var instance = m.getAttribute(attribute);
        if (instance == null) return;
        var old = instance.getModifier(id);
        if (old != null && Double.compare(old.getAmount(), value) == 0) return;
        if (old != null) instance.removeModifier(id);
        if (value > 0)
            instance.addTransientModifier(
                    new AttributeModifier(
                            id,
                            "Travail maid bauble",
                            value,
                            AttributeModifier.Operation.MULTIPLY_TOTAL));
        if (attribute == Attributes.MAX_HEALTH && old != null)
            com.thelongtravail.compat.maidspell.MaidSpellHealth.clampAfterModifierRemoval(m);
    }

    public static void tick(EntityMaid m) {
        var s = state(m);
        var crown = crown(m);
        attribute(m, Attributes.MAX_HEALTH, HEALTH, FloralCombat.value(crown, Flower.PEONY));
        attribute(
                m, Attributes.ATTACK_SPEED, ATTACK, FloralCombat.value(crown, Flower.ORANGE_TULIP));
        attribute(
                m, ForgeMod.SWIM_SPEED.get(), SWIM, FloralCombat.value(crown, Flower.BLUE_ORCHID));
        attribute(
                m,
                Attributes.MOVEMENT_SPEED,
                SPEED,
                FloralCombat.value(crown, Flower.PINK_PETALS)
                        + (FlowerData.active(crown, Flower.SUNFLOWER) && sunny(m)
                                ? FlourishingItemsConfig.extra(Flower.SUNFLOWER, "speedBonus")
                                : 0));
        attribute(
                m,
                Attributes.MOVEMENT_SPEED,
                EKI_SPEED,
                eki(m) == Phase.MALICE ? RainConfig.SPEED.get() : 0);
        if (!has(m, ModRegistry.SWORD_AND_LANTERN.get())) MaidLanternCombat.forget(m);
        else
            for (var effect :
                    new MobEffect[] {
                        MobEffects.BLINDNESS, MobEffects.DARKNESS, MobEffects.CONFUSION
                    }) if (m.hasEffect(effect)) m.removeEffect(effect);
        CompoundTag lamps = null;
        var handler = m.getMaidBauble();
        for (int i = 0; i < handler.getSlots(); i++)
            if (handler.getStackInSlot(i).is(ModRegistry.SWORD_AND_LANTERN.get())) {
                if (lamps == null) lamps = new CompoundTag();
                lamps.put(Integer.toString(i), handler.getStackInSlot(i).save(new CompoundTag()));
            }
        if (!Objects.equals(lamps, s.lamps)) {
            s.lamps = lamps;
            NetworkHandler.sendToTrackingEntity(
                    SyncBaubleMessage.fullSync(m.getId(), handler.getSyncClientBauble(m)), m);
        }
        if (TimeStopManager.frozen(m)) return;
        if (FlowerData.active(crown, Flower.OXEYE_DAISY)) {
            if (++s.heal
                    >= FlourishingItemsConfig.ticks(
                            FlourishingItemsConfig.extra(Flower.OXEYE_DAISY, "intervalSeconds"))) {
                s.heal = 0;
                if (m.getHealth() < m.getMaxHealth())
                    m.heal((float) FlourishingItemsConfig.value(Flower.OXEYE_DAISY));
            }
        } else s.heal = 0;
        if (tokaido(m)) {
            if (++s.rainHeal >= RainConfig.ticks(RainConfig.HEAL_SECONDS.get())) {
                s.rainHeal = 0;
                if (m.getHealth() < m.getMaxHealth() && RainConfig.HEAL.get() > 0)
                    m.heal(RainConfig.HEAL.get().floatValue());
            }
        } else s.rainHeal = 0;
        // 未触发过星之声的女仆不需要持久化冷却记录。
        var persistent = m.getPersistentData();
        if (!persistent.hasUUID(ID)) return;
        var cooldownLedger = MaidAccessoryLedger.get(m.getServer());
        var data = cooldownLedger.find(persistent.getUUID(ID));
        if (data == null) return;
        boolean dirty = false;
        if (data.getInt("StarCooldown") > 0 && !TimeStopManager.locked(identity(m))) {
            data.putInt("StarCooldown", data.getInt("StarCooldown") - 1);
            dirty = true;
        }
        if (data.getInt("StarInterval") > 0) {
            data.putInt("StarInterval", data.getInt("StarInterval") - 1);
            dirty = true;
        }
        if (data.getInt("StarReceivedInterval") > 0) {
            data.putInt("StarReceivedInterval", data.getInt("StarReceivedInterval") - 1);
            dirty = true;
        }
        if (dirty) cooldownLedger.setDirty();
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void denyCurios(CurioEquipEvent e) {
        if (e.getEntity() instanceof EntityMaid
                && "the_long_travail"
                        .equals(
                                ForgeRegistries.ITEMS
                                        .getKey(e.getStack().getItem())
                                        .getNamespace())) e.setResult(Event.Result.DENY);
    }

    @SubscribeEvent
    public void join(EntityJoinLevelEvent e) {
        if (e.getEntity() instanceof EntityMaid m) watch(m);
    }

    @SubscribeEvent
    public void leave(EntityLeaveLevelEvent e) {
        if (!e.getLevel().isClientSide && e.getEntity() instanceof EntityMaid m) {
            STATES.remove(m);
            MaidLanternCombat.forget(m);
        }
    }

    @SubscribeEvent
    public void transformOut(MaidAndItemTransformEvent.ToItem e) {
        if (!e.getMaid().level().isClientSide) e.getData().putUUID(ID, identity(e.getMaid()));
    }

    @SubscribeEvent
    public void transformIn(MaidAndItemTransformEvent.ToMaid e) {
        if (e.getMaid().level().isClientSide || !e.getData().hasUUID(ID)) return;
        var id = e.getData().getUUID(ID);
        e.getMaid().getPersistentData().putUUID(ID, id);
        var forge = e.getData().getCompound("ForgeData");
        forge.putUUID(ID, id);
        e.getData().put("ForgeData", forge);
    }

    @SubscribeEvent
    public void heal(LivingHealEvent e) {
        if (e.getEntity() instanceof EntityMaid m && !m.level().isClientSide && e.getAmount() > 0)
            e.setAmount(
                    FloralCombat.safe(
                            e.getAmount() * (1 + FloralCombat.value(crown(m), Flower.PINK_TULIP))));
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void effect(MobEffectEvent.Applicable e) {
        if (e.getEntity() instanceof EntityMaid m && has(m, ModRegistry.SWORD_AND_LANTERN.get())) {
            var effect = e.getEffectInstance().getEffect();
            if (effect == MobEffects.DARKNESS
                    || effect == MobEffects.BLINDNESS
                    || effect == MobEffects.CONFUSION) e.setResult(Event.Result.DENY);
        }
    }

    @SubscribeEvent
    public void death(LivingDeathEvent e) {
        if (e.getEntity().level().isClientSide
                || !(e.getEntity() instanceof Enemy)
                || FloralCombat.transfer(e.getEntity(), e.getSource())) return;
        var m = attacker(e.getSource());
        if (m == null) return;
        double value = FloralCombat.value(crown(m), Flower.PITCHER_PLANT);
        if (value <= 0) return;
        PENDING.add(
                () -> {
                    if (e.isCanceled() || e.getEntity().isAlive() || !m.isAlive() || m.isRemoved())
                        return;
                    var s = state(m);
                    long now = m.level().getGameTime();
                    if (ready(now, s.kill, Flower.PITCHER_PLANT)) {
                        s.kill = now;
                        m.heal(FloralCombat.safe(m.getMaxHealth() * value));
                    }
                });
    }

    @SubscribeEvent
    public void serverTick(TickEvent.ServerTickEvent e) {
        if (e.phase != TickEvent.Phase.END) return;
        for (var m : List.copyOf(STATES.keySet())) {
            if (m.isRemoved() || !m.isAlive()) {
                STATES.remove(m);
                MaidLanternCombat.forget(m);
            } else tick(m);
        }
        int count = PENDING.size();
        while (count-- > 0 && !PENDING.isEmpty()) PENDING.removeFirst().run();
    }

    @SubscribeEvent
    public void stopped(net.minecraftforge.event.server.ServerStoppedEvent e) {
        STATES.clear();
        EFFECT_TIMES.clear();
        PENDING.clear();
        MaidLanternCombat.clear();
    }
}
