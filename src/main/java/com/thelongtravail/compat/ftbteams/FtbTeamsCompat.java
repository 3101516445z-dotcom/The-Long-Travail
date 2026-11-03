package com.thelongtravail.compat.ftbteams;

import com.thelongtravail.TheLongTravail;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.fml.ModList;

import java.util.*;

@net.minecraftforge.fml.common.Mod.EventBusSubscriber(modid = TheLongTravail.MODID)
public final class FtbTeamsCompat {
    public enum Relationship {
        SAME,
        DIFFERENT,
        UNAVAILABLE
    }

    interface Access {
        boolean installed();

        Object api() throws ReflectiveOperationException;

        Object loaded(Object api) throws ReflectiveOperationException;

        Object manager(Object api) throws ReflectiveOperationException;

        Object same(Object manager, UUID a, UUID b) throws ReflectiveOperationException;

        Object team(Object manager, ServerPlayer player) throws ReflectiveOperationException;

        Object members(Object team) throws ReflectiveOperationException;
    }

    private static Access access = new DirectAccess();
    private static long nextWarning;
    private static boolean warned;

    private static final class DirectAccess implements Access {
        public boolean installed() {
            return ModList.get().isLoaded("ftbteams");
        }

        public Object api() throws ReflectiveOperationException {
            return com.thelongtravail.compat.DirectCalls.call(
                    () -> dev.ftb.mods.ftbteams.api.FTBTeamsAPI.api());
        }

        public Object loaded(Object api) throws ReflectiveOperationException {
            return com.thelongtravail.compat.DirectCalls.call(
                    () -> ((dev.ftb.mods.ftbteams.api.FTBTeamsAPI.API) api).isManagerLoaded());
        }

        public Object manager(Object api) throws ReflectiveOperationException {
            return com.thelongtravail.compat.DirectCalls.call(
                    () -> ((dev.ftb.mods.ftbteams.api.FTBTeamsAPI.API) api).getManager());
        }

        public Object same(Object manager, UUID a, UUID b) throws ReflectiveOperationException {
            return com.thelongtravail.compat.DirectCalls.call(
                    () ->
                            ((dev.ftb.mods.ftbteams.api.TeamManager) manager)
                                    .arePlayersInSameTeam(a, b));
        }

        public Object team(Object manager, ServerPlayer player)
                throws ReflectiveOperationException {
            return com.thelongtravail.compat.DirectCalls.call(
                    () ->
                            ((dev.ftb.mods.ftbteams.api.TeamManager) manager)
                                    .getTeamForPlayer(player));
        }

        public Object members(Object team) throws ReflectiveOperationException {
            return com.thelongtravail.compat.DirectCalls.call(
                    () -> ((dev.ftb.mods.ftbteams.api.Team) team).getMembers());
        }
    }

    private static boolean bool(Object value) {
        if (!(value instanceof Boolean result))
            throw new IllegalStateException("Invalid FTB Teams boolean result");
        return result;
    }

    private static Object manager() throws ReflectiveOperationException {
        Object api = Objects.requireNonNull(access.api(), "FTB Teams API is null");
        if (!bool(access.loaded(api)))
            throw new IllegalStateException("FTB Teams manager is not loaded");
        return Objects.requireNonNull(access.manager(api), "FTB Teams manager is null");
    }

    private static void failure(Throwable failure) {
        long now = System.nanoTime();
        if (!warned || now - nextWarning >= 0) {
            warned = true;
            nextWarning = now + 60_000_000_000L;
            TheLongTravail.LOGGER.warn(
                    "FTB Teams query unavailable; player targets are excluded and time stop is"
                            + " cancelled",
                    failure);
        }
    }

    public static Relationship relationship(ServerPlayer player, Player other) {
        try {
            if (!access.installed()) return Relationship.DIFFERENT;
            return bool(access.same(manager(), player.getUUID(), other.getUUID()))
                    ? Relationship.SAME
                    : Relationship.DIFFERENT;
        } catch (ReflectiveOperationException | RuntimeException | LinkageError failure) {
            failure(failure);
            return Relationship.UNAVAILABLE;
        }
    }

    public static boolean lanternTeammates(ServerPlayer player, Player other) {
        return relationship(player, other) != Relationship.DIFFERENT;
    }

    // 空 Optional 表示查询失败；查询成功但没有队伍时，返回包含空集合的 Optional。
    public static Optional<Set<UUID>> timeStopMembers(ServerPlayer player) {
        try {
            if (!access.installed()) return Optional.of(Set.of());
            Object raw = access.team(manager(), player);
            if (!(raw instanceof Optional<?> team))
                throw new IllegalStateException("Invalid FTB Teams optional result");
            if (team.isEmpty()) return Optional.of(Set.of());
            Object members = access.members(team.get());
            if (!(members instanceof Collection<?> collection))
                throw new IllegalStateException("Invalid FTB Teams member collection");
            Set<UUID> result = new HashSet<>();
            for (Object member : collection) {
                if (!(member instanceof UUID id))
                    throw new IllegalStateException("Invalid FTB Teams member UUID");
                result.add(id);
            }
            return Optional.of(Set.copyOf(result));
        } catch (ReflectiveOperationException | RuntimeException | LinkageError failure) {
            failure(failure);
            return Optional.empty();
        }
    }

    @net.minecraftforge.eventbus.api.SubscribeEvent
    public static void stopped(net.minecraftforge.event.server.ServerStoppedEvent event) {
        warned = false;
        nextWarning = 0;
    }

    private FtbTeamsCompat() {}
}
