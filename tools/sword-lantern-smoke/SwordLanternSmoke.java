package travail.smoke;

import com.mojang.authlib.GameProfile;
import com.thelongtravail.valley.*;
import com.thelongtravail.registry.ModRegistry;
import com.thelongtravail.helper.CombatContext;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.effect.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import top.theillusivec4.curios.api.*;
import top.theillusivec4.curios.api.type.capability.ICurio;
import top.theillusivec4.curios.common.inventory.CurioStacksHandler;
import java.util.*;

@Mod("travail_smoke")
public final class SwordLanternSmoke {
    static void check(boolean b,String name){if(!b)throw new AssertionError(name);}
    static void eq(double a,double b,String name){check(Math.abs(a-b)<.0001,name+": "+a+" != "+b);}
    static final class P extends ServerPlayer {
        P(ServerLevel l){super(l.getServer(),l,new GameProfile(UUID.randomUUID(),"Lamp"+UUID.randomUUID().toString().substring(0,8)));
            var wire=new net.minecraft.network.Connection(net.minecraft.network.protocol.PacketFlow.SERVERBOUND){@Override public void send(net.minecraft.network.protocol.Packet<?> p){}};
            connection=new net.minecraft.server.network.ServerGamePacketListenerImpl(l.getServer(),wire,this){@Override public void send(net.minecraft.network.protocol.Packet<?> p){}};
            setPos(0,100,0);l.addNewPlayer(this);
            try{var spawn=ServerPlayer.class.getDeclaredField("spawnInvulnerableTime");spawn.setAccessible(true);spawn.setInt(this,0);}catch(Exception e){throw new RuntimeException(e);}
            var inv=CuriosApi.getCuriosInventory(this).resolve().orElseThrow();
            var slots=new HashMap<String,top.theillusivec4.curios.api.type.inventory.ICurioStacksHandler>();
            slots.put("belt",new CurioStacksHandler(inv,"belt",2,true,false,true,ICurio.DropRule.DEFAULT));inv.setCurios(slots);wear(this,true);
        }
    }
    static void wear(P p,boolean on){CuriosApi.getCuriosInventory(p).resolve().orElseThrow().getStacksHandler("belt").orElseThrow().getStacks().setStackInSlot(0,on?new ItemStack(ModRegistry.SWORD_AND_LANTERN.get()):ItemStack.EMPTY);}
    static Cow cow(ServerLevel l){Cow e=new Cow(EntityType.COW,l);e.setPos(2,100,0);l.addFreshEntity(e);return e;}
    static long now(ServerLevel l){return l.getGameTime();}
    static void time(ServerLevel l,long t){l.getServer().getWorldData().overworldData().setGameTime(t);}
    static final class Cancel {
        LivingEntity victim;
        @SubscribeEvent public void attack(LivingAttackEvent e){if(e.getEntity()==victim)e.setCanceled(true);}
    }
    private P tickingPlayer;
    private int ticks;
    public SwordLanternSmoke(){MinecraftForge.EVENT_BUS.addListener(this::run);MinecraftForge.EVENT_BUS.register(this);}
    @SubscribeEvent(priority=net.minecraftforge.eventbus.api.EventPriority.LOWEST)
    public void realTick(net.minecraftforge.event.TickEvent.ServerTickEvent e){
        if(e.phase!=net.minecraftforge.event.TickEvent.Phase.END||tickingPlayer==null)return;
        try {
            ticks++;
            if(ticks==12){
                check(tickingPlayer.level().getBlockState(tickingPlayer.blockPosition().above()).isAir(),"visual lighting creates no real source");
                wear(tickingPlayer,false);
            }
            if(ticks==13){
                check(tickingPlayer.level().getBlockState(tickingPlayer.blockPosition().above()).isAir(),"actual server tick unequip cleanup");
                players(e.getServer()).remove(tickingPlayer);tickingPlayer=null;
                System.out.println("SWORD_LANTERN_REAL_TICK_PASS");finish(e.getServer(),true);
            }
        }catch(Throwable t){t.printStackTrace();tickingPlayer=null;finish(e.getServer(),false);}
    }
    @SuppressWarnings("unchecked")
    static List<ServerPlayer> players(net.minecraft.server.MinecraftServer server) {
        try{var f=net.minecraft.server.players.PlayerList.class.getDeclaredField("players");f.setAccessible(true);return (List<ServerPlayer>)f.get(server.getPlayerList());}
        catch(Exception e){throw new RuntimeException(e);}
    }
    static void finish(net.minecraft.server.MinecraftServer server,boolean pass){
        try{java.nio.file.Files.writeString(java.nio.file.Path.of("sword-lantern-result.txt"),pass?"PASS":"FAIL");}catch(Exception e){throw new RuntimeException(e);}server.halt(false);
    }
    void run(ServerStartedEvent event){
        boolean pass=false;ServerLevel l=event.getServer().overworld();
        try{
            P p=new P(l);Cow target=cow(l);
            eq(SwordLanternState.bonus(p,target),.1,"alone baseline");
            p.setHealth(10);eq(SwordLanternState.bonus(p,target),.1,"exact half excluded");
            p.setHealth(9);eq(SwordLanternState.bonus(p,target),.2,"below half");p.setHealth(20);
            Zombie undead=new Zombie(EntityType.ZOMBIE,l);undead.setPos(3,100,0);l.addFreshEntity(undead);
            eq(SwordLanternState.bonus(p,undead),.2,"first undead hit snapshot");
            undead.hurt(l.damageSources().playerAttack(p),1);
            eq(SwordLanternState.bonus(p,target),.2,"undead global bonus");
            undead.discard();SwordLanternState.forget(p);
            float before=target.getHealth();target.hurt(l.damageSources().playerAttack(p),2);eq(before-target.getHealth(),2.2,"existing damage path");
            long started=now(l);
            for(int i=1;i<=20;i++){time(l,started+i*300);target.invulnerableTime=0;target.setHealth(20);target.hurt(l.damageSources().playerAttack(p),1);}
            eq(SwordLanternState.bonus(p,target),.1,"6000 ticks excluded");time(l,started+6001);eq(SwordLanternState.bonus(p,target),.2,"6001 ticks included");
            time(l,started+6400);eq(SwordLanternState.bonus(p,target),.1,"400 tick timeout");
            SwordLanternConfig.DURATION.set(0D);Cancel cancel=new Cancel();cancel.victim=target;MinecraftForge.EVENT_BUS.register(cancel);
            SwordLanternState.forget(p);target.invulnerableTime=0;target.hurt(l.damageSources().playerAttack(p),1);time(l,now(l)+1);
            eq(SwordLanternState.bonus(p,target),.1,"cancelled damage cannot start combat");MinecraftForge.EVENT_BUS.unregister(cancel);
            target.setAbsorptionAmount(10);target.invulnerableTime=0;target.hurt(l.damageSources().playerAttack(p),1);time(l,now(l)+1);
            eq(SwordLanternState.bonus(p,target),.2,"absorption loss starts combat");
            wear(p,false);SwordLanternState.tick(p);wear(p,true);eq(SwordLanternState.bonus(p,target),.1,"unequip resets duration");SwordLanternConfig.DURATION.set(300D);
            List<Zombie> crowd=new ArrayList<>();
            for(int i=0;i<10;i++){Zombie z=new Zombie(EntityType.ZOMBIE,l);z.setPos(4+i,100,0);z.setTarget(p);l.addFreshEntity(z);crowd.add(z);}
            time(l,now(l)+10);eq(SwordLanternState.bonus(p,null),.2,"ten enemies");
            Zombie eleventh=new Zombie(EntityType.ZOMBIE,l);eleventh.setPos(64,100,0);eleventh.setTarget(p);l.addFreshEntity(eleventh);crowd.add(eleventh);
            time(l,now(l)+10);eq(SwordLanternState.bonus(p,null),.3,"eleven and 64 block boundary");
            eleventh.setPos(64.01,100,0);eq(SwordLanternState.bonus(p,null),.2,"outside radius immediately excluded");
            crowd.forEach(Entity::discard);SwordLanternState.forget(p);
            P other=new P(l);other.setPos(3,100,0);
            eq(SwordLanternState.bonus(p,null),0,"neutral player breaks solitude");
            check(other.hurt(l.damageSources().playerAttack(p),1),"PvP damage accepted");eq(SwordLanternState.bonus(p,null),.1,"enemy player permits solitude");
            var team=l.getScoreboard().addPlayerTeam("lamp_allies");team.setAllowFriendlyFire(true);
            l.getScoreboard().addPlayerToTeam(p.getScoreboardName(),team);l.getScoreboard().addPlayerToTeam(other.getScoreboardName(),team);
            eq(SwordLanternState.bonus(p,null),0,"team identity immediately overrides enemy");
            SwordLanternConfig.DURATION.set(0D);SwordLanternState.forget(p);other.invulnerableTime=0;other.hurt(l.damageSources().playerAttack(p),1);time(l,now(l)+1);
            eq(SwordLanternState.bonus(p,null),0,"friendly fire cannot start combat");SwordLanternConfig.DURATION.set(300D);
            for(MobEffect effect:new MobEffect[]{MobEffects.BLINDNESS,MobEffects.DARKNESS,MobEffects.CONFUSION})check(!p.addEffect(new MobEffectInstance(effect,100)),"effect immunity "+effect);
            var inv=CuriosApi.getCuriosInventory(p).resolve().orElseThrow();
            var item=(top.theillusivec4.curios.api.type.capability.ICurioItem)ModRegistry.SWORD_AND_LANTERN.get();
            check(!item.canEquip(new SlotContext("belt",p,1,false,true),new ItemStack(ModRegistry.SWORD_AND_LANTERN.get())),"duplicate equip rejected");
            inv.getStacksHandler("belt").orElseThrow().getStacks().setStackInSlot(1,new ItemStack(ModRegistry.SWORD_AND_LANTERN.get()));
            eq(SwordLanternState.bonus(p,null),0,"forced duplicate no extra effects");
            // 分担分支必须继承同一份增伤，不能再次计算腰带条件。
            l.getScoreboard().removePlayerFromTeam(other.getScoreboardName(),team);SwordLanternState.forget(p);
            var oi=CuriosApi.getCuriosInventory(other).resolve().orElseThrow();
            var os=new HashMap<>(oi.getCurios());os.put("body",new CurioStacksHandler(oi,"body",1,true,false,true,ICurio.DropRule.DEFAULT));oi.setCurios(os);
            var wolf=new net.minecraft.world.entity.animal.Wolf(EntityType.WOLF,l);wolf.setTame(true);wolf.setOwnerUUID(other.getUUID());wolf.setPos(4,100,0);wolf.setHealth(wolf.getMaxHealth());l.addFreshEntity(wolf);
            ItemStack affection=new ItemStack(ModRegistry.AFFECTION.get());affection.getOrCreateTag().putUUID("BoundEntity",wolf.getUUID());affection.getOrCreateTag().putUUID("BoundOwner",other.getUUID());
            oi.getStacksHandler("body").orElseThrow().getStacks().setStackInSlot(0,affection);
            other.setHealth(20);other.invulnerableTime=0;float wh=wolf.getHealth();
            other.hurt(l.damageSources().playerAttack(p),4);
            eq(20-other.getHealth(),2.2,"split player branch gets bonus once");eq(wh-wolf.getHealth(),1.1,"split companion branch gets bonus once");
            wolf.discard();oi.getStacksHandler("body").orElseThrow().getStacks().setStackInSlot(0,ItemStack.EMPTY);
            other.setPos(65,100,0);SwordLanternState.forget(p);eq(SwordLanternState.bonus(p,null),.1,"forced duplicates remain single bonus");other.setPos(3,100,0);
            // 旧版光源只清理，不再维护；包括重建后的旧方块实体和时停区域。
            BlockPos light=p.blockPosition().above();
            l.setBlockAndUpdate(light,LanternLight.BLOCK.get().defaultBlockState());
            var old=l.getBlockEntity(light);var saved=old.saveWithFullMetadata();old.load(saved);old.onLoad();
            LanternLight.tick();check(l.getBlockState(light).isAir(),"legacy light cleaned after load");
            l.setBlockAndUpdate(light,LanternLight.BLOCK.get().defaultBlockState());l.getBlockEntity(light).onLoad();
            l.setBlockAndUpdate(light,Blocks.STONE.defaultBlockState());LanternLight.tick();check(l.getBlockState(light).is(Blocks.STONE),"cleanup preserves replacement");
            l.setBlockAndUpdate(light,LanternLight.BLOCK.get().defaultBlockState());l.getBlockEntity(light).onLoad();
            var manager=com.thelongtravail.boundless.TimeStopManager.get(l);
            check(manager.start(p,true,now(l),p.position(),32,100,Set.of(p.getUUID())),"start time stop");
            check(!l.setBlockAndUpdate(light,Blocks.STONE.defaultBlockState()),"time stop blocks normal writes");
            LanternLight.tick();check(l.getBlockState(light).isAir(),"legacy cleanup during time stop");
            var packet=new com.thelongtravail.network.LanternPacket(l.dimension().location(),p.getUUID(),true);
            var buf=new net.minecraft.network.FriendlyByteBuf(io.netty.buffer.Unpooled.buffer());
            com.thelongtravail.network.LanternPacket.encode(packet,buf);
            check(packet.equals(com.thelongtravail.network.LanternPacket.decode(buf)),"visual light state packet round trip");buf.release();
            for(String id:List.of("abandoned_mineshaft","ancient_city"))check(event.getServer().getLootData().getLootTable(new ResourceLocation("minecraft","chests/"+id)).getPool("the_long_travail:shared_items")!=null,"loot injected "+id);
            check(event.getServer().getLootData().getLootTable(new ResourceLocation("minecraft","chests/ancient_city_ice_box")).getPool("the_long_travail:shared_items")==null,"ice box excluded");
            com.thelongtravail.boundless.TimeStopManager.stopping(event.getServer());
            inv.getStacksHandler("belt").orElseThrow().getStacks().setStackInSlot(1,ItemStack.EMPTY);
            p.setNoGravity(true);p.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);
            tickingPlayer=p;players(event.getServer()).add(p);com.thelongtravail.valley.LanternSync.tick(p);
            System.out.println("SWORD_LANTERN_SMOKE_PASS");pass=true;
        }catch(Throwable t){t.printStackTrace();}
        finally{if(!pass)finish(event.getServer(),false);}
    }
}
