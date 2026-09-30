package travail.smoke;

import com.thelongtravail.TravailAspect;
import com.thelongtravail.config.TravailConfig;
import com.thelongtravail.data.*;
import com.thelongtravail.event.TravailEvents;
import com.thelongtravail.registry.ModRegistry;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.effect.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.event.entity.living.MobEffectEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import java.util.*;

public final class JourneyBudgetSmoke {
    private static void check(boolean ok, String message) { if (!ok) throw new AssertionError(message); }
    private static class Cancel {
        FakePlayer player; int removes, adds;
        Cancel(FakePlayer player) { this.player = player; }
        @SubscribeEvent public void remove(MobEffectEvent.Remove event) {
            if (event.getEntity() == player) { removes++; event.setCanceled(true); }
        }
        @SubscribeEvent public void add(MobEffectEvent.Applicable event) {
            if (event.getEntity() == player) { adds++; event.setResult(net.minecraftforge.eventbus.api.Event.Result.DENY); }
        }
    }
    public static void run(ServerLevel level) throws Exception {
        var player = new FakePlayer(level, new com.mojang.authlib.GameProfile(UUID.randomUUID(), "JourneyBudget"));
        var diary = new ItemStack(ModRegistry.LONG_TRAVAIL.get());
        var emptyIdentity = LongTravailData.queryIdentity(diary);
        check(emptyIdentity.equals(LongTravailData.queryIdentity(diary)) && !diary.hasTag(), "read-only stable uninitialized identity");
        check(LongTravailData.tryInitialize(diary, player), "initialize");
        var before = LongTravailData.queryIdentity(diary);
        check(before.revision() == 1, "initial revision");
        LongTravailData.putLong(diary, "UnrelatedCooldown", 55);
        check(before.equals(LongTravailData.queryIdentity(diary)), "unrelated runtime data does not invalidate");
        var target = LongTravailData.requirementsForDisplay(diary, TravailAspect.FLOURISHING, false).get(0).id();
        check(LongTravailData.visitBiome(diary, target), "visit progresses");
        var changed = LongTravailData.queryIdentity(diary);
        check(changed.revision() == 2 && !before.equals(changed), "progress increments once");
        check(!LongTravailData.visitBiome(diary, target) && changed.equals(LongTravailData.queryIdentity(diary)), "repeat visit keeps revision");
        check(!changed.equals(LongTravailData.queryIdentity(diary.copy())), "identical replacement stack invalidates");
        diary.setTag(diary.getTag().copy());
        check(!changed.equals(LongTravailData.queryIdentity(diary)), "network NBT replacement invalidates");
        var root = diary.getTag().getCompound("LongTravail"); root.remove("RequirementsRevision");
        var legacy = diary.getTag().copy();
        check(LongTravailData.tryInitialize(diary, player) && legacy.equals(diary.getTag()), "legacy initialization remains read-only");
        check(LongTravailData.queryIdentity(diary).revision() == 0, "missing revision reads as zero");
        LongTravailData.requirementsChanged(diary);
        check(LongTravailData.queryIdentity(diary).revision() == 1, "integration mutation API advances revision");
        root.put("Requirements", new CompoundTag() {
            @Override public boolean equals(Object other) { throw new AssertionError("identity must not compare NBT contents"); }
        });
        check(LongTravailData.queryIdentity(diary).equals(LongTravailData.queryIdentity(diary)), "no deep NBT comparison");

        int limit = TravailConfig.MAX_AUTOMATIC_EFFECT_ACTIONS.get(), actions = TravailConfig.FAR_WITNESS_ACTION_COUNT.get();
        Cancel listener = new Cancel(player);
        player.addEffect(new MobEffectInstance(MobEffects.POISON, 100));
        MinecraftForge.EVENT_BUS.register(listener);
        try {
            TravailConfig.MAX_AUTOMATIC_EFFECT_ACTIONS.set(2); TravailConfig.FAR_WITNESS_ACTION_COUNT.set(10);
            var process = TravailEvents.class.getDeclaredMethod("processFarReachWitness", net.minecraft.server.level.ServerPlayer.class);
            process.setAccessible(true); AutomaticEffectBudget.forget(player);
            process.invoke(null, player);
            check(listener.removes == 1 && listener.adds == 1, "cancelled cleanse and denied grant share cap");
            process.invoke(null, player);
            check(listener.removes == 1 && listener.adds == 1, "same window does not replenish");
            AutomaticEffectBudget.forget(player); process.invoke(null, player);
            check(listener.removes == 2 && listener.adds == 2, "lifecycle reset clears window");
            TravailConfig.MAX_AUTOMATIC_EFFECT_ACTIONS.set(0); process.invoke(null, player);
            check(listener.removes == 3 && listener.adds == 12, "zero restores all configured actions");
            System.out.println("TRAVAIL_JOURNEY_BUDGET_PASS: revisions, legacy, replacement, no deep comparison; failed cleanse/grant shared cap, reset and disabled behavior");
        } finally {
            MinecraftForge.EVENT_BUS.unregister(listener); AutomaticEffectBudget.forget(player); CleanseRotation.forget(player);
            TravailConfig.MAX_AUTOMATIC_EFFECT_ACTIONS.set(limit); TravailConfig.FAR_WITNESS_ACTION_COUNT.set(actions);
        }
    }
}
