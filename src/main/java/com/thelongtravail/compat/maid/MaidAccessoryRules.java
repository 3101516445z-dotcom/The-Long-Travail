package com.thelongtravail.compat.maid;

import static com.thelongtravail.compat.maid.MaidAccess.equipped;
import static com.thelongtravail.compat.maid.MaidAccess.owner;

import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.thelongtravail.TravailAspect;
import com.thelongtravail.abyss.RainState;
import com.thelongtravail.compat.maid.MaidAccessoryRuntime.Phase;
import com.thelongtravail.config.RainConfig;
import com.thelongtravail.data.LongTravailData;
import com.thelongtravail.flourishing.FloralCombat;
import com.thelongtravail.flourishing.Flower;
import com.thelongtravail.helper.TravailCurios;
import com.thelongtravail.registry.ModRegistry;
import com.thelongtravail.valley.LanternSources;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/** Qualification and value queries; effect execution and state cleanup remain in the runtime.
 * Phase retains its existing public type for compatibility. Phase-only queries intentionally
 * do not imply that the corresponding accessory is equipped or that the maid is alive.
 */
public final class MaidAccessoryRules {
    static boolean has(EntityMaid m, Item item) {
        return !equipped(m, item).isEmpty();
    }

    public static ItemStack lorentzStack(LivingEntity e) {
        return e instanceof EntityMaid m
                ? equipped(m, ModRegistry.LORENTZ_FANTASY.get())
                : ItemStack.EMPTY;
    }

    public static int lorentzPhase(LivingEntity e) {
        return e instanceof EntityMaid m
                ? switch (phase(m, TravailAspect.BOUNDLESS)) {
                    case MALICE -> 1;
                    case WITNESS -> 2;
                    default -> 0;
                }
                : 0;
    }

    public static boolean quixote(LivingEntity e) {
        return e instanceof EntityMaid m && has(m, ModRegistry.DON_QUIXOTE.get());
    }

    public static int quixotePhase(LivingEntity e) {
        if (!(e instanceof EntityMaid m)) return 0;
        return switch (phase(m, TravailAspect.FAR_REACH)) {
            case MALICE -> 1;
            case WITNESS -> 2;
            default -> 0;
        };
    }

    public static ItemStack flowerStack(LivingEntity entity) {
        return entity instanceof EntityMaid maid
                ? equipped(maid, ModRegistry.EVERLASTING_FLOWER.get())
                : ItemStack.EMPTY;
    }

    public static int flowerPhase(LivingEntity entity) {
        if (!(entity instanceof EntityMaid maid)) return 0;
        return switch (phase(maid, TravailAspect.FLOURISHING)) {
            case MALICE -> 1;
            case WITNESS -> 2;
            default -> 0;
        };
    }

    public static boolean lafite(LivingEntity entity) {
        return entity instanceof EntityMaid m && has(m, ModRegistry.MIDNIGHT_LAFITE.get());
    }

    public static int lafitePhase(LivingEntity entity) {
        if (!(entity instanceof EntityMaid m)) return 0;
        return switch (phase(m, TravailAspect.ABYSS)) {
            case MALICE -> 1;
            case WITNESS -> 2;
            default -> 0;
        };
    }

    public static int phosphophyllitePhase(LivingEntity entity) {
        if (!(entity instanceof EntityMaid maid) || !has(maid, ModRegistry.PHOSPHOPHYLLITE.get()))
            return 0;
        return switch (phase(maid, TravailAspect.DEEP_VALLEY)) {
            case MALICE -> 1;
            case WITNESS -> 2;
            default -> 0;
        };
    }

    public static int relicPhase(LivingEntity e) {
        if (!(e instanceof EntityMaid m)) return 0;
        return switch (phase(m, TravailAspect.UNDERWORLD)) {
            case MALICE -> 1;
            case WITNESS -> 2;
            default -> 0;
        };
    }

    public static ItemStack relicStack(LivingEntity e, boolean banner) {
        return e instanceof EntityMaid m
                ? equipped(m, banner ? ModRegistry.SOUL_BANNER.get() : ModRegistry.DEMON_KING.get())
                : ItemStack.EMPTY;
    }

    public static Phase phase(EntityMaid m, TravailAspect aspect) {
        var p = owner(m);
        if (p == null) return Phase.INACTIVE;
        var diary = TravailCurios.stack(p);
        if (diary.isEmpty()) return Phase.INACTIVE;
        return LongTravailData.hasWitness(diary, aspect) ? Phase.WITNESS : Phase.MALICE;
    }

    public static boolean lantern(Entity e) {
        return e instanceof EntityMaid m
                && LanternSources.enabled(equipped(m, ModRegistry.SWORD_AND_LANTERN.get()));
    }

    public static ItemStack springCrown(LivingEntity entity) {
        return entity instanceof EntityMaid maid ? crown(maid) : ItemStack.EMPTY;
    }

    static ItemStack crown(EntityMaid m) {
        return equipped(m, ModRegistry.SPRING_CORNER.get());
    }

    public static double swimBonus(EntityMaid m) {
        return FloralCombat.value(crown(m), Flower.BLUE_ORCHID);
    }

    static boolean sunny(EntityMaid m) {
        return m.level().isDay() && m.level().canSeeSky(m.blockPosition());
    }

    public static Phase eki(EntityMaid m) {
        return has(m, ModRegistry.EKI.get())
                        && RainState.weather(
                                m, RainConfig.EKI_LOCAL.get(), RainConfig.EKI_SKY.get())
                ? phase(m, TravailAspect.ABYSS)
                : Phase.INACTIVE;
    }

    static boolean tokaido(EntityMaid m) {
        return has(m, ModRegistry.TOKAIDO.get())
                && RainState.weather(
                        m, RainConfig.TOKAIDO_LOCAL.get(), RainConfig.TOKAIDO_SKY.get());
    }

    public static boolean seesUndead(LivingEntity e) {
        return e instanceof EntityMaid m
                && has(m, ModRegistry.THOUSAND_YEARS.get())
                && phase(m, TravailAspect.UNDERWORLD) == Phase.WITNESS;
    }

    public static boolean canHarm(LivingEntity attacker, Player other) {
        if (!(attacker instanceof EntityMaid m)) return false;
        var p = owner(m);
        return p != null && p != other && p.server.isPvpAllowed() && p.canHarmPlayer(other);
    }

    private MaidAccessoryRules() {}
}
