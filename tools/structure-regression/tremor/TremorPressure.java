package travail.smoke;

import com.google.gson.*;
import com.thelongtravail.TravailAspect;
import com.thelongtravail.api.TravailStateApi.AspectState;
import com.thelongtravail.compat.ftbultimine.TremorUltimine;
import com.thelongtravail.config.DeepValleyItemsConfig;
import com.thelongtravail.data.LongTravailData;
import com.thelongtravail.registry.ModRegistry;
import com.thelongtravail.valley.*;

import net.minecraft.core.*;
import net.minecraft.nbt.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.*;
import net.minecraft.world.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.item.enchantment.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.phys.*;
import net.minecraftforge.common.*;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.*;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.*;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.registries.ForgeRegistries;

import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.type.capability.ICurio;
import top.theillusivec4.curios.common.inventory.CurioStacksHandler;

import java.nio.file.*;
import java.util.*;

/** 按实际 tick 分批执行高次数效果检查，不作实体性能基准。 */
public final class TremorPressure {
    private static TremorPressure current;
    private final MinecraftServer server;
    private final ServerLevel level;
    private final BlockPos origin = new BlockPos(128, 100, 128);
    private final AABB arena = new AABB(120, 96, 120, 168, 108, 152);
    private final List<ServerPlayer> players = new ArrayList<>();
    private final List<ItemStack> diaries = new ArrayList<>();
    private final Map<String, Long> counters = new LinkedHashMap<>();
    private final Map<String, Long> expected = new TreeMap<>();
    private final Map<BlockPos, Integer> rolls = new HashMap<>();
    private final List<BlockPos> placed = new ArrayList<>();
    private final List<Map<String, Object>> stages = new ArrayList<>();
    private int phase, index, ticks, expectedXp, fault;
    private long checks, operation;
    private boolean observing, cancelBreak, cancelUse, denyBlock, denyItem, recurse;
    private boolean failOnce;
    private final boolean ftb = ModList.get().isLoaded("ftbultimine");
    private final String[] names = {
        "ordinary",
        "containers",
        "crafting",
        "state",
        "faults",
        "interleaved",
        "ftb",
        "compat",
        "persistence"
    };
    private final int[] targets = {100000, 10000, 10000, 10000, 600, 4000, 1000, 1, 1};
    private final int[] batches = {128, 24, 128, 64, 8, 16, 4, 1, 1};

    private TremorPressure(MinecraftServer server, int baseline) {
        this.server = server;
        this.level = server.overworld();
        checks = baseline;
        counters.put("baselineAssertions", (long) baseline);
        if (!ftb) targets[6] = 0;
        if (Boolean.getBoolean("travail.tremor.targeted"))
            for (int i = 0; i < 7; i++) targets[i] = 0;
        if (Boolean.getBoolean("travail.tremor.chainOnly"))
            for (int i = 0; i < 6; i++) targets[i] = 0;
        if (Boolean.getBoolean("travail.tremor.interleavedRetest")) targets[5] = 4000;
        for (int x = 7; x <= 10; x++)
            for (int z = 7; z <= 9; z++) {
                level.setChunkForced(x, z, true);
                level.getChunk(x, z);
            }
        for (int i = 0; i < 32; i++) {
            var p = new TremorSmoke.TestPlayer(level);
            p.setGameMode(GameType.SURVIVAL);
            p.setShiftKeyDown(true);
            p.setPos(origin.getX() + .5, origin.getY() + 1, origin.getZ() + .5);
            p.setXRot(90);
            p.setYRot(0);
            p.setItemInHand(
                    InteractionHand.MAIN_HAND,
                    new ItemStack(ModRegistry.HIGH_FREQUENCY_TREMOR.get()));
            var inv = CuriosApi.getCuriosInventory(p).resolve().orElseThrow();
            var slot =
                    new CurioStacksHandler(
                            inv, "travel_diary", 1, true, false, true, ICurio.DropRule.DEFAULT);
            inv.setCurios(new HashMap<>(Map.of("travel_diary", slot)));
            var diary = new ItemStack(ModRegistry.LONG_TRAVAIL.get());
            slot.getStacks().setStackInSlot(0, diary);
            players.add(p);
            diaries.add(diary);
        }
    }

    public static void begin(MinecraftServer server, int baseline) throws Exception {
        current = new TremorPressure(server, baseline);
        MinecraftForge.EVENT_BUS.register(current);
        Files.writeString(Path.of("pressure-result.json"), "{\"status\":\"RUNNING\"}");
        System.out.println("TREMOR_PRESSURE_BEGIN ftb=" + current.ftb);
    }

    public static void failed(MinecraftServer server, Throwable error) {
        error.printStackTrace();
        try {
            Files.writeString(Path.of("tremor-result.txt"), "FAIL " + error);
            if (current != null) current.report("FAIL", error.toString());
        } catch (Exception e) {
            e.printStackTrace();
        }
        server.halt(false);
    }

    public static void observe(List<ItemStack> drops, ServerLevel level, BlockPos pos) {
        var c = current;
        if (c == null || !c.observing || c.level != level || !c.placed.contains(pos)) return;
        c.rolls.merge(pos.immutable(), 1, Integer::sum);
        if (c.fault == 2 && !c.failOnce) {
            c.failOnce = true;
            throw new IllegalStateException("TREMOR_INJECTED_LOOT_FAILURE");
        }
        for (ItemStack s : drops) c.add(c.expected, s, 1);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void breaking(BlockEvent.BreakEvent event) {
        if (!observing || event.getLevel() != level || !placed.contains(event.getPos())) return;
        if (fault == 1 && !failOnce) {
            failOnce = true;
            throw new IllegalStateException("TREMOR_INJECTED_EVENT_FAILURE");
        }
        if (cancelBreak) event.setCanceled(true);
        if (recurse) {
            check(
                    !PlayerBlockBreaker.breakBlock(
                            (ServerPlayer) event.getPlayer(), event.getPos(), AspectState.MALICE),
                    "recursive request rejected");
        }
        if (!event.isCanceled() && !event.getPlayer().isCreative())
            expectedXp += event.getExpToDrop();
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void interacting(PlayerInteractEvent.RightClickBlock event) {
        if (!observing || event.getLevel() != level) return;
        if (cancelUse) event.setCanceled(true);
        if (denyBlock) event.setUseBlock(Event.Result.DENY);
        if (denyItem) event.setUseItem(Event.Result.DENY);
    }

    @SubscribeEvent
    public void tick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || current != this) return;
        try {
            ticks++;
            if (ticks % 200 == 0) {
                report("RUNNING", "");
                System.out.println("TREMOR_PROGRESS phase=" + names[phase] + " index=" + index);
            }
            for (int i = 0; i < batches[phase] && index < targets[phase]; i++, index++) {
                operation++;
                switch (phase) {
                    case 0 -> ordinary();
                    case 1 -> container();
                    case 2 -> crafting();
                    case 3 -> state();
                    case 4 -> faults();
                    case 5 -> interleaved();
                    case 6 -> chain();
                    case 7 -> compat();
                    case 8 -> persistence();
                }
            }
            if (index == targets[phase]) {
                String scenarioStatus =
                        index == 0
                                ? "SKIPPED"
                                : ((phase == 4 && counters.containsKey("injectedPostRemovalLoss"))
                                                || (phase == 7
                                                        && counters.containsKey(
                                                                "injectedFtbPostRemovalLoss")))
                                        ? "RECOVERY_PASS_LOSS_UNRESOLVED"
                                        : "PASS";
                stages.add(
                        Map.of(
                                "scenario",
                                names[phase],
                                "operations",
                                index,
                                "status",
                                scenarioStatus));
                System.out.println(
                        "TREMOR_STAGE_DONE "
                                + names[phase]
                                + " operations="
                                + index
                                + " assertions="
                                + checks
                                + " status="
                                + scenarioStatus);
                report("RUNNING", "");
                phase++;
                index = 0;
                if (phase == names.length) {
                    report(
                            (counters.containsKey("injectedPostRemovalLoss")
                                            || counters.containsKey("injectedFtbPostRemovalLoss"))
                                    ? "COMPLETED_WITH_UNRESOLVED_FAULT_LOSS"
                                    : "PASS",
                            "");
                    Files.writeString(
                            Path.of("tremor-result.txt"),
                            "PASS " + checks + " pressure assertions " + counters);
                    MinecraftForge.EVENT_BUS.unregister(this);
                    current = null;
                    server.halt(false);
                }
            }
        } catch (Throwable error) {
            failed(server, error);
            current = null;
        }
    }

    private void check(boolean ok, String text) {
        checks++;
        if (!ok)
            throw new AssertionError(
                    names[Math.min(phase, names.length - 1)]
                            + " index="
                            + index
                            + " op="
                            + operation
                            + " "
                            + text);
    }

    private void count(String key) {
        counters.merge(key, 1L, Long::sum);
    }

    private static String key(ItemStack s) {
        return ForgeRegistries.ITEMS.getKey(s.getItem())
                + "|"
                + (s.hasTag() ? s.getTag().toString() : "");
    }

    private void add(Map<String, Long> map, ItemStack stack, int multiple) {
        if (!stack.isEmpty()) map.merge(key(stack), (long) stack.getCount() * multiple, Long::sum);
    }

    private List<ItemStack> actualStacks() {
        return level.getEntitiesOfClass(ItemEntity.class, arena).stream()
                .map(ItemEntity::getItem)
                .toList();
    }

    private Map<String, Long> actual() {
        Map<String, Long> m = new TreeMap<>();
        for (var s : actualStacks()) add(m, s, 1);
        return m;
    }

    private void start() {
        observing = false;
        for (var pos : placed) level.removeBlock(pos, false);
        level.getEntitiesOfClass(ItemEntity.class, arena).forEach(Entity::discard);
        level.getEntitiesOfClass(ExperienceOrb.class, arena).forEach(Entity::discard);
        placed.clear();
        expected.clear();
        rolls.clear();
        expectedXp = 0;

        fault = 0;
        failOnce = false;
        cancelBreak = false;
        cancelUse = false;
        denyBlock = false;
        denyItem = false;
        recurse = false;
        observing = true;
    }

    private void place(BlockPos pos, Block block) {
        placed.add(pos.immutable());
        level.setBlock(pos, block.defaultBlockState(), 3);
    }

    private void reconcile(boolean xp) {
        check(
                expected.equals(actual()),
                "loot ledger expected=" + expected + " actual=" + actual());
        for (var entry : rolls.entrySet())
            check(entry.getValue() == 1, "loot rerolled at " + entry.getKey());
        if (xp) {
            int actualXp =
                    level.getEntitiesOfClass(ExperienceOrb.class, arena).stream()
                            .mapToInt(
                                    orb -> {
                                        CompoundTag saved = orb.saveWithoutId(new CompoundTag());
                                        return orb.getValue()
                                                * (saved.contains("Count")
                                                        ? saved.getInt("Count")
                                                        : 1);
                                    })
                            .sum();
            check(
                    actualXp == expectedXp,
                    "experience ledger expected=" + expectedXp + " actual=" + actualXp);
        }
        check(!PlayerBlockBreaker.active(), "break scope leaked");
        check(!TremorUltimine.dispatching(), "FTB scope leaked");
    }

    private ServerPlayer actor(int i, boolean witness) {
        var p = players.get(i);
        LongTravailData.setWitness(diaries.get(i), TravailAspect.DEEP_VALLEY, witness);
        p.setGameMode(GameType.SURVIVAL);
        p.setShiftKeyDown(true);
        p.getFoodData().setFoodLevel(20);
        p.getFoodData().setSaturation(20);
        return p;
    }

    private void ordinary() {
        boolean witness = (index & 1) == 0, normal = index % 10 == 0;
        var p = actor(index % 32, witness);
        var tool = p.getMainHandItem();
        tool.removeTagKey("Enchantments");
        if (index % 3 == 0) tool.enchant(Enchantments.BLOCK_FORTUNE, 3);
        else if (index % 3 == 1) tool.enchant(Enchantments.SILK_TOUCH, 1);
        int damage = index % 2031;
        tool.getOrCreateTag().putInt("Damage", damage);
        start();
        Block[] blocks = {
            Blocks.STONE,
            Blocks.DIAMOND_ORE,
            Blocks.IRON_ORE,
            Blocks.OAK_LOG,
            Blocks.DIRT,
            Blocks.OBSIDIAN
        };
        place(origin, blocks[(index / 3) % blocks.length]);
        boolean success =
                normal
                        ? p.gameMode.destroyBlock(origin)
                        : PlayerBlockBreaker.breakBlock(
                                p, origin, witness ? AspectState.WITNESS : AspectState.MALICE);
        check(success && level.getBlockState(origin).isAir(), "ordinary removal");
        reconcile(true);
        check(tool.getDamageValue() == damage, "damage increase");
        tool.hurtAndBreak(10000, p, x -> {});
        tool.setDamageValue(damage + 50);
        check(!tool.isEmpty() && tool.getDamageValue() == damage, "damage protection");
        var target = new net.minecraft.world.entity.monster.Zombie(level);
        tool.hurtEnemy(target, p);
        check(tool.getDamageValue() == damage, "attack durability");
        count(normal ? "normalMining" : "specialBreaks");
    }

    private void container() {
        boolean witness = index % 2 == 0, shulker = index % 3 == 0;
        var p = actor(index % 32, witness);
        p.getMainHandItem().removeTagKey("Enchantments");
        start();
        place(origin, shulker ? Blocks.SHULKER_BOX : index % 3 == 1 ? Blocks.CHEST : Blocks.BARREL);
        var inventory = (Container) level.getBlockEntity(origin);
        int amount = (index % 64) + 1;
        var content = new ItemStack(Items.DIAMOND, amount);
        content.getOrCreateTag().putString("inventory_token", "op-" + operation);
        inventory.setItem(index % inventory.getContainerSize(), content.copy());
        check(
                PlayerBlockBreaker.breakBlock(
                        p, origin, witness ? AspectState.WITNESS : AspectState.MALICE),
                "container removal");
        var drops = actualStacks();
        long shells =
                drops.stream()
                        .filter(
                                s ->
                                        s.is(
                                                shulker
                                                        ? Items.SHULKER_BOX
                                                        : index % 3 == 1
                                                                ? Items.CHEST
                                                                : Items.BARREL))
                        .mapToLong(ItemStack::getCount)
                        .sum();
        check(shells == 1, "container shell count");
        Map<String, Long> contents = new TreeMap<>();
        for (var s : drops) {
            if (s.is(Items.DIAMOND)) add(contents, s, 1);
            if (s.hasTag()) {
                var list = s.getTag().getCompound("BlockEntityTag").getList("Items", 10);
                for (int j = 0; j < list.size(); j++)
                    add(contents, ItemStack.of(list.getCompound(j)), 1);
            }
        }
        check(
                contents.equals(Map.of(key(content), (long) amount)),
                "inventory conservation " + contents);
        check(!PlayerBlockBreaker.active(), "container scope");
        count("containers");
    }

    private TransientCraftingContainer grid() {
        var menu =
                new AbstractContainerMenu(null, 0) {
                    public ItemStack quickMoveStack(
                            net.minecraft.world.entity.player.Player p, int slot) {
                        return ItemStack.EMPTY;
                    }

                    public boolean stillValid(net.minecraft.world.entity.player.Player p) {
                        return true;
                    }
                };
        return new TransientCraftingContainer(menu, 3, 3);
    }

    private void crafting() {
        var recipe =
                (NbtInheritingShapedRecipe)
                        level.getRecipeManager()
                                .byKey(
                                        new net.minecraft.resources.ResourceLocation(
                                                "the_long_travail:high_frequency_tremor"))
                                .orElseThrow();
        var grid = grid();
        var essence =
                new ItemStack(
                        ForgeRegistries.ITEMS.getValue(
                                new net.minecraft.resources.ResourceLocation(
                                        "the_long_travail:deep_valley_essence")));
        grid.setItem(0, essence);
        grid.setItem(8, essence.copy());
        grid.setItem(2, new ItemStack(ModRegistry.AZRAEL.get()));
        grid.setItem(6, new ItemStack(ModRegistry.IMMORTAL_BUD.get()));
        var source = new ItemStack(Items.NETHERITE_PICKAXE);
        source.setDamageValue(index % 2031);
        source.enchant(
                index % 2 == 0 ? Enchantments.BLOCK_FORTUNE : Enchantments.SILK_TOUCH,
                index % 2 == 0 ? 3 : 1);
        source.setHoverName(net.minecraft.network.chat.Component.literal("Tremor-" + index));
        var nested = new CompoundTag();
        nested.putIntArray("nested", new int[] {index, ~index, 42});
        source.getOrCreateTag().put("probe", nested);
        grid.setItem(4, source);
        check(recipe.matches(grid, level), "recipe match");
        var out = recipe.assemble(grid, level.registryAccess());
        check(
                out.getCount() == 1
                        && out.is(ModRegistry.HIGH_FREQUENCY_TREMOR.get())
                        && TremorSmoke.repairedTag(source).equals(out.getTag())
                        && out.getDamageValue() == 0,
                "NBT inheritance except repaired damage");
        out.getTag().getCompound("probe").putInt("changed", 1);
        check(!source.getTag().getCompound("probe").contains("changed"), "deep NBT independence");
        grid.setItem(1, new ItemStack(Items.DIRT));
        check(!recipe.matches(grid, level), "invalid extra ingredient rejected");
        var p = actor(index % 32, false);
        p.getMainHandItem().removeTagKey("Enchantments");
        var speed =
                new PlayerEvent.BreakSpeed(
                        p, Blocks.STONE.defaultBlockState(), (index % 200 + 1) / 8F, origin);
        HighFrequencyTremorActions.speed(speed);
        check(speed.getNewSpeed() == speed.getOriginalSpeed() * 2, "speed factor");
        count("crafts");
    }

    private void state() {
        var p = actor(index % 32, index % 2 == 0);
        p.getMainHandItem().removeTagKey("Enchantments");
        start();
        int variant = index % 10;
        place(origin, variant == 0 || variant == 1 ? Blocks.BARRIER : Blocks.DIAMOND_ORE);
        boolean expectedBreak = variant >= 5;
        if (variant == 1) {
            DeepValleyItemsConfig.TREMOR_BLACKLIST.set(List.of());
            LongTravailData.setWitness(diaries.get(index % 32), TravailAspect.DEEP_VALLEY, true);

            expectedBreak = true;
        }
        if (variant == 2) cancelBreak = true;
        if (variant == 3) cancelUse = true;
        if (variant == 4) denyBlock = true;
        var hit = new BlockHitResult(Vec3.atCenterOf(origin), Direction.UP, origin, false);
        ForgeHooks.onRightClickBlock(p, InteractionHand.MAIN_HAND, origin, hit);
        check(
                level.getBlockState(origin).isAir() == expectedBreak,
                "state/blacklist/protection variant=" + variant);
        reconcile(true);
        DeepValleyItemsConfig.TREMOR_BLACKLIST.set(List.of("minecraft:barrier"));
        count("stateTransitions");
    }

    private void faults() throws Exception {
        var p = actor(index % 32, false);
        p.getMainHandItem().removeTagKey("Enchantments");
        start();
        place(origin, Blocks.STONE);
        fault = index % 3;
        if (fault == 0) {
            recurse = true;
            check(
                    PlayerBlockBreaker.breakBlock(p, origin, AspectState.MALICE),
                    "outer recursion break");
            reconcile(true);
        } else {
            boolean thrown = false;
            try {
                PlayerBlockBreaker.breakBlock(p, origin, AspectState.MALICE);
            } catch (RuntimeException injected) {
                thrown = true;
            }
            check(thrown && failOnce, "fault reached");
            check(!PlayerBlockBreaker.active(), "exception break scope cleanup");
            if (fault == 1)
                check(
                        level.getBlockState(origin).is(Blocks.STONE),
                        "pre-removal failure preserved block");
            else {
                check(level.getBlockState(origin).isAir(), "post-removal fault observation");
                count("injectedPostRemovalLoss");
            }
            check(actual().isEmpty(), "exception did not invent extra loot");
        }
        var next = actor((index + 1) % 32, true);
        next.getMainHandItem().removeTagKey("Enchantments");
        start();
        place(origin, Blocks.DIAMOND_ORE);
        check(
                PlayerBlockBreaker.breakBlock(next, origin, AspectState.WITNESS),
                "next player recovery");
        reconcile(true);
        for (String name : List.of("ACTIVE", "HARVESTING")) {
            var f = PlayerBlockBreaker.class.getDeclaredField(name);
            f.setAccessible(true);
            Object v = ((ThreadLocal<?>) f.get(null)).get();
            check(v == null || Boolean.FALSE.equals(v), "scope " + name);
        }
        count("faultRecovery");
    }

    private void interleaved() {
        int n = new int[] {1, 4, 16, 32}[index % 4];
        start();
        place(origin, Blocks.DIAMOND_ORE);
        for (int j = 0; j < n; j++) {
            int id = (index / 4) % 2 == 0 ? j : n - j - 1;
            boolean witness = (id & 1) == 0;
            var p = actor(id, witness);
            p.getMainHandItem().removeTagKey("Enchantments");

            boolean success =
                    PlayerBlockBreaker.breakBlock(
                            p, origin, witness ? AspectState.WITNESS : AspectState.MALICE);
            check(success == (j == 0), "shared block exactly one winner");
            count("interleavedAttempts");
        }
        reconcile(true);
        count("interleavedRounds");
    }

    private void chain() throws Exception {
        boolean witness = index % 2 == 0;
        var p = actor(index % 32, witness);
        p.getMainHandItem().removeTagKey("Enchantments");
        start();
        int size = new int[] {1, 16, 64, 256, 512}[index % 5];
        for (int i = 0; i < size; i++) place(origin.offset(i % 32, 0, i / 32), Blocks.DIAMOND_ORE);
        Class<?> type = Class.forName("dev.ftb.mods.ftbultimine.FTBUltimine");
        Object instance = type.getField("instance").get(null);
        if (index == 0) {
            Object max =
                    Class.forName("dev.ftb.mods.ftbultimine.config.FTBUltimineServerConfig")
                            .getField("MAX_BLOCKS")
                            .get(null);
            counters.put("ftbOriginalMaxBlocks", ((Number) call(max, "get")).longValue());
            call(max, "set", 512);
            counters.put("ftbTestMaxBlocks", 512L);
        }
        Object data =
                type.getMethod(
                                "getOrCreatePlayerData",
                                net.minecraft.world.entity.player.Player.class)
                        .invoke(instance, p);
        call(data, "clearCache");
        data.getClass().getMethod("setPressed", boolean.class).invoke(data, true);
        p.setPos(origin.getX() + .5, origin.getY() + 1, origin.getZ() + .5);
        p.setXRot(90);
        ForgeHooks.onRightClickBlock(
                p,
                InteractionHand.MAIN_HAND,
                origin,
                new BlockHitResult(Vec3.atCenterOf(origin), Direction.UP, origin, false));
        long removed = placed.stream().filter(pos -> level.getBlockState(pos).isAir()).count();
        check(removed > 0, "FTB actual selection made progress size=" + size);
        reconcile(true);
        counters.merge("ftbRemoved", removed, Long::sum);
        counters.merge("ftbSize" + size + "MaxActual", removed, Math::max);
        data.getClass().getMethod("setPressed", boolean.class).invoke(data, false);
        count("ftbInteractions");
    }

    private static Object call(Object target, String name, Object... args) throws Exception {
        for (var m : target.getClass().getMethods()) {
            if (!m.getName().equals(name) || m.getParameterCount() != args.length) continue;
            boolean match = true;
            for (int i = 0; i < args.length; i++) {
                Class<?> t = m.getParameterTypes()[i];
                if (t.isPrimitive())
                    t =
                            t == boolean.class
                                    ? Boolean.class
                                    : t == int.class
                                            ? Integer.class
                                            : t == long.class ? Long.class : t;
                if (args[i] != null && !t.isInstance(args[i])) match = false;
            }
            if (match) return m.invoke(target, args);
        }
        throw new NoSuchMethodException(target.getClass() + "." + name);
    }

    private void compat() throws Exception {
        boundaries();
        additionalBoundaries();
        for (String id :
                List.of(
                        "sophisticatedstorage:chest",
                        "sophisticatedstorage:barrel",
                        "sophisticatedstorage:diamond_chest",
                        "sophisticatedbackpacks:backpack",
                        "lootr:lootr_chest")) {
            var key = new net.minecraft.resources.ResourceLocation(id);
            if (!ForgeRegistries.BLOCKS.containsKey(key)) continue;
            Block block = ForgeRegistries.BLOCKS.getValue(key);
            for (int round = 0; round < 100; round++) {
                var p = actor(0, (round & 1) == 0);
                p.getMainHandItem().removeTagKey("Enchantments");
                start();
                place(origin, block);
                var entity = level.getBlockEntity(origin);
                check(entity != null, "mod container block entity " + id);
                if (id.equals("sophisticatedbackpacks:backpack"))
                    call(entity, "setBackpack", new ItemStack(ForgeRegistries.ITEMS.getValue(key)));
                var handler =
                        entity.getCapability(
                                        net.minecraftforge.common.capabilities.ForgeCapabilities
                                                .ITEM_HANDLER)
                                .resolve();
                if (id.equals("sophisticatedbackpacks:backpack"))
                    handler =
                            Optional.of(
                                    (net.minecraftforge.items.IItemHandler)
                                            call(
                                                    call(entity, "getBackpackWrapper"),
                                                    "getInventoryHandler"));
                var token = new ItemStack(Items.DIAMOND, 17);
                token.getOrCreateTag().putString("modInventoryProbe", id + round);
                boolean stored =
                        handler.isPresent()
                                && handler.get().insertItem(0, token.copy(), false).isEmpty();
                if (id.startsWith("sophisticated"))
                    check(stored, "mod inventory fixture populated " + id);
                check(
                        PlayerBlockBreaker.breakBlock(
                                p,
                                origin,
                                (round & 1) == 0 ? AspectState.WITNESS : AspectState.MALICE),
                        "mod container break " + id);
                if (stored) {
                    var shellsExpected = new TreeMap<>(expected);
                    var shellsActual = new TreeMap<>(actual());
                    shellsExpected.remove(key(token));
                    shellsActual.remove(key(token));
                    check(shellsExpected.equals(shellsActual), "mod shell ledger " + id);
                    for (var roll : rolls.values()) check(roll == 1, "mod loot rolled once " + id);
                    check(
                            !PlayerBlockBreaker.active() && !TremorUltimine.dispatching(),
                            "mod break scopes cleaned");
                    check(
                            expectedXp == 0
                                    && level.getEntitiesOfClass(ExperienceOrb.class, arena)
                                            .isEmpty(),
                            "container XP unchanged");
                    long loose =
                            actualStacks().stream()
                                    .filter(s -> ItemStack.isSameItemSameTags(s, token))
                                    .mapToLong(ItemStack::getCount)
                                    .sum();
                    var shells =
                            actualStacks().stream()
                                    .filter(s -> s.is(ForgeRegistries.ITEMS.getValue(key)))
                                    .toList();
                    check(
                            shells.stream().mapToInt(ItemStack::getCount).sum() <= 1,
                            "unknown storage shell not doubled " + id);
                    if (!shells.isEmpty()) {
                        var held = p.getMainHandItem();
                        var shell = shells.get(0).copy();
                        observing = false;
                        level.setBlockAndUpdate(origin.below(), Blocks.STONE.defaultBlockState());
                        p.setItemInHand(InteractionHand.MAIN_HAND, shell);
                        var result =
                                shell.useOn(
                                        new net.minecraft.world.item.context.UseOnContext(
                                                p,
                                                InteractionHand.MAIN_HAND,
                                                new BlockHitResult(
                                                        Vec3.atCenterOf(origin.below()),
                                                        Direction.UP,
                                                        origin.below(),
                                                        false)));
                        check(
                                result.consumesAction() && level.getBlockEntity(origin) != null,
                                "mod container replace " + id);
                        net.minecraftforge.items.IItemHandler restored =
                                level.getBlockEntity(origin)
                                        .getCapability(
                                                net.minecraftforge.common.capabilities
                                                        .ForgeCapabilities.ITEM_HANDLER)
                                        .resolve()
                                        .orElse(null);
                        if (id.equals("sophisticatedbackpacks:backpack"))
                            restored =
                                    (net.minecraftforge.items.IItemHandler)
                                            call(
                                                    call(
                                                            level.getBlockEntity(origin),
                                                            "getBackpackWrapper"),
                                                    "getInventoryHandler");
                        check(restored != null, "restored inventory accessible " + id);
                        long inside = 0;
                        for (int slot = 0; slot < restored.getSlots(); slot++)
                            if (ItemStack.isSameItemSameTags(restored.getStackInSlot(slot), token))
                                inside += restored.getStackInSlot(slot).getCount();
                        check(inside + loose == 17, "mod inventory restoration " + id);
                        p.setItemInHand(InteractionHand.MAIN_HAND, held);
                    } else check(loose == 17, "mod contents retained without shell " + id);
                    count("modInventoryRoundTrips:" + id);
                } else reconcile(true);
                count("modContainerBreaks:" + id);
            }
        }
        if (ModList.get().isLoaded("lootr")) {
            Object api =
                    Class.forName("noobanidus.mods.lootr.api.LootrAPI")
                            .getField("INSTANCE")
                            .get(null);
            Class<?> fillerType = Class.forName("noobanidus.mods.lootr.api.LootFiller");
            Block block =
                    ForgeRegistries.BLOCKS.getValue(
                            new net.minecraft.resources.ResourceLocation("lootr:lootr_chest"));
            var table =
                    new net.minecraft.resources.ResourceLocation("minecraft:chests/simple_dungeon");
            for (int round = 0; round < 100; round++) {
                var first = actor(0, (round & 1) == 0);
                var second = actor(1, true);
                start();
                place(origin, block);
                var entity = (RandomizableContainerBlockEntity) level.getBlockEntity(origin);
                entity.setLootTable(table, 20261027L + round);
                Object tileId = call(entity, "getTileId");
                int[] filled = {0};
                Object filler =
                        java.lang.reflect.Proxy.newProxyInstance(
                                fillerType.getClassLoader(),
                                new Class<?>[] {fillerType},
                                (proxy, method, args) -> {
                                    if (method.getName().equals("unpackLootTable")) {
                                        var player =
                                                (net.minecraft.world.entity.player.Player) args[0];
                                        var inventory = (Container) args[1];
                                        var token =
                                                new ItemStack(
                                                        Items.DIAMOND,
                                                        player.getUUID().equals(first.getUUID())
                                                                ? 11
                                                                : 13);
                                        token.getOrCreateTag()
                                                .putString(
                                                        "lootrProbe", player.getUUID().toString());
                                        inventory.setItem(0, token);
                                        filled[0]++;
                                        return null;
                                    }
                                    if (method.getName().equals("toString"))
                                        return "TremorLootFixture";
                                    if (method.getName().equals("hashCode"))
                                        return System.identityHashCode(proxy);
                                    if (method.getName().equals("equals")) return proxy == args[0];
                                    return null;
                                });
                java.util.function.Supplier<net.minecraft.resources.ResourceLocation>
                        tableSupplier = () -> table;
                java.util.function.LongSupplier seedSupplier = () -> 20261027L;
                var a =
                        (Container)
                                call(
                                        api,
                                        "getInventory",
                                        level,
                                        tileId,
                                        origin,
                                        first,
                                        entity,
                                        filler,
                                        tableSupplier,
                                        seedSupplier);
                var b =
                        (Container)
                                call(
                                        api,
                                        "getInventory",
                                        level,
                                        tileId,
                                        origin,
                                        second,
                                        entity,
                                        filler,
                                        tableSupplier,
                                        seedSupplier);
                check(
                        a != null && b != null && a != b && filled[0] == 2,
                        "real Lootr per-player stores created");
                check(
                        a.getItem(0).getCount() == 11 && b.getItem(0).getCount() == 13,
                        "distinct Lootr inventories");
                check(
                        PlayerBlockBreaker.breakBlock(
                                first,
                                origin,
                                (round & 1) == 0 ? AspectState.WITNESS : AspectState.MALICE),
                        "Lootr private inventory break");
                reconcile(true);
                check(
                        actualStacks().stream()
                                .noneMatch(s -> s.hasTag() && s.getTag().contains("lootrProbe")),
                        "private loot not copied into public drops");
                check(
                        a.getItem(0).getCount() == 11 && b.getItem(0).getCount() == 13,
                        "private inventories unchanged by block loot");
                count("lootrTwoPlayerPrivateInventory");
            }
        }
        if (ModList.get().isLoaded("ftbchunks")) {
            Object teams =
                    Class.forName("dev.ftb.mods.ftbteams.data.TeamManagerImpl")
                            .getField("INSTANCE")
                            .get(null);
            var owner = actor(0, true);
            var member = actor(1, true);
            var stranger = actor(2, true);
            for (var p : List.of(owner, member, stranger))
                call(
                        teams,
                        "playerLoggedIn",
                        p,
                        p.getUUID(),
                        "Tremor" + p.getUUID().toString().substring(0, 8));
            Object party = call(teams, "createParty", owner, "Tremor isolation");
            call(party, "invite", owner, List.of(member.getGameProfile()));
            call(party, "join", member);
            Object api =
                    Class.forName("dev.ftb.mods.ftbchunks.api.FTBChunksAPI")
                            .getMethod("api")
                            .invoke(null);
            Object claim =
                    call(
                            api,
                            "claimAsPlayer",
                            owner,
                            level.dimension(),
                            new ChunkPos(origin),
                            false);
            check(Boolean.TRUE.equals(call(claim, "isSuccess")), "real chunk claim created");
            for (var p : List.of(stranger, owner, member)) {
                start();
                place(origin, Blocks.DIAMOND_ORE);
                p.getMainHandItem().removeTagKey("Enchantments");
                ForgeHooks.onRightClickBlock(
                        p,
                        InteractionHand.MAIN_HAND,
                        origin,
                        new BlockHitResult(Vec3.atCenterOf(origin), Direction.UP, origin, false));
                check(
                        level.getBlockState(origin).isAir() == (p != stranger),
                        "real claim actor=" + p.getUUID());
                reconcile(true);
                count("realClaimPermissions");
            }
            start();
            var outside = origin.west();
            place(outside, Blocks.DIAMOND_ORE);
            place(origin, Blocks.DIAMOND_ORE);
            stranger.setPos(outside.getX() + .5, outside.getY() + 1, outside.getZ() + .5);
            stranger.setXRot(90);
            Object ftbInstance =
                    Class.forName("dev.ftb.mods.ftbultimine.FTBUltimine")
                            .getField("instance")
                            .get(null);
            Object data = call(ftbInstance, "getOrCreatePlayerData", stranger);
            call(data, "setPressed", true);
            ForgeHooks.onRightClickBlock(
                    stranger,
                    InteractionHand.MAIN_HAND,
                    outside,
                    new BlockHitResult(Vec3.atCenterOf(outside), Direction.UP, outside, false));
            check(
                    level.getBlockState(outside).isAir()
                            && level.getBlockState(origin).is(Blocks.DIAMOND_ORE),
                    "real cross-claim chain protection");
            reconcile(true);
            call(data, "setPressed", false);
            count("realCrossClaimSelection");
        }
    }

    private void additionalBoundaries() throws Exception {
        for (int round = 0; round < 100; round++) {
            var actor = actor(0, false);
            start();
            place(origin, TremorFaultBlocks.REFUSING.get());
            check(
                    !PlayerBlockBreaker.breakBlock(actor, origin, AspectState.MALICE)
                            && level.getBlockState(origin).is(TremorFaultBlocks.REFUSING.get()),
                    "block refusal respected");
            reconcile(true);
            check(
                    PlayerBlockBreaker.breakBlock(actor, origin, AspectState.WITNESS)
                            && level.getBlockState(origin).isAir(),
                    "witness overrides refusal");
            reconcile(true);
            start();
            place(origin, TremorFaultBlocks.REPLACING.get());
            check(
                    !PlayerBlockBreaker.breakBlock(actor, origin, AspectState.WITNESS)
                            && level.getBlockState(origin).is(Blocks.DIRT),
                    "replacement not destroyed or rewarded");
            reconcile(true);
            start();
            place(origin, TremorFaultBlocks.NEIGHBOR.get());
            place(origin.east(), Blocks.DIRT);
            check(
                    PlayerBlockBreaker.breakBlock(actor, origin, AspectState.MALICE)
                            && level.getBlockState(origin.east()).isAir(),
                    "neighbor callback ran");
            reconcile(true);
            check(
                    actualStacks().stream()
                                    .filter(s -> s.is(Items.DIRT))
                                    .mapToInt(ItemStack::getCount)
                                    .sum()
                            == 1,
                    "neighbor loot not doubled");
            count("blockCallbackBoundaries");
        }
        var p = actor(0, true);
        start();
        place(origin, Blocks.DIAMOND_ORE);
        check(
                !PlayerBlockBreaker.breakBlock(p, origin, AspectState.INACTIVE),
                "inactive kernel denied");
        var inv = CuriosApi.getCuriosInventory(p).resolve().orElseThrow();
        var diarySlot = inv.getCurios().get("travel_diary").getStacks();
        var diary = diarySlot.getStackInSlot(0);
        diarySlot.setStackInSlot(0, ItemStack.EMPTY);
        ForgeHooks.onRightClickBlock(
                p,
                InteractionHand.MAIN_HAND,
                origin,
                new BlockHitResult(Vec3.atCenterOf(origin), Direction.UP, origin, false));
        check(!level.getBlockState(origin).isAir(), "unequipped diary denied");
        diarySlot.setStackInSlot(0, diary);
        reconcile(true);
        var unloaded = new BlockPos(900000, 100, 900000);
        check(!level.hasChunkAt(unloaded), "unloaded fixture");
        check(
                !PlayerBlockBreaker.breakBlock(p, unloaded, AspectState.WITNESS)
                        && !level.hasChunkAt(unloaded),
                "unloaded chunk remains unloaded");
        var border = level.getWorldBorder();
        double size = border.getSize();
        border.setSize(10);
        try {
            check(
                    !PlayerBlockBreaker.breakBlock(p, origin, AspectState.WITNESS),
                    "world border denied");
        } finally {
            border.setSize(size);
        }
        var caster = actor(1, true);
        var time = com.thelongtravail.boundless.TimeStopManager.get(level);
        check(
                time.start(
                        caster,
                        true,
                        level.getGameTime(),
                        p.position(),
                        8,
                        100,
                        Set.of(caster.getUUID())),
                "time stop fixture activated");
        try {
            check(com.thelongtravail.boundless.TimeStopManager.frozen(p), "target really frozen");
            check(
                    !PlayerBlockBreaker.breakBlock(p, origin, AspectState.WITNESS),
                    "frozen player denied");
        } finally {
            com.thelongtravail.boundless.TimeStopManager.clear();
        }
        reconcile(true);
        count("worldAndStateBoundarySuite");
        for (int round = 0; round < 20; round++) {
            p = actor(0, (round & 1) == 0);
            p.getMainHandItem().removeTagKey("Enchantments");
            start();
            place(origin, Blocks.DIAMOND_ORE);
            check(p.gameMode.destroyBlock(origin), "normal mining in both aspect states");
            reconcile(true);
            count("normalMiningStateComparisons");
        }
        var protectedTool = p.getMainHandItem();
        p.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.NETHERITE_PICKAXE));
        start();
        place(origin, Blocks.STONE);
        fault = 2;
        boolean vanillaThrew = false;
        try {
            p.gameMode.destroyBlock(origin);
        } catch (IllegalStateException injected) {
            vanillaThrew = true;
        } finally {
            p.setItemInHand(InteractionHand.MAIN_HAND, protectedTool);
        }
        check(
                vanillaThrew
                        && failOnce
                        && level.getBlockState(origin).isAir()
                        && actual().isEmpty(),
                "vanilla control also loses drops after injected post-removal failure");
        count("vanillaPostRemovalFailureControl");
        start();
        if (ftb) {
            Object instance =
                    Class.forName("dev.ftb.mods.ftbultimine.FTBUltimine")
                            .getField("instance")
                            .get(null);
            for (int round = 0; round < 200; round++) {
                p = actor(0, false);
                p.getMainHandItem().removeTagKey("Enchantments");
                start();
                place(origin, Blocks.STONE);
                fault = round % 2 + 1;
                Object data = call(instance, "getOrCreatePlayerData", p);
                call(data, "clearCache");
                call(data, "setPressed", true);
                ForgeHooks.onRightClickBlock(
                        p,
                        InteractionHand.MAIN_HAND,
                        origin,
                        new BlockHitResult(Vec3.atCenterOf(origin), Direction.UP, origin, false));
                check(failOnce, "FTB injected fault reached");
                check(
                        !PlayerBlockBreaker.active() && !TremorUltimine.dispatching(),
                        "FTB fault scopes cleaned");
                if (fault == 2) {
                    check(
                            level.getBlockState(origin).isAir() && actual().isEmpty(),
                            "FTB post-removal loss recorded");
                    count("injectedFtbPostRemovalLoss");
                } else
                    check(
                            level.getBlockState(origin).is(Blocks.STONE),
                            "FTB pre-removal preserved");
                call(data, "setPressed", false);
                for (String field : List.of("DISPATCH", "HANDLED")) {
                    var f = TremorUltimine.class.getDeclaredField(field);
                    f.setAccessible(true);
                    check(
                            !Boolean.TRUE.equals(((ThreadLocal<?>) f.get(null)).get()),
                            "FTB scope " + field);
                }
                var next = actor(1, true);
                next.getMainHandItem().removeTagKey("Enchantments");
                start();
                place(origin, Blocks.DIAMOND_ORE);
                ForgeHooks.onRightClickBlock(
                        next,
                        InteractionHand.MAIN_HAND,
                        origin,
                        new BlockHitResult(Vec3.atCenterOf(origin), Direction.UP, origin, false));
                check(level.getBlockState(origin).isAir(), "FTB next player recovers");
                reconcile(true);
                count("ftbFaultRecovery");
            }
            Class<?> config =
                    Class.forName("dev.ftb.mods.ftbultimine.config.FTBUltimineServerConfig");
            Object max = config.getField("MAX_BLOCKS").get(null),
                    cost = config.getField("EXPERIENCE_PER_BLOCK").get(null),
                    cooldown = config.getField("ULTIMINE_COOLDOWN").get(null);
            Object oldMax = call(max, "get"),
                    oldCost = call(cost, "get"),
                    oldCooldown = call(cooldown, "get");
            p = actor(0, true);
            p.giveExperiencePoints(1000);
            Object data = call(instance, "getOrCreatePlayerData", p);
            try {
                call(max, "set", 3);
                call(cost, "set", 2D);
                start();
                for (int i = 0; i < 4; i++) place(origin.east(i), Blocks.DIAMOND_ORE);
                call(data, "clearCache");
                call(data, "setPressed", true);
                int beforeXp = p.totalExperience;
                ForgeHooks.onRightClickBlock(
                        p,
                        InteractionHand.MAIN_HAND,
                        origin,
                        new BlockHitResult(Vec3.atCenterOf(origin), Direction.UP, origin, false));
                check(
                        placed.stream().filter(pos -> level.getBlockState(pos).isAir()).count()
                                == 3,
                        "FTB configured selection limit");
                reconcile(true);
                var pending = data.getClass().getDeclaredField("pendingXPCost");
                pending.setAccessible(true);
                check(pending.getDouble(data) == 4D, "bridge queued exactly four XP cost");
                check(
                        !p.createCommandSourceStack().hasPermission(2),
                        "ordinary player permission fixture");
                call(data, "takePendingXP", p);
                check(
                        p.totalExperience == beforeXp,
                        "FTB non-operator command cost limitation reproduced: before="
                                + beforeXp
                                + " after="
                                + p.totalExperience);
                call(data, "addPendingXPCost", p, 2);
                call(data, "takePendingXP", p);
                check(
                        p.totalExperience == beforeXp,
                        "upstream-only XP cost control also unchanged");
                count("ftbNonOpXpDeductionBlocked");
                server.getPlayerList().op(p.getGameProfile());
                try {
                    check(
                            p.createCommandSourceStack().hasPermission(2),
                            "operator permission fixture");
                    call(data, "addPendingXPCost", p, 2);
                    call(data, "takePendingXP", p);
                    check(
                            p.totalExperience == beforeXp - 4,
                            "upstream command deducts four XP for operator: before="
                                    + beforeXp
                                    + " after="
                                    + p.totalExperience);
                    count("ftbOperatorXpCharged");
                } finally {
                    server.getPlayerList().deop(p.getGameProfile());
                }
                call(cooldown, "set", 100L);
                Class<?> tracker = Class.forName("dev.ftb.mods.ftbultimine.CooldownTracker");
                check(
                        ((Number)
                                                tracker.getMethod(
                                                                "getLastUltimineTime",
                                                                net.minecraft.world.entity.player
                                                                        .Player.class)
                                                        .invoke(null, p))
                                        .longValue()
                                > 0,
                        "FTB success records cooldown time");
                call(cooldown, "set", oldCooldown);
                start();
                for (int i = 0; i < 4; i++) place(origin.east(i), Blocks.DIAMOND_ORE);
                p.getFoodData().setFoodLevel(0);
                ForgeHooks.onRightClickBlock(
                        p,
                        InteractionHand.MAIN_HAND,
                        origin,
                        new BlockHitResult(Vec3.atCenterOf(origin), Direction.UP, origin, false));
                check(
                        placed.stream().filter(pos -> level.getBlockState(pos).isAir()).count()
                                == 1,
                        "zero food disables chain but permits item single break");
                reconcile(true);
                count("ftbConfiguredLimitsAndCosts");
            } finally {
                call(max, "set", oldMax);
                call(cost, "set", oldCost);
                call(cooldown, "set", oldCooldown);
                call(data, "setPressed", false);
                p.getFoodData().setFoodLevel(20);
            }
        }
    }

    private void boundaries() {
        for (int v = 0; v < 9; v++) {
            var p = actor(0, true);
            var tool = p.getMainHandItem();
            p.getMainHandItem().removeTagKey("Enchantments");
            start();
            place(origin, Blocks.DIAMOND_ORE);
            InteractionHand hand = InteractionHand.MAIN_HAND;
            if (v == 0) denyItem = true;
            if (v == 1) p.setShiftKeyDown(false);
            if (v == 2) hand = InteractionHand.OFF_HAND;
            if (v == 3) p.setItemInHand(hand, new ItemStack(Items.NETHERITE_PICKAXE));
            if (v == 4) p.setGameMode(GameType.ADVENTURE);
            if (v == 5) p.setGameMode(GameType.SPECTATOR);
            if (v == 6) p.setGameMode(GameType.CREATIVE);
            if (v == 7)
                DeepValleyItemsConfig.TREMOR_BLACKLIST.set(List.of("minecraft:diamond_ore"));
            if (v == 8) {
                var list = new ArrayList<String>();
                for (int i = 0; i < 10000; i++) list.add("probe:block_" + i);
                list.add("minecraft:diamond_ore");
                DeepValleyItemsConfig.TREMOR_BLACKLIST.set(list);
            }
            ForgeHooks.onRightClickBlock(
                    p,
                    hand,
                    origin,
                    new BlockHitResult(Vec3.atCenterOf(origin), Direction.UP, origin, false));
            check(level.getBlockState(origin).isAir() == (v == 6), "eligibility boundary " + v);
            reconcile(true);
            p.setItemInHand(InteractionHand.MAIN_HAND, tool);
            DeepValleyItemsConfig.TREMOR_BLACKLIST.set(List.of("minecraft:barrier"));
            count("eligibilityBoundaries");
        }
        var p = actor(0, true);
        var held = p.getMainHandItem();
        for (int v = 0; v < 16; v++) {
            p.removeAllEffects();
            p.setOnGround((v & 1) == 0);
            if ((v & 2) != 0)
                p.addEffect(
                        new net.minecraft.world.effect.MobEffectInstance(
                                net.minecraft.world.effect.MobEffects.DIG_SPEED, 100, 1));
            if ((v & 4) != 0)
                p.addEffect(
                        new net.minecraft.world.effect.MobEffectInstance(
                                net.minecraft.world.effect.MobEffects.DIG_SLOWDOWN, 100, 1));
            var base = new ItemStack(Items.NETHERITE_PICKAXE);
            var tremor = new ItemStack(ModRegistry.HIGH_FREQUENCY_TREMOR.get());
            if ((v & 8) != 0) {
                base.enchant(Enchantments.BLOCK_EFFICIENCY, 5);
                tremor.enchant(Enchantments.BLOCK_EFFICIENCY, 5);
            }
            p.setItemInHand(InteractionHand.MAIN_HAND, base);
            float normal = p.getDigSpeed(Blocks.STONE.defaultBlockState(), origin);
            p.setItemInHand(InteractionHand.MAIN_HAND, tremor);
            float special = p.getDigSpeed(Blocks.STONE.defaultBlockState(), origin);
            check(Math.abs(special - normal * 2) < 0.00001F, "actual speed modifiers " + v);
            count("actualSpeedComparisons");
        }
        p.removeAllEffects();
        p.setOnGround(true);
        p.setItemInHand(InteractionHand.MAIN_HAND, held);
        for (int v = 0; v < 16; v++) {
            p.getInventory().clearContent();
            p.setItemInHand(InteractionHand.MAIN_HAND, held);
            var menu =
                    new CraftingMenu(
                            7, p.getInventory(), ContainerLevelAccess.create(level, origin));
            var essence =
                    new ItemStack(
                            ForgeRegistries.ITEMS.getValue(
                                    new net.minecraft.resources.ResourceLocation(
                                            "the_long_travail:deep_valley_essence")));
            menu.getSlot(1).set(essence);
            menu.getSlot(9).set(essence.copy());
            menu.getSlot(3).set(new ItemStack(ModRegistry.AZRAEL.get()));
            menu.getSlot(7).set(new ItemStack(ModRegistry.IMMORTAL_BUD.get()));
            var source = new ItemStack(Items.NETHERITE_PICKAXE);
            source.getOrCreateTag().putString("menuProbe", "round" + v);
            source.setDamageValue(100 + v);
            menu.getSlot(5).set(source);
            check(menu.getSlot(0).hasItem(), "actual crafting result");
            ItemStack output;
            if ((v & 1) == 0) output = menu.quickMoveStack(p, 0);
            else {
                output = menu.getSlot(0).remove(1);
                menu.getSlot(0).onTake(p, output);
            }
            check(
                    output.is(ModRegistry.HIGH_FREQUENCY_TREMOR.get())
                            && TremorSmoke.repairedTag(source).equals(output.getTag())
                            && output.getDamageValue() == 0,
                    "actual crafting NBT");
            for (int slot = 1; slot <= 9; slot++)
                check(!menu.getSlot(slot).hasItem(), "actual ingredient consumed " + slot);
            count((v & 1) == 0 ? "shiftClickCrafts" : "normalClickCrafts");
        }
        p.setItemInHand(InteractionHand.MAIN_HAND, held);
    }

    private void persistence() throws Exception {
        observing = false;
        var pos = origin.offset(0, 0, 20);
        level.setBlockAndUpdate(pos, Blocks.CHEST.defaultBlockState());
        var chest = (ChestBlockEntity) level.getBlockEntity(pos);
        var tool = new ItemStack(ModRegistry.HIGH_FREQUENCY_TREMOR.get());
        tool.getOrCreateTag().putInt("Damage", 347);
        tool.getOrCreateTag().putString("persistence", "tremor-20261027");
        chest.setItem(0, tool);
        Files.writeString(
                Path.of("persistence-position.txt"),
                pos.getX() + "," + pos.getY() + "," + pos.getZ());
        count("persistenceFixtureWritten");
    }

    private void report(String status, String error) throws Exception {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("status", status);
        data.put("ftb", ftb);
        data.put("worldSeed", level.getSeed());
        data.put("sequence", "fixed-index-v1");
        data.put("ticks", ticks);
        data.put("assertions", checks);
        data.put("operations", operation);
        data.put("phase", names[Math.min(phase, names.length - 1)]);
        data.put("index", index);
        data.put("counts", counters);
        data.put("scenarios", stages);
        data.put("error", error);
        if (!error.isEmpty()) {
            data.put("expectedLoot", expected);
            data.put("actualLoot", actual());
            data.put("expectedXp", expectedXp);
            data.put(
                    "blocks",
                    placed.stream().map(pos -> pos + "=" + level.getBlockState(pos)).toList());
            data.put(
                    "tools",
                    players.stream()
                            .map(p -> p.getMainHandItem().save(new CompoundTag()).toString())
                            .toList());
        }
        data.put(
                "faultBoundary",
                "Injected post-removal loot exceptions are recorded as losses, not atomic rollback"
                        + " or production correctness passes.");
        Files.writeString(
                Path.of("pressure-result.json"),
                new GsonBuilder().setPrettyPrinting().create().toJson(data));
    }
}
