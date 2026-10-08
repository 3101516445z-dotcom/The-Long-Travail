package com.thelongtravail.boundless;

import com.thelongtravail.config.BoundlessItemsConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.level.*;
import net.minecraft.world.phys.Vec3;
import java.util.*;

// 只在逻辑服务端线程使用。组锁定与区域冻结严格分开。
public final class TimeStopManager {
    public static final class Field {
        public final long id,start; public long end; public final Vec3 center; public final double radius;
        public final Set<UUID> allies; final Set<Long> chunks; Group group;
        Field(long id,long start,long end,Vec3 center,double radius,Set<UUID> allies,Group group){this.id=id;this.start=start;this.end=end;this.center=center;this.radius=radius;this.allies=Set.copyOf(allies);this.group=group;chunks=TimeStopGeometry.chunks(center,radius);}
        public boolean contains(Vec3 p){return center.distanceToSqr(p)<=radius*radius;}
    }
    private static final class Group {
        long deadline; final Map<UUID,Integer> members=new HashMap<>();
        Group(long deadline){this.deadline=deadline;}
    }
    private static final Map<ServerLevel,TimeStopManager> WORLDS=new WeakHashMap<>();
    private static final Map<UUID,Integer> OFFLINE_COOLDOWNS=new HashMap<>();
    public final ServerLevel level;
    private final Map<Long,Field> fields=new LinkedHashMap<>();
    private final Map<Long,List<Field>> index=new HashMap<>();
    private Set<Long> coverage=Set.of();
    private final Set<Group> groups=Collections.newSetFromMap(new IdentityHashMap<>());
    private long nextId=1, revision; boolean dirty;
    public long revision(){return revision;}
    public static boolean active(Level level){if(!(level instanceof ServerLevel s))return false;var m=WORLDS.get(s);return m!=null&&!m.fields.isEmpty();}
    private TimeStopManager(ServerLevel level){this.level=level;}
    public static TimeStopManager get(ServerLevel l){return WORLDS.computeIfAbsent(l,TimeStopManager::new);}
    public static Collection<Field> fields(ServerLevel l){var m=WORLDS.get(l);return m==null?List.of():List.copyOf(m.fields.values());}
    public static boolean locked(UUID player){for(var m:WORLDS.values())for(var g:m.groups)if(g.members.containsKey(player))return true;return false;}
    public static boolean frozen(Level l,BlockPos p){
        if(l.isClientSide)return com.thelongtravail.network.TimeStopPacket.clientBlock.test(l,p);
        if(!(l instanceof ServerLevel server))return false;
        var manager=WORLDS.get(server);
        if(manager==null||manager.fields.isEmpty())return false;
        var candidates=manager.index.get(ChunkPos.asLong(p.getX()>>4,p.getZ()>>4));
        if(candidates==null)return false;
        for(Field f:candidates){double x=p.getX()+.5-f.center.x,y=p.getY()+.5-f.center.y,z=p.getZ()+.5-f.center.z;
            if(x*x+y*y+z*z<=f.radius*f.radius)return true;}
        return false;
    }
    public static boolean frozen(Entity e){
        if(e.level().isClientSide)return com.thelongtravail.network.TimeStopPacket.clientFrozen.test(e);
        if(!(e.level() instanceof ServerLevel s))return false;
        var manager=WORLDS.get(s);
        if(manager==null||manager.fields.isEmpty())return false;
        for(Field f:manager.at(e.position()))if(f.contains(e.position())&&!TimeStopExemptions.exempt(e,f.allies))return true;
        return false;
    }
    public Set<Long> coveredChunks(){return coverage;}
    public static boolean anyActive(){for(var m:WORLDS.values())if(!m.fields.isEmpty())return true;return false;}
    private List<Field> at(Vec3 p){return index.getOrDefault(ChunkPos.asLong((int)Math.floor(p.x/16),(int)Math.floor(p.z/16)),List.of());}
    private Collection<Field> along(Vec3 p,Vec3 delta){
        if(fields.isEmpty())return List.of();Vec3 end=p.add(delta);
        int x0=(int)Math.floor(Math.min(p.x,end.x)/16),x1=(int)Math.floor(Math.max(p.x,end.x)/16),z0=(int)Math.floor(Math.min(p.z,end.z)/16),z1=(int)Math.floor(Math.max(p.z,end.z)/16);
        long width=(long)x1-x0+1,height=(long)z1-z0+1;
        if(width>256||height>256||width*height>256)return fields.values();
        Set<Field> out=Collections.newSetFromMap(new IdentityHashMap<>());
        for(int dx=0;dx<width;dx++)for(int dz=0;dz<height;dz++)out.addAll(index.getOrDefault(ChunkPos.asLong(x0+dx,z0+dz),List.of()));return out;
    }
    public static Vec3 clip(Entity e,Vec3 delta){
        if(e.level().isClientSide)return com.thelongtravail.network.TimeStopPacket.clientClip.apply(e,delta);
        if(!(e.level() instanceof ServerLevel s))return delta;
        var manager=WORLDS.get(s);if(manager==null||manager.fields.isEmpty())return delta;
        double t=1;for(Field f:manager.along(e.position(),delta))if(!TimeStopExemptions.exempt(e,f.allies))t=Math.min(t,TimeStopGeometry.entryInside(e.position(),delta,f.center,f.radius));
        return t<1?delta.scale(t):delta;
    }
    public boolean start(ServerPlayer p,boolean witness){return start(p,witness,level.getGameTime(),p.position(),BoundlessItemsConfig.get("star.radius"),BoundlessItemsConfig.ticks(witness?"star.witnessDuration":"star.maliceDuration"),TimeStopExemptions.snapshot(p));}
    // 所有调用均须遵守数量限制和组锁定。
    public boolean start(ServerPlayer p,boolean witness,long now,Vec3 center,double radius,int duration,Set<UUID> allies){
        // 与客户端协议和原版可用坐标范围一致，必须在修改现有区域之前拒绝非法输入。
        if(p==null||p.serverLevel()!=level||center==null||!Double.isFinite(center.lengthSqr())
                ||Math.abs(center.x)>30_000_000||Math.abs(center.z)>30_000_000
                ||!Double.isFinite(radius)||radius<=0||radius>com.thelongtravail.network.TimeStopPacket.MAX_RADIUS||duration<=0
                ||allies==null||allies.size()>com.thelongtravail.network.TimeStopPacket.MAX_ALLIES)return false;
        for(UUID ally:allies)if(ally==null)return false;
        expire(now);if(locked(p.getUUID()))return false;
        Set<Long> covered=TimeStopGeometry.chunks(center,radius);
        if(covered.stream().anyMatch(c->index.getOrDefault(c,List.of()).size()>=BoundlessItemsConfig.PER_CHUNK.get()))return false;
        Set<Group> joining=Collections.newSetFromMap(new IdentityHashMap<>());
        for(Field f:fields.values())if(TimeStopGeometry.overlaps(center,radius,f.center,f.radius))joining.add(f.group);
        Group merged=new Group(deadline(now,BoundlessItemsConfig.ticks("star.groupLimit")));
        for(Group g:joining){merged.deadline=Math.min(merged.deadline,g.deadline);merged.members.putAll(g.members);}
        if(merged.deadline<=now)return false;
        for(Field f:fields.values())if(joining.contains(f.group)){f.group=merged;f.end=Math.min(f.end,merged.deadline);}
        groups.removeAll(joining);groups.add(merged);
        int cooldown=witness?BoundlessItemsConfig.ticks("star.witnessCooldown"):0;
        merged.members.put(p.getUUID(),cooldown);
        // 提前写入离线可保存的冷却；组锁期间不会递减。
        if(cooldown>0)p.getPersistentData().putInt(StarVoiceState.COOLDOWN,Math.max(cooldown,p.getPersistentData().getInt(StarVoiceState.COOLDOWN)));
        Field f=new Field(nextId++,now,Math.min(deadline(now,duration),merged.deadline),center,radius,allies,merged);fields.put(f.id,f);
        rebuild();dirty=true;return true;
    }
    private static long deadline(long now,int duration){return now>Long.MAX_VALUE-duration?Long.MAX_VALUE:now+duration;}
    private void rebuild(){revision++;index.clear();for(Field f:fields.values())for(long c:f.chunks)index.computeIfAbsent(c,k->new ArrayList<>()).add(f);coverage=Set.copyOf(index.keySet());}
    public void expire(long now){
        if(fields.isEmpty()&&groups.isEmpty())return;
        boolean removed=fields.values().removeIf(f->f.end<=now);if(removed){rebuild();dirty=true;}
        Set<Group> live=Collections.newSetFromMap(new IdentityHashMap<>());fields.values().forEach(f->live.add(f.group));
        for(Iterator<Group> i=groups.iterator();i.hasNext();){Group g=i.next();if(live.contains(g))continue;
            g.members.forEach((uuid,cooldown)->{if(cooldown<=0)return;ServerPlayer p=level.getServer().getPlayerList().getPlayer(uuid);
                if(p!=null)p.getPersistentData().putInt(StarVoiceState.COOLDOWN,Math.max(cooldown,p.getPersistentData().getInt(StarVoiceState.COOLDOWN)));
                else OFFLINE_COOLDOWNS.merge(uuid,cooldown,Math::max);});i.remove();}
    }
    public static void login(ServerPlayer p){Integer c=OFFLINE_COOLDOWNS.remove(p.getUUID());if(c!=null)p.getPersistentData().putInt(StarVoiceState.COOLDOWN,Math.max(c,p.getPersistentData().getInt(StarVoiceState.COOLDOWN)));}
    public void tick(boolean broadcast){expire(level.getGameTime());FrozenBlockTasks.get(level).tick(level);if(broadcast&&(dirty||level.getGameTime()%10==0)){com.thelongtravail.network.TimeStopPacket.send(level);dirty=false;}}
    public static void stopping(net.minecraft.server.MinecraftServer server){
        for(var l:server.getAllLevels()){TimeStopManager m=get(l);m.fields.clear();m.rebuild();m.expire(Long.MAX_VALUE);FrozenBlockTasks.get(l).tick(l);}
    }
    public static void clear(){WORLDS.clear();OFFLINE_COOLDOWNS.clear();com.thelongtravail.network.TimeStopSync.clear();}
}
