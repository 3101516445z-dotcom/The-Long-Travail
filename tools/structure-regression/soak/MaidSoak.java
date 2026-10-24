package travail.soak;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.github.tartaricacid.touhoulittlemaid.item.ItemFilm;
import com.thelongtravail.compat.maid.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
import java.util.*;
import net.minecraft.server.level.ServerPlayer;
import com.mojang.authlib.GameProfile;
import java.nio.file.*;
@Mod("travail_smoke")
public final class MaidSoak {
    private static final class TestPlayer extends ServerPlayer {
        TestPlayer(ServerLevel level) {
            super(level.getServer(), level, new GameProfile(UUID.randomUUID(), "MaidTest"));
            var wire = new net.minecraft.network.Connection(net.minecraft.network.protocol.PacketFlow.SERVERBOUND) {
                @Override public void send(net.minecraft.network.protocol.Packet<?> p) {}
            };
            connection = new net.minecraft.server.network.ServerGamePacketListenerImpl(level.getServer(), wire, this) {
                @Override public void send(net.minecraft.network.protocol.Packet<?> p) {}
            };
        }
        boolean disconnected;
        @Override public boolean hasDisconnected() { return disconnected; }
    }
    private static void ownerOnline(ServerLevel level, TestPlayer p, boolean online) throws Exception {
        var field = net.minecraft.server.players.PlayerList.class.getDeclaredField("playersByUUID"); field.setAccessible(true);
        var map = (Map<UUID, ServerPlayer>)field.get(level.getServer().getPlayerList());
        if (online) map.put(p.getUUID(), p); else map.remove(p.getUUID());
    }
    private TestPlayer owner;
    private UUID remoteEntity, remoteIdentity;
    private EntityMaid remote;
    private ServerLevel level;
    private final List<EntityMaid> maids=new ArrayList<>();
    private final Set<UUID> identities=new HashSet<>();
    private final List<String> samples=new ArrayList<>();
    private int ticks, cycles, checks;
    private boolean done;
    private long started;
    public MaidSoak(){MinecraftForge.EVENT_BUS.addListener(this::start);MinecraftForge.EVENT_BUS.addListener(EventPriority.LOWEST,this::tick);}
    private void check(boolean ok,String message){checks++;if(!ok)throw new AssertionError(message);}
    private Map<?,?> states() throws Exception {var f=Class.forName("com.thelongtravail.compat.maid.MaidRuntimeState").getDeclaredField("STATES");f.setAccessible(true);return (Map<?,?>)f.get(null);}
    private int pending() throws Exception {var f=Class.forName("com.thelongtravail.compat.maid.MaidRuntimeState").getDeclaredField("PENDING");f.setAccessible(true);return ((Collection<?>)f.get(null)).size();}
    private void start(ServerStartedEvent event){
        level=event.getServer().overworld();started=System.nanoTime();
        try {owner=new TestPlayer(level);ownerOnline(level,owner,true);for(int i=0;i<16;i++){
            var m=(EntityMaid)ForgeRegistries.ENTITY_TYPES.getValue(new ResourceLocation("touhou_little_maid:maid")).create(level);
            m.setTame(true);m.setOwnerUUID(owner.getUUID());m.setNoAi(true);m.setNoGravity(true);m.setInvulnerable(true);
            m.setPos(i%4,120,i/4);level.getChunkAt(m.blockPosition());level.setChunkForced(0,0,true);
            check(level.addFreshEntity(m),"spawn");maids.add(m);identities.add(MaidAccess.identity(m));
            MaidAccessoryRuntime.starCooldown(m,10000);
        }
            remote=(EntityMaid)ForgeRegistries.ENTITY_TYPES.getValue(new ResourceLocation("touhou_little_maid:maid")).create(level);
            remote.setTame(true);remote.setOwnerUUID(UUID.randomUUID());remote.setNoAi(true);remote.setNoGravity(true);remote.setInvulnerable(true);remote.setPos(4096,120,0);
            level.getChunkAt(remote.blockPosition());level.setChunkForced(256,0,true);check(level.addFreshEntity(remote),"remote spawn");remoteEntity=remote.getUUID();remoteIdentity=MaidAccess.identity(remote);MaidAccessoryRuntime.starCooldown(remote,10000);
        }catch(Throwable error){finish(error);}
    }
    private void tick(TickEvent.ServerTickEvent event){
        if(done||level==null||event.phase!=TickEvent.Phase.END)return;
        try {
            ticks++;
            check(states().size()<=17,"state cache grows beyond live cohort");
            check(pending()==0,"unexpected queued work");
            if(ticks==100)level.setChunkForced(256,0,false);
            if(ticks==700){check(level.getEntity(remoteEntity)==null,"remote entity actually unloaded pos="+remote.position()+" chunk="+remote.chunkPosition()+" forced="+level.getForcedChunks()+" loaded="+level.getChunkSource().hasChunk(256,0));check(!states().containsKey(remote),"unloaded entity removed from cache");samples.add("actual chunk unload PASS at tick=700");}
            if(ticks==800)level.setChunkForced(256,0,true);
            if(ticks==840){remote=(EntityMaid)level.getEntity(remoteEntity);check(remote!=null,"remote entity actually reloaded");check(remoteIdentity.equals(MaidAccess.identity(remote)),"chunk reload preserves identity");samples.add("actual chunk reload PASS at tick=840");}
            if(ticks%100==50){boolean online=(ticks/100)%2==0;owner.disconnected=!online;ownerOnline(level,owner,online);for(var m:maids)check((MaidAccess.owner(m)!=null)==online,"owner availability follows disconnect/reconnect fixture");}
            if(ticks%200==0){
                for(int i=0;i<maids.size();i++){
                    var old=maids.get(i);var id=MaidAccess.identity(old);var owner=old.getOwnerUUID();
                    int cooldown=MaidAccessoryLedger.get(level.getServer()).find(id).getInt("StarCooldown");
                    var film=ItemFilm.maidToFilm(old);old.discard();
                    check(!states().containsKey(old),"removed entity retained in cache");
                    // Null player preserves saved owner; this is server-side film restoration, not a player interaction simulation.
                    ItemFilm.filmToMaid(film,level,new net.minecraft.core.BlockPos(i%4,120,i/4),null);
                    EntityMaid restored=null;
                    for(var e:level.getAllEntities())if(e instanceof EntityMaid m&&!m.isRemoved()&&id.equals(MaidAccess.identity(m)))restored=m;
                    check(restored!=null,"film restore identity");check(owner.equals(restored.getOwnerUUID()),"owner retained");
                    restored.setNoAi(true);restored.setNoGravity(true);restored.setInvulnerable(true);maids.set(i,restored);
                    check(MaidAccessoryLedger.get(level.getServer()).find(id).getInt("StarCooldown")==cooldown,"film must not reset cooldown");
                }
                cycles++;
                int entries=MaidAccessoryLedger.get(level.getServer()).save(new CompoundTag()).getList("Entries",10).size();
                check(entries==17,"ledger grows across repeated same-identity restores");
                samples.add("tick="+ticks+" cycles="+cycles+" states="+states().size()+" ledger="+entries+" pending="+pending());
                System.out.println("SOAK "+samples.get(samples.size()-1));
            }
            if(ticks==2400){for(var m:maids)m.discard();remote.discard();ownerOnline(level,owner,false);check(states().isEmpty(),"final removal clears states");finish(null);}
        }catch(Throwable error){finish(error);}
    }
    private void finish(Throwable error){done=true;if(error!=null)error.printStackTrace();try{Files.writeString(Path.of("soak-result.txt"),(error==null?"PASS":"FAIL "+error)+" ticks="+ticks+" cycles="+cycles+" checks="+checks+" elapsedSeconds="+(System.nanoTime()-started)/1e9+"\n"+String.join("\n",samples));}catch(Exception failure){throw new RuntimeException(failure);}level.getServer().halt(false);}
}
