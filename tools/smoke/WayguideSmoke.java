package travail.smoke;

import com.mojang.authlib.GameProfile;
import com.thelongtravail.TravailAspect;
import com.thelongtravail.config.TravailConfig;
import com.thelongtravail.data.*;
import com.thelongtravail.entity.WayguideEntity;
import com.thelongtravail.registry.ModRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.*;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.*;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.type.capability.ICurio;
import top.theillusivec4.curios.common.inventory.CurioStacksHandler;
import java.util.*;
import java.nio.file.*;

// 实际服务器逐刻回归：异步请求必须允许世界任务队列正常运行。
public final class WayguideSmoke {
    private final ServerLevel level;
    private final FakePlayer player;
    private final ItemStack diary;
    private final List<WayguideEntity> pearls = new ArrayList<>();
    private final List<ItemEntity> drops = new ArrayList<>();
    private final List<String> messages = new ArrayList<>();
    private int stage, startTick, tick, retryTick, dropsBeforeRetry;
    private boolean cancelPearl, cancelDrop;
    private WayguideEntity pearl;
    private int oldTimeout, oldLimit;
    private static void check(boolean ok, String message) { if (!ok) throw new AssertionError(message); }
    private static ResourceLocation id(String name) { return new ResourceLocation("minecraft", name); }
    private static ListTag ids(String... names) { var list = new ListTag(); for (String n : names) list.add(StringTag.valueOf(id(n).toString())); return list; }
    private void targets(String[] biomes, String[] structures) {
        var root = diary.getTag().getCompound("LongTravail");
        for (var aspect : TravailAspect.values()) {
            var req = root.getCompound("Requirements").getCompound(aspect.id());
            req.put("Biomes", ids(biomes)); req.put("Structures", ids(structures));
            req.put("CompletedBiomes", new ListTag()); req.put("CompletedStructures", new ListTag());
        }
        LongTravailData.requirementsChanged(diary); WayguideSearch.forget(player);
    }
    private Object scanner() throws Exception {
        return WayguidePreparationSupport.scanner(level, diary, player.blockPosition());
    }
    private static Map<?, ?> map(String name) throws Exception {
        var field = WayguideSearch.class.getDeclaredField(name); field.setAccessible(true); return (Map<?, ?>) field.get(null);
    }
    private boolean busy() throws Exception { return map("TASKS").containsKey(player.getUUID()); }
    private void start(boolean creative) {
        player.getAbilities().instabuild = creative; player.getInventory().clearContent();
        var stack = new ItemStack(ModRegistry.WAYGUIDE.get(), 2);
        stack.setHoverName(Component.literal("Kept Wayguide")); stack.getOrCreateTag().putString("ExternalData", "keep");
        player.setItemInHand(InteractionHand.OFF_HAND, stack); player.getCooldowns().removeCooldown(ModRegistry.WAYGUIDE.get());
        check(WayguideSearch.use(player, InteractionHand.OFF_HAND), "starts offhand search");
        pearl = pearls.get(pearls.size() - 1); startTick = tick;
        check(pearl.searching() && player.getOffhandItem().getCount() == (creative ? 2 : 1), "one reserved item, overhead search immediately");
    }
    private int itemCount() { return player.getInventory().items.stream().filter(s -> s.is(ModRegistry.WAYGUIDE.get())).mapToInt(ItemStack::getCount).sum() + (player.getOffhandItem().is(ModRegistry.WAYGUIDE.get()) ? player.getOffhandItem().getCount() : 0); }
    private void forceMiss() throws Exception {
        Object task = map("TASKS").get(player.getUUID()); var sf = task.getClass().getDeclaredField("scanner"); sf.setAccessible(true);
        WayguidePreparationSupport.prepare(task);
        var scanner = sf.get(task); var bf = scanner.getClass().getDeclaredField("biomes"); bf.setAccessible(true);
        bf.set(scanner, Set.of(level.registryAccess().registryOrThrow(Registries.BIOME).getHolderOrThrow(net.minecraft.resources.ResourceKey.create(Registries.BIOME, id("nether_wastes")))));
    }
    public static void run(ServerLevel level) throws Exception { new WayguideSmoke(level); }
    private WayguideSmoke(ServerLevel level) throws Exception {
        this.level = level; WayguideSearch.clear();
        oldTimeout = TravailConfig.WAYGUIDE_TIMEOUT_SECONDS.get(); oldLimit = TravailConfig.WAYGUIDE_MAX_SAMPLES.get();
        player = new FakePlayer(level, new GameProfile(UUID.randomUUID(), "WayguideSmoke")) {
            @Override public void displayClientMessage(Component text, boolean actionBar) {
                if (text.getContents() instanceof net.minecraft.network.chat.contents.TranslatableContents t) messages.add(t.getKey());
            }
        };
        player.setPos(0.5, 90, 0.5);
        var chunk = new net.minecraft.world.level.ChunkPos(player.blockPosition());
        level.getChunkSource().addRegionTicket(net.minecraft.server.level.TicketType.FORCED, chunk, 2, chunk);
        level.getChunkAt(player.blockPosition());
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModRegistry.WAYGUIDE.get(), 2));
        check(!WayguideSearch.use(player, InteractionHand.MAIN_HAND) && player.getMainHandItem().getCount() == 2, "unequipped preserves item");
        var inv = CuriosApi.getCuriosInventory(player).resolve().orElseThrow();
        var slots = new CurioStacksHandler(inv, "travel_diary", 1, true, false, true, ICurio.DropRule.DEFAULT);
        inv.setCurios(new HashMap<>(Map.of("travel_diary", slots)));
        diary = new ItemStack(ModRegistry.LONG_TRAVAIL.get()); check(LongTravailData.tryInitialize(diary, player), "initialized"); slots.getStacks().setStackInSlot(0, diary);
        targets(new String[]{"plains"}, new String[]{"village_plains"});
        check(LongTravailData.remainingBiomes(diary).size() == 1, "deduplicates six chapters");
        check(scanner().getClass().getSimpleName().equals("BiomeScanner"), "unrevealed biomes precede structures");
        int oldRadius = TravailConfig.WAYGUIDE_RADIUS.get(); TravailConfig.WAYGUIDE_RADIUS.set(10000);
        Object radiusScanner = scanner(); var radiusField = radiusScanner.getClass().getSuperclass().getDeclaredField("radius"); radiusField.setAccessible(true);
        check(radiusField.getInt(radiusScanner) == 5000, "runtime hard clamp even with an oversized config value"); TravailConfig.WAYGUIDE_RADIUS.set(oldRadius);
        targets(new String[]{"nether_wastes"}, new String[]{"village_plains"});
        check(scanner().getClass().getSimpleName().equals("StructureScanner"), "dimension-specific structure fallback");
        LongTravailData.visitStructure(diary, id("village_plains")); check(scanner() == null, "completed targets excluded");
        check(WayguideSearch.use(player, InteractionHand.MAIN_HAND), "dimension filtering is deferred");
        for (int n = 0; n < 20 && busy(); n++) WayguideSearch.tick();
        check(!busy() && messages.contains("message.the_long_travail.wayguide.no_targets"), "deferred no-target search refunded");
        var feet = new Vec3(10.5, 80, 20.5); var dest = WayguideGeometry.destination(feet, new BlockPos(10, -40, 20));
        check(dest.equals(feet.add(0, 3, 0)), "same XZ ignores target Y and hovers overhead");
        var crafting = new net.minecraft.world.inventory.TransientCraftingContainer(player.inventoryMenu, 3, 3);
        Item[] recipeItems = {Items.LAPIS_LAZULI, Items.DIAMOND, Items.LAPIS_LAZULI, Items.DIAMOND, Items.NETHER_STAR, Items.DIAMOND, Items.LAPIS_LAZULI, Items.DIAMOND, Items.LAPIS_LAZULI};
        for (int i = 0; i < 9; i++) crafting.setItem(i, new ItemStack(recipeItems[i]));
        check(level.getRecipeManager().getRecipeFor(net.minecraft.world.item.crafting.RecipeType.CRAFTING, crafting, level).orElseThrow().assemble(crafting, level.registryAccess()).is(ModRegistry.WAYGUIDE.get()), "exact recipe");
        targets(new String[]{"plains", "forest", "desert", "ocean", "taiga", "savanna", "river", "swamp", "beach", "snowy_plains"}, new String[]{});
        MinecraftForge.EVENT_BUS.register(this); stage = -1; // 等所有 ServerStarted 配置初始化完成。
    }
    @SubscribeEvent public void capture(EntityJoinLevelEvent event) {
        if (event.getEntity() instanceof WayguideEntity entity) { if (cancelPearl) event.setCanceled(true); else pearls.add(entity); }
        if (event.getEntity() instanceof ItemEntity entity && entity.getItem().is(ModRegistry.WAYGUIDE.get())) { if (cancelDrop) event.setCanceled(true); else drops.add(entity); }
    }
    @SubscribeEvent(priority = EventPriority.LOWEST) public void tick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        tick++;
        try {
            check(tick < 2000, "whole-suite deadline");
            if (stage == -1) {
                start(false); check(!WayguideSearch.use(player, InteractionHand.OFF_HAND), "no repeat while searching");
                player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.STONE)); stage = 0; return;
            }
            if (stage == 0) {
                if (busy()) { if (tick - startTick < 20) check(pearl.searching(), "minimum one-second hover"); return; }
                check(!pearl.searching() && !pearl.isRemoved() && tick - startTick >= 20, "same pearl launches after hover: ticks=" + tick + ", removed=" + pearl.isRemoved() + ", messages=" + messages);
                cancelDrop = true; for (int i = 0; i < TravailConfig.WAYGUIDE_GUIDE_DURATION_SECONDS.get() * 20; i++) pearl.tick(); check(!pearl.isRemoved() && drops.isEmpty(), "cancelled drop retained");
                var saved = pearl.saveWithoutId(new CompoundTag()); pearl.discard(); var restored = ModRegistry.WAYGUIDE_ENTITY.get().create(level); restored.load(saved);
                cancelDrop = false; for (int i = 0; i < 20; i++) restored.tick();
                check(restored.isRemoved() && drops.size() == 1 && drops.get(0).getItem().getTag().getString("ExternalData").equals("keep") && drops.get(0).getItem().getHoverName().getString().equals("Kept Wayguide"), "saved flight retry preserves one custom item");
                cancelPearl = true; player.getCooldowns().removeCooldown(ModRegistry.WAYGUIDE.get());
                check(!WayguideSearch.use(player, InteractionHand.OFF_HAND) && player.getOffhandItem().getCount() == 1, "cancelled spawn preserves held item"); cancelPearl = false;
                start(true); stage = 1;
            } else if (stage == 1) {
                if (busy()) return; check(!pearl.searching() && player.getOffhandItem().getCount() == 2, "cache also hovers, creative preserves stack");
                int before = drops.size(); for (int i = 0; i < TravailConfig.WAYGUIDE_GUIDE_DURATION_SECONDS.get() * 20; i++) pearl.tick(); check(pearl.isRemoved() && drops.size() == before, "creative never duplicates drops");
                targets(new String[]{"mushroom_fields"}, new String[]{"village_plains"}); TravailConfig.WAYGUIDE_MAX_SAMPLES.set(1); start(false); forceMiss(); stage = 2;
            } else if (stage == 2) {
                if (busy()) return; check(pearl.isRemoved() && itemCount() == 2 && messages.get(messages.size() - 1).endsWith("not_found"), "budget failure returns item without structure fallback");
                TravailConfig.WAYGUIDE_MAX_SAMPLES.set(oldLimit); TravailConfig.WAYGUIDE_TIMEOUT_SECONDS.set(1);
                start(false); forceMiss(); stage = 3;
            } else if (stage == 3) {
                if (busy()) return; check(pearl.isRemoved() && itemCount() == 2 && messages.get(messages.size() - 1).endsWith("timeout"), "wall-clock timeout returns exactly one");
                TravailConfig.WAYGUIDE_TIMEOUT_SECONDS.set(oldTimeout); start(false); LongTravailData.requirementsChanged(diary); WayguideSearch.tick();
                check(!busy() && itemCount() == 2, "diary changes cancel and refund");
                stage = 30;
            } else if (stage == 30) {
                start(false); WayguideSearch.clear(); check(!busy() && itemCount() == 2, "reload/server clear refunds");
                start(false); diary.setCount(0); WayguideSearch.tick(); diary.setCount(1);
                check(!busy() && itemCount() == 2, "unequipping diary returns reserved item");
                stage = 31;
            } else if (stage == 31) {
                start(false); player.setPos(300.5, 90, 0.5); WayguideSearch.tick(); player.setPos(0.5, 90, 0.5);
                check(!busy() && itemCount() == 2, "moving away from origin cancels and returns item");
                start(false); player.setHealth(0); WayguideSearch.tick();
                check(!busy() && itemCount() == 1 && WayguideReturns.get(level.getServer()).pendingRecords() == 1, "death retains item outside old inventory");
                var respawned = new FakePlayer(level, player.getGameProfile());
                WayguideReturns.get(level.getServer()).deliver(respawned); WayguideReturns.get(level.getServer()).deliver(respawned);
                check(respawned.getInventory().items.stream().filter(s -> s.is(ModRegistry.WAYGUIDE.get())).mapToInt(ItemStack::getCount).sum() == 1 && WayguideReturns.get(level.getServer()).pendingRecords() == 0, "respawned owner receives exactly one reserved item");
                player.setHealth(20);
                stage = 32;
            } else if (stage == 32) {
                start(false); for (int i = 0; i < player.getInventory().items.size(); i++) player.getInventory().items.set(i, new ItemStack(Items.STONE, 64));
                cancelDrop = true; WayguideSearch.forget(player); check(WayguideReturns.get(level.getServer()).pendingRecords() == 1, "full inventory cancelled drop kept in saved queue");
                var queueSaved = WayguideReturns.get(level.getServer()).save(new CompoundTag()); check(queueSaved.getList("Returns", Tag.TAG_COMPOUND).size() == 1, "offline return persists");
                var loadMethod = WayguideReturns.class.getDeclaredMethod("load", CompoundTag.class); loadMethod.setAccessible(true);
                check(((WayguideReturns) loadMethod.invoke(null, queueSaved)).pendingRecords() == 1, "saved return record round-trips");
                cancelDrop = false; dropsBeforeRetry = drops.size(); retryTick = tick; stage = 33;
            } else if (stage == 33) {
                WayguideReturns.get(level.getServer()).deliver(player); WayguideReturns.get(level.getServer()).deliver(player);
                if (tick - retryTick < 20) { check(drops.size() == dropsBeforeRetry, "failed drop waits twenty ticks despite repeated immediate calls"); return; }
                check(drops.size() == dropsBeforeRetry + 1 && WayguideReturns.get(level.getServer()).pendingRecords() == 0, "full inventory drops once on retry");
                start(false); var saved = pearl.saveWithoutId(new CompoundTag()); WayguideSearch.forget(player);
                var restored = ModRegistry.WAYGUIDE_ENTITY.get().create(level); restored.load(saved); restored.tick(); WayguideReturns.get(level.getServer()).deliver(player);
                check(itemCount() == 2 && restored.isRemoved(), "stale saved search cannot duplicate an already returned escrow");
                targets(new String[]{}, new String[]{"mineshaft"}); start(false); stage = 4;
            } else if (stage == 4 || stage == 5) {
                if (busy()) return;
                String structure = stage == 4 ? "mineshaft" : "stronghold";
                check(!pearl.searching() && !pearl.isRemoved(), "async real structure found: " + structure);
                Object cache = map("CACHE").get(player.getUUID()); var tf = cache.getClass().getDeclaredField("target"); tf.setAccessible(true); Object target = tf.get(cache);
                var pf = target.getClass().getDeclaredField("pos"); pf.setAccessible(true); BlockPos pos = (BlockPos) pf.get(target);
                check(Math.hypot(pos.getX() - 0, pos.getZ() - 0) <= 5000, "5000 hard radius");
                var cs = new net.minecraft.world.level.ChunkPos(pos); var structureType = level.registryAccess().registryOrThrow(Registries.STRUCTURE).get(id(structure));
                var start = level.getChunk(cs.x, cs.z, net.minecraft.world.level.chunk.ChunkStatus.STRUCTURE_STARTS).getStartForStructure(structureType);
                check(start != null && start.isValid(), "target is a real structure start");
                pearl.refund(player);
                if (stage == 4) { targets(new String[]{}, new String[]{"stronghold"}); start(false); stage = 5; } else finish(true, "PASS");
            }
        } catch (Throwable error) { error.printStackTrace(); finish(false, error.toString()); }
    }
    private void finish(boolean pass, String detail) {
        WayguideSearch.clear(); pearls.forEach(WayguideEntity::discard); drops.forEach(ItemEntity::discard);
        var chunk = new net.minecraft.world.level.ChunkPos(player.blockPosition());
        level.getChunkSource().removeRegionTicket(net.minecraft.server.level.TicketType.FORCED, chunk, 2, chunk);
        TravailConfig.WAYGUIDE_TIMEOUT_SECONDS.set(oldTimeout); TravailConfig.WAYGUIDE_MAX_SAMPLES.set(oldLimit); MinecraftForge.EVENT_BUS.unregister(this);
        try { Files.writeString(Path.of(System.getProperty("travail.smoke.result")), pass ? "PASS" : "FAIL: " + detail); } catch (Exception e) { throw new RuntimeException(e); }
        System.out.println("TRAVAIL_WAYGUIDE_" + (pass ? "PASS" : "FAIL") + ": " + detail); level.getServer().halt(false);
    }
}
