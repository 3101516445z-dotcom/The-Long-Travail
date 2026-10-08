package travail.smoke;

import com.mojang.authlib.GameProfile;
import com.thelongtravail.TravailAspect;
import com.thelongtravail.data.*;
import com.thelongtravail.helper.TravailCurios;
import com.thelongtravail.registry.ModRegistry;
import net.minecraft.nbt.*;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.util.FakePlayer;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.type.capability.ICurio;
import top.theillusivec4.curios.common.inventory.CurioStacksHandler;
import java.util.*;
import java.util.concurrent.CompletableFuture;

public final class WayguideSchedulingSmoke {
    private static void check(boolean ok, String message) { if (!ok) throw new AssertionError(message); }
    private static Object get(Object object, String field) throws Exception {
        return WayguidePreparationSupport.field(object.getClass(), field).get(object);
    }
    private static Map<?, ?> tasks() throws Exception {
        return (Map<?, ?>) WayguidePreparationSupport.field(WayguideSearch.class, "TASKS").get(null);
    }
    private static FakePlayer player(ServerLevel level) {
        var player = new FakePlayer(level, new GameProfile(UUID.randomUUID(), "ScheduleSmoke")) {
            @Override public void displayClientMessage(Component text, boolean actionBar) {}
        };
        player.setPos(0.5, 90, 0.5);
        var inv = CuriosApi.getCuriosInventory(player).resolve().orElseThrow();
        var slots = new CurioStacksHandler(inv, "travel_diary", 1, true, false, true, ICurio.DropRule.DEFAULT);
        inv.setCurios(new HashMap<>(Map.of("travel_diary", slots)));
        var diary = new ItemStack(ModRegistry.LONG_TRAVAIL.get());
        check(LongTravailData.tryInitialize(diary, player), "initialize"); slots.getStacks().setStackInSlot(0, diary);
        var requirements = diary.getTag().getCompound("LongTravail").getCompound("Requirements");
        for (var aspect : TravailAspect.values()) {
            requirements.getCompound(aspect.id()).put("Biomes", new ListTag());
            requirements.getCompound(aspect.id()).put("Structures", new ListTag());
        }
        var ids = new ListTag();
        for (int i = 0; i < 5000; i++) ids.add(StringTag.valueOf("schedule_smoke:missing_" + i));
        ids.add(StringTag.valueOf("minecraft:mushroom_fields"));
        requirements.getCompound(TravailAspect.FLOURISHING.id()).put("Biomes", ids);
        LongTravailData.requirementsChanged(diary);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModRegistry.WAYGUIDE.get(), 2));
        check(WayguideSearch.use(player, InteractionHand.MAIN_HAND), "accept pending preparation");
        return player;
    }
    private static int count(FakePlayer player) {
        return player.getInventory().items.stream().filter(s -> s.is(ModRegistry.WAYGUIDE.get())).mapToInt(ItemStack::getCount).sum();
    }
    public static void run(ServerLevel level) throws Exception {
        borderChange(level);
        ringOrder(level);
        replacementDuringPreparation(level);
        WayguideSearch.clear(); List<FakePlayer> players = new ArrayList<>();
        try {
            for (int i = 0; i < 8; i++) players.add(player(level));
            for (var player : players) {
                var task = tasks().get(player.getUUID());
                check(get(task, "scanner") == null && get(get(task, "preparation"), "targets") == null,
                        "right click must not enumerate targets or construct scanner");
            }
            WayguideSearch.tick();
            for (var task : tasks().values()) check((int) get(task, "samples") == 0, "preparation does not consume samples");
            for (int n = 0; n < 200; n++) {
                WayguideSearch.tick();
                boolean all = true;
                for (var task : tasks().values()) {
                    var prep = get(task, "preparation");
                    if (prep != null && (int) get(prep, "phase") == 0) all = false;
                }
                if (all) break;
                if (n == 199) throw new AssertionError("preparation starvation");
            }
            var cancelled = players.get(0); WayguideSearch.forget(cancelled);
            check(!tasks().containsKey(cancelled.getUUID()) && count(cancelled) == 2, "forget during preparation refunds once");
            var changed = players.get(1); LongTravailData.requirementsChanged(TravailCurios.stack(changed));
            var expired = players.get(2); var expiredTask = tasks().get(expired.getUUID());
            WayguidePreparationSupport.field(expiredTask.getClass(), "started").setLong(expiredTask,
                    System.nanoTime() - (long) get(expiredTask, "timeout") - 1);
            for (int n = 0; n < 200 && (tasks().containsKey(changed.getUUID()) || tasks().containsKey(expired.getUUID())); n++) WayguideSearch.tick();
            check(!tasks().containsKey(changed.getUUID()) && count(changed) == 2, "changed journey cancels queued preparation");
            check(!tasks().containsKey(expired.getUUID()) && count(expired) == 2, "timeout includes queued preparation");
            WayguideSearch.stopped();
            for (var player : players) check(count(player) == 2, "stop returns every remaining reservation");
            check(tasks().isEmpty(), "stop drains tasks");
            System.out.println("WAYGUIDE_SCHEDULING_PASS: deferred setup, eight-player fairness, sample accounting, revision cancellation, preparation timeout, stop refunds");
        } finally { WayguideSearch.stopped(); players.forEach(JourneyQueries::forget); }
    }
    private static void borderChange(ServerLevel level) throws Exception {
        WayguideSearch.clear();var border=level.getWorldBorder();
        double size=border.getSize(),cx=border.getCenterX(),cz=border.getCenterZ();
        var player=player(level);
        try {
            var task=tasks().get(player.getUUID());
            var targetType=Class.forName(WayguideSearch.class.getName()+"$Target");
            var targetCtor=targetType.getDeclaredConstructors()[0];targetCtor.setAccessible(true);
            Object target=targetCtor.newInstance(new net.minecraft.resources.ResourceLocation("minecraft:mushroom_fields"),new net.minecraft.core.BlockPos(100,90,0));
            var scannerType=Class.forName(WayguideSearch.class.getName()+"$CachedScanner");
            var scannerCtor=scannerType.getDeclaredConstructors()[0];scannerCtor.setAccessible(true);
            WayguidePreparationSupport.field(task.getClass(),"scanner").set(task,scannerCtor.newInstance(level,player.blockPosition(),target));
            WayguidePreparationSupport.field(task.getClass(),"preparation").set(task,null);
            WayguidePreparationSupport.field(task.getClass(),"startTick").setLong(task,level.getGameTime()-20);
            border.setCenter(0,0);border.setSize(32);
            for(int n=0;n<100&&tasks().containsKey(player.getUUID());n++)WayguideSearch.tick();
            check(!tasks().containsKey(player.getUUID())&&count(player)==2,"border shrink before launch cancels and refunds instead of launching outside");
            var cacheType=Class.forName(WayguideSearch.class.getName()+"$Cached");
            var cacheCtor=cacheType.getDeclaredConstructors()[0];cacheCtor.setAccessible(true);
            Object cached=cacheCtor.newInstance(get(task,"journey"),level.dimension(),player.blockPosition(),target,level.getGameTime()+100);
            @SuppressWarnings("unchecked") var cache=(Map<UUID,Object>)WayguidePreparationSupport.field(WayguideSearch.class,"CACHE").get(null);
            cache.put(player.getUUID(),cached);player.getCooldowns().removeCooldown(ModRegistry.WAYGUIDE.get());
            check(WayguideSearch.use(player,InteractionHand.MAIN_HAND),"new search after border change accepted");
            check(get(tasks().get(player.getUUID()),"scanner")==null,"outside-border cached target must be invalidated");
            WayguideSearch.forget(player);check(count(player)==2,"new search cancellation refunds once");
            System.out.println("WAYGUIDE_BORDER_PASS: border rechecked before launch and cache reuse; refund once");
        } finally {WayguideSearch.forget(player);JourneyQueries.forget(player);border.setCenter(cx,cz);border.setSize(size);}
    }
    private static void replacementDuringPreparation(ServerLevel level) throws Exception {
        WayguideSearch.clear();
        var player = player(level);
        try {
            var old = TravailCurios.stack(player);
            var current = old.copy();
            CuriosApi.getCuriosInventory(player).resolve().orElseThrow().getStacksHandler("travel_diary")
                    .orElseThrow().getStacks().setStackInSlot(0, current);
            // 正常的同旅程替换应继续搜索；旧对象被移除后不再是当前装备的权威数据。
            var requirements = old.getTag().getCompound("LongTravail").getCompound("Requirements");
            for (var aspect : TravailAspect.values()) requirements.getCompound(aspect.id()).put("Biomes", new ListTag());
            LongTravailData.requirementsChanged(old);
            for (int n = 0; n < 100; n++) {
                var task = tasks().get(player.getUUID());
                check(task != null, "same-journey replacement must not read detached old diary during preparation");
                var prep = get(task, "preparation");
                if (prep == null || (int) get(prep, "phase") > 0) break;
                WayguideSearch.tick();
            }
            var task = tasks().get(player.getUUID());
            check(task != null, "replacement stays eligible");
            var prep = get(task, "preparation");
            if (prep != null) check(get(prep, "diary") == current, "preparation follows currently equipped diary");
            System.out.println("WAYGUIDE_REPLACEMENT_PASS: same-journey copy continues with current equipment, detached old object ignored");
        } finally { WayguideSearch.forget(player); JourneyQueries.forget(player); }
    }
    private static void ringOrder(ServerLevel level) throws Exception {
        var structure = level.registryAccess().registryOrThrow(net.minecraft.core.registries.Registries.STRUCTURE)
                .getHolderOrThrow(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.STRUCTURE,
                        new net.minecraft.resources.ResourceLocation("minecraft", "stronghold")));
        var placement = level.getChunkSource().getGeneratorState().getPlacementsForStructure(structure).stream()
                .filter(p -> p instanceof net.minecraft.world.level.levelgen.structure.placement.ConcentricRingsStructurePlacement)
                .findFirst().orElseThrow();
        var type = Class.forName(WayguideSearch.class.getName() + "$PlacementCursor");
        var constructor = type.getDeclaredConstructors()[0]; constructor.setAccessible(true);
        var origin = new net.minecraft.core.BlockPos(8, 90, 8);
        var cursor = constructor.newInstance(level, origin, 5000, placement, List.of(structure));
        List<net.minecraft.world.level.ChunkPos> rings = new ArrayList<>();
        for (int n = 64; n > 0; n--) {
            rings.add(new net.minecraft.world.level.ChunkPos(n, 0));
            rings.add(new net.minecraft.world.level.ChunkPos(-n, 0));
        }
        WayguidePreparationSupport.field(type, "ringFuture").set(cursor, CompletableFuture.completedFuture(rings));
        var budgetType = Class.forName(WayguideSearch.class.getName() + "$LoadBudget");
        var budgetConstructor = budgetType.getDeclaredConstructor(); budgetConstructor.setAccessible(true);
        var ready = type.getDeclaredMethod("ready", budgetType); ready.setAccessible(true);
        check(!(boolean) ready.invoke(cursor, budgetConstructor.newInstance()), "large ring preparation must yield");
        boolean complete = false;
        for (int n = 0; n < 100 && !complete; n++) complete = (boolean) ready.invoke(cursor, budgetConstructor.newInstance());
        check(complete, "ring preparation completes");
        var expected = rings.stream().sorted(Comparator.comparingDouble(p -> p.getMiddleBlockPosition(origin.getY()).distSqr(origin))).toList();
        var next = type.getDeclaredMethod("next"); next.setAccessible(true);
        for (var pos : expected) check(pos.equals(next.invoke(cursor)), "stable nearest ring order, including distance ties");
        check(next.invoke(cursor) == null, "ring exhaustion");
        System.out.println("WAYGUIDE_RING_ORDER_PASS: 128 positions, incremental preparation and stable distance ties");
    }
}
