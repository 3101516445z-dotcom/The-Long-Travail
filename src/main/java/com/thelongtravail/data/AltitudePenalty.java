package com.thelongtravail.data;

import com.thelongtravail.TravailAspect;
import com.thelongtravail.config.TravailConfig;
import com.thelongtravail.helper.TravailCurios;
import com.thelongtravail.network.TooltipConfigSync;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

// 高空减速在读取速度时计算，不写入 AttributeModifier， 避免被其他模组通过抵消修饰符或重算属性移除。
public final class AltitudePenalty {
    // 缓存由各 Player 实例持有，避免全局玩家引用和共享缓存锁。
    public interface Cache {
        float travail$altitudeFactor();
        void travail$invalidateAltitude();
    }
    private static long serverRevision;
    public static void configReloaded() { serverRevision++; }
    public static long revision(Player player) {
        return player.level().isClientSide ? TooltipConfigSync.revision() : serverRevision;
    }

    // 移速乘数；1.0 表示无惩罚。
    public static float factor(Player player) {
        if (player == null) return 1.0F;
        return player instanceof Cache cache ? cache.travail$altitudeFactor() : compute(player);
    }

    public static void forget(Player player) {
        if (player instanceof Cache cache) cache.travail$invalidateAltitude();
    }

    public static float compute(Player player) {
        ItemStack diary = TravailCurios.stack(player);
        if (diary.isEmpty() || LongTravailData.hasWitness(diary, TravailAspect.BOUNDLESS)) return 1.0F;
        double threshold = TravailConfig.BOUNDLESS_HEIGHT_THRESHOLD.get();
        double reduction = TravailConfig.BOUNDLESS_SPEED_REDUCTION.get();
        if (player.level().isClientSide) {
            threshold = TooltipConfigSync.decimal("boundless.heightThreshold", threshold);
            reduction = TooltipConfigSync.decimal("boundless.speedReduction", reduction);
        }
        if (reduction <= 0.0D || player.getY() <= threshold) return 1.0F;
        return (float) (1.0D - Math.min(1.0D, reduction));
    }

    private AltitudePenalty() {}
}
