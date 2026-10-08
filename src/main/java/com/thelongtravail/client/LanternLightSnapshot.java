package com.thelongtravail.client;

import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.core.SectionPos;
import java.util.*;

// 快照构建后只读，可供网格构建线程安全使用；查询时按实际采样位置选择区段。
public final class LanternLightSnapshot {
    public static final LanternLightSnapshot EMPTY=new LanternLightSnapshot(new LanternLighting.Source[0]);
    private static final LanternLighting.Source[] NONE=new LanternLighting.Source[0];
    private final LanternLighting.Source[] all;
    private final Long2ObjectOpenHashMap<LanternLighting.Source[]> sections=new Long2ObjectOpenHashMap<>();
    public LanternLightSnapshot(LanternLighting.Source[] sources){
        all=sources.clone();
        var building=new Long2ObjectOpenHashMap<List<LanternLighting.Source>>();
        double radius=LanternLighting.RADIUS+1;
        for(var source:all)
            for(int x=section(source.x()-radius);x<=section(source.x()+radius);x++)
                for(int y=section(source.y()-radius);y<=section(source.y()+radius);y++)
                    for(int z=section(source.z()-radius);z<=section(source.z()+radius);z++)
                        building.computeIfAbsent(SectionPos.asLong(x,y,z),k->new ArrayList<>()).add(source);
        building.long2ObjectEntrySet().forEach(entry->sections.put(entry.getLongKey(),entry.getValue().toArray(LanternLighting.Source[]::new)));
    }
    private static int section(double value){return (int)Math.floor(value/16);}
    public boolean isEmpty(){return all.length==0;}
    public LanternLighting.Source[] sources(){return all.clone();}
    public int merge(double x,double y,double z,int packed){
        if((packed&255)>=240||all.length==0)return packed;
        var nearby=sections.get(SectionPos.asLong(section(x),section(y),section(z)));
        com.thelongtravail.data.HotPathMetrics.Counter.LIGHT_CANDIDATES.add(nearby==null?0:nearby.length);
        return LanternLighting.merge(nearby==null?NONE:nearby,x,y,z,packed);
    }
}
