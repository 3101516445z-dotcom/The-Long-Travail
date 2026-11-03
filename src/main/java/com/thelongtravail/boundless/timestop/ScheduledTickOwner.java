package com.thelongtravail.boundless.timestop;

import net.minecraft.server.level.ServerLevel;

public interface ScheduledTickOwner {
    void travail$bindTickWorld(ServerLevel level);
}
