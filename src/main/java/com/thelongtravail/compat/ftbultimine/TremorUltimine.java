package com.thelongtravail.compat.ftbultimine;

import com.thelongtravail.TheLongTravail;
import com.thelongtravail.TravailAspect;
import com.thelongtravail.api.TravailStateApi;
import com.thelongtravail.valley.PlayerBlockBreaker;
import com.thelongtravail.valley.TremorFeedback;
import net.minecraft.core.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.fml.ModList;
import java.util.*;

// 可选联动桥接：未安装 FTB 时不链接其类型。
public final class TremorUltimine {
    private static final ThreadLocal<Boolean> DISPATCH = ThreadLocal.withInitial(() -> false);
    private static final ThreadLocal<Boolean> HANDLED = ThreadLocal.withInitial(() -> false);

    public static boolean dispatching() {
        return DISPATCH.get();
    }

    public static Object pass() {
        try {
            return Class.forName("dev.architectury.event.EventResult")
                    .getMethod("pass")
                    .invoke(null);
        } catch (ReflectiveOperationException e) {
            if (e.getCause() instanceof Error error) throw error;
            throw new IllegalStateException("Architectury EventResult contract changed", e);
        }
    }

    public static boolean tryBreak(
            ServerPlayer player, InteractionHand hand, BlockPos pos, Direction face) {
        if (!ModList.get().isLoaded("ftbultimine")) return false;
        DISPATCH.set(true);
        HANDLED.set(false);
        try {
            Class<?> type = Class.forName("dev.ftb.mods.ftbultimine.FTBUltimine");
            Object instance = type.getField("instance").get(null);
            type.getMethod(
                            "blockRightClick",
                            Player.class,
                            InteractionHand.class,
                            BlockPos.class,
                            Direction.class)
                    .invoke(instance, player, hand, pos, face);
            return HANDLED.get();
        } catch (ReflectiveOperationException e) {
            if (e.getCause() instanceof Error error) throw error;
            TheLongTravail.LOGGER.error(
                    "High Frequency Tremor FTB bridge failed; no fallback break performed", e);
            TremorFeedback.FAILURE.send(player);
            return true;
        } finally {
            DISPATCH.remove();
            HANDLED.remove();
        }
    }

    public static int batch(ServerPlayer player, Object data) {
        HANDLED.set(true);
        var level = player.serverLevel();
        var held = player.getMainHandItem();
        var mode = TravailStateApi.state(player, TravailAspect.DEEP_VALLEY);
        try {
            Object raw = data.getClass().getMethod("cachedPositions").invoke(data);
            if (!(raw instanceof Collection<?> positions)) return 0;
            Set<BlockPos> snapshot = new LinkedHashSet<>();
            for (Object entry : positions) {
                if (!(entry instanceof BlockPos pos)) return 0;
                snapshot.add(pos.immutable());
            }
            int count = 0;
            for (BlockPos pos : snapshot) {
                var state = level.hasChunkAt(pos) ? level.getBlockState(pos) : null;
                try {
                    if (!PlayerBlockBreaker.sameActor(player, level, held, mode)) break;
                    if (PlayerBlockBreaker.breakBlock(player, pos, mode)) count++;
                } catch (RuntimeException failure) {
                    PlayerBlockBreaker.reportFailure(level, pos, state, failure);
                    TremorFeedback.FAILURE.send(player);
                    break;
                }
            }
            return count;
        } catch (ReflectiveOperationException e) {
            if (e.getCause() instanceof Error error) throw error;
            TheLongTravail.LOGGER.error(
                    "Unable to read FTB selection for High Frequency Tremor", e);
            TremorFeedback.FAILURE.send(player);
            return 0;
        }
    }

    private TremorUltimine() {}
}
