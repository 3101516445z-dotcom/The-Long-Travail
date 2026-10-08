package travail.smoke;

import com.mojang.authlib.GameProfile;
import com.thelongtravail.TravailAspect;
import com.thelongtravail.data.*;
import com.thelongtravail.registry.ModRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.*;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.ChunkStatus;
import net.minecraft.world.level.levelgen.structure.*;
import net.minecraft.world.level.levelgen.structure.pieces.PiecesContainer;
import net.minecraft.world.level.levelgen.structure.structures.DesertPyramidPiece;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.type.capability.ICurio;
import top.theillusivec4.curios.common.inventory.CurioStacksHandler;
import java.util.*;

@Mod("travail_smoke")
public final class SelectedSmoke {
    private static final ResourceLocation TARGET = new ResourceLocation("minecraft:desert_pyramid");
    private static void check(boolean value,String message){if(!value)throw new AssertionError(message);}
    private static final class Player extends ServerPlayer {
        Player(ServerLevel level) {
            super(level.getServer(),level,new GameProfile(UUID.randomUUID(),"AsyncVisitTest"));
            var wire=new net.minecraft.network.Connection(net.minecraft.network.protocol.PacketFlow.SERVERBOUND){
                @Override public void send(net.minecraft.network.protocol.Packet<?> packet){}
            };
            connection=new net.minecraft.server.network.ServerGamePacketListenerImpl(level.getServer(),wire,this){
                @Override public void send(net.minecraft.network.protocol.Packet<?> packet){}
            };
        }
    }
    private Player player;
    private ItemStack diary;
    private ServerLevel level;
    private Structure structure;
    private int ticks;
    private boolean running;
    private static Object field(Object object,String name)throws Exception{
        var field=object instanceof Class<?> c?c.getDeclaredField(name):object.getClass().getDeclaredField(name);
        field.setAccessible(true);return field.get(object instanceof Class<?>?null:object);
    }
    private static Collection<?> pending()throws Exception{return (Collection<?>)field(JourneyVisits.class,"PENDING");}
    public SelectedSmoke(){MinecraftForge.EVENT_BUS.register(this);}
    private static ItemStack equip(Player player) {
        var inventory=CuriosApi.getCuriosInventory(player).resolve().orElseThrow();
        var slots=new CurioStacksHandler(inventory,"travel_diary",1,true,false,true,ICurio.DropRule.DEFAULT);
        inventory.setCurios(new HashMap<>(Map.of("travel_diary",slots)));
        var diary=new ItemStack(ModRegistry.LONG_TRAVAIL.get());
        check(LongTravailData.tryInitialize(diary,player),"initialize diary");
        slots.getStacks().setStackInSlot(0,diary);
        var requirements=diary.getTag().getCompound("LongTravail").getCompound("Requirements");
        for(var aspect:TravailAspect.values()){
            requirements.getCompound(aspect.id()).put("Biomes",new ListTag());
            requirements.getCompound(aspect.id()).put("Structures",new ListTag());
        }
        var ids=new ListTag();ids.add(StringTag.valueOf(TARGET.toString()));
        requirements.getCompound(TravailAspect.FAR_REACH.id()).put("Structures",ids);
        LongTravailData.requirementsChanged(diary);
        return diary;
    }
    private void observe(){JourneyVisits.observe(player,diary,Map.of(structure,TARGET));}
    @SubscribeEvent(priority=net.minecraftforge.eventbus.api.EventPriority.LOWEST) public void started(ServerStartedEvent event) {
        try {
            level=event.getServer().overworld();
            structure=level.registryAccess().registryOrThrow(Registries.STRUCTURE).get(TARGET);
            player=new Player(level);player.setPos(800.5,75,805.5);diary=equip(player);
            var origin=level.getChunk(50,50);
            var references=new HashMap<>(origin.getAllReferences());
            references.put(structure,new it.unimi.dsi.fastutil.longs.LongOpenHashSet(new long[]{ChunkPos.asLong(57,57)}));
            origin.setAllReferences(references);
            // 起点只有 STRUCTURE_STARTS，尚未成为可同步读取的 FULL 区块。
            var start=level.getChunk(57,57,ChunkStatus.STRUCTURE_STARTS);
            var piece=new DesertPyramidPiece(RandomSource.create(45),0,0);
            var box=piece.getBoundingBox();piece.move(800-box.minX(),70-box.minY(),800-box.minZ());
            start.setStartForStructure(structure,new StructureStart(structure,new ChunkPos(57,57),0,new PiecesContainer(List.of(piece))));
            check(level.getChunkSource().getChunkNow(57,57)==null,"fixture start is not full-loaded");
            observe();observe();check(pending().size()==1,"identical observations deduplicated");
            check(HotPathMetrics.Counter.JOURNEY_REQUESTS.value()==0,"observation never blocks to request chunk");
            // 外部需求版本变化必须失效，不能把旧位置应用于新要求。
            LongTravailData.requirementsChanged(diary);JourneyVisits.tick();
            check(pending().isEmpty(),"external revision invalidates queued observation");
            observe();JourneyVisits.forget(player);check(pending().isEmpty(),"logout cancels queued observation");
            observe();Object observation=pending().iterator().next();
            var created=observation.getClass().getDeclaredField("created");created.setAccessible(true);created.setLong(observation,System.nanoTime()-31_000_000_000L);
            JourneyVisits.tick();check(pending().isEmpty(),"expired observation removed");
            observe();JourneyVisits.tick();
            Object cancelled=field(JourneyVisits.class,"inFlight");
            check(cancelled!=null,"budget starts an async request");
            JourneyVisits.invalidate();
            check((Boolean)field(cancelled,"released"),"reload releases ticket");
            check(field(JourneyVisits.class,"inFlight")==cancelled,"reload preserves occupied in-flight slot");
            check(!((java.util.concurrent.CompletableFuture<?>)field(cancelled,"future")).isCancelled(),"shared future is never cancelled");
            JourneyVisits.clear();HotPathMetrics.reset();
            observe();
            player.setPos(820.5,75,805.5); // 已离开真实部件，仍应补记观察时位置。
            check(LongTravailData.remainingStructures(diary).contains(TARGET),"progress not granted before confirmation");
            running=true;
        } catch(Throwable failure){failure.printStackTrace();finish(event.getServer(),false);}
    }
    @SubscribeEvent public void tick(TickEvent.ServerTickEvent event) {
        if(!running||event.phase!=TickEvent.Phase.END)return;
        try {
            if(++ticks>200)throw new AssertionError("async confirmation timeout");
            if(!pending().isEmpty())return;
            check(!LongTravailData.remainingStructures(diary).contains(TARGET),"arrival is confirmed using captured position after player leaves; requests="+HotPathMetrics.Counter.JOURNEY_REQUESTS.value()+", expired="+HotPathMetrics.Counter.JOURNEY_EXPIRED.value()+", confirmed="+HotPathMetrics.Counter.JOURNEY_CONFIRMED.value());
            check(HotPathMetrics.Counter.JOURNEY_REQUESTS.value()==1,"single budgeted request");
            check(HotPathMetrics.Counter.JOURNEY_CONFIRMED.value()==1,"exactly one confirmed progression");
            check(field(JourneyVisits.class,"inFlight")==null,"completed request released");
            System.out.println("JOURNEY_ASYNC_PASS: real server ticks, captured position, revision invalidation, expiry, logout, deduplication, one request");
            finish(event.getServer(),true);
        } catch(Throwable failure){failure.printStackTrace();finish(event.getServer(),false);}
    }
    private void finish(net.minecraft.server.MinecraftServer server,boolean pass){
        running=false;JourneyVisits.clear();
        try{java.nio.file.Files.writeString(java.nio.file.Path.of("selected-result.txt"),pass?"PASS":"FAIL");}
        catch(Exception failure){throw new RuntimeException(failure);}
        server.halt(false);
    }
}
