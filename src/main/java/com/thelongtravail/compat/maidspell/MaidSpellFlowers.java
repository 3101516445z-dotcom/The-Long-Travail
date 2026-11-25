package com.thelongtravail.compat.maidspell;

import static com.thelongtravail.flourishing.FloralCombat.value;

import com.thelongtravail.config.FlourishingItemsConfig;
import com.thelongtravail.flourishing.Flower;

import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public final class MaidSpellFlowers {
    public static final String SCARLET_ZHUHUA = "touhou_little_maid_spell:scarlet_zhuhua";
    public static final String YUE_LINGLAN = "touhou_little_maid_spell:yue_linglan";
    public static final String JINGXU_YOULAN = "touhou_little_maid_spell:jingxu_youlan";
    public static final String STAR_GLOW_FLOWER_CLUSTER =
            "touhou_little_maid_spell:star_glow_flower_cluster";

    public static double damageBonus(ItemStack crown, LivingEntity wearer, LivingEntity victim) {
        double bonus =
                victim.hasEffect(MobEffects.WITHER) ? value(crown, Flower.SCARLET_ZHUHUA) : 0;
        if (wearer.level().dimension().equals(Level.END))
            bonus += value(crown, Flower.STAR_GLOW_FLOWER_CLUSTER);
        return bonus;
    }

    public static double reduction(ItemStack crown, LivingEntity wearer) {
        double orchid = value(crown, Flower.JINGXU_YOULAN);
        if (orchid == 0) return 0;
        for (var effect : wearer.getActiveEffects())
            if (effect.getEffect().getCategory() == MobEffectCategory.HARMFUL) return orchid;
        return 0;
    }

    public static double healingBonus(ItemStack crown, LivingEntity wearer) {
        return wearer.getHealth()
                        < wearer.getMaxHealth()
                                * FlourishingItemsConfig.extra(
                                        Flower.YUE_LINGLAN, "healthThreshold")
                ? value(crown, Flower.YUE_LINGLAN)
                : 0;
    }

    private MaidSpellFlowers() {}
}
