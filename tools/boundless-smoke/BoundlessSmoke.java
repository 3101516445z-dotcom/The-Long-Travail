package travail.smoke;

import com.mojang.authlib.GameProfile;
import com.thelongtravail.TravailAspect;
import com.thelongtravail.boundless.*;
import com.thelongtravail.config.BoundlessItemsConfig;
import com.thelongtravail.data.LongTravailData;
import com.thelongtravail.registry.ModRegistry;
import net.minecraft.core.*;
import net.minecraft.server.level.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.animal.*;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.item.*;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.fml.common.Mod;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.type.capability.ICurio.DropRule;
import top.theillusivec4.curios.common.inventory.CurioStacksHandler;
import java.util.*;

@Mod("travail_smoke")
public final class BoundlessSmoke {
    private static void check(boolean value,String message){if(!value)throw new AssertionError(message);}
    // PlayerList 的公开视图不可修改；隔离夹具将虚拟连接玩家登记到其内部列表，接受真实服务端刻。
    @SuppressWarnings("unchecked") private static List<ServerPlayer> registeredPlayers(net.minecraft.server.MinecraftServer server) throws Exception {
        var field=net.minecraft.server.players.PlayerList.class.getDeclaredField("players");field.setAccessible(true);
        return (List<ServerPlayer>)field.get(server.getPlayerList());
    }
    private static final class TestPlayer extends ServerPlayer {
        TestPlayer(ServerLevel l){super(l.getServer(),l,new GameProfile(UUID.randomUUID(),"BoundlessTest"));
            var wire=new net.minecraft.network.Connection(net.minecraft.network.protocol.PacketFlow.SERVERBOUND){@Override public void send(net.minecraft.network.protocol.Packet<?> p){}};
            connection=new net.minecraft.server.network.ServerGamePacketListenerImpl(l.getServer(),wire,this){@Override public void send(net.minecraft.network.protocol.Packet<?> p){}};
            try{var f=ServerPlayer.class.getDeclaredField("spawnInvulnerableTime");f.setAccessible(true);f.setInt(this,0);}catch(Exception e){throw new RuntimeException(e);}
        }
    }
    private static final class Companion extends Cow implements OwnableEntity {
        LivingEntity owner;
        Companion(ServerLevel level,LivingEntity owner){super(EntityType.COW,level);this.owner=owner;}
        @Override public UUID getOwnerUUID(){return owner==null?null:owner.getUUID();}
        @Override public LivingEntity getOwner(){return owner;}
    }
    private static ItemStack equip(TestPlayer p,boolean witness){
        var inv=CuriosApi.getCuriosInventory(p).resolve().orElseThrow();
        Map<String,top.theillusivec4.curios.api.type.inventory.ICurioStacksHandler> slots=new HashMap<>();
        for(String id:List.of("travel_diary","head","necklace"))slots.put(id,new CurioStacksHandler(inv,id,2,true,false,true,DropRule.DEFAULT));inv.setCurios(slots);
        ItemStack diary=new ItemStack(ModRegistry.LONG_TRAVAIL.get());LongTravailData.initialize(diary,p);
        for(TravailAspect aspect:TravailAspect.values())LongTravailData.setWitness(diary,aspect,true);
        LongTravailData.setWitness(diary,TravailAspect.BOUNDLESS,witness);
        inv.getStacksHandler("travel_diary").orElseThrow().getStacks().setStackInSlot(0,diary);
        inv.getStacksHandler("head").orElseThrow().getStacks().setStackInSlot(0,new ItemStack(ModRegistry.DAYDREAM.get()));
        inv.getStacksHandler("necklace").orElseThrow().getStacks().setStackInSlot(0,new ItemStack(ModRegistry.STAR_VOICE.get()));return diary;
    }
    private static Cow cow(ServerLevel l,Vec3 pos){var c=new Cow(EntityType.COW,l);c.setPos(pos);l.addFreshEntity(c);return c;}
    private static void reset(ServerLevel l){TimeStopManager.get(l).expire(Long.MAX_VALUE);TimeStopManager.clear();}
    private Cow frozen,outside;private TestPlayer caster,dreamPlayer;private float hurtHealth;private int ticks,originalAge;private Vec3 originalPosition;private ServerLevel level;private long firstGameTime,firstDayTime;private int firstRainTime;
    public BoundlessSmoke(){MinecraftForge.EVENT_BUS.addListener(this::start);MinecraftForge.EVENT_BUS.addListener(this::tick);}
    private void start(ServerStartedEvent e){try{
        // 概率强制测试使用内存配置，避免文件监听器与连续 autosave 相互干扰。
        com.thelongtravail.config.TravailConfig.SPECS.get("aspects/boundless.toml").setConfig(com.electronwill.nightconfig.core.CommentedConfig.inMemory());
        level=e.getServer().overworld();level.getChunk(0,0);level.getChunk(4,0);
        var direct=new ArrayList<net.minecraft.network.chat.Component>();new ItemStack(ModRegistry.DAYDREAM.get()).getItem().appendHoverText(new ItemStack(ModRegistry.DAYDREAM.get()),level,direct,net.minecraft.world.item.TooltipFlag.Default.NORMAL);check(direct.size()==5,"dedicated-server tooltip generation has no client dependency");
        var p=new TestPlayer(level);p.setPos(0,80,0);var q=new TestPlayer(level);q.setPos(60,80,0);var z=new TestPlayer(level);
        long now=level.getGameTime();var m=TimeStopManager.get(level);
        check(m.start(p,true,now,p.position(),32,40,Set.of(p.getUUID())),"first cast");
        check(m.start(q,true,now+20,q.position(),32,100,Set.of(q.getUUID())),"intersecting spheres join");
        check(!m.start(z,true,now+20,p.position(),32,100,Set.of(z.getUUID())),"chunk cap rejects third");
        check(TimeStopManager.frozen(p)==false,"caster immune in exclusive area");
        p.setPos(30,80,0);check(TimeStopManager.frozen(p),"enemy overlap overrides caster exemption");p.setPos(0,80,0);
        m.expire(now+40);check(TimeStopManager.locked(p.getUUID()),"original participant remains locked after own field expires");
        check(!m.start(p,true,now+40,p.position(),32,40,Set.of(p.getUUID())),"same player cannot recast while group alive");
        m.expire(now+120);check(!TimeStopManager.locked(p.getUUID()),"group releases");check(p.getPersistentData().getInt(StarVoiceState.COOLDOWN)==100,"post group witness cooldown");
        reset(level);p.setPos(0,80,0);q.setPos(60,80,0);m=TimeStopManager.get(level);
        check(m.start(p,true,now,p.position(),32,300,Set.of(p.getUUID())),"long field");
        check(m.start(q,true,now+260,q.position(),32,100,Set.of(q.getUUID())),"13 second late join");
        check(TimeStopManager.fields(level).stream().allMatch(f->f.end==now+300),"late nonteam field clipped to 15 second deadline");reset(level);
        p.setPos(0,80,0);q.setPos(120,80,0);z.setPos(60,80,0);m=TimeStopManager.get(level);
        check(m.start(p,true,now,p.position(),32,300,Set.of(p.getUUID())),"old group");check(m.start(q,true,now+220,q.position(),32,200,Set.of(q.getUUID())),"separate new group");
        check(m.start(z,true,now+260,z.position(),32,100,Set.of(z.getUUID())),"bridge merges separate groups");check(TimeStopManager.fields(level).stream().allMatch(f->f.end==now+300),"merge clips previous newer group too");reset(level);
        var wolf=new Wolf(EntityType.WOLF,level);wolf.setOwnerUUID(p.getUUID());var arrow=new Arrow(EntityType.ARROW,level);arrow.setOwner(wolf);
        check(TimeStopExemptions.exempt(wolf,Set.of(p.getUUID()))&&TimeStopExemptions.exempt(arrow,Set.of(p.getUUID())),"companion projectile exemption");
        var allay=new net.minecraft.world.entity.animal.allay.Allay(EntityType.ALLAY,level);
        allay.getBrain().setMemory(net.minecraft.world.entity.ai.memory.MemoryModuleType.LIKED_PLAYER,p.getUUID());
        check(TimeStopExemptions.exempt(allay,Set.of(p.getUUID())),"allay liked-player exemption");
        var maid=new Companion(level,p);var summon=new Companion(level,maid);arrow.setOwner(summon);
        check(TimeStopExemptions.exempt(summon,Set.of(p.getUUID()))&&TimeStopExemptions.exempt(arrow,Set.of(p.getUUID())),"companion of companion and its projectile");
        maid.owner=summon;check(TimeStopExemptions.owner(summon)==null,"cyclic ownership fails closed without recursion");
        check(TimeStopGeometry.chunks(new Vec3(-.5,0,-.5),32).contains(ChunkPos.asLong(-1,-1)),"negative chunk projection");
        check(Math.abs(TimeStopGeometry.entry(new Vec3(-50,80,0),new Vec3(100,0,0),new Vec3(0,80,0),32)-.18)<1e-8,"fast trajectory intersects sphere");
        var params=new net.minecraft.world.level.storage.loot.LootParams.Builder(level).withParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.ORIGIN,Vec3.ZERO).create(net.minecraft.world.level.storage.loot.parameters.LootContextParamSets.CHEST);
        for(String id:List.of("stronghold_corridor","stronghold_crossing","stronghold_library","end_city_treasure","village/village_plains_house")){
            var table=level.getServer().getLootData().getLootTable(new net.minecraft.resources.ResourceLocation("minecraft","chests/"+id));check(table.getPool("the_long_travail:shared_items")!=null,"loaded chest pool "+id);int dreams=0,stars=0;
            for(int seed=1;seed<=5000;seed++){int count=0;for(ItemStack loot:table.getRandomItems(params,seed*7919L)){if(loot.is(ModRegistry.DAYDREAM.get())){dreams+=loot.getCount();count+=loot.getCount();}if(loot.is(ModRegistry.STAR_VOICE.get())){stars+=loot.getCount();count+=loot.getCount();}}check(count<=1,"shared pool max one accessory");}
            double d=id.startsWith("village/")?.02:.1,s=id.startsWith("village/")?0:id.equals("end_city_treasure")?.05:.02;
            check(Math.abs(dreams-5000*d)<4*Math.sqrt(5000*d*(1-d))&&(s==0?stars==0:Math.abs(stars-5000*s)<4*Math.sqrt(5000*s*(1-s))),"absolute loot probabilities "+id+" "+dreams+"/"+stars);System.out.println("BOUNDLESS_LOOT "+id+" "+dreams+"/"+stars);
        }
        equip(p,true);p.getPersistentData().remove(StarVoiceState.COOLDOWN);p.getPersistentData().remove(StarVoiceState.INTERVAL);
        BoundlessItemsConfig.NUMBERS.get("star.witnessChance").set(1d);
        var victim=cow(level,new Vec3(2,80,0));victim.setAbsorptionAmount(5);victim.hurt(level.damageSources().playerAttack(p),1);
        check(TimeStopManager.locked(p.getUUID()),"actual absorbed attack can trigger");
        check(TimeStopManager.fields(level).size()==1,"one HP absorption hurt gets one cast");
        reset(level);p.getPersistentData().remove(StarVoiceState.COOLDOWN);p.getPersistentData().remove(StarVoiceState.INTERVAL);
        BoundlessItemsConfig.NUMBERS.get("star.witnessChance").set(0d);
        cow(level,new Vec3(3,80,0)).hurt(level.damageSources().playerAttack(p),1);
        check(p.getPersistentData().getInt(StarVoiceState.INTERVAL)==20,"failed witness roll consumes interval");
        BoundlessItemsConfig.NUMBERS.get("star.witnessChance").set(1d);cow(level,new Vec3(4,80,0)).hurt(level.damageSources().playerAttack(p),1);
        check(TimeStopManager.fields(level).isEmpty(),"AOE attack same tick cannot roll again");
        ItemStack diary=equip(p,false);p.invulnerableTime=0;BoundlessItemsConfig.NUMBERS.get("star.maliceChance").set(1d);
        p.hurt(level.damageSources().generic(),1);check(TimeStopManager.fields(level).isEmpty(),"unsourced damage cannot trigger malice");p.invulnerableTime=0;
        p.hurt(level.damageSources().playerAttack(p),1);check(TimeStopManager.fields(level).isEmpty(),"self source cannot trigger");p.invulnerableTime=0;
        p.setHealth(20);p.setAbsorptionAmount(0);p.removeAllEffects();p.invulnerableTime=0;p.setGameMode(GameType.SURVIVAL);
        check(!com.thelongtravail.api.TravailStateApi.hasBoundlessWitness(p)&&BoundlessEquipment.equipped(p,false),"malice equipment state");
        boolean received=p.hurt(level.damageSources().mobAttack(victim),3);check(TimeStopManager.locked(p.getUUID()),"sourced actual received damage triggers malice: hurt="+received+" health="+p.getHealth()+" frozen="+TimeStopManager.frozen(p)+" alive="+p.isAlive()+" chance="+BoundlessItemsConfig.get("star.maliceChance"));reset(level);
        LongTravailData.setWitness(diary,TravailAspect.BOUNDLESS,true);p.setHealth(20);p.invulnerableTime=0;p.setGameMode(GameType.SURVIVAL);
        level.setBlock(new BlockPos(0,79,0),Blocks.STONE.defaultBlockState(),3);p.setPos(.5,80,.5);DreamState.cast(p);
        check(p.isSpectator()&&DreamState.active(p),"dream enters spectator");check(p.getPersistentData().getInt(DreamState.COOLDOWN)==1200,"player cooldown at cast");
        DreamState.cast(p);check(!p.isSpectator()&&!DreamState.active(p),"second press wakes");DreamState.cast(p);check(!p.isSpectator(),"cooldown blocks new dream");
        level.setBlock(new BlockPos(20,79,0),Blocks.STONE.defaultBlockState(),3);
        for(double gap:new double[]{0,.2,1,2.5,3,4,5}){
            p.getPersistentData().putInt(DreamState.COOLDOWN,0);p.setPos(.5,80,.5);DreamState.cast(p);
            Vec3 destination=new Vec3(20.5,80+gap,.5);p.setPos(destination);DreamState.finish(p);
            check(p.position().distanceToSqr(destination)<1e-8,"safe wake keeps current position; ground gap="+gap+" actual="+p.position());
        }
        p.getPersistentData().putInt(DreamState.COOLDOWN,0);p.setPos(.5,80,.5);DreamState.cast(p);p.setPos(20.5,85.1,.5);DreamState.finish(p);
        check(p.position().distanceToSqr(new Vec3(.5,80,.5))<1e-8,"more than 5 blocks still returns to entry");
        level.setBlock(new BlockPos(24,79,0),Blocks.STONE_SLAB.defaultBlockState(),3);
        p.getPersistentData().putInt(DreamState.COOLDOWN,0);p.setPos(.5,80,.5);DreamState.cast(p);p.setPos(24.5,79.7,.5);DreamState.finish(p);
        check(p.position().distanceToSqr(new Vec3(24.5,79.7,.5))<1e-8,"hovering above a bottom slab keeps current position");
        p.getPersistentData().putInt(DreamState.COOLDOWN,0);p.setPos(.5,80,.5);DreamState.cast(p);p.setPos(96.5,200,.5);DreamState.finish(p);
        check(p.position().distanceToSqr(new Vec3(.5,80,.5))<1e-8,"unsupported high/void position still returns to entry");
        level.setBlock(new BlockPos(20,80,0),Blocks.STONE.defaultBlockState(),3);
        p.getPersistentData().putInt(DreamState.COOLDOWN,0);p.setPos(.5,80,.5);DreamState.cast(p);p.setPos(20.5,80,.5);DreamState.finish(p);
        check(p.position().distanceToSqr(new Vec3(.5,80,.5))<1e-8,"inside solid block still returns to entry");
        level.setBlock(new BlockPos(20,80,0),Blocks.AIR.defaultBlockState(),3);
        List<String> edgeFailures=new ArrayList<>();
        // 从非整格、非方块中心入梦时，回退应保留原始坐标。
        Vec3 exactAnchor=new Vec3(.25,80.2,.75);
        p.getPersistentData().putInt(DreamState.COOLDOWN,0);p.setPos(exactAnchor);DreamState.cast(p);p.setPos(96.5,200,.5);DreamState.finish(p);
        check(p.position().distanceToSqr(exactAnchor)<1e-8,"unsafe wake returns to exact safe entry coordinates: "+p.position());
        // 足底找到支撑不代表整个站立身体及落地路径没有危险。
        level.setBlock(new BlockPos(28,79,0),Blocks.CAMPFIRE.defaultBlockState(),3);
        p.getPersistentData().putInt(DreamState.COOLDOWN,0);p.setPos(.5,80,.5);DreamState.cast(p);p.setPos(28.5,79.5375,.5);DreamState.finish(p);
        if(p.position().distanceToSqr(new Vec3(.5,80,.5))>=1e-8)edgeFailures.add("lit campfire support is unsafe");
        level.setBlock(new BlockPos(21,80,0),Blocks.WATER.defaultBlockState(),3);
        p.getPersistentData().putInt(DreamState.COOLDOWN,0);p.setPos(.5,80,.5);DreamState.cast(p);p.setPos(20.85,80,.5);DreamState.finish(p);
        if(p.position().distanceToSqr(new Vec3(.5,80,.5))>=1e-8)edgeFailures.add("water touching side of standing body is unsafe");
        level.setBlock(new BlockPos(21,80,0),Blocks.AIR.defaultBlockState(),3);
        level.setBlock(new BlockPos(28,79,0),Blocks.CAMPFIRE.defaultBlockState().setValue(net.minecraft.world.level.block.CampfireBlock.LIT,false),3);
        p.getPersistentData().putInt(DreamState.COOLDOWN,0);p.setPos(.5,80,.5);DreamState.cast(p);Vec3 coldCampfire=new Vec3(28.5,79.5375,.5);p.setPos(coldCampfire);DreamState.finish(p);
        check(p.position().distanceToSqr(coldCampfire)<1e-8,"unlit campfire support remains safe");
        equip(p,false);BoundlessItemsConfig.NUMBERS.get("star.maliceChance").set(1d);p.setHealth(20);p.invulnerableTime=0;
        var ownerlessArrow=new Arrow(EntityType.ARROW,level);
        p.hurt(level.damageSources().arrow(ownerlessArrow,null),2);
        if(!TimeStopManager.locked(p.getUUID()))edgeFailures.add("ownerless projectile is still a sourced damage entity");
        reset(level);
        check(edgeFailures.isEmpty(),"additional boundary failures: "+edgeFailures);
        p.invulnerableTime=0;p.setHealth(20);ownerlessArrow.setOwner(p);
        p.hurt(level.damageSources().arrow(ownerlessArrow,null),2);
        check(!TimeStopManager.locked(p.getUUID()),"own projectile with missing source shooter still cannot trigger");
        p.invulnerableTime=0;p.hurt(level.damageSources().arrow(ownerlessArrow,p),2);
        check(!TimeStopManager.locked(p.getUUID()),"normally attributed self projectile cannot trigger");
        check(com.thelongtravail.api.TimeStopApi.isFrozen((Entity)null)==false,"optional script API null safety");
        // 同组最后一个区域结束，离线成员重新登录仍获得完整组后冷却。
        p.setPos(0,80,0);q.setPos(60,80,0);m=TimeStopManager.get(level);
        check(m.start(p,true,now,p.position(),32,40,Set.of(p.getUUID())),"offline cooldown first member");
        check(m.start(q,true,now+20,q.position(),32,100,Set.of(q.getUUID())),"offline cooldown second member");
        m.expire(now+40);StarVoiceState.tick(p);check(p.getPersistentData().getInt(StarVoiceState.COOLDOWN)==100,"own field end does not consume cooldown during group lock");
        m.expire(now+120);p.getPersistentData().remove(StarVoiceState.COOLDOWN);TimeStopManager.login(p);
        check(p.getPersistentData().getInt(StarVoiceState.COOLDOWN)==100,"offline group end cooldown restored on login");
        for(int i=0;i<99;i++)StarVoiceState.tick(p);check(p.getPersistentData().getInt(StarVoiceState.COOLDOWN)==1,"full five second cooldown still blocks through tick 99");
        StarVoiceState.tick(p);check(p.getPersistentData().getInt(StarVoiceState.COOLDOWN)==0,"five second cooldown expires at tick 100");reset(level);
        // 冷却由玩家持有，死亡克隆不能消除。
        p.getPersistentData().putInt(DreamState.COOLDOWN,777);p.getPersistentData().putInt(StarVoiceState.COOLDOWN,88);p.getPersistentData().putInt(StarVoiceState.INTERVAL,7);
        var clone=new TestPlayer(level);com.thelongtravail.boundless.BoundlessEvents.clone(new net.minecraftforge.event.entity.player.PlayerEvent.Clone(clone,p,true));
        check(clone.getPersistentData().getInt(DreamState.COOLDOWN)==777&&clone.getPersistentData().getInt(StarVoiceState.COOLDOWN)==88&&clone.getPersistentData().getInt(StarVoiceState.INTERVAL)==7,"death clone preserves all boundless cooldowns");
        var dependencyPlayer=new TestPlayer(level);equip(dependencyPlayer,true);
        var depInv=CuriosApi.getCuriosInventory(dependencyPlayer).resolve().orElseThrow();depInv.getStacksHandler("travel_diary").orElseThrow().getStacks().setStackInSlot(0,ItemStack.EMPTY);
        check(!BoundlessEquipment.equipped(dependencyPlayer,false),"missing diary immediately disables star voice");
        com.thelongtravail.helper.DependentAccessories.reconcile(dependencyPlayer);
        check(depInv.getStacksHandler("necklace").orElseThrow().getStacks().getStackInSlot(0).isEmpty()&&dependencyPlayer.getInventory().contains(new ItemStack(ModRegistry.STAR_VOICE.get())),"missing diary returns star voice to inventory");
        var end=level.getServer().getLevel(net.minecraft.world.level.Level.END);var endCaster=new TestPlayer(end);endCaster.setPos(0,80,0);
        check(TimeStopManager.get(level).start(p,false,now,new Vec3(0,80,0),32,40,Set.of(p.getUUID())),"overworld field for dimension check");
        check(!TimeStopManager.frozen(endCaster),"same coordinates in another dimension remain normal");
        check(TimeStopManager.get(end).start(endCaster,false,now,endCaster.position(),32,40,Set.of(endCaster.getUUID())),"another dimension has independent field capacity");
        TimeStopManager.get(end).expire(Long.MAX_VALUE);reset(level);
        System.out.println("BOUNDLESS_ADDITIONAL_EDGES_PASS: exact dream anchor, hazards, direct source, self projectile, group cooldown, death clone, diary removal");
        equip(p,true);
        System.out.println("DREAM_SAFE_LANDING_PASS: grounded, slight hover, safe short fall, void and solid block");
        p.getPersistentData().putInt(DreamState.COOLDOWN,0);DreamState.cast(p);p.setGameMode(GameType.CREATIVE);DreamState.finish(p);check(p.gameMode.getGameModeForPlayer()==GameType.CREATIVE,"external game mode respected");
        BoundlessItemsConfig.NUMBERS.get("star.witnessChance").set(.05);BoundlessItemsConfig.NUMBERS.get("star.maliceChance").set(.1);
        caster=p;frozen=cow(level,new Vec3(0,81,0));outside=cow(level,new Vec3(80,81,0));frozen.invulnerableTime=20;frozen.setDeltaMovement(.1,.1,0);originalAge=frozen.tickCount;originalPosition=frozen.position();
        frozen.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.MOVEMENT_SPEED,100));
        BlockPos button=new BlockPos(2,80,0);level.setBlock(button,Blocks.STONE_BUTTON.defaultBlockState().setValue(net.minecraft.world.level.block.ButtonBlock.POWERED,true),3);
        BlockPos left=new BlockPos(-33,80,0),right=new BlockPos(-32,80,0),piston=new BlockPos(-33,80,1);level.setBlock(left,Blocks.CHEST.defaultBlockState(),3);level.setBlock(right,Blocks.CHEST.defaultBlockState(),3);
        var chestA=(net.minecraft.world.level.block.entity.ChestBlockEntity)level.getBlockEntity(left);var chestB=(net.minecraft.world.level.block.entity.ChestBlockEntity)level.getBlockEntity(right);
        level.setBlock(piston,Blocks.PISTON.defaultBlockState().setValue(net.minecraft.world.level.block.piston.PistonBaseBlock.FACING,Direction.EAST),3);
        for(BlockPos at:List.of(new BlockPos(6,80,0),new BlockPos(80,80,1))){
            // 复用隔离测试世界时，须清除上次熔炉的燃烧和烧炼计时。
            level.removeBlock(at,false);
            level.setBlock(at,Blocks.FURNACE.defaultBlockState(),3);
            var furnace=(net.minecraft.world.level.block.entity.FurnaceBlockEntity)level.getBlockEntity(at);
            furnace.setItem(0,new ItemStack(Items.RAW_IRON));furnace.setItem(1,new ItemStack(Items.COAL));
            var initial=furnace.saveWithoutMetadata();
            check(initial.getShort("CookTime")==0&&initial.getShort("BurnTime")==0,"fresh furnace counters before time stop");
        }
        check(TimeStopManager.get(level).start(p,false,level.getGameTime(),new Vec3(0,80,0),32,45,Set.of(p.getUUID())),"real tick field");
        level.scheduleTick(button,Blocks.STONE_BUTTON,5);check(level.getBlockTicks().hasScheduledTick(button,Blocks.STONE_BUTTON),"held scheduled task remains discoverable");
        var savedTasks=FrozenBlockTasks.get(level).save(new net.minecraft.nbt.CompoundTag());
        var loadTasks=FrozenBlockTasks.class.getDeclaredMethod("load",net.minecraft.nbt.CompoundTag.class);loadTasks.setAccessible(true);
        var restoredTasks=(FrozenBlockTasks)loadTasks.invoke(null,savedTasks.copy());
        check(restoredTasks.held(button,Blocks.STONE_BUTTON,false)&&restoredTasks.save(new net.minecraft.nbt.CompoundTag()).equals(savedTasks),"held tasks persist type, remaining delay, priority, order and updates through NBT reload");
        check(!level.setBlock(button,Blocks.DIRT.defaultBlockState(),3),"frozen block mutation blocked");
        var access=net.minecraft.world.inventory.ContainerLevelAccess.create(level,button);
        check(FrozenContainers.frozen(new net.minecraft.world.inventory.CraftingMenu(90,p.getInventory(),access)),"crafting table menu origin frozen");
        check(FrozenContainers.frozen(new net.minecraft.world.inventory.EnchantmentMenu(91,p.getInventory(),access)),"enchanting menu origin frozen");
        var drop=new net.minecraft.world.entity.item.ItemEntity(level,0,81,0,new ItemStack(Items.DIAMOND,3));drop.setNoPickUpDelay();level.addFreshEntity(drop);drop.playerTouch(p);check(drop.isAlive()&&drop.getItem().getCount()==3,"exempt caster cannot pick frozen drops");
        check(!net.minecraft.world.level.block.entity.HopperBlockEntity.addItem(chestA,drop)&&drop.isAlive()&&chestA.isEmpty(),"outside hopper cannot vacuum frozen item");
        var mergable=net.minecraft.world.entity.item.ItemEntity.class.getDeclaredMethod("isMergable");mergable.setAccessible(true);check(!(boolean)mergable.invoke(drop),"frozen item cannot merge with outside stack");
        var orb=new net.minecraft.world.entity.ExperienceOrb(level,0,81,0,5);level.addFreshEntity(orb);var merge=net.minecraft.world.entity.ExperienceOrb.class.getDeclaredMethod("canMerge",net.minecraft.world.entity.ExperienceOrb.class,int.class,int.class);merge.setAccessible(true);check(!(boolean)merge.invoke(null,orb,orb.getId(),5),"frozen XP cannot merge into outside orb");
        var award=net.minecraft.world.entity.ExperienceOrb.class.getDeclaredMethod("tryMergeToExisting",ServerLevel.class,Vec3.class,int.class);award.setAccessible(true);check(!(boolean)award.invoke(null,level,new Vec3(0,81,0),5),"new frozen XP cannot merge outside before spawning");
        p.setItemSlot(EquipmentSlot.MAINHAND,new ItemStack(Items.WHEAT,3));p.connection.handleInteract(net.minecraft.network.protocol.game.ServerboundInteractPacket.createInteractionPacket(frozen,false,net.minecraft.world.InteractionHand.MAIN_HAND));check(!frozen.isInLove(),"exempt caster cannot feed frozen creature");p.setItemSlot(EquipmentSlot.MAINHAND,ItemStack.EMPTY);
        check(FrozenContainers.frozen(new net.minecraft.world.CompoundContainer(chestA,chestB)),"double chest crossing boundary protected");ItemStack item=new ItemStack(Items.DIAMOND,3);
        check(net.minecraft.world.level.block.entity.HopperBlockEntity.addItem(chestA,chestB,item,Direction.EAST).getCount()==3&&chestB.isEmpty(),"hopper does not transfer or consume at boundary");
        var state=level.getBlockState(piston);check(!state.triggerEvent(level,piston,0,Direction.EAST.get3DDataValue())&&!level.getBlockState(piston).getValue(net.minecraft.world.level.block.piston.PistonBaseBlock.EXTENDED)&&level.getBlockState(piston.east()).isAir(),"piston protected atomically before head changes");
        var incoming=new Arrow(EntityType.ARROW,level);incoming.setPos(-50,80,0);incoming.setDeltaMovement(100,0,0);level.addFreshEntity(incoming);incoming.tick();check(incoming.getX()<-31.9&&incoming.getX()>-32.1&&TimeStopManager.frozen(incoming),"fast projectile freezes at boundary");check(incoming.getDeltaMovement().x>90,"projectile pending velocity retained");
        var explosion=new net.minecraft.world.level.Explosion(level,null,2,80,0,4,false,net.minecraft.world.level.Explosion.BlockInteraction.DESTROY);explosion.getToBlow().add(button);explosion.finalizeExplosion(false);check(level.getBlockState(button).is(Blocks.STONE_BUTTON),"explosion cannot drop or destroy frozen block");
        System.out.println("BOUNDLESS_SYNCHRONOUS_PASS");
        firstGameTime=level.getGameTime();firstDayTime=level.getDayTime();
        level.setWeatherParameters(0,1000,true,false);firstRainTime=((net.minecraft.world.level.storage.ServerLevelData)level.getLevelData()).getRainTime();
    }catch(Throwable t){t.printStackTrace();finish(e.getServer(),false);}}
    private void tick(TickEvent.ServerTickEvent e){if(e.phase!=TickEvent.Phase.END||frozen==null)return;try{
        ticks++;
        if(ticks==25){check(level.getGameTime()>firstGameTime&&level.getDayTime()>firstDayTime,"dimension game time and daylight continue during local freeze");check(frozen.tickCount==originalAge&&frozen.position().equals(originalPosition),"real frozen entity age and position");check(outside.tickCount>originalAge,"outside entity continues");check(frozen.invulnerableTime==0,"invulnerability naturally counts down while frozen");check(frozen.getEffect(net.minecraft.world.effect.MobEffects.MOVEMENT_SPEED).getDuration()==100,"effect duration paused");check(level.getBlockState(new BlockPos(2,80,0)).getValue(net.minecraft.world.level.block.ButtonBlock.POWERED),"scheduled button task paused");float before=frozen.getHealth();frozen.hurt(level.damageSources().playerAttack(caster),3);check(frozen.getHealth()<before,"damage resolves immediately during freeze");hurtHealth=frozen.getHealth();}
        if(ticks==26){frozen.hurt(level.damageSources().playerAttack(caster),3);check(frozen.getHealth()==hurtHealth,"normal invulnerability rejects immediate equal followup hit");}
        if(ticks==25){
            check(level.isRaining()&&((net.minecraft.world.level.storage.ServerLevelData)level.getLevelData()).getRainTime()<firstRainTime,"dimension weather cycle continues during local freeze");
            var heldFurnace=level.getBlockEntity(new BlockPos(6,80,0)).saveWithoutMetadata();
            var runningFurnace=level.getBlockEntity(new BlockPos(80,80,1)).saveWithoutMetadata();
            check(heldFurnace.getShort("CookTime")==0&&heldFurnace.getShort("BurnTime")==0,"furnace inside sphere does not consume fuel or cook: "+heldFurnace);
            check(runningFurnace.getShort("CookTime")>0&&runningFurnace.getShort("BurnTime")>0,"furnace outside sphere keeps cooking");
        }
        if(ticks==60){check(frozen.tickCount>originalAge,"entity resumes after freeze");check(!level.getBlockState(new BlockPos(2,80,0)).getValue(net.minecraft.world.level.block.ButtonBlock.POWERED),"scheduled task resumes after original remaining delay");System.out.println("BOUNDLESS_REAL_TICK_PASS");
            check(level.getBlockEntity(new BlockPos(6,80,0)).saveWithoutMetadata().getShort("CookTime")>0,"frozen furnace resumes after field expiry");
            dreamPlayer=new TestPlayer(level);equip(dreamPlayer,true);dreamPlayer.setPos(.5,80,.5);DreamState.cast(dreamPlayer);
            registeredPlayers(e.getServer()).add(dreamPlayer);
        }
        if(ticks==259)check(DreamState.active(dreamPlayer)&&dreamPlayer.isSpectator(),"automatic dream remains active through tick 199");
        if(ticks==260){check(!DreamState.active(dreamPlayer)&&!dreamPlayer.isSpectator(),"registered server player automatically wakes at tick 200");check(dreamPlayer.getPersistentData().getInt(DreamState.COOLDOWN)==1000,"automatic wake keeps remaining player cooldown");System.out.println("DREAM_AUTOMATIC_SERVER_TICK_PASS");finish(e.getServer(),true);}
    }catch(Throwable t){t.printStackTrace();finish(e.getServer(),false);}}
    private void finish(net.minecraft.server.MinecraftServer server,boolean pass){frozen=null;try{if(dreamPlayer!=null){registeredPlayers(server).remove(dreamPlayer);DreamState.finish(dreamPlayer);}java.nio.file.Files.writeString(java.nio.file.Path.of("boundless-result.txt"),pass?"PASS":"FAIL");}catch(Exception e){throw new RuntimeException(e);}finally{server.halt(false);}}
}
