package com.thelongtravail.boundless;
import net.minecraft.world.entity.*;
import java.util.*;
// 无敌帧是唯一放行的原版受伤计时，每个实体每服务端刻只递减一次。
public final class FrozenEntityClock {
    public interface Clock { boolean travail$markClock(long tick); }
    // 本刻已正常更新的实体进入冻结后，不能再由 doTick 重复更新受伤计时。
    public static void normal(Entity e){if(e instanceof LivingEntity)((Clock)e).travail$markClock(clock(e));}
    public static void tick(Entity e){long now=clock(e);if(!((Clock)e).travail$markClock(now))return;
        if(e instanceof net.minecraft.server.level.ServerPlayer p){com.thelongtravail.data.StiffState.pause(p);com.thelongtravail.data.VisualDeprivation.pause(p);}
        if(e instanceof LivingEntity&&e.invulnerableTime>0)e.invulnerableTime--;
    }
    private static long clock(Entity e){return e.level() instanceof net.minecraft.server.level.ServerLevel level?level.getServer().getTickCount():e.level().getGameTime();}
    public static void clear(){} // 时间戳随实体销毁，无需集中清理。
    private FrozenEntityClock(){}
}
