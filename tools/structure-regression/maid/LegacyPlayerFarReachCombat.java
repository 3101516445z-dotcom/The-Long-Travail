package travail.smoke;

import com.thelongtravail.api.TravailStateApi;
import com.thelongtravail.config.FarReachItemsConfig;
import com.thelongtravail.farreach.*;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.*;
import net.minecraft.world.damagesource.*;

// c5d920a 的玩家规则快照；不随生产规则重构修改。
public final class LegacyPlayerFarReachCombat {
    public static void ignite(net.minecraft.world.entity.LivingEntity target, DamageSource source) {
        if (!com.thelongtravail.config.ModeEffectRules.ICARUS_FIRE.applies(target)
                || target.level().isClientSide
                || target.fireImmune()
                || FarReachItemsConfig.IGNITE_SECONDS.get() == 0) return;
        if (source.getEntity() instanceof ServerPlayer player
                && player != target
                && FarReachEquipment.equipped(player, false))
            target.setSecondsOnFire(FarReachItemsConfig.IGNITE_SECONDS.get());
    }

    public static double bonus(ServerPlayer p) {
        if (!FarReachEquipment.equipped(p, false)) return 0;
        if (TravailStateApi.hasFarReachWitness(p))
            return IcarusEnvironment.openSky(p)
                    ? FarReachItemsConfig.get("icarus.witnessBonus")
                    : 0;
        if (!TravailStateApi.hasFarReachMalice(p)) return 0;
        double sun = IcarusEnvironment.sunlight(p.level().getDayTime());
        return sun == 0
                ? 0
                : sun
                        * FarReachItemsConfig.get(
                                IcarusEnvironment.sunny(p.level()) && IcarusEnvironment.openSky(p)
                                        ? "icarus.sunnyPeak"
                                        : "icarus.malicePeak");
    }

    public static boolean immune(ServerPlayer p, DamageSource source) {
        if (!FarReachEquipment.equipped(p, false)) return false;
        if (source.is(DamageTypeTags.IS_FALL) || source.is(DamageTypes.FLY_INTO_WALL)) return true;
        if (!TravailStateApi.hasFarReachWitness(p)) return false;
        if (source.is(DamageTypes.MAGIC) || source.is(DamageTypes.INDIRECT_MAGIC)) return true;
        for (String entry : FarReachItemsConfig.EXTRA_IMMUNITIES.get()) {
            boolean tag = entry.startsWith("#");
            ResourceLocation id = ResourceLocation.tryParse(tag ? entry.substring(1) : entry);
            if (id != null
                    && (tag
                            ? source.is(TagKey.create(Registries.DAMAGE_TYPE, id))
                            : source.typeHolder()
                                    .unwrapKey()
                                    .map(k -> k.location().equals(id))
                                    .orElse(false))) return true;
        }
        return false;
    }

    private LegacyPlayerFarReachCombat() {}
}
