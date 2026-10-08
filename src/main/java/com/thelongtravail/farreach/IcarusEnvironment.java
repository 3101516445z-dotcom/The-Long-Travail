package com.thelongtravail.farreach;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

public final class IcarusEnvironment {
    public static double sunlight(long dayTime){long t=Math.floorMod(dayTime,24000L);return t<12000?Math.max(0,Math.sin(Math.PI*t/12000.0)):0;}
    public static boolean sunny(Level level){return !level.dimension().equals(Level.NETHER)&&(level.dimension().equals(Level.END)||(!level.getLevelData().isRaining()&&!level.getLevelData().isThundering()));}
    public static boolean openSky(Player p){
        Level level=p.level();BlockPos eye=BlockPos.containing(p.getX(),p.getEyeY(),p.getZ());
        if(!level.dimension().equals(Level.NETHER)&&!level.dimension().equals(Level.END))return level.canSeeSky(eye);
        BlockPos.MutableBlockPos cursor=eye.mutable();
        // 无天空光照的维度不能使用 canSeeSky 判断，须检查垂直透光路径，透明玻璃不视为遮挡。
        // 低于世界最低高度时，原版查询始终返回空气，因此从最低可放置方块的位置开始检查遮挡。
        for(int y=Math.max(eye.getY(),level.getMinBuildHeight());y<level.getMaxBuildHeight();y++){
            cursor.setY(y);var state=level.getBlockState(cursor);
            if(state.getLightBlock(level,cursor)>0||!state.propagatesSkylightDown(level,cursor))return false;
        }
        return true;
    }
    private IcarusEnvironment(){}
}
