package com.thelongtravail.valley;

import com.thelongtravail.network.LanternPacket;
import com.thelongtravail.network.TravailNetwork;
import net.minecraft.server.level.ServerPlayer;
import java.util.*;

public final class LanternSync {
    private static final Map<UUID,Boolean> LAST = new HashMap<>();
    public static void tick(ServerPlayer p) {
        boolean active = SwordLanternState.equipped(p);
        Boolean last = LAST.put(p.getUUID(), active);
        if (last == null || last != active || active && p.server.getTickCount() % 20 == 0)
            TravailNetwork.sendLantern(p, packet(p, active));
    }
    private static LanternPacket packet(ServerPlayer p, boolean active) {
        return new LanternPacket(p.level().dimension().location(), p.getUUID(), active);
    }
    public static void tracking(ServerPlayer viewer, ServerPlayer target) {
        TravailNetwork.sendLanternTo(viewer, packet(target, SwordLanternState.equipped(target)));
    }
    public static void forget(ServerPlayer p) {
        LAST.remove(p.getUUID()); TravailNetwork.sendLantern(p, packet(p, false));
    }
    public static void clear() { LAST.clear(); }
    private LanternSync() {}
}
