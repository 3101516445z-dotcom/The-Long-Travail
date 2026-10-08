package travail.smoke;

import com.mojang.authlib.GameProfile;
import com.thelongtravail.TravailAspect;
import com.thelongtravail.config.*;
import com.thelongtravail.data.LongTravailData;
import com.thelongtravail.farreach.*;
import com.thelongtravail.helper.*;
import com.thelongtravail.registry.ModRegistry;
import net.minecraft.core.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.*;
import net.minecraft.world.*;
import net.minecraft.world.damagesource.*;
import net.minecraft.world.effect.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.item.enchantment.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.storage.loot.*;
import net.minecraft.world.level.storage.loot.parameters.*;
import net.minecraft.world.phys.*;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import top.theillusivec4.curios.api.*;
import top.theillusivec4.curios.api.type.capability.ICurio.DropRule;
import top.theillusivec4.curios.common.inventory.CurioStacksHandler;
import java.util.*;

@Mod("travail_smoke")
public final class FarReachSmoke {
    private static void check(boolean ok,String message){if(!ok)throw new AssertionError(message);}
    private static void near(double a,double b,String message){check(Math.abs(a-b)<.002,message+" actual="+a+" expected="+b);}
    private static final class P extends ServerPlayer {
        P(ServerLevel l){super(l.getServer(),l,new GameProfile(UUID.randomUUID(),"FarTest"));
            var wire=new net.minecraft.network.Connection(net.minecraft.network.protocol.PacketFlow.SERVERBOUND){@Override public void send(net.minecraft.network.protocol.Packet<?> p){}};
            connection=new net.minecraft.server.network.ServerGamePacketListenerImpl(l.getServer(),wire,this){@Override public void send(net.minecraft.network.protocol.Packet<?> p){}};
            setPos(0,200,0);
        }
    }
    private static final class WaterItem extends ItemEntity {
        boolean wet=true;
        WaterItem(ServerLevel l,int count){super(l,0,200,0,new ItemStack(Items.GOLD_BLOCK,count));setNoGravity(true);}
        @Override public boolean isInWater(){return wet;}
    }
    public static final class Reject {
        @SubscribeEvent public void join(EntityJoinLevelEvent e){if(e.getEntity() instanceof ItemEntity item&&!item.getItem().is(Items.GOLD_BLOCK))e.setCanceled(true);}
    }
    public static final class Reenter {
        final ItemEntity input;
        Reenter(ItemEntity input){this.input=input;}
        @SubscribeEvent public void join(EntityJoinLevelEvent e){if(e.getEntity() instanceof ItemEntity output&&output!=input)GoldenAgeActions.tick(input);}
    }
    public static final class LateDeny {
        @SubscribeEvent(priority=net.minecraftforge.eventbus.api.EventPriority.LOWEST,receiveCanceled=true)
        public void click(PlayerInteractEvent.RightClickBlock event){event.setCanceled(true);event.setCancellationResult(InteractionResult.FAIL);}
    }
    public static final class CrossReenter {
        final ItemEntity other;boolean fired;
        CrossReenter(ItemEntity other){this.other=other;}
        @SubscribeEvent public void join(EntityJoinLevelEvent event){if(!fired&&event.getEntity() instanceof ItemEntity output&&!output.getItem().is(Items.GOLD_BLOCK)){fired=true;GoldenAgeActions.tick(other);}}
    }
    private static ItemStack equip(P p){
        var inv=CuriosApi.getCuriosInventory(p).resolve().orElseThrow();var slots=new HashMap<String,top.theillusivec4.curios.api.type.inventory.ICurioStacksHandler>();
        for(String s:List.of("travel_diary","charm","back"))slots.put(s,new CurioStacksHandler(inv,s,2,true,false,true,DropRule.DEFAULT));inv.setCurios(slots);
        ItemStack diary=new ItemStack(ModRegistry.LONG_TRAVAIL.get());LongTravailData.initialize(diary,p);for(TravailAspect a:TravailAspect.values())LongTravailData.setWitness(diary,a,true);
        slot(p,"travel_diary",0,diary);slot(p,"charm",0,new ItemStack(ModRegistry.GOLDEN_AGE.get()));slot(p,"back",0,new ItemStack(ModRegistry.ICARUS.get()));return diary;
    }
    private static void slot(P p,String s,int i,ItemStack stack){CuriosApi.getCuriosInventory(p).resolve().orElseThrow().getStacksHandler(s).orElseThrow().getStacks().setStackInSlot(i,stack);}
    private static void healthy(P p)throws Exception{var f=ServerPlayer.class.getDeclaredField("spawnInvulnerableTime");f.setAccessible(true);f.setInt(p,0);p.invulnerableTime=0;p.setHealth(20);p.setAbsorptionAmount(0);p.removeAllEffects();}
    private static int items(P p){int count=0;for(ItemStack s:p.getInventory().items)if(s.is(Items.FLINT)||s.is(Items.GOLD_NUGGET))count+=s.getCount();return count;}
    private static PlayerInteractEvent.RightClickBlock click(P p,InteractionHand hand){return new PlayerInteractEvent.RightClickBlock(p,hand,new BlockPos(0,199,0),new BlockHitResult(new Vec3(0,200,0),Direction.UP,new BlockPos(0,199,0),false));}
    private static PlayerInteractEvent.RightClickBlock use(P p,InteractionHand hand){var event=click(p,hand);return net.minecraftforge.common.ForgeHooks.onRightClickBlock(p,hand,event.getPos(),event.getHitVec());}
    private static List<ItemEntity> drops(ServerLevel l){return l.getEntitiesOfClass(ItemEntity.class,new AABB(-10,190,-10,10,210,10));}
    private static void clearDrops(ServerLevel l){drops(l).forEach(Entity::discard);l.getServer().getWorldData().overworldData().setGameTime(l.getGameTime()+1);GoldenAgeActions.reload();}
    private static void waterTicks(WaterItem e,int ticks){for(int i=0;i<ticks;i++)GoldenAgeActions.tick(e);}
    public FarReachSmoke(){MinecraftForge.EVENT_BUS.addListener(this::run);}
    private void run(ServerStartedEvent event){boolean pass=false;try{
        TravailConfig.SPECS.values().forEach(spec->spec.setConfig(com.electronwill.nightconfig.core.CommentedConfig.inMemory()));
        ServerLevel level=event.getServer().overworld();level.getChunk(0,0);level.setDayTime(6000);level.setWeatherParameters(100000,0,false,false);
        P p=new P(level);level.addNewPlayer(p);ItemStack diary=equip(p);healthy(p);
        // 只重置上次测试使用的位置，避免残留屋顶影响日照测试。
        level.setBlockAndUpdate(new BlockPos(0,205,0),Blocks.AIR.defaultBlockState());
        flight(level,p);flightCrossAudit(level,p);growthDetached(level,p);ignition(level,p);equipmentAndDig(level,p,diary);combat(level,p,diary);pools(p);water(level,p,diary);dimensions(event.getServer());growth(level);recipes(level);loot(level);
        integration(level,p,diary);auditEdges(level,p,diary);CombatRulesSmoke.run(level);
        var snapshot=com.thelongtravail.network.TravailNetwork.class.getDeclaredMethod("tooltipSnapshot");snapshot.setAccessible(true);check(snapshot.invoke(null)!=null,"tooltip sync budget");
        System.out.println("FAR_REACH_SMOKE_PASS");pass=true;
    }catch(Throwable error){error.printStackTrace();}finally{try{java.nio.file.Files.writeString(java.nio.file.Path.of("farreach-result.txt"),pass?"PASS":"FAIL");}catch(Exception e){throw new RuntimeException(e);}event.getServer().halt(false);}}
    private static void flight(ServerLevel level, P p) throws Exception {
        var update=LivingEntity.class.getDeclaredMethod("updateFallFlying");update.setAccessible(true);
        slot(p,"travel_diary",0,ItemStack.EMPTY);
        p.setItemSlot(EquipmentSlot.CHEST,new ItemStack(Items.DIAMOND_CHESTPLATE));p.setOnGround(false);
        check(p.tryToStartFallFlying(),"Icarus starts gliding with chest armor and no diary");
        update.invoke(p);check(p.isFallFlying(),"Icarus sustains gliding");
        var rocket=new ItemStack(Items.FIREWORK_ROCKET,2);p.setItemInHand(InteractionHand.MAIN_HAND,rocket);
        var result=Items.FIREWORK_ROCKET.use(level,p,InteractionHand.MAIN_HAND);
        check(result.getResult().consumesAction()&&rocket.getCount()==1,"vanilla firework propulsion accepts Icarus gliding");
        for(int i=0;i<40;i++)update.invoke(p);
        check(p.getItemBySlot(EquipmentSlot.CHEST).getDamageValue()==0,"chest armor not damaged by flight");
        p.stopFallFlying();p.setOnGround(true);check(!p.tryToStartFallFlying(),"ground restriction retained");p.setOnGround(false);
        p.addEffect(new MobEffectInstance(MobEffects.LEVITATION,100));check(!p.tryToStartFallFlying(),"levitation restriction retained");p.removeAllEffects();
        check(p.tryToStartFallFlying(),"can resume after levitation");
        slot(p,"back",0,ItemStack.EMPTY);update.invoke(p);check(!p.isFallFlying(),"removal stops flight without alternative");
        check(!p.tryToStartFallFlying(),"chest armor alone cannot glide");
        var back=CuriosApi.getCuriosInventory(p).resolve().orElseThrow().getStacksHandler("back").orElseThrow();
        back.getCosmeticStacks().setStackInSlot(0,new ItemStack(ModRegistry.ICARUS.get()));
        check(!p.tryToStartFallFlying(),"cosmetic slot cannot grant flight");back.getCosmeticStacks().setStackInSlot(0,ItemStack.EMPTY);
        p.setItemSlot(EquipmentSlot.CHEST,new ItemStack(Items.ELYTRA));check(p.tryToStartFallFlying(),"vanilla chest elytra still works");update.invoke(p);check(p.isFallFlying(),"vanilla chest fallback sustained");
        p.stopFallFlying();p.setItemSlot(EquipmentSlot.CHEST,ItemStack.EMPTY);slot(p,"back",0,new ItemStack(ModRegistry.ICARUS.get()));
        System.out.println("ICARUS_FLIGHT_PASS");
    }
    private static void flightCrossAudit(ServerLevel level,P p) throws Exception {
        var update=LivingEntity.class.getDeclaredMethod("updateFallFlying");update.setAccessible(true);
        var ticks=LivingEntity.class.getDeclaredField("fallFlyTicks");ticks.setAccessible(true);
        p.stopFallFlying();p.setOnGround(false);p.removeAllEffects();
        var elytra=new ItemStack(Items.ELYTRA);elytra.setDamageValue(100);p.setItemSlot(EquipmentSlot.CHEST,elytra);
        slot(p,"back",0,new ItemStack(ModRegistry.ICARUS.get()));
        check(p.tryToStartFallFlying(),"Icarus plus elytra starts");
        ticks.setInt(p,19);update.invoke(p);check(elytra.getDamageValue()==100,"Icarus suppresses vanilla elytra durability at due tick");
        slot(p,"back",0,ItemStack.EMPTY);ticks.setInt(p,19);update.invoke(p);
        check(p.isFallFlying()&&elytra.getDamageValue()==101,"removal hands over mid-flight to chest elytra and resumes wear");
        slot(p,"back",0,new ItemStack(ModRegistry.ICARUS.get()));ticks.setInt(p,39);update.invoke(p);
        check(p.isFallFlying()&&elytra.getDamageValue()==101,"equipping mid-flight suspends wear");
        elytra.setDamageValue(elytra.getMaxDamage()-1);p.stopFallFlying();
        check(p.tryToStartFallFlying(),"broken chest elytra does not block Icarus");update.invoke(p);check(p.isFallFlying(),"broken chest with Icarus sustains");
        slot(p,"back",0,ItemStack.EMPTY);update.invoke(p);check(!p.isFallFlying(),"removal with broken chest stops");
        check(!p.tryToStartFallFlying(),"broken elytra alone denied");
        elytra.setDamageValue(elytra.getMaxDamage()-2);check(p.tryToStartFallFlying(),"one usable durability can start");
        ticks.setInt(p,19);update.invoke(p);check(elytra.getDamageValue()==elytra.getMaxDamage()-1,"last usable durability consumed");
        update.invoke(p);
        System.out.println("ICARUS_LAST_DURABILITY continued="+p.isFallFlying());
        p.stopFallFlying();slot(p,"back",0,new ItemStack(ModRegistry.ICARUS.get()));p.setItemSlot(EquipmentSlot.CHEST,ItemStack.EMPTY);
        check(p.tryToStartFallFlying(),"restart empty chest");p.addEffect(new MobEffectInstance(MobEffects.LEVITATION,100));update.invoke(p);check(!p.isFallFlying(),"levitation interrupts existing Icarus flight");p.removeAllEffects();
        check(p.tryToStartFallFlying(),"restart before landing");p.setOnGround(true);update.invoke(p);check(!p.isFallFlying(),"landing interrupts existing Icarus flight");p.setOnGround(false);
        var attr=net.minecraftforge.registries.ForgeRegistries.ATTRIBUTES.getValue(new net.minecraft.resources.ResourceLocation("caelus","fall_flying"));
        if(attr!=null){
            var instance=p.getAttribute(attr);check(instance!=null,"Caelus attribute attached");
            var apiClass=Class.forName("top.theillusivec4.caelus.api.CaelusApi");var api=apiClass.getMethod("getInstance").invoke(null);
            var apiState=apiClass.getMethod("canFallFly",LivingEntity.class);
            check(apiState.invoke(api,p).toString().equals("ALLOW"),"Caelus API recognizes Icarus source");
            check((boolean)apiClass.getMethod("canFly",LivingEntity.class).invoke(api,p),"Caelus canFly recognizes Icarus");
            slot(p,"back",0,ItemStack.EMPTY);
            check(apiState.invoke(api,p).toString().equals("DEFAULT"),"removal immediately clears optional source");
            slot(p,"back",0,new ItemStack(ModRegistry.ICARUS.get()));
            System.out.println("ICARUS_CAELUS_API_ICARUS_ONLY="+apiState.invoke(api,p));
            var deny=new net.minecraft.world.entity.ai.attributes.AttributeModifier(java.util.UUID.randomUUID(),"audit deny",-1,net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADDITION);
            instance.addTransientModifier(deny);boolean started=p.tryToStartFallFlying();update.invoke(p);
            check(!started&&!p.isFallFlying(),"Caelus DENY blocks initial Icarus start");
            p.startFallFlying();update.invoke(p);check(!p.isFallFlying(),"Caelus DENY cancels existing flight");
            p.setItemSlot(EquipmentSlot.CHEST,new ItemStack(Items.ELYTRA));
            check(!p.tryToStartFallFlying(),"Icarus cannot use chest fallback to bypass DENY");
            p.setItemSlot(EquipmentSlot.CHEST,ItemStack.EMPTY);
            System.out.println("ICARUS_CAELUS_DENY start="+started+" sustained="+p.isFallFlying());instance.removeModifier(deny);
            check(p.tryToStartFallFlying(),"removing DENY restores flight");update.invoke(p);check(p.isFallFlying(),"restored flight sustained");
            var other=new net.minecraft.world.entity.ai.attributes.AttributeModifier(java.util.UUID.randomUUID(),"other source",1,net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADDITION);
            instance.addTransientModifier(other);slot(p,"back",0,ItemStack.EMPTY);update.invoke(p);
            check(p.isFallFlying(),"other Caelus source survives Icarus removal");instance.removeModifier(other);update.invoke(p);
            check(!p.isFallFlying(),"no stale Icarus source remains");slot(p,"back",0,new ItemStack(ModRegistry.ICARUS.get()));
        }
        p.stopFallFlying();p.setItemSlot(EquipmentSlot.CHEST,ItemStack.EMPTY);ticks.setInt(p,0);
        System.out.println("ICARUS_CROSS_AUDIT_PASS");
    }
    private static void growthDetached(ServerLevel level,P p) throws Exception {
        BlockPos crop=p.blockPosition().offset(2,0,2);level.setBlockAndUpdate(crop.below(),Blocks.FARMLAND.defaultBlockState());level.setBlockAndUpdate(crop,Blocks.WHEAT.defaultBlockState());
        var cache=IcarusGrowth.class.getDeclaredField("LAST");cache.setAccessible(true);
        var worlds=(java.util.Map<ServerLevel,java.util.Map<Long,Long>>)cache.get(null);worlds.clear();
        long time=level.getGameTime();
        for(int i=0;i<20;i++){
            level.getServer().getWorldData().overworldData().setGameTime(time+i);
            MinecraftForge.EVENT_BUS.post(new net.minecraftforge.event.TickEvent.LevelTickEvent(net.minecraftforge.fml.LogicalSide.SERVER,net.minecraftforge.event.TickEvent.Phase.END,level,()->true));
        }
        check(worlds.isEmpty(),"equipped Icarus does not invoke reserved growth on world ticks");
        check(level.getBlockState(crop).getValue(CropBlock.AGE)==0,"Icarus does not advance crop");
        check(IcarusGrowth.grow(level,crop,time+20),"reserved growth remains callable");
        level.setBlockAndUpdate(crop,Blocks.AIR.defaultBlockState());
        System.out.println("ICARUS_GROWTH_DETACHED_PASS");
    }
    public static final class CancelIgnitionHit {
        LivingEntity target;
        @SubscribeEvent public void hit(net.minecraftforge.event.entity.living.LivingDamageEvent e){if(e.getEntity()==target)e.setCanceled(true);}
    }
    private static void ignition(ServerLevel level,P p) throws Exception {
        slot(p,"travel_diary",0,ItemStack.EMPTY);slot(p,"back",0,new ItemStack(ModRegistry.ICARUS.get()));
        Cow cow=new Cow(EntityType.COW,level);cow.setPos(1,200,0);level.addFreshEntity(cow);
        cow.hurt(level.damageSources().playerAttack(p),1);check(cow.getRemainingFireTicks()==200,"melee ignites ten seconds without diary");
        cow.setRemainingFireTicks(500);cow.invulnerableTime=0;cow.hurt(level.damageSources().playerAttack(p),1);check(cow.getRemainingFireTicks()==500,"longer fire preserved");
        cow.clearFire();cow.invulnerableTime=0;cow.setAbsorptionAmount(4);float health=cow.getHealth();
        cow.hurt(level.damageSources().playerAttack(p),1);check(cow.getHealth()==health&&cow.getRemainingFireTicks()==200,"absorption-only hit ignites");
        cow.clearFire();cow.invulnerableTime=0;cow.setAbsorptionAmount(0);
        cow.hurt(level.damageSources().arrow(new net.minecraft.world.entity.projectile.Arrow(level,p),p),1);check(cow.getRemainingFireTicks()==200,"attributed arrow ignites");
        cow.clearFire();cow.invulnerableTime=0;var cancel=new CancelIgnitionHit();cancel.target=cow;MinecraftForge.EVENT_BUS.register(cancel);
        try{cow.hurt(level.damageSources().playerAttack(p),1);check(cow.getRemainingFireTicks()<=0,"cancelled hit does not ignite");}finally{MinecraftForge.EVENT_BUS.unregister(cancel);}
        cow.invulnerableTime=0;cow.hurt(level.damageSources().playerAttack(p),0);check(cow.getRemainingFireTicks()<=0,"zero hit does not ignite");
        cow.setRemainingFireTicks(50);cow.invulnerableTime=0;cow.hurt(level.damageSources().onFire(),1);check(cow.getRemainingFireTicks()==50,"burn ticks do not refresh themselves");
        cow.clearFire();cow.invulnerableTime=0;slot(p,"back",0,ItemStack.EMPTY);cow.hurt(level.damageSources().playerAttack(p),1);check(cow.getRemainingFireTicks()<=0,"unequipped does not ignite");
        slot(p,"back",0,new ItemStack(ModRegistry.ICARUS.get()));
        var blaze=new net.minecraft.world.entity.monster.Blaze(EntityType.BLAZE,level);blaze.setPos(2,200,0);level.addFreshEntity(blaze);blaze.hurt(level.damageSources().playerAttack(p),1);check(blaze.getRemainingFireTicks()<=0,"fire immunity retained");blaze.discard();cow.discard();
        Cow configured=new Cow(EntityType.COW,level);configured.setPos(3,200,0);level.addFreshEntity(configured);
        int original=FarReachItemsConfig.IGNITE_SECONDS.get();
        try {
            FarReachItemsConfig.IGNITE_SECONDS.set(3);
            configured.hurt(level.damageSources().playerAttack(p),1);check(configured.getRemainingFireTicks()==60,"configured ignition duration");
            var snapshot=com.thelongtravail.network.TooltipConfigSync.class.getDeclaredMethod("serverValues");snapshot.setAccessible(true);
            check(((java.util.Map<?,?>)snapshot.invoke(null)).get("farItem.icarus.igniteSeconds").equals(3D),"ignition tooltip sync uses server value");
            FarReachItemsConfig.IGNITE_SECONDS.set(0);configured.clearFire();configured.invulnerableTime=0;
            configured.hurt(level.damageSources().playerAttack(p),1);check(configured.getRemainingFireTicks()<=0,"zero disables ignition");
            for(var entry:TravailConfig.SPECS.entrySet()) {
                var values=com.electronwill.nightconfig.core.CommentedConfig.inMemory();entry.getValue().correct(values);
                check(values.contains("loot")==entry.getKey().equals("general.toml"),"unified loot only in general config: "+entry.getKey());
                if(entry.getKey().equals("general.toml"))check(values.contains("loot.enabled")&&values.contains("loot.tables"),"unified loot config retains enable and table bindings");
            }
        } finally {FarReachItemsConfig.IGNITE_SECONDS.set(original);configured.discard();}
        System.out.println("ICARUS_IGNITION_PASS");
    }
    private static void equipmentAndDig(ServerLevel l,P p,ItemStack diary){
        p.getInventory().selected=8;
        var item=(top.theillusivec4.curios.api.type.capability.ICurioItem)ModRegistry.ICARUS.get();
        check(!item.canEquip(new SlotContext("back",p,1,false,true),new ItemStack(ModRegistry.ICARUS.get())),"second equipped copy refused");
        check(!item.canEquip(new SlotContext("back",p,0,true,true),new ItemStack(ModRegistry.ICARUS.get())),"cosmetic refused");
        slot(p,"travel_diary",0,ItemStack.EMPTY);check(FarReachEquipment.equipped(p,false)&&FarReachCombat.immune(p,p.damageSources().fall()),"base independent of diary");near(FarReachCombat.bonus(p),0,"no diary bonus");slot(p,"travel_diary",0,diary);
        l.setBlockAndUpdate(new BlockPos(0,199,0),Blocks.SAND.defaultBlockState());
        int before=items(p);use(p,InteractionHand.MAIN_HAND);check(items(p)==before+1,"dig yields one");
        use(p,InteractionHand.MAIN_HAND);use(p,InteractionHand.OFF_HAND);check(items(p)==before+1,"cooldown and offhand");
        l.getServer().getWorldData().overworldData().setGameTime(l.getGameTime()+2);use(p,InteractionHand.MAIN_HAND);check(items(p)==before+2&&l.getBlockState(new BlockPos(0,199,0)).is(Blocks.SAND),"two tick cooldown and block preserved");
        var cancelled=click(p,InteractionHand.MAIN_HAND);cancelled.setCanceled(true);MinecraftForge.EVENT_BUS.post(cancelled);check(items(p)==before+2,"cancelled interaction ignored");
        System.out.println("FAR_EQUIPMENT_DIG_PASS");
    }
    private static void combat(ServerLevel l,P p,ItemStack diary)throws Exception{
        near(IcarusEnvironment.sunlight(0),0,"dawn");near(IcarusEnvironment.sunlight(6000),1,"noon");near(IcarusEnvironment.sunlight(18000),0,"night");
        LongTravailData.setWitness(diary,TravailAspect.FAR_REACH,false);near(FarReachCombat.bonus(p),1,"sunny noon +100%");
        slot(p,"back",1,new ItemStack(ModRegistry.ICARUS.get()));near(FarReachCombat.bonus(p),1,"forced copies do not stack");slot(p,"back",1,ItemStack.EMPTY);
        l.setWeatherParameters(0,1000,true,false);near(FarReachCombat.bonus(p),.5,"rain noon +50%");l.setWeatherParameters(100000,0,false,false);
        Cow cow=new Cow(EntityType.COW,l);cow.setPos(0,200,1);cow.setHealth(10);l.addFreshEntity(cow);cow.hurt(l.damageSources().playerAttack(p),2);near(cow.getHealth(),6,"actual outgoing double damage");cow.discard();
        healthy(p);p.hurt(p.damageSources().fall(),5);near(p.getHealth(),20,"actual fall immunity");check(p.getActiveEffects().isEmpty(),"immune fall skips malice feedback");
        LongTravailData.setWitness(diary,TravailAspect.FAR_REACH,true);healthy(p);p.hurt(p.damageSources().magic(),5);near(p.getHealth(),20,"actual magic immunity");
        check(!FarReachCombat.immune(p,p.damageSources().wither()),"wither not automatic");
        FarReachItemsConfig.EXTRA_IMMUNITIES.set(List.of("minecraft:wither"));check(FarReachCombat.immune(p,p.damageSources().wither()),"extra id");
        FarReachItemsConfig.EXTRA_IMMUNITIES.set(List.of("#minecraft:is_fire"));check(FarReachCombat.immune(p,p.damageSources().inFire()),"extra tag");FarReachItemsConfig.EXTRA_IMMUNITIES.set(List.of());
        l.setDayTime(18000);near(FarReachCombat.bonus(p),.5,"witness nighttime");l.setDayTime(6000);
        System.out.println("FAR_COMBAT_PASS");
    }
    private static void pools(P p){
        check(GoldenAgePools.enchantments().contains(Enchantments.BINDING_CURSE)&&GoldenAgePools.enchantments().contains(Enchantments.MENDING),"curses and treasures allowed");
        FarReachItemsConfig.ENCHANTMENT_BLACKLIST.set(List.of("minecraft:mending"));check(!GoldenAgePools.enchantments().contains(Enchantments.MENDING),"enchantment blacklist");FarReachItemsConfig.ENCHANTMENT_BLACKLIST.set(List.of());
        check(GoldenAgePools.nutritious(new FoodProperties.Builder().nutrition(4).saturationMod(.75F).build()),"saturation boundary accepted");
        check(!GoldenAgePools.nutritious(new FoodProperties.Builder().nutrition(4).saturationMod(.7F).build()),"saturation boundary rejected");
        var harmful=new FoodProperties.Builder().nutrition(6).saturationMod(1).effect(()->new MobEffectInstance(MobEffects.POISON,20),1).build();
        check(!GoldenAgePools.nutritious(harmful),"harmful denied");FarReachItemsConfig.ALLOW_HARMFUL.set(true);check(GoldenAgePools.nutritious(harmful),"harmful allowed");FarReachItemsConfig.ALLOW_HARMFUL.set(false);
        FarReachItemsConfig.FOOD_BLACKLIST.set(List.of("minecraft:cooked_beef"));check(GoldenAgePools.foods(p).stream().noneMatch(s->s.is(Items.COOKED_BEEF)),"food blacklist");FarReachItemsConfig.FOOD_BLACKLIST.set(List.of());
        System.out.println("FAR_POOLS_PASS");
    }
    private static void water(ServerLevel l,P p,ItemStack diary){
        clearDrops(l);LongTravailData.setWitness(diary,TravailAspect.FAR_REACH,false);
        WaterItem e=new WaterItem(l,64);l.addFreshEntity(e);GoldenAgeActions.register(e,p);waterTicks(e,9);check(e.getItem().getCount()==64,"nine ticks no conversion");
        e.wet=false;waterTicks(e,1);e.wet=true;waterTicks(e,9);check(e.getItem().getCount()==64,"leave water resets");waterTicks(e,1);check(e.getItem().getCount()==56,"ten ticks player budget eight");waterTicks(e,5);check(e.getItem().getCount()==56,"same tick budget cannot repeat");
        WaterItem stranger=new WaterItem(l,1);l.addFreshEntity(stranger);waterTicks(stranger,20);check(stranger.isAlive()&&stranger.getItem().getCount()==1,"unregistered ignored");
        clearDrops(l);FarReachItemsConfig.NUMBERS.get("golden_age.batchSize").set(64.0);e=new WaterItem(l,64);l.addFreshEntity(e);GoldenAgeActions.register(e,p);waterTicks(e,10);
        int books=0,lapis=0;for(ItemEntity result:drops(l)){if(result.getItem().is(Items.ENCHANTED_BOOK)){books++;var tags=EnchantedBookItem.getEnchantments(result.getItem());check(tags.size()==1&&tags.getCompound(0).getShort("lvl")==1,"single level one enchantment");}if(result.getItem().is(Items.LAPIS_BLOCK))lapis+=result.getItem().getCount();}
        check(books>0&&lapis>0&&books+lapis==64&&!e.isAlive(),"64 independent results conserved");FarReachItemsConfig.NUMBERS.get("golden_age.batchSize").set(8.0);
        clearDrops(l);e=new WaterItem(l,1);l.addFreshEntity(e);GoldenAgeActions.register(e,p);var reject=new Reject();MinecraftForge.EVENT_BUS.register(reject);try{waterTicks(e,10);check(e.getItem().getCount()==1,"cancelled spawn preserves gold");}finally{MinecraftForge.EVENT_BUS.unregister(reject);}
        clearDrops(l);e=new WaterItem(l,1);l.addFreshEntity(e);GoldenAgeActions.register(e,p);var nested=new Reenter(e);MinecraftForge.EVENT_BUS.register(nested);try{waterTicks(e,10);check(!e.isAlive()&&drops(l).stream().filter(r->r!=nested.input).mapToInt(r->r.getItem().getCount()).sum()==1,"spawn callback reentry cannot duplicate gold");}finally{MinecraftForge.EVENT_BUS.unregister(nested);}
        clearDrops(l);LongTravailData.setWitness(diary,TravailAspect.FAR_REACH,true);FarReachItemsConfig.NUMBERS.get("golden_age.minNutrition").set(1000.0);e=new WaterItem(l,1);l.addFreshEntity(e);GoldenAgeActions.register(e,p);waterTicks(e,10);check(e.getItem().getCount()==1,"empty food pool preserves gold");FarReachItemsConfig.NUMBERS.get("golden_age.minNutrition").set(4.0);
        clearDrops(l);p.getInventory().setItem(p.getInventory().selected,new ItemStack(Items.GOLD_BLOCK,2));check(p.drop(true),"manual Q toss");check(drops(l).stream().anyMatch(GoldenAgeActions::pending),"manual drop mixin registers");
        ItemEntity program=p.drop(new ItemStack(Items.GOLD_BLOCK),false);check(program!=null&&!GoldenAgeActions.pending(program),"programmatic drop not registered");
        clearDrops(l);p.inventoryMenu.setCarried(new ItemStack(Items.GOLD_BLOCK));p.inventoryMenu.clicked(-999,0,ClickType.PICKUP,p);check(drops(l).stream().anyMatch(GoldenAgeActions::pending),"inventory throw registered");clearDrops(l);
        System.out.println("FAR_WATER_MANUAL_PASS");
    }
    private static void dimensions(net.minecraft.server.MinecraftServer server){
        for(var key:List.of(Level.END,Level.NETHER)){
            ServerLevel l=Objects.requireNonNull(server.getLevel(key));l.getChunk(0,0);P p=new P(l);ItemStack diary=equip(p);LongTravailData.setWitness(diary,TravailAspect.FAR_REACH,false);l.setDayTime(6000);
            check(IcarusEnvironment.openSky(p),"open dimension "+key);near(FarReachCombat.bonus(p),key==Level.END?1:.5,"dimension daytime");
            l.setBlockAndUpdate(new BlockPos(0,205,0),Blocks.GLASS.defaultBlockState());check(IcarusEnvironment.openSky(p),"glass transparent");l.setBlockAndUpdate(new BlockPos(0,205,0),Blocks.STONE.defaultBlockState());check(!IcarusEnvironment.openSky(p),"roof blocks");
            LongTravailData.setWitness(diary,TravailAspect.FAR_REACH,true);near(FarReachCombat.bonus(p),0,"roof witness blocked");l.setBlockAndUpdate(new BlockPos(0,205,0),Blocks.AIR.defaultBlockState());near(FarReachCombat.bonus(p),.5,"open witness");
            if(key==Level.NETHER){
                p.setPos(0,64,0);
                for(int y=65;y<l.getMaxBuildHeight();y++)l.setBlockAndUpdate(new BlockPos(0,y,0),Blocks.AIR.defaultBlockState());
                check(IcarusEnvironment.openSky(p),"nether below bedrock height with open column");
                near(FarReachCombat.bonus(p),.5,"low nether open witness bonus");
                BlockPos roof=new BlockPos(0,127,0);
                l.setBlockAndUpdate(roof,Blocks.BEDROCK.defaultBlockState());
                check(!IcarusEnvironment.openSky(p),"actual bedrock roof blocks sky");
                near(FarReachCombat.bonus(p),0,"actual bedrock roof blocks witness bonus");
                l.setBlockAndUpdate(roof,Blocks.GLASS.defaultBlockState());
                check(IcarusEnvironment.openSky(p),"low nether glass roof remains transparent");
                l.setBlockAndUpdate(roof,Blocks.AIR.defaultBlockState());
            }
        }
        System.out.println("FAR_DIMENSIONS_PASS");
    }
    private static void growth(ServerLevel l){
        for(int x=-2;x<=2;x++)for(int z=-2;z<=2;z++)l.getChunk(x,z);
        BlockPos pos=new BlockPos(3,200,3);l.setBlockAndUpdate(pos.below(),Blocks.FARMLAND.defaultBlockState());l.setBlockAndUpdate(pos,Blocks.WHEAT.defaultBlockState());
        check(IcarusGrowth.grow(l,pos,1000),"crop attempt");check(!IcarusGrowth.grow(l,pos,1000),"overlap dedup");check(IcarusGrowth.grow(l,pos,1020),"next interval");
        // 先清除上次成功测试留下的树冠，再检查自然生长。
        for(int x=2;x<=14;x++)for(int z=2;z<=14;z++)for(int y=200;y<240;y++)l.setBlock(new BlockPos(x,y,z),Blocks.AIR.defaultBlockState(),2);
        BlockPos tree=new BlockPos(8,240,8);for(int x=2;x<=14;x++)for(int z=2;z<=14;z++)for(int y=240;y<280;y++)l.setBlock(new BlockPos(x,y,z),Blocks.AIR.defaultBlockState(),2);
        var lighting=l.getChunkSource().getLightEngine();var lit=lighting.lightChunk(l.getChunkAt(tree),false);
        l.getServer().managedBlock(()->{lighting.tryScheduleUpdate();return lit.isDone();});lit.join();
        l.setBlockAndUpdate(tree.below(),Blocks.DIRT.defaultBlockState());l.setBlockAndUpdate(tree,Blocks.OAK_SAPLING.defaultBlockState());check(IcarusGrowth.eligible(l.getBlockState(tree)),"sapling covered");
        for(int i=0;i<300&&l.getBlockState(tree).is(Blocks.OAK_SAPLING);i++)IcarusGrowth.grow(l,tree,2000+i*20L);
        check(l.getBlockState(tree).is(Blocks.OAK_LOG),"natural tree generated");check(!IcarusGrowth.eligible(Blocks.GRASS_BLOCK.defaultBlockState()),"grass not included");
        System.out.println("FAR_GROWTH_PASS");
    }
    private static void recipes(ServerLevel l){
        CraftingContainer grid=new TransientCraftingContainer(new AbstractContainerMenu(null,0){@Override public ItemStack quickMoveStack(net.minecraft.world.entity.player.Player p,int i){return ItemStack.EMPTY;}@Override public boolean stillValid(net.minecraft.world.entity.player.Player p){return true;}},3,3);
        Item[][] recipes={{Items.GOLD_BLOCK,Items.NETHERITE_INGOT,Items.GOLD_BLOCK,Items.LAPIS_LAZULI,Items.NETHER_STAR,Items.LAPIS_LAZULI,Items.GOLD_BLOCK,Items.NETHERITE_INGOT,Items.GOLD_BLOCK},{Items.GOLD_INGOT,Items.NETHER_STAR,Items.GOLD_INGOT,Items.FEATHER,Items.ELYTRA,Items.FEATHER,Items.BLAZE_POWDER,Items.AIR,Items.BLAZE_POWDER}};
        for(int r=0;r<2;r++){for(int i=0;i<9;i++)grid.setItem(i,new ItemStack(recipes[r][i]));ItemStack result=l.getRecipeManager().getRecipeFor(RecipeType.CRAFTING,grid,l).orElseThrow().assemble(grid,l.registryAccess());check(result.is(r==0?ModRegistry.GOLDEN_AGE.get():ModRegistry.ICARUS.get()),"recipe "+r);}
        System.out.println("FAR_RECIPES_PASS");
    }
    private static void loot(ServerLevel l){
        for(String rule:new ArrayList<String>(){{addAll(travail.smoke.LegacyLootBaseline.GOLD);addAll(travail.smoke.LegacyLootBaseline.ICARUS);}}){
            String[] parts=rule.split("\\|");LootTable table=l.getServer().getLootData().getLootTable(new ResourceLocation(parts[0]));
            Item expected=parts[0].contains("end_city")||parts[0].contains("desert")?ModRegistry.ICARUS.get():ModRegistry.GOLDEN_AGE.get();int hits=0;
            for(int i=0;i<1000;i++){var params=new LootParams.Builder(l).withParameter(LootContextParams.ORIGIN,new Vec3(0,200,0)).create(LootContextParamSets.CHEST);int n=table.getRandomItems(params,i+987L).stream().filter(s->s.is(expected)).mapToInt(ItemStack::getCount).sum();check(n<=1,"one loot result");hits+=n;}
            double expectedHits=1000*Double.parseDouble(parts[1]);check(Math.abs(hits-expectedHits)<25,"loot probability "+parts[0]+" hits="+hits);System.out.println("FAR_LOOT "+parts[0]+" "+hits+"/1000");
        }
        System.out.println("FAR_LOOT_PASS");
    }
    private static void integration(ServerLevel l,P p,ItemStack diary)throws Exception{
        // 实际受伤、伤害分担和嵌套受伤的调用链须复用同一份攻击加成，且只应用一次。
        l.setDayTime(6000);l.setWeatherParameters(100000,0,false,false);LongTravailData.setWitness(diary,TravailAspect.FAR_REACH,false);
        P victim=new P(l);l.addNewPlayer(victim);equip(victim);healthy(victim);victim.setPos(2,200,0);
        var inv=CuriosApi.getCuriosInventory(victim).resolve().orElseThrow();var map=new HashMap<>(inv.getCurios());map.put("body",new CurioStacksHandler(inv,"body",1,true,false,true,DropRule.DEFAULT));inv.setCurios(map);
        var wolf=new net.minecraft.world.entity.animal.Wolf(EntityType.WOLF,l);wolf.setPos(2,200,1);wolf.tame(victim);wolf.setHealth(wolf.getMaxHealth());l.addFreshEntity(wolf);
        ItemStack affection=new ItemStack(ModRegistry.AFFECTION.get());affection.getOrCreateTag().putUUID(com.thelongtravail.flourishing.Affection.TARGET,wolf.getUUID());affection.getOrCreateTag().putUUID(com.thelongtravail.flourishing.Affection.OWNER,victim.getUUID());slot(victim,"body",0,affection);
        float before=wolf.getHealth();victim.hurt(l.damageSources().playerAttack(p),8);near(victim.getHealth(),12,"shared player bonus once");near(wolf.getHealth(),before-4,"shared companion bonus once");
        victim.discard();wolf.discard();
        // 通过实际的 ItemEntity.tick 注入验证行为，并覆盖无法合并的输入物品。
        clearDrops(l);for(int x=-1;x<=1;x++)for(int z=-1;z<=1;z++){l.setBlockAndUpdate(new BlockPos(x,199,z),Blocks.STONE.defaultBlockState());l.setBlockAndUpdate(new BlockPos(x,200,z),Blocks.WATER.defaultBlockState());}
        ItemEntity gold=new ItemEntity(l,.5,200,.5,new ItemStack(Items.GOLD_BLOCK,16));gold.setNoGravity(true);gold.setDeltaMovement(Vec3.ZERO);l.addFreshEntity(gold);GoldenAgeActions.register(gold,p);
        ItemEntity plain=new ItemEntity(l,.5,200,.5,new ItemStack(Items.GOLD_BLOCK,1));plain.setNoGravity(true);plain.setDeltaMovement(Vec3.ZERO);l.addFreshEntity(plain);
        for(int i=0;i<9;i++)gold.tick();check(gold.getItem().getCount()==16,"real tick nine water ticks");gold.tick();check(gold.getItem().getCount()==8&&plain.getItem().getCount()==1,"real tick converts ten, no merge with plain gold count="+gold.getItem().getCount()+" plain="+plain.getItem().getCount()+" data="+gold.getPersistentData()+" wet="+gold.isInWater()+" pos="+gold.position());
        var saved=new net.minecraft.nbt.CompoundTag();gold.save(saved);ItemEntity restored=new ItemEntity(l,0,200,0,new ItemStack(Items.GOLD_BLOCK));restored.load(saved);check(GoldenAgeActions.pending(restored)&&!restored.getItem().hasTag(),"entity-only ownership persists without item NBT");
        clearDrops(l);for(int x=-1;x<=1;x++)for(int z=-1;z<=1;z++)l.setBlockAndUpdate(new BlockPos(x,200,z),Blocks.AIR.defaultBlockState());
        // 未装备饰品或距离过远时暂停转换，其他玩家的饰品不影响判定。
        WaterItem pending=new WaterItem(l,1);l.addFreshEntity(pending);GoldenAgeActions.register(pending,p);slot(p,"charm",0,ItemStack.EMPTY);waterTicks(pending,10);check(pending.getItem().getCount()==1,"unequipped owner does not convert");slot(p,"charm",0,new ItemStack(ModRegistry.GOLDEN_AGE.get()));p.setPos(20,200,0);waterTicks(pending,1);check(pending.getItem().getCount()==1,"distant owner does not convert");p.setPos(0,200,0);
        // 直接调用任一被动效果入口也不能绕过时停。
        P caster=new P(l);caster.setPos(0,200,0);var manager=com.thelongtravail.boundless.TimeStopManager.get(l);
        check(manager.start(caster,false,l.getGameTime(),caster.position(),8,40,Set.of(caster.getUUID())),"test time stop starts");
        try{check(com.thelongtravail.boundless.TimeStopManager.frozen(pending),"pending item frozen");waterTicks(pending,20);check(pending.getItem().getCount()==1,"frozen item does not convert");BlockPos crop=new BlockPos(3,200,3);l.setBlockAndUpdate(crop,Blocks.WHEAT.defaultBlockState());check(!IcarusGrowth.grow(l,crop,50000),"frozen crop not accelerated");}
        finally{com.thelongtravail.boundless.TimeStopManager.clear();}
        clearDrops(l);
        LongTravailData.setWitness(diary,TravailAspect.FAR_REACH,true);healthy(p);FarReachItemsConfig.EXTRA_IMMUNITIES.set(List.of("minecraft:generic_kill"));p.hurt(p.damageSources().genericKill(),100);near(p.getHealth(),20,"configured non-magic immunity respected");FarReachItemsConfig.EXTRA_IMMUNITIES.set(List.of());
        System.out.println("FAR_SHARING_REAL_TICK_TIMESTOP_PASS");
    }
    private static void auditEdges(ServerLevel l,P p,ItemStack diary){
        List<String> failures=new ArrayList<>();
        p.getInventory().selected=8;p.getInventory().setItem(8,ItemStack.EMPTY);
        l.setBlockAndUpdate(new BlockPos(0,199,0),Blocks.SAND.defaultBlockState());
        l.getServer().getWorldData().overworldData().setGameTime(l.getGameTime()+2);
        int before=items(p);var deny=new LateDeny();MinecraftForge.EVENT_BUS.register(deny);
        try{var result=net.minecraftforge.common.ForgeHooks.onRightClickBlock(p,InteractionHand.MAIN_HAND,new BlockPos(0,199,0),new BlockHitResult(new Vec3(0,200,0),Direction.UP,new BlockPos(0,199,0),false));
            int gained=items(p)-before;System.out.println("FAR_AUDIT_LATE_DENY_GAINED="+gained);if(gained!=0||result.getCancellationResult()!=InteractionResult.FAIL)failures.add("late protection cancellation grants reward");
        }finally{MinecraftForge.EVENT_BUS.unregister(deny);}
        clearDrops(l);LongTravailData.setWitness(diary,TravailAspect.FAR_REACH,false);
        WaterItem first=new WaterItem(l,16),second=new WaterItem(l,16);l.addFreshEntity(first);l.addFreshEntity(second);GoldenAgeActions.register(first,p);GoldenAgeActions.register(second,p);waterTicks(first,9);waterTicks(second,9);
        var nested=new CrossReenter(second);MinecraftForge.EVENT_BUS.register(nested);
        try{waterTicks(first,1);int converted=32-first.getItem().getCount()-second.getItem().getCount();System.out.println("FAR_AUDIT_CROSS_ENTITY_CONVERTED="+converted);if(converted!=8)failures.add("cross-entity reentry violates player budget");
            GoldenAgeActions.reload();waterTicks(second,1);int afterReload=32-first.getItem().getCount()-second.getItem().getCount();System.out.println("FAR_AUDIT_RELOAD_CONVERTED="+afterReload);if(afterReload!=8)failures.add("reload resets same-tick player budget");}
        finally{MinecraftForge.EVENT_BUS.unregister(nested);clearDrops(l);}
        check(failures.isEmpty(),String.join("; ",failures));System.out.println("FAR_AUDIT_EDGES_PASS");
    }
}
