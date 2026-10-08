package travail.smoke;

import com.mojang.authlib.GameProfile;
import com.thelongtravail.TravailAspect;
import com.thelongtravail.config.TravailConfig;
import com.thelongtravail.data.AltitudePenalty;
import com.thelongtravail.data.LongTravailData;
import com.thelongtravail.registry.ModRegistry;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.util.FakePlayer;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.common.inventory.CurioStacksHandler;
import top.theillusivec4.curios.api.type.capability.ICurio;
import java.util.*;

public final class AltitudeCacheSmoke {
    private static void check(boolean ok, String message) { if (!ok) throw new AssertionError(message); }
    private static boolean same(float a, float b) { return Math.abs(a - b) < 0.00001F; }
    public static void run(ServerLevel level) {
        var players = new ArrayList<FakePlayer>();
        var diaries = new ArrayList<ItemStack>();
        float factor = (float) (1 - TravailConfig.BOUNDLESS_SPEED_REDUCTION.get());
        for (int i = 0; i < 100; i++) {
            var player = new FakePlayer(level, new GameProfile(UUID.randomUUID(), "Cache" + i));
            check(player instanceof AltitudePenalty.Cache, "Player mixin owns cache");
            player.setPos(0, TravailConfig.BOUNDLESS_HEIGHT_THRESHOLD.get() + 1, 0);
            var diary = new ItemStack(ModRegistry.LONG_TRAVAIL.get());
            LongTravailData.initialize(diary, player);
            LongTravailData.setWitness(diary, TravailAspect.BOUNDLESS, false);
            var inventory = CuriosApi.getCuriosInventory(player).resolve().orElseThrow();
            var slots = new CurioStacksHandler(inventory, "travel_diary", 1, true, false, true, ICurio.DropRule.DEFAULT);
            inventory.setCurios(new HashMap<>(Map.of("travel_diary", slots)));
            slots.getStacks().setStackInSlot(0, diary);
            check(same(AltitudePenalty.factor(player), factor), "initial factor");
            players.add(player); diaries.add(diary);
        }
        for (var diary : diaries) LongTravailData.setWitness(diary, TravailAspect.BOUNDLESS, true);
        // 故意在同一刻修改状态且不触发装备回调；其他 100 名玩家读取后缓存仍应有效，
        // 不能像全局 64 项缓存那样因整体清空而被逐出。
        for (int pass = 0; pass < 3; pass++) for (var player : players)
            check(same(AltitudePenalty.factor(player), factor), "100 interleaved per-player caches stay warm");
        var player = players.get(0); var diary = diaries.get(0);
        player.tickCount++;
        check(AltitudePenalty.factor(player) == 1, "next tick observes witness change");
        LongTravailData.setWitness(diary, TravailAspect.BOUNDLESS, false);
        AltitudePenalty.forget(player);
        check(same(AltitudePenalty.factor(player), factor), "explicit same-tick invalidation");
        double original = TravailConfig.BOUNDLESS_SPEED_REDUCTION.get();
        try {
            TravailConfig.BOUNDLESS_SPEED_REDUCTION.set(0.5D);
            AltitudePenalty.configReloaded();
            check(AltitudePenalty.factor(player) == 0.5F, "config revision invalidates within same tick");
        } finally {
            TravailConfig.BOUNDLESS_SPEED_REDUCTION.set(original);
            AltitudePenalty.configReloaded();
        }
        var speedAttribute = player.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED);
        var bonus = new net.minecraft.world.entity.ai.attributes.AttributeModifier(UUID.randomUUID(), "test speed bonus", 0.5,
                net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.MULTIPLY_TOTAL);
        speedAttribute.addTransientModifier(bonus);
        float altitudeSpeed = player.getSpeed();
        check(altitudeSpeed > 0, "baseline with altitude and external speed bonus");
        com.thelongtravail.data.StiffState.start(player, 100);
        check(ModRegistry.STIFF.get().getAttributeModifiers().isEmpty(), "stiff no longer adds speed attribute penalties");
        check(player.getSpeed() == 0, "stiff overrides altitude and positive speed bonus");
        var legacyId = UUID.fromString("d2b21a50-a731-4f15-91ea-5cfcb76e818f");
        speedAttribute.removeModifier(legacyId);
        check(player.getSpeed() == 0, "removing former modifier cannot bypass stiff");
        speedAttribute.removeModifiers();
        check(player.getSpeed() == 0, "clearing all speed modifiers cannot bypass stiff");
        speedAttribute.addTransientModifier(bonus);
        player.removeEffect(ModRegistry.STIFF.get());
        check(player.getSpeed() == 0, "dispelling icon cannot bypass independent stiff");
        com.thelongtravail.data.StiffState.clear(player);
        check(same(player.getSpeed(), altitudeSpeed), "dispelling immediately restores altitude plus external bonus");
        player.addEffect(new net.minecraft.world.effect.MobEffectInstance(ModRegistry.STIFF.get(), 1));
        speedAttribute.addPermanentModifier(new net.minecraft.world.entity.ai.attributes.AttributeModifier(legacyId, "legacy stiff", -1,
                net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.MULTIPLY_TOTAL));
        check(!player.getEffect(ModRegistry.STIFF.get()).tick(player, () -> {}), "one tick effect expires");
        player.removeEffect(ModRegistry.STIFF.get());
        check(speedAttribute.getModifier(legacyId) == null && speedAttribute.getModifier(bonus.getId()) != null,
                "expiry cleanup removes legacy modifier but preserves external bonus");
        check(same(player.getSpeed(), altitudeSpeed), "legacy saved penalty does not survive removal");
        var zombie = new net.minecraft.world.entity.monster.Zombie(level);
        zombie.setSpeed(0.25F);
        zombie.addEffect(new net.minecraft.world.effect.MobEffectInstance(ModRegistry.STIFF.get(), 100, 2));
        check(zombie.getSpeed() == 0, "non-player living entity and amplified stiff");
        zombie.removeEffect(ModRegistry.STIFF.get());
        check(same(zombie.getSpeed(), 0.25F), "non-player speed restored immediately");
        System.out.println("TRAVAIL_STIFF_PASS: attribute clearing, altitude stacking, external bonuses, dispel, legacy cleanup, non-player speed");
        CuriosApi.getCuriosInventory(player).resolve().orElseThrow().getStacksHandler("travel_diary").orElseThrow()
                .getStacks().setStackInSlot(0, ItemStack.EMPTY);
        ((top.theillusivec4.curios.api.type.capability.ICurioItem) ModRegistry.LONG_TRAVAIL.get()).onUnequip(
                new top.theillusivec4.curios.api.SlotContext("travel_diary", player, 0, false, true), ItemStack.EMPTY, diary);
        check(AltitudePenalty.factor(player) == 1, "unequip invalidates immediately");
        System.out.println("TRAVAIL_ALTITUDE_CACHE_PASS: 100 players interleaved, next-tick change, explicit invalidation, config reload, unequip");
    }
}
