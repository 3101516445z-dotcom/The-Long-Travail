package com.thelongtravail.client;

import com.thelongtravail.network.LanternPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.SectionPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import java.util.*;

@Mod.EventBusSubscriber(modid="the_long_travail", value=Dist.CLIENT)
public final class LanternLightingClient {
    private record Status(ResourceLocation dimension, long received) {}
    private static final Map<UUID,Status> ACTIVE = new HashMap<>();
    private static Map<UUID,LanternLighting.Source> previous = Map.of();
    private record SectionBounds(int x0, int x1, int y0, int y1, int z0, int z1) {
        static SectionBounds of(LanternLighting.Source source) {
            double r = LanternLighting.RADIUS + 1;
            return new SectionBounds(Mth.floor(source.x()-r)>>4, Mth.floor(source.x()+r)>>4,
                    Mth.floor(source.y()-r)>>4, Mth.floor(source.y()+r)>>4,
                    Mth.floor(source.z()-r)>>4, Mth.floor(source.z()+r)>>4);
        }
        Set<Long> sections() {
            Set<Long> result = new HashSet<>();
            for (int x=x0; x<=x1; x++) for (int y=y0; y<=y1; y++) for (int z=z0; z<=z1; z++)
                result.add(SectionPos.asLong(x,y,z));
            return Set.copyOf(result);
        }
    }
    private record Footprint(SectionBounds bounds, Set<Long> sections) {}
    private static final Map<UUID, Footprint> footprints = new HashMap<>();
    private static final Map<UUID,LanternLighting.Source> current = new HashMap<>();
    private static volatile LanternLightSnapshot lights = LanternLightSnapshot.EMPTY;
    private static ClientLevel world;
    private static long lastRebuild;
    public static LanternLighting.Source[] snapshot() { return lights.sources(); }
    public static LanternLightSnapshot lightingSnapshot() { return lights; }
    public static void receive(LanternPacket p) {
        if(p.active()) ACTIVE.put(p.player(), new Status(p.dimension(),System.nanoTime()));
        else ACTIVE.remove(p.player());
    }
    @SubscribeEvent public static void logout(net.minecraftforge.client.event.ClientPlayerNetworkEvent.LoggingOut event) {
        ACTIVE.clear();previous=Map.of();footprints.clear();current.clear();lights=LanternLightSnapshot.EMPTY;world=null;
    }
    @SubscribeEvent public static void frame(TickEvent.RenderTickEvent event) {
        if(event.phase != TickEvent.Phase.START) return;
        Minecraft mc=Minecraft.getInstance();
        if(mc.level==null) { lights=LanternLightSnapshot.EMPTY;previous=Map.of();footprints.clear();current.clear();world=null;return; }
        if(world!=mc.level) { world=mc.level;previous=Map.of();footprints.clear();lights=LanternLightSnapshot.EMPTY;lastRebuild=0; }
        long now=System.nanoTime();
        // 游戏暂停期间，已确认的装备状态不会过期。
        if(mc.isPaused()) ACTIVE.replaceAll((id,s)->new Status(s.dimension,now));
        else ACTIVE.values().removeIf(s->now-s.received>10_000_000_000L);
        current.clear();
        float partial=mc.isPaused()?mc.getFrameTime():event.renderTickTime;
        for(var p:mc.level.players()) {
            Status status=ACTIVE.get(p.getUUID());
            if(status==null||!status.dimension.equals(mc.level.dimension().location())||!p.isAlive()||p.isSpectator())continue;
            double x=Mth.lerp(partial,p.xo,p.getX()),y=Mth.lerp(partial,p.yo,p.getY())+p.getBbHeight()*.55,z=Mth.lerp(partial,p.zo,p.getZ());
            current.put(p.getUUID(),new LanternLighting.Source(x,y,z));
        }
        boolean membership=!previous.keySet().equals(current.keySet());
        boolean moved=membership;
        if(!moved) for(var entry:current.entrySet()) {
            var a=entry.getValue();var b=previous.get(entry.getKey());
            if(Math.abs(a.x()-b.x())>.08||Math.abs(a.y()-b.y())>.08||Math.abs(a.z()-b.z())>.08){moved=true;break;}
        }
        // 渲染线程每帧采样；区段重建最多每 33ms 一批，避免高帧率反复撤销异步构建。
        if(!moved||!membership&&now-lastRebuild<33_000_000L)return;
        lights=new LanternLightSnapshot(current.values().toArray(LanternLighting.Source[]::new));
        Set<Long> dirty=new HashSet<>();
        for(UUID id:previous.keySet()) if(!Objects.equals(previous.get(id),current.get(id))) {
            Footprint old = footprints.get(id);
            if (old != null) dirty.addAll(old.sections());
        }
        for(UUID id:current.keySet()) if(!Objects.equals(previous.get(id),current.get(id))) {
            SectionBounds bounds = SectionBounds.of(current.get(id));
            Footprint footprint = footprints.get(id);
            if (footprint == null || !footprint.bounds().equals(bounds)) {
                footprint = new Footprint(bounds, bounds.sections());
                footprints.put(id, footprint);
            }
            // 即使区段集合不变，区段内移动仍必须更新亮度。
            dirty.addAll(footprint.sections());
        }
        footprints.keySet().retainAll(current.keySet());
        previous=Map.copyOf(current);lastRebuild=now;
        for(long packed:dirty) {
            int x=SectionPos.x(packed),y=SectionPos.y(packed),z=SectionPos.z(packed);
            if(y>=mc.level.getMinSection()&&y<mc.level.getMaxSection()) {
                com.thelongtravail.data.HotPathMetrics.Counter.LANTERN_DIRTY_SECTIONS.add(1);
                mc.levelRenderer.setSectionDirty(x,y,z);
            }
        }
    }
    public static int light(double x,double y,double z,int packed) { return lights.merge(x,y,z,packed); }
    private LanternLightingClient() {}
}
