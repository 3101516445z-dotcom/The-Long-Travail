package travail.smoke;

import com.mojang.authlib.GameProfile;
import com.thelongtravail.TravailAspect;
import com.thelongtravail.combat.*;
import com.thelongtravail.data.LongTravailData;
import com.thelongtravail.farreach.SunChaser;
import com.thelongtravail.registry.*;
import com.thelongtravail.underworld.*;

import net.minecraft.server.level.*;
import net.minecraft.world.effect.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.entity.projectile.*;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.enchantment.*;
import net.minecraft.world.level.GameType;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.fml.common.Mod;

import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.type.capability.ICurio.DropRule;
import top.theillusivec4.curios.common.inventory.CurioStacksHandler;

import java.nio.file.*;
import java.util.*;

@Mod("travail_smoke")
public final class SunBladeSmoke {
    private int checks, failures, pendingRestart, integratedReadyTick;
    private ServerStartedEvent pendingIntegrated;
    private boolean realClient;
    private final List<String> failureMessages = new ArrayList<>();
    private ServerLevel level;
    private ServerPlayer player;
    private ItemStack diary;
    private CurioStacksHandler belt;
    private Cow replayTarget;
    private DamageReplayCases replayCases;
    private int replayStarted;
    private float environmentLoss, replayOwnedLoss, replayOwnedInput;
    private int replayHurtDepth;
    private int replayPlayerCalls, replayExtras;

    private void check(boolean value, String message) {
        checks++;
        if (!value) {
            if (Boolean.getBoolean("travail.sun.collect")
                    || Boolean.getBoolean("travail.sun.replayOnly")) {
                failures++;
                if (failureMessages.size() < 100) failureMessages.add(message);
            } else throw new AssertionError(message);
        }
    }

    private void near(double value, double expected, String message) {
        check(Math.abs(value - expected) < .001, message + ": " + value + " expected " + expected);
    }

    public SunBladeSmoke() {
        MinecraftForge.EVENT_BUS.addListener(this::run);
        MinecraftForge.EVENT_BUS.addListener(this::tick);
        MinecraftForge.EVENT_BUS.addListener(
                (net.minecraftforge.event.entity.living.MobEffectEvent.Added e) -> {
                    if (e.getEntity() == replayTarget)
                        replayLog("effect added=" + e.getEffectInstance());
                });
        MinecraftForge.EVENT_BUS.addListener(
                (net.minecraftforge.event.entity.living.MobEffectEvent.Remove e) -> {
                    if (e.getEntity() == replayTarget) replayLog("effect remove=" + e.getEffect());
                });
        MinecraftForge.EVENT_BUS.addListener(
                (net.minecraftforge.event.entity.living.MobEffectEvent.Expired e) -> {
                    if (e.getEntity() == replayTarget)
                        replayLog("effect expired=" + e.getEffectInstance());
                });
    }

    private void run(ServerStartedEvent event) {
        if (!event.getServer().isDedicatedServer() && !realClient) {
            pendingIntegrated = event;
            return;
        }
        String result;
        try {
            level = event.getServer().overworld();
            setup();
            if (Boolean.getBoolean("travail.sun.restart")) {
                restart(event);
                return;
            }
            if (Boolean.getBoolean("travail.sun.replayOnly")) {
                if (Boolean.getBoolean("travail.sun.acquisition")) acquisition();
                startReplay();
                return;
            }
            combat();
            new DamageChainCases(level, this::check).run();
            acquisition();
            if (level.getEntity(player.getUUID()) != player) level.addNewPlayer(player);
            new SunBladeExtreme(level, player, diary, belt, this::check).run();
            Files.write(Path.of("sun-blade-failures.txt"), failureMessages);
            Files.writeString(
                    Path.of("sun-blade-sync-result.txt"),
                    "checks=" + checks + " failures=" + failures);
            startReplay();
            return;
        } catch (Throwable error) {
            error.printStackTrace();
            result = "FAIL checks=" + checks + " " + error;
        }
        try {
            Files.writeString(Path.of("sun-blade-result.txt"), result);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        System.out.println("SUN_BLADE_" + result);
        event.getServer().halt(false);
    }

    private void replayLog(String message) {
        try {
            Files.writeString(
                    Path.of("sun-blade-replay-trace.txt"),
                    level.getServer().getTickCount() + " " + message + "\n",
                    StandardOpenOption.CREATE,
                    StandardOpenOption.APPEND);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    private void startReplay() {
        try {
            Files.writeString(Path.of("sun-blade-replay-trace.txt"), "");
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
        if (Boolean.getBoolean("travail.sun.replayOnly")) {
            level.setChunkForced(0, 0, true);
            player.teleportTo(level, .5, 100, .5, 0, 0);
            level.setBlockAndUpdate(
                    new net.minecraft.core.BlockPos(0, 99, 0),
                    net.minecraft.world.level.block.Blocks.STONE.defaultBlockState());
            player.setHealth(player.getMaxHealth());
        }
        player.getInventory().setItem(0, new ItemStack(ModRegistry.SUN_CHASER.get()));
        belt.getStacks().setStackInSlot(0, new ItemStack(ModRegistry.KAGOTSURUBE.get()));
        // 保留真实实体 tick；单独记账环境伤害，避免把火焰与凋零误算成玩家追加。
        replayTarget =
                new Cow(EntityType.COW, level) {
                    @Override
                    public void setHealth(float value) {
                        if (this == replayTarget && value > getHealth() && replayStarted > 0) {
                            replayLog(
                                    "health increase "
                                            + getHealth()
                                            + " -> "
                                            + value
                                            + " stack="
                                            + java.util.Arrays.toString(
                                                    Thread.currentThread().getStackTrace()));
                        }
                        super.setHealth(value);
                    }

                    @Override
                    public boolean hurt(
                            net.minecraft.world.damagesource.DamageSource source, float amount) {
                        float before = getHealth();
                        boolean root = replayHurtDepth++ == 0;
                        boolean owned = source.getEntity() == player;
                        if (owned) {
                            replayPlayerCalls++;
                            replayOwnedInput += amount;
                            if (SunChaser.strike(source))
                                near(amount, 25, "submitted fixed additional damage");
                            if (SunChaser.strike(source)) replayExtras++;
                        }
                        try {
                            return super.hurt(source, amount);
                        } finally {
                            replayHurtDepth--;
                            if (owned && root) replayOwnedLoss += before - getHealth();
                            if (!owned) environmentLoss += before - getHealth();
                            replayLog(
                                    "hurt owned="
                                            + owned
                                            + " source="
                                            + source
                                            + " input="
                                            + amount
                                            + " before="
                                            + before
                                            + " after="
                                            + getHealth()
                                            + " wither="
                                            + getEffect(MobEffects.WITHER)
                                            + " belt="
                                            + Kagotsurube.equipped(player));
                        }
                    }
                };
        replayTarget.setPos(2, 100, 0);
        replayTarget.setNoAi(true);
        replayTarget.setNoGravity(true);
        replayTarget.getAttribute(Attributes.MAX_HEALTH).setBaseValue(1000);
        replayTarget.setHealth(1000);
        replayTarget.setSecondsOnFire(100);
        level.addFreshEntity(replayTarget);
        var policy =
                new DamageReplay.Policy() {
                    public boolean valid(LivingEntity a, LivingEntity b) {
                        return true;
                    }

                    public boolean continueChain(LivingEntity a, LivingEntity b) {
                        return false;
                    }
                };
        DamageReplay.enqueue(
                player, replayTarget, player.damageSources().playerAttack(player), 10, policy);
        DamageReplay.enqueue(
                player, replayTarget, player.damageSources().playerAttack(player), 10, policy);
        replayStarted = level.getServer().getTickCount();
        replayLog(
                "enqueued player="
                        + player.position()
                        + " alive="
                        + player.isAlive()
                        + " targetRegistered="
                        + (level.getEntity(replayTarget.getUUID()) == replayTarget)
                        + " belt="
                        + Kagotsurube.equipped(player));
    }

    private void tick(net.minecraftforge.event.TickEvent.ServerTickEvent event) {
        if (pendingIntegrated != null
                && event.phase == net.minecraftforge.event.TickEvent.Phase.END
                && !event.getServer().getPlayerList().getPlayers().isEmpty()) {
            var candidate = event.getServer().getPlayerList().getPlayers().get(0);
            if (integratedReadyTick == 0)
                integratedReadyTick = event.getServer().getTickCount() + 40;
            if (event.getServer().getTickCount() < integratedReadyTick) return;
            if (CuriosApi.getCuriosInventory(candidate).resolve().isEmpty()) {
                System.out.println(
                        "SUN_REPLAY_NOT_READY alive="
                                + candidate.isAlive()
                                + " removed="
                                + candidate.isRemoved()
                                + " position="
                                + candidate.position());
                if (event.getServer().getTickCount() < integratedReadyTick + 200) return;
            }
            realClient = true;
            var ready = pendingIntegrated;
            pendingIntegrated = null;
            run(ready);
            return;
        }
        if (pendingRestart > 0
                && event.phase == net.minecraftforge.event.TickEvent.Phase.START
                && event.getServer().getTickCount() >= pendingRestart) {
            pendingRestart = 0;
            try {
                verifyRestart();
            } catch (Throwable ex) {
                try {
                    Files.writeString(Path.of("sun-blade-result.txt"), "FAIL restart " + ex);
                } catch (Exception ignored) {
                }
            }
            event.getServer().halt(false);
            return;
        }
        if (replayTarget != null && event.phase == net.minecraftforge.event.TickEvent.Phase.START) {
            int elapsed = event.getServer().getTickCount() - replayStarted;
            if (!realClient && !Boolean.getBoolean("travail.sun.replayOnly")) {
                if (elapsed == 5) {
                    replayCases = new DamageReplayCases(level, player, this::check);
                    replayCases.start();
                }
                if (replayCases != null) replayCases.tick();
            }
            if (elapsed < 10 || elapsed % 20 == 0)
                replayLog(
                        "tick elapsed="
                                + elapsed
                                + " player="
                                + player.position()
                                + " alive="
                                + player.isAlive()
                                + " targetRemoved="
                                + replayTarget.isRemoved()
                                + " targetRegistered="
                                + (level.getEntity(replayTarget.getUUID()) == replayTarget)
                                + " health="
                                + replayTarget.getHealth()
                                + " wither="
                                + replayTarget.getEffect(MobEffects.WITHER)
                                + " calls="
                                + replayPlayerCalls
                                + " extras="
                                + replayExtras
                                + " belt="
                                + Kagotsurube.equipped(player));
        }
        if (event.phase != net.minecraftforge.event.TickEvent.Phase.START
                || replayTarget == null
                || event.getServer().getTickCount() < replayStarted + 120) return;
        String result;
        try {
            if (replayCases != null) replayCases.verify();
            // 常规燃烧和凋零持续运行，归属仍保持四笔玩家伤害。
            near(replayOwnedInput, 70, "submitted two 10 replays and two 25 strikes");
            replayLog("owned actual loss=" + replayOwnedLoss + " submitted=" + replayOwnedInput);
            if (!realClient) near(replayOwnedLoss, 70, "isolated actual owned loss");
            if (!realClient)
                near(
                        replayTarget.getHealth(),
                        930 - environmentLoss,
                        "isolated final health accounting");
            check(
                    replayTarget.getEffect(MobEffects.WITHER) != null
                            && replayTarget.getEffect(MobEffects.WITHER).getDuration() >= 280
                            && replayTarget.getEffect(MobEffects.WITHER).getDuration() <= 283,
                    "replay confirmations extend wither exactly four times");
            check(
                    replayTarget.invulnerableTime == 0,
                    "replay finally does not restore suppressed cooldown");
            check(
                    replayPlayerCalls == 4 && replayExtras == 2,
                    "natural tick does not create attributed recursive damage");
            check(environmentLoss > 0, "natural fire and wither actually ran");
            if (Integer.getInteger("travail.sun.soakTicks", 0) > 0) {
                replayTarget.discard();
                replayTarget = null;
                new SunBladeSoak(
                        level, player, belt, Integer.getInteger("travail.sun.soakTicks", 36000));
                return;
            }
            result =
                    failures == 0
                            ? "PASS checks=" + checks
                            : "FAIL checks=" + checks + " failures=" + failures;
            Files.write(Path.of("sun-blade-failures.txt"), failureMessages);
        } catch (Throwable error) {
            error.printStackTrace();
            result = "FAIL checks=" + checks + " " + error;
        }
        replayTarget = null;
        try {
            Files.writeString(Path.of("sun-blade-result.txt"), result);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        System.out.println("SUN_BLADE_" + result);
        event.getServer().halt(false);
    }

    private void setup() {
        if (realClient) player = level.getServer().getPlayerList().getPlayers().get(0);
        else {
            player =
                    new ServerPlayer(
                            level.getServer(),
                            level,
                            new GameProfile(UUID.randomUUID(), "SunBladeTest"));
            var wire =
                    new net.minecraft.network.Connection(
                            net.minecraft.network.protocol.PacketFlow.SERVERBOUND) {
                        @Override
                        public void send(net.minecraft.network.protocol.Packet<?> packet) {}
                    };
            player.connection =
                    new net.minecraft.server.network.ServerGamePacketListenerImpl(
                            level.getServer(), wire, player) {
                        @Override
                        public void send(net.minecraft.network.protocol.Packet<?> packet) {}
                    };
        }
        player.setGameMode(GameType.SURVIVAL);
        player.setPos(.5, 100, .5);
        var inv = CuriosApi.getCuriosInventory(player).resolve().orElseThrow();
        var travel =
                new CurioStacksHandler(inv, "travel_diary", 1, true, false, true, DropRule.DEFAULT);
        belt = new CurioStacksHandler(inv, "belt", 1, true, false, true, DropRule.DEFAULT);
        inv.setCurios(new HashMap<>(Map.of("travel_diary", travel, "belt", belt)));
        diary = new ItemStack(ModRegistry.LONG_TRAVAIL.get());
        travel.getStacks().setStackInSlot(0, diary);
        for (var aspect : TravailAspect.values()) LongTravailData.setWitness(diary, aspect, true);
        player.getInventory().setItem(0, new ItemStack(ModRegistry.SUN_CHASER.get()));
        belt.getStacks().setStackInSlot(0, new ItemStack(ModRegistry.KAGOTSURUBE.get()));
        check(SunChaser.carrying(player), "carrying inventory");
        check(Kagotsurube.equipped(player), "belt functional");
    }

    private Cow cow() {
        Cow cow = new Cow(EntityType.COW, level);
        cow.setPos(2, 100, 0);
        cow.getAttribute(Attributes.MAX_HEALTH).setBaseValue(1000);
        cow.setHealth(1000);
        return cow;
    }

    private void hit(Cow cow, float amount) {
        cow.hurt(player.damageSources().playerAttack(player), amount);
    }

    private void combat() {
        check(!player.hurt(player.damageSources().lava(), 4), "lava immunity");
        player.setSecondsOnFire(20);
        check(!player.isOnFire(), "ignition prevented");
        var lavaPos = player.blockPosition();
        level.setBlockAndUpdate(
                lavaPos, net.minecraft.world.level.block.Blocks.LAVA.defaultBlockState());
        player.baseTick();
        check(SunChaser.lavaExempt(player), "lava grants fluid curse exemption");
        player.setHealth(10);
        player.heal(1);
        float referenceHealing = player.getHealth() - 10;
        System.out.println("SUN_HEAL_REFERENCE input=1 actual=" + referenceHealing);
        player.setHealth(10);
        player.tickCount = 20;
        SunChaser.tick(
                new net.minecraftforge.event.TickEvent.PlayerTickEvent(
                        net.minecraftforge.event.TickEvent.Phase.END, player));
        near(
                player.getHealth(),
                10 + referenceHealing,
                "lava matches ordinary one-point healing every twenty ticks");
        level.setBlockAndUpdate(
                lavaPos, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState());
        player.baseTick();
        check(!SunChaser.lavaExempt(player), "leaving lava removes curse exemption");
        Cow target = cow();
        target.setSecondsOnFire(20);
        player.getAttribute(ModAttributes.CRITICAL_CHANCE.get()).setBaseValue(1);
        hit(target, 10);
        near(target.getHealth(), 955, "critical main hit plus exact 25 direct strike");
        check(
                target.getEffect(MobEffects.WITHER).getDuration() == 200,
                "two real hits extend wither twice");
        check(
                SunChaser.suppressed(target) && target.invulnerableTime == 0,
                "suppression and zero cooldown");
        float before = target.getHealth();
        target.heal(100);
        near(target.getHealth(), before, "normal healing blocked");
        hit(target, 10);
        near(target.getHealth(), before - 45, "same tick next hit and one extra strike");
        player.getAttribute(ModAttributes.CRITICAL_CHANCE.get()).setBaseValue(0);
        target.getPersistentData()
                .putLong("the_long_travail:sun_suppression_until", level.getGameTime());
        target.heal(10);
        near(target.getHealth(), before - 35, "expired healing restored");
        Cow plain = cow();
        hit(plain, 10);
        near(plain.getHealth(), 990, "nonburning no extra strike");
        check(plain.getEffect(MobEffects.WITHER).getDuration() == 100, "base wither five seconds");
        Cow absorbed = cow();
        absorbed.setAbsorptionAmount(20);
        hit(absorbed, 10);
        check(
                absorbed.getHealth() == 1000 && absorbed.hasEffect(MobEffects.WITHER),
                "absorption loss confirms damage");
        Cow immune = cow();
        immune.setInvulnerable(true);
        hit(immune, 10);
        check(!immune.hasEffect(MobEffects.WITHER), "cancelled damage does not apply wither");
        Cow strong = cow();
        strong.addEffect(new MobEffectInstance(MobEffects.WITHER, 400, 3));
        hit(strong, 10);
        check(
                strong.getEffect(MobEffects.WITHER).getAmplifier() == 3
                        && strong.getEffect(MobEffects.WITHER).getDuration() == 500,
                "preserve high wither level");
        Cow infinite = cow();
        infinite.addEffect(new MobEffectInstance(MobEffects.WITHER, -1, 2));
        hit(infinite, 10);
        check(
                infinite.getEffect(MobEffects.WITHER).isInfiniteDuration(),
                "infinite wither retained");
        LongTravailData.setWitness(diary, TravailAspect.FAR_REACH, false);
        Cow burning = cow();
        burning.setSecondsOnFire(10);
        Arrow arrow = new Arrow(level, player);
        burning.hurt(player.damageSources().arrow(arrow, player), 10);
        near(
                burning.getHealth(),
                985,
                "projectile plus burning additive fifty percent after conversion");
        check(!SunChaser.suppressed(burning), "malice has no witness suppression");
        Cow ranged = cow();
        ranged.hurt(player.damageSources().arrow(arrow, player), 10);
        near(ranged.getHealth(), 987, "projectile thirty percent");
        Cow fire = cow();
        fire.setSecondsOnFire(10);
        hit(fire, 10);
        near(fire.getHealth(), 988, "burning twenty percent");
        Arrow incoming = new Arrow(level, cow());
        incoming.setDeltaMovement(1, .5, -.25);
        check(ProjectileReflection.reflect(incoming, player), "arrow reflected");
        near(incoming.getDeltaMovement().x, -1, "arrow velocity reversed");
        check(incoming.getOwner() == player, "reflection ownership");
        SmallFireball ball = new SmallFireball(level, cow(), 1, 0, 0);
        ball.setDeltaMovement(1, 0, 0);
        double power = ball.xPower;
        ProjectileReflection.reflect(ball, player);
        near(ball.xPower, -power, "fireball acceleration reversed");
        var homing = new ShulkerBullet(level, cow(), player, net.minecraft.core.Direction.Axis.Y);
        homing.setDeltaMovement(1, 0, 0);
        ProjectileReflection.reflect(homing, player);
        var saved = new net.minecraft.nbt.CompoundTag();
        homing.saveWithoutId(saved);
        check(!saved.hasUUID("Target"), "reflected shulker bullet clears former homing target");
        LongTravailData.setWitness(diary, TravailAspect.UNDERWORLD, false);
        Kagotsurube.refresh(player);
        near(CombatAttributes.insight(player), 15, "malice insight");
        near(CombatAttributes.overlord(player), .1, "malice overlord");
        Kagotsurube.refresh(player);
        near(CombatAttributes.insight(player), 15, "modifier idempotent");
        LongTravailData.setWitness(diary, TravailAspect.UNDERWORLD, true);
        Kagotsurube.refresh(player);
        near(CombatAttributes.insight(player), 0, "witness removes insight");
        near(CombatAttributes.overlord(player), 0, "witness removes overlord");
        var original = player.damageSources().arrow(arrow, player);
        var converted = Kagotsurube.convert(target, original);
        check(
                converted.is(DirectStrikeDamage.TYPE)
                        && DamageIdentity.original(converted) == original,
                "converted source preserves original instance");
        LongTravailData.setWitness(diary, TravailAspect.FAR_REACH, true);
        Cow defended = cow();
        defended.setSecondsOnFire(10);
        defended.getAttribute(ModAttributes.INSIGHT.get()).setBaseValue(5);
        hit(defended, 10);
        near(
                defended.getHealth(),
                975,
                "target Insight still reduces primary and additional direct strike");
        Cow armored = cow();
        armored.getAttribute(Attributes.ARMOR).setBaseValue(30);
        armored.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 200, 4));
        hit(armored, 10);
        near(armored.getHealth(), 990, "converted direct strike bypasses armor and resistance");
        try {
            DamageScope.hurt(
                    armored,
                    player.damageSources().playerAttack(player),
                    1,
                    (s, value) -> {
                        throw new IllegalStateException("fixture");
                    });
            throw new AssertionError("exception expected");
        } catch (IllegalStateException expected) {
            check(
                    !DamageScope.claimSun(player.getUUID()),
                    "exception closes additional damage scope");
        }
        Cow loop = cow();
        loop.setSecondsOnFire(10);
        for (int i = 0; i < 10; i++) hit(loop, 1);
        near(loop.getHealth(), 740, "ten same-tick hits each produce one strike without recursion");
        check(
                loop.getEffect(MobEffects.WITHER).getDuration() == 2000,
                "twenty finite damage confirmations");
        Cow retaliation = cow();
        retaliation.setSecondsOnFire(10);
        InsightRetaliation.hit(player, retaliation, 10);
        near(retaliation.getHealth(), 965, "player retaliation can trigger one sun strike");
        Cow quixote = cow();
        quixote.setSecondsOnFire(10);
        var special =
                new net.minecraft.world.damagesource.DamageSource(
                        level.registryAccess()
                                .registryOrThrow(
                                        net.minecraft.core.registries.Registries.DAMAGE_TYPE)
                                .getHolderOrThrow(
                                        com.thelongtravail.farreach.DonQuixoteCombat.STRIKE),
                        player);
        quixote.hurt(special, 10);
        near(
                quixote.getHealth(),
                965,
                "converted Quixote strike remains finite and triggers sun once");
        check(
                quixote.getEffect(MobEffects.WITHER).getDuration() == 200,
                "converted special and sun both count");
        Cow lorentz = cow();
        lorentz.setSecondsOnFire(10);
        com.thelongtravail.boundless.LorentzFantasy.hurtStrike(player, lorentz, 10);
        near(lorentz.getHealth(), 965, "Lorentz special triggers one sun strike");
        check(lorentz.invulnerableTime == 0, "Lorentz restoration respects suppression");
        var noOwner = target.damageSources().wither();
        check(Kagotsurube.convert(target, noOwner) == noOwner, "unattributed wither not converted");
        belt.getStacks().setStackInSlot(0, ItemStack.EMPTY);
        Kagotsurube.refresh(player);
        check(!Kagotsurube.equipped(player), "unequip disables blade");
        player.getInventory().setItem(0, ItemStack.EMPTY);
        check(!SunChaser.carrying(player), "removal disables carrying");
    }

    private void archaeology() {
        var params =
                new net.minecraft.world.level.storage.loot.LootParams.Builder(level)
                        .withParameter(
                                net.minecraft.world.level.storage.loot.parameters.LootContextParams
                                        .ORIGIN,
                                net.minecraft.world.phys.Vec3.ZERO)
                        .create(
                                net.minecraft.world.level.storage.loot.parameters
                                        .LootContextParamSets.ARCHAEOLOGY);
        for (String name :
                List.of(
                        "desert_pyramid",
                        "desert_well",
                        "ocean_ruin_cold",
                        "ocean_ruin_warm",
                        "trail_ruins_common",
                        "trail_ruins_rare")) {
            var table =
                    level.getServer()
                            .getLootData()
                            .getLootTable(
                                    new net.minecraft.resources.ResourceLocation(
                                            "minecraft", "archaeology/" + name));
            int hits = 0;
            for (int i = 0; i < 10000; i++) {
                var items = table.getRandomItems(params, 20261028L + i);
                if (!realClient)
                    check(
                            items.size() == 1 && items.get(0).getCount() == 1,
                            "archaeology single item " + name + ":" + i);
                if (items.stream().anyMatch(stack -> stack.is(ModRegistry.SUN_CHASER.get())))
                    hits++;
            }
            if (!realClient)
                check(
                        hits >= 390 && hits <= 610,
                        "archaeology 5% fixed seed tolerance " + name + " hits=" + hits);
            System.out.println("SUN_ARCHAEOLOGY " + name + " " + hits + "/10000");
        }
    }

    private void restart(ServerStartedEvent event) throws Exception {
        Path state = Path.of("sun-blade-restart-state.txt");
        if (!Files.exists(state)) {
            Cow t = cow();
            t.setPos(8, 100, 8);
            t.setNoAi(true);
            t.setNoGravity(true);
            level.addFreshEntity(t);
            level.setChunkForced(0, 0, true);
            t.addEffect(new MobEffectInstance(MobEffects.WITHER, 1000, 3));
            hit(t, 1);
            Files.writeString(state, t.getUUID().toString());
            Files.writeString(Path.of("sun-blade-result.txt"), "PASS restart prepared");
        } else {
            level.setChunkForced(0, 0, true);
            pendingRestart = event.getServer().getTickCount() + 40;
            return;
        }
        event.getServer().halt(false);
    }

    private void verifyRestart() throws Exception {

        var t =
                (Cow)
                        level.getEntity(
                                UUID.fromString(
                                        Files.readString(Path.of("sun-blade-restart-state.txt"))
                                                .trim()));
        check(t != null, "real restart restores saved entity");
        check(SunChaser.suppressed(t), "real restart preserves suppression deadline");
        check(
                t.getEffect(MobEffects.WITHER).getAmplifier() == 3,
                "real restart preserves wither amplifier");
        check(
                t.getEffect(MobEffects.WITHER).getDuration() > 1000,
                "real restart preserves extended duration");
        float hp = t.getHealth();
        t.heal(20);
        near(t.getHealth(), hp, "real restart still blocks normal heal");
        Files.writeString(
                Path.of("sun-blade-result.txt"), "PASS restart verified checks=" + checks);
    }

    private void acquisition() {
        archaeology();
        var menu =
                new AbstractContainerMenu(null, 0) {
                    @Override
                    public ItemStack quickMoveStack(
                            net.minecraft.world.entity.player.Player p, int i) {
                        return ItemStack.EMPTY;
                    }

                    @Override
                    public boolean stillValid(net.minecraft.world.entity.player.Player p) {
                        return true;
                    }
                };
        var grid = new TransientCraftingContainer(menu, 3, 3);
        var recipe =
                new KagotsurubeRecipe(
                        new net.minecraft.resources.ResourceLocation(
                                "the_long_travail", "kagotsurube"),
                        net.minecraft.world.item.crafting.CraftingBookCategory.EQUIPMENT);
        ItemStack book = new ItemStack(Items.ENCHANTED_BOOK);
        EnchantedBookItem.addEnchantment(
                book, new EnchantmentInstance(Enchantments.VANISHING_CURSE, 1));
        grid.setItem(1, book);
        grid.setItem(
                4,
                new ItemStack(ModRegistry.ESSENCES.get(TravailAspect.UNDERWORLD.ordinal()).get()));
        grid.setItem(6, new ItemStack(Items.WITHER_ROSE));
        grid.setItem(7, new ItemStack(Items.NETHERITE_SWORD));
        grid.setItem(8, new ItemStack(Items.WITHER_ROSE));
        check(recipe.matches(grid, level), "cursed book recipe");
        check(
                recipe.assemble(grid, level.registryAccess()).is(ModRegistry.KAGOTSURUBE.get()),
                "recipe result");
        int curses = 0;
        for (var enchantment : net.minecraftforge.registries.ForgeRegistries.ENCHANTMENTS)
            if (enchantment.isCurse()) {
                ItemStack mixed = new ItemStack(Items.ENCHANTED_BOOK);
                EnchantedBookItem.addEnchantment(mixed, new EnchantmentInstance(enchantment, 1));
                EnchantedBookItem.addEnchantment(
                        mixed, new EnchantmentInstance(Enchantments.UNBREAKING, 3));
                grid.setItem(1, mixed);
                check(
                        recipe.matches(grid, level),
                        "all registered curses and mixed books " + enchantment.getDescriptionId());
                curses++;
            }
        check(curses >= 2, "vanilla curses included");
        grid.setItem(1, new ItemStack(Items.ENCHANTED_BOOK));
        check(!recipe.matches(grid, level), "empty enchanted book rejected");
        grid.setItem(1, book);
        grid.setItem(0, new ItemStack(Items.STONE));
        check(!recipe.matches(grid, level), "extra ingredient rejected");
        var villager = new net.minecraft.world.entity.npc.Villager(EntityType.VILLAGER, level);
        villager.setVillagerData(
                villager.getVillagerData()
                        .setProfession(net.minecraft.world.entity.npc.VillagerProfession.FLETCHER)
                        .setLevel(5));
        var offers = new net.minecraft.world.item.trading.MerchantOffers();
        com.thelongtravail.loot.ArchaeologyTrades.ensure(villager, offers);
        check(
                offers.size() == 1 && offers.get(0).getResult().is(ModRegistry.SUN_CHASER.get()),
                "master fletcher backfill");
        com.thelongtravail.loot.ArchaeologyTrades.ensure(villager, offers);
        check(offers.size() == 1, "trade backfill idempotent");
        check(
                com.thelongtravail.loot.ArchaeologyTrades.fixed(offers.get(0)),
                "one Icarus plus one essence fixed cost");
        check(
                level.getRecipeManager()
                        .byKey(
                                new net.minecraft.resources.ResourceLocation(
                                        "the_long_travail", "kagotsurube"))
                        .isPresent(),
                "recipe loads from datapack");
    }
}
