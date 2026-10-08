package com.thelongtravail.valley;

import com.thelongtravail.registry.ModRegistry;
import com.thelongtravail.boundless.TimeStopExemptions;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraftforge.fml.ModList;
import top.theillusivec4.curios.api.CuriosApi;
import java.util.*;

// 仅服务端运行；关系和状态不写入物品或玩家存档。
public final class SwordLanternState {
    public static final double RANGE_SQR=64*64;
    private static final Map<UUID,State> STATES=new HashMap<>();
    private static final class State {
        final Map<UUID,Long> enemies=new HashMap<>();
        List<LivingEntity> nearby=List.of();
        long scan=Long.MIN_VALUE, start=-1, last=-1;
    }
    public static boolean equipped(Player p) {
        return p.isAlive()&&!p.isSpectator()&&CuriosApi.getCuriosInventory(p).map(inv->inv.findCurios(ModRegistry.SWORD_AND_LANTERN.get()).stream()
                .anyMatch(r->!r.slotContext().cosmetic()&&r.slotContext().identifier().equals("belt"))).orElse(false);
    }
    private static boolean warned;
    public static boolean teammates(ServerPlayer p,Player other) {
        if(p==other||p.getTeam()!=null&&p.getTeam().isAlliedTo(other.getTeam()))return true;
        if(!ModList.get().isLoaded("ftbteams"))return false;
        try {
            Object api=Class.forName("dev.ftb.mods.ftbteams.api.FTBTeamsAPI").getMethod("api").invoke(null);
            Class<?> apiType=Class.forName("dev.ftb.mods.ftbteams.api.FTBTeamsAPI$API");
            if(!(boolean)apiType.getMethod("isManagerLoaded").invoke(api))return false;
            Object manager=apiType.getMethod("getManager").invoke(api);
            return (boolean)Class.forName("dev.ftb.mods.ftbteams.api.TeamManager").getMethod("arePlayersInSameTeam",UUID.class,UUID.class).invoke(manager,p.getUUID(),other.getUUID());
        } catch(ReflectiveOperationException|LinkageError e) {
            if(!warned){warned=true;com.thelongtravail.TheLongTravail.LOGGER.warn("剑与灯 FTB Teams 关系查询失败",e);}
            return true; // 无法确认时保守排除玩家，不能把可能的队友视为敌人。
        }
    }
    private static boolean friendly(ServerPlayer p,LivingEntity e) {
        if(e==p||e instanceof Player other&&teammates(p,other)||p.isAlliedTo(e)||e.isAlliedTo(p))return true;
        UUID owner=TimeStopExemptions.owner(e);
        if(p.getUUID().equals(owner))return true;
        ServerPlayer master=owner==null?null:p.server.getPlayerList().getPlayer(owner);
        return master!=null&&teammates(p,master);
    }
    private static boolean nearby(ServerPlayer p,LivingEntity e) {
        return e!=p&&e.isAlive()&&!e.isRemoved()&&!e.isSpectator()&&e.level()==p.level()&&p.distanceToSqr(e)<=RANGE_SQR;
    }
    private static boolean eligible(ServerPlayer p,LivingEntity e) {
        return nearby(p,e)&&e.isAttackable()&&!friendly(p,e)
                &&(!(e instanceof Player other)||!other.isCreative()&&p.server.isPvpAllowed()&&p.canHarmPlayer(other));
    }
    private static State update(ServerPlayer p) {
        State s=STATES.computeIfAbsent(p.getUUID(),id->new State()); long now=p.level().getGameTime();
        if(s.last>=0&&now-s.last>=SwordLanternConfig.ticks(SwordLanternConfig.TIMEOUT)){s.start=-1;s.last=-1;}
        s.enemies.entrySet().removeIf(entry->{
            Entity e=p.serverLevel().getEntity(entry.getKey());
            return now-entry.getValue()>=SwordLanternConfig.ticks(SwordLanternConfig.MEMORY)||!(e instanceof LivingEntity living)||!eligible(p,living);
        });
        if(s.scan==Long.MIN_VALUE||now-s.scan>=10) {
            s.nearby=p.level().getEntitiesOfClass(LivingEntity.class,p.getBoundingBox().inflate(64),e->nearby(p,e));s.scan=now;
        }
        return s;
    }
    private static boolean enemy(ServerPlayer p,State s,LivingEntity e,LivingEntity pending) {
        return eligible(p,e)&&(e==pending||e instanceof Mob mob&&mob.getTarget()==p||s.enemies.containsKey(e.getUUID()));
    }
    public static double bonus(ServerPlayer p,LivingEntity pending) {
        if(!equipped(p)){forget(p);return 0;}
        State s=update(p); Set<UUID> seen=new HashSet<>(); int enemies=0; boolean undead=false,alone=true;
        List<LivingEntity> candidates=new ArrayList<>(s.nearby);
        // 玩家关系及到达范围即时检查，不等待下一次环境扫描。
        candidates.addAll(p.serverLevel().players());
        for(UUID id:s.enemies.keySet()){Entity e=p.serverLevel().getEntity(id);if(e instanceof LivingEntity living)candidates.add(living);}
        if(pending!=null)candidates.add(pending);
        for(LivingEntity e:candidates) {
            if(!seen.add(e.getUUID())||!nearby(p,e))continue;
            boolean hostile=enemy(p,s,e,pending);
            if(hostile){enemies++;undead|=(e==pending?com.thelongtravail.underworld.UnderworldItems.attackType(p,e):e.getMobType())==MobType.UNDEAD;}
            if(e instanceof Player&&!hostile||!(e instanceof Player)&&p.getUUID().equals(TimeStopExemptions.owner(e)))alone=false;
        }
        int conditions=(enemies>10?1:0)+(undead?1:0)+(alone?1:0)
                +(s.start>=0&&p.level().getGameTime()-s.start>SwordLanternConfig.ticks(SwordLanternConfig.DURATION)?1:0)
                +(p.getHealth()<p.getMaxHealth()*SwordLanternConfig.HEALTH.get()?1:0);
        return conditions*SwordLanternConfig.BONUS.get();
    }
    public static void confirmed(LivingEntity victim,DamageSource source) {
        if(!(source.getEntity() instanceof LivingEntity cause)||cause==victim)return;
        if(cause instanceof ServerPlayer p)record(p,victim);
        if(victim instanceof ServerPlayer p)record(p,cause);
    }
    private static void record(ServerPlayer p,LivingEntity other) {
        // 致死一击也刷新久战，但死亡目标不会继续留在有效敌人集合中。
        if(!equipped(p)||other==p||other.level()!=p.level()||p.distanceToSqr(other)>RANGE_SQR
                ||other.isSpectator()||friendly(p,other)
                ||other instanceof Player target&&(!p.server.isPvpAllowed()||target.isCreative()||!p.canHarmPlayer(target)))return;
        State s=update(p);long now=p.level().getGameTime();
        if(s.start<0)s.start=now;s.last=now;s.enemies.put(other.getUUID(),now);
    }
    public static void tick(ServerPlayer p){if(equipped(p))update(p);else forget(p);}
    public static void forget(ServerPlayer p){STATES.remove(p.getUUID());}
    public static void clear(){STATES.clear();}
    private SwordLanternState(){}
}
