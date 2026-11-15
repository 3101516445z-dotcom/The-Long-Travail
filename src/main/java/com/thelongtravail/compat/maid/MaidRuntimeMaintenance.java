package com.thelongtravail.compat.maid;

import net.minecraftforge.event.TickEvent;

// 在游戏刻 END 阶段协调维护；状态存储不依赖周期效果执行组件。
final class MaidRuntimeMaintenance {
    private static int extraSlots = -1;

    public static void serverTick(TickEvent.ServerTickEvent e) {
        if (e.phase != TickEvent.Phase.END) return;
        int configured =
                (int) com.thelongtravail.config.ThemeItemConfig.get("sea_in_a_bottle.extraSlots");
        boolean slotsChanged = configured != extraSlots;
        extraSlots = configured;
        for (var m : MaidRuntimeState.tracked()) {
            if (slotsChanged) MaidSeaBottleSlots.changed(m);
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
