package com.thelongtravail.data;

import com.thelongtravail.TravailAspect;
import com.thelongtravail.helper.TravailCurios;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.player.Player;

/** 音效规则不依赖来源；查询当前装备时不修改物品状态。 */
public final class WitnessSoundRule {
    public static boolean muteGain(Player player, MobEffect effect, boolean gain, boolean enabled) {
        if (!enabled || !gain || player == null || effect == null || !effect.isBeneficial()) return false;
        var diary = TravailCurios.stack(player);
        return !diary.isEmpty() && (LongTravailData.hasWitness(diary, TravailAspect.FAR_REACH)
                || LongTravailData.hasWitness(diary, TravailAspect.DEEP_VALLEY));
    }

    private WitnessSoundRule() {}
}
