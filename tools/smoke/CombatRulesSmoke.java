package travail.smoke;

import com.mojang.authlib.GameProfile;
import com.thelongtravail.TravailAspect;
import com.thelongtravail.config.TravailConfig;
import com.thelongtravail.data.LongTravailData;
import com.thelongtravail.data.VisualDeprivation;
import com.thelongtravail.helper.CombatContext;
import com.thelongtravail.helper.TrueDamage;
import com.thelongtravail.registry.ModRegistry;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.event.entity.living.*;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import java.util.*;

/** 通过真实 Forge 事件分发和变换后的 Player/LivingEntity 测试，仅由隔离审查服务器加载。 */
public final class CombatRulesSmoke {
    private static final class Victim extends FakePlayer {
        boolean fluid;
        int visualStarts;
        int doubleRolls;
        boolean fixedRandom;
        final net.minecraft.util.RandomSource testRandom = new net.minecraft.world.level.levelgen.LegacyRandomSource(1) {
            @Override public double nextDouble() { doubleRolls++; return 0.75; }
        };
        Victim(ServerLevel level, String name) {
            super(level, new GameProfile(UUID.randomUUID(), name));
            var wire = new net.minecraft.network.Connection(net.minecraft.network.protocol.PacketFlow.SERVERBOUND) {
                @Override public void send(net.minecraft.network.protocol.Packet<?> packet) { capture(packet); }
            };
            connection = new net.minecraft.server.network.ServerGamePacketListenerImpl(level.getServer(), wire, this) {
                @Override public void send(net.minecraft.network.protocol.Packet<?> packet) { capture(packet); }
            };
        }
        void capture(net.minecraft.network.protocol.Packet<?> packet) {
            if (packet instanceof net.minecraft.network.protocol.game.ClientboundCustomPayloadPacket payload
                    && payload.getIdentifier().toString().equals("the_long_travail:main")) {
                var data = new net.minecraft.network.FriendlyByteBuf(payload.getData().duplicate());
                if (data.readVarInt() == 4 && data.readVarInt() > 0) visualStarts++;
            }
        }
        @Override public boolean isInvulnerableTo(DamageSource source) { return false; }
        @Override public boolean canHarmPlayer(net.minecraft.world.entity.player.Player other) { return true; }
        @Override public boolean isInWaterOrBubble() { return fluid; }
        @Override public net.minecraft.util.RandomSource getRandom() {
            return fixedRandom ? testRandom : super.getRandom();
        }
    }

    private static final class External {
        Victim victim;
        String mode = "none";
        boolean nested;
        int hurtCalls;
        External(Victim victim) { this.victim = victim; }
        @SubscribeEvent(priority = EventPriority.HIGHEST)
        public void attack(LivingAttackEvent event) {
            if (event.getEntity() != victim) return;
            if (mode.equals("attackCancel")) event.setCanceled(true);
            if (mode.equals("throw")) throw new IllegalStateException("intentional combat fixture exception");
            if (mode.equals("nested") && !nested) {
                nested = true;
                victim.hurt(event.getSource(), 1);
                check(CombatContext.current(victim, event.getSource()) != null, "nested hit restores outer frame");
                event.setCanceled(true);
            }
        }
        @SubscribeEvent(priority = EventPriority.HIGHEST)
        public void hurt(LivingHurtEvent event) {
            if (event.getEntity() != victim) return;
            hurtCalls++;
            if (mode.equals("hurtCancel")) event.setCanceled(true);
            if (mode.equals("hurtZero")) event.setAmount(0);
            if (mode.equals("increase")) event.setAmount(5);
        }
        @SubscribeEvent(priority = EventPriority.LOWEST)
        public void damage(LivingDamageEvent event) {
            if (event.getEntity() != victim) return;
            if (mode.equals("damageCancel")) event.setCanceled(true);
            if (mode.equals("damageZero")) event.setAmount(0);
        }
    }

    private static void check(boolean ok, String message) { if (!ok) throw new AssertionError(message); }
    private static ItemStack equip(Victim player) {
        ItemStack diary = new ItemStack(ModRegistry.LONG_TRAVAIL.get());
        check(LongTravailData.tryInitialize(diary, player), "initialize combat diary");
        for (var aspect : TravailAspect.values()) LongTravailData.setWitness(diary, aspect, true);
        var inventory = top.theillusivec4.curios.api.CuriosApi.getCuriosInventory(player).resolve().orElseThrow();
        var slots = new top.theillusivec4.curios.common.inventory.CurioStacksHandler(inventory, "travel_diary", 1,
                true, false, true, top.theillusivec4.curios.api.type.capability.ICurio.DropRule.DEFAULT);
        inventory.setCurios(new HashMap<>(Map.of("travel_diary", slots)));
        slots.getStacks().setStackInSlot(0, diary);
        return diary;
    }
    private static void reset(Victim player) throws Exception {
        var protection = ServerPlayer.class.getDeclaredField("spawnInvulnerableTime");
        protection.setAccessible(true);
        protection.setInt(player, 0);
        player.invulnerableTime = 0;
        player.setHealth(20);
        player.setAbsorptionAmount(0);
        player.removeAllEffects();
        VisualDeprivation.clear(player);
        player.visualStarts = 0;
        player.addEffect(new MobEffectInstance(MobEffects.LUCK, 200));
    }
    private static void feedback(Victim player, boolean expected, String message) {
        check(player.hasEffect(MobEffects.LUCK) != expected && (player.visualStarts > 0) == expected, message);
    }
    private static List<DamageSource> abyssEnvironment(Victim player) {
        return List.of(player.damageSources().inWall(), player.damageSources().cramming(),
                player.damageSources().drown(), player.damageSources().freeze());
    }

    public static void run(ServerLevel level) throws Exception {
        var settings = List.<net.minecraftforge.common.ForgeConfigSpec.ConfigValue<?>>of(
                TravailConfig.ABYSS_CANCEL_CHANCE, TravailConfig.FAR_ALL_CLEAR_CHANCE,
                TravailConfig.VALLEY_VISUAL_DEPRIVATION_CHANCE, TravailConfig.FLOURISHING_DAMAGE_MULTIPLIER,
                TravailConfig.FLUID_EROSION_UNAVOIDABLE, TravailConfig.VALLEY_STIFF_CHANCE,
                TravailConfig.UNDERWORLD_DAMAGE_REDUCTION, TravailConfig.ABYSS_ENVIRONMENT_MULTIPLIER,
                TravailConfig.FLUID_PROTECTION, TravailConfig.RECEIVED_TRIGGER, TravailConfig.RECEIVED_COOLDOWN, TravailConfig.RECEIVED_EXCLUSIONS);
        var values = settings.stream().map(v -> v.get()).toList();
        Victim victim = new Victim(level, "CombatVictim");
        Victim attacker = new Victim(level, "CombatAttacker");
        ItemStack diary = equip(victim), attackerDiary = equip(attacker);
        LongTravailData.setWitness(diary, TravailAspect.FAR_REACH, false);
        LongTravailData.setWitness(diary, TravailAspect.DEEP_VALLEY, false);
        LongTravailData.setWitness(attackerDiary, TravailAspect.DEEP_VALLEY, false);
        External external = new External(victim);
        MinecraftForge.EVENT_BUS.register(external);
        DamageSource ordinary = victim.damageSources().generic();
        DamageSource fluid = new DamageSource(level.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(TrueDamage.FLUID));
        DamageSource playerHit = victim.damageSources().playerAttack(attacker);
        DamageSource playerFluid = new DamageSource(level.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(TrueDamage.FLUID), attacker);
        try {
            TravailConfig.ABYSS_CANCEL_CHANCE.set(1D);
            TravailConfig.FAR_ALL_CLEAR_CHANCE.set(1D);
            TravailConfig.VALLEY_VISUAL_DEPRIVATION_CHANCE.set(1D);
            TravailConfig.VALLEY_STIFF_CHANCE.set(1D);
            TravailConfig.FLOURISHING_DAMAGE_MULTIPLIER.set(2D);
            TravailConfig.FLUID_EROSION_UNAVOIDABLE.set(true);
            TravailConfig.FLUID_PROTECTION.set(TravailConfig.FluidProtection.ENFORCED);
            TravailConfig.RECEIVED_TRIGGER.set(TravailConfig.ReceivedTrigger.ATTACK_ATTEMPT);
            TravailConfig.RECEIVED_COOLDOWN.set(0);
            TravailConfig.RECEIVED_EXCLUSIONS.set(List.of());
            for (DamageSource environment : abyssEnvironment(victim)) {
                reset(victim);
                victim.hurt(environment, 1);
                check(victim.getHealth() == 20, "own immunity blocks " + environment.getMsgId());
                feedback(victim, false, "own immunity suppresses feedback for " + environment.getMsgId());
            }
            TravailConfig.ABYSS_ENVIRONMENT_MULTIPLIER.set(3D);
            LongTravailData.setWitness(diary, TravailAspect.ABYSS, false);
            for (DamageSource environment : abyssEnvironment(victim)) {
                reset(victim);
                // 每次命中读取当前倍率，避免配置并发重载使预期值失效。
                double multiplier = TravailConfig.ABYSS_ENVIRONMENT_MULTIPLIER.get();
                check(multiplier != 1D, "abyss malice multiplier is not neutral: " + multiplier);
                victim.hurt(environment, 1);
                check(Math.abs(victim.getHealth() - (20 - multiplier)) < 1.0E-4, "abyss malice amplifies "
                        + environment.getMsgId() + ": " + victim.getHealth() + " at x" + multiplier);
                feedback(victim, true, "abyss malice keeps feedback for " + environment.getMsgId());
            }
            LongTravailData.setWitness(diary, TravailAspect.ABYSS, true);
            victim.fluid = true;
            reset(victim);
            victim.hurt(fluid, 1);
            check(victim.getHealth() == 20, "own fluid immunity overrides true damage penetration");
            feedback(victim, false, "fluid immunity suppresses feedback");
            // 此场景不涉及诅咒随机判定，因此可直接统计免疫判定失败次数。
            LongTravailData.setWitness(diary, TravailAspect.FAR_REACH, true);
            LongTravailData.setWitness(diary, TravailAspect.DEEP_VALLEY, true);
            TravailConfig.ABYSS_CANCEL_CHANCE.set(0.5D);
            reset(victim);
            victim.fixedRandom = true;
            victim.doubleRolls = 0;
            victim.hurt(ordinary, 1);
            victim.fixedRandom = false;
            check(victim.getHealth() == 19 && victim.doubleRolls == 1, "failed fluid immunity rolls once across Attack/Hurt");
            LongTravailData.setWitness(diary, TravailAspect.FAR_REACH, false);
            LongTravailData.setWitness(diary, TravailAspect.DEEP_VALLEY, false);
            TravailConfig.ABYSS_CANCEL_CHANCE.set(1D);
            victim.fluid = false;
            for (String mode : List.of("attackCancel", "hurtCancel", "hurtZero", "damageCancel", "damageZero")) {
                external.mode = mode;
                reset(victim);
                victim.hurt(ordinary, 1);
                check(victim.getHealth() == 20, "ordinary respects " + mode);
                feedback(victim, true, "external " + mode + " preserves feedback");
                check(victim.visualStarts == 1, "one feedback per hit " + mode);
                LongTravailData.setWitness(diary, TravailAspect.FLOURISHING, false);
                reset(victim);
                victim.hurt(fluid, 1);
                check(victim.getHealth() == 18, "fluid protects own x2 at " + mode + ": " + victim.getHealth());
                check(victim.visualStarts == 1, "fluid feedback exactly once");
                LongTravailData.setWitness(diary, TravailAspect.FLOURISHING, true);
            }
            external.mode = "increase";
            reset(victim);
            LongTravailData.setWitness(diary, TravailAspect.FLOURISHING, false);
            victim.hurt(fluid, 1);
            check(victim.getHealth() == 15, "external increase retained without multiplying twice");
            LongTravailData.setWitness(diary, TravailAspect.FLOURISHING, true);
            external.mode = "hurtCancel";
            TravailConfig.FLUID_EROSION_UNAVOIDABLE.set(false);
            reset(victim);
            victim.hurt(fluid, 1);
            check(victim.getHealth() == 20, "penetration switch off");
            TravailConfig.FLUID_EROSION_UNAVOIDABLE.set(true);
            external.mode = "none";
            LongTravailData.setWitness(attackerDiary, TravailAspect.BOUNDLESS, false);
            attacker.getAbilities().flying = true;
            for (DamageSource source : List.of(playerHit, playerFluid)) {
                reset(victim); reset(attacker);
                victim.hurt(source, 1);
                check(victim.getHealth() == 20, "flying output forbidden, including fluid");
                feedback(victim, true, "attacker restriction does not exempt victim feedback");
                check(!attacker.hasEffect(ModRegistry.STIFF.get()), "blocked attack not successful");
            }
            attacker.getAbilities().flying = false;
            for (String mode : List.of("damageCancel", "damageZero", "none")) {
                external.mode = mode;
                reset(victim); reset(attacker);
                victim.hurt(playerHit, 1);
                check(attacker.hasEffect(ModRegistry.STIFF.get()) == mode.equals("none"), "success only after HP loss: " + mode);
            }
            external.mode = "none";
            reset(victim); reset(attacker);
            victim.setAbsorptionAmount(10);
            victim.hurt(playerHit, 1);
            check(!attacker.hasEffect(ModRegistry.STIFF.get()), "absorbed hit not successful");
            LongTravailData.setWitness(attackerDiary, TravailAspect.UNDERWORLD, false);
            TravailConfig.UNDERWORLD_DAMAGE_REDUCTION.set(0.5D);
            LongTravailData.setWitness(diary, TravailAspect.FLOURISHING, false);
            external.mode = "damageCancel";
            reset(victim); reset(attacker);
            victim.hurt(playerFluid, 2);
            check(victim.getHealth() == 18, "protected floor includes victim x2 and attacker x0.5 once");
            check(attacker.hasEffect(ModRegistry.STIFF.get()), "restored true damage counts as actual successful attack");
            LongTravailData.setWitness(diary, TravailAspect.FLOURISHING, true);
            external.mode = "none";
            TravailConfig.UNDERWORLD_DAMAGE_REDUCTION.set(1D);
            reset(victim);
            victim.hurt(playerFluid, 1);
            check(victim.getHealth() == 20, "internally approved zero not restored");
            LongTravailData.setWitness(attackerDiary, TravailAspect.UNDERWORLD, true);
            external.mode = "nested";
            reset(victim);
            victim.hurt(ordinary, 1);
            check(victim.getHealth() == 19 && victim.visualStarts == 2, "same-target same-source nested hits remain independent");
            check(CombatContext.current(victim, ordinary) == null, "outer scope removed");
            external.mode = "throw";
            reset(victim);
            try { victim.hurt(ordinary, 1); throw new AssertionError("fixture exception expected"); }
            catch (IllegalStateException expected) { check(expected.getMessage().contains("fixture"), "expected fixture exception"); }
            check(CombatContext.current(victim, ordinary) == null, "exception removes context");
            external.mode = "none";
            reset(victim);
            victim.hurt(ordinary, 1);
            check(victim.getHealth() == 19 && victim.visualStarts == 1, "damage works after exception");
            reset(attacker);
            var zombie = new net.minecraft.world.entity.monster.Zombie(level);
            zombie.hurt(playerHit, 1);
            check(attacker.hasEffect(ModRegistry.STIFF.get()), "non-player actual damage also awards successful hit");
            for (var policy : TravailConfig.FluidProtection.values()) {
                TravailConfig.FLUID_PROTECTION.set(policy);
                for (String mode : List.of("attackCancel", "hurtCancel", "damageCancel", "hurtZero", "damageZero", "none")) {
                    external.mode = mode;
                    reset(victim);
                    victim.hurt(fluid, 1);
                    boolean cancelled = mode.endsWith("Cancel");
                    boolean hurt = mode.equals("none") || policy == TravailConfig.FluidProtection.ENFORCED
                            || (policy == TravailConfig.FluidProtection.RESPECT_CANCELLATION && !cancelled);
                    check(victim.getHealth() == (hurt ? 19 : 20), "policy matrix " + policy + "/" + mode + ": " + victim.getHealth());
                }
            }
            TravailConfig.FLUID_PROTECTION.set(TravailConfig.FluidProtection.ENFORCED);
            TravailConfig.RECEIVED_TRIGGER.set(TravailConfig.ReceivedTrigger.HEALTH_LOSS);
            for (String mode : List.of("attackCancel", "hurtCancel", "damageCancel", "hurtZero", "damageZero", "none")) {
                external.mode = mode; reset(victim); victim.hurt(ordinary, 1);
                feedback(victim, mode.equals("none"), "HP trigger " + mode);
            }
            external.mode = "none"; reset(victim); victim.setAbsorptionAmount(10); victim.hurt(ordinary, 1);
            feedback(victim, false, "absorption is not health loss");
            TravailConfig.RECEIVED_TRIGGER.set(TravailConfig.ReceivedTrigger.ATTACK_ATTEMPT);
            TravailConfig.RECEIVED_EXCLUSIONS.set(List.of("minecraft:generic"));
            reset(victim); victim.hurt(ordinary, 1);
            feedback(victim, false, "excluded damage does not trigger malice");
            check(victim.getHealth() == 19, "exclusion does not suppress damage");
            TravailConfig.RECEIVED_EXCLUSIONS.set(List.of());
            TravailConfig.RECEIVED_COOLDOWN.set(20);
            com.thelongtravail.helper.ReceivedMalice.forget(victim);
            external.mode = "nested"; external.nested = false;
            reset(victim); victim.hurt(ordinary, 1);
            check(victim.visualStarts == 1, "nested hits share reserved cooldown");
            external.mode = "none"; reset(victim); victim.hurt(ordinary, 1);
            feedback(victim, false, "same-tick cooldown preserved");
            com.thelongtravail.helper.ReceivedMalice.forget(victim);
            reset(victim); victim.hurt(ordinary, 1);
            feedback(victim, true, "runtime reset clears cooldown");
            System.out.println("TRAVAIL_CONFIG_COMBAT_PASS: 18 policy cases, HP trigger, absorption, exclusions, nested cooldown");
            System.out.println("TRAVAIL_COMBAT_PASS: internal immunity, external cancellation/zeroing, multiplier floor, flying output, actual HP loss, nested hits, exception cleanup");
        } finally {
            MinecraftForge.EVENT_BUS.unregister(external);
            for (int i = 0; i < settings.size(); i++) restore(settings.get(i), values.get(i));
            VisualDeprivation.clear(victim); VisualDeprivation.clear(attacker);
        }
    }
    @SuppressWarnings({"rawtypes", "unchecked"})
    private static void restore(net.minecraftforge.common.ForgeConfigSpec.ConfigValue value, Object previous) { value.set(previous); }
}
