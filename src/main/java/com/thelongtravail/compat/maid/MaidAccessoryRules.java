package com.thelongtravail.compat.maid;

import net.minecraftforge.registries.RegistryObject;
import static com.thelongtravail.compat.maid.MaidAccess.equipped;
import static com.thelongtravail.compat.maid.MaidAccess.owner;

import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.thelongtravail.TravailAspect;
import com.thelongtravail.abyss.rain.RainState;
import com.thelongtravail.api.TravailStateApi.AspectState;
import com.thelongtravail.compat.maid.MaidAccessoryRuntime.Phase;
import com.thelongtravail.config.RainConfig;
import com.thelongtravail.journey.LongTravailData;
import com.thelongtravail.flourishing.FloralCombat;
import com.thelongtravail.flourishing.Flower;
import com.thelongtravail.equipment.TravailCurios;
import com.thelongtravail.registry.ModRegistry;
import com.thelongtravail.valley.lantern.LanternSources;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

// 查询资格与数值，效果执行和状态清理由对应运行组件负责。
// Phase 沿用公开类型以保持兼容；仅查询阶段不代表已装备对应饰品或女仆仍存活。
public final class MaidAccessoryRules {
    static boolean has(
            EntityMaid maid,
            RegistryObject<Item> item) {
        return item.isPresent() && has(maid, item.get());
    }

    public static boolean hasEquipped(LivingEntity entity, Item item) {
        return entity instanceof EntityMaid maid && has(maid, item);
    }

    public static AspectState aspectState(LivingEntity entity, TravailAspect aspect) {
        if (!(entity instanceof EntityMaid maid)) return AspectState.INACTIVE;
        return switch (phase(maid, aspect)) {
            case MALICE -> AspectState.MALICE;
            case WITNESS -> AspectState.WITNESS;
            default -> AspectState.INACTIVE;
        };
    }

    static boolean has(EntityMaid m, Item item) {
        return !equipped(m, item).isEmpty();
    }

    public static ItemStack lorentzStack(LivingEntity e) {
        return e instanceof EntityMaid m
                ? equipped(m, ModRegistry.LORENTZ_FANTASY)
                : ItemStack.EMPTY;
    }

    public static int lorentzPhase(LivingEntity e) {
        return aspectState(e, TravailAspect.BOUNDLESS).phase();
    }

    public static boolean quixote(LivingEntity e) {
        return ModRegistry.DON_QUIXOTE.isPresent() && hasEquipped(e, ModRegistry.DON_QUIXOTE.get());
    }

    public static int quixotePhase(LivingEntity e) {
        return aspectState(e, TravailAspect.FAR_REACH).phase();
    }

    public static ItemStack flowerStack(LivingEntity entity) {
        return entity instanceof EntityMaid maid
                ? equipped(maid, ModRegistry.EVERLASTING_FLOWER)
                : ItemStack.EMPTY;
    }

    public static int flowerPhase(LivingEntity entity) {
        return aspectState(entity, TravailAspect.FLOURISHING).phase();
    }

    public static boolean lafite(LivingEntity entity) {
        return entity instanceof EntityMaid m && has(m, ModRegistry.MIDNIGHT_LAFITE);
    }

    public static int lafitePhase(LivingEntity entity) {
        return aspectState(entity, TravailAspect.ABYSS).phase();
    }

    public static int phosphophyllitePhase(LivingEntity entity) {
        if (!(entity instanceof EntityMaid maid) || !has(maid, ModRegistry.PHOSPHOPHYLLITE))
            return 0;
        return aspectState(entity, TravailAspect.DEEP_VALLEY).phase();
    }

    public static int relicPhase(LivingEntity e) {
        return aspectState(e, TravailAspect.UNDERWORLD).phase();
    }

    public static ItemStack relicStack(LivingEntity e, boolean banner) {
        return e instanceof EntityMaid m
                ? equipped(m, banner ? ModRegistry.SOUL_BANNER : ModRegistry.DEMON_KING)
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
                && LanternSources.enabled(equipped(m, ModRegistry.SWORD_AND_LANTERN));
    }

    public static ItemStack springCrown(LivingEntity entity) {
        return entity instanceof EntityMaid maid ? crown(maid) : ItemStack.EMPTY;
    }

    static ItemStack crown(EntityMaid m) {
        return equipped(m, ModRegistry.SPRING_CORNER);
    }

    public static double swimBonus(EntityMaid m) {
        return FloralCombat.value(crown(m), Flower.BLUE_ORCHID);
    }

    static boolean sunny(EntityMaid m) {
        return m.level().isDay() && m.level().canSeeSky(m.blockPosition());
    }

    public static Phase eki(EntityMaid m) {
        return has(m, ModRegistry.EKI)
                        && RainState.weather(
                                m, RainConfig.EKI_LOCAL.get(), RainConfig.EKI_SKY.get())
                ? phase(m, TravailAspect.ABYSS)
                : Phase.INACTIVE;
    }

    static boolean tokaido(EntityMaid m) {
        return has(m, ModRegistry.TOKAIDO)
                && RainState.weather(
                        m, RainConfig.TOKAIDO_LOCAL.get(), RainConfig.TOKAIDO_SKY.get());
    }

    public static boolean seesUndead(LivingEntity e) {
        return e instanceof EntityMaid m
                && has(m, ModRegistry.THOUSAND_YEARS)
                && phase(m, TravailAspect.UNDERWORLD) == Phase.WITNESS;
    }

    public static boolean canHarm(LivingEntity attacker, Player other) {
        if (!(attacker instanceof EntityMaid m)) return false;
        var p = owner(m);
        return p != null && p != other && p.server.isPvpAllowed() && p.canHarmPlayer(other);
    }

    private MaidAccessoryRules() {}
}
