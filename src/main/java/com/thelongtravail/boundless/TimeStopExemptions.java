package com.thelongtravail.boundless;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraftforge.fml.ModList;
import java.util.*;
public final class TimeStopExemptions {
    private static boolean warned;
    public static Set<UUID> snapshot(ServerPlayer p){
        Set<UUID> out=new HashSet<>();out.add(p.getUUID());
        for(ServerPlayer other:p.server.getPlayerList().getPlayers()){
            boolean same=p.getTeam()!=null&&p.getTeam().isAlliedTo(other.getTeam());
            if(!same&&ModList.get().isLoaded("ftbteams"))try{
                Class<?> apiClass=Class.forName("dev.ftb.mods.ftbteams.api.FTBTeamsAPI");Object api=apiClass.getMethod("api").invoke(null);
                Class<?> apiInterface=Class.forName("dev.ftb.mods.ftbteams.api.FTBTeamsAPI$API");
                if((boolean)apiInterface.getMethod("isManagerLoaded").invoke(api)){
                    Object manager=apiInterface.getMethod("getManager").invoke(api);
                    same=(boolean)Class.forName("dev.ftb.mods.ftbteams.api.TeamManager").getMethod("arePlayersInSameTeam",UUID.class,UUID.class).invoke(manager,p.getUUID(),other.getUUID());
                }
            }catch(ReflectiveOperationException|LinkageError e){if(!warned){warned=true;com.thelongtravail.TheLongTravail.LOGGER.warn("FTB Teams时停豁免适配不可用",e);}}
            if(same)out.add(other.getUUID());
        }
        if(ModList.get().isLoaded("ftbteams"))try{
            Class<?> type=Class.forName("dev.ftb.mods.ftbteams.api.FTBTeamsAPI$API");
            Object api=Class.forName("dev.ftb.mods.ftbteams.api.FTBTeamsAPI").getMethod("api").invoke(null);
            if((boolean)type.getMethod("isManagerLoaded").invoke(api)){
                Object manager=type.getMethod("getManager").invoke(api);
                var team=(Optional<?>)Class.forName("dev.ftb.mods.ftbteams.api.TeamManager").getMethod("getTeamForPlayer",ServerPlayer.class).invoke(manager,p);
                if(team.isPresent())for(Object member:(Collection<?>)Class.forName("dev.ftb.mods.ftbteams.api.Team").getMethod("getMembers").invoke(team.get()))if(member instanceof UUID id)out.add(id);
            }
        }catch(ReflectiveOperationException|LinkageError e){if(!warned){warned=true;com.thelongtravail.TheLongTravail.LOGGER.warn("FTB Teams时停豁免快照不可用",e);}}
        return Set.copyOf(out);
    }
    public static UUID owner(Entity e){
        // 高频普通实体和链首终点不需要循环检测集合；真实归属链仍保留 UUID 去重和 16 层上限。
        if(e==null)return null;
        if(e instanceof net.minecraft.world.entity.player.Player)return e.getUUID();
        if(e instanceof net.minecraft.world.entity.animal.allay.Allay allay)return allay.getBrain().getMemory(net.minecraft.world.entity.ai.memory.MemoryModuleType.LIKED_PLAYER).orElse(null);
        if(!(e instanceof OwnableEntity||e instanceof Projectile
                ||e instanceof net.minecraft.world.entity.projectile.EvokerFangs||e instanceof net.minecraft.world.entity.LightningBolt))return null;
        Set<UUID> seen=new HashSet<>();
        for(int depth=0;e!=null&&depth<16&&seen.add(e.getUUID());depth++){
            if(e instanceof net.minecraft.world.entity.player.Player)return e.getUUID();
            if(e instanceof net.minecraft.world.entity.animal.allay.Allay allay)return allay.getBrain().getMemory(net.minecraft.world.entity.ai.memory.MemoryModuleType.LIKED_PLAYER).orElse(null);
            if(e instanceof net.minecraft.world.entity.projectile.EvokerFangs fangs){e=fangs.getOwner();continue;}
            if(e instanceof net.minecraft.world.entity.LightningBolt bolt){e=bolt.getCause();continue;}
            if(e instanceof Projectile shot){Entity next=shot.getOwner();if(next==null)return ((com.thelongtravail.mixin.TimeStopProjectileAccessor)(Object)shot).travail$ownerUUID();e=next;continue;}
            if(!(e instanceof OwnableEntity owned))return null;
            Entity next=owned.getOwner();if(next==null)return owned.getOwnerUUID();e=next;
        }return null;
    }
    public static boolean exempt(Entity e,Set<UUID> allies){if(allies.contains(e.getUUID()))return true;UUID owner=owner(e);return owner!=null&&allies.contains(owner);}
    private TimeStopExemptions(){}
}
