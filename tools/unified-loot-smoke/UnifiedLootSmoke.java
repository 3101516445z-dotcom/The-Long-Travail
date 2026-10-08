package travail.smoke;

import com.electronwill.nightconfig.core.CommentedConfig;
import com.thelongtravail.config.*;
import com.thelongtravail.loot.*;
import java.nio.file.*;
import java.util.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.*;
import net.minecraft.world.level.storage.loot.*;
import net.minecraft.world.level.storage.loot.parameters.*;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLPaths;

@Mod("travail_smoke")
public final class UnifiedLootSmoke {
    private final Path path = FMLPaths.CONFIGDIR.get().resolve("the_long_travail/general.toml");
    private String original;
    public UnifiedLootSmoke() { MinecraftForge.EVENT_BUS.register(this); }
    private static void check(boolean condition, String message) { if (!condition) throw new AssertionError(message); }
    private static ResourceLocation id(String text) { return new ResourceLocation(text); }
    private static CommentedConfig rule(double chance, String... items) {
        var config = CommentedConfig.inMemory(); config.set("chance", chance); config.set("items", List.of(items)); return config;
    }
    private static LootTable blank(String name) { var table=LootTable.lootTable().build(); table.setLootTableId(id(name)); UnifiedLoot.inject(table); return table; }
    private void save(CommentedConfig config) throws Exception { ConfigFileIO.write(path, config, "Unified loot isolated test"); }
    private static LootParams params(MinecraftServer server) {
        return new LootParams.Builder(server.overworld()).withParameter(LootContextParams.ORIGIN, Vec3.ZERO).create(LootContextParamSets.CHEST);
    }
    @SubscribeEvent public void started(ServerStartedEvent event) {
        var server = event.getServer();
        try {
            original = Files.readString(path);
            var config = ConfigFileIO.read(path);
            var parsed = UnifiedLoot.parse(config);
            check(parsed.size()==31, "31 default tables");
            var ancient = parsed.get(id("minecraft:chests/ancient_city"));
            check(Math.abs(ancient.chance()-.085)<1e-7 && ancient.items().size()==3, "ancient city combined chance");
            var expected = Map.of("a_thousand_years_later", .03, "azrael", .025, "sword_and_lantern", .03);
            int total = ancient.items().stream().mapToInt(UnifiedLoot.Entry::weight).sum();
            for (var entry:ancient.items()) {
                String key=net.minecraftforge.registries.ForgeRegistries.ITEMS.getKey(entry.item()).getPath();
                check(Math.abs(ancient.chance()*entry.weight()/total-expected.get(key))<1e-7,"individual probability "+key);
            }
            for (var entry:parsed.entrySet()) {
                var actual=server.getLootData().getLootTable(entry.getKey());
                check(actual.getPool(UnifiedLoot.POOL)!=null,"real loaded pool "+entry.getKey());
                var gson=Deserializers.createLootTableSerializer().create();
                long count=gson.toJsonTree(actual,LootTable.class).getAsJsonObject().getAsJsonArray("pools").asList().stream()
                    .filter(p->p.getAsJsonObject().has("name") && p.getAsJsonObject().get("name").getAsString().startsWith("the_long_travail:")).count();
                check(count==1,"only one Travail pool "+entry.getKey());
                var table=blank(entry.getKey().toString()); var pool=table.getPool(UnifiedLoot.POOL); UnifiedLoot.inject(table);
                check(pool==table.getPool(UnifiedLoot.POOL),"idempotent");
                Map<Item,Integer> hits=new HashMap<>();
                for(int seed=1;seed<=8000;seed++) {
                    var drops=table.getRandomItems(params(server), seed*7919L);
                    check(drops.stream().mapToInt(ItemStack::getCount).sum()<=1,"all accessories mutually exclusive");
                    for(var stack:drops) hits.merge(stack.getItem(),stack.getCount(),Integer::sum);
                }
                int weights=entry.getValue().items().stream().mapToInt(UnifiedLoot.Entry::weight).sum();
                for(var item:entry.getValue().items()) {
                    double p=entry.getValue().chance()*item.weight()/weights;
                    check(Math.abs(hits.getOrDefault(item.item(),0)-8000*p)<6*Math.sqrt(8000*p*(1-p))+3,"sampled probability "+entry.getKey());
                }
            }
            check(server.getLootData().getLootTable(id("minecraft:chests/ancient_city_ice_box")).getPool(UnifiedLoot.POOL)==null,"unconfigured table untouched");
            var custom=net.minecraftforge.common.ForgeHooks.loadLootTable(Deserializers.createLootTableSerializer().create(),id("minecraft:chests/ancient_city"),
                com.google.gson.JsonParser.parseString("{\"type\":\"minecraft:chest\",\"pools\":[{\"rolls\":1,\"entries\":[{\"type\":\"minecraft:item\",\"name\":\"minecraft:diamond\"}]}]}"),true);
            check(custom.getPool(UnifiedLoot.POOL)!=null && custom.getRandomItems(params(server),1).stream().anyMatch(s->s.is(Items.DIAMOND)),"world datapack injection preserves original drops");
            CommentedConfig tables=config.get("loot.tables");
            tables.set(List.of("travail_smoke:custom"),rule(1,"minecraft:diamond|1|2|4"));
            tables.set(List.of("minecraft:chests/ancient_city"),rule(1,"minecraft:emerald|1|1|1"));
            check(TravailConfig.SPECS.get("general.toml").isCorrect(config),"dynamic table accepted by Forge");
            TravailConfig.SPECS.get("general.toml").correct(config);
            check(tables.contains(List.of("travail_smoke:custom")),"correction preserves user table");
            save(config);
            check(ConfigFileIO.read(path).<CommentedConfig>get("loot.tables").contains(List.of("travail_smoke:custom")),"quoted TOML ID round trip");
            System.out.println("UNIFIED_LOOT_DEFAULTS_PASS: 31 tables, probability sampling, exclusivity, datapacks, dynamic config");
            server.reloadResources(server.getPackRepository().getSelectedIds()).whenComplete((unused,error)->server.execute(()->afterReload(server,error)));
        } catch(Throwable failure) { finish(server,failure); }
    }
    private void afterReload(MinecraftServer server,Throwable error) {
        try {
            if(error!=null)throw new RuntimeException(error);
            var config=ConfigFileIO.read(path);
            check(UnifiedLoot.parse(config).containsKey(id("travail_smoke:custom")),"custom ID survives config watcher");
            var table=server.getLootData().getLootTable(id("travail_smoke:custom"));
            check(table.getPool(UnifiedLoot.POOL)!=null,"new table injected by real resource reload");
            for(int seed=0;seed<100;seed++) {
                int count=table.getRandomItems(params(server),seed).stream().filter(s->s.is(Items.DIAMOND)).mapToInt(ItemStack::getCount).sum();
                check(count>=2&&count<=4,"configured quantity range");
            }
            var ancient=server.getLootData().getLootTable(id("minecraft:chests/ancient_city"));
            check(ancient.getRandomItems(params(server),1).stream().anyMatch(s->s.is(Items.EMERALD)),"changed table reload");
            CommentedConfig tables=config.get("loot.tables");
            tables.set(List.of("travail_smoke:custom"),rule(1,"missing:item|1|1|1"));
            try { UnifiedLoot.parse(config); throw new AssertionError("bad item accepted"); }
            catch(IllegalArgumentException expected) { check(expected.getMessage().contains("travail_smoke:custom")&&expected.getMessage().contains("items[0]"),"diagnostic names table and entry"); }
            save(config); UnifiedLoot.reload();
            check(blank("travail_smoke:custom").getPool(UnifiedLoot.POOL)!=null,"invalid edit retains previous valid rules");
            config.set("loot.enabled",false); save(config);
            server.reloadResources(server.getPackRepository().getSelectedIds()).whenComplete((unused,failure)->server.execute(()->{
                try {
                    if(failure!=null)throw new RuntimeException(failure);
                    check(server.getLootData().getLootTable(id("minecraft:chests/ancient_city")).getPool(UnifiedLoot.POOL)==null,"disabled removes pools on real reload");
                    System.out.println("UNIFIED_LOOT_RELOAD_PASS: live reload, custom IDs, quantity, invalid snapshot, disable");
                    finish(server,null);
                } catch(Throwable ex){finish(server,ex);}
            }));
        }catch(Throwable failure){finish(server,failure);}
    }
    private void finish(MinecraftServer server,Throwable failure) {
        try {
            if(original!=null)Files.writeString(path,original);
            if(failure!=null)failure.printStackTrace();
            Files.writeString(Path.of("unified-loot-result.txt"),failure==null?"PASS":"FAIL");
        }catch(Exception ex){ex.printStackTrace();}
        server.halt(false);
    }
}
