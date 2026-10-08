package travail.smoke;

import com.mojang.authlib.GameProfile;
import com.thelongtravail.TravailAspect;
import com.thelongtravail.config.*;
import com.thelongtravail.data.LongTravailData;
import com.thelongtravail.registry.ModRegistry;
import com.thelongtravail.underworld.*;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.*;
import net.minecraft.world.effect.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.entity.monster.Spider;
import net.minecraft.world.item.*;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import top.theillusivec4.curios.api.*;
import top.theillusivec4.curios.api.type.capability.ICurio.DropRule;
import top.theillusivec4.curios.common.inventory.CurioStacksHandler;
import java.util.*;

@Mod("travail_smoke")
public final class UnderworldSmoke {
    public static LivingEntity earlyRescue;
    public UnderworldSmoke(){MinecraftForge.EVENT_BUS.register(this);}
    private static void check(boolean value,String message){if(!value)throw new AssertionError(message);}
    private static class TestPlayer extends ServerPlayer {
        TestPlayer(ServerLevel level){
            super(level.getServer(),level,new GameProfile(UUID.randomUUID(),"UnderworldTest"));
            var wire=new net.minecraft.network.Connection(net.minecraft.network.protocol.PacketFlow.SERVERBOUND){
                @Override public void send(net.minecraft.network.protocol.Packet<?> packet){}
            };
            connection=new net.minecraft.server.network.ServerGamePacketListenerImpl(level.getServer(),wire,this){
                @Override public void send(net.minecraft.network.protocol.Packet<?> packet){}
            };
            setPos(.5,90,.5);((SpawnProtectionAccess)this).travail$protection(0);
        }
    }
    private static ItemStack equip(ServerPlayer p) {
        var inv=CuriosApi.getCuriosInventory(p).resolve().orElseThrow();
        Map<String,top.theillusivec4.curios.api.type.inventory.ICurioStacksHandler> slots=new HashMap<>();
        for(String id:List.of("travel_diary","ring","charm"))slots.put(id,new CurioStacksHandler(inv,id,2,true,false,true,DropRule.DEFAULT));
        inv.setCurios(slots);
        ItemStack diary=new ItemStack(ModRegistry.LONG_TRAVAIL.get());LongTravailData.initialize(diary,p);
        CompoundTag root=diary.getOrCreateTag().getCompound("LongTravail");
        root.putInt("Witnesses",63&~TravailAspect.UNDERWORLD.mask());root.remove("ForcedMalices");root.remove("ForcedWitnesses");
        check(root.hasUUID("JourneyId"),"initialized journey");
        inv.getStacksHandler("travel_diary").orElseThrow().getStacks().setStackInSlot(0,diary);
        inv.getStacksHandler("ring").orElseThrow().getStacks().setStackInSlot(0,new ItemStack(ModRegistry.THOUSAND_YEARS.get()));
        inv.getStacksHandler("charm").orElseThrow().getStacks().setStackInSlot(0,new ItemStack(ModRegistry.BOOK_OF_DEAD.get()));
        return diary;
    }
    private static void effects(ServerPlayer p){for(var effect:List.of(MobEffects.WITHER,MobEffects.POISON,MobEffects.WEAKNESS,MobEffects.MOVEMENT_SLOWDOWN))p.addEffect(new MobEffectInstance(effect,400));}
    public static final class Rescue {
        ServerPlayer watched;
        @SubscribeEvent public void death(LivingDeathEvent e){if(e.getEntity()==watched){e.setCanceled(true);watched.setHealth(1);}}
    }
    @SubscribeEvent public void run(ServerStartedEvent e){
        boolean pass=false;var server=e.getServer();var level=server.overworld();
        try {
            UnderworldStorage.afterLoads(server);
            String[][] lootCases={{"ancient_city","a_thousand_years_later","0.03"},{"nether_bridge","a_thousand_years_later","0.02"},{"desert_pyramid","book_of_the_dead","0.02"},{"stronghold_library","book_of_the_dead","0.03"}};
            var lootJson=net.minecraft.world.level.storage.loot.Deserializers.createLootTableSerializer().create();
            for(String[] c:lootCases){
                var table=server.getLootData().getLootTable(new net.minecraft.resources.ResourceLocation("minecraft","chests/"+c[0]));
                var pool=table.getPool("the_long_travail:shared_items");check(pool!=null,"loaded table has accessory pool: "+c[0]);
                var json=lootJson.toJsonTree(pool).getAsJsonObject();
                check(json.get("rolls").getAsInt()==1,"one independent roll");
                int weightSum=0,itemWeight=0;
                for(var raw:json.getAsJsonArray("entries")){var item=raw.getAsJsonObject();int weight=item.has("weight")?item.get("weight").getAsInt():1;weightSum+=weight;if(item.get("name").getAsString().equals("the_long_travail:"+c[1]))itemWeight=weight;}
                double probability=json.getAsJsonArray("conditions").get(0).getAsJsonObject().get("chance").getAsDouble()*itemWeight/weightSum;
                check(itemWeight>0 && Math.abs(probability-Double.parseDouble(c[2]))<1E-6,"shared pool preserves item chance");
                com.thelongtravail.loot.UnifiedLoot.inject(table);check(table.getPool("the_long_travail:shared_items")==pool,"duplicate injection is ignored");
            }
            var untouched=server.getLootData().getLootTable(new net.minecraft.resources.ResourceLocation("minecraft","chests/ancient_city_ice_box"));
            check(untouched.getPool("the_long_travail:shared_items")==null&&untouched.getPool("the_long_travail:shared_items")==null,"unselected table untouched");
            System.out.println("UNDERWORLD_LOOT_TABLES_PASS");
            TravailConfig.UNDERWORLD_DAMAGE_REDUCTION.set(0D);
            level.getGameRules().getRule(GameRules.RULE_KEEPINVENTORY).set(true,server);
            TestPlayer attacker=new TestPlayer(level);ItemStack diary=equip(attacker);
            Cow cow=new Cow(EntityType.COW,level);cow.setPos(2,90,0);cow.addEffect(new MobEffectInstance(MobEffects.WITHER,400,2));
            cow.hurt(level.damageSources().playerAttack(attacker),4);
            check(Math.abs(cow.getHealth()-4.8)<.001,"wither III adds 30% once: "+cow.getHealth());
            CuriosApi.getCuriosInventory(attacker).resolve().orElseThrow().getStacksHandler("ring").orElseThrow().getStacks().setStackInSlot(1,new ItemStack(ModRegistry.THOUSAND_YEARS.get()));
            check(Math.abs(UnderworldItems.bonus(cow,level.damageSources().playerAttack(attacker))-.3)<.001,"forced duplicates do not stack");
            LongTravailData.setWitness(diary,TravailAspect.UNDERWORLD,true);
            Spider spider=new Spider(EntityType.SPIDER,level);spider.setPos(1,90,0);
            check(UnderworldItems.attackType(attacker,spider)==MobType.UNDEAD,"replace arthropod");
            Enchantments.BANE_OF_ARTHROPODS.doPostAttack(attacker,spider,5);
            check(!spider.hasEffect(MobEffects.MOVEMENT_SLOWDOWN),"bane slowing suppressed");
            ItemStack sword=new ItemStack(Items.STICK);sword.enchant(Enchantments.SMITE,2);sword.enchant(Enchantments.BANE_OF_ARTHROPODS,5);
            attacker.setItemSlot(EquipmentSlot.MAINHAND,sword);var ticker=LivingEntity.class.getDeclaredField("attackStrengthTicker");ticker.setAccessible(true);ticker.setInt(attacker,40);attacker.attack(spider);
            check(Math.abs(spider.getHealth()-10)<.001,"smite II replaces bane V on actual spider attack: "+spider.getHealth());
            TestPlayer dying=new TestPlayer(level);ItemStack dyingDiary=equip(dying);effects(dying);
            dying.hurt(level.damageSources().generic(),30);
            var ledger=UnderworldLedger.get(server);var death=ledger.deaths.get(dying.getUUID());
            check(!dying.isAlive()&&death!=null&&death.getBoolean("Ready"),"real death creates choice");
            check(LongTravailData.hasWitness(dyingDiary,TravailAspect.UNDERWORLD),"witness on committed overkill");
            TestPlayer equal=new TestPlayer(level);ItemStack equalDiary=equip(equal);effects(equal);equal.hurt(level.damageSources().generic(),20);
            check(!LongTravailData.hasWitness(equalDiary,TravailAspect.UNDERWORLD),"strict greater than max");
            TestPlayer absorbed=new TestPlayer(level);ItemStack absorbedDiary=equip(absorbed);effects(absorbed);absorbed.setAbsorptionAmount(20);absorbed.hurt(level.damageSources().generic(),40);
            check(!LongTravailData.hasWitness(absorbedDiary,TravailAspect.UNDERWORLD),"compare after absorption");
            TestPlayer rescued=new TestPlayer(level);equip(rescued);effects(rescued);Rescue rescue=new Rescue();rescue.watched=rescued;MinecraftForge.EVENT_BUS.register(rescue);
            rescued.hurt(level.damageSources().generic(),30);MinecraftForge.EVENT_BUS.unregister(rescue);
            check(rescued.isAlive()&&!ledger.deaths.containsKey(rescued.getUUID()),"cancelled death grants nothing");
            TestPlayer early=new TestPlayer(level);ItemStack earlyDiary=equip(early);effects(early);earlyRescue=early;
            early.hurt(level.damageSources().generic(),100);
            check(earlyRescue==null&&early.isAlive()&&early.getHealth()==early.getMaxHealth(),"early cancellation restores life before normal death event");
            check(!ledger.deaths.containsKey(early.getUUID()),"early-cancelled death cannot grant revival choice");
            check(!LongTravailData.hasWitness(earlyDiary,TravailAspect.UNDERWORLD),"early-cancelled overkill cannot grant witness");
            System.out.println("UNDERWORLD_EARLY_DEATH_INTERCEPTION_PASS");
            TestPlayer killed=new TestPlayer(level);equip(killed);killed.hurt(level.damageSources().genericKill(),10000000);
            check(!ledger.deaths.containsKey(killed.getUUID()),"threshold inclusive");
            TestPlayer smallKill=new TestPlayer(level);equip(smallKill);smallKill.hurt(level.damageSources().genericKill(),30);
            check(ledger.deaths.containsKey(smallKill.getUUID()),"small kill allowed");
            TestPlayer below=new TestPlayer(level);equip(below);below.setPos(0,level.getMinBuildHeight()-.01,0);below.hurt(level.damageSources().generic(),30);
            check(!ledger.deaths.containsKey(below.getUUID()),"position excludes non-void damage below world");
            ItemStack book=new ItemStack(ModRegistry.BOOK_OF_DEAD.get());UUID identity=UnderworldLedger.identity(book);CompoundTag serialized=book.save(new CompoundTag());ledger.consume(identity);
            check(ItemStack.of(serialized).isEmpty(),"serialized spent book cannot restore");
            CompoundTag crystal=new CompoundTag();crystal.put("storedStack0",serialized.copy());ledger.reconcileTag(crystal,0);
            check(crystal.getCompound("storedStack0").getByte("Count")==0,"nested storage crystal cleanup");
            UnderworldLedger restored=UnderworldLedger.load(ledger.save(new CompoundTag()));check(restored.spent(identity),"consumption persists");
            for(int x=-3;x<=3;x++)for(int y=89;y<=93;y++)for(int z=-3;z<=3;z++)level.setBlock(new BlockPos(x,y,z),Blocks.AIR.defaultBlockState(),3);
            Vec3 origin=new Vec3(.5,90,.5);level.setBlock(new BlockPos(0,90,0),Blocks.STONE.defaultBlockState(),3);
            RevivalPosition search=new RevivalPosition(level,origin,.6,1.8);int steps=0;while(!search.step(32)&&steps++<100){}
            check(search.result()!=null&&search.result().z<0&&Math.abs(search.result().x-.5)<1E-5,"north wins equal distance: "+search.result());
            check(search.compare(origin.add(1,0,0),origin.add(0,0,1))<0,"east before south");
            check(search.compare(origin.add(-1,0,0),origin.add(0,1,0))<0,"west before up");
            level.setBlock(new BlockPos(0,90,0),Blocks.AIR.defaultBlockState(),3);
            // 实际死亡后调用同一套复活提交流程，通过模拟网络连接避免依赖客户端。
            BlockPos bound=new BlockPos(10,90,10);dying.setRespawnPosition(level.dimension(),bound,25,true,false);
            var method=BookRevival.class.getDeclaredMethod("revive",ServerPlayer.class,CompoundTag.class,Vec3.class);method.setAccessible(true);
            method.invoke(null,dying,death,origin);
            ServerPlayer fresh=dying.connection.player;
            check(fresh!=dying&&fresh.isAlive()&&fresh.position().distanceToSqr(origin)<.001,"real PlayerList respawn at death position");
            check(bound.equals(fresh.getRespawnPosition()),"bound spawn preserved");
            var field=ServerPlayer.class.getDeclaredField("spawnInvulnerableTime");field.setAccessible(true);check(field.getInt(fresh)==60,"vanilla 60 tick protection");
            check(!ledger.deaths.containsKey(fresh.getUUID()),"revival ticket consumed");
            // 通过实际掉落流程验证：关闭 keepInventory 时不保留或返还书籍。
            level.getGameRules().getRule(GameRules.RULE_KEEPINVENTORY).set(false,server);
            TestPlayer dropped=new TestPlayer(level);equip(dropped);dropped.setPos(8.5,90,8.5);
            level.setBlock(new BlockPos(8,89,8),Blocks.STONE.defaultBlockState(),3);
            ItemStack droppedBook=UnderworldItems.equipped(dropped,false);UUID droppedId=UnderworldLedger.identity(droppedBook);
            dropped.hurt(level.damageSources().generic(),30);
            check(UnderworldItems.equipped(dropped,false).isEmpty(),"book follows ordinary Curios drops");
            CompoundTag droppedDeath=ledger.deaths.get(dropped.getUUID());check(droppedDeath!=null,"drop does not remove death-time qualification");
            droppedDeath.putBoolean("Hardcore",true);
            method.invoke(null,dropped,droppedDeath,new Vec3(8.5,90,8.5));
            check(ledger.spent(droppedId),"hardcore revival consumes dropped identity");
            check(UnderworldItems.equipped(dropped.connection.player,false).isEmpty(),"no fabricated book return");
            if(net.minecraftforge.fml.ModList.get().isLoaded("gravestone")){
                check(!ledger.graves.isEmpty(),"actual grave captured through optional mixin");
                for(String key:ledger.graves){int sep=key.lastIndexOf('|');if(!key.substring(0,sep).equals(level.dimension().location().toString()))continue;
                    var be=level.getBlockEntity(BlockPos.of(Long.parseLong(key.substring(sep+1))));if(be==null)continue;
                    Object grave=be.getClass().getMethod("getDeath").invoke(be);
                    for(Object raw:(Iterable<?>)grave.getClass().getMethod("getAllItems").invoke(grave))check(!ledger.consumed((ItemStack)raw),"consumed book removed from loaded grave");
                }
            }
            if(net.minecraftforge.fml.ModList.get().isLoaded("enigmaticlegacy")){
                Class<?> items=Class.forName("com.aizistral.enigmaticlegacy.registries.EnigmaticItems");
                TestPlayer cube=new TestPlayer(level);equip(cube);
                CuriosApi.getCuriosInventory(cube).resolve().orElseThrow().getStacksHandler("ring").orElseThrow().getStacks().setStackInSlot(1,new ItemStack((Item)items.getField("THE_CUBE").get(null)));
                cube.hurt(level.damageSources().generic(),30);
                check(cube.isAlive()&&!ledger.deaths.containsKey(cube.getUUID()),"actual Enigmatic cube rescue precedes book");
                Object storage=items.getField("STORAGE_CRYSTAL").get(null);
                ItemStack nestedBook=new ItemStack(ModRegistry.BOOK_OF_DEAD.get());UUID nestedId=UnderworldLedger.identity(nestedBook);
                var drops=new ArrayList<net.minecraft.world.entity.item.ItemEntity>();drops.add(new net.minecraft.world.entity.item.ItemEntity(level,0,90,0,nestedBook));
                ItemStack actualCrystal=(ItemStack)storage.getClass().getMethod("storeDropsOnCrystal",Collection.class,net.minecraft.world.entity.player.Player.class,ItemStack.class).invoke(storage,drops,cube,null);
                ledger.consume(nestedId);
                storage.getClass().getMethod("retrieveDropsFromCrystal",ItemStack.class,net.minecraft.world.entity.player.Player.class,ItemStack.class).invoke(storage,actualCrystal,cube,null);
                for(int i=0;i<cube.getInventory().getContainerSize();i++)check(!ledger.consumed(cube.getInventory().getItem(i)),"actual storage crystal does not restore consumed book");
                System.out.println("UNDERWORLD_OPTIONAL_COMPAT_PASS");
            }
            System.out.println("UNDERWORLD_SMOKE_PASS");pass=true;
        } catch(Throwable t){t.printStackTrace();}
        finally {try{java.nio.file.Files.writeString(java.nio.file.Path.of("underworld-result.txt"),pass?"PASS":"FAIL");}catch(Exception ex){throw new RuntimeException(ex);}server.halt(false);}
    }
}
