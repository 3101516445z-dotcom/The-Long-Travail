package com.thelongtravail.farreach;

import com.thelongtravail.TheLongTravail;
import com.thelongtravail.boundless.TimeStopManager;
import com.thelongtravail.config.FarReachItemsConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.*;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import java.util.*;

/**
 * 预留的生长工具，不注册到事件总线，也不由伊卡洛斯触发。
 * 后续饰品须显式调用 tick，并提供自身的生效条件。
 * 保留原有配置键和方块标签，供后续复用。
 */
public final class IcarusGrowth {
    public static final TagKey<Block> EXTRA=TagKey.create(Registries.BLOCK,new ResourceLocation(TheLongTravail.MODID,"icarus_growth"));
    private static final Map<ServerLevel,Map<Long,Long>> LAST=new WeakHashMap<>();
    public static boolean eligible(BlockState state){Block b=state.getBlock();return state.isRandomlyTicking()&&(b instanceof CropBlock||b instanceof StemBlock||b instanceof NetherWartBlock||b instanceof SaplingBlock||state.is(EXTRA));}
    public static boolean grow(ServerLevel level,BlockPos pos,long now){
        if(!level.hasChunkAt(pos)||TimeStopManager.frozen(level,pos))return false;
        BlockState state=level.getBlockState(pos);if(!eligible(state))return false;
        // 树木可能长到树苗所在区块之外，须先确认周围 3×3 个区块已加载。
        if(state.getBlock() instanceof SaplingBlock&&!level.isAreaLoaded(pos,16))return false;
        Map<Long,Long> times=LAST.computeIfAbsent(level,k->new HashMap<>());int interval=FarReachItemsConfig.ticks("icarus.growthInterval");
        if(now-times.getOrDefault(pos.asLong(),now-interval)<interval)return false;
        times.put(pos.asLong(),now);state.randomTick(level,pos,level.random);return true;
    }
    public static void tick(ServerLevel level,java.util.function.Predicate<ServerPlayer> source){
        long now=level.getGameTime();int radius=(int)FarReachItemsConfig.get("icarus.radius"),height=(int)FarReachItemsConfig.get("icarus.height");
        int interval=FarReachItemsConfig.ticks("icarus.growthInterval");
        int phase=Math.floorMod(now,interval);
        for(ServerPlayer player:level.players()){
            if(!source.test(player)||TimeStopManager.frozen(player))continue;
            BlockPos center=player.blockPosition();
            // 按固定世界坐标划分扫描条带，避免玩家移动时反复扫描同一片区域。
            for(int dx=-radius;dx<=radius;dx++)for(int dz=-radius;dz<=radius;dz++){
                if(dx*dx+dz*dz>radius*radius)continue;
                int x=center.getX()+dx,z=center.getZ()+dz;
                if(Math.floorMod(x*31L+z,interval)!=phase)continue;
                for(int dy=-height;dy<=height;dy++)grow(level,new BlockPos(x,center.getY()+dy,z),now);
            }
        }
        Map<Long,Long> times=LAST.get(level);if(times!=null&&now%200==0){times.values().removeIf(t->now-t>Math.max(200,interval));if(times.isEmpty())LAST.remove(level);}
    }
    private IcarusGrowth(){}
}
