package com.thelongtravail.boundless;
import com.thelongtravail.config.BoundlessItemsConfig;
import com.thelongtravail.network.BoundlessPacket;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.phys.Vec3;
public final class DreamState {
    public static final String RECORD="LongTravailDream",COOLDOWN="LongTravailDreamCooldown";
    public static boolean active(ServerPlayer p){return p.getPersistentData().contains(RECORD);}
    public static void cast(ServerPlayer p){
        if(TimeStopManager.frozen(p))return;
        if(active(p)){finish(p,true);return;}
        if(!p.isAlive()||p.isSpectator()||!BoundlessEquipment.equipped(p,true)||p.containerMenu!=p.inventoryMenu)return;
        if(p.getPersistentData().getInt(COOLDOWN)>0){message(p,"unavailable");return;}
        CompoundTag n=new CompoundTag();n.putInt("mode",p.gameMode.getGameModeForPlayer().getId());n.putString("dimension",p.level().dimension().location().toString());
        n.putDouble("x",p.getX());n.putDouble("y",p.getY());n.putDouble("z",p.getZ());n.putInt("left",BoundlessItemsConfig.ticks("dream.duration"));
        p.getPersistentData().put(RECORD,n);
        if(!p.setGameMode(GameType.SPECTATOR)){p.getPersistentData().remove(RECORD);message(p,"unavailable");return;}
        p.stopRiding();p.getPersistentData().putInt(COOLDOWN,BoundlessItemsConfig.ticks("dream.cooldown"));BoundlessPacket.sendDream(p,true);message(p,"success");
    }
    public static void tick(ServerPlayer p){
        int oldCooldown=p.getPersistentData().getInt(COOLDOWN);StarVoiceState.decrement(p,COOLDOWN);
        if(oldCooldown>0&&p.getPersistentData().getInt(COOLDOWN)==0)BoundlessPacket.sendDream(p,active(p));
        if(!active(p))return;
        CompoundTag n=p.getPersistentData().getCompound(RECORD);
        if(!p.isAlive()||!p.isSpectator()||!BoundlessEquipment.equipped(p,true)||!n.getString("dimension").equals(p.level().dimension().location().toString())){finish(p,p.isAlive());return;}
        int left=n.getInt("left")-1;n.putInt("left",left);if(left<=0)finish(p,true);
    }
    private static boolean safe(ServerPlayer p,Vec3 v){
        BlockPos b=BlockPos.containing(v);var l=p.serverLevel();
        var box=p.getDimensions(Pose.STANDING).makeBoundingBox(v);
        if(!l.hasChunkAt(b)||!l.getWorldBorder().isWithinBounds(b)||v.y<l.getMinBuildHeight()||v.y+2>=l.getMaxBuildHeight()||!l.noCollision(null,box))return false;
        // 观察者经常略微悬空；允许5格内自然落地，落地伤害仍按原版计算。
        // 略微扩大检测范围，确保恰好相距 5 格的支撑面也能被碰撞检测识别。
        double search=5.0001;
        double drop=net.minecraft.world.phys.shapes.Shapes.collide(net.minecraft.core.Direction.Axis.Y,box,l.getBlockCollisions(null,box.expandTowards(0,-search,0)),-search);
        if(drop < -5.00001)return false;
        // 检查整个站立身体与落地路径，包含实际支撑表面；仅查脚底中心会漏掉侧面危险。
        var path=box.expandTowards(0,drop,0);
        for(BlockPos at:BlockPos.betweenClosed(BlockPos.containing(path.minX,path.minY-1e-7,path.minZ),
                BlockPos.containing(path.maxX-1e-7,path.maxY-1e-7,path.maxZ-1e-7))){
            var block=l.getBlockState(at);
            if(!l.getFluidState(at).isEmpty()||block.is(net.minecraft.tags.BlockTags.FIRE)
                    ||block.is(net.minecraft.world.level.block.Blocks.POWDER_SNOW)
                    ||block.is(net.minecraft.world.level.block.Blocks.WITHER_ROSE)
                    ||block.is(net.minecraft.world.level.block.Blocks.MAGMA_BLOCK)
                    ||block.is(net.minecraft.world.level.block.Blocks.CACTUS)
                    ||block.is(net.minecraft.world.level.block.Blocks.SWEET_BERRY_BUSH)
                    ||block.is(net.minecraft.tags.BlockTags.CAMPFIRES)
                        &&block.hasProperty(net.minecraft.world.level.block.CampfireBlock.LIT)
                        &&block.getValue(net.minecraft.world.level.block.CampfireBlock.LIT))return false;
        }
        return true;
    }
    private static Vec3 near(ServerPlayer p,Vec3 anchor,int radius){
        p.serverLevel().getChunk(BlockPos.containing(anchor));
        for(int r=0;r<=radius;r++)for(int dy=0;dy<=8;dy++)for(int sign:dy==0?new int[]{1}:new int[]{1,-1})
            for(int x=-r;x<=r;x++)for(int z=-r;z<=r;z++){
                if(Math.max(Math.abs(x),Math.abs(z))!=r)continue;
                Vec3 v=Vec3.atBottomCenterOf(BlockPos.containing(anchor).offset(x,dy*sign,z));
                if(safe(p,v))return v;
            }
        return null;
    }
    private static Vec3 surface(ServerPlayer p,Vec3 anchor){
        var l=p.serverLevel();
        for(int r=0;r<=16;r++)for(int x=-r;x<=r;x++)for(int z=-r;z<=r;z++){
            if(Math.max(Math.abs(x),Math.abs(z))!=r)continue;
            BlockPos column=BlockPos.containing(anchor).offset(x,0,z);
            if(!l.getWorldBorder().isWithinBounds(column))continue;
            int height=l.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,column.getX(),column.getZ());
            Vec3 candidate=Vec3.atBottomCenterOf(new BlockPos(column.getX(),height,column.getZ()));
            if(safe(p,candidate))return candidate;
        }
        return null;
    }
    private static void message(ServerPlayer p,String key){com.thelongtravail.network.TravailNetwork.sendDreamMessage(p,key);}
    public static void finish(ServerPlayer p){finish(p,false);}
    public static void finish(ServerPlayer p,boolean notify){
        if(!active(p)){BoundlessPacket.sendDream(p,false);return;}
        CompoundTag n=p.getPersistentData().getCompound(RECORD).copy();p.getPersistentData().remove(RECORD);
        // 外部修改游戏模式时只撤销托管标记，不覆盖管理员的选择。
        if(p.isSpectator()){
            p.setCamera(p);Vec3 landing=p.position();
            if(!safe(p,landing)){
                Vec3 anchor=n.getString("dimension").equals(p.level().dimension().location().toString())?new Vec3(n.getDouble("x"),n.getDouble("y"),n.getDouble("z")):Vec3.atBottomCenterOf(p.serverLevel().getSharedSpawnPos());
                Vec3 found=safe(p,anchor)?anchor:near(p,anchor,8);
                if(found==null)found=surface(p,anchor);
                if(found==null){Vec3 spawn=Vec3.atBottomCenterOf(p.serverLevel().getSharedSpawnPos());found=near(p,spawn,8);if(found==null)found=surface(p,spawn);}
                landing=found==null?anchor:found;
            }
            p.teleportTo(p.serverLevel(),landing.x,landing.y,landing.z,p.getYRot(),p.getXRot());p.fallDistance=0;
            p.setGameMode(GameType.byId(n.getInt("mode")));
        }
        BoundlessPacket.sendDream(p,false);
        if(notify)message(p,"wake");
    }
    private DreamState(){}
}
