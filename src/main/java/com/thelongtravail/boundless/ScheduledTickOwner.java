package com.thelongtravail.boundless;

import net.minecraft.server.level.ServerLevel;

public interface ScheduledTickOwner {
    void travail$bindTickWorld(ServerLevel level);
}
