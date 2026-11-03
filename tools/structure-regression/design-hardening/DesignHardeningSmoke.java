package travail.design;

import com.mojang.authlib.GameProfile;
import com.thelongtravail.config.*;
import com.thelongtravail.farreach.*;
import com.thelongtravail.flourishing.*;
import com.thelongtravail.mixin.effects.EffectDurationAccessor;
import com.thelongtravail.registry.ModRegistry;
import com.thelongtravail.runtime.effects.EffectCopies;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.*;
import net.minecraft.world.effect.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.inventory.TransientCraftingContainer;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.*;

import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.type.capability.ICurio;
import top.theillusivec4.curios.common.inventory.CurioStacksHandler;

import java.nio.file.*;
import java.util.*;

@Mod("travail_smoke")
public final class DesignHardeningSmoke {
    private static boolean faultFood, faultDurability;
    private static int calls;
    private static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, "travail_smoke");
    private static final RegistryObject<Item> FOOD =
            ITEMS.register(
                    "fault_food",
                    () ->
                            new Item(
                                    new Item.Properties()
                                            .food(
                                                    new FoodProperties.Builder()
                                                            .nutrition(20)
                                                            .saturationMod(1)
                                                            .build())) {
                                @Override
                                public FoodProperties getFoodProperties(
                                        ItemStack stack, LivingEntity eater) {
                                    calls++;
                                    if (faultFood)
                                        throw new IllegalStateException("injected food query");
                                    return super.getFoodProperties(stack, eater);
                                }
                            });
    private static final RegistryObject<Item> TOOL =
            ITEMS.register(
                    "fault_tool",
                    () ->
                            new Item(new Item.Properties().durability(100)) {
                                @Override
                                public int getMaxDamage(ItemStack stack) {
                                    if (faultDurability)
                                        throw new IllegalStateException(
                                                "injected durability query");
                                    return 100;
                                }
                            });
    private MinecraftServer server;
    private boolean done;
    private int checks;
    private final List<String> evidence = new ArrayList<>();

    public DesignHardeningSmoke() {
        ITEMS.register(
                net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext.get().getModEventBus());
        MinecraftForge.EVENT_BUS.addListener((ServerStartedEvent e) -> server = e.getServer());
        MinecraftForge.EVENT_BUS.addListener(this::tick);
    }

    private void check(boolean ok, String label) {
        if (!ok) throw new AssertionError(label);
        checks++;
    }

    private void tick(TickEvent.ServerTickEvent event) {
        if (server == null || done || event.phase != TickEvent.Phase.END) return;
        done = true;
        String result;
        try {
            run();
            result = "PASS checks=" + checks;
        } catch (Throwable failure) {
            failure.printStackTrace();
            result = "FAIL checks=" + checks + " " + failure;
        }
        try {
            Files.writeString(Path.of("design-result.txt"), result);
            Files.write(Path.of("design-evidence.txt"), evidence);
        } catch (Exception failure) {
            throw new RuntimeException(failure);
        }
        System.out.println("DESIGN_HARDENING " + result);
        server.halt(false);
    }

    private static class Player extends ServerPlayer {
        Player(ServerLevel level) {
            super(level.getServer(), level, new GameProfile(UUID.randomUUID(), "DesignProbe"));
            var wire =
                    new net.minecraft.network.Connection(
                            net.minecraft.network.protocol.PacketFlow.SERVERBOUND) {
                        @Override
                        public void send(net.minecraft.network.protocol.Packet<?> packet) {}
                    };
            connection =
                    new net.minecraft.server.network.ServerGamePacketListenerImpl(
                            level.getServer(), wire, this) {
                        @Override
                        public void send(net.minecraft.network.protocol.Packet<?> packet) {}
                    };
        }
    }

    private static EffectDurationAccessor access(MobEffectInstance e) {
        return (EffectDurationAccessor) e;
    }

    private MobEffectInstance chain(int count) {
        MobEffectInstance tail = null;
        for (int n = count - 1; n >= 0; n--) {
            var next =
                    new MobEffectInstance(
                            MobEffects.POISON,
                            n % 3 == 0 ? -1 : 101 + n,
                            n % 4,
                            n % 2 == 0,
                            n % 2 != 0,
                            true);
            access(next).travail$hiddenEffect(tail);
            tail = next;
        }
        return tail;
    }

    private void compare(MobEffectInstance input, MobEffectInstance output, double multiplier) {
        while (input != null) {
            check(output != null && output != input, "independent full chain");
            check(
                    input.getEffect() == output.getEffect()
                            && input.getAmplifier() == output.getAmplifier(),
                    "effect and level retained");
            check(
                    input.isAmbient() == output.isAmbient()
                            && input.isVisible() == output.isVisible()
                            && input.showIcon() == output.showIcon(),
                    "flags retained");
            check(
                    output.getDuration()
                            == (input.getDuration() > 0
                                    ? (int) Math.ceil(input.getDuration() * multiplier)
                                    : input.getDuration()),
                    "duration including infinite");
            input = access(input).travail$hiddenEffect();
            output = access(output).travail$hiddenEffect();
        }
        check(output == null, "no extra nodes");
    }

    private void run() throws Exception {
        var level = server.overworld();
        var player = new Player(level);
        var inv = CuriosApi.getCuriosInventory(player).resolve().orElseThrow();
        var slots =
                new HashMap<
                        String, top.theillusivec4.curios.api.type.inventory.ICurioStacksHandler>();
        for (String id : List.of("hands", "head", "travel_diary", "back"))
            slots.put(
                    id,
                    new CurioStacksHandler(inv, id, 1, true, false, true, ICurio.DropRule.DEFAULT));
        inv.setCurios(slots);
        golden(player);
        goldenConversion();
        bud(player);
        effects(player, slots);
        icarus();
        evidence.add(
                "Server world="
                        + level.dimension().location()
                        + "; simulated player connection, no real client packets");
    }

    private void golden(Player player) {
        var old = FarReachItemsConfig.FOOD_BLACKLIST.get();
        try {
            FarReachItemsConfig.FOOD_BLACKLIST.set(List.of());
            var clean = GoldenAgePools.foods(player);
            check(
                    clean.stream().anyMatch(s -> s.is(FOOD.get())),
                    "fault food is normally eligible");
            var expected =
                    clean.stream()
                            .filter(s -> !s.is(FOOD.get()))
                            .map(s -> ForgeRegistries.ITEMS.getKey(s.getItem()))
                            .toList();
            faultFood = true;
            for (int n = 0; n < 100; n++) {
                var actual =
                        GoldenAgePools.foods(player).stream()
                                .map(s -> ForgeRegistries.ITEMS.getKey(s.getItem()))
                                .toList();
                check(
                        actual.equals(expected) && !actual.isEmpty(),
                        "bad food skipped; valid order and entries unchanged");
            }
            var second = new Player(player.serverLevel());
            check(
                    GoldenAgePools.foods(second).size() == expected.size(),
                    "other player retains valid pool");
            check(!GoldenAgePools.enchantments().isEmpty(), "enchantment pool unaffected");
            FarReachItemsConfig.FOOD_BLACKLIST.set(List.of("travail_smoke:fault_food"));
            int before = calls;
            GoldenAgePools.foods(player);
            check(calls == before, "blacklist excludes before callback");
            FarReachItemsConfig.FOOD_BLACKLIST.set(List.of());
            faultFood = false;
            check(
                    GoldenAgePools.foods(player).stream().anyMatch(s -> s.is(FOOD.get())),
                    "repaired item rejoins without restart");
            evidence.add(
                    "Golden Age: 100 complete registry sweeps with injected query failure; exact"
                            + " healthy pool order retained; recovery/blacklist/second player"
                            + " verified");
        } finally {
            faultFood = false;
            FarReachItemsConfig.FOOD_BLACKLIST.set(old);
            GoldenAgePools.clear();
        }
    }

    private void goldenConversion() {
        var chance = FarReachItemsConfig.NUMBERS.get("golden_age.foodChance");
        double oldChance = chance.get();
        var level = server.overworld();
        var pos = new net.minecraft.core.BlockPos(0, 120, 0);
        level.getChunkAt(pos);
        try {
            faultFood = true;
            for (double probability : new double[] {0, 1}) {
                chance.set(probability);
                var player = new Player(level);
                player.setPos(0, 120, 0);
                player.getInventory().setItem(0, new ItemStack(ModRegistry.GOLDEN_AGE.get()));
                var inv = CuriosApi.getCuriosInventory(player).resolve().orElseThrow();
                var slot =
                        new CurioStacksHandler(
                                inv, "travel_diary", 1, true, false, true, ICurio.DropRule.DEFAULT);
                inv.setCurios(Map.of("travel_diary", slot));
                var diary = new ItemStack(ModRegistry.LONG_TRAVAIL.get());
                com.thelongtravail.journey.LongTravailData.tryInitialize(diary, player);
                com.thelongtravail.journey.LongTravailData.setWitness(
                        diary, com.thelongtravail.TravailAspect.FAR_REACH, true);
                slot.getStacks().setStackInSlot(0, diary);
                level.players().add(player);
                try {
                    check(
                            com.thelongtravail.api.TravailStateApi.hasFarReachWitness(player),
                            "conversion witness active");
                    var gold =
                            new net.minecraft.world.entity.item.ItemEntity(
                                    level, 0, 120, 0, new ItemStack(Items.GOLD_BLOCK)) {
                                @Override
                                public boolean isInWater() {
                                    return true;
                                }
                            };
                    GoldenAgeActions.register(gold, player);
                    gold.getPersistentData()
                            .putInt(
                                    "LongTravailGoldWetTicks",
                                    FarReachItemsConfig.ticks("golden_age.waterTime"));
                    var area = new net.minecraft.world.phys.AABB(pos).inflate(2);
                    var before =
                            level.getEntitiesOfClass(
                                    net.minecraft.world.entity.item.ItemEntity.class, area);
                    GoldenAgeActions.tick(gold);
                    var output =
                            level.getEntitiesOfClass(
                                    net.minecraft.world.entity.item.ItemEntity.class, area);
                    output.removeAll(before);
                    check(
                            gold.getItem().isEmpty() && output.size() == 1,
                            "bad food does not block actual gold conversion");
                    var stack = output.get(0).getItem();
                    check(
                            probability == 0
                                    ? stack.is(Items.HONEY_BLOCK)
                                    : GoldenAgePools.foods(player).stream()
                                            .anyMatch(s -> s.is(stack.getItem())),
                            "honey and healthy food branches retained");
                    output.forEach(Entity::discard);
                } finally {
                    level.players().remove(player);
                }
            }
            evidence.add(
                    "Golden Age: real conversion dispatch at foodChance 0 and 1; one gold consumed"
                        + " only after honey/healthy food entity is generated despite faulty food");
        } finally {
            faultFood = false;
            chance.set(oldChance);
        }
    }

    private void bud(Player player) {
        var recipe =
                new ImmortalBudRecipe(
                        new ResourceLocation("travail_smoke", "bud"), CraftingBookCategory.MISC);
        var grid = new TransientCraftingContainer(player.inventoryMenu, 2, 2);
        grid.setItem(0, new ItemStack(ModRegistry.IMMORTAL_BUD.get()));
        var target = new ItemStack(TOOL.get());
        target.setDamageValue(50);
        grid.setItem(1, target);
        faultDurability = true;
        try {
            for (int n = 0; n < 500; n++) {
                check(!recipe.matches(grid, player.level()), "invalid durability rejects match");
                check(
                        recipe.assemble(grid, player.level().registryAccess()).isEmpty(),
                        "invalid durability rejects assembly");
                check(
                        grid.getItem(1) == target
                                && target.getCount() == 1
                                && grid.getItem(0).getCount() == 1,
                        "inputs retained");
            }
        } finally {
            faultDurability = false;
        }
        check(recipe.matches(grid, player.level()), "recovered tool accepted");
        var out = recipe.assemble(grid, player.level().registryAccess());
        check(
                ImmortalBud.marked(out)
                        && out.getDamageValue() == 0
                        && target.getDamageValue() == 50,
                "repair on copy only");
        target = new ItemStack(Items.DIAMOND_SWORD);
        target.setDamageValue(400);
        grid.setItem(1, target);
        out = recipe.assemble(grid, player.level().registryAccess());
        check(
                ImmortalBud.marked(out)
                        && out.getDamageValue() == 0
                        && target.getDamageValue() == 400,
                "vanilla tool semantics");
        evidence.add(
                "Immortal Bud: 500 match/assemble failures preserve input; valid custom and vanilla"
                        + " tool recovery");
    }

    private void effects(
            Player player,
            Map<String, top.theillusivec4.curios.api.type.inventory.ICurioStacksHandler> slots) {
        for (int size : new int[] {1, 2, 16, 128}) {
            var input = chain(size);
            compare(input, EffectCopies.copy(input), 1);
            compare(input, EffectCopies.shortened(input, .8), .8);
        }
        var cycle = chain(2);
        access(cycle).travail$duration(200);
        access(access(cycle).travail$hiddenEffect()).travail$hiddenEffect(cycle);
        for (var bad : List.of(cycle, chain(129))) {
            boolean rejected = false;
            try {
                EffectCopies.copy(bad);
            } catch (EffectCopies.InvalidChain expected) {
                rejected = true;
            }
            check(rejected, "cyclic or over-limit input rejected");
        }
        slots.get("hands")
                .getStacks()
                .setStackInSlot(0, new ItemStack(ModRegistry.MIDNIGHT_LAFITE.get()));
        for (int n = 0; n < 500; n++)
            check(!player.addEffect(cycle), "real addEffect rejects bad lafite chain");
        check(!player.hasEffect(MobEffects.POISON), "no partial effect applied");
        var normal = new MobEffectInstance(MobEffects.POISON, 200, 1);
        check(player.addEffect(normal), "lafite normal effect accepted");
        check(
                player.getEffect(MobEffects.POISON).getDuration()
                        == (int)
                                Math.max(
                                        1,
                                        Math.ceil(
                                                200
                                                        * AbyssItemsConfig.get(
                                                                "poisonDurationMultiplier"))),
                "lafite duration unchanged");
        player.removeAllEffects();
        slots.get("hands").getStacks().setStackInSlot(0, ItemStack.EMPTY);
        var crown = new ItemStack(ModRegistry.SPRING_CORNER.get());
        FlowerData.write(crown, List.of(Flower.SPORE_BLOSSOM));
        slots.get("head").getStacks().setStackInSlot(0, crown);
        for (int n = 0; n < 500; n++)
            check(!player.addEffect(cycle), "real addEffect rejects bad spore chain");
        check(!player.hasEffect(MobEffects.POISON), "spore no partial effect");
        check(
                player.addEffect(new MobEffectInstance(MobEffects.POISON, 200)),
                "normal spore effect accepted");
        check(
                player.getEffect(MobEffects.POISON).getDuration()
                        == (int)
                                Math.ceil(
                                        200
                                                * (1
                                                        - FlourishingItemsConfig.value(
                                                                Flower.SPORE_BLOSSOM))),
                "spore normal duration");
        player.removeAllEffects();
        slots.get("head").getStacks().setStackInSlot(0, ItemStack.EMPTY);
        evidence.add(
                "Effects: copied normal/infinite chains through 128 nodes; cycle/129 rejected; 1000"
                    + " real addEffect rejection calls, normal Lafite/Spore durations preserved");
    }

    private void icarus() {
        var end = server.getLevel(Level.END);
        int[] positionReads = {0};
        var cow =
                new net.minecraft.world.entity.animal.Cow(EntityType.COW, end) {
                    @Override
                    public double getEyeY() {
                        positionReads[0]++;
                        return super.getEyeY();
                    }
                };
        cow.setPos(0, end.getMaxBuildHeight() + 1, 0);
        for (int n = 0; n < 10000; n++)
            check(IcarusCombat.maliceBonus(cow, 0) == 0, "zero sunlight result");
        check(positionReads[0] == 0, "zero sunlight avoids environment coordinates");
        check(
                IcarusCombat.maliceBonus(cow, 1)
                        == FarReachItemsConfig.get("icarus.malicePeak")
                                + FarReachItemsConfig.get("icarus.sunnyExtraPeak"),
                "daytime open-sky result retained");
        check(positionReads[0] > 0, "nonzero branch still queries environment");
        check(
                IcarusCombat.witnessBonus(cow) == FarReachItemsConfig.get("icarus.witnessBonus"),
                "witness retained");
        evidence.add(
                "Icarus: 10000 zero-sun calls with position-read sentinel; positive-sun and witness"
                        + " retained");
    }
}
