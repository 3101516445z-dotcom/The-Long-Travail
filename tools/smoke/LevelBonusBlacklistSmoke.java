package travail.smoke;
import com.thelongtravail.TravailAspect;
import com.thelongtravail.config.TravailConfig;
import com.thelongtravail.data.*;
import com.thelongtravail.registry.ModRegistry;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.*;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.util.FakePlayer;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.common.inventory.CurioStacksHandler;
import top.theillusivec4.curios.api.type.capability.ICurio;
import java.util.*;
public final class LevelBonusBlacklistSmoke {
    private static void check(boolean ok, String reason) { if (!ok) throw new AssertionError(reason); }
    public static void run(ServerLevel level) {
        var old = TravailConfig.FAR_WITNESS_LEVEL_BLACKLIST.get();
        int bonus = TravailConfig.FAR_WITNESS_LEVEL_BONUS.get();
        var player = new FakePlayer(level, new com.mojang.authlib.GameProfile(UUID.randomUUID(), "BonusBlacklist"));
        var diary = new ItemStack(ModRegistry.LONG_TRAVAIL.get());
        var inventory = CuriosApi.getCuriosInventory(player).resolve().orElseThrow();
        var slots = new CurioStacksHandler(inventory, "travel_diary", 1, true, false, true, ICurio.DropRule.DEFAULT);
        inventory.setCurios(new HashMap<>(Map.of("travel_diary", slots)));
        slots.getStacks().setStackInSlot(0, diary);
        LongTravailData.tryInitialize(diary, player);
        LongTravailData.setWitness(diary, TravailAspect.FAR_REACH, true);
        try {
            TravailConfig.FAR_WITNESS_LEVEL_BONUS.set(1);
            TravailConfig.FAR_WITNESS_LEVEL_BLACKLIST.set(List.of(" minecraft:night_vision ", "minecraft:night_vision", "missing_mod:custom", "INVALID ID"));
            RuntimePools.reload();
            check(RuntimePools.current().levelBonusBlacklist().size() == 2, "trim, deduplicate, tolerate absent mod, reject malformed ID");
            player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 200, 0, true, false, false));
            var blocked = player.getEffect(MobEffects.NIGHT_VISION);
            check(blocked.getAmplifier() == 0 && blocked.getDuration() == 200, "blacklisted first gain unchanged");
            check(blocked.isAmbient() && !blocked.isVisible() && !blocked.showIcon(), "flags preserved");
            player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 400, 0));
            check(player.getEffect(MobEffects.NIGHT_VISION).getAmplifier() == 0, "renewal does not increase level");
            player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 200, 0));
            check(player.getEffect(MobEffects.MOVEMENT_SPEED).getAmplifier() == 1, "unlisted effect still boosted");
            player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 200, 4));
            check(player.getEffect(MobEffects.NIGHT_VISION).getAmplifier() == 4, "existing high-level source preserved");
            TravailConfig.FAR_WITNESS_LEVEL_BLACKLIST.set(List.of()); RuntimePools.reload();
            player.removeEffect(MobEffects.NIGHT_VISION);
            player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 200, 0));
            check(player.getEffect(MobEffects.NIGHT_VISION).getAmplifier() == 1, "reload to empty restores bonus");
            TravailConfig.FAR_WITNESS_LEVEL_BLACKLIST.set(List.of("minecraft:night_vision")); RuntimePools.reload();
            check(player.getEffect(MobEffects.NIGHT_VISION).getAmplifier() == 1, "reload does not downgrade active effects");
            System.out.println("TRAVAIL_LEVEL_BLACKLIST_PASS: real grant event, first gain, renewal, flags, high level, unlisted bonus, parsing, reload");
        } finally {
            TravailConfig.FAR_WITNESS_LEVEL_BLACKLIST.set(old); TravailConfig.FAR_WITNESS_LEVEL_BONUS.set(bonus);
            RuntimePools.reload(); EffectChanges.forget(player);
        }
    }
}
