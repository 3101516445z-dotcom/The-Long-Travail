package travail.smoke;

import com.mojang.authlib.GameProfile;
import com.thelongtravail.TravailAspect;
import com.thelongtravail.config.*;
import com.thelongtravail.data.LongTravailData;
import com.thelongtravail.abyss.*;
import com.thelongtravail.registry.ModRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.*;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.*;
import net.minecraft.world.level.storage.ServerLevelData;
import net.minecraft.world.level.storage.loot.*;
import net.minecraft.world.level.storage.loot.parameters.*;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.common.inventory.CurioStacksHandler;
import top.theillusivec4.curios.api.type.capability.ICurio.DropRule;
import java.util.*;

public final class RainItemsSmoke {
    private static LootTable reloadLoot(ServerLevel level, String id, boolean worldDatapack) throws java.io.IOException {
        var key = new ResourceLocation(id);
        var resource = new ResourceLocation(key.getNamespace(), "loot_tables/" + key.getPath() + ".json");
        try (var reader = level.getServer().getResourceManager().getResourceOrThrow(resource).openAsReader()) {
            return net.minecraftforge.common.ForgeHooks.loadLootTable(Deserializers.createLootTableSerializer().create(),
                    key, com.google.gson.JsonParser.parseReader(reader), worldDatapack);
        }
    }
    private static final class TestPlayer extends FakePlayer {
        TestPlayer(ServerLevel level,String name) {
            super(level,new GameProfile(UUID.randomUUID(),name));
            var wire=new net.minecraft.network.Connection(net.minecraft.network.protocol.PacketFlow.SERVERBOUND) {
                @Override public void send(net.minecraft.network.protocol.Packet<?> p) {}
            };
            connection=new net.minecraft.server.network.ServerGamePacketListenerImpl(level.getServer(),wire,this) {
                @Override public void send(net.minecraft.network.protocol.Packet<?> p) {}
            };
        }
        @Override public boolean isInvulnerableTo(DamageSource source) { return false; }
    }
    private static void check(boolean value,String description) { if(!value) throw new AssertionError("RAIN: "+description); }
    private static void near(float a,float b,String description) { check(Math.abs(a-b)<.001,description+" actual="+a+" expected="+b); }
    private static final class Cap {
        Cow target; boolean cancel;
        @SubscribeEvent public void cap(LivingDamageEvent e) {
            if(e.getEntity()==target) { if(cancel)e.setCanceled(true); else e.setAmount(Math.min(2,e.getAmount())); }
        }
    }
    private static void reset(TestPlayer p) throws Exception {
        var f=ServerPlayer.class.getDeclaredField("spawnInvulnerableTime");f.setAccessible(true);f.setInt(p,0);
        p.invulnerableTime=0;p.setHealth(20);p.setAbsorptionAmount(0);p.removeAllEffects();
    }
    private static ItemStack equipment(TestPlayer p,boolean boots) {
        var h=CuriosApi.getCuriosInventory(p).resolve().orElseThrow();
        var diary=new CurioStacksHandler(h,"travel_diary",1,true,false,true,DropRule.DEFAULT);
        var feet=new CurioStacksHandler(h,"feet",1,true,false,true,DropRule.DEFAULT);
        var curio=new CurioStacksHandler(h,"curio",1,true,false,true,DropRule.DEFAULT);
        h.setCurios(new HashMap<>(Map.of("travel_diary",diary,"feet",feet,"curio",curio)));
        ItemStack d=new ItemStack(ModRegistry.LONG_TRAVAIL.get());
        check(LongTravailData.tryInitialize(d,p),"initialize diary");
        for(var aspect:TravailAspect.values())LongTravailData.setWitness(d,aspect,true);
        diary.getStacks().setStackInSlot(0,d);
        if(boots)feet.getStacks().setStackInSlot(0,new ItemStack(ModRegistry.TOKAIDO.get()));
        return d;
    }
    public static void run(ServerLevel level) throws Exception {
        var nativeData=(ServerLevelData)level.getLevelData();
        int clear=nativeData.getClearWeatherTime(),rain=nativeData.getRainTime(),thunder=nativeData.getThunderTime();
        boolean raining=nativeData.isRaining(),thundering=nativeData.isThundering();
        float rl=level.getRainLevel(1),tl=RainWeather.rawThunderLevel(level);
        TestPlayer p=new TestPlayer(level,"RainTest");
        try {
            ItemStack diary=equipment(p,true);
            level.setWeatherParameters(0,500,true,false);level.setRainLevel(1);
            check(RainState.equipped(p),"feet equipment");
            check(!RainState.carrying(p),"empty carrying");
            ItemStack eki=new ItemStack(ModRegistry.EKI.get());
            p.getEnderChestInventory().setItem(0,eki);check(RainState.carrying(p),"ender chest");
            RainConfig.ENDER.set(false);check(!RainState.carrying(p),"ender config off");RainConfig.ENDER.set(true);
            p.getEnderChestInventory().clearContent();p.setItemSlot(EquipmentSlot.HEAD,eki);
            check(RainState.carrying(p),"armor precompat");p.setItemSlot(EquipmentSlot.HEAD,ItemStack.EMPTY);
            var curio=CuriosApi.getCuriosInventory(p).resolve().orElseThrow().getStacksHandler("curio").orElseThrow().getStacks();
            curio.setStackInSlot(0,eki);check(RainState.carrying(p),"curios precompat");curio.setStackInSlot(0,ItemStack.EMPTY);
            RainState.inventoryTick(p,0);check(!RainState.carrying(p),"native tick cannot bypass locations");
            RainState.inventoryTick(p,-1);check(RainState.carrying(p),"generic external tick");
            long time=level.getGameTime();nativeData.setGameTime(time+3);check(!RainState.carrying(p),"external expiry");nativeData.setGameTime(time);RainState.forget(p);
            p.getInventory().items.set(0,eki);
            LongTravailData.setWitness(diary,TravailAspect.ABYSS,false);
            near(RainState.speed(p),1.2F,"malice speed");
            p.getInventory().items.set(1,eki.copy());near(RainState.speed(p),1.2F,"duplicates do not stack");
            com.thelongtravail.data.StiffState.start(p,100);near(p.getSpeed(),0,"stiff wins");com.thelongtravail.data.StiffState.clear(p);p.removeAllEffects();
            Cow cow=new Cow(EntityType.COW,level);cow.setHealth(10);
            Cap cap=new Cap();cap.target=cow;MinecraftForge.EVENT_BUS.register(cap);
            try {
                check(cow.hurt(level.damageSources().playerAttack(p),6),"damage accepted");near(cow.getHealth(),7,"2-point cap becomes 3 final damage");
                cow.invulnerableTime=0;cap.cancel=true;cow.hurt(level.damageSources().playerAttack(p),6);near(cow.getHealth(),7,"cancel remains zero");
            } finally { MinecraftForge.EVENT_BUS.unregister(cap); }
            reset(p);LongTravailData.setWitness(diary,TravailAspect.ABYSS,true);RainConfig.IMMUNITY.set(1D);
            check(!p.hurt(level.damageSources().generic(),4),"guaranteed Eki immunity");near(p.getHealth(),20,"no health loss");
            RainConfig.IMMUNITY.set(0D);reset(p);
            check(!p.hurt(level.damageSources().inWall(),4),"failed Eki roll falls back to native environmental immunity");
            RainConfig.FALLBACK.set(false);reset(p);check(p.hurt(level.damageSources().inWall(),4),"fallback can be disabled");RainConfig.FALLBACK.set(true);
            TravailConfig.RECEIVED_TRIGGER.set(TravailConfig.ReceivedTrigger.ATTACK_ATTEMPT);TravailConfig.RECEIVED_COOLDOWN.set(0);
            LongTravailData.setWitness(diary,TravailAspect.FAR_REACH,false);TravailConfig.FAR_ALL_CLEAR_CHANCE.set(1D);
            LongTravailData.setWitness(diary,TravailAspect.ABYSS,false);reset(p);p.addEffect(new MobEffectInstance(net.minecraft.world.effect.MobEffects.LUCK,100));
            p.hurt(level.damageSources().generic(),4);check(p.hasEffect(net.minecraft.world.effect.MobEffects.LUCK),"Malice skip received penalty");
            RainConfig.MALICE_SKIP.set(false);reset(p);p.addEffect(new MobEffectInstance(net.minecraft.world.effect.MobEffects.LUCK,100));
            p.hurt(level.damageSources().generic(),4);check(!p.hasEffect(net.minecraft.world.effect.MobEffects.LUCK),"Malice penalty toggle");RainConfig.MALICE_SKIP.set(true);
            LongTravailData.setWitness(diary,TravailAspect.ABYSS,true);RainConfig.IMMUNITY.set(1D);reset(p);p.addEffect(new MobEffectInstance(net.minecraft.world.effect.MobEffects.LUCK,100));
            p.hurt(level.damageSources().generic(),4);check(p.hasEffect(net.minecraft.world.effect.MobEffects.LUCK),"immunity skips penalty");
            RainConfig.IMMUNE_SKIP.set(false);reset(p);p.addEffect(new MobEffectInstance(net.minecraft.world.effect.MobEffects.LUCK,100));
            p.hurt(level.damageSources().generic(),4);check(!p.hasEffect(net.minecraft.world.effect.MobEffects.LUCK),"immunity penalty toggle at attack attempt");RainConfig.IMMUNE_SKIP.set(true);
            reset(p);p.hurt(level.damageSources().genericKill(),Float.MAX_VALUE);check(p.isDeadOrDying(),"kill bypasses Eki");
            TestPlayer boots=new TestPlayer(level,"RainBoots");equipment(boots,true);reset(boots);
            check(!boots.hurt(level.damageSources().inFire(),4),"hidden native fire resistance");
            check(!boots.hasEffect(net.minecraft.world.effect.MobEffects.FIRE_RESISTANCE),"no potion instance");
            boots.getFoodData().setFoodLevel(20);boots.getFoodData().setSaturation(0);RainEvents.recoverFood(boots);near(boots.getFoodData().getSaturationLevel(),.5F,"saturation at full hunger");
            RainConfig.FOOD.set(0);RainEvents.recoverFood(boots);near(boots.getFoodData().getSaturationLevel(),1,"saturation only");RainConfig.FOOD.set(1);
            boots.setHealth(10);
            for(int i=0;i<20;i++)RainEvents.tick(new TickEvent.PlayerTickEvent(TickEvent.Phase.END,boots));near(boots.getHealth(),11,"one point every 20 ticks");
            var w=RainWeather.get(level);level.setWeatherParameters(2400,1200,false,false);level.setRainLevel(0);
            w.start(level,1200);check(w.active()&&nativeData.isRaining(),"force rain flag");near(level.getRainLevel(1),0,"start must not snap intensity");check(!w.naturalRain(level),"natural snapshot remains clear");
            w.advance(level);check(w.remaining()==1199,"weather countdown");w.start(level,1200);check(w.remaining()==1200&&!w.naturalRain(level),"refresh preserves original");
            var saved=RainWeather.load(w.save(new CompoundTag()));check(saved.remaining()==1200&&!saved.naturalRain(level),"weather NBT roundtrip");
            var desert=level.registryAccess().registryOrThrow(Registries.BIOME).getOrThrow(Biomes.DESERT);
            var snow=level.registryAccess().registryOrThrow(Registries.BIOME).getOrThrow(Biomes.SNOWY_PLAINS);
            BlockPos pos=new BlockPos(0,100,0);
            check(desert.getPrecipitationAt(pos)==Biome.Precipitation.NONE,"no global biome mutation");
            try(var scope=new PrecipitationScope(level)) {
                check(desert.getPrecipitationAt(pos)==Biome.Precipitation.RAIN,"dry rain");
                check(snow.getPrecipitationAt(pos)==Biome.Precipitation.RAIN,"snow to rain");
                RainConfig.SNOW_RAIN.set(false);check(snow.getPrecipitationAt(pos)==Biome.Precipitation.SNOW,"snow switch");RainConfig.SNOW_RAIN.set(true);
            }
            try(var scope=new PrecipitationScope(level.getServer().getLevel(Level.NETHER))) { check(desert.getPrecipitationAt(pos)==Biome.Precipitation.NONE,"dimension isolation"); }
            w.restore(level);check(!level.getLevelData().isRaining()&&nativeData.getClearWeatherTime()==2400,"restore frozen cycle");
            level.setRainLevel(.5F);level.setThunderLevel(.8F);
            w.start(level,2);w.restore(level);
            near(level.getRainLevel(1),.5F,"restore transitional rain strength");
            near(level.getThunderLevel(1),.4F,"restore transitional thunder without multiplying rain twice");
            level.setRainLevel(0);level.setThunderLevel(.8F);
            w.start(level,2);w.restore(level);level.setRainLevel(1);
            near(level.getThunderLevel(1),.8F,"preserve raw thunder even when rain strength was zero");
            level.setRainLevel(0);level.setThunderLevel(0);
            w.start(level,2);w.advance(level);check(w.active(),"duration has one tick remaining");w.advance(level);
            check(!w.active()&&!nativeData.isRaining()&&nativeData.getClearWeatherTime()==2400,"automatic expiry restores original");
            w.start(level,50);RainConfig.SKILL_ENABLED.set(false);w.advance(level);check(!w.active()&&!nativeData.isRaining(),"disabling active skill restores weather");RainConfig.SKILL_ENABLED.set(true);
            var weatherTick=ServerLevel.class.getDeclaredMethod("advanceWeatherCycle");weatherTick.setAccessible(true);
            level.setWeatherParameters(2400,1200,false,false);level.setRainLevel(0);level.setThunderLevel(0);
            w.start(level,150);
            check(RainState.passive(boots),"passive starts before visible rain threshold");
            weatherTick.invoke(level);near(level.getRainLevel(1),.01F,"native first fade tick");
            for(int i=1;i<100;i++)weatherTick.invoke(level);
            near(level.getRainLevel(1),1,"native five second fade in");
            w.start(level,50);near(level.getRainLevel(1),1,"refresh keeps intensity");
            for(int i=0;i<50;i++)weatherTick.invoke(level);
            check(!w.active()&&!nativeData.isRaining()&&nativeData.getClearWeatherTime()==2400,"expiry preserves paused timers");
            near(level.getRainLevel(1),.99F,"expiry begins native fade out");
            check(!RainState.passive(boots),"residual rain does not extend passive");
            for(int i=0;i<100;i++)weatherTick.invoke(level);
            near(level.getRainLevel(1),0,"native fade out complete");
            level.setWeatherParameters(0,800,true,true);level.setRainLevel(.8F);level.setThunderLevel(.7F);
            w.start(level,10);weatherTick.invoke(level);
            near(RainWeather.rawThunderLevel(level),.69F,"thunder fades without snapping");
            for(int i=1;i<10;i++)weatherTick.invoke(level);
            check(nativeData.isThundering(),"restore thunder flag");
            near(RainWeather.rawThunderLevel(level),.62F,"thunder smoothly reverses on expiry");
            System.out.println("RAIN_FADE_PASS: real native weather ticks, fade in/out, refresh, thunder and passive boundaries");
            w.start(level,50);level.getServer().getCommands().performPrefixedCommand(level.getServer().createCommandSourceStack(),"weather clear 200");check(!w.active()&&!nativeData.isRaining(),"admin override");
            RainEvents.cast(boots);check(RainState.cooldown(boots)==6000&&w.active(),"successful cast starts cooldown");
            TestPlayer rejected=new TestPlayer(level,"RainReject");equipment(rejected,true);RainConfig.REPEAT.set(RainConfig.Repeat.REJECT);
            RainEvents.cast(rejected);check(RainState.cooldown(rejected)==0,"rejected repeat has no cooldown");RainConfig.REPEAT.set(RainConfig.Repeat.REFRESH);
            TestPlayer clone=new TestPlayer(level,"RainClone");RainEvents.clone(new PlayerEvent.Clone(clone,boots,true));check(RainState.cooldown(clone)==6000,"death clone cooldown");
            RainEvents.tick(new TickEvent.PlayerTickEvent(TickEvent.Phase.END,clone));check(RainState.cooldown(clone)==5999,"unequipped cooldown ticks");
            CompoundTag playerData=new CompoundTag();boots.saveWithoutId(playerData);TestPlayer rejoin=new TestPlayer(level,"RainRejoin");rejoin.load(playerData);check(RainState.cooldown(rejoin)==6000,"player NBT cooldown roundtrip");
            var initialChest = level.getServer().getLootData().getLootTable(new ResourceLocation("minecraft:chests/buried_treasure"));
            check(initialChest.getPool("the_long_travail:shared_items") != null && initialChest.getPool("the_long_travail:eki_0") == null && initialChest.getPool("the_long_travail:tokaido_0") == null, "configured native pools present after real resource loading");
            var gson = Deserializers.createLootTableSerializer().create();
            for (var spec : List.of("minecraft:chests/buried_treasure", "minecraft:gameplay/fishing")) {
                var json = gson.toJsonTree(level.getServer().getLootData().getLootTable(new ResourceLocation(spec)), LootTable.class).getAsJsonObject();
                int shared = 0;
                for (var element : json.getAsJsonArray("pools")) {
                    var pool = element.getAsJsonObject();
                    if (!pool.has("name") || !pool.get("name").getAsString().startsWith("the_long_travail:shared_items")) continue;
                    shared++;
                    check(pool.getAsJsonArray("entries").size()==2, "one shared pool contains both entries");
                    double chance = pool.getAsJsonArray("conditions").get(0).getAsJsonObject().get("chance").getAsDouble();
                    check(Math.abs(chance-(spec.contains("fishing")?.05:.15))<.000001, "default total probabilities are 5% and 15%");
                }
                check(shared==1, "exactly one shared pool per target table");
            }
            LootParams params=new LootParams.Builder(level).withParameter(LootContextParams.ORIGIN,p.position()).create(LootContextParamSets.CHEST);
            var table=reloadLoot(level,"minecraft:chests/buried_treasure",false);
            var loot=table.getRandomItems(params);
            check(loot.stream().anyMatch(s0->!s0.is(ModRegistry.EKI.get())&&!s0.is(ModRegistry.TOKAIDO.get())),"preserve native chest loot");
            var hook=new net.minecraft.world.entity.projectile.FishingHook(boots,level,0,0);
            var fishParams=new LootParams.Builder(level).withParameter(LootContextParams.ORIGIN,boots.position())
                    .withParameter(LootContextParams.TOOL,new ItemStack(Items.FISHING_ROD)).withParameter(LootContextParams.THIS_ENTITY,hook).create(LootContextParamSets.FISHING);
            var fishing=reloadLoot(level,"minecraft:gameplay/fishing",true);
            check(fishing.getPool("the_long_travail:shared_items") != null, "world datapack path also injects fixed native pools");
            String serialized = Deserializers.createLootTableSerializer().create().toJson(fishing, LootTable.class);
            check(serialized.contains("the_long_travail:eki") && serialized.contains("the_long_travail:tokaido") && serialized.contains("minecraft:random_chance"), "normal loot table serialization retains both items and chance");
            int ekiHits=0,tokaidoHits=0;
            for(int i=1;i<=5000;i++) {
                var catchItems=fishing.getRandomItems(fishParams,i*7919L);
                int e=(int)catchItems.stream().filter(s0->s0.is(ModRegistry.EKI.get())).count();
                int t=(int)catchItems.stream().filter(s0->s0.is(ModRegistry.TOKAIDO.get())).count();
                check(e+t<=1,"fishing chooses at most one accessory");ekiHits+=e;tokaidoHits+=t;
                check(catchItems.stream().anyMatch(s0->!s0.is(ModRegistry.EKI.get())&&!s0.is(ModRegistry.TOKAIDO.get())),"preserve native catch");
            }
            check(Math.abs(ekiHits-125)<60&&Math.abs(tokaidoHits-125)<60,"fixed fishing total chance and equal weights");
            System.out.println("NATIVE_RAIN_LOOT_PASS: fixed pools, datapack path, serialization, fishing and native drops");
            var configCheck=com.electronwill.nightconfig.core.CommentedConfig.inMemory();
            TravailConfig.SPECS.get("aspects/abyss.toml").correct(configCheck);
            configCheck.set("items.tokaido.slots",List.of("a".repeat(128)));
            check(TravailConfig.SPECS.get("aspects/abyss.toml").isCorrect(configCheck),"128-character slot accepted by config");
            var buffer=new net.minecraft.network.FriendlyByteBuf(io.netty.buffer.Unpooled.buffer());
            try {
                var packet=new com.thelongtravail.network.RainPacket(p.getId(),false,true,true,1,false,List.of(),List.of(),List.of("a".repeat(128)));
                com.thelongtravail.network.RainPacket.encode(packet,buffer);
                check(packet.equals(com.thelongtravail.network.RainPacket.decode(buffer)),"max length slot network roundtrip");
            } finally { buffer.release(); }
            configCheck.set("items.tokaido.slots",List.of("a".repeat(129)));
            check(!TravailConfig.SPECS.get("aspects/abyss.toml").isCorrect(configCheck),"oversized slot rejected before network encoding");
            System.out.println("RAIN_ITEMS_PASS: carrying, cap/cancel, immunity/kill, stiff, native fire, recovery, snapshot/refresh/admin, biome scope, cooldown persistence, loot");
        } finally {
            RainWeather.get(level).discard(level);
            nativeData.setClearWeatherTime(clear);nativeData.setRainTime(rain);nativeData.setThunderTime(thunder);nativeData.setRaining(raining);nativeData.setThundering(thundering);level.setRainLevel(rl);level.setThunderLevel(tl);
            TravailConfig.SPECS.values().forEach(spec -> spec.setConfig(com.electronwill.nightconfig.core.CommentedConfig.inMemory()));
        }
    }
}
