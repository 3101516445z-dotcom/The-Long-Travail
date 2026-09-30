package com.thelongtravail.item;

import com.thelongtravail.helper.TravailCurios;
import com.thelongtravail.registry.ModRegistry;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;
import top.theillusivec4.curios.api.CuriosApi;
import java.util.List;

public final class HomecomingItem extends Item {
    public HomecomingItem() { super(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC)); }

    @Override
    public Component getName(ItemStack stack) {
        return super.getName(stack).copy().withStyle(style -> style
                .withColor(0xC5AA77).withInsertion("the_long_travail:name"));
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        for (int index = 0; index < 4; index++)
            tooltip.add(Component.translatable("tooltip.the_long_travail.homecoming.prose." + index)
                    .withStyle(style -> style.withColor(0xBC995E)));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        if (player instanceof ServerPlayer serverPlayer) {
            var found = TravailCurios.find(player);
            var inventory = CuriosApi.getCuriosInventory(player).resolve();
            var slots = found.isPresent() && inventory.isPresent()
                    ? inventory.get().getStacksHandler(found.get().slotContext().identifier()) : java.util.Optional.<top.theillusivec4.curios.api.type.inventory.ICurioStacksHandler>empty();
            if (slots.isEmpty()) return unavailable(player, hand);
            int index = found.get().slotContext().index();
            var stacks = slots.get().getStacks();
            ItemStack equipped = stacks.getStackInSlot(index);
            if (!equipped.is(ModRegistry.LONG_TRAVAIL.get())) return unavailable(player, hand);

            ItemStack returning = equipped.copy(); // Preserve the diary's complete progress/NBT.
            stacks.setStackInSlot(index, ItemStack.EMPTY); // Intentional server-side release of the binding.
            if (!player.getAbilities().instabuild) player.getItemInHand(hand).shrink(1);
            TravailCurios.returnToInventoryOrDrop(player, returning);
            TravailCurios.syncExtraSlots(serverPlayer, 0);
            player.getInventory().setChanged();
            player.containerMenu.broadcastChanges();
            com.thelongtravail.network.TravailNetwork.sendItemSound(serverPlayer, com.thelongtravail.network.ItemSoundCue.HOMECOMING);
            player.displayClientMessage(Component.translatable("message.the_long_travail.homecoming.success"), true);
        }
        // Inventory insertion may have reused the now-empty hand slot; return its current stack.
        return InteractionResultHolder.sidedSuccess(player.getItemInHand(hand), level.isClientSide);
    }

    private InteractionResultHolder<ItemStack> unavailable(Player player, InteractionHand hand) {
        player.displayClientMessage(Component.translatable("message.the_long_travail.homecoming.unavailable"), true);
        return InteractionResultHolder.fail(player.getItemInHand(hand));
    }
}
