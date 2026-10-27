package travail.configtests;

import com.thelongtravail.config.*;

import com.mojang.authlib.GameProfile;
import com.thelongtravail.abyss.AbundanceEffectPool;
import com.thelongtravail.abyss.LiquefiedAbundance;
import com.thelongtravail.flourishing.ImmortalBud;
import com.thelongtravail.flourishing.ImmortalBudRecipe;
import com.thelongtravail.network.TooltipConfigSync;
import com.thelongtravail.registry.ModRegistry;
import com.thelongtravail.valley.SwordLanternConfig;
import com.thelongtravail.valley.SwordLanternState;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.inventory.TransientCraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraftforge.common.util.FakePlayerFactory;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.type.capability.ICurio.DropRule;
import top.theillusivec4.curios.common.inventory.CurioStacksHandler;
import java.util.*;

// 在隔离 Forge 服务端启动后运行，结束时恢复配置和临时实体。
public final class ItemConfigIntegrationTest {
    private int checks;

    private void check(boolean condition, String message) {
        checks++;
        if (!condition) throw new AssertionError(message);
    }

    public static int run(MinecraftServer server) throws Exception {
        var test = new ItemConfigIntegrationTest();
        test.verify(server);
        return test.checks;
    }

    private void verify(MinecraftServer server) throws Exception {
        var oldBud = FlourishingItemsConfig.BUD_BLACKLIST.get();
        boolean oldRepair = FlourishingItemsConfig.BUD_REPAIR.get();
        var oldWhite = AbyssItemsConfig.ABUNDANCE_WHITELIST.get();
        var oldBlack = AbyssItemsConfig.ABUNDANCE_BLACKLIST.get();
        double oldRadius = SwordLanternConfig.RADIUS.get();
        int oldMinimum = SwordLanternConfig.CROWD_MINIMUM.get();
        var world = server.overworld();
        var player =
                FakePlayerFactory.get(
                        world,
                        new GameProfile(
                                UUID.fromString("038b9975-ae25-4510-a44c-38ca65e975b2"),
                                "ItemConfigTest"));
        var enemies = new ArrayList<Zombie>();
        var inventory = CuriosApi.getCuriosInventory(player).resolve().orElseThrow();
        var oldSlots = new HashMap<>(inventory.getCurios());
        try {
            check(
                    oldRepair
                            && oldBud.isEmpty()
                            && oldWhite.isEmpty()
                            && oldRadius == 64
                            && oldMinimum == 11,
                    "compatible defaults");
            var target = new ItemStack(Items.DIAMOND_SWORD);
            target.setDamageValue(100);
            target.getOrCreateTag().putString("test_data", "preserved");
            var recipe =
                    new ImmortalBudRecipe(
                            new ResourceLocation("the_long_travail:test_bud"),
                            CraftingBookCategory.MISC);
            var grid = new TransientCraftingContainer(player.inventoryMenu, 2, 2);
            grid.setItem(0, new ItemStack(ModRegistry.IMMORTAL_BUD.get()));
            grid.setItem(1, target);
            check(recipe.matches(grid, world), "default recipe accepts damaged tool");
            var repaired = recipe.assemble(grid, world.registryAccess());
            check(
                    repaired.getDamageValue() == 0 && ImmortalBud.marked(repaired),
                    "default full repair");
            check(
                    target.getDamageValue() == 100 && !ImmortalBud.marked(target),
                    "input not mutated");
            FlourishingItemsConfig.BUD_REPAIR.set(false);
            var preserved = recipe.assemble(grid, world.registryAccess());
            check(
                    preserved.getDamageValue() == 100 && ImmortalBud.marked(preserved),
                    "repair disabled");
            check(
                    preserved.getTag().getString("test_data").equals("preserved"),
                    "custom data retained");
            preserved.setDamageValue(101);
            check(preserved.getDamageValue() == 100, "protection remains with existing damage");
            FlourishingItemsConfig.BUD_BLACKLIST.set(List.of("minecraft:diamond_sword"));
            check(
                    !recipe.matches(grid, world)
                            && recipe.assemble(grid, world.registryAccess()).isEmpty(),
                    "blacklist blocks preview and output");
            check(
                    ImmortalBud.marked(preserved) && ImmortalBud.protectsDrop(preserved),
                    "blacklist preserves existing marks");
            FlourishingItemsConfig.BUD_BLACKLIST.set(List.of());
            check(recipe.matches(grid, world), "removing blacklist permits new applications");
            check(
                    !ImmortalBud.accepts(preserved)
                            && !ImmortalBud.accepts(new ItemStack(Items.STONE)),
                    "existing eligibility rules retained");

            AbyssItemsConfig.ABUNDANCE_BLACKLIST.set(List.of());
            AbyssItemsConfig.ABUNDANCE_WHITELIST.set(List.of());
            LiquefiedAbundance.reload();
            var baseline = AbundanceEffectPool.ids();
            var entriesField = AbundanceEffectPool.class.getDeclaredField("entries");
            entriesField.setAccessible(true);
            var baselineEntries = (List<?>) entriesField.get(null);
            var baselineSpeed =
                    baselineEntries.stream()
                            .map(e -> (AbundanceEffectPool.Entry) e)
                            .filter(
                                    e ->
                                            e.effect()
                                                    == net.minecraft.world.effect.MobEffects
                                                            .MOVEMENT_SPEED)
                            .findFirst()
                            .orElseThrow();
            check(
                    baseline.contains("minecraft:speed") && baseline.contains("minecraft:poison"),
                    "empty whitelist unrestricted");
            AbyssItemsConfig.ABUNDANCE_WHITELIST.set(
                    List.of(
                            "minecraft:speed",
                            "minecraft:speed",
                            "minecraft:poison",
                            "missing:effect"));
            AbyssItemsConfig.ABUNDANCE_BLACKLIST.set(List.of("minecraft:poison"));
            LiquefiedAbundance.reload();
            check(
                    AbundanceEffectPool.ids().equals(List.of("minecraft:speed")),
                    "blacklist precedence and deduplication");
            check(
                    ((List<?>) entriesField.get(null)).equals(List.of(baselineSpeed)),
                    "whitelist preserves all existing levels and durations");
            var random = RandomSource.create(27);
            for (int i = 0; i < 100; i++)
                check(
                        AbundanceEffectPool.sample(random).getEffect()
                                == net.minecraft.world.effect.MobEffects.MOVEMENT_SPEED,
                        "sampling stays within whitelist");
            var playerInput = new ItemStack(ModRegistry.LIQUEFIED_ABUNDANCE.get(), 3);
            player.setItemInHand(InteractionHand.MAIN_HAND, playerInput);
            LiquefiedAbundance.begin(player, InteractionHand.MAIN_HAND);
            LiquefiedAbundance.reload();
            check(
                    LiquefiedAbundance.complete(player, playerInput, true, 0) == playerInput
                            && playerInput.getCount() == 3,
                    "reload cancels pending conversion without consumption");
            for (var list :
                    List.of(
                            List.of("missing:effect"),
                            List.of("INVALID ID"),
                            List.of("minecraft:poison"))) {
                AbyssItemsConfig.ABUNDANCE_WHITELIST.set(list);
                LiquefiedAbundance.reload();
                check(
                        AbundanceEffectPool.ids().isEmpty()
                                && AbundanceEffectPool.sample(random) == null,
                        "invalid or fully blocked whitelist fails closed");
                player.setItemInHand(InteractionHand.MAIN_HAND, playerInput);
                LiquefiedAbundance.begin(player, InteractionHand.MAIN_HAND);
                check(
                        LiquefiedAbundance.complete(player, playerInput, true, 0) == playerInput
                                && playerInput.getCount() == 3,
                        "empty conversion preserves materials");
            }
            AbyssItemsConfig.ABUNDANCE_WHITELIST.set(List.of());
            AbyssItemsConfig.ABUNDANCE_BLACKLIST.set(List.of());
            LiquefiedAbundance.reload();
            check(AbundanceEffectPool.ids().equals(baseline), "empty lists restore original pool");

            var belt =
                    new CurioStacksHandler(
                            inventory, "belt", 1, true, false, true, DropRule.DEFAULT);
            inventory.setCurios(Map.of("belt", belt));
            belt.getStacks().setStackInSlot(0, new ItemStack(ModRegistry.SWORD_AND_LANTERN.get()));
            player.setPos(0, 80, 0);
            for (int i = 0; i < 11; i++) {
                var mob = EntityType.ZOMBIE.create(world);
                mob.setPos(16, 80, i * .1);
                mob.setNoAi(true);
                mob.setTarget(player);
                world.addFreshEntity(mob);
                enemies.add(mob);
            }
            double expectedBase =
                    SwordLanternConfig.ALONE_BONUS.get() + SwordLanternConfig.UNDEAD_BONUS.get();
            check(
                    Math.abs(
                                    SwordLanternState.bonus(player, null)
                                            - expectedBase
                                            - SwordLanternConfig.CROWD_BONUS.get())
                            < 1e-9,
                    "default 11 enemies trigger crowd bonus");
            SwordLanternConfig.CROWD_MINIMUM.set(12);
            check(
                    Math.abs(SwordLanternState.bonus(player, null) - expectedBase) < 1e-9,
                    "threshold changes immediately");
            SwordLanternConfig.CROWD_MINIMUM.set(2);
            SwordLanternConfig.RADIUS.set(8D);
            check(
                    Math.abs(
                                    SwordLanternState.bonus(player, null)
                                            - SwordLanternConfig.ALONE_BONUS.get())
                            < 1e-9,
                    "radius shrink removes enemies and undead bonus");
            SwordLanternConfig.RADIUS.set(32D);
            check(
                    Math.abs(
                                    SwordLanternState.bonus(player, null)
                                            - expectedBase
                                            - SwordLanternConfig.CROWD_BONUS.get())
                            < 1e-9,
                    "radius expansion refreshes cached scan within same tick");
            var valuesMethod = TooltipConfigSync.class.getDeclaredMethod("serverValues");
            var poolsMethod = TooltipConfigSync.class.getDeclaredMethod("serverPools");
            valuesMethod.setAccessible(true);
            poolsMethod.setAccessible(true);
            var values = (Map<?, ?>) valuesMethod.invoke(null);
            var pools = (Map<?, ?>) poolsMethod.invoke(null);
            check(
                    values.get("bud.repair").equals(0D)
                            && values.get("swordLantern.radius").equals(32D)
                            && values.get("swordLantern.crowdMinimum").equals(2D),
                    "numeric synchronization reflects server config");
            check(
                    pools.containsKey("bud.blacklist") && pools.containsKey("abundance.whitelist"),
                    "list synchronization present");
            var numeric = new LinkedHashMap<String, Double>();
            values.forEach((key, value) -> numeric.put((String) key, (Double) value));
            var lists = new LinkedHashMap<String, List<String>>();
            pools.forEach(
                    (key, value) ->
                            lists.put(
                                    (String) key,
                                    ((List<?>) value).stream().map(String.class::cast).toList()));
            var snapshot =
                    new com.thelongtravail.network.TooltipConfigCodec.Snapshot(numeric, lists);
            var buffer =
                    new net.minecraft.network.FriendlyByteBuf(io.netty.buffer.Unpooled.buffer());
            try {
                com.thelongtravail.network.TooltipConfigCodec.encode(snapshot, buffer);
                check(
                        com.thelongtravail.network.TooltipConfigCodec.decode(buffer)
                                .equals(snapshot),
                        "configuration synchronization codec round trip");
            } finally {
                buffer.release();
            }
        } finally {
            FlourishingItemsConfig.BUD_BLACKLIST.set(oldBud);
            FlourishingItemsConfig.BUD_REPAIR.set(oldRepair);
            AbyssItemsConfig.ABUNDANCE_WHITELIST.set(oldWhite);
            AbyssItemsConfig.ABUNDANCE_BLACKLIST.set(oldBlack);
            SwordLanternConfig.RADIUS.set(oldRadius);
            SwordLanternConfig.CROWD_MINIMUM.set(oldMinimum);
            LiquefiedAbundance.reload();
            SwordLanternState.forget(player);
            enemies.forEach(Zombie::discard);
            inventory.setCurios(oldSlots);
            player.getInventory().clearContent();
        }
    }
}
