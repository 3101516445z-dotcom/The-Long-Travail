package travail.smoke;

import com.mojang.authlib.GameProfile;
import com.thelongtravail.boundless.*;
import com.thelongtravail.client.LanternLighting;
import com.thelongtravail.client.LanternLightSnapshot;
import com.thelongtravail.data.HotPathMetrics;
import com.thelongtravail.data.HotPathMetrics.Counter;
import com.thelongtravail.network.*;
import com.thelongtravail.underworld.*;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.*;
import net.minecraft.server.level.*;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.ticks.*;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.fml.common.Mod;
import java.util.*;

@Mod("travail_smoke")
public final class HotPathSmoke {
    private static void check(boolean ok,String message){if(!ok)throw new AssertionError(message);}
    public HotPathSmoke(){MinecraftForge.EVENT_BUS.addListener(this::run);}
    private static final class TestPlayer extends ServerPlayer {
        TestPlayer(ServerLevel level){
            super(level.getServer(),level,new GameProfile(UUID.randomUUID(),"HotPathTest"));
            var wire=new net.minecraft.network.Connection(net.minecraft.network.protocol.PacketFlow.SERVERBOUND){
                @Override public void send(net.minecraft.network.protocol.Packet<?> packet){}
            };
            connection=new net.minecraft.server.network.ServerGamePacketListenerImpl(level.getServer(),wire,this){
                @Override public void send(net.minecraft.network.protocol.Packet<?> packet){}
            };
        }
    }
    private void run(ServerStartedEvent event){
        boolean pass=false;
        try{
            FrozenQueueChecks.run();lanternFootprints();
            nbt();lighting();boundaries(event.getServer().overworld());skyBoundary(event.getServer().getLevel(net.minecraft.world.level.Level.END));timersAndTasks(event.getServer().overworld());overlapRelease(event.getServer().overworld());maintenance(event.getServer().overworld());
            JourneyQuerySmoke.run(event.getServer().overworld());
            System.out.println("HOTPATH_SERVER_PASS");pass=true;
        }catch(Throwable error){error.printStackTrace();}
        finally{
            TimeStopManager.stopping(event.getServer());
            try{java.nio.file.Files.writeString(java.nio.file.Path.of("hotpath-result.txt"),pass?"PASS":"FAIL");}
            catch(Exception error){throw new RuntimeException(error);}
            event.getServer().halt(false);
        }
    }
    private static CompoundTag book(UUID id){
        CompoundTag tag=new CompoundTag(),data=new CompoundTag();
        data.putUUID(UnderworldLedger.BOOK_ID,id);tag.putString("id","the_long_travail:book_of_the_dead");
        tag.putByte("Count",(byte)1);tag.put("tag",data);return tag;
    }
    private static int finish(NbtReconcileCursor cursor,UnderworldLedger ledger){
        int steps=0;while(cursor.step(ledger))check(++steps<1_000_000,"bounded fixture completion");return steps;
    }
    private static void nbt(){
        var ledger=new UnderworldLedger();UUID id=UUID.randomUUID();ledger.consume(id);
        CompoundTag root=new CompoundTag();ListTag many=new ListTag();
        for(int i=0;i<10000;i++)many.add(book(id));root.put("items",many);
        var cursor=new NbtReconcileCursor(()->root);
        for(int i=0;i<128;i++)check(cursor.step(ledger),"large NBT remains incremental");
        check(((CompoundTag)many.get(9999)).getByte("Count")==1,"budget does not traverse whole NBT");
        int steps=finish(cursor,ledger);
        for(Tag entry:many)check(((CompoundTag)entry).getByte("Count")==0,"all nested spent identities eventually cleared");
        CompoundTag original=book(id),replacement=book(id),holder=new CompoundTag();holder.put("slot",original);
        cursor=new NbtReconcileCursor(()->holder);cursor.step(ledger);cursor.step(ledger);
        holder.put("slot",replacement);finish(cursor,ledger);
        check(original.getByte("Count")==1&&replacement.getByte("Count")==0,"detached child is not mutated");
        CompoundTag changed=new CompoundTag();changed.putInt("first",1);cursor=new NbtReconcileCursor(()->changed);
        cursor.step(ledger);changed.put("later",book(id));finish(cursor,ledger);
        check(changed.getCompound("later").getByte("Count")==0,"in-place compound mutation restarts safely");
        CompoundTag[] mutableRoot={new CompoundTag()};cursor=new NbtReconcileCursor(()->mutableRoot[0]);cursor.step(ledger);
        mutableRoot[0]=book(id);finish(cursor,ledger);check(mutableRoot[0].getByte("Count")==0,"root replacement revalidated");
        UUID journey=UUID.randomUUID();ledger.witness(journey);CompoundTag diary=new CompoundTag(),data=new CompoundTag();
        data.putUUID("JourneyId",journey);diary.put("LongTravail",data);
        ledger.reconcileTag(diary,0);ledger.reconcileTag(diary,0);
        check(data.getLong("RequirementsRevision")==1&&data.getBoolean("UnderworldBookWitness"),"immediate path is idempotent");
        ListTag scalars=new ListTag();for(int i=0;i<10000;i++)scalars.add(IntTag.valueOf(i));
        check(finish(new NbtReconcileCursor(()->scalars),ledger)<4,"scalar list pruned without visiting elements");
        for(int depth:new int[]{64,65}){
            CompoundTag deep=book(id);for(int i=0;i<depth;i++){CompoundTag parent=new CompoundTag();parent.put("nested",deep);deep=parent;}
            CompoundTag top=deep;finish(new NbtReconcileCursor(()->top),ledger);
            for(int i=0;i<depth;i++)deep=deep.getCompound("nested");
            check(deep.getByte("Count")== (depth==64?0:1),"depth cutoff remains inclusive at 64: "+depth);
        }
        System.out.println("NBT_CURSOR_PASS: 10000 nested books, "+steps+" remaining steps; replacement, mutation, idempotence");
    }
    @SuppressWarnings("unchecked") private static void maintenance(ServerLevel level)throws Exception{
        var players=level.getServer().getPlayerList();
        var field=net.minecraft.server.players.PlayerList.class.getDeclaredField("playersByUUID");field.setAccessible(true);
        var indexed=(Map<UUID,ServerPlayer>)field.get(players);
        var ledger=UnderworldLedger.get(level.getServer());UUID bookId=UUID.randomUUID();ledger.consume(bookId);
        var a=new TestPlayer(level);var b=new TestPlayer(level);
        var first=new ListTag();var second=new ListTag();
        for(int n=0;n<2000;n++){first.add(book(bookId));second.add(book(bookId));}
        a.getPersistentData().put("BudgetProbe",first);b.getPersistentData().put("BudgetProbe",second);
        indexed.put(a.getUUID(),a);indexed.put(b.getUUID(),b);
        try{
            UnderworldMaintenance.request(a);UnderworldMaintenance.request(b);
            HotPathMetrics.reset();UnderworldMaintenance.tick(level.getServer());
            check(Counter.NBT_STEPS.value()<=2048,"global NBT budget shared by players");
            check(((CompoundTag)first.get(1999)).getByte("Count")==1&&((CompoundTag)second.get(1999)).getByte("Count")==1,"large scans yield before completion");
            int ticks=0;
            while(((CompoundTag)first.get(1999)).getByte("Count")!=0||((CompoundTag)second.get(1999)).getByte("Count")!=0){
                check(ticks++<2000,"both players eventually progress");
                if(ticks%20==0){UnderworldMaintenance.request(a);UnderworldMaintenance.request(b);}
                HotPathMetrics.reset();UnderworldMaintenance.tick(level.getServer());
                check(Counter.NBT_STEPS.value()<=2048,"budget holds through resumptions");
            }
            UnderworldMaintenance.forget(a.getUUID());UnderworldMaintenance.forget(b.getUUID());
            HotPathMetrics.reset();UnderworldMaintenance.tick(level.getServer());
            check(Counter.NBT_STEPS.value()==0,"logout clears queued closures");
            // 一个持续更换 NBT 的前置槽位不能饿死同一玩家的后续根节点。
            var busy = new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.STONE);
            a.getInventory().setItem(0, busy);
            CompoundTag probe = book(bookId);
            a.getPersistentData().put("StarvationProbe", probe);
            UnderworldMaintenance.request(a);
            for (int tick = 0; tick < 200 && probe.getByte("Count") != 0; tick++) {
                CompoundTag replacement = new CompoundTag();
                ListTag payload = new ListTag();
                for (int i = 0; i < 10000; i++) payload.add(new CompoundTag());
                replacement.put("payload", payload);busy.setTag(replacement);
                HotPathMetrics.reset();UnderworldMaintenance.tick(level.getServer());
                check(Counter.NBT_STEPS.value() <= 2048, "mutation retains shared budget");
            }
            check(probe.getByte("Count") == 0, "changing first slot must not starve later roots");
            System.out.println("NBT_MAINTENANCE_PASS: shared 2048-step budget, two-player fairness, resumption, cleanup, changing-root fairness");
        }finally{indexed.remove(a.getUUID());indexed.remove(b.getUUID());UnderworldMaintenance.clear();}
    }
    private static void skyBoundary(ServerLevel level){
        level.getChunk(0,0);var player=new TestPlayer(level);
        for(int y=level.getMinBuildHeight();y<level.getMaxBuildHeight();y++)level.setBlock(new BlockPos(0,y,0),Blocks.AIR.defaultBlockState(),2);
        player.setPos(.5,-100_000_000,.5);
        check(com.thelongtravail.farreach.IcarusEnvironment.openSky(player),"far below build height with clear column remains open");
        BlockPos bottom=new BlockPos(0,level.getMinBuildHeight(),0);
        level.setBlock(bottom,Blocks.STONE.defaultBlockState(),2);
        check(!com.thelongtravail.farreach.IcarusEnvironment.openSky(player),"first buildable block still obstructs below-world observer");
        level.setBlock(bottom,Blocks.GLASS.defaultBlockState(),2);
        check(com.thelongtravail.farreach.IcarusEnvironment.openSky(player),"glass at bottom remains transparent");
        level.setBlock(bottom,Blocks.AIR.defaultBlockState(),2);
        System.out.println("ICARUS_HEIGHT_BOUNDARY_PASS: extreme negative Y, first buildable obstruction, glass transparency");
    }
    private static final class OwnedArrow extends Arrow {
        net.minecraft.world.entity.Entity owner;
        OwnedArrow(ServerLevel level,net.minecraft.world.entity.Entity owner){super(EntityType.ARROW,level);this.owner=owner;}
        @Override public net.minecraft.world.entity.Entity getOwner(){return owner;}
    }
    private static void boundaries(ServerLevel level) throws Exception {
        var manager=TimeStopManager.get(level);manager.expire(Long.MAX_VALUE);
        var caster=new TestPlayer(level);caster.setPos(128,80,128);
        long now=level.getGameTime();
        for(double radius:new double[]{0,-1,Double.NaN,Double.POSITIVE_INFINITY,129})
            check(!manager.start(caster,false,now,caster.position(),radius,20,Set.of(caster.getUUID())),
                    "invalid radius rejected before creating field: "+radius);
        check(!manager.start(caster,false,now,new Vec3(Double.NaN,80,0),16,20,Set.of(caster.getUUID())),"nonfinite center rejected");
        check(!manager.start(caster,false,now,new Vec3(1e100,80,0),16,20,Set.of(caster.getUUID())),"unrepresentable center rejected before chunk enumeration");
        var invalidAllies=new HashSet<UUID>();invalidAllies.add(null);
        check(!manager.start(caster,false,now,caster.position(),16,20,invalidAllies),"null identity rejected before group mutation");
        var tooMany=new HashSet<UUID>();for(int i=0;i<4097;i++)tooMany.add(new UUID(0,i));
        check(!manager.start(caster,false,now,caster.position(),16,20,tooMany),"oversized ally snapshot cannot reach decoder");
        var otherLevel=level.getServer().getLevel(net.minecraft.world.level.Level.END);
        check(!manager.start(new TestPlayer(otherLevel),false,now,caster.position(),16,20,Set.of()),"cross-world caster rejected");
        check(TimeStopManager.fields(level).isEmpty()&&!TimeStopManager.locked(caster.getUUID()),"invalid input leaves manager and group locks unchanged");
        check(manager.start(caster,false,Long.MAX_VALUE-10,caster.position(),16,20,Set.of(caster.getUUID())),"near-overflow clock creates finite remaining field");
        check(TimeStopManager.fields(level).iterator().next().end==Long.MAX_VALUE,"field deadline saturates without overflow");
        manager.expire(Long.MAX_VALUE);
        var coverage=manager.coveredChunks();check(coverage==manager.coveredChunks(),"unchanged coverage snapshot reused");
        check(manager.start(caster,false,now,caster.position(),16,20,Set.of(caster.getUUID())),"field for saturated-coordinate broad phase");
        var along=TimeStopManager.class.getDeclaredMethod("along",Vec3.class,Vec3.class);along.setAccessible(true);
        check(((Collection<?>)along.invoke(manager,new Vec3(1e100,80,1e100),new Vec3(1,0,1))).isEmpty(),"saturated chunk index loop terminates");
        check(((Collection<?>)along.invoke(manager,new Vec3(-1e100,80,0),new Vec3(2e100,0,0))).size()==1,"extreme segment uses bounded full-field fallback");
        manager.expire(Long.MAX_VALUE);
        Vec3 sphere=new Vec3(0,80,0),from=new Vec3(-30_000_000,80,0),motion=new Vec3(60_000_000,0,0);
        check(manager.start(caster,false,now,sphere,1,20,Set.of(caster.getUUID())),"small field for fast crossing");
        var fast=new Cow(EntityType.COW,level);fast.setPos(from.x,from.y,from.z);
        Vec3 stopped=from.add(TimeStopManager.clip(fast,motion));
        check(stopped.distanceToSqr(sphere)<=1&&stopped.x<0,"very fast motion must stop inside entry surface, not pass through sphere: "+stopped);
        Vec3 tangentFrom=new Vec3(-100,81,0),tangentDelta=new Vec3(200,0,0);
        Vec3 tangent=tangentFrom.add(tangentDelta.scale(TimeStopGeometry.entryInside(tangentFrom,tangentDelta,sphere,1)));
        check(tangent.distanceToSqr(sphere)<=1,"tangent inset must not push contact outside sphere");
        Random geometryRandom=new Random(41278);
        for(int i=0;i<10000;i++){
            Vec3 origin=new Vec3(geometryRandom.nextDouble()*200-100,geometryRandom.nextDouble()*200-100,geometryRandom.nextDouble()*200-100);
            Vec3 velocity=new Vec3(geometryRandom.nextDouble()*400-200,geometryRandom.nextDouble()*400-200,geometryRandom.nextDouble()*400-200);
            double radius=1+geometryRandom.nextDouble()*127,a=velocity.lengthSqr(),b=2*origin.dot(velocity),d=origin.lengthSqr()-radius*radius;
            double expected=1,disc=b*b-4*a*d;
            if(d<=0)expected=0;else if(a>=1e-12&&disc>=0){double root=(-b-Math.sqrt(disc))/(2*a);if(root>=0&&root<=1)expected=root;}
            check(Math.abs(TimeStopGeometry.entry(origin,velocity,Vec3.ZERO,radius)-expected)<1e-9,"ordinary geometry remains equivalent");
        }
        manager.expire(Long.MAX_VALUE);
        var cow=new Cow(EntityType.COW,level);cow.invulnerableTime=10;
        check(TimeStopExemptions.owner(cow)==null&&TimeStopExemptions.owner(caster).equals(caster.getUUID()),"terminal ownership fast paths");
        net.minecraft.world.entity.Entity chain=caster;
        for(int i=0;i<15;i++)chain=new OwnedArrow(level,chain);
        check(TimeStopExemptions.owner(chain).equals(caster.getUUID()),"owner at depth 15 retained");
        check(TimeStopExemptions.owner(new OwnedArrow(level,chain))==null,"owner at depth 16 still excluded");
        var cycle=new OwnedArrow(level,null);cycle.owner=cycle;
        check(TimeStopExemptions.owner(cycle)==null,"cyclic ownership terminates");
        var duplicate=new OwnedArrow(level,caster);duplicate.setUUID(caster.getUUID());
        check(TimeStopExemptions.owner(duplicate)==null,"duplicate UUID owner chain still rejected");
        FrozenEntityClock.tick(cow);
        var data=level.getServer().getWorldData().overworldData();
        try {
            data.setGameTime(now+100);
            FrozenEntityClock.tick(cow);
            check(cow.invulnerableTime==9,"world clock adjustment must not count as another server tick");
        } finally {data.setGameTime(now);}
        System.out.println("DEEP_BOUNDARIES_PASS: invalid geometry rejected; clock adjustment does not double decrement");
    }
    private static void lighting(){
        Random random=new Random(63429);
        LanternLighting.Source[] sources=new LanternLighting.Source[128];
        for(int i=0;i<sources.length;i++)sources[i]=new LanternLighting.Source(random.nextDouble()*512-256,random.nextDouble()*128-64,random.nextDouble()*512-256);
        var snapshot=new LanternLightSnapshot(sources);HotPathMetrics.reset();
        for(int i=0;i<20000;i++){
            var source=sources[i%sources.length];
            double x=source.x()+random.nextDouble()*24-12,y=source.y()+random.nextDouble()*24-12,z=source.z()+random.nextDouble()*24-12;
            if(i%3==0)x=Math.floor(x/16)*16;
            int packed=(random.nextInt(16)*16<<16)|random.nextInt(15)*16;
            check(snapshot.merge(x,y,z,packed)==LanternLighting.merge(sources,x,y,z,packed),"indexed light equals exhaustive sample");
        }
        long candidates=Counter.LIGHT_CANDIDATES.value();
        check(candidates<20000L*sources.length/8,"spatial index materially reduces candidates");
        sources[0]=new LanternLighting.Source(1e6,1e6,1e6);
        check(snapshot.sources()[0].x()!=1e6,"snapshot owns its input array");
        System.out.println("LANTERN_INDEX_PASS: 20000 equal samples; candidates="+candidates+" vs 2560000 exhaustive");
    }
    private static void lanternFootprints() throws Exception {
        var type=Class.forName("com.thelongtravail.client.LanternLightingClient$SectionBounds");
        var of=type.getDeclaredMethod("of",LanternLighting.Source.class);of.setAccessible(true);
        var sections=type.getDeclaredMethod("sections");sections.setAccessible(true);
        var random=new Random(8441);
        for(int i=0;i<2000;i++){
            var source=new LanternLighting.Source(random.nextDouble()*4000-2000,random.nextDouble()*1000-500,random.nextDouble()*4000-2000);
            Set<Long> expected=new HashSet<>();double r=LanternLighting.RADIUS+1;
            for(int x=net.minecraft.util.Mth.floor(source.x()-r)>>4;x<=net.minecraft.util.Mth.floor(source.x()+r)>>4;x++)
                for(int y=net.minecraft.util.Mth.floor(source.y()-r)>>4;y<=net.minecraft.util.Mth.floor(source.y()+r)>>4;y++)
                    for(int z=net.minecraft.util.Mth.floor(source.z()-r)>>4;z<=net.minecraft.util.Mth.floor(source.z()+r)>>4;z++)
                        expected.add(net.minecraft.core.SectionPos.asLong(x,y,z));
            check(expected.equals(sections.invoke(of.invoke(null,source))),"cached lantern coverage equals original at "+source);
        }
        System.out.println("LANTERN_FOOTPRINT_PASS: 2000 positive and negative positions match original dirty sections");
    }
    private static void overlapRelease(ServerLevel level) {
        var manager=TimeStopManager.get(level);manager.expire(Long.MAX_VALUE);
        var tasks=FrozenBlockTasks.get(level);tasks.tick(level);
        level.getChunk(20,20);level.getChunk(30,30);
        var a=new TestPlayer(level);a.setPos(320,80,320);
        var b=new TestPlayer(level);b.setPos(328,80,320);
        var distant=new TestPlayer(level);distant.setPos(480,80,480);
        long now=level.getGameTime();
        check(manager.start(a,false,now,a.position(),16,10,Set.of(a.getUUID())),"first overlapping field");
        check(manager.start(b,false,now,b.position(),16,30,Set.of(b.getUUID())),"second overlapping field");
        check(manager.start(distant,false,now,distant.position(),16,100,Set.of(distant.getUUID())),"unrelated field");
        var shared=new BlockPos(326,80,320);var remote=new BlockPos(480,80,480);
        check(tasks.hold(level,new ScheduledTick<>(Blocks.STONE_BUTTON,shared,now+17,TickPriority.HIGH,987),false),"shared task held");
        check(tasks.hold(level,new ScheduledTick<>(Blocks.STONE_BUTTON,remote,now+17,TickPriority.HIGH,988),false),"remote task held");
        tasks.tick(level);HotPathMetrics.reset();
        manager.expire(now+10);tasks.tick(level);
        check(tasks.held(shared,Blocks.STONE_BUTTON,false),"remaining overlapping field prevents premature release");
        check(Counter.RELEASE_CANDIDATES.value()==1,"only changed coverage task inspected");
        HotPathMetrics.reset();manager.expire(now+30);tasks.tick(level);
        check(!tasks.held(shared,Blocks.STONE_BUTTON,false)&&tasks.held(remote,Blocks.STONE_BUTTON,false),"shared task released while remote task stays frozen");
        check(Counter.RELEASE_CANDIDATES.value()==1,"remote queue not scanned on nearby expiry");
        manager.expire(Long.MAX_VALUE);tasks.tick(level);
        System.out.println("INDEXED_OVERLAP_RELEASE_PASS: overlap expiry, unrelated queue skipped, exact release point");
    }
    @SuppressWarnings("unchecked") private static void timersAndTasks(ServerLevel level)throws Exception{
        level.getChunk(8,8);level.getChunk(9,8);
        TimeStopManager.get(level).expire(Long.MAX_VALUE);
        FrozenBlockTasks.get(level).tick(level);
        level.getBlockTicks().clearArea(new net.minecraft.world.level.levelgen.structure.BoundingBox(128,0,128,159,255,143));
        var caster=new TestPlayer(level);caster.setPos(128,80,128);level.addNewPlayer(caster);
        var other=new TestPlayer(level);other.setPos(150,80,128);
        var cow=new Cow(EntityType.COW,level);cow.setPos(129,80,128);level.addFreshEntity(cow);
        cow.invulnerableTime=10;FrozenEntityClock.normal(cow);FrozenEntityClock.tick(cow);
        check(cow.invulnerableTime==10,"normal-to-frozen in same tick does not double decrement");
        var fresh=new Cow(EntityType.COW,level);fresh.invulnerableTime=10;FrozenEntityClock.tick(fresh);FrozenEntityClock.tick(fresh);
        check(fresh.invulnerableTime==9,"two frozen entry points decrement only once");
        var manager=TimeStopManager.get(level);long now=level.getGameTime();
        var oldCoverage=manager.coveredChunks();
        check(manager.start(caster,false,now,caster.position(),16,100,Set.of(caster.getUUID())),"first field");
        check(manager.start(other,false,now,other.position(),16,100,Set.of(caster.getUUID(),other.getUUID())),"second field");
        check(oldCoverage.isEmpty()&&!manager.coveredChunks().isEmpty(),"coverage snapshot invalidates without mutating previous view");
        check(manager.coveredChunks()==manager.coveredChunks(),"active coverage snapshot reused");
        var arrow=new Arrow(EntityType.ARROW,level);arrow.setOwner(caster);arrow.setPos(110,80,128);level.addFreshEntity(arrow);
        ProjectileIndex.clear();HotPathMetrics.reset();TimeStopSync.send(level);
        long entities=0;for(var entity:level.getAllEntities())entities++;
        check(Counter.SYNC_ENTITY_VISITS.value()==entities,"one entity traversal for multiple fields");
        check(Counter.OWNER_LOOKUPS.value()<=entities,"owners evaluated once per entity per batch");
        check(Counter.SYNC_PACKETS.value()==1,"initial viewer snapshot");
        var last=TimeStopSync.class.getDeclaredField("LAST");last.setAccessible(true);
        var packet=((Map<ServerPlayer,TimeStopPacket>)last.get(null)).get(caster);
        check(packet.fields().size()==2&&packet.fields().stream().allMatch(f->f.allies().contains(arrow.getUUID())),"fast projectile exemption in both views");
        TimeStopSync.send(level);check(Counter.SYNC_PACKETS.value()==1,"unchanged snapshots suppressed");
        check(Counter.SYNC_ENTITY_VISITS.value()==entities,"second sync reuses projectile index");
        var laterArrow=new Arrow(EntityType.ARROW,level);laterArrow.setOwner(caster);laterArrow.setPos(0,80,0);level.addFreshEntity(laterArrow);
        TimeStopSync.send(level);
        check(((Map<ServerPlayer,TimeStopPacket>)last.get(null)).get(caster).fields().stream().allMatch(f->f.allies().contains(laterArrow.getUUID())),"remote projectile join indexed without world scan");
        check(Counter.SYNC_ENTITY_VISITS.value()==entities,"join does not force dimension scan");
        laterArrow.setOwner(cow);TimeStopSync.send(level);
        check(((Map<ServerPlayer,TimeStopPacket>)last.get(null)).get(caster).fields().stream().noneMatch(f->f.allies().contains(laterArrow.getUUID())),"owner changes re-evaluated");
        laterArrow.discard();check(!ProjectileIndex.snapshot(level).contains(laterArrow),"removed projectile leaves index");
        var tasks=FrozenBlockTasks.get(level);tasks.tick(level);HotPathMetrics.reset();tasks.tick(level);
        check(Counter.TASK_CONTAINER_SCANS.value()==0,"unchanged containers not rescanned");
        var access=(com.thelongtravail.mixin.TimeStopTicksAccessor<net.minecraft.world.level.block.Block>)(Object)level.getBlockTicks();
        var container=access.travail$containers().get(net.minecraft.world.level.ChunkPos.asLong(8,8));
        var pos=new BlockPos(130,80,128);
        container.schedule(new ScheduledTick<>(Blocks.STONE_BUTTON,pos,now+17,TickPriority.HIGH,432));
        tasks.tick(level);check(tasks.held(pos,Blocks.STONE_BUTTON,false),"direct container scheduling detected");
        check(Counter.TASK_CONTAINER_SCANS.value()==1,"only changed container rescanned");
        manager.expire(Long.MAX_VALUE);tasks.tick(level);
        var restored=container.getAll().filter(t->t.pos().equals(pos)&&t.type()==Blocks.STONE_BUTTON).findFirst().orElseThrow();
        check(restored.triggerTick()==now+17&&restored.priority()==TickPriority.HIGH&&restored.subTickOrder()==432,"remaining delay priority and order preserved: "+restored+" now="+now);
        TimeStopSync.send(level);
        check(((Map<ServerPlayer,TimeStopPacket>)last.get(null)).get(caster).fields().isEmpty(),"last field sends empty snapshot");
        System.out.println("TIMESTOP_HOTPATH_PASS: single entity scan, snapshot deduplication, clocks, unchanged containers, direct insertion, restore order");
    }
}
