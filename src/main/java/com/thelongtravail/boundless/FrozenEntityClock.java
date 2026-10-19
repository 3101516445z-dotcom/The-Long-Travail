package com.thelongtravail.boundless;

import net.minecraft.world.entity.*;
import java.util.*;

public final class FrozenEntityClock {
    public interface Clock {
        boolean travail$markClock(long tick);
    }

    // 实体若已在本游戏刻正常更新，进入冻结后就不能再由 doTick 重复更新受伤计时。
    public static void normal(Entity e) {
        if (e instanceof LivingEntity) ((Clock) e).travail$markClock(clock(e));
    }

    public static void tick(Entity e) {
        long now = clock(e);
        if (!((Clock) e).travail$markClock(now)) return;
        if (e instanceof net.minecraft.server.level.ServerPlayer p) {
            com.thelongtravail.data.StiffState.pause(p);
            com.thelongtravail.data.VisualDeprivation.pause(p);
        }
        if (e instanceof LivingEntity && e.invulnerableTime > 0) e.invulnerableTime--;
    }

    private static long clock(Entity e) {
        return e.level() instanceof net.minecraft.server.level.ServerLevel level
                ? level.getServer().getTickCount()
                : e.level().getGameTime();
    }

    public static void clear() {} // 时间戳保存在实体内部，实体销毁后无需单独清理。

    private FrozenEntityClock() {}
}
