package com.thelongtravail.compat.maid;

import net.minecraftforge.event.TickEvent;

// 在游戏刻 END 阶段协调维护；状态存储不依赖周期效果执行组件。
final class MaidRuntimeMaintenance {
    public static void serverTick(TickEvent.ServerTickEvent e) {
        if (e.phase != TickEvent.Phase.END) return;
        for (var m : MaidRuntimeState.tracked()) {
            if (m.isRemoved() || !m.isAlive()) {
                MaidRuntimeState.forget(m);
            } else {
                com.thelongtravail.boundless.LorentzFantasy.refresh(m);
                MaidPeriodicEffects.tick(m);
            }
        }
        MaidRuntimeState.drainPending();
    }

    private MaidRuntimeMaintenance() {}
}
