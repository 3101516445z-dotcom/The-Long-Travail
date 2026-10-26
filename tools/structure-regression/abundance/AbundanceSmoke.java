package travail.abundance;

import com.mojang.authlib.GameProfile;
import com.thelongtravail.abyss.*;
import com.thelongtravail.config.AbyssItemsConfig;
import com.thelongtravail.registry.ModRegistry;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.*;
import net.minecraft.world.entity.AreaEffectCloud;
import net.minecraft.world.entity.projectile.ThrownPotion;
import net.minecraft.world.inventory.TransientCraftingContainer;
import net.minecraft.world.item.*;
import net.minecraft.world.item.alchemy.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BrewingStandBlockEntity;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.brewing.BrewingRecipeRegistry;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

import java.nio.file.*;
import java.util.*;

@Mod("travail_smoke")
public final class AbundanceSmoke {
    private MinecraftServer server;
    private int checks;
    private boolean done;

    public AbundanceSmoke() {
        MinecraftForge.EVENT_BUS.addListener(this::start);
        MinecraftForge.EVENT_BUS.addListener(this::tick);
    }

    private void start(ServerStartedEvent event) {
        server = event.getServer();
    }

    private void check(boolean condition, String message) {
        checks++;
        if (!condition) throw new AssertionError(message);
    }

    private void tick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || server == null || done) return;
        done = true;
        String result;
        try {
            run();
            result = "PASS checks=" + checks;
        } catch (Throwable error) {
            error.printStackTrace();
            result = "FAIL checks=" + checks + " " + error;
        }
        System.out.println("ABUNDANCE " + result);
        try {
            Files.writeString(Path.of("abundance-result.txt"), result);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        server.halt(false);
    }

    private void run() throws Exception {
        var speed = MobEffects.MOVEMENT_SPEED;
        var heal = MobEffects.HEAL;
        var glow = MobEffects.GLOWING;
        List<Potion> samples =
                List.of(
                        new Potion(new MobEffectInstance(speed, 100, 0)),
                        new Potion(new MobEffectInstance(speed, 100, 0)),
                        new Potion(new MobEffectInstance(speed, 200, 0)),
                        new Potion(
                                new MobEffectInstance(speed, 50, 1),
                                new MobEffectInstance(heal, 0, 1)));
        var table = AbundanceEffectPool.build(List.of(speed, heal, glow), samples, Set.of(), 300);
        check(table.size() == 3, "effect-level sampling, not potion count");
        check(table.get(0).levels().size() == 2, "deduplicated levels");
        check(
                table.get(0).levels().get(0).durations().equals(List.of(100, 200)),
                "deduplicated durations");
        check(
                table.get(0).levels().get(1).durations().equals(List.of(50)),
                "level/duration association");
        check(table.get(1).levels().get(0).durations().equals(List.of(1)), "instant placeholder");
        check(table.get(2).levels().get(0).durations().equals(List.of(300)), "15-second fallback");
        var random = RandomSource.create(54321);
        Set<String> seen = new HashSet<>();
        for (int i = 0; i < 2000; i++) {
            var e = AbundanceEffectPool.sample(table, random);
            if (e.getEffect() == speed)
                check(
                        e.getAmplifier() == 0
                                ? Set.of(100, 200).contains(e.getDuration())
                                : e.getDuration() == 50,
                        "valid speed tuple");
            else if (e.getEffect() == heal)
                check(e.getAmplifier() == 1 && e.getDuration() == 1, "instant tuple");
            else
                check(
                        e.getEffect() == glow && e.getAmplifier() == 0 && e.getDuration() == 300,
                        "fallback tuple");
            seen.add(
                    e.getEffect().getDescriptionId()
                            + ":"
                            + e.getAmplifier()
                            + ":"
                            + e.getDuration());
        }
        check(seen.size() == 5, "all supported tuples reached");
        check(AbundanceEffectPool.sample(List.of(), random) == null, "empty pool");
        check(
                AbundanceEffectPool.build(List.of(speed), samples, Set.of(speed), 300).isEmpty(),
                "blacklist");
        check(
                AbundanceEffectPool.ticks(2) == 40 && AbundanceEffectPool.ticks(15) == 300,
                "seconds conversion");
        var infinite = new MobEffectInstance(speed, -1, 0);
        check(
                AbundanceEffectPool.build(
                                List.of(speed), List.of(new Potion(infinite)), Set.of(), 300)
                        .get(0)
                        .levels()
                        .get(0)
                        .durations()
                        .equals(List.of(-1)),
                "infinite sample");
        check(
                AbundanceEffectPool.build(
                                        List.of(speed),
                                        List.of(new Potion(new MobEffectInstance(speed, 0, 3))),
                                        Set.of(),
                                        300)
                                .get(0)
                                .levels()
                                .get(0)
                                .amplifier()
                        == 0,
                "invalid sample fallback");

        var world = server.overworld();
        var player =
                FakePlayerFactory.get(
                        world,
                        new GameProfile(
                                UUID.fromString("18679523-4d7b-4d43-9fc3-47e5e42c731e"),
                                "AbundanceTest"));
        player.setPos(0, 80, 0);
        var item = ModRegistry.LIQUEFIED_ABUNDANCE.get();
        check(item.getMaxStackSize() == 64, "stack size");
        check(item.getUseAnimation(new ItemStack(item)) == UseAnim.BOW, "bow animation");
        for (var hand : InteractionHand.values()) {
            for (boolean sneak : List.of(false, true)) {
                for (int mode = 0; mode < 3; mode++) {
                    player.getInventory().clearContent();
                    player.getPersistentData().putInt(LiquefiedAbundance.MODE_KEY, mode);
                    player.setShiftKeyDown(sneak);
                    var input = new ItemStack(item, 64);
                    player.setItemInHand(hand, input);
                    item.use(world, player, hand);
                    item.releaseUsing(input, world, player, 35);
                    check(
                            LiquefiedAbundance.mode(player) == (mode + 1) % 3
                                    && input.getCount() == 64,
                            "five ticks switches without consuming");
                    player.stopUsingItem();
                    item.use(world, player, hand);
                    item.releaseUsing(input, world, player, 34);
                    check(
                            LiquefiedAbundance.mode(player) == (mode + 1) % 3
                                    && input.getCount() == 64,
                            "six ticks cancels");
                    player.stopUsingItem();
                    item.use(world, player, hand);
                    var result = item.finishUsingItem(input, world, player);
                    var expected =
                            switch ((mode + 1) % 3) {
                                case 1 -> Items.SPLASH_POTION;
                                case 2 -> Items.LINGERING_POTION;
                                default -> Items.POTION;
                            };
                    check(
                            input.getCount() == 63
                                    && player.getInventory().countItem(expected) == 1,
                            "selected type ignores sneak and hand");
                    player.stopUsingItem();
                }
            }
        }
        var updateBoundary =
                net.minecraft.world.entity.LivingEntity.class.getDeclaredMethod(
                        "updateUsingItem", ItemStack.class);
        updateBoundary.setAccessible(true);
        double oldHold = AbyssItemsConfig.ABUNDANCE_HOLD.get();
        for (double seconds : List.of(0.5, 2.0, 5.0, 10.0)) {
            AbyssItemsConfig.ABUNDANCE_HOLD.set(seconds);
            int total = (int) Math.ceil(seconds * 20);
            check(LiquefiedAbundance.useTicks() == total, "config duration");
            player.getInventory().clearContent();
            var input = new ItemStack(item, 64);
            player.setItemInHand(InteractionHand.MAIN_HAND, input);
            item.use(world, player, InteractionHand.MAIN_HAND);
            for (int tick = 0; tick < total - 1; tick++) updateBoundary.invoke(player, input);
            check(input.getCount() == 64, "not early at configured threshold");
            updateBoundary.invoke(player, input);
            check(input.getCount() == 63, "exact configured threshold completes");
            player.stopUsingItem();
        }
        AbyssItemsConfig.ABUNDANCE_HOLD.set(oldHold);
        player.getPersistentData().putInt(LiquefiedAbundance.MODE_KEY, 2);
        var persistent = player.saveWithoutId(new CompoundTag());
        check(
                persistent.getCompound("ForgeData").getInt(LiquefiedAbundance.MODE_KEY) == 2,
                "mode serialized in player NBT");
        var inputReload = new ItemStack(item, 64);
        player.setItemInHand(InteractionHand.MAIN_HAND, inputReload);
        item.use(world, player, InteractionHand.MAIN_HAND);
        LiquefiedAbundance.reload();
        item.releaseUsing(inputReload, world, player, 39);
        check(
                LiquefiedAbundance.mode(player) == 2 && inputReload.getCount() == 64,
                "reload cancels without switching");
        player.getInventory().clearContent();
        var stack = new ItemStack(item, 64);
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        item.use(world, player, InteractionHand.MAIN_HAND);
        item.finishUsingItem(stack, world, player);
        check(stack.getCount() == 63, "long consumes one");
        check(
                player.getInventory().countItem(Items.LINGERING_POTION) == 1,
                "long gives one lingering");
        player.releaseUsingItem();
        check(
                stack.getCount() == 63
                        && player.getInventory().countItem(Items.LINGERING_POTION) == 1,
                "release cannot double settle");
        item.use(world, player, InteractionHand.MAIN_HAND);
        LiquefiedAbundance.cancel(player);
        player.releaseUsingItem();
        check(stack.getCount() == 63, "cancel does not consume");
        item.use(world, player, InteractionHand.MAIN_HAND);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.STONE));
        item.releaseUsing(stack, world, player, 20);
        check(
                stack.getCount() == 63 && player.getMainHandItem().is(Items.STONE),
                "switch preserves items");
        player.stopUsingItem();
        player.getInventory().clearContent();
        stack = new ItemStack(item, 2);
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        item.use(world, player, InteractionHand.MAIN_HAND);
        var updateUse =
                net.minecraft.world.entity.LivingEntity.class.getDeclaredMethod(
                        "updateUsingItem", ItemStack.class);
        updateUse.setAccessible(true);
        for (int i = 0; i < 39; i++) updateUse.invoke(player, stack);
        check(stack.getCount() == 2 && player.isUsingItem(), "39 ticks do not complete");
        updateUse.invoke(player, stack);
        check(stack.getCount() == 1 && !player.isUsingItem(), "40th tick completes exactly once");
        check(
                player.getInventory().countItem(Items.LINGERING_POTION) == 1,
                "real vanilla finish delivery");
        player.getInventory().clearContent();
        player.getAbilities().instabuild = true;
        stack = new ItemStack(item);
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        for (int i = 0; i < 2; i++) {
            item.use(world, player, InteractionHand.MAIN_HAND);
            item.finishUsingItem(stack, world, player);
            player.stopUsingItem();
        }
        check(
                stack.getCount() == 1
                        && player.getInventory().countItem(Items.LINGERING_POTION) == 2,
                "creative always delivers");
        player.getAbilities().instabuild = false;
        for (int i = 0; i < player.getInventory().items.size(); i++)
            player.getInventory().items.set(i, new ItemStack(Items.STONE, 64));
        stack = new ItemStack(item, 2);
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        int beforeDrops =
                world.getEntitiesOfClass(
                                net.minecraft.world.entity.item.ItemEntity.class,
                                player.getBoundingBox().inflate(5))
                        .size();
        item.use(world, player, InteractionHand.MAIN_HAND);
        item.finishUsingItem(stack, world, player);
        check(stack.getCount() == 1, "full inventory consumes one");
        check(
                world.getEntitiesOfClass(
                                        net.minecraft.world.entity.item.ItemEntity.class,
                                        player.getBoundingBox().inflate(5))
                                .size()
                        == beforeDrops + 1,
                "full inventory drops result");
        player.stopUsingItem();
        player.getAbilities().instabuild = true;
        int creativeCount = stack.getCount();
        int creativeDrops =
                world.getEntitiesOfClass(
                                net.minecraft.world.entity.item.ItemEntity.class,
                                player.getBoundingBox().inflate(5))
                        .size();
        item.use(world, player, InteractionHand.MAIN_HAND);
        item.finishUsingItem(stack, world, player);
        check(stack.getCount() == creativeCount, "creative full inventory preserves input");
        check(
                world.getEntitiesOfClass(
                                        net.minecraft.world.entity.item.ItemEntity.class,
                                        player.getBoundingBox().inflate(5))
                                .size()
                        == creativeDrops + 1,
                "creative full inventory drops output");
        player.stopUsingItem();
        player.getAbilities().instabuild = false;
        var savedBlacklist = AbyssItemsConfig.ABUNDANCE_BLACKLIST.get();
        AbyssItemsConfig.ABUNDANCE_BLACKLIST.set(
                ForgeRegistries.MOB_EFFECTS.getKeys().stream().map(Object::toString).toList());
        LiquefiedAbundance.reload();
        item.use(world, player, InteractionHand.MAIN_HAND);
        item.finishUsingItem(stack, world, player);
        check(stack.getCount() == 1, "empty live pool preserves input");
        AbyssItemsConfig.ABUNDANCE_BLACKLIST.set(savedBlacklist);
        LiquefiedAbundance.reload();
        player.stopUsingItem();

        var effect = new MobEffectInstance(speed, 300, 0);
        var marked = LiquefiedAbundance.potion(Items.LINGERING_POTION, effect);
        check(
                PotionUtils.getMobEffects(marked).get(0).getDuration() == 300,
                "bottle retains full duration");
        check(LiquefiedAbundance.cloudEffect(marked, effect).getDuration() == 75, "cloud quarter");
        check(
                LiquefiedAbundance.cloudEffect(marked, infinite).isInfiniteDuration(),
                "infinite cloud");
        var instant = new MobEffectInstance(heal, 1, 1);
        check(LiquefiedAbundance.cloudEffect(marked, instant) == instant, "instant unchanged");
        check(
                LiquefiedAbundance.cloudEffect(new ItemStack(Items.LINGERING_POTION), effect)
                        == effect,
                "other custom potions unchanged");
        var thrown = new ThrownPotion(world, player);
        for (var old :
                world.getEntitiesOfClass(AreaEffectCloud.class, new AABB(-5, 75, -5, 5, 85, 5)))
            old.discard();
        thrown.setPos(0, 80, 0);
        thrown.setItem(marked);
        var method =
                ThrownPotion.class.getDeclaredMethod(
                        "makeAreaOfEffectCloud", ItemStack.class, Potion.class);
        method.setAccessible(true);
        method.invoke(thrown, marked, Potions.EMPTY);
        var clouds =
                world.getEntitiesOfClass(AreaEffectCloud.class, new AABB(-5, 75, -5, 5, 85, 5));
        check(clouds.size() == 1, "actual thrown potion creates cloud with mixin applied");
        CompoundTag nbt = new CompoundTag();
        clouds.get(0).saveWithoutId(nbt);
        check(
                MobEffectInstance.load(nbt.getList("Effects", 10).getCompound(0)).getDuration()
                        == 75,
                "actual cloud NBT quarter duration");
        var restored = new AreaEffectCloud(world, 0, 80, 0);
        restored.load(nbt);
        CompoundTag savedAgain = new CompoundTag();
        restored.saveWithoutId(savedAgain);
        check(
                MobEffectInstance.load(savedAgain.getList("Effects", 10).getCompound(0))
                                .getDuration()
                        == 75,
                "reload does not quarter twice");

        check(
                BrewingRecipeRegistry.isValidInput(new ItemStack(Items.DRAGON_BREATH)),
                "dragon breath accepted");
        check(
                BrewingRecipeRegistry.getOutput(
                                new ItemStack(Items.DRAGON_BREATH),
                                new ItemStack(Items.NETHER_WART))
                        .is(item),
                "brew recipe output");
        check(
                BrewingRecipeRegistry.getOutput(
                                new ItemStack(Items.DRAGON_BREATH, 2),
                                new ItemStack(Items.NETHER_WART))
                        .isEmpty(),
                "bottle count guard");
        BlockPos pos = new BlockPos(8, 80, 8);
        world.setBlockAndUpdate(pos, Blocks.BREWING_STAND.defaultBlockState());
        var stand = (BrewingStandBlockEntity) world.getBlockEntity(pos);
        stand.clearContent();
        check(
                stand.canPlaceItem(0, new ItemStack(Items.DRAGON_BREATH)),
                "automated bottle insertion");
        for (int i = 0; i < 3; i++) stand.setItem(i, new ItemStack(Items.DRAGON_BREATH));
        stand.setItem(3, new ItemStack(Items.NETHER_WART));
        stand.setItem(4, new ItemStack(Items.BLAZE_POWDER));
        for (int i = 0; i < 402; i++)
            BrewingStandBlockEntity.serverTick(world, pos, world.getBlockState(pos), stand);
        for (int i = 0; i < 3; i++)
            check(
                    stand.getItem(i).is(item) && stand.getItem(i).getCount() == 1,
                    "actual brew bottle " + i);
        check(stand.getItem(3).isEmpty(), "one wart per batch");
        var crafting = new TransientCraftingContainer(player.inventoryMenu, 3, 3);
        crafting.setItem(0, new ItemStack(Items.HONEY_BOTTLE));
        crafting.setItem(4, new ItemStack(Items.LAPIS_LAZULI));
        crafting.setItem(8, new ItemStack(Items.MILK_BUCKET));
        var recipe =
                (net.minecraft.world.item.crafting.CraftingRecipe)
                        server.getRecipeManager()
                                .byKey(new ResourceLocation("the_long_travail:liquefied_abundance"))
                                .orElseThrow();
        check(recipe.matches(crafting, world), "shapeless match");
        check(recipe.assemble(crafting, world.registryAccess()).is(item), "craft output");
        var remainders = recipe.getRemainingItems(crafting);
        check(
                remainders.get(0).is(Items.GLASS_BOTTLE) && remainders.get(8).is(Items.BUCKET),
                "craft container returns");
    }
}
