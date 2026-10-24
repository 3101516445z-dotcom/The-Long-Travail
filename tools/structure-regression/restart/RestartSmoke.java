package travail.restart;

import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.electronwill.nightconfig.core.CommentedConfig;
import com.thelongtravail.compat.maid.*;
import com.thelongtravail.config.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.server.*;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ConfigTracker;
import net.minecraftforge.registries.ForgeRegistries;
import java.nio.file.*;
import java.util.*;
import java.util.function.Predicate;

@Mod("travail_smoke")
public final class RestartSmoke {
    private final boolean prepare="before".equals(System.getProperty("travail.restart.stage"));
    private final Properties data=new Properties();
    private final List<String> checks=new ArrayList<>();
    private MinecraftServer server;
    private UUID entityId, identity;
    private int due;
    private boolean done;
    public RestartSmoke() {
        MinecraftForge.EVENT_BUS.addListener(this::start);
        MinecraftForge.EVENT_BUS.addListener(this::tick);
        MinecraftForge.EVENT_BUS.addListener(this::stopping);
    }
    private void check(boolean yes,String label){if(!yes)throw new AssertionError(label);checks.add(label);}
    private void update(String file,Predicate<CommentedConfig> edit)throws Exception {
        var config=ConfigTracker.INSTANCE.fileMap().get("the_long_travail/"+file);
        var update=config.getClass().getDeclaredMethod("update",Predicate.class);update.setAccessible(true);
        check((boolean)update.invoke(config,edit),"configuration committed: "+file);
    }
    private void start(ServerStartedEvent event){
        server=event.getServer();
        try {
            if(prepare){
                update("general.toml",c->{c.set("requirements.biomeDrawCount",7);return true;});
                update("aspects/abyss.toml",c->{
                    c.set("items.perfect_fluid.itemBlacklist",List.of("minecraft:stone","minecraft:dirt","minecraft:stone"));
                    c.set("items.perfect_fluid.compatInitialization.maidSoulAdded",true);return true;});
                var level=server.overworld();
                var maid=(EntityMaid)ForgeRegistries.ENTITY_TYPES.getValue(new ResourceLocation("touhou_little_maid:maid")).create(level);
                maid.setTame(true);maid.setOwnerUUID(UUID.randomUUID());maid.setNoAi(true);maid.setNoGravity(true);maid.setInvulnerable(true);
                var spawn=level.getSharedSpawnPos();maid.setPos(spawn.getX()+2,120,spawn.getZ()+2);
                level.getChunkAt(maid.blockPosition());level.setChunkForced(maid.chunkPosition().x,maid.chunkPosition().z,true);
                check(level.addFreshEntity(maid),"real maid added to saved world");
                entityId=maid.getUUID();identity=MaidAccess.identity(maid);
                MaidAccessoryRuntime.starCooldown(maid,2000);
                MaidAccessoryRuntime.quixoteCooldown(maid,1700);
                var orphan=UUID.randomUUID();var ledger=MaidAccessoryLedger.get(server);
                ledger.entry(orphan).putInt("StarCooldown",3456);((net.minecraft.world.level.saveddata.SavedData)ledger).setDirty();
                data.setProperty("orphan",orphan.toString());
                data.setProperty("entity",entityId.toString());data.setProperty("identity",identity.toString());
                data.setProperty("owner",maid.getOwnerUUID().toString());
                data.setProperty("pid",Long.toString(ProcessHandle.current().pid()));
                due=server.getTickCount()+20;
            } else {
                try(var in=Files.newInputStream(Path.of("restart.properties"))){data.load(in);}
                check(!data.getProperty("pid").equals(Long.toString(ProcessHandle.current().pid())),"different JVM process");
                entityId=UUID.fromString(data.getProperty("entity"));identity=UUID.fromString(data.getProperty("identity"));
                check(TravailConfig.BIOME_DRAW_COUNT.get()==7,"general config survives restart");
                check(AbyssItemsConfig.BLACKLIST.get().equals(List.of("minecraft:stone","minecraft:dirt","minecraft:stone")),"list order duplicates and intentional removal survive startup");
                check(AbyssItemsConfig.MAID_SOUL_ADDED.get(),"initialization marker survives restart without readding defaults");
                var ledger=MaidAccessoryLedger.get(server);var entry=ledger.find(identity);
                check(entry!=null,"maid identity resolves persistent ledger");
                check(entry.getInt("StarCooldown")==Integer.parseInt(data.getProperty("star")),"star cooldown exact at startup");
                check(entry.getInt("QuixoteCooldown")==Integer.parseInt(data.getProperty("quixote")),"quixote cooldown exact at startup");
                check(ledger.find(UUID.fromString(data.getProperty("orphan"))).getInt("StarCooldown")==3456,"offline identity ledger survives restart");
                due=server.getTickCount()+20;
            }
        }catch(Throwable error){finish(error);}
    }
    private void tick(TickEvent.ServerTickEvent event){
        if(done||server==null||event.phase!=TickEvent.Phase.END||server.getTickCount()<due)return;
        try {
            var entity=server.overworld().getEntity(entityId);
            check(entity instanceof EntityMaid,"real maid loaded from world");
            var maid=(EntityMaid)entity;
            check(MaidAccess.identity(maid).equals(identity),"maid identity stable");
            check(maid.getOwnerUUID().toString().equals(data.getProperty("owner")),"maid owner stable");
            if(prepare){
                var type=Class.forName("com.thelongtravail.compat.maid.MaidRuntimeState");
                var field=type.getDeclaredField("STATES");field.setAccessible(true);
                Object state=((Map<?,?>)field.get(null)).get(maid);
                var armed=state.getClass().getDeclaredField("armed");armed.setAccessible(true);
                armed.setBoolean(state,true);
                check(armed.getBoolean(state),"transient armed state deliberately seeded before shutdown");
            }
            if(!prepare){
                var entry=MaidAccessoryLedger.get(server).find(identity);
                int expected=Integer.parseInt(data.getProperty("star"));
                check(entry.getInt("StarCooldown")<expected&&entry.getInt("StarCooldown")>=expected-25,"cooldown resumes with ticks, no offline catch-up");
                var type=Class.forName("com.thelongtravail.compat.maid.MaidRuntimeState");
                var field=type.getDeclaredField("STATES");field.setAccessible(true);
                Object state=((Map<?,?>)field.get(null)).get(maid);
                check(state!=null,"transient state reconstructed");
                var armed=state.getClass().getDeclaredField("armed");armed.setAccessible(true);
                check(!armed.getBoolean(state),"no stale armed action on restored maid");
            }
            finish(null);
        }catch(Throwable error){finish(error);}
    }
    private void stopping(ServerStoppingEvent event){
        if(!prepare||identity==null)return;
        try{
            var entry=MaidAccessoryLedger.get(server).find(identity);
            data.setProperty("star",Integer.toString(entry.getInt("StarCooldown")));
            data.setProperty("quixote",Integer.toString(entry.getInt("QuixoteCooldown")));
            try(var out=Files.newOutputStream(Path.of("restart.properties"))){data.store(out,"captured after final tick, before save");}
        }catch(Exception failure){throw new RuntimeException(failure);}
    }
    private void finish(Throwable error){
        done=true;if(error!=null)error.printStackTrace();
        try{Files.writeString(Path.of("restart-result.txt"),(error==null?"PASS":"FAIL")+" stage="+(prepare?"prepare":"verify")+" checks="+checks.size()+"\n"+String.join("\n",checks)+(error==null?"":"\n"+error));}
        catch(Exception failure){throw new RuntimeException(failure);}
        server.halt(false);
    }
}
