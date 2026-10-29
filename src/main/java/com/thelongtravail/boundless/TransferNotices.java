package com.thelongtravail.boundless;

import com.thelongtravail.TheLongTravail;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import java.util.Map;
import java.util.WeakHashMap;

/** 服务端提示同一组失败输入一次；失败预览不得继续领取。 */
@Mod.EventBusSubscriber(modid = TheLongTravail.MODID)
public final class TransferNotices {
    private record Notice(AbstractContainerMenu menu, EnchantmentTransfer.Failure failure) {}

    private static final Map<Player, Notice> shown = new WeakHashMap<>();

    public static boolean blocked(AbstractContainerMenu menu, Player player) {
        if (player.level().isClientSide) return false;
        if (!(menu instanceof InventoryMenu || menu instanceof CraftingMenu)
                || menu.slots.size() < 2
                || !(menu.getSlot(1).container instanceof CraftingContainer grid)) return false;
        var failure = EnchantmentTransferRecipe.failure(grid);
        if (failure == null) {
            shown.remove(player);
            return false;
        }
        Notice previous = shown.get(player);
        boolean repeatedUnknown =
                previous != null
                        && failure.enchantment() == null
                        && failure.equals(previous.failure());
        if (previous == null
                || previous.menu() != menu
                || previous.failure() != failure && !repeatedUnknown) {
            player.displayClientMessage(failure.message(), true);
            shown.put(player, new Notice(menu, failure));
        }
        // 在 vanilla 真正减少输入数量前清除失效结果并同步。
        if (menu.getSlot(0).hasItem()) {
            menu.getSlot(0).set(ItemStack.EMPTY);
            menu.broadcastChanges();
        }
        return true;
    }

    @SubscribeEvent
    public static void tick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.player.level().isClientSide) return;
        if (!(event.player.containerMenu instanceof InventoryMenu
                || event.player.containerMenu instanceof CraftingMenu)) shown.remove(event.player);
        else blocked(event.player.containerMenu, event.player);
    }

    @SubscribeEvent
    public static void logout(
            net.minecraftforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent event) {
        shown.remove(event.getEntity());
    }

    @SubscribeEvent
    public static void stopped(net.minecraftforge.event.server.ServerStoppedEvent event) {
        shown.clear();
    }

    private TransferNotices() {}
}
