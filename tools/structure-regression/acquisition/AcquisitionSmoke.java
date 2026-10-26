package travail.smoke;

import com.google.gson.*;
import com.thelongtravail.boundless.BoundlessAcquisition;
import com.thelongtravail.loot.ArchaeologyTrades;
import com.thelongtravail.loot.UnifiedLoot;
import com.thelongtravail.registry.ModRegistry;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.npc.*;
import net.minecraft.world.inventory.TransientCraftingContainer;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.trading.*;
import net.minecraft.world.level.storage.loot.*;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.fml.common.Mod;

import java.nio.file.*;
import java.util.*;

@Mod("travail_smoke")
public final class AcquisitionSmoke {
    private int checks;

    public AcquisitionSmoke() {
        MinecraftForge.EVENT_BUS.addListener(this::started);
    }

    private void check(boolean value, String message) {
        checks++;
        if (!value) throw new AssertionError(message);
    }

    private void started(ServerStartedEvent event) {
        String result = "FAIL";
        try {
            run(event.getServer().overworld());
            AcquisitionPressure.run(event.getServer().overworld());
            result = "PASS " + checks + " acquisition checks";
        } catch (Throwable error) {
            error.printStackTrace();
        } finally {
            try {
                Files.writeString(Path.of("acquisition-result.txt"), result);
            } catch (Exception error) {
                throw new RuntimeException(error);
            }
            System.out.println(result);
            event.getServer().halt(false);
        }
    }

    private JsonObject table(ServerLevel level, String id) {
        var table = level.getServer().getLootData().getLootTable(new ResourceLocation(id));
        return Deserializers.createLootTableSerializer()
                .create()
                .toJsonTree(table)
                .getAsJsonObject();
    }

    private Map<String, Double> chances(ServerLevel level, String id, String poolName) {
        var pools = table(level, id).getAsJsonArray("pools");
        for (var raw : pools) {
            var pool = raw.getAsJsonObject();
            if (!pool.has("name") || !pool.get("name").getAsString().equals(poolName)) continue;
            double chance = 1;
            if (pool.has("conditions"))
                for (var condition : pool.getAsJsonArray("conditions")) {
                    var c = condition.getAsJsonObject();
                    if (c.has("chance")) chance *= c.get("chance").getAsDouble();
                }
            var entries = pool.getAsJsonArray("entries");
            double total = 0;
            for (var entry : entries) total += weight(entry.getAsJsonObject());
            Map<String, Double> result = new HashMap<>();
            for (var entry : entries) {
                var e = entry.getAsJsonObject();
                if (e.has("name"))
                    result.merge(
                            e.get("name").getAsString(), chance * weight(e) / total, Double::sum);
            }
            return result;
        }
        return Map.of();
    }

    private double weight(JsonObject e) {
        return e.has("weight") ? e.get("weight").getAsDouble() : 1;
    }

    private void chest(ServerLevel level, String id, Map<String, Double> expected) {
        var actual = chances(level, "minecraft:chests/" + id, UnifiedLoot.POOL);
        check(actual.size() == expected.size(), id + " item count " + actual);
        expected.forEach(
                (item, chance) ->
                        check(
                                Math.abs(
                                                actual.getOrDefault("the_long_travail:" + item, -1D)
                                                        - chance)
                                        < 1e-7,
                                id + " " + item));
    }

    private void craft(ServerLevel level, Item[] ingredients, Item result, int count) {
        var player = FakePlayerFactory.getMinecraft(level);
        var grid = new TransientCraftingContainer(player.inventoryMenu, 3, 3);
        for (int i = 0; i < 9; i++)
            if (ingredients[i] != null) grid.setItem(i, new ItemStack(ingredients[i]));
        var recipe = level.getRecipeManager().getRecipeFor(RecipeType.CRAFTING, grid, level);
        if (result == null) {
            check(recipe.isEmpty(), "removed recipe still matches");
            return;
        }
        check(recipe.isPresent(), "recipe missing " + result);
        var output = recipe.orElseThrow().assemble(grid, level.registryAccess());
        check(output.is(result) && output.getCount() == count, "recipe output " + output);
    }

    private void trade(ServerLevel level, boolean mason) {
        var villager = new Villager(EntityType.VILLAGER, level);
        villager.setVillagerData(
                villager.getVillagerData()
                        .setProfession(
                                mason ? VillagerProfession.MASON : VillagerProfession.FLETCHER)
                        .setLevel(5));
        Item result = mason ? ModRegistry.PHOSPHOPHYLLITE.get() : ModRegistry.LORENTZ_FANTASY.get();
        var old =
                new MerchantOffer(
                        new ItemStack(Items.EMERALD, 64),
                        new ItemStack(Items.DRAGON_BREATH, 64),
                        new ItemStack(result),
                        1,
                        30,
                        0F);
        var tag = old.createTag();
        tag.putInt("uses", 1);
        tag.putInt("demand", 3);
        tag.putInt("specialPrice", 2);
        old = new MerchantOffer(tag);
        var offers = new MerchantOffers();
        offers.add(old);
        offers.add(old);
        if (mason) ArchaeologyTrades.ensure(villager, offers);
        else BoundlessAcquisition.ensure(villager, offers);
        check(offers.size() == 1, "duplicate trade");
        var current = offers.get(0);
        check(
                current.getBaseCostA().is(mason ? Items.LAPIS_LAZULI : Items.DRAGON_BREATH),
                "trade first material");
        check(current.getBaseCostA().getCount() == (mason ? 64 : 16), "trade first quantity");
        check(
                current.getCostB().is(mason ? Items.GREEN_DYE : Items.DRAGON_HEAD)
                        && current.getCostB().getCount() == 1,
                "trade second cost");
        var after = current.createTag();
        tag.remove("buy");
        tag.remove("buyB");
        after.remove("buy");
        after.remove("buyB");
        check(tag.equals(after), "trade state lost");
        if (mason) ArchaeologyTrades.ensure(villager, offers);
        else BoundlessAcquisition.ensure(villager, offers);
        check(offers.get(0) == current, "trade not idempotent");
        offers.clear();
        if (mason) ArchaeologyTrades.ensure(villager, offers);
        else BoundlessAcquisition.ensure(villager, offers);
        check(
                offers.size() == 1
                        && offers.get(0).getCostB().is(mason ? Items.GREEN_DYE : Items.DRAGON_HEAD),
                "new trade");
        villager.discard();
    }

    private void carryingChecks(ServerLevel level) {
        var p = FakePlayerFactory.getMinecraft(level);
        p.setHealth(p.getMaxHealth());
        p.getInventory().clearContent();
        var inventory =
                top.theillusivec4.curios.api.CuriosApi.getCuriosInventory(p)
                        .resolve()
                        .orElseThrow();
        var slots =
                new HashMap<
                        String, top.theillusivec4.curios.api.type.inventory.ICurioStacksHandler>();
        for (String id : List.of("travel_diary", "curio", "body", "necklace"))
            slots.put(
                    id,
                    new top.theillusivec4.curios.common.inventory.CurioStacksHandler(
                            inventory,
                            id,
                            2,
                            true,
                            false,
                            true,
                            top.theillusivec4.curios.api.type.capability.ICurio.DropRule.DEFAULT));
        inventory.setCurios(slots);
        var diary = new ItemStack(ModRegistry.LONG_TRAVAIL.get());
        slots.get("travel_diary").getStacks().setStackInSlot(0, diary);
        var gold = new ItemStack(ModRegistry.GOLDEN_AGE.get());
        p.getInventory().setItem(0, gold);
        check(com.thelongtravail.farreach.FarReachEquipment.equipped(p, true), "gold in inventory");
        p.getInventory().setItem(0, ItemStack.EMPTY);
        check(!com.thelongtravail.farreach.FarReachEquipment.equipped(p, true), "gold removed");
        p.getInventory().offhand.set(0, gold);
        check(com.thelongtravail.farreach.FarReachEquipment.equipped(p, true), "gold in offhand");
        p.getInventory().offhand.set(0, ItemStack.EMPTY);
        var book = new ItemStack(ModRegistry.BOOK_OF_DEAD.get());
        var spare = new ItemStack(ModRegistry.BOOK_OF_DEAD.get());
        p.getInventory().setItem(0, book);
        p.getInventory().setItem(1, spare);
        check(
                com.thelongtravail.underworld.UnderworldItems.equipped(p, false) == book,
                "actual carried book");
        slots.get("travel_diary").getStacks().setStackInSlot(0, ItemStack.EMPTY);
        check(
                com.thelongtravail.underworld.UnderworldItems.equipped(p, false).isEmpty(),
                "book requires diary");
        slots.get("travel_diary").getStacks().setStackInSlot(0, diary);
        p.setHealth(0);
        check(
                com.thelongtravail.underworld.UnderworldItems.equipped(p, false) == book,
                "book found at death");
        p.setHealth(p.getMaxHealth());
        var ledger = com.thelongtravail.underworld.UnderworldLedger.get(level.getServer());
        ledger.consume(com.thelongtravail.underworld.UnderworldLedger.identity(book));
        check(
                com.thelongtravail.underworld.UnderworldItems.equipped(p, false) == spare,
                "spent book skipped");
        com.thelongtravail.underworld.UnderworldStorage.player(p);
        check(book.isEmpty() && !spare.isEmpty(), "only selected book consumed");
        p.getInventory().setItem(1, ItemStack.EMPTY);
        p.getEnderChestInventory().setItem(0, spare);
        check(
                com.thelongtravail.underworld.UnderworldItems.equipped(p, false) == spare,
                "book in ender chest");
        ledger.consume(com.thelongtravail.underworld.UnderworldLedger.identity(spare));
        com.thelongtravail.underworld.UnderworldStorage.player(p);
        check(spare.isEmpty(), "ender book consumed");
        slots.get("body")
                .getStacks()
                .setStackInSlot(0, new ItemStack(ModRegistry.DON_QUIXOTE.get()));
        check(
                !com.thelongtravail.farreach.DonQuixoteCombat.equipped(p),
                "quixote old slot inactive");
        slots.get("curio")
                .getStacks()
                .setStackInSlot(0, new ItemStack(ModRegistry.DON_QUIXOTE.get()));
        check(
                com.thelongtravail.farreach.DonQuixoteCombat.equipped(p),
                "quixote generic slot active");
        slots.get("necklace")
                .getStacks()
                .setStackInSlot(0, new ItemStack(ModRegistry.STAR_VOICE.get()));
        check(
                !com.thelongtravail.boundless.BoundlessEquipment.equipped(p, false),
                "star old slot inactive");
        slots.get("curio")
                .getStacks()
                .setStackInSlot(1, new ItemStack(ModRegistry.STAR_VOICE.get()));
        check(
                com.thelongtravail.boundless.BoundlessEquipment.equipped(p, false),
                "star generic slot active");
        slots.get("travel_diary").getStacks().setStackInSlot(0, ItemStack.EMPTY);
        check(
                !com.thelongtravail.boundless.BoundlessEquipment.equipped(p, false),
                "star still requires diary");
    }

    private void run(ServerLevel level) {
        carryingChecks(level);
        var fish =
                chances(level, "minecraft:gameplay/fishing/treasure", "the_long_travail:materials");
        for (String item : List.of("eki", "tokaido", "perfect_fluid", "abyss_essence"))
            check(
                    Math.abs(fish.getOrDefault("the_long_travail:" + item, -1D) - .1) < 1e-8,
                    "fishing " + item + fish);
        check(
                chances(level, "minecraft:gameplay/fishing", UnifiedLoot.POOL).isEmpty(),
                "fishing parent reward remains");
        chest(
                level,
                "ancient_city",
                Map.of("a_thousand_years_later", .05, "sword_and_lantern", .05));
        chest(
                level,
                "end_city_treasure",
                Map.of(
                        "free_and_easy_wandering",
                        .075,
                        "star_voice",
                        .05,
                        "lorentz_fantasy",
                        .075));
        for (String id :
                List.of("stronghold_corridor", "stronghold_crossing", "stronghold_library"))
            chest(level, id, Map.of("azrael", .075, "star_voice", .075));
        chest(
                level,
                "nether_bridge",
                Map.of(
                        "golden_age",
                        .05,
                        "demon_king_of_the_sixth_heaven",
                        .05,
                        "book_of_the_dead",
                        .05));
        chest(
                level,
                "woodland_mansion",
                Map.of("midnight_lafite", .075, "a_thousand_years_later", .075));
        chest(level, "shipwreck_supply", Map.of("midnight_lafite", .05, "perfect_fluid", .05));
        chest(level, "abandoned_mineshaft", Map.of("azrael", .075, "sword_and_lantern", .075));
        chest(
                level,
                "village/village_armorer",
                Map.of(
                        "affection",
                        .05,
                        "free_and_easy_wandering",
                        .05,
                        "everlasting_flower",
                        .05));
        for (Item wool : List.of(Items.WHITE_WOOL, Items.RED_WOOL, Items.BLACK_WOOL))
            craft(
                    level,
                    new Item[] {
                        wool,
                        Items.SCULK,
                        wool,
                        wool,
                        Items.SCULK_SHRIEKER,
                        wool,
                        wool,
                        Items.SCULK,
                        wool
                    },
                    ModRegistry.UNDERGROUND_VELVET.get(),
                    4);
        craft(
                level,
                new Item[] {
                    Items.STRING,
                    Items.LAPIS_LAZULI,
                    Items.STRING,
                    Items.STRING,
                    Items.DIAMOND,
                    Items.STRING,
                    Items.STRING,
                    Items.LAPIS_LAZULI,
                    Items.STRING
                },
                ModRegistry.UNDERGROUND_VELVET.get(),
                1);
        craft(
                level,
                new Item[] {
                    Items.STRING,
                    Items.SCULK,
                    Items.STRING,
                    Items.STRING,
                    Items.SCULK_SHRIEKER,
                    Items.STRING,
                    Items.STRING,
                    Items.SCULK,
                    Items.STRING
                },
                null,
                0);
        craft(
                level,
                new Item[] {
                    null,
                    Items.POPPY,
                    null,
                    Items.POPPY,
                    Items.EXPERIENCE_BOTTLE,
                    Items.POPPY,
                    null,
                    Items.POPPY,
                    null
                },
                ModRegistry.MISTLETOE.get(),
                1);
        craft(
                level,
                new Item[] {
                    Items.GLOWSTONE_DUST,
                    Items.BONE_MEAL,
                    Items.GLOWSTONE_DUST,
                    Items.BONE_MEAL,
                    ModRegistry.SPRING_CORNER.get(),
                    Items.BONE_MEAL,
                    Items.GLOWSTONE_DUST,
                    Items.BONE_MEAL,
                    Items.GLOWSTONE_DUST
                },
                ModRegistry.EVERLASTING_FLOWER.get(),
                1);
        trade(level, true);
        trade(level, false);
    }
}
