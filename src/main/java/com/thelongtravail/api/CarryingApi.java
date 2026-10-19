package com.thelongtravail.api;

import com.thelongtravail.config.*;
import com.thelongtravail.registry.ModRegistry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.*;
import top.theillusivec4.curios.api.CuriosApi;
import java.util.*;

// 只能在服务端游戏线程调用；提供方只报告实际携带情况，不判断日记或效果资格。
public final class CarryingApi {
    public enum CarryingResult {
        PRESENT,
        ABSENT,
        UNKNOWN
    }

    private static final Map<ResourceLocation, Long> NEXT_WARNING = new HashMap<>();

    @FunctionalInterface
    public interface Provider {
        boolean carries(ServerPlayer player, Item item);
    }

    private record Signal(ResourceLocation source, Item item) {}

    private record Location(String area, String slot, int index) {}

    private static final Map<ResourceLocation, Provider> PROVIDERS = new LinkedHashMap<>();
    private static final Map<ServerPlayer, Map<Signal, Long>> SIGNALS = new WeakHashMap<>();
    private static final Map<ServerPlayer, Map<Item, Location>> LOCATIONS = new WeakHashMap<>();
    private static final Set<ServerPlayer> QUERYING = Collections.newSetFromMap(new IdentityHashMap<>());
    private static final ResourceLocation LEGACY = new ResourceLocation("the_long_travail", "legacy_inventory_tick");

    public static void registerProvider(ResourceLocation id, Provider provider) {
        Objects.requireNonNull(id);
        Objects.requireNonNull(provider);
        if (PROVIDERS.putIfAbsent(id, provider) != null)
            throw new IllegalArgumentException("Duplicate carrying provider: " + id);
    }

    public static void reportCarried(ServerPlayer player, Item item, ResourceLocation source) {
        checkThread(player);
        SIGNALS.computeIfAbsent(player, p -> new HashMap<>())
                .put(
                        new Signal(Objects.requireNonNull(source), Objects.requireNonNull(item)),
                        player.level().getGameTime());
    }

    public static void inventoryTick(ServerPlayer player, Item item, int slot) {
        if (slot < 0 && allowed(item, "externalInventoryTick")) reportCarried(player, item, LEGACY);
    }

    private static void checkThread(ServerPlayer player) {
        if (!player.server.isSameThread())
            throw new IllegalStateException("CarryingApi requires the server game thread");
    }

    public static boolean allowed(Item item, String area) {
        if (!CarryingConfig.enabled(area)) return false;
        if (item == ModRegistry.EKI.get())
            return switch (area) {
                case "inventory" -> RainConfig.INVENTORY.get();
                case "offhand" -> RainConfig.OFFHAND.get();
                case "armor" -> RainConfig.ARMOR.get();
                case "enderChest" -> RainConfig.ENDER.get();
                case "curios" -> RainConfig.CURIOS.get();
                default -> RainConfig.EXTERNAL_TICK.get();
            };
        return item != ModRegistry.TOWER_OF_BABEL.get() || FarReachRelicsConfig.carrying(area);
    }

    // 返回布尔值的兼容接口仅将已确认存在的物品视为携带；需要保守判定的调用应使用 query 处理 UNKNOWN。
    public static boolean isCarrying(ServerPlayer player, Item item) {
        return query(player, item) == CarryingResult.PRESENT;
    }

    public static CarryingResult query(ServerPlayer player, Item item) {
        checkThread(player);
        if (!player.isAlive() || player.isSpectator()) return CarryingResult.ABSENT;
        if (!QUERYING.add(player)) throw new IllegalStateException("Recursive carrying provider query");
        try {
            var locations = LOCATIONS.computeIfAbsent(player, p -> new HashMap<>());
            var hint = locations.get(item);
            if (hint != null && valid(player, item, hint)) return CarryingResult.PRESENT;
            locations.remove(item);
            for (String area : List.of("inventory", "offhand", "armor", "enderChest")) {
                if (!allowed(item, area)) continue;
                int size =
                        area.equals("enderChest")
                                ? player.getEnderChestInventory().getContainerSize()
                                : stacks(player, area).size();
                for (int i = 0; i < size; i++) {
                    ItemStack stack =
                            area.equals("enderChest")
                                    ? player.getEnderChestInventory().getItem(i)
                                    : stacks(player, area).get(i);
                    if (stack.is(item)) {
                        locations.put(item, new Location(area, "", i));
                        return CarryingResult.PRESENT;
                    }
                }
            }
            if (allowed(item, "curios")) {
                var found =
                        CuriosApi.getCuriosInventory(player)
                                .resolve()
                                .flatMap(
                                        inv ->
                                                inv.findCurios(item).stream()
                                                        .filter(r -> !r.slotContext().cosmetic())
                                                        .findFirst());
                if (found.isPresent()) {
                    var c = found.get().slotContext();
                    locations.put(item, new Location("curios", c.identifier(), c.index()));
                    return CarryingResult.PRESENT;
                }
            }
            if (!allowed(item, "externalInventoryTick")) return CarryingResult.ABSENT;
            var signals = SIGNALS.get(player);
            long now = player.level().getGameTime();
            if (signals != null) {
                signals.entrySet().removeIf(e -> now < e.getValue() || now - e.getValue() > 2);
                if (signals.keySet().stream().anyMatch(s -> s.item == item)) return CarryingResult.PRESENT;
            }
            boolean failed = false;
            for (var entry : List.copyOf(PROVIDERS.entrySet())) {
                try {
                    if (entry.getValue().carries(player, item)) return CarryingResult.PRESENT;
                } catch (RuntimeException ex) {
                    failed = true;
                    long time = System.nanoTime();
                    Long next = NEXT_WARNING.get(entry.getKey());
                    if (next == null || time - next >= 0) {
                        NEXT_WARNING.put(entry.getKey(), time + 60_000_000_000L);
                        com.thelongtravail.TheLongTravail.LOGGER.warn(
                                "Carrying provider {} failed; result remains unknown", entry.getKey(), ex);
                    }
                }
            }
            return failed ? CarryingResult.UNKNOWN : CarryingResult.ABSENT;
        } finally {
            QUERYING.remove(player);
        }
    }

    private static List<ItemStack> stacks(ServerPlayer p, String area) {
        return switch (area) {
            case "inventory" -> p.getInventory().items;
            case "offhand" -> p.getInventory().offhand;
            default -> p.getInventory().armor;
        };
    }

    private static boolean valid(ServerPlayer p, Item item, Location l) {
        if (!allowed(item, l.area) || l.index < 0) return false;
        if (l.area.equals("curios"))
            return CuriosApi.getCuriosInventory(p)
                    .resolve()
                    .flatMap(h -> h.getStacksHandler(l.slot))
                    .map(h -> l.index < h.getStacks().getSlots() && h.getStacks().getStackInSlot(l.index).is(item))
                    .orElse(false);
        if (l.area.equals("enderChest"))
            return l.index < p.getEnderChestInventory().getContainerSize()
                    && p.getEnderChestInventory().getItem(l.index).is(item);
        var stacks = stacks(p, l.area);
        return l.index < stacks.size() && stacks.get(l.index).is(item);
    }

    public static void forget(ServerPlayer player) {
        SIGNALS.remove(player);
        LOCATIONS.remove(player);
    }

    public static void clear() {
        SIGNALS.clear();
        LOCATIONS.clear();
        QUERYING.clear();
        NEXT_WARNING.clear();
    }

    private CarryingApi() {}
}
