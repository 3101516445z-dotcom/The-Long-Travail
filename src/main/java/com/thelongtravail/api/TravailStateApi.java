package com.thelongtravail.api;

import com.thelongtravail.TravailAspect;
import com.thelongtravail.data.LongTravailData;
import com.thelongtravail.helper.TravailCurios;
import com.thelongtravail.registry.ModRegistry;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;
import java.util.Objects;

/**
 * 供外部集成使用的只读状态查询。玩家查询只采用 Curios 中首个已装备的日记，背包中的日记不生效。
 * 须在逻辑游戏线程调用；服务端状态为准，客户端读取同步副本。
 * MALICE/WITNESS 表示所属状态，不代表其条件效果此刻正在触发。
 */
public final class TravailStateApi {
    public enum AspectState { INACTIVE, MALICE, WITNESS }

    /** 未装备日记时返回 INACTIVE；已装备但未初始化的日记按现有规则判定。 */
    public static AspectState state(@Nullable Player player, TravailAspect aspect) {
        Objects.requireNonNull(aspect, "aspect");
        ItemStack diary = equipped(player);
        if (diary.isEmpty()) return AspectState.INACTIVE;
        return LongTravailData.hasWitness(diary, aspect) ? AspectState.WITNESS : AspectState.MALICE;
    }

    public static boolean hasMalice(@Nullable Player player, TravailAspect aspect) {
        return state(player, aspect) == AspectState.MALICE;
    }
    public static boolean hasWitness(@Nullable Player player, TravailAspect aspect) {
        return state(player, aspect) == AspectState.WITNESS;
    }

    public static boolean hasFlourishingMalice(@Nullable Player player) { return hasMalice(player, TravailAspect.FLOURISHING); }
    public static boolean hasFlourishingWitness(@Nullable Player player) { return hasWitness(player, TravailAspect.FLOURISHING); }
    public static boolean hasAbyssMalice(@Nullable Player player) { return hasMalice(player, TravailAspect.ABYSS); }
    public static boolean hasAbyssWitness(@Nullable Player player) { return hasWitness(player, TravailAspect.ABYSS); }
    public static boolean hasFarReachMalice(@Nullable Player player) { return hasMalice(player, TravailAspect.FAR_REACH); }
    public static boolean hasFarReachWitness(@Nullable Player player) { return hasWitness(player, TravailAspect.FAR_REACH); }
    public static boolean hasDeepValleyMalice(@Nullable Player player) { return hasMalice(player, TravailAspect.DEEP_VALLEY); }
    public static boolean hasDeepValleyWitness(@Nullable Player player) { return hasWitness(player, TravailAspect.DEEP_VALLEY); }
    public static boolean hasUnderworldMalice(@Nullable Player player) { return hasMalice(player, TravailAspect.UNDERWORLD); }
    public static boolean hasUnderworldWitness(@Nullable Player player) { return hasWitness(player, TravailAspect.UNDERWORLD); }
    public static boolean hasBoundlessMalice(@Nullable Player player) { return hasMalice(player, TravailAspect.BOUNDLESS); }
    public static boolean hasBoundlessWitness(@Nullable Player player) { return hasWitness(player, TravailAspect.BOUNDLESS); }

    /** 一次装备查询即可读取多个状态；状态变更后须重新获取快照。 */
    public static Snapshot snapshot(@Nullable Player player) { return inspectDiary(equipped(player)); }

    /** 仅读取物品保存的状态，不保证该物品已被装备。 */
    public static Snapshot inspectDiary(@Nullable ItemStack diary) {
        if (diary == null || diary.isEmpty() || !diary.is(ModRegistry.LONG_TRAVAIL.get())) return Snapshot.ABSENT;
        int witnesses = 0;
        for (TravailAspect aspect : TravailAspect.values()) {
            if (LongTravailData.hasWitness(diary, aspect)) witnesses |= aspect.mask();
        }
        return new Snapshot(true, LongTravailData.isInitialized(diary), witnesses);
    }

    /** 不可变快照，不持有 ItemStack 或 Player 引用，也不写入 NBT。 */
    public static final class Snapshot {
        private static final Snapshot ABSENT = new Snapshot(false, false, 0);
        private final boolean diaryPresent, initialized;
        private final int witnesses;
        private Snapshot(boolean diaryPresent, boolean initialized, int witnesses) {
            this.diaryPresent = diaryPresent; this.initialized = initialized; this.witnesses = witnesses;
        }
        public boolean diaryPresent() { return diaryPresent; }
        public boolean initialized() { return initialized; }
        public AspectState state(TravailAspect aspect) {
            Objects.requireNonNull(aspect, "aspect");
            if (!diaryPresent) return AspectState.INACTIVE;
            return (witnesses & aspect.mask()) != 0 ? AspectState.WITNESS : AspectState.MALICE;
        }
        public boolean hasMalice(TravailAspect aspect) { return state(aspect) == AspectState.MALICE; }
        public boolean hasWitness(TravailAspect aspect) { return state(aspect) == AspectState.WITNESS; }
    }

    private static ItemStack equipped(@Nullable Player player) {
        return player == null ? ItemStack.EMPTY : TravailCurios.stack(player);
    }
    private TravailStateApi() {}
}
