package com.thelongtravail.api;
import com.thelongtravail.boundless.TimeStopManager;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
// 供全局计时脚本和第三方机器调用，须在对应逻辑端的游戏线程中使用。
public final class TimeStopApi {
    public static boolean isFrozen(Entity e){return e!=null&&TimeStopManager.frozen(e);}
    public static boolean isFrozen(Level l,BlockPos p){return l!=null&&p!=null&&TimeStopManager.frozen(l,p);}
    private TimeStopApi(){}
}
