package com.thelongtravail.compat.maid;

import net.minecraftforge.event.TickEvent;

/** Coordinates each END tick; state storage does not depend on periodic effect execution. */
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
