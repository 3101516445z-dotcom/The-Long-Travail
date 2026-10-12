package com.thelongtravail.item;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;
import java.util.List;

public final class RenewalItem extends Item {
    public RenewalItem() {
        super(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC));
    }

    @Override
    public Component getName(ItemStack stack) {
        return super.getName(stack)
                .copy()
                .withStyle(
                        style -> style.withColor(0xC5AA77).withInsertion("the_long_travail:name"));
    }

    @Override
    public void appendHoverText(
            ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        for (int index = 0; index < 3; index++)
            tooltip.add(
                    Component.translatable("tooltip.the_long_travail.renewal.prose." + index)
                            .withStyle(style -> style.withColor(0xBC995E)));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(
            Level level, Player player, InteractionHand hand) {
        if (player.getCooldowns().isOnCooldown(this))
            return InteractionResultHolder.fail(player.getItemInHand(hand));
        if (player instanceof ServerPlayer serverPlayer) {
            var result = com.thelongtravail.data.JourneyService.resetJourney(serverPlayer, hand);
            if (!result.changed()) {
                if (result.status() == com.thelongtravail.data.JourneyOperations.Status.NO_DIARY)
                    return unavailable(player, hand);
                player.displayClientMessage(
                        com.thelongtravail.helper.ActionMessages.text(
                                result.status()
                                                == com.thelongtravail.data.JourneyOperations.Status
                                                        .INVALID_POOL
                                        ? "message.the_long_travail.journey.invalid_pool"
                                        : "message.the_long_travail.journey."
                                                + result.status()
                                                        .name()
                                                        .toLowerCase(java.util.Locale.ROOT)),
                        true);
                return InteractionResultHolder.fail(player.getItemInHand(hand));
            }
            player.getCooldowns().addCooldown(this, 20);
            com.thelongtravail.network.TravailNetwork.sendItemSound(
                    serverPlayer, com.thelongtravail.network.ItemSoundCue.RENEWAL);
            player.displayClientMessage(
                    com.thelongtravail.helper.ActionMessages.text(
                            "message.the_long_travail.renewal.success"),
                    true);
        }
        return InteractionResultHolder.sidedSuccess(player.getItemInHand(hand), level.isClientSide);
    }

    private InteractionResultHolder<ItemStack> unavailable(Player player, InteractionHand hand) {
        player.displayClientMessage(
                com.thelongtravail.helper.ActionMessages.text(
                        "message.the_long_travail.homecoming.unavailable"),
                true);
        return InteractionResultHolder.fail(player.getItemInHand(hand));
    }
}
