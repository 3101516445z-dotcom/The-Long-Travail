package com.thelongtravail.item;

import com.thelongtravail.data.LongTravailData;
import com.thelongtravail.event.TravailEvents;
import com.thelongtravail.helper.TravailCurios;
import com.thelongtravail.registry.ModRegistry;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;
import top.theillusivec4.curios.api.CuriosApi;
import java.util.List;

public final class RenewalItem extends Item {
    public RenewalItem() { super(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC)); }
    @Override
    public Component getName(ItemStack stack) {
        return super.getName(stack).copy().withStyle(style -> style
                .withColor(0xC5AA77).withInsertion("the_long_travail:name"));
    }
    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        for (int index = 0; index < 3; index++)
            tooltip.add(Component.translatable("tooltip.the_long_travail.renewal.prose." + index)
                    .withStyle(style -> style.withColor(0xBC995E)));
    }
    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        if (player.getCooldowns().isOnCooldown(this)) return InteractionResultHolder.fail(player.getItemInHand(hand));
        if (player instanceof ServerPlayer serverPlayer) {
            var found = TravailCurios.find(player);
            var inventory = CuriosApi.getCuriosInventory(player).resolve();
            var slots = found.isPresent() && inventory.isPresent()
                    ? inventory.get().getStacksHandler(found.get().slotContext().identifier())
                    : java.util.Optional.<top.theillusivec4.curios.api.type.inventory.ICurioStacksHandler>empty();
            if (slots.isEmpty()) return unavailable(player, hand);
            int index = found.get().slotContext().index();
            var stacks = slots.get().getStacks();
            ItemStack equipped = stacks.getStackInSlot(index);
            if (!equipped.is(ModRegistry.LONG_TRAVAIL.get())) return unavailable(player, hand);
            ItemStack refreshed = LongTravailData.refreshedCopy(equipped, serverPlayer);
            if (refreshed.isEmpty()) {
                player.displayClientMessage(Component.translatable("message.the_long_travail.journey.invalid_pool"), true);
                return InteractionResultHolder.fail(player.getItemInHand(hand));
            }
            stacks.setStackInSlot(index, refreshed);
            // 先消耗物品再返还额外槽位的饰品，因为返还物品可能复用刚清空的手持槽。
            if (!player.getAbilities().instabuild) player.getItemInHand(hand).shrink(1);
            player.getCooldowns().addCooldown(this, 20);
            TravailEvents.onDiaryReset(serverPlayer, refreshed);
            player.getInventory().setChanged();
            player.containerMenu.broadcastChanges();
            com.thelongtravail.network.TravailNetwork.sendItemSound(serverPlayer, com.thelongtravail.network.ItemSoundCue.RENEWAL);
            player.displayClientMessage(Component.translatable("message.the_long_travail.renewal.success"), true);
        }
        return InteractionResultHolder.sidedSuccess(player.getItemInHand(hand), level.isClientSide);
    }
    private InteractionResultHolder<ItemStack> unavailable(Player player, InteractionHand hand) {
        player.displayClientMessage(Component.translatable("message.the_long_travail.homecoming.unavailable"), true);
        return InteractionResultHolder.fail(player.getItemInHand(hand));
    }
}
