package travail.smoke;

import com.mojang.authlib.GameProfile;
import com.thelongtravail.TravailAspect;
import com.thelongtravail.combat.*;
import com.thelongtravail.config.FlourishingItemsConfig;
import com.thelongtravail.data.LongTravailData;
import com.thelongtravail.farreach.SunChaser;
import com.thelongtravail.flourishing.*;
import com.thelongtravail.registry.*;

import net.minecraft.server.level.*;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;

import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.type.capability.ICurio.DropRule;
import top.theillusivec4.curios.common.inventory.CurioStacksHandler;

import java.util.*;
import java.util.function.BiConsumer;

/** 使用真实玩家 hurt；燃烧快照强制为真，覆盖免火装备下的极端交叉条件。 */
final class DamageChainCases {
    private final ServerLevel level;
    private final BiConsumer<Boolean, String> check;
    private int depth, maximum, calls;

    private static final class Cancellation {
        final ServerPlayer target;

        Cancellation(ServerPlayer target) {
            this.target = target;
        }

        @net.minecraftforge.eventbus.api.SubscribeEvent
        public void hurt(net.minecraftforge.event.entity.living.LivingHurtEvent event) {
            if (event.getEntity() == target) event.setCanceled(true);
        }
    }

    DamageChainCases(ServerLevel level, BiConsumer<Boolean, String> check) {
        this.level = level;
        this.check = check;
    }

    private final class Actor extends ServerPlayer {
        int sun, insight, rose;

        Actor(String name, double insightValue, boolean crown) {
            super(level.getServer(), level, new GameProfile(UUID.randomUUID(), name));
            var wire =
                    new net.minecraft.network.Connection(
                            net.minecraft.network.protocol.PacketFlow.SERVERBOUND);
            connection =
                    new net.minecraft.server.network.ServerGamePacketListenerImpl(
                            level.getServer(), wire, this) {
                        @Override
                        public void send(net.minecraft.network.protocol.Packet<?> packet) {}
                    };
            setGameMode(GameType.SURVIVAL);
            try {
                var spawn = ServerPlayer.class.getDeclaredField("spawnInvulnerableTime");
                spawn.setAccessible(true);
                spawn.setInt(this, 0);
            } catch (ReflectiveOperationException error) {
                throw new AssertionError(error);
            }
            setPos(20, 100, 0);
            getAttribute(Attributes.MAX_HEALTH).setBaseValue(1000);
            setHealth(1000);
            getAttribute(ModAttributes.INSIGHT.get()).setBaseValue(insightValue);
            var inv = CuriosApi.getCuriosInventory(this).resolve().orElseThrow();
            var slots = new HashMap<String, CurioStacksHandler>();
            for (String id : new String[] {"travel_diary", "belt", "head"})
                slots.put(
                        id,
                        new CurioStacksHandler(inv, id, 1, true, false, true, DropRule.DEFAULT));
            inv.setCurios(new HashMap<>(slots));
            var diary = new ItemStack(ModRegistry.LONG_TRAVAIL.get());
            for (var aspect : TravailAspect.values())
                LongTravailData.setWitness(diary, aspect, true);
            slots.get("travel_diary").getStacks().setStackInSlot(0, diary);
            slots.get("belt")
                    .getStacks()
                    .setStackInSlot(0, new ItemStack(ModRegistry.KAGOTSURUBE.get()));
            getInventory().setItem(0, new ItemStack(ModRegistry.SUN_CHASER.get()));
            if (crown) {
                var stack = new ItemStack(ModRegistry.SPRING_CORNER.get());
                FlowerData.write(stack, List.of(Flower.ROSE_BUSH));
                slots.get("head").getStacks().setStackInSlot(0, stack);
                check.accept(!Affection.equipped(this, true).isEmpty(), "chain crown equipped");
            }
        }

        @Override
        public boolean isOnFire() {
            return true;
        }

        @Override
        public boolean hurt(DamageSource source, float amount) {
            if (++calls > 64) throw new AssertionError("unbounded cross-effect calls");
            maximum = Math.max(maximum, ++depth);
            if (SunChaser.strike(source)) sun++;
            if (InsightRetaliation.isInsight(source)) insight++;
            if (FloralCombat.rose(source)) rose++;
            try {
                return super.hurt(source, amount);
            } finally {
                depth--;
            }
        }
    }

    void run() {
        var cooldown = FlourishingItemsConfig.NUMBERS.get("flowers.rose_bush.cooldownSeconds");
        double old = cooldown.get();
        cooldown.set(0d);
        try {
            for (boolean crown : new boolean[] {false, true}) {
                calls = depth = maximum = 0;
                Actor a = new Actor("ChainA", 1, crown);
                Actor b = new Actor("ChainB", 5, crown);
                b.hurt(a.damageSources().playerAttack(a), 100);
                check.accept(
                        a.sun == 1 && b.sun == 1,
                        "one Sun per actor in shared chain: " + a.sun + "/" + b.sun);
                check.accept(
                        a.insight == 2 && b.insight == 0,
                        "two scheduled counters without return counter");
                check.accept(
                        a.rose == (crown ? 1 : 0) && b.rose == 0,
                        "zero cooldown rose cannot bounce");
                check.accept(
                        calls == (crown ? 6 : 5) && maximum <= 4 && depth == 0,
                        "finite complete chain calls=" + calls + " depth=" + maximum);
                check.accept(
                        Math.abs(b.getHealth() - 885) < .001,
                        "primary and Sun retain independent defense");
                check.accept(
                        b.getEffect(MobEffects.WITHER).getDuration() == 200,
                        "two confirmed hits on target");
                check.accept(!DamageScope.claimSun(a.getUUID()), "root Sun scope closed");
                check.accept(
                        !InsightRetaliation.isRetaliation(a.damageSources().playerAttack(a)),
                        "retaliation scope closed");
            }
            for (var type :
                    List.of(
                            com.thelongtravail.farreach.DonQuixoteCombat.STRIKE,
                            com.thelongtravail.boundless.LorentzFantasy.VOID,
                            FloralCombat.ROSE)) {
                calls = depth = maximum = 0;
                Actor a = new Actor("TypedA", 0, false);
                Actor b = new Actor("TypedB", 0, false);
                var source =
                        new DamageSource(
                                level.registryAccess()
                                        .registryOrThrow(
                                                net.minecraft.core.registries.Registries
                                                        .DAMAGE_TYPE)
                                        .getHolderOrThrow(type),
                                a);
                b.hurt(source, 10);
                check.accept(b.sun == 1 && calls == 2, "typed secondary permits one Sun: " + type);
                check.accept(
                        b.getEffect(MobEffects.WITHER).getDuration() == 200,
                        "typed secondary and Sun both confirm: " + type);
            }
            calls = depth = maximum = 0;
            Actor attacker = new Actor("BoundaryA", 0, false);
            Actor absorbed = new Actor("AbsorbedB", 0, false);
            absorbed.setAbsorptionAmount(1000);
            absorbed.hurt(attacker.damageSources().playerAttack(attacker), 100);
            check.accept(
                    absorbed.getHealth() == 1000 && absorbed.getAbsorptionAmount() == 875,
                    "all absorption confirms primary and Sun without health loss");
            check.accept(
                    absorbed.sun == 1 && absorbed.getEffect(MobEffects.WITHER).getDuration() == 200,
                    "absorption confirmation count");
            Actor blocked = new Actor("OverlordB", 0, false);
            blocked.getAttribute(ModAttributes.OVERLORD.get()).setBaseValue(.2);
            blocked.hurt(attacker.damageSources().playerAttack(attacker), 100);
            check.accept(
                    blocked.getHealth() == 1000
                            && blocked.sun == 0
                            && !blocked.hasEffect(MobEffects.WITHER),
                    "overlord zero does not confirm");
            Actor cancelled = new Actor("CancelledB", 5, false);
            var listener = new Cancellation(cancelled);
            net.minecraftforge.common.MinecraftForge.EVENT_BUS.register(listener);
            int counters = attacker.insight;
            try {
                cancelled.hurt(attacker.damageSources().playerAttack(attacker), 100);
                check.accept(
                        cancelled.getHealth() == 1000
                                && cancelled.sun == 0
                                && attacker.insight == counters
                                && !cancelled.hasEffect(MobEffects.WITHER),
                        "Hurt cancellation prevents confirmations and retaliation");
            } finally {
                net.minecraftforge.common.MinecraftForge.EVENT_BUS.unregister(listener);
            }
            Actor a = new Actor("ChainException", 0, false);
            var source = a.damageSources().playerAttack(a);
            try {
                DamageScope.hurt(
                        a,
                        source,
                        1,
                        (s, amount) -> {
                            check.accept(DamageScope.claimSun(a.getUUID()), "outer claim accepted");
                            DamageScope.hurt(
                                    a,
                                    source,
                                    1,
                                    (nested, value) -> {
                                        throw new IllegalStateException("nested fixture");
                                    });
                            return true;
                        });
                throw new AssertionError("nested failure expected");
            } catch (IllegalStateException expected) {
                check.accept(
                        com.thelongtravail.helper.CombatContext.current(a, source) == null,
                        "nested exception restores context");
                check.accept(!DamageScope.claimSun(a.getUUID()), "nested exception removes chain");
            }
            DamageScope.hurt(
                    a,
                    source,
                    1,
                    (s, amount) -> {
                        check.accept(
                                DamageScope.claimSun(a.getUUID()), "next root has fresh claim");
                        return false;
                    });
        } finally {
            cooldown.set(old);
        }
    }
}
