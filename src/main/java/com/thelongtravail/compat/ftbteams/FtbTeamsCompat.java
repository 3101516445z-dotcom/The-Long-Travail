package com.thelongtravail.compat.ftbteams;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.fml.ModList;
import java.util.*;

public final class FtbTeamsCompat {
    private static boolean timeStopWarned;
    private static boolean warned;

    public static boolean lanternTeammates(ServerPlayer p, Player other) {
        if (!ModList.get().isLoaded("ftbteams")) return false;
        try {
            Object api =
                    Class.forName("dev.ftb.mods.ftbteams.api.FTBTeamsAPI")
                            .getMethod("api")
                            .invoke(null);
            Class<?> apiType = Class.forName("dev.ftb.mods.ftbteams.api.FTBTeamsAPI$API");
            if (!(boolean) apiType.getMethod("isManagerLoaded").invoke(api)) return false;
            Object manager = apiType.getMethod("getManager").invoke(api);
            return (boolean)
                    Class.forName("dev.ftb.mods.ftbteams.api.TeamManager")
                            .getMethod("arePlayersInSameTeam", UUID.class, UUID.class)
                            .invoke(manager, p.getUUID(), other.getUUID());
        } catch (ReflectiveOperationException | LinkageError e) {
            if (!warned) {
                warned = true;
                com.thelongtravail.TheLongTravail.LOGGER.warn("剑与灯 FTB Teams 关系查询失败", e);
            }
            return true; // 无法确认时保守排除玩家，不能把可能的队友视为敌人。
        }
    }

    public static boolean timeStopTeammates(ServerPlayer p, ServerPlayer other) {
        boolean same = false;
        if (ModList.get().isLoaded("ftbteams"))
            try {
                Class<?> apiClass = Class.forName("dev.ftb.mods.ftbteams.api.FTBTeamsAPI");
                Object api = apiClass.getMethod("api").invoke(null);
                Class<?> apiInterface = Class.forName("dev.ftb.mods.ftbteams.api.FTBTeamsAPI$API");
                if ((boolean) apiInterface.getMethod("isManagerLoaded").invoke(api)) {
                    Object manager = apiInterface.getMethod("getManager").invoke(api);
                    same =
                            (boolean)
                                    Class.forName("dev.ftb.mods.ftbteams.api.TeamManager")
                                            .getMethod(
                                                    "arePlayersInSameTeam", UUID.class, UUID.class)
                                            .invoke(manager, p.getUUID(), other.getUUID());
                }
            } catch (ReflectiveOperationException | LinkageError e) {
                if (!timeStopWarned) {
                    timeStopWarned = true;
                    com.thelongtravail.TheLongTravail.LOGGER.warn("FTB Teams时停豁免适配不可用", e);
                }
            }
        return same;
    }

    public static void addTimeStopMembers(ServerPlayer p, Set<UUID> out) {
        if (ModList.get().isLoaded("ftbteams"))
            try {
                Class<?> type = Class.forName("dev.ftb.mods.ftbteams.api.FTBTeamsAPI$API");
                Object api =
                        Class.forName("dev.ftb.mods.ftbteams.api.FTBTeamsAPI")
                                .getMethod("api")
                                .invoke(null);
                if ((boolean) type.getMethod("isManagerLoaded").invoke(api)) {
                    Object manager = type.getMethod("getManager").invoke(api);
                    var team =
                            (Optional<?>)
                                    Class.forName("dev.ftb.mods.ftbteams.api.TeamManager")
                                            .getMethod("getTeamForPlayer", ServerPlayer.class)
                                            .invoke(manager, p);
                    if (team.isPresent())
                        for (Object member :
                                (Collection<?>)
                                        Class.forName("dev.ftb.mods.ftbteams.api.Team")
                                                .getMethod("getMembers")
                                                .invoke(team.get()))
                            if (member instanceof UUID id) out.add(id);
                }
            } catch (ReflectiveOperationException | LinkageError e) {
                if (!timeStopWarned) {
                    timeStopWarned = true;
                    com.thelongtravail.TheLongTravail.LOGGER.warn("FTB Teams时停豁免快照不可用", e);
                }
            }
    }

    private FtbTeamsCompat() {}
}
