package travail.smoke;

import com.mojang.authlib.GameProfile;
import com.thelongtravail.TravailAspect;
import com.thelongtravail.valley.*;
import com.thelongtravail.config.*;
import com.thelongtravail.data.LongTravailData;
import com.thelongtravail.helper.*;
import com.thelongtravail.registry.ModRegistry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.*;
import net.minecraft.world.damagesource.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.item.*;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.event.entity.living.*;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import top.theillusivec4.curios.api.*;
import top.theillusivec4.curios.api.type.capability.ICurio.DropRule;
import top.theillusivec4.curios.common.inventory.CurioStacksHandler;
import java.util.*;

@Mod("travail_smoke")
public final class AzraelSmoke {
    private static void check(boolean ok, String message) { if (!ok) throw new AssertionError(message); }
    private static final class TestPlayer extends ServerPlayer {
        TestPlayer(ServerLevel level) {
            super(level.getServer(), level, new GameProfile(UUID.randomUUID(), "AzraelTest"));
            var wire = new net.minecraft.network.Connection(net.minecraft.network.protocol.PacketFlow.SERVERBOUND) {
                @Override public void send(net.minecraft.network.protocol.Packet<?> packet) {}
            };
            connection = new net.minecraft.server.network.ServerGamePacketListenerImpl(level.getServer(), wire, this) {
                @Override public void send(net.minecraft.network.protocol.Packet<?> packet) {}
            };
        }
    }
    private static final class Probe {
        LivingEntity watched;
        int hits, deaths, drops, resurrect, totemsUsed, totemsDropped;
        boolean cancelDamage, blockExecution, cancelDeath, sawPlayer, cancelSelfExecution;
        DamageSource lastDeath;
        @SubscribeEvent public void hurt(LivingAttackEvent event) {
            if (event.getEntity() != watched) return;
            hits++;
            if (cancelDamage || blockExecution && event.getSource().is(AzraelExecution.TYPE)
                    || cancelSelfExecution && event.getSource().is(AzraelExecution.SELF)) event.setCanceled(true);
        }
        @SubscribeEvent public void death(LivingDeathEvent event) {
            if (event.getEntity() != watched) return;
            deaths++; lastDeath = event.getSource();
            if (cancelDeath || resurrect-- > 0) { event.setCanceled(true); event.getEntity().setHealth(10); }
        }
        @SubscribeEvent public void drops(LivingDropsEvent event) {
            if (event.getEntity() != watched) return;
            drops++; sawPlayer = event.isRecentlyHit() && event.getSource().getEntity() instanceof ServerPlayer;
            totemsDropped += event.getDrops().stream().filter(e -> e.getItem().is(Items.TOTEM_OF_UNDYING))
                    .mapToInt(e -> e.getItem().getCount()).sum();
        }
        @SubscribeEvent public void totem(LivingUseTotemEvent event) { if (event.getEntity() == watched) totemsUsed++; }
    }
    private Cow tickVictim;
    private TestPlayer tickPlayer;
    private int observedTicks;
    public AzraelSmoke() {
        MinecraftForge.EVENT_BUS.addListener(this::run);
        MinecraftForge.EVENT_BUS.addListener(this::verifyServerTick);
    }
    private void verifyServerTick(net.minecraftforge.event.TickEvent.ServerTickEvent event) {
        if (event.phase != net.minecraftforge.event.TickEvent.Phase.START || tickVictim == null || observedTicks++ == 0) return;
        boolean passed = false;
        try {
            check(!tickVictim.isAlive() && pending() == 0, "real server END tick drains queued executions");
            System.out.println("AZRAEL_REAL_SERVER_TICK_PASS");
            System.out.println("AZRAEL_SMOKE_PASS"); passed = true;
        } catch (Throwable failure) { failure.printStackTrace(); }
        finally {
            AzraelState.forget(tickPlayer); tickVictim = null;
            finishServer(event.getServer(), passed);
        }
    }
    private static void finishServer(net.minecraft.server.MinecraftServer server, boolean passed) {
        AzraelConfig.MALICE.set(.025); AzraelConfig.TARGET.set(.005); AzraelConfig.SELF.set(.005);
        try { java.nio.file.Files.writeString(java.nio.file.Path.of("azrael-result.txt"), passed ? "PASS" : "FAIL"); }
        catch (Exception error) { throw new RuntimeException(error); }
        finally { server.halt(false); }
    }
    private static Cow cow(ServerLevel level) {
        Cow cow = new Cow(EntityType.COW, level); cow.setPos(0, 80, 0); level.addFreshEntity(cow); return cow;
    }
    private static void clean(TestPlayer player) throws Exception {
        var spawn = ServerPlayer.class.getDeclaredField("spawnInvulnerableTime"); spawn.setAccessible(true); spawn.setInt(player, 0);
        player.invulnerableTime = 0; player.setHealth(20); player.setAbsorptionAmount(0); player.removeAllEffects();
    }
    private static ItemStack equipment(TestPlayer player) {
        var inv = CuriosApi.getCuriosInventory(player).resolve().orElseThrow();
        Map<String, top.theillusivec4.curios.api.type.inventory.ICurioStacksHandler> slots = new HashMap<>();
        for (String id : List.of("travel_diary", "head", "feet", "body"))
            slots.put(id, new CurioStacksHandler(inv, id, 2, true, false, true, DropRule.DEFAULT));
        inv.setCurios(slots);
        ItemStack diary = new ItemStack(ModRegistry.LONG_TRAVAIL.get());
        LongTravailData.initialize(diary, player);
        for (TravailAspect aspect : TravailAspect.values()) LongTravailData.setWitness(diary, aspect, true);
        inv.getStacksHandler("travel_diary").orElseThrow().getStacks().setStackInSlot(0, diary);
        inv.getStacksHandler("head").orElseThrow().getStacks().setStackInSlot(0, new ItemStack(ModRegistry.AZRAEL.get()));
        return diary;
    }
    private static void execution(ServerLevel level, TestPlayer player) {
        Probe probe = new Probe(); MinecraftForge.EVENT_BUS.register(probe);
        try {
            Cow ordinary = cow(level); probe.watched = ordinary;
            ordinary.invulnerableTime = 20;
            ordinary.setAbsorptionAmount(10);
            ordinary.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.TOTEM_OF_UNDYING));
            ordinary.setDropChance(EquipmentSlot.MAINHAND, 2F);
            ordinary.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.DAMAGE_RESISTANCE, 100, 4));
            check(AzraelExecution.execute(player, ordinary) == AzraelExecution.Result.DEAD, "first layer death");
            check(probe.hits == 1 && probe.deaths == 1 && probe.drops == 1 && probe.sawPlayer, "first layer one death/drop with player credit");
            check(probe.totemsUsed == 0 && probe.totemsDropped == 1 && Float.isFinite(ordinary.getAbsorptionAmount()),
                    "totem not consumed, drops normally, and absorption stays finite");
            Cow second = cow(level); probe.watched = second; probe.hits = probe.deaths = probe.drops = 0; probe.blockExecution = true;
            check(AzraelExecution.execute(player, second) == AzraelExecution.Result.DEAD, "generic kill fallback");
            check(probe.hits == 11 && probe.deaths == 1 && probe.lastDeath.is(DamageTypes.GENERIC_KILL) && probe.sawPlayer, "ten plus one with source");
            Cow third = cow(level); probe.watched = third; probe.hits = probe.deaths = probe.drops = 0; probe.cancelDamage = true;
            check(AzraelExecution.execute(player, third) == AzraelExecution.Result.DEAD, "direct death fallback");
            check(probe.hits == 11 && probe.deaths == 1 && probe.drops == 1 && third.getKillCredit() == player, "direct death credit and no duplicate loot");
            Cow protectedCow = cow(level); probe.watched = protectedCow; probe.hits = probe.deaths = 0; probe.cancelDeath = true;
            check(AzraelExecution.execute(player, protectedCow) == AzraelExecution.Result.SURVIVED && protectedCow.isAlive(), "cancelled death survives");
            check(probe.hits == 11 && probe.deaths == 1, "bounded attempts against cancellation");
            Cow revived = cow(level); probe.watched = revived; probe.hits = probe.deaths = 0; probe.cancelDeath = probe.cancelDamage = probe.blockExecution = false; probe.resurrect = 2;
            var revivedResult = AzraelExecution.execute(player, revived);
            check(revivedResult == AzraelExecution.Result.DEAD && probe.hits == 3 && probe.deaths == 3,
                    "retry after two revivals: " + revivedResult + " hits=" + probe.hits + " deaths=" + probe.deaths);
        } finally { MinecraftForge.EVENT_BUS.unregister(probe); }
    }
    private static void reviewChecks(ServerLevel level, TestPlayer player) throws Exception {
        Probe probe = new Probe(); MinecraftForge.EVENT_BUS.register(probe);
        ItemStack previousHand = player.getMainHandItem();
        boolean previousPvp = level.getServer().isPvpAllowed();
        List<String> failures = new ArrayList<>();
        try {
            player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,
                    new ItemStack(Items.DIAMOND_SWORD).setHoverName(net.minecraft.network.chat.Component.literal("Named weapon")));
            for (int layer = 1; layer <= 3; layer++) {
                Cow victim = cow(level); probe.watched = victim;
                probe.blockExecution = layer >= 2; probe.cancelDamage = layer == 3;
                check(AzraelExecution.execute(player, victim) == AzraelExecution.Result.DEAD, "review death layer " + layer);
                var message = probe.lastDeath.getLocalizedDeathMessage(victim).getContents();
                if (!(message instanceof net.minecraft.network.chat.contents.TranslatableContents c)
                        || !c.getKey().equals("death.attack.the_long_travail.azrael_execution") || c.getArgs().length != 1)
                    failures.add("death message layer " + layer + ": " + message);
            }
            TestPlayer protectedPlayer = new TestPlayer(level); clean(protectedPlayer);
            level.getServer().setPvpAllowed(false);
            var result = AzraelExecution.execute(player, protectedPlayer);
            if (result != AzraelExecution.Result.INVALID || protectedPlayer.getHealth() != 20)
                failures.add("execution must recheck disabled PvP: " + result);
            level.getServer().setPvpAllowed(true);
            var scoreboard = level.getScoreboard();
            var team = scoreboard.addPlayerTeam("azrael_review");
            try {
                team.setAllowFriendlyFire(false);
                scoreboard.addPlayerToTeam(player.getScoreboardName(), team);
                scoreboard.addPlayerToTeam(protectedPlayer.getScoreboardName(), team);
                check(AzraelExecution.execute(player, protectedPlayer) == AzraelExecution.Result.INVALID
                        && protectedPlayer.getHealth() == 20, "execution rechecks team friendly fire");
            } finally { scoreboard.removePlayerTeam(team); }
            level.getServer().setPvpAllowed(false);
            // 关闭 PvP 后仍须允许反噬处决自己，包括使用 generic_kill 的兜底路径。
            TestPlayer self = new TestPlayer(level); clean(self);
            probe.watched = self; probe.cancelDamage = false; probe.blockExecution = false;
            probe.cancelSelfExecution = true;
            check(AzraelExecution.execute(self, self) == AzraelExecution.Result.DEAD, "self execution ignores disabled PvP");
            var selfMessage = probe.lastDeath.getLocalizedDeathMessage(self).getContents();
            if (!(selfMessage instanceof net.minecraft.network.chat.contents.TranslatableContents c)
                    || !c.getKey().equals("death.attack.the_long_travail.azrael_self") || c.getArgs().length != 1)
                failures.add("self fallback death message: " + selfMessage);
        } finally {
            player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, previousHand);
            level.getServer().setPvpAllowed(previousPvp);
            MinecraftForge.EVENT_BUS.unregister(probe);
        }
        check(failures.isEmpty(), String.join("; ", failures));
        System.out.println("AZRAEL_REVIEW_PASS");
    }
    private static void mechanics(ServerLevel level, TestPlayer player, ItemStack diary) throws Exception {
        check(AzraelState.witnessOutcome(0, .005, .005) == 1, "target interval");
        check(AzraelState.witnessOutcome(.005, .005, .005) == 2, "self interval");
        check(AzraelState.witnessOutcome(.01, .005, .005) == 0, "none interval");
        check(AzraelState.witnessOutcome(.8, .8, .8) == 2, "oversubscribed probabilities normalized");
        LongTravailData.setWitness(diary, TravailAspect.DEEP_VALLEY, false);
        AzraelConfig.MALICE.set(1D);
        Cow attacker = cow(level), target = cow(level);
        clean(player); player.hurt(level.damageSources().mobAttack(attacker), 1);
        target.hurt(level.damageSources().playerAttack(player), 1);
        AzraelState.drain(level.getServer());
        check(!target.isAlive(), "actual sourced damage arms next attack");
        Cow next = cow(level); next.hurt(level.damageSources().playerAttack(player), 1); AzraelState.drain(level.getServer());
        check(next.isAlive(), "one charge consumed");
        clean(player); player.setAbsorptionAmount(5); player.hurt(level.damageSources().mobAttack(attacker), 1);
        Cow absorbed = cow(level); absorbed.hurt(level.damageSources().playerAttack(player), 1); AzraelState.drain(level.getServer());
        check(!absorbed.isAlive(), "absorption damage arms charge");
        AzraelState.forget(player); clean(player); player.hurt(level.damageSources().fall(), 1);
        Cow fall = cow(level); fall.hurt(level.damageSources().playerAttack(player), 1); AzraelState.drain(level.getServer());
        check(fall.isAlive(), "environmental damage cannot arm");
        AzraelConfig.TARGET.set(0D); AzraelConfig.SELF.set(1D);
        LongTravailData.setWitness(diary, TravailAspect.DEEP_VALLEY, true);
        Probe probe = new Probe(); probe.watched = player; probe.cancelDeath = true; MinecraftForge.EVENT_BUS.register(probe);
        try {
            Cow witness = cow(level); clean(player); witness.hurt(level.damageSources().playerAttack(player), 1); AzraelState.drain(level.getServer());
            check(witness.isAlive() && probe.deaths == 12, "self only, at most 10+1+1 attempts, no recursion: deaths=" + probe.deaths + " targetAlive=" + witness.isAlive());
        } finally { MinecraftForge.EVENT_BUS.unregister(probe); AzraelState.forget(player); }
    }
    private static int pending() throws Exception {
        var field = AzraelState.class.getDeclaredField("REQUESTS"); field.setAccessible(true);
        return ((Deque<?>) field.get(null)).size();
    }
    private static void damageTriggers(ServerLevel level, TestPlayer player, ItemStack diary) throws Exception {
        LongTravailData.setWitness(diary, TravailAspect.DEEP_VALLEY, false);
        AzraelState.forget(player); AzraelConfig.MALICE.set(1D);
        Cow attacker = cow(level);
        // 多次受伤只保留一次机会，攻击判定未成功时不消耗机会。
        for (int i = 0; i < 3; i++) { clean(player); player.hurt(level.damageSources().mobAttack(attacker), 1); }
        Probe probe = new Probe(); probe.cancelDamage = true; probe.watched = cow(level);
        MinecraftForge.EVENT_BUS.register(probe);
        try { probe.watched.hurt(level.damageSources().playerAttack(player), 1); }
        finally { MinecraftForge.EVENT_BUS.unregister(probe); }
        Cow invulnerable = cow(level); invulnerable.hurt(level.damageSources().generic(), 2);
        invulnerable.hurt(level.damageSources().playerAttack(player), 1);
        cow(level).hurt(level.damageSources().playerAttack(player), 0);
        check(pending() == 0, "cancelled/iframe/zero damage cannot consume charge or enqueue");
        DamageSource magic = new DamageSource(level.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE)
                .getHolderOrThrow(DamageTypes.MAGIC), player, player);
        Cow spell = cow(level); spell.hurt(magic, 1); AzraelState.drain(level.getServer());
        check(!spell.isAlive(), "player-sourced magic consumes preserved charge");
        Cow extra = cow(level); extra.hurt(magic, 1); AzraelState.drain(level.getServer());
        check(extra.isAlive(), "three received hits still grant only one chance");

        clean(player); player.hurt(level.damageSources().mobAttack(attacker), 1);
        Cow lethal = cow(level); lethal.hurt(magic, 100);
        Cow afterLethal = cow(level); afterLethal.hurt(magic, 1); AzraelState.drain(level.getServer());
        check(!lethal.isAlive() && afterLethal.isAlive(), "ordinary lethal damage consumes malice chance");

        LongTravailData.setWitness(diary, TravailAspect.DEEP_VALLEY, true);
        AzraelState.forget(player); AzraelConfig.TARGET.set(1D); AzraelConfig.SELF.set(0D);
        Cow mixed = cow(level); mixed.setAbsorptionAmount(1); mixed.hurt(magic, 3);
        check(mixed.getAbsorptionAmount() == 0 && mixed.getHealth() < mixed.getMaxHealth() && pending() == 1,
                "absorption plus health loss rolls once");
        AzraelState.drain(level.getServer()); check(pending() == 0, "execution cannot recursively enqueue");
        Cow absorbed = cow(level); absorbed.setAbsorptionAmount(5); absorbed.hurt(magic, 1);
        check(absorbed.getHealth() == absorbed.getMaxHealth() && pending() == 1, "absorption-only outgoing damage rolls");
        AzraelState.drain(level.getServer());
        DamageSource rose = new DamageSource(level.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE)
                .getHolderOrThrow(com.thelongtravail.flourishing.FloralCombat.ROSE), player);
        Cow reflected = cow(level); reflected.hurt(rose, 1); AzraelState.drain(level.getServer());
        check(!reflected.isAlive(), "player-sourced retaliation participates");
        cow(level).hurt(level.damageSources().magic(), 1);
        check(pending() == 0, "unattributed damage does not borrow player attribution");

        // LivingDamageEvent 在扣除伤害吸收量后、扣除生命值前触发。
        for (boolean absorbFirst : List.of(false, true)) {
            LongTravailData.setWitness(diary, TravailAspect.DEEP_VALLEY, false); AzraelState.forget(player);
            clean(player); player.hurt(level.damageSources().mobAttack(attacker), 1);
            Cow outer = cow(level), nested = cow(level);
            if (absorbFirst) outer.setAbsorptionAmount(1);
            Object listener = new Object() {
                @SubscribeEvent public void damage(LivingDamageEvent event) {
                    if (event.getEntity() == outer && !AzraelExecution.active()) nested.hurt(magic, 1);
                }
            };
            MinecraftForge.EVENT_BUS.register(listener);
            try { outer.hurt(magic, 3); }
            finally { MinecraftForge.EVENT_BUS.unregister(listener); }
            check(pending() == 1, "nested damage consumes only one malice opportunity");
            AzraelState.drain(level.getServer());
            check(outer.isAlive() != absorbFirst && nested.isAlive() == absorbFirst,
                    "actual loss order selects first target; absorptionFirst=" + absorbFirst);
        }
        LongTravailData.setWitness(diary, TravailAspect.DEEP_VALLEY, true);
        AzraelState.forget(player); AzraelConfig.TARGET.set(0D); AzraelConfig.SELF.set(1D);
        probe = new Probe(); probe.watched = player; probe.cancelDeath = true;
        MinecraftForge.EVENT_BUS.register(probe);
        try {
            clean(player); Cow dead = cow(level); dead.hurt(magic, 100); AzraelState.drain(level.getServer());
            check(!dead.isAlive() && probe.deaths == 12, "ordinary lethal damage still rolls self execution");
        } finally { MinecraftForge.EVENT_BUS.unregister(probe); AzraelState.forget(player); }
        System.out.println("AZRAEL_DAMAGE_TRIGGERS_PASS");
    }
    private static void lifecycleChecks(ServerLevel level) throws Exception {
        AzraelConfig.TARGET.set(1D); AzraelConfig.SELF.set(0D);
        for (String change : List.of("azrael", "diary", "witness", "logout", "dimension", "death")) {
            TestPlayer player = new TestPlayer(level); ItemStack diary = equipment(player); clean(player);
            Cow target = cow(level); target.hurt(level.damageSources().playerAttack(player), 1);
            check(pending() == 1, "queued before lifecycle change " + change);
            var inv = CuriosApi.getCuriosInventory(player).resolve().orElseThrow();
            switch (change) {
                case "azrael" -> inv.getStacksHandler("head").orElseThrow().getStacks().setStackInSlot(0, ItemStack.EMPTY);
                case "diary" -> inv.getStacksHandler("travel_diary").orElseThrow().getStacks().setStackInSlot(0, ItemStack.EMPTY);
                case "witness" -> LongTravailData.setWitness(diary, TravailAspect.DEEP_VALLEY, false);
                case "logout" -> MinecraftForge.EVENT_BUS.post(new net.minecraftforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent(player));
                case "dimension" -> MinecraftForge.EVENT_BUS.post(new net.minecraftforge.event.entity.player.PlayerEvent.PlayerChangedDimensionEvent(
                        player, net.minecraft.world.level.Level.OVERWORLD, net.minecraft.world.level.Level.NETHER));
                case "death" -> player.hurt(level.damageSources().genericKill(), 100);
            }
            AzraelState.drain(level.getServer());
            check(target.isAlive() && pending() == 0, "stale request discarded after " + change);
            AzraelState.forget(player);
        }
        // 移除一名玩家的待处理请求时，不得丢弃其他玩家的请求。
        TestPlayer first = new TestPlayer(level), second = new TestPlayer(level);
        equipment(first); equipment(second); clean(first); clean(second);
        Cow a = cow(level), b = cow(level);
        a.hurt(level.damageSources().playerAttack(first), 1); b.hurt(level.damageSources().playerAttack(second), 1);
        AzraelState.forget(first); AzraelState.drain(level.getServer());
        check(a.isAlive() && !b.isAlive(), "players have independent pending requests");
        AzraelState.forget(second);

        TestPlayer player = new TestPlayer(level); ItemStack diary = equipment(player); clean(player);
        var inv = CuriosApi.getCuriosInventory(player).resolve().orElseThrow();
        var head = inv.getStacksHandler("head").orElseThrow().getStacks();
        head.setStackInSlot(1, new ItemStack(ModRegistry.AZRAEL.get()));
        Cow duplicate = cow(level); duplicate.hurt(level.damageSources().playerAttack(player), 1);
        check(pending() == 1, "forced duplicate Azrael still rolls exactly once");
        AzraelState.drain(level.getServer());
        LongTravailData.setWitness(diary, TravailAspect.DEEP_VALLEY, false); AzraelState.forget(player);
        AzraelConfig.MALICE.set(0D);
        player.hurt(level.damageSources().mobAttack(cow(level)), 1);
        cow(level).hurt(level.damageSources().playerAttack(player), 1);
        AzraelConfig.MALICE.set(1D);
        Cow noRetry = cow(level); noRetry.hurt(level.damageSources().playerAttack(player), 1);
        check(pending() == 0, "failed malice roll consumes opportunity");
        clean(player); player.hurt(level.damageSources().mobAttack(cow(level)), 1);
        ItemStack original = head.getStackInSlot(0), refreshed = original.copy();
        ((top.theillusivec4.curios.api.type.capability.ICurioItem)original.getItem()).onUnequip(
                new SlotContext("head", player, 0, false, true), refreshed, original);
        head.setStackInSlot(0, refreshed);
        DependentAccessories.reconcile(player);
        Cow refreshTarget = cow(level); refreshTarget.hurt(level.damageSources().playerAttack(player), 1);
        AzraelState.drain(level.getServer()); check(!refreshTarget.isAlive(), "same-item NBT refresh preserves opportunity");
        AzraelState.forget(player);
        // 间接投射物的伤害归属于其玩家主人，而非箭矢实体。
        LongTravailData.setWitness(diary, TravailAspect.DEEP_VALLEY, true);
        var arrow = new net.minecraft.world.entity.projectile.Arrow(level, player);
        Cow shot = cow(level); shot.hurt(level.damageSources().arrow(arrow, player), 1);
        AzraelState.drain(level.getServer()); check(!shot.isAlive(), "projectile owner participates");
        AzraelState.forget(player);
        System.out.println("AZRAEL_LIFECYCLE_PASS");
    }
    private static void fallbackChecks(ServerLevel level) throws Exception {
        TestPlayer player = new TestPlayer(level); clean(player);
        int attempts = AzraelConfig.ATTEMPTS.get();
        boolean generic = AzraelConfig.KILL_FALLBACK.get(), direct = AzraelConfig.DEATH_FALLBACK.get();
        Probe probe = new Probe(); MinecraftForge.EVENT_BUS.register(probe);
        try {
            AzraelConfig.ATTEMPTS.set(3); AzraelConfig.KILL_FALLBACK.set(false); AzraelConfig.DEATH_FALLBACK.set(false);
            Cow blocked = cow(level); probe.watched = blocked; probe.cancelDamage = true;
            check(AzraelExecution.execute(player, blocked) == AzraelExecution.Result.SURVIVED
                    && probe.hits == 3 && probe.deaths == 0, "configured attempt bound and disabled fallbacks");
            AzraelConfig.DEATH_FALLBACK.set(true); probe.hits = probe.deaths = 0;
            check(AzraelExecution.execute(player, blocked) == AzraelExecution.Result.DEAD
                    && probe.hits == 3 && probe.deaths == 1, "direct fallback works independently of generic fallback");
            Cow cancelled = cow(level);
            Object cancelOnly = new Object() {
                @SubscribeEvent public void death(LivingDeathEvent event) {
                    if (event.getEntity() == cancelled) event.setCanceled(true);
                }
            };
            MinecraftForge.EVENT_BUS.register(cancelOnly);
            try {
                check(AzraelExecution.execute(player, cancelled) == AzraelExecution.Result.SURVIVED
                        && cancelled.isAlive() && Float.isFinite(cancelled.getHealth()), "cancelled death without healing remains alive");
            } finally { MinecraftForge.EVENT_BUS.unregister(cancelOnly); }
            Cow throwing = new Cow(EntityType.COW, level) {
                @Override public boolean hurt(DamageSource source, float amount) { throw new IllegalStateException("execution fixture"); }
            };
            try { AzraelExecution.execute(player, throwing); throw new AssertionError("expected fixture exception"); }
            catch (IllegalStateException expected) { check(expected.getMessage().equals("execution fixture"), "fixture exception origin"); }
            check(!AzraelExecution.active(), "execution context cleaned after exception");
            check(AzraelExecution.execute(player, cow(level)) == AzraelExecution.Result.DEAD, "later executions unaffected by exception");
        } finally {
            MinecraftForge.EVENT_BUS.unregister(probe);
            AzraelConfig.ATTEMPTS.set(attempts); AzraelConfig.KILL_FALLBACK.set(generic); AzraelConfig.DEATH_FALLBACK.set(direct);
        }
        System.out.println("AZRAEL_FALLBACK_EDGES_PASS");
    }
    private static void armorDurabilityChecks(ServerLevel level) throws Exception {
        boolean previousPvp = level.getServer().isPvpAllowed();
        level.getServer().setPvpAllowed(true);
        try {
            for (boolean self : List.of(false, true)) {
                TestPlayer attacker = new TestPlayer(level), victim = self ? attacker : new TestPlayer(level);
                clean(attacker); clean(victim);
                var slots = List.of(EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET, EquipmentSlot.OFFHAND);
                var items = List.of(Items.DIAMOND_HELMET, Items.DIAMOND_CHESTPLATE, Items.DIAMOND_LEGGINGS, Items.DIAMOND_BOOTS, Items.SHIELD);
                List<ItemStack> stacks = new ArrayList<>();
                for (int i = 0; i < slots.size(); i++) {
                    ItemStack stack = new ItemStack(items.get(i)); stack.setDamageValue(7);
                    stacks.add(stack); victim.setItemSlot(slots.get(i), stack);
                }
                Probe probe = new Probe(); probe.watched = victim; probe.cancelDeath = true;
                Object tags = new Object() {
                    @SubscribeEvent public void hurt(LivingAttackEvent event) {
                        if (event.getEntity() != victim || !AzraelExecution.active()) return;
                        check(event.getSource().is(net.minecraft.tags.DamageTypeTags.BYPASSES_ARMOR), "execution and generic fallback bypass armor");
                        check(event.getSource().is(net.minecraft.tags.DamageTypeTags.BYPASSES_SHIELD), "execution and generic fallback bypass shield");
                        check(!event.getSource().is(net.minecraft.tags.DamageTypeTags.DAMAGES_HELMET), "no separate helmet damage path");
                    }
                };
                MinecraftForge.EVENT_BUS.register(probe); MinecraftForge.EVENT_BUS.register(tags);
                try {
                    check(AzraelExecution.execute(attacker, victim) == AzraelExecution.Result.SURVIVED, "reviving armored player survives");
                    check(probe.hits == 11 && probe.deaths == 12, "armor test covers ten infinite hits and both fallbacks");
                    for (int i = 0; i < slots.size(); i++) check(victim.getItemBySlot(slots.get(i)) == stacks.get(i)
                            && stacks.get(i).getCount() == 1 && stacks.get(i).getDamageValue() == 7,
                            "durability unchanged self=" + self + " slot=" + slots.get(i));
                } finally { MinecraftForge.EVENT_BUS.unregister(probe); MinecraftForge.EVENT_BUS.unregister(tags); }
            }
        } finally { level.getServer().setPvpAllowed(previousPvp); }
        System.out.println("AZRAEL_ARMOR_DURABILITY_PASS: target/self, 10 infinite hits + 2 fallbacks, armor and shield unchanged");
    }
    private static void fullInventoryChecks(ServerLevel level) throws Exception {
        for (boolean creative : List.of(false, true)) {
            TestPlayer player = new TestPlayer(level); equipment(player); clean(player); player.setPos(0, 80, 0);
            player.getAbilities().instabuild = creative;
            for (int i = 0; i < player.getInventory().items.size(); i++) player.getInventory().items.set(i, new ItemStack(Items.COBBLESTONE, 64));
            var inv = CuriosApi.getCuriosInventory(player).resolve().orElseThrow();
            inv.getStacksHandler("travel_diary").orElseThrow().getStacks().setStackInSlot(0, ItemStack.EMPTY);
            var area = player.getBoundingBox().inflate(8);
            java.util.function.IntSupplier drops = () -> level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class, area)
                    .stream().filter(e -> e.getItem().is(ModRegistry.AZRAEL.get())).mapToInt(e -> e.getItem().getCount()).sum();
            int before = drops.getAsInt();
            MinecraftForge.EVENT_BUS.post(new net.minecraftforge.event.TickEvent.PlayerTickEvent(
                    net.minecraftforge.event.TickEvent.Phase.END, player));
            DependentAccessories.reconcile(player);
            check(inv.getStacksHandler("head").orElseThrow().getStacks().getStackInSlot(0).isEmpty()
                    && drops.getAsInt() == before + 1, "full inventory drops exactly one accessory, creative=" + creative);
        }
        System.out.println("AZRAEL_FULL_INVENTORY_PASS");
    }
    private static void equipmentChecks(TestPlayer player, ItemStack diary) {
        var inv = CuriosApi.getCuriosInventory(player).resolve().orElseThrow();
        var diarySlots = inv.getStacksHandler("travel_diary").orElseThrow().getStacks();
        var head = inv.getStacksHandler("head").orElseThrow().getStacks();
        var feet = inv.getStacksHandler("feet").orElseThrow().getStacks();
        var item = (top.theillusivec4.curios.api.type.capability.ICurioItem) ModRegistry.AZRAEL.get();
        check(!item.canEquip(new SlotContext("head", player, 1, false, true), new ItemStack(ModRegistry.AZRAEL.get())), "reject duplicate Azrael");
        feet.setStackInSlot(0, new ItemStack(ModRegistry.TOKAIDO.get()));
        check(!((top.theillusivec4.curios.api.type.capability.ICurioItem)ModRegistry.TOKAIDO.get()).canEquip(new SlotContext("feet", player, 1, false, true), new ItemStack(ModRegistry.TOKAIDO.get())), "reject duplicate Tokaido");
        ((top.theillusivec4.curios.api.type.capability.ICurioItem)ModRegistry.LONG_TRAVAIL.get()).onUnequip(new SlotContext("travel_diary", player, 0, false, true), diary.copy(), diary);
        diarySlots.setStackInSlot(0, diary.copy()); DependentAccessories.reconcile(player);
        check(head.getStackInSlot(0).is(ModRegistry.AZRAEL.get()), "same-slot refresh does not unequip");
        diarySlots.setStackInSlot(0, ItemStack.EMPTY); diarySlots.setStackInSlot(1, diary.copy()); DependentAccessories.reconcile(player);
        check(head.getStackInSlot(0).is(ModRegistry.AZRAEL.get()), "move completed before dependency check");
        head.setStackInSlot(1, new ItemStack(ModRegistry.AZRAEL.get())); diarySlots.setStackInSlot(1, ItemStack.EMPTY);
        DependentAccessories.reconcile(player); DependentAccessories.reconcile(player);
        check(head.getStackInSlot(0).isEmpty() && head.getStackInSlot(1).isEmpty(), "all forced duplicates removed");
        check(player.getInventory().countItem(ModRegistry.AZRAEL.get()) == 2, "return exactly once");
        check(!item.canEquip(new SlotContext("head", player, 0, false, true), new ItemStack(ModRegistry.AZRAEL.get())), "requires diary");
        diarySlots.setStackInSlot(0, diary.copy());
        head.setStackInSlot(0, new ItemStack(ModRegistry.AZRAEL.get()));
        player.getInventory().selected = 8;
        player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, new ItemStack(ModRegistry.HOMECOMING.get()));
        ModRegistry.HOMECOMING.get().use(player.level(), player, net.minecraft.world.InteractionHand.MAIN_HAND);
        check(TravailCurios.stack(player).isEmpty() && head.getStackInSlot(0).isEmpty(), "Homecoming immediately removes Azrael");
        check(player.getInventory().countItem(ModRegistry.AZRAEL.get()) == 3, "Homecoming preserves item");

        for (boolean spring : List.of(true, false)) {
            var accessory = spring ? ModRegistry.SPRING_GAME.get() : ModRegistry.AFFECTION.get();
            String slot = spring ? "head" : "body";
            var stacks = inv.getStacksHandler(slot).orElseThrow().getStacks();
            ItemStack first = new ItemStack(accessory); stacks.setStackInSlot(0, first);
            check(!((top.theillusivec4.curios.api.type.capability.ICurioItem)accessory).canEquip(new SlotContext(slot, player, 1, false, true), new ItemStack(accessory)), "existing accessory rejects duplicates");
            stacks.setStackInSlot(1, new ItemStack(accessory));
            check(com.thelongtravail.flourishing.Affection.equipped(player, spring) == first, "forced duplicates still select first accessory");
            stacks.setStackInSlot(0, ItemStack.EMPTY); stacks.setStackInSlot(1, ItemStack.EMPTY);
        }
        player.getInventory().items.set(10, new ItemStack(ModRegistry.EKI.get()));
        player.getInventory().items.set(11, new ItemStack(ModRegistry.EKI.get()));
        check(com.thelongtravail.abyss.RainState.carrying(player), "multiple Eki are one presence flag");
        player.getInventory().items.set(10, ItemStack.EMPTY);
        check(com.thelongtravail.abyss.RainState.carrying(player), "Eki hint moves to remaining copy");
        player.getInventory().items.set(11, ItemStack.EMPTY);
        check(!com.thelongtravail.abyss.RainState.carrying(player), "Eki absence invalidates hint");
    }
    private static void lootChecks(ServerLevel level) throws Exception {
        var gson = net.minecraft.world.level.storage.loot.Deserializers.createLootTableSerializer().create();
        var defaults = List.copyOf(travail.smoke.LegacyLootBaseline.AZRAEL);
        for (String rule : defaults) {
            var id = new ResourceLocation(rule.split("\\|")[0]);
            var table = level.getServer().getLootData().getLootTable(id);
            var json = gson.toJsonTree(table, net.minecraft.world.level.storage.loot.LootTable.class).getAsJsonObject();
            int count = 0;
            for (var element : json.getAsJsonArray("pools")) {
                var pool = element.getAsJsonObject();
                if (!pool.has("name") || !pool.get("name").getAsString().startsWith("the_long_travail:shared_items")) continue;
                count++;
                int weightSum=0,itemWeight=0;
                for(var raw:pool.getAsJsonArray("entries")){var item=raw.getAsJsonObject();int weight=item.has("weight")?item.get("weight").getAsInt():1;weightSum+=weight;if(item.get("name").getAsString().equals("the_long_travail:azrael"))itemWeight=weight;}
                double probability=pool.getAsJsonArray("conditions").get(0).getAsJsonObject().get("chance").getAsDouble()*itemWeight/weightSum;
                check(itemWeight>0 && Math.abs(probability-.025)<1e-7,"shared pool preserves Azrael 2.5% chance");
            }
            check(count == 1, "exactly one Azrael pool per chest table");
        }
        var id = new ResourceLocation("minecraft:chests/abandoned_mineshaft");
        var resource = new ResourceLocation("minecraft:loot_tables/chests/abandoned_mineshaft.json");
        try (var reader = level.getServer().getResourceManager().getResourceOrThrow(resource).openAsReader()) {
            var table = net.minecraftforge.common.ForgeHooks.loadLootTable(gson, id, com.google.gson.JsonParser.parseReader(reader), true);
            var pool = table.getPool("the_long_travail:shared_items");
            com.thelongtravail.loot.UnifiedLoot.inject(table);
            check(pool != null && table.getPool("the_long_travail:shared_items") == pool, "fixed datapack pool is injected exactly once");
        }
    }
    private void run(ServerStartedEvent event) {
        boolean passed = false;
        try {
            // 隔离测试会连续强制概率；使用内存配置，避免 autosave 与文件监听重载竞争。
            com.thelongtravail.config.TravailConfig.SPECS.values().forEach(spec ->
                    spec.setConfig(com.electronwill.nightconfig.core.CommentedConfig.inMemory()));
            ServerLevel level = event.getServer().overworld(); TestPlayer player = new TestPlayer(level);
            ItemStack diary = equipment(player); clean(player);
            check(AzraelConfig.MALICE.get() == .025 && AzraelConfig.TARGET.get() == .005 && AzraelConfig.SELF.get() == .005, "default probabilities");
            for (String rule : travail.smoke.LegacyLootBaseline.AZRAEL) {
                var table = event.getServer().getLootData().getLootTable(new ResourceLocation(rule.split("\\|")[0]));
                check(table.getPool("the_long_travail:shared_items") != null, "configured loot pool " + rule);
            }
            lootChecks(level);
            reviewChecks(level, player);
            execution(level, player); mechanics(level, player, diary); damageTriggers(level, player, diary); equipmentChecks(player, diary);
            CombatRulesSmoke.run(level);
            lifecycleChecks(level); fallbackChecks(level); armorDurabilityChecks(level); fullInventoryChecks(level);
            AzraelConfig.TARGET.set(1D); AzraelConfig.SELF.set(0D);
            tickPlayer = new TestPlayer(level); equipment(tickPlayer); clean(tickPlayer);
            tickVictim = cow(level); tickVictim.hurt(level.damageSources().playerAttack(tickPlayer), 1);
            check(pending() == 1 && tickVictim.isAlive(), "queued for real server tick");
            passed = true;
        } catch (Throwable failure) { failure.printStackTrace(); }
        finally {
            if (!passed) finishServer(event.getServer(), false);
        }
    }
}
