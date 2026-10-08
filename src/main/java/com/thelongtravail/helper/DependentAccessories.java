package com.thelongtravail.helper;

import com.thelongtravail.TheLongTravail;
import com.thelongtravail.registry.ModRegistry;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.SlotContext;
import java.util.*;
import java.util.function.*;

// 前置条件以槽位操作完成后的实际装备为准，不保存玩家或 ItemStack 引用。
@Mod.EventBusSubscriber(modid = TheLongTravail.MODID)
public final class DependentAccessories {
    private record Rule(Supplier<Item> item, Predicate<ServerPlayer> requirement, Consumer<ServerPlayer> cleanup) {}
    private record Position(String slot, int index, Item item) {}
    private static final List<Rule> RULES = new ArrayList<>();
    private static final Set<ServerPlayer> CHECKING = Collections.newSetFromMap(new IdentityHashMap<>());
    static {
        register(ModRegistry.THOUSAND_YEARS, p -> !TravailCurios.stack(p).isEmpty(), p -> {});
        register(ModRegistry.BOOK_OF_DEAD, p -> !TravailCurios.stack(p).isEmpty(), p -> {});
        register(ModRegistry.STAR_VOICE, p -> !TravailCurios.stack(p).isEmpty(), p -> {});
        register(ModRegistry.AZRAEL, p -> !TravailCurios.stack(p).isEmpty(), com.thelongtravail.valley.AzraelState::forget);
    }
    public static void register(Supplier<Item> item, Predicate<ServerPlayer> requirement, Consumer<ServerPlayer> cleanup) {
        RULES.add(new Rule(item, requirement, cleanup));
    }
    // 当前槽位的同类替换允许通过；不同物品互不排斥。
    public static boolean unique(SlotContext context, Item item) {
        if (!(context.entity() instanceof Player player)) return false;
        return CuriosApi.getCuriosInventory(player).map(inv -> inv.findCurios(item).stream()
                .filter(r -> !r.slotContext().cosmetic())
                .noneMatch(r -> !r.slotContext().identifier().equals(context.identifier())
                        || r.slotContext().index() != context.index())).orElse(false);
    }
    // 道具完成槽位修改后可主动调用；普通 NBT 更新和卸装回调留到本刻末尾复查。
    public static void reconcile(ServerPlayer player) {
        if (!CHECKING.add(player)) return;
        try {
            for (Rule rule : List.copyOf(RULES)) {
                if (rule.requirement.test(player)) continue;
                rule.cleanup.accept(player);
                var inventory = CuriosApi.getCuriosInventory(player).resolve();
                if (inventory.isEmpty()) continue;
                var positions = inventory.get().findCurios(rule.item.get()).stream()
                        .filter(r -> !r.slotContext().cosmetic())
                        .map(r -> new Position(r.slotContext().identifier(), r.slotContext().index(), rule.item.get())).toList();
                for (Position position : positions) {
                    if (rule.requirement.test(player)) break;
                    var current = CuriosApi.getCuriosInventory(player).resolve()
                            .flatMap(inv -> inv.getStacksHandler(position.slot));
                    if (current.isEmpty()) continue;
                    var stacks = current.get().getStacks();
                    if (position.index >= stacks.getSlots()) continue;
                    ItemStack stack = stacks.getStackInSlot(position.index);
                    if (!stack.is(position.item)) continue;
                    ItemStack returning = stack.copy();
                    stacks.setStackInSlot(position.index, ItemStack.EMPTY);
                    TravailCurios.returnToInventoryOrDrop(player, returning);
                }
            }
        } finally { CHECKING.remove(player); }
    }
    @SubscribeEvent public static void tick(TickEvent.PlayerTickEvent event) {
        if (event.phase == TickEvent.Phase.END && event.player instanceof ServerPlayer player) reconcile(player);
    }
    private DependentAccessories() {}
}
