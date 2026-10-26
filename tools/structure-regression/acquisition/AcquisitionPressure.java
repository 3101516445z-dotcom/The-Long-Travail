package travail.smoke;

import com.google.gson.*;
import com.thelongtravail.api.CarryingApi;
import com.thelongtravail.boundless.BoundlessEquipment;
import com.thelongtravail.config.CarryingConfig;
import com.thelongtravail.farreach.*;
import com.thelongtravail.registry.ModRegistry;
import com.thelongtravail.underworld.*;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.*;
import net.minecraft.world.level.storage.loot.*;
import net.minecraft.world.level.storage.loot.parameters.*;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.registries.ForgeRegistries;

import top.theillusivec4.curios.api.*;
import top.theillusivec4.curios.api.type.capability.*;
import top.theillusivec4.curios.common.inventory.CurioStacksHandler;

import java.util.*;

final class AcquisitionPressure {
    private static int checks;

    private static void check(boolean ok, String message) {
        checks++;
        if (!ok) throw new AssertionError(message);
    }

    static void run(ServerLevel level) throws Exception {
        loot(level);
        carrying(level);
        System.out.println("PRESSURE_PASS checks=" + checks + " seed=20261026");
    }

    private static void rate(int count, int n, double p, String label) {
        double tolerance = 7 * Math.sqrt(n * p * (1 - p)) + 2;
        check(
                Math.abs(count - n * p) <= tolerance,
                label + " count=" + count + " expected=" + n * p);
    }

    private static void loot(ServerLevel level) throws Exception {
        var params =
                new LootParams.Builder(level)
                        .withParameter(LootContextParams.ORIGIN, Vec3.ZERO)
                        .withParameter(LootContextParams.TOOL, new ItemStack(Items.FISHING_ROD))
                        .create(LootContextParamSets.FISHING);
        var context =
                new LootContext.Builder(params).withOptionalRandomSeed(20261026L).create(null);
        JsonObject defaults;
        try (var reader =
                new java.io.InputStreamReader(
                        com.thelongtravail.loot.UnifiedLootConfig.class.getResourceAsStream(
                                "/the_long_travail/default_loot.json"),
                        java.nio.charset.StandardCharsets.UTF_8)) {
            defaults = JsonParser.parseReader(reader).getAsJsonObject();
        }
        int n = 30000;
        for (var entry : defaults.entrySet()) {
            var rule = entry.getValue().getAsJsonObject();
            var table =
                    level.getServer()
                            .getLootData()
                            .getLootTable(new ResourceLocation(entry.getKey()));
            var pool = table.getPool("the_long_travail:shared_items");
            check(pool != null, "missing pool " + entry.getKey());
            Map<Item, Double> expected = new HashMap<>();
            double total = 0;
            for (var item : rule.getAsJsonArray("items"))
                total += Integer.parseInt(item.getAsString().split("\\|")[1]);
            for (var item : rule.getAsJsonArray("items")) {
                var parts = item.getAsString().split("\\|");
                expected.put(
                        ForgeRegistries.ITEMS.getValue(new ResourceLocation(parts[0])),
                        rule.get("chance").getAsDouble() * Integer.parseInt(parts[1]) / total);
            }
            Map<Item, Integer> counts = new HashMap<>();
            int[] produced = {0};
            for (int i = 0; i < n; i++) {
                produced[0] = 0;
                pool.addRandomItems(
                        stack -> {
                            check(
                                    stack.getCount() == 1 && expected.containsKey(stack.getItem()),
                                    "unexpected chest result");
                            counts.merge(stack.getItem(), 1, Integer::sum);
                            produced[0]++;
                        },
                        context);
                check(produced[0] <= 1, "duplicate pool reward");
            }
            for (var e : expected.entrySet())
                rate(counts.getOrDefault(e.getKey(), 0), n, e.getValue(), entry.getKey());
            rate(
                    counts.values().stream().mapToInt(Integer::intValue).sum(),
                    n,
                    rule.get("chance").getAsDouble(),
                    entry.getKey() + " total");
        }
        var treasure =
                level.getServer()
                        .getLootData()
                        .getLootTable(new ResourceLocation("minecraft:gameplay/fishing/treasure"));
        Map<Item, Integer> counts = new HashMap<>();
        int[] produced = {0};
        for (int i = 0; i < 100000; i++) {
            produced[0] = 0;
            treasure.getRandomItemsRaw(
                    context,
                    stack -> {
                        counts.merge(stack.getItem(), 1, Integer::sum);
                        produced[0]++;
                    });
            check(produced[0] == 1, "treasure must return one item");
        }
        for (var item :
                List.of(
                        ModRegistry.EKI.get(),
                        ModRegistry.TOKAIDO.get(),
                        ModRegistry.PERFECT_FLUID.get()))
            rate(counts.getOrDefault(item, 0), 100000, .1, "fishing " + item);
        System.out.println(
                "LOOT_PRESSURE_PASS chest_rolls=" + defaults.size() * n + " treasure_rolls=100000");
    }

    private static void carrying(ServerLevel level) {
        var p = FakePlayerFactory.getMinecraft(level);
        p.setHealth(p.getMaxHealth());
        p.getInventory().clearContent();
        p.getEnderChestInventory().clearContent();
        var inv = CuriosApi.getCuriosInventory(p).resolve().orElseThrow();
        var slots =
                new HashMap<
                        String, top.theillusivec4.curios.api.type.inventory.ICurioStacksHandler>();
        for (String id : List.of("curio", "travel_diary", "body", "necklace", "charm"))
            slots.put(
                    id,
                    new CurioStacksHandler(inv, id, 2, true, false, true, ICurio.DropRule.DEFAULT));
        inv.setCurios(slots);
        var random = new Random(20261026L);
        var ledger = UnderworldLedger.get(level.getServer());
        for (int round = 0; round < 10000; round++) {
            p.setHealth(p.getMaxHealth());
            p.getInventory().clearContent();
            p.getEnderChestInventory().clearContent();
            for (var h : slots.values())
                for (int j = 0; j < 2; j++) {
                    h.getStacks().setStackInSlot(j, ItemStack.EMPTY);
                    h.getCosmeticStacks().setStackInSlot(j, ItemStack.EMPTY);
                }
            boolean diary = random.nextBoolean();
            if (diary)
                slots.get("travel_diary")
                        .getStacks()
                        .setStackInSlot(0, new ItemStack(ModRegistry.LONG_TRAVAIL.get()));
            int area = random.nextInt(6);
            boolean enabled = random.nextBoolean();
            String key =
                    List.of("inventory", "offhand", "armor", "enderChest", "curios", "curios")
                            .get(area);
            CarryingConfig.AREAS.get(key).set(enabled);
            var gold = new ItemStack(ModRegistry.GOLDEN_AGE.get());
            var book = new ItemStack(ModRegistry.BOOK_OF_DEAD.get());
            switch (area) {
                case 0 -> {
                    p.getInventory().items.set(10, gold);
                    p.getInventory().items.set(11, book);
                }
                case 1 -> {
                    p.getInventory().offhand.set(0, book);
                    p.getInventory().items.set(10, gold);
                }
                case 2 -> {
                    p.getInventory().armor.set(0, gold);
                    p.getInventory().armor.set(1, book);
                }
                case 3 -> {
                    p.getEnderChestInventory().setItem(0, gold);
                    p.getEnderChestInventory().setItem(1, book);
                }
                case 4 -> {
                    slots.get("charm").getStacks().setStackInSlot(0, gold);
                    slots.get("charm").getStacks().setStackInSlot(1, book);
                }
                case 5 -> {
                    slots.get("charm").getCosmeticStacks().setStackInSlot(0, gold);
                    slots.get("charm").getCosmeticStacks().setStackInSlot(1, book);
                }
            }
            boolean expected = enabled && area != 5;
            check(
                    FarReachEquipment.equipped(p, true) == (area == 1 || expected),
                    "gold area " + area);
            p.setHealth(0);
            check(
                    (UnderworldItems.equipped(p, false) == book) == (diary && expected),
                    "book death area " + area);
            p.setHealth(p.getMaxHealth());
            ledger.consume(UnderworldLedger.identity(book));
            check(UnderworldItems.equipped(p, false).isEmpty(), "spent book cannot revive twice");
            UnderworldStorage.player(p);
            check(book.isEmpty() && !gold.isEmpty(), "book cleanup must preserve other items");
            CarryingConfig.AREAS.get(key).set(true);
            boolean generic = random.nextBoolean(), cosmetic = random.nextBoolean();
            var quixote = new ItemStack(ModRegistry.DON_QUIXOTE.get());
            var star = new ItemStack(ModRegistry.STAR_VOICE.get());
            var q = slots.get(generic ? "curio" : "body");
            var t = slots.get(generic ? "curio" : "necklace");
            (cosmetic ? q.getCosmeticStacks() : q.getStacks()).setStackInSlot(0, quixote);
            (cosmetic ? t.getCosmeticStacks() : t.getStacks()).setStackInSlot(1, star);
            check(DonQuixoteCombat.equipped(p) == (generic && !cosmetic), "quixote slot");
            check(
                    BoundlessEquipment.equipped(p, false) == (generic && !cosmetic && diary),
                    "star slot/diary");
            check(
                    !((ICurioItem) gold.getItem())
                            .canEquip(new SlotContext("charm", p, 0, false, true), gold),
                    "gold equip denied");
        }
        CarryingApi.forget(p);
        System.out.println("CARRYING_PRESSURE_PASS transitions=10000");
    }
}
