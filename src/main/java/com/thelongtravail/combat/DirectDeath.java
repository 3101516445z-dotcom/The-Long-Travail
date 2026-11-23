package com.thelongtravail.combat;

import com.thelongtravail.mixin.execution.AzraelLivingAccessor;
import com.thelongtravail.valley.execution.ExecutionData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

public final class DirectDeath {
    public static void creditPlayer(LivingEntity target, ServerPlayer player) {
        target.setLastHurtByPlayer(player);
        ((AzraelLivingAccessor) target).travail$recentPlayerHit(100);
    }

    // 只走普通死亡和掉落流程。Forge 取消死亡时，恢复本次尝试写入的状态。
    public static boolean tryPlayerKill(LivingEntity target, ServerPlayer player) {
        var state = (AzraelLivingAccessor) target;
        if (!target.isAlive() || state.travail$dead()) return false;
        var data = (ExecutionData) target.getEntityData();
        float health = data.travail$storedHealth();
        Player previousPlayer = state.travail$lastPlayer();
        int previousTime = state.travail$recentPlayerHit();
        try {
            creditPlayer(target, player);
            data.travail$commitHealth(0);
            target.die(player.damageSources().playerAttack(player));
            return state.travail$dead();
        } finally {
            if (!state.travail$dead() && !target.isRemoved()) {
                if (data.travail$storedHealth() <= 0) data.travail$commitHealth(health);
                if (state.travail$lastPlayer() == player
                        && state.travail$recentPlayerHit() == 100) {
                    target.setLastHurtByPlayer(previousPlayer);
                    state.travail$recentPlayerHit(previousTime);
                }
            }
        }
    }

    private DirectDeath() {}
}
