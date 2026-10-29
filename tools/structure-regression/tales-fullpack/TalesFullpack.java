package travail.talesfullpack;

import com.mojang.authlib.GameProfile;
import com.thelongtravail.boundless.EnchantmentTransfer;
import com.thelongtravail.registry.ModRegistry;
import java.nio.file.*;
import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.enchantment.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.entity.ChiseledBookShelfBlockEntity;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

@Mod("travail_tales_stress")
public final class TalesFullpack {
    private MinecraftServer server;
    private ServerLevel world;
    private final List<FakePlayer> players = new ArrayList<>();
    private List<Enchantment> enchantments;
    private int clientTicks, phase, calculations, crafts, checks, anvils, tables, shelves, full, shifts;
    private volatile boolean done;
    private volatile Throwable failure;
    private boolean finished;
    private long start, maxBatchNanos;
    private final List<Double> batches = new ArrayList<>();
    private final Set<String> positiveCoverage = new HashSet<>();
    private static final BlockPos ORIGIN = new BlockPos(16, 100, 16);
    private static final BlockPos CHEST = ORIGIN.offset(0, 0, 3);

    public TalesFullpack() {
        MinecraftForge.EVENT_BUS.addListener(this::clientTick);
        MinecraftForge.EVENT_BUS.addListener(this::serverTick);
    }

    private void check(boolean ok, String label) {
        checks++;
        if (!ok) throw new AssertionError(label + " calculation=" + calculations + " craft=" + crafts);
    }

    private ItemStack tales() { return new ItemStack(ModRegistry.ANCIENT_STRANGE_TALES.get()); }
    private ItemStack enchanted(Item item, Enchantment enchantment, int level) {
        ItemStack result = new ItemStack(item);
        EnchantmentHelper.setEnchantments(Map.of(enchantment, level), result);
        return result;
    }
    private int level(ItemStack stack, Enchantment enchantment) {
        return EnchantmentHelper.getEnchantments(stack).getOrDefault(enchantment, 0);
    }

    private void clientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END || finished) return;
        Minecraft mc = Minecraft.getInstance();
        try {
            clientTicks++;
            mc.setWindowActive(true);
            if (clientTicks > 18000) throw new AssertionError("client tick guard phase=" + phase);
            if (phase == 0 && clientTicks > 100 && mc.getOverlay() == null && mc.screen != null
                    && mc.screen.getClass().getSimpleName().matches("AccessibilityOnboardingScreen|StartupAdviceScreen")) mc.setScreen(new TitleScreen());
            if (phase == 0 && clientTicks > 100 && mc.getOverlay() == null && mc.screen instanceof TitleScreen) {
                mc.createWorldOpenFlows().loadLevel(new TitleScreen(), "tales");
                phase = 1;
            } else if (phase == 1 && mc.player != null && mc.getSingleplayerServer() != null && mc.screen == null) {
                server = mc.getSingleplayerServer();
                server.execute(() -> {
                    try { initialize(); } catch (Throwable error) { fail(error); }
                });
                phase = 2;
            } else if (phase == 2 && done) {
                if (failure != null) throw new AssertionError("server pressure", failure);
                finish(mc, "PASS fullpack checks=" + checks + " calculations=" + calculations + " crafts=" + crafts
                        + " anvils=" + anvils + " tables=" + tables + " shelves=" + shelves + " fullInventory=" + full + " shiftClicks=" + shifts);
            }
            if (clientTicks % 200 == 0) System.out.println("TALES_FULLPACK phase=" + phase + " calculations=" + calculations + " crafts=" + crafts);
        } catch (Throwable error) { error.printStackTrace(); finish(mc, "FAIL " + error); }
    }

    private void initialize() throws Exception {
        world = server.overworld();
        start = System.nanoTime();
        world.getChunkAt(ORIGIN);
        if (Boolean.getBoolean("tales.persistenceOnly")) {
            var chest = (ChestBlockEntity) world.getBlockEntity(CHEST);
            check(chest != null, "persisted chest exists");
            ItemStack saved = chest.getItem(0);
            check(EnchantmentTransfer.isTales(saved) && level(saved, Enchantments.SHARPNESS) == 255, "full JVM restart book level");
            check(saved.getHoverName().getString().equals("Tales pressure persistence") && saved.getTag().getString("pressureMarker").equals("20261029"), "full JVM restart metadata");
            done = true;
            return;
        }
        for (int i = 0; i < 4; i++) {
            FakePlayer p = FakePlayerFactory.get(world, new GameProfile(UUID.nameUUIDFromBytes(("TalesPressure" + i).getBytes(java.nio.charset.StandardCharsets.UTF_8)), "TalesPressure" + i));
            p.setPos(16.5 + i, 101, 16.5);
            p.setInvulnerable(true);
            players.add(p);
        }
        for (var p : server.getPlayerList().getPlayers()) { p.setInvulnerable(true); p.teleportTo(16.5, 102, 16.5); }
        for (int x = -2; x <= 5; x++) for (int z = -2; z <= 5; z++) world.setBlockAndUpdate(ORIGIN.offset(x, -1, z), Blocks.STONE.defaultBlockState());
        world.setBlockAndUpdate(ORIGIN, Blocks.CRAFTING_TABLE.defaultBlockState());
        world.setBlockAndUpdate(ORIGIN.offset(1, 0, 0), Blocks.ENCHANTING_TABLE.defaultBlockState());
        world.setBlockAndUpdate(ORIGIN.offset(2, 0, 0), Blocks.CHISELED_BOOKSHELF.defaultBlockState());
        enchantments = new ArrayList<>(ForgeRegistries.ENCHANTMENTS.getValues());
        enchantments.sort(Comparator.comparing(e -> ForgeRegistries.ENCHANTMENTS.getKey(e).toString()));
        check(enchantments.size() > 39, "modded enchantments loaded");
        Files.writeString(Path.of("tales-enchantments.txt"), String.join("\n", enchantments.stream().map(e -> ForgeRegistries.ENCHANTMENTS.getKey(e).toString()).toList()));
        Files.writeString(Path.of("tales-loaded-mods.txt"), String.join("\n", ModList.get().getMods().stream().map(m -> m.getModId() + " " + m.getVersion()).sorted().toList()));
    }

    private void serverTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || world == null || done) return;
        long began = System.nanoTime();
        try {
            if (calculations < 100000) {
                for (int i = 0; i < 500 && calculations < 100000; i++, calculations++) calculate(calculations);
            } else {
                for (int i = 0; i < 10 && crafts < 10000; i++, crafts++) craft(crafts);
                if (crafts >= 10000) complete();
            }
            long elapsed = System.nanoTime() - began;
            maxBatchNanos = Math.max(maxBatchNanos, elapsed);
            batches.add(elapsed / 1e6);
        } catch (Throwable error) { fail(error); }
    }

    private void calculate(int n) {
        Enchantment e = enchantments.get(n % enchantments.size());
        int sourceLevel = 1 + n % 255;
        int targetLevel = (n * 37) % 256;
        boolean extraction = (n / enchantments.size()) % 2 == 0;
        ItemStack source = enchanted(extraction ? Items.ENCHANTED_BOOK : ModRegistry.ANCIENT_STRANGE_TALES.get(), e, sourceLevel);
        ItemStack target = targetLevel == 0 ? (extraction ? tales() : new ItemStack(Items.ENCHANTED_BOOK))
                : enchanted(extraction ? ModRegistry.ANCIENT_STRANGE_TALES.get() : Items.ENCHANTED_BOOK, e, targetLevel);
        source.getOrCreateTag().putInt("sourceMarker", n);
        ItemStack before = source.copy();
        EnchantmentTransfer.Filter filter = EnchantmentTransfer.Filter.values()[(n / enchantments.size()) % 3];
        boolean selected = filter == EnchantmentTransfer.Filter.ALL || e.isCurse() == (filter == EnchantmentTransfer.Filter.CURSE);
        var result = EnchantmentTransfer.transfer(source, target, filter);
        check(ItemStack.matches(source, before), "source remains immutable");
        if (!selected || targetLevel == 255) check(result == null, "filtered or full destination");
        else {
            check(result != null, "registered enchantment transfers");
            positiveCoverage.add(ForgeRegistries.ENCHANTMENTS.getKey(e) + (extraction ? "/extract" : "/apply"));
            check(level(result.source(), e) + level(result.target(), e) == sourceLevel + targetLevel && level(result.target(), e) <= 255, "level conservation");
            check(result.source().getCount() == 1 && result.target().getCount() == 1, "one of each item");
            check(result.source().getTag().getInt("sourceMarker") == n, "custom data preserved");
            ItemStack loaded = ItemStack.of(result.target().save(new CompoundTag()));
            check(ItemStack.matches(loaded, result.target()), "NBT roundtrip");
        }
    }

    private int count(List<ItemStack> stacks, Item item) {
        return stacks.stream().filter(s -> s.is(item)).mapToInt(ItemStack::getCount).sum();
    }

    private void craft(int n) {
        FakePlayer p = players.get(n % players.size());
        p.getInventory().clearContent();
        boolean filled = n % 20 == 0;
        boolean shift = n % 2 == 0 && !filled;
        if (filled) { for (int i = 0; i < p.getInventory().items.size(); i++) p.getInventory().items.set(i, new ItemStack(Items.STONE, 64)); full++; }
        boolean extraction = n % 6 < 3;
        int filter = n % 3;
        boolean stacked = extraction && n % 7 == 0;
        Item targetItem = stacked ? Items.STICK : Items.DIAMOND_SWORD;
        ItemStack source = enchanted(extraction ? targetItem : ModRegistry.ANCIENT_STRANGE_TALES.get(), Enchantments.SHARPNESS, 10);
        source.enchant(Enchantments.VANISHING_CURSE, 1);
        source.getOrCreateTag().putInt("pressureRound", n);
        if (stacked) source.setCount(4);
        Item sourceKind = source.getItem();
        var menu = new CraftingMenu(10, p.getInventory(), ContainerLevelAccess.create(world, ORIGIN));
        menu.getSlot(1).set(extraction ? tales() : source);
        menu.getSlot(2).set(extraction ? source : new ItemStack(targetItem));
        Item catalyst = extraction ? Items.QUARTZ : Items.LAPIS_LAZULI;
        menu.getSlot(3).set(new ItemStack(catalyst, 4));
        Item filterItem = filter == 1 ? Items.HONEY_BOTTLE : Items.FERMENTED_SPIDER_EYE;
        if (filter != 0) menu.getSlot(4).set(new ItemStack(filterItem, 4));
        var oldDrops = world.getEntitiesOfClass(ItemEntity.class, p.getBoundingBox().inflate(8));
        Set<UUID> previous = new HashSet<>(); oldDrops.forEach(e -> previous.add(e.getUUID()));
        check(!menu.getSlot(0).getItem().isEmpty(), "actual recipe preview");
        ItemStack output;
        if (shift) { output = menu.quickMoveStack(p, 0); shifts++; }
        else { output = menu.getSlot(0).remove(1); menu.getSlot(0).onTake(p, output); }
        check(!output.isEmpty() && level(output, Enchantments.SHARPNESS) == (filter == 2 ? 0 : 10)
                && level(output, Enchantments.VANISHING_CURSE) == (filter == 1 ? 0 : 1), "actual result enchantments");
        var drops = world.getEntitiesOfClass(ItemEntity.class, p.getBoundingBox().inflate(8)).stream().filter(e -> !previous.contains(e.getUUID())).toList();
        List<ItemStack> actual = new ArrayList<>();
        for (int i = 1; i <= 9; i++) actual.add(menu.getSlot(i).getItem());
        actual.addAll(p.getInventory().items);
        drops.forEach(e -> actual.add(e.getItem()));
        if (!shift) actual.add(output);
        check(count(actual, ModRegistry.ANCIENT_STRANGE_TALES.get()) == 1, "exactly one surviving tales");
        check(count(actual, targetItem) == (stacked ? 4 : 1), "subject count conserved");
        check(count(actual, catalyst) == 3, "one catalyst consumed");
        if (filter != 0) check(count(actual, filterItem) == 3, "one filter consumed");
        check(count(actual, Items.GLASS_BOTTLE) == (filter == 1 ? 1 : 0), "glass bottle conserved including drops");
        List<ItemStack> sources = actual.stream().filter(s -> s.is(sourceKind)).toList();
        int sharpTotal = actual.stream().mapToInt(s -> level(s, Enchantments.SHARPNESS) * s.getCount()).sum();
        int curseTotal = actual.stream().mapToInt(s -> level(s, Enchantments.VANISHING_CURSE) * s.getCount()).sum();
        check(sharpTotal == 10 * (stacked ? 4 : 1) && curseTotal == (stacked ? 4 : 1), "actual menu enchantment conservation");
        check(sources.stream().allMatch(s -> s.getTag() != null && s.getTag().getInt("pressureRound") == n), "actual source custom data");
        drops.forEach(ItemEntity::discard);
        if (n % 10 == 0) { anvil(p); shelf(); }
        if (n % 100 == 0) table(p);
    }

    private void anvil(FakePlayer p) {
        p.experienceLevel = 100;
        var menu = new AnvilMenu(20, p.getInventory());
        menu.getSlot(0).set(tales()); menu.getSlot(1).set(enchanted(Items.ENCHANTED_BOOK, Enchantments.ALL_DAMAGE_PROTECTION, 3));
        check(EnchantmentTransfer.isTales(menu.getSlot(2).getItem()) && level(menu.getSlot(2).getItem(), Enchantments.ALL_DAMAGE_PROTECTION) == 3, "fullpack anvil accepts book");
        menu.getSlot(0).set(new ItemStack(Items.DIAMOND_SWORD)); menu.getSlot(1).set(enchanted(ModRegistry.ANCIENT_STRANGE_TALES.get(), Enchantments.SHARPNESS, 5));
        check(menu.getSlot(2).getItem().isEmpty(), "fullpack anvil cannot consume tales");
        menu.getSlot(0).set(enchanted(ModRegistry.ANCIENT_STRANGE_TALES.get(), Enchantments.SHARPNESS, 255)); menu.getSlot(1).set(ItemStack.EMPTY); menu.setItemName("Pressure rename");
        check(level(menu.getSlot(2).getItem(), Enchantments.SHARPNESS) == 255, "rename preserves 255");
        anvils++;
    }

    private void table(FakePlayer p) {
        p.experienceLevel = 100;
        var menu = new EnchantmentMenu(30, p.getInventory(), ContainerLevelAccess.create(world, ORIGIN.offset(1, 0, 0)));
        menu.getSlot(0).set(tales()); menu.getSlot(1).set(new ItemStack(Items.LAPIS_LAZULI, 3));
        check(menu.costs[0] > 0 && menu.clickMenuButton(p, 0), "fullpack enchanting action");
        check(EnchantmentTransfer.isTales(menu.getSlot(0).getItem()) && menu.getSlot(0).getItem().isEnchanted(), "fullpack enchanting identity");
        tables++;
    }

    private void shelf() {
        var shelf = (ChiseledBookShelfBlockEntity) world.getBlockEntity(ORIGIN.offset(2, 0, 0));
        ItemStack input = enchanted(ModRegistry.ANCIENT_STRANGE_TALES.get(), Enchantments.SHARPNESS, 255);
        ItemStack remainder = HopperBlockEntity.addItem(null, shelf, input.copy(), Direction.UP);
        check(remainder.isEmpty(), "hopper insertion accepts tales");
        ItemStack returned = shelf.removeItem(0, 1);
        check(ItemStack.matches(input, returned), "shelf output preserves enchantments");
        shelves++;
    }

    private void complete() throws Exception {
        check(positiveCoverage.size() == enchantments.size() * 2, "every registered enchantment transferred in both directions");
        world.setBlockAndUpdate(CHEST, Blocks.CHEST.defaultBlockState());
        ItemStack stored = enchanted(ModRegistry.ANCIENT_STRANGE_TALES.get(), Enchantments.SHARPNESS, 255);
        stored.setHoverName(Component.literal("Tales pressure persistence")); stored.getOrCreateTag().putString("pressureMarker", "20261029");
        ((ChestBlockEntity) world.getBlockEntity(CHEST)).setItem(0, stored);
        List<Double> sorted = batches.stream().sorted().toList();
        String report = "PASS checks=" + checks + " calculations=" + calculations + " crafts=" + crafts + " registeredEnchantments=" + enchantments.size()
                + " positiveEnchantmentDirections=" + positiveCoverage.size() + " anvilChecks=" + anvils + " enchantingActions=" + tables + " bookshelfCycles=" + shelves + " fullInventories=" + full + " shiftClicks=" + shifts
                + " elapsedSeconds=" + (System.nanoTime() - start) / 1e9 + " maxFixtureBatchMs=" + maxBatchNanos / 1e6
                + " p95FixtureBatchMs=" + sorted.get((int)(sorted.size() * 0.95)) + "\nFixture batches only; not whole-pack TPS/FPS or real multiplayer acceptance.";
        Files.writeString(Path.of("server-stress-result.txt"), report);
        System.out.println("TALES_FULLPACK " + report);
        done = true;
    }

    private void fail(Throwable error) { failure = error; error.printStackTrace(); done = true; }
    private void finish(Minecraft mc, String result) {
        finished = true;
        try { Files.writeString(Path.of("client-result.txt"), result); }
        catch (Exception error) { error.printStackTrace(); }
        System.out.println("TALES_FULLPACK " + result);
        mc.stop();
    }
}
