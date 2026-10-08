package com.thelongtravail.underworld;

import com.thelongtravail.TheLongTravail;
import com.thelongtravail.TravailAspect;
import com.thelongtravail.valley.AzraelExecution;
import com.thelongtravail.config.UnderworldItemsConfig;
import com.thelongtravail.data.LongTravailData;
import com.thelongtravail.helper.TravailCurios;
import com.thelongtravail.network.UnderworldPacket;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.*;
import net.minecraft.server.level.*;
import net.minecraft.world.damagesource.*;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import java.util.*;

@Mod.EventBusSubscriber(modid=TheLongTravail.MODID)
public final class BookRevival {
    public static final class Hit implements AutoCloseable {
        final Hit previous; final ServerPlayer player; final DamageSource source; final float input;
        float finalDamage,max; boolean four;
        Hit(ServerPlayer p,DamageSource s,float amount){previous=HIT.get();player=p;source=s;input=amount;HIT.set(this);}
        @Override public void close(){if(previous==null)HIT.remove();else HIT.set(previous);}
    }
    private record Death(ServerPlayer player,DamageSource source,ItemStack diary,UUID book,UUID journey,boolean witness,boolean eligible) {}
    public record Respawn(ServerPlayer old,ServerLevel level,Vec3 position, net.minecraft.resources.ResourceKey<net.minecraft.world.level.Level> spawnDimension, net.minecraft.core.BlockPos spawnPosition, float spawnAngle, boolean spawnForced) {}
    private record Search(UUID death,RevivalPosition search) {}
    private static final ThreadLocal<Hit> HIT=new ThreadLocal<>();
    private static final ThreadLocal<Death> DEATH=new ThreadLocal<>();
    private static final ThreadLocal<Respawn> RESPAWN=new ThreadLocal<>();
    private static final Map<UUID,Search> SEARCHES=new HashMap<>();
    public static Hit hit(ServerPlayer p,DamageSource s,float amount){return new Hit(p,s,amount);}
    public static void damage(ServerPlayer p,float amount) {
        Hit h=HIT.get();if(h==null||h.player!=p)return;
        h.finalDamage=amount;h.max=p.getMaxHealth();
        h.four=p.hasEffect(MobEffects.WITHER)&&p.hasEffect(MobEffects.POISON)&&p.hasEffect(MobEffects.WEAKNESS)&&p.hasEffect(MobEffects.MOVEMENT_SLOWDOWN);
    }
    public static boolean excluded(ServerPlayer p,DamageSource s,float raw) {
        return p.getY()<p.level().getMinBuildHeight()||AzraelExecution.matches(p,s)
                ||s.is(AzraelExecution.TYPE)||s.is(AzraelExecution.SELF)
                ||s.is(DamageTypes.GENERIC_KILL)&&(!(raw>=0)||raw>=UnderworldItemsConfig.KILL_THRESHOLD.get());
    }
    // 死亡事件之前只保存资格，不取消死亡、不提取物品。
    public static AutoCloseable beginDeath(ServerPlayer p,DamageSource source) {
        Death previous=DEATH.get();Hit h=HIT.get();
        ItemStack book=UnderworldItems.equipped(p,false),diary=TravailCurios.stack(p);
        boolean matching=h!=null&&h.player==p&&h.source==source;
        boolean eligible=!book.isEmpty()&&!p.isSpectator()&&!excluded(p,source,matching?h.input:0);
        UUID journey=LongTravailData.queryIdentity(diary).journey();
        boolean witness=eligible&&matching&&h.four&&h.finalDamage>h.max
                &&!LongTravailData.hasWitness(diary,TravailAspect.UNDERWORLD);
        DEATH.set(new Death(p,source,diary,eligible?UnderworldLedger.identity(book):null,journey,witness,eligible));
        return ()->{if(previous==null)DEATH.remove();else DEATH.set(previous);};
    }
    // 在所有 LivingDeathEvent 监听器返回后、原版发送死亡通知前执行。
    public static void accepted(ServerPlayer p,DamageSource s,boolean cancelled) {
        Death d=DEATH.get();if(d==null||d.player!=p||d.source!=s||cancelled)return;
        var ledger=UnderworldLedger.get(p.server);SEARCHES.remove(p.getUUID());ledger.deaths.remove(p.getUUID());ledger.setDirty();
        if(!d.eligible)return;
        CompoundTag n=new CompoundTag();n.putUUID("Player",p.getUUID());n.putUUID("Death",UUID.randomUUID());n.putUUID("Book",d.book);
        n.putString("Dimension",p.level().dimension().location().toString());n.putDouble("X",p.getX());n.putDouble("Y",p.getY());n.putDouble("Z",p.getZ());
        n.putFloat("Yaw",p.getYRot());n.putFloat("Pitch",p.getXRot());n.putBoolean("Hardcore",p.server.isHardcore());
        var dimensions=p.getDimensions(net.minecraft.world.entity.Pose.STANDING);n.putFloat("Width",dimensions.width);n.putFloat("Height",dimensions.height);
        ledger.deaths.put(p.getUUID(),n);ledger.setDirty();
        // 先发送复活资格，再发送原版死亡通知，避免立即重生早于资格同步。
        UnderworldPacket.send(p,n.getUUID("Death"),true);
    }
    public static void finished(ServerPlayer p) {
        Death d=DEATH.get();if(d==null||d.player!=p||p.isAlive())return;
        var ledger=UnderworldLedger.get(p.server);CompoundTag n=ledger.deaths.get(p.getUUID());
        if(n==null)return;
        n.putBoolean("Ready",true);ledger.setDirty();
        if(d.witness&&d.journey!=null) {
            ledger.witness(d.journey);ledger.reconcile(d.diary);UnderworldStorage.player(p);
            UnderworldStorage.sweep(p.server,p);
        }
    }
    public static Respawn respawn(){return RESPAWN.get();}
    public static void request(ServerPlayer p,UUID id) {
        var n=UnderworldLedger.get(p.server).deaths.get(p.getUUID());
        if(p.isAlive()||n==null||!n.getBoolean("Ready")||!n.getUUID("Death").equals(id)||SEARCHES.containsKey(p.getUUID()))return;
        if(n.getBoolean("Hardcore")&&UnderworldLedger.get(p.server).spent(n.getUUID("Book")))return;
        ServerLevel level=p.server.getLevel(ResourceKey.create(Registries.DIMENSION,new ResourceLocation(n.getString("Dimension"))));
        if(level==null)return;
        Vec3 pos=new Vec3(n.getDouble("X"),n.getDouble("Y"),n.getDouble("Z"));
        SEARCHES.put(p.getUUID(),new Search(id,new RevivalPosition(level,pos,n.contains("Width")?n.getFloat("Width"):.6,n.contains("Height")?n.getFloat("Height"):1.8)));
    }
    private static void revive(ServerPlayer p,CompoundTag n,Vec3 pos) {
        var ledger=UnderworldLedger.get(p.server);
        if(p.isAlive()||ledger.deaths.get(p.getUUID())!=n||n.getBoolean("Hardcore")&&ledger.spent(n.getUUID("Book")))return;
        ServerLevel level=p.server.getLevel(ResourceKey.create(Registries.DIMENSION,new ResourceLocation(n.getString("Dimension"))));
        if(level==null)return;
        // 物品消耗与重生在同一服务端任务中执行，记录处理状态以阻止重复网络请求。
        n.putBoolean("Ready",false);ledger.setDirty();
        RESPAWN.set(new Respawn(p,level,pos,p.getRespawnDimension(),p.getRespawnPosition(),p.getRespawnAngle(),p.isRespawnForced()));
        try {
            ServerPlayer fresh=p.server.getPlayerList().respawn(p,false);
            fresh.connection.player=fresh;
            if(n.getBoolean("Hardcore")){ledger.consume(n.getUUID("Book"));UnderworldStorage.player(fresh);UnderworldStorage.sweep(p.server,fresh);}
            fresh.connection.teleport(pos.x,pos.y,pos.z,n.getFloat("Yaw"),n.getFloat("Pitch"));
            ((SpawnProtectionAccess)fresh).travail$protection(UnderworldItemsConfig.PROTECTION.get());
            ledger.deaths.remove(p.getUUID());ledger.setDirty();UnderworldPacket.send(fresh,n.getUUID("Death"),false);
        } finally {RESPAWN.remove();}
    }
    @SubscribeEvent public static void tick(TickEvent.ServerTickEvent e) {
        if(e.phase!=TickEvent.Phase.END)return;
        UnderworldStorage.afterLoads(e.getServer());
        UnderworldMaintenance.tick(e.getServer());
        for(UUID player:List.copyOf(SEARCHES.keySet())) {
            ServerPlayer p=e.getServer().getPlayerList().getPlayer(player);Search search=SEARCHES.get(player);
            CompoundTag n=UnderworldLedger.get(e.getServer()).deaths.get(player);
            if(p==null||p.isAlive()||n==null||!n.getUUID("Death").equals(search.death)){SEARCHES.remove(player);continue;}
            if(search.search.step(256)){
                SEARCHES.remove(player);
                if(search.search.result()!=null){if(search.search.clear(search.search.result()))revive(p,n,search.search.result());else request(p,search.death);}
            }
        }
    }
    @SubscribeEvent public static void playerTick(TickEvent.PlayerTickEvent e) {
        if(e.phase==TickEvent.Phase.END&&e.player instanceof ServerPlayer p&&p.tickCount%20==0)UnderworldMaintenance.request(p);
    }
    @SubscribeEvent public static void login(PlayerEvent.PlayerLoggedInEvent e) {
        if(e.getEntity() instanceof ServerPlayer p) {
            var n=UnderworldLedger.get(p.server).deaths.get(p.getUUID());
            if(n!=null&&!p.isAlive())UnderworldPacket.send(p,n.getUUID("Death"),true);
            else if(n!=null){UnderworldLedger.get(p.server).deaths.remove(p.getUUID());UnderworldLedger.get(p.server).setDirty();}
        }
    }
    @SubscribeEvent public static void normalRespawn(PlayerEvent.PlayerRespawnEvent e) {
        if(e.getEntity() instanceof ServerPlayer p&&(RESPAWN.get()==null||!RESPAWN.get().old.getUUID().equals(p.getUUID()))){var d=UnderworldLedger.get(p.server);d.deaths.remove(p.getUUID());d.setDirty();SEARCHES.remove(p.getUUID());UnderworldPacket.send(p,new UUID(0,0),false);}
    }
    @SubscribeEvent public static void logoutMaintenance(PlayerEvent.PlayerLoggedOutEvent e){UnderworldMaintenance.forget(e.getEntity().getUUID());}
    @SubscribeEvent public static void cloneMaintenance(PlayerEvent.Clone e){UnderworldMaintenance.forget(e.getOriginal().getUUID());}
    @SubscribeEvent public static void stopped(ServerStoppedEvent e){UnderworldStorage.clearPending();UnderworldMaintenance.clear();SEARCHES.clear();HIT.remove();DEATH.remove();RESPAWN.remove();}
    private BookRevival(){}
}
