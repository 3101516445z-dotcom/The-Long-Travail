package travail.smoke;

import com.mojang.authlib.GameProfile;
import com.thelongtravail.TravailAspect;
import com.thelongtravail.config.TravailConfig;
import com.thelongtravail.data.LongTravailData;
import com.thelongtravail.registry.ModRegistry;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.*;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeMap;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.util.FakePlayer;
import java.util.*;

// 在真实属性清理回调中重新造成伤害；夹具回调设上限，回归失败时也不会无限递归。
public final class AttackCleanseSmoke {
    public static final class CallbackEffect extends MobEffect {
        public CallbackEffect() { super(MobEffectCategory.BENEFICIAL, 0); }
        @Override public void removeAttributeModifiers(LivingEntity entity, AttributeMap attributes, int amplifier) {
            super.removeAttributeModifiers(entity, attributes, amplifier);
            if (entity instanceof Attacker player) {
                player.removals++;
                check(player.removals <= 8, "bounded callback recursion");
                if (player.callback != null) player.callback.run();
            }
        }
    }
    private static final class Attacker extends FakePlayer {
        int removals;
        Runnable callback;
        ItemStack diary;
        Attacker(ServerLevel level) {
            super(level, new GameProfile(UUID.randomUUID(), "CleanseProbe"));
            var wire = new net.minecraft.network.Connection(net.minecraft.network.protocol.PacketFlow.SERVERBOUND);
            connection = new net.minecraft.server.network.ServerGamePacketListenerImpl(level.getServer(), wire, this) {
                @Override public void send(net.minecraft.network.protocol.Packet<?> packet) {}
            };
            diary = new ItemStack(ModRegistry.LONG_TRAVAIL.get());
            check(LongTravailData.tryInitialize(diary, this), "initialize diary");
            for (var aspect : TravailAspect.values()) LongTravailData.setWitness(diary, aspect, true);
            LongTravailData.setWitness(diary, TravailAspect.FAR_REACH, false);
            LongTravailData.setWitness(diary, TravailAspect.DEEP_VALLEY, false);
            var inventory = top.theillusivec4.curios.api.CuriosApi.getCuriosInventory(this).resolve().orElseThrow();
            var slots = new top.theillusivec4.curios.common.inventory.CurioStacksHandler(inventory, "travel_diary", 1,
                    true, false, true, top.theillusivec4.curios.api.type.capability.ICurio.DropRule.DEFAULT);
            inventory.setCurios(new HashMap<>(Map.of("travel_diary", slots)));
            slots.getStacks().setStackInSlot(0, diary);
        }
        void buff() { addEffect(new MobEffectInstance(TravailSmoke.ATTACK_CALLBACK.get(), 600)); }
        long deadline() { return LongTravailData.getLong(diary, "AttackCleanseCooldown"); }
    }
    private static void check(boolean ok, String message) { if (!ok) throw new AssertionError(message); }
    private static void hit(Attacker attacker) {
        var target = new Zombie(attacker.serverLevel());
        float before = target.getHealth();
        target.hurt(target.damageSources().playerAttack(attacker), 1);
        check(target.getHealth() < before, "nested and ordinary hits retain actual damage");
    }
    public static void run(ServerLevel level) {
        var settings = List.<net.minecraftforge.common.ForgeConfigSpec.ConfigValue<?>>of(
                TravailConfig.FAR_ATTACK_CLEAR_CHANCE, TravailConfig.FAR_ATTACK_COOLDOWN_SECONDS,
                TravailConfig.VALLEY_STIFF_CHANCE);
        var values = settings.stream().map(v -> v.get()).toList();
        List<Attacker> players = new ArrayList<>();
        try {
            TravailConfig.FAR_ATTACK_CLEAR_CHANCE.set(1D);
            TravailConfig.VALLEY_STIFF_CHANCE.set(1D);
            for (double seconds : new double[]{5D, 0D}) {
                TravailConfig.FAR_ATTACK_COOLDOWN_SECONDS.set(seconds);
                var player = new Attacker(level); players.add(player); player.buff();
                player.callback = () -> {
                    check(player.deadline() == level.getGameTime() + (seconds == 0 ? 0 : 100), "cooldown reserved before callback");
                    player.buff();
                    if (player.removals == 1) {
                        hit(player);
                        check(player.hasEffect(ModRegistry.STIFF.get()), "nested attack still runs Deep Valley before outer callback returns");
                    }
                };
                hit(player);
                check(player.removals == 1, "one cleanse during nested callback at " + seconds);
                player.callback = null;
                hit(player);
                check(player.removals == (seconds == 0 ? 2 : 1), "independent same-tick hit honors zero/positive cooldown");
            }
            TravailConfig.FAR_ATTACK_COOLDOWN_SECONDS.set(0D);
            var a = new Attacker(level); var b = new Attacker(level); players.add(a); players.add(b);
            a.buff(); b.buff();
            a.callback = () -> { a.buff(); if (a.removals == 1) hit(b); };
            b.callback = () -> { if (b.removals == 1) hit(a); };
            hit(a);
            check(a.removals == 1 && b.removals == 1, "A -> B -> A blocks only reentered player");
            a.callback = null; b.callback = null;
            hit(a); check(a.removals == 2, "outer guard released after nested players");

            var failure = new Attacker(level); players.add(failure); failure.buff();
            TravailConfig.FAR_ATTACK_COOLDOWN_SECONDS.set(5D);
            failure.callback = () -> { throw new IllegalStateException("expected cleanse callback failure"); };
            try { hit(failure); throw new AssertionError("callback exception must propagate"); }
            catch (IllegalStateException expected) { check(expected.getMessage().equals("expected cleanse callback failure"), "expected exception"); }
            check(failure.deadline() == level.getGameTime() + 100, "exception retains reserved cooldown");
            failure.callback = null; failure.buff();
            hit(failure); check(failure.removals == 1, "exception does not reset positive cooldown");
            TravailConfig.FAR_ATTACK_COOLDOWN_SECONDS.set(0D);
            hit(failure); check(failure.removals == 2, "guard released and zero ignores old deadline");

            var empty = new Attacker(level); players.add(empty);
            TravailConfig.FAR_ATTACK_COOLDOWN_SECONDS.set(5D);
            hit(empty); check(empty.deadline() == level.getGameTime() + 100, "empty effect pool still consumes successful roll cooldown");
            var miss = new Attacker(level); players.add(miss); miss.buff();
            TravailConfig.FAR_ATTACK_CLEAR_CHANCE.set(0D);
            hit(miss); check(miss.removals == 0 && miss.deadline() == 0, "failed roll consumes no cooldown");
            TravailConfig.FAR_ATTACK_CLEAR_CHANCE.set(1D);
            LongTravailData.setWitness(miss.diary, TravailAspect.FAR_REACH, true);
            hit(miss); check(miss.removals == 0 && miss.deadline() == 0, "Far Reach witness exempt");
            LongTravailData.setWitness(miss.diary, TravailAspect.FAR_REACH, false);
            var absorbed = new Zombie(level); absorbed.setAbsorptionAmount(10);
            absorbed.hurt(absorbed.damageSources().playerAttack(miss), 1);
            check(miss.removals == 0 && miss.deadline() == 0, "absorption-only hit does not cleanse");
            System.out.println("ATTACK_CLEANSE_PASS: callback reservation, positive/zero cooldown, same-tick independent attacks, A-B-A, exception cleanup, other attack rules, empty/missed/witness/absorption cases");
        } finally {
            for (var player : players) { player.callback = null; com.thelongtravail.data.EffectChanges.forget(player); }
            for (int i = 0; i < settings.size(); i++) restore(settings.get(i), values.get(i));
        }
    }
    @SuppressWarnings({"rawtypes", "unchecked"})
    private static void restore(net.minecraftforge.common.ForgeConfigSpec.ConfigValue value, Object previous) { value.set(previous); }
}
