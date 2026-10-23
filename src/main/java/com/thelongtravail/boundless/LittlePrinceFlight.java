package com.thelongtravail.boundless;

import com.thelongtravail.api.CarryingApi.CarryingResult;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.server.level.ServerPlayer;

// 只维护飞行权限；起飞和停止飞行仍由原版输入控制。
public final class LittlePrinceFlight {
    private static final int SYNC_INTERVAL = 20;
    // 此标记仅用于恢复本版本授予的权限，不表示本模组独占飞行权限。
    private static final String ACTIVE = "TravailPrinceFlightActive";
    private static final Map<ServerPlayer, State> STATES = new WeakHashMap<>();

    private static final class State {
        boolean active;
        boolean forceSync;
        boolean pendingSync;
        boolean sent;
        boolean lastGrant;
        boolean pendingRevoke;
        int lastSync;
    }

    public static boolean update(ServerPlayer player, CarryingResult carrying) {
        State state = STATES.computeIfAbsent(player, p -> new State());
        boolean active =
                player.isAlive()
                        && !player.isSpectator()
                        && (carrying == CarryingResult.PRESENT
                                || carrying == CarryingResult.UNKNOWN && state.active);
        boolean previous = state.active || player.getPersistentData().getBoolean(ACTIVE);
        state.active = active;
        if (active) player.getPersistentData().putBoolean(ACTIVE, true);
        else player.getPersistentData().remove(ACTIVE);

        // 游戏模式负责管理自身能力，但仍须撤销客户端保存的旧授权。
        boolean protectedMode = player.isCreative() || player.isSpectator();
        boolean grant = active && !protectedMode;
        var abilities = player.getAbilities();
        if (grant) {
            boolean repair = !abilities.mayfly;
            abilities.mayfly = true;
            state.pendingSync |=
                    !previous
                            || repair
                            || state.forceSync
                            || !state.sent
                            || player.tickCount - state.lastSync >= SYNC_INTERVAL;
        } else if (previous && !protectedMode) {
            abilities.mayfly = false;
            abilities.flying = false;
            state.pendingSync = true;
            state.pendingRevoke = true;
        }
        state.pendingSync |= state.forceSync || (state.sent && state.lastGrant != grant);
        state.forceSync = false;
        // 同一游戏刻内多次刷新可能改变状态，最终状态留到下一刻发送，
        // 避免重复发包，同时保留尚未发送的授权或撤销状态。
        if (state.pendingSync && (!state.sent || state.lastSync != player.tickCount)) {
            com.thelongtravail.network.TravailNetwork.sendPrinceFlight(
                    player, grant, !grant && state.pendingRevoke);
            state.lastGrant = grant;
            state.lastSync = player.tickCount;
            state.sent = true;
            state.pendingSync = false;
            state.pendingRevoke = false;
        }
        return active;
    }

    public static void resync(ServerPlayer player) {
        STATES.computeIfAbsent(player, p -> new State()).forceSync = true;
    }

    public static void forget(ServerPlayer player) {
        STATES.remove(player);
    }

    public static void clear() {
        STATES.clear();
    }

    private LittlePrinceFlight() {}
}
