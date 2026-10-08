package com.thelongtravail.client;

import com.thelongtravail.data.LongTravailData;
import com.thelongtravail.registry.ModRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.item.ItemStack;
import top.theillusivec4.curios.api.CuriosApi;
import java.util.UUID;
import java.util.function.Supplier;

// 阅读身份只读；槽位同步产生的新 ItemStack 不代表新旅程。
public final class DiaryReadingTarget implements Supplier<ItemStack> {
    public record Location(Object owner, String kind, String slot, int index) {}
    public record Observation(Location location, Supplier<ItemStack> source, ItemStack stack, double x, double y) {}
    private final Observation origin;
    private final ItemStack initial;
    private UUID journey;
    private final boolean initiallyUninitialized;
    private ItemStack preview;

    public DiaryReadingTarget(Observation origin) {
        this.origin = origin;
        initial = origin.stack().copy();
        journey = journey(initial);
        initiallyUninitialized = !LongTravailData.isInitialized(initial);
        preview = origin.stack();
    }
    private static UUID journey(ItemStack stack) { return LongTravailData.queryIdentity(stack).journey(); }
    private boolean accepts(ItemStack stack) {
        if (!stack.is(ModRegistry.LONG_TRAVAIL.get())) return false;
        UUID current = journey(stack);
        if (journey != null) return journey.equals(current);
        if (origin.location() != null && initiallyUninitialized && current != null && LongTravailData.isInitialized(stack)) {
            journey = current;
            return true;
        }
        return ItemStack.matches(initial, stack);
    }
    public boolean matches(Observation observation) {
        if (observation == null) return false;
        if (origin.location() != null) {
            if (!origin.location().equals(observation.location())) return false;
            return accepts(observation.stack()) && accepts(origin.source().get());
        }
        // 无槽位的配方/预览没有可靠的位置身份，要求鼠标保持在原位置。
        if (observation.location() != null || Math.abs(origin.x() - observation.x()) >= .5
                || Math.abs(origin.y() - observation.y()) >= .5 || !accepts(observation.stack())) return false;
        preview = observation.stack();
        return true;
    }
    @Override public ItemStack get() {
        ItemStack current = origin.location() == null ? preview : origin.source().get();
        return accepts(current) ? current : ItemStack.EMPTY;
    }
    public static Observation observe(Screen screen, ItemStack stack, double x, double y) {
        var player = Minecraft.getInstance().player;
        if (player != null) {
            var curios = CuriosApi.getCuriosInventory(player).resolve();
            if (curios.isPresent()) for (var entry : curios.get().getCurios().entrySet()) {
                for (boolean cosmetic : new boolean[]{false, true}) {
                    var stacks = cosmetic ? entry.getValue().getCosmeticStacks() : entry.getValue().getStacks();
                    for (int i = 0; i < stacks.getSlots(); i++) if (stacks.getStackInSlot(i) == stack) {
                        String id = entry.getKey(); int index = i;
                        return new Observation(new Location(player, cosmetic ? "cosmetic" : "curios", id, i), () ->
                            CuriosApi.getCuriosInventory(player).resolve().flatMap(h -> h.getStacksHandler(id)).map(h -> {
                                var current = cosmetic ? h.getCosmeticStacks() : h.getStacks();
                                return index < current.getSlots() ? current.getStackInSlot(index) : ItemStack.EMPTY;
                            }).orElse(ItemStack.EMPTY), stack, x, y);
                    }
                }
            }
        }
        if (screen instanceof AbstractContainerScreen<?> container) {
            var menu = container.getMenu();
            for (int i = 0; i < menu.slots.size(); i++) if (menu.slots.get(i).getItem() == stack) {
                int index = i;
                return new Observation(new Location(menu, "menu", "", index),
                        () -> index < menu.slots.size() ? menu.slots.get(index).getItem() : ItemStack.EMPTY, stack, x, y);
            }
        }
        if (player != null) {
            var inventory = player.getInventory();
            for (int i = 0; i < inventory.getContainerSize(); i++) if (inventory.getItem(i) == stack) {
                int index = i;
                return new Observation(new Location(inventory, "inventory", "", i), () -> inventory.getItem(index), stack, x, y);
            }
        }
        return new Observation(null, () -> stack, stack, x, y);
    }
}
