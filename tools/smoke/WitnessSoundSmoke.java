package travail.smoke;

import com.thelongtravail.TravailAspect;
import com.thelongtravail.client.EffectSoundPolicy;
import com.thelongtravail.config.TravailConfig;
import com.thelongtravail.data.LongTravailData;
import com.thelongtravail.data.WitnessSoundRule;
import com.thelongtravail.registry.ModRegistry;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.util.FakePlayer;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.type.capability.ICurio;
import top.theillusivec4.curios.common.inventory.CurioStacksHandler;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

public final class WitnessSoundSmoke {
    private static void check(boolean ok, String message) { if (!ok) throw new AssertionError(message); }
    public static void run(ServerLevel level) {
        check(TravailConfig.MUTE_BENEFICIAL_EFFECT_GAIN_SOUNDS.getDefault(), "enabled by default");
        var player = new FakePlayer(level, new com.mojang.authlib.GameProfile(UUID.randomUUID(), "WitnessSound"));
        var diary = new ItemStack(ModRegistry.LONG_TRAVAIL.get());
        LongTravailData.tryInitialize(diary, player);
        var root = diary.getTag().getCompound("LongTravail");
        root.putInt("Witnesses", TravailAspect.FAR_REACH.mask());
        player.getInventory().setItem(0, diary);
        check(!WitnessSoundRule.muteGain(player, MobEffects.MOVEMENT_SPEED, true, true), "inventory is not equipped");
        var inventory = CuriosApi.getCuriosInventory(player).resolve().orElseThrow();
        var slots = new CurioStacksHandler(inventory, "travel_diary", 1, true, false, true, ICurio.DropRule.DEFAULT);
        inventory.setCurios(new HashMap<>(Map.of("travel_diary", slots)));
        slots.getStacks().setStackInSlot(0, diary);
        var external = new MobEffect(MobEffectCategory.BENEFICIAL, 0) {};
        var neutral = new MobEffect(MobEffectCategory.NEUTRAL, 0) {};
        for (var aspect : TravailAspect.values()) {
            root.putInt("Witnesses", aspect.mask());
            boolean expected = aspect == TravailAspect.FAR_REACH || aspect == TravailAspect.DEEP_VALLEY;
            check(WitnessSoundRule.muteGain(player, external, true, true) == expected, "external effect and single witness " + aspect);
            check(!WitnessSoundRule.muteGain(player, external, true, false), "disabled switch");
            check(!WitnessSoundRule.muteGain(player, external, false, true), "loss unaffected");
            check(!WitnessSoundRule.muteGain(player, MobEffects.POISON, true, true), "harmful warning unaffected");
            check(!WitnessSoundRule.muteGain(player, neutral, true, true), "neutral unaffected");
        }
        root.putInt("Witnesses", TravailAspect.FAR_REACH.mask() | TravailAspect.DEEP_VALLEY.mask());
        LongTravailData.setWitness(diary, TravailAspect.FAR_REACH, false);
        check(WitnessSoundRule.muteGain(player, external, true, true), "other witness remains sufficient");
        LongTravailData.setWitness(diary, TravailAspect.DEEP_VALLEY, false);
        check(!WitnessSoundRule.muteGain(player, external, true, true), "forced malice overrides both");
        LongTravailData.setWitness(diary, TravailAspect.FAR_REACH, true);
        check(WitnessSoundRule.muteGain(player, external, true, true), "forced witness activates");
        var before = diary.getTag().copy();
        var plays = new AtomicInteger();
        var counts = new HashMap<String, Integer>();
        var policy = new EffectSoundPolicy((reason, id) -> counts.merge(reason, 1, Integer::sum));
        java.util.function.BooleanSupplier playback = () -> {
            if (WitnessSoundRule.muteGain(player, external, true, true)) {
                counts.merge("quiet-witness-beneficial-gain", 1, Integer::sum);
                return false;
            }
            plays.incrementAndGet();
            return true;
        };
        // 外部赋予效果没有本模组的原因包，且可能使有界队列溢出。
        for (int i = 0; i < 1000; i++) policy.notification("external:buff", true, playback);
        policy.advance(); policy.advance();
        check(plays.get() == 0, "unmarked repeated gains including overflow stay quiet");
        check(counts.equals(Map.of("quiet-witness-beneficial-gain", 1000)), "muted queue and overflow never count playback");
        check(before.equals(diary.getTag()), "sound rule never mutates diary");
        policy.notification("external:buff", true, playback);
        slots.getStacks().setStackInSlot(0, ItemStack.EMPTY);
        policy.advance(); policy.advance();
        check(plays.get() == 1, "unequip before playback restores sound");
        check(counts.getOrDefault("play-gain", 0) == 1 && !counts.containsKey("overflow-play"), "only actual playback counted");
        check(!WitnessSoundRule.muteGain(null, external, true, true), "logout safe");
        check(!WitnessSoundRule.muteGain(player, null, true, true), "unknown effect safe");
        System.out.println("TRAVAIL_WITNESS_SOUND_PASS: default, all witnesses, external positives, negative/neutral/loss, toggle, forced state, inventory/equipment, overflow, unequip, read-only");
    }
}
