package travail.smoke;

import com.mojang.authlib.GameProfile;
import com.thelongtravail.TravailAspect;
import com.thelongtravail.data.*;
import com.thelongtravail.helper.TrueDamage;
import com.thelongtravail.registry.ModRegistry;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.fml.common.Mod;
import java.util.ArrayList;
import java.util.UUID;

/** Isolated dev-server fixture, never packaged into the production mod. */
@Mod("travail_smoke")
public final class TravailSmoke {
    private static final net.minecraftforge.registries.DeferredRegister<net.minecraft.world.effect.MobEffect> EFFECTS =
            net.minecraftforge.registries.DeferredRegister.create(net.minecraftforge.registries.ForgeRegistries.MOB_EFFECTS, "travail_smoke");
    private static final java.util.List<net.minecraftforge.registries.RegistryObject<net.minecraft.world.effect.MobEffect>> PROTECTED = new ArrayList<>();
    private static int removalAttempts;
    static {
        for (int i = 0; i < 70; i++) PROTECTED.add(EFFECTS.register("protected_" + i,
                () -> new net.minecraft.world.effect.MobEffect(net.minecraft.world.effect.MobEffectCategory.HARMFUL, 0) {}));
    }
    public TravailSmoke() {
        EFFECTS.register(net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext.get().getModEventBus());
        MinecraftForge.EVENT_BUS.addListener(this::run);
        MinecraftForge.EVENT_BUS.addListener(this::cancelRemoval);
    }
    private void cancelRemoval(net.minecraftforge.event.entity.living.MobEffectEvent.Remove event) {
        if (PROTECTED.stream().anyMatch(e -> e.get() == event.getEffect())) { removalAttempts++; event.setCanceled(true); }
    }
    private static void check(boolean ok, String message) {
        if (!ok) throw new AssertionError(message);
    }
    private static void hotpath(net.minecraft.server.level.ServerPlayer player) throws Exception {
        RuntimePools.reload();
        var entries = java.util.List.of(new WeightedEntry(new net.minecraft.resources.ResourceLocation("minecraft", "stone"), "stone", 1),
                new WeightedEntry(new net.minecraft.resources.ResourceLocation("minecraft", "dirt"), "dirt", 3));
        var table = new WeightedTable<>(entries, WeightedEntry::weight);
        var oldRandom = net.minecraft.util.RandomSource.create(31); var newRandom = net.minecraft.util.RandomSource.create(31);
        for (int i = 0; i < 10000; i++) check(WeightedPicker.one(entries, WeightedEntry::weight, oldRandom).equals(table.choose(newRandom)), "weight distribution/seed unchanged");
        check(RuntimePools.current().maxLevels().size() > 0, "pool level snapshot");
        player.removeAllEffects();
        for (var effect : PROTECTED) player.addEffect(new MobEffectInstance(effect.get(), 1000));
        removalAttempts = 0;
        check(CleanseRotation.remove(player, 1) == 0 && removalAttempts == 32, "failed-removal budget");
        var queueField = CleanseRotation.class.getDeclaredField("QUEUES"); queueField.setAccessible(true);
        var queues = (java.util.Map<?,?>) queueField.get(null);
        var queue = (java.util.ArrayDeque<?>) queues.get(player); Object previousHead = queue.peekFirst();
        CleanseRotation.remove(player, 1);
        check(removalAttempts == 64 && previousHead != queue.peekFirst(), "fair rotation across failed rounds");
        check(player.getActiveEffects().size() == 70, "never bypass cancellation");
        CleanseRotation.forget(player);
        EffectChanges.add(player, new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 100));
        var ownershipField = EffectChanges.class.getDeclaredField("OWNED"); ownershipField.setAccessible(true);
        var ownership = (java.util.Map<?,?>) ownershipField.get(null);
        check(ownership.containsKey(player), "own gain tracked");
        player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 500, 2));
        check(!ownership.containsKey(player), "external potion cancels ownership");
        check(!EffectChanges.remove(player, PROTECTED.get(0).get(), false), "cancelled removal is not a transition");
        EffectChanges.add(player, new MobEffectInstance(MobEffects.JUMP, 1));
        player.getEffect(MobEffects.JUMP).tick(player, () -> {});
        player.removeEffectNoUpdate(MobEffects.JUMP); EffectChanges.tick(player);
        check(!ownership.containsKey(player), "owned expiry released");
        var diary = new ItemStack(ModRegistry.LONG_TRAVAIL.get());
        LongTravailData.initialize(diary, player);
        JourneyQueries.update(player, diary);
        var statesField = JourneyQueries.class.getDeclaredField("STATES"); statesField.setAccessible(true);
        Object state = ((java.util.Map<?,?>) statesField.get(null)).get(player);
        var nextRead = state.getClass().getDeclaredField("nextRead"); nextRead.setAccessible(true);
        long cachedDeadline = nextRead.getLong(state);
        JourneyQueries.update(player, diary);
        check(nextRead.getLong(state) == cachedDeadline, "stationary observation reused");
        player.setPos(player.getX() + 1, player.getY(), player.getZ()); JourneyQueries.update(player, diary);
        var position = state.getClass().getDeclaredField("pos"); position.setAccessible(true);
        check(position.get(state).equals(player.blockPosition()), "block movement invalidates cache");
        var root = diary.getTag().getCompound("LongTravail"); var requirements = root.getCompound("Requirements");
        for (var aspect : TravailAspect.values()) { var tag = requirements.getCompound(aspect.id());
            tag.put("Biomes", new net.minecraft.nbt.ListTag()); tag.put("Structures", new net.minecraft.nbt.ListTag()); }
        check(!LongTravailData.hasRemainingBiomes(diary) && LongTravailData.remainingStructures(diary).isEmpty(), "completed conditions stop querying");
        var newDiary = diary.copy(); newDiary.getTag().getCompound("LongTravail").putUUID("JourneyId", UUID.randomUUID());
        JourneyQueries.update(player, newDiary);
        var identity = state.getClass().getDeclaredField("diary"); identity.setAccessible(true);
        check(identity.get(state).equals(LongTravailData.queryIdentity(newDiary)), "replacement diary invalidates progress cache");
        var biomeField = state.getClass().getDeclaredField("biome"); biomeField.setAccessible(true);
        biomeField.set(state, new net.minecraft.resources.ResourceLocation("travail_smoke", "stale"));
        nextRead.setLong(state, player.level().getGameTime());
        JourneyQueries.update(player, newDiary);
        check(!biomeField.get(state).toString().equals("travail_smoke:stale"), "stationary TTL refreshes world observation");
        var newcomer = new net.minecraftforge.common.util.FakePlayer(player.serverLevel(), new GameProfile(UUID.randomUUID(), "NewTraveler"));
        newcomer.setPos(player.position());
        newDiary.getTag().getCompound("LongTravail").putInt("Witnesses", 63);
        check(PlayerJourneyData.discoveredBiomeCount(newcomer) == 0, "fresh personal journey");
        JourneyQueries.update(newcomer, newDiary);
        check(PlayerJourneyData.discoveredBiomeCount(newcomer) == 1, "all witnesses still discover personal biomes");
        JourneyQueries.update(newcomer, newDiary);
        check(PlayerJourneyData.discoveredBiomeCount(newcomer) == 1, "same biome counted only once");
        JourneyQueries.forget(newcomer);
        JourneyQueries.forget(player); EffectChanges.forget(player);
        System.out.println("TRAVAIL_HOTPATH_PASS: 10000 weighted draws, removal budget/fairness/cancellation, ownership/expiry, stationary/moving/replaced diary caches");
    }

    private void run(ServerStartedEvent event) {
        boolean passed = false;
        try {
            check(!Boolean.getBoolean("travail.smoke.forceFailure"), "intentional smoke failure-exit verification");
            // Fixtures mutate settings frequently. Do not autosave/reload intermediate broken pools.
            com.thelongtravail.config.TravailConfig.SPEC.setConfig(com.electronwill.nightconfig.core.CommentedConfig.inMemory());
            RuntimePools.reload();
            MobEffectInstance first = new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 200, 0, true, false, false);
            EffectUpgrade.apply(first, 2, 5);
            check(first.getAmplifier() == 2 && first.getDuration() == 200, "first effect input");
            check(first.isAmbient() && !first.isVisible() && !first.showIcon(), "effect flags");
            MobEffectInstance next = new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 400, 0);
            EffectUpgrade.apply(next, 2, 5); first.update(next);
            check(first.getAmplifier() == 2 && first.getDuration() == 400, "effect renewal");
            var stack = new ItemStack(ModRegistry.LONG_TRAVAIL.get());
            check(!stack.hasTag(), "fresh stack");
            for (var aspect : TravailAspect.values()) LongTravailData.hasWitness(stack, aspect);
            LongTravailData.requirementsForDisplay(stack, TravailAspect.ABYSS, false);
            check(!stack.hasTag(), "read-only NBT");
            ModRegistry.LONG_TRAVAIL.get().appendHoverText(stack, event.getServer().overworld(), new ArrayList<>(), TooltipFlag.NORMAL);
            ModRegistry.HOMECOMING.get().appendHoverText(new ItemStack(ModRegistry.HOMECOMING.get()), event.getServer().overworld(), new ArrayList<>(), TooltipFlag.NORMAL);
            var player = new net.minecraftforge.common.util.FakePlayer(event.getServer().overworld(),
                    new GameProfile(UUID.randomUUID(), "TravailSmoke")) {
                @Override public boolean isInvulnerableTo(net.minecraft.world.damagesource.DamageSource source) { return false; }
            };
            var spawnProtection = net.minecraft.server.level.ServerPlayer.class.getDeclaredField("spawnInvulnerableTime");
            spawnProtection.setAccessible(true);
            spawnProtection.setInt(player, 0);
            player.setHealth(20); player.setAbsorptionAmount(10);
            TrueDamage.hurtFluid(player, 2);
            check(Math.abs(player.getHealth() - 18) < 0.001F, "fluid bypasses absorption: health=" + player.getHealth());
            check(player.getAbsorptionAmount() == 10, "fluid preserves absorption");
            player.invulnerableTime = 0;
            player.hurt(player.damageSources().generic(), 2);
            check(player.getAbsorptionAmount() == 8 && player.getHealth() == 18, "ordinary damage still absorbs");
            player.invulnerableTime = 0;
            player.setHealth(1);
            player.setAbsorptionAmount(3);
            player.setItemInHand(net.minecraft.world.InteractionHand.OFF_HAND, new ItemStack(net.minecraft.world.item.Items.TOTEM_OF_UNDYING));
            TrueDamage.hurtFluid(player, 4);
            check(player.isAlive() && player.hasEffect(MobEffects.ABSORPTION) && player.getAbsorptionAmount() != 3,
                    "totem absorption not overwritten by old snapshot");
            var level = event.getServer().overworld();
            var spawn = level.getSharedSpawnPos();
            player.setPos(spawn.getX(), 100, spawn.getZ());
            var near = new net.minecraft.world.entity.monster.Phantom(net.minecraft.world.entity.EntityType.PHANTOM, level);
            var far = new net.minecraft.world.entity.monster.Phantom(net.minecraft.world.entity.EntityType.PHANTOM, level);
            var corner = new net.minecraft.world.entity.monster.Phantom(net.minecraft.world.entity.EntityType.PHANTOM, level);
            near.setPos(player.getX() + 2, 100, player.getZ());
            far.setPos(player.getX() + 40, 100, player.getZ());
            corner.setPos(player.getX() + 31, 100, player.getZ() + 31);
            for (var phantom : java.util.List.of(near, far, corner)) {
                level.getChunkAt(phantom.blockPosition()); // Fixture loads targets; production query must not.
                check(level.addFreshEntity(phantom), "spawn phantom fixture");
            }
            var kill = com.thelongtravail.event.TravailEvents.class.getDeclaredMethod("killNearbyPhantoms", net.minecraft.server.level.ServerPlayer.class);
            kill.setAccessible(true);
            kill.invoke(null, player);
            check(!near.isAlive() && far.isAlive() && corner.isAlive(), "local spherical phantom query");
            far.discard(); corner.discard();
            var speed = FlightSpeedAdjustment.apply(0.1F, 0.05F, 0.01F, 0.8D);
            check(Math.abs(speed.applied() - 0.02F) < 0.00001F, "external flight baseline");
            check(FlightSpeedAdjustment.restore(0.15F, speed.baseline(), speed.applied()) == 0.15F, "external flight restore");
            var zero = FlightSpeedAdjustment.apply(0.05F, 0.05F, 0.05F, 1D);
            check(zero.applied() == 0F && FlightSpeedAdjustment.restore(0F, zero.baseline(), 0F) == 0.05F, "full flight reduction");
            hotpath(player);
            RenewalSmoke.run(level);
            ReviewRegressionSmoke.run(level);
            AltitudeCacheSmoke.run(level);
            GlidingSlowdownSmoke.run(level);
            CombatRulesSmoke.run(level);
            RewardDeliverySmoke.run(level);
            JourneyBudgetSmoke.run(level);
            StateApiSmoke.run(level);
            WitnessSoundSmoke.run(level);
            MaliceRemovalSmoke.run(level);
            LevelBonusBlacklistSmoke.run(level);
            passed = true;
            System.out.println("TRAVAIL_SMOKE_PASS: effect merge, read-only NBT, dedicated tooltips, fluid/normal absorption, totem, phantom radius, flight ownership");
        } catch (Throwable error) {
            System.out.println("TRAVAIL_SMOKE_FAIL");
            error.printStackTrace();
        } finally {
            try {
                java.nio.file.Files.writeString(java.nio.file.Path.of(System.getProperty("travail.smoke.result", "smoke-result.txt")),
                        passed ? "PASS" : "FAIL");
            } catch (java.io.IOException error) {
                throw new java.io.UncheckedIOException(error);
            } finally { event.getServer().halt(false); }
        }
    }
}
