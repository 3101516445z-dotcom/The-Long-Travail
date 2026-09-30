package com.thelongtravail.item;

import com.thelongtravail.TravailAspect;
import com.thelongtravail.data.PlayerJourneyData;
import com.thelongtravail.network.TravailNetwork;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;
import java.util.List;

public final class RevelationStoneItem extends Item {
    private final TravailAspect aspect;

    public RevelationStoneItem(TravailAspect aspect) {
        super(new Item.Properties().stacksTo(64));
        this.aspect = aspect;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player instanceof ServerPlayer serverPlayer) {
            boolean unlocked = PlayerJourneyData.reveal(serverPlayer, aspect);
            TravailNetwork.sendJourney(serverPlayer);
            player.displayClientMessage(Component.translatable(unlocked
                    ? "message.the_long_travail.revealed" : "message.the_long_travail.already_revealed",
                    Component.translatable("aspect.the_long_travail." + aspect.id())), true);
            if (unlocked) {
                if (!player.getAbilities().instabuild) stack.shrink(1);
                TravailNetwork.sendItemSound(serverPlayer, com.thelongtravail.network.ItemSoundCue.stone(aspect));
            }
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.the_long_travail.revelation_stone",
                Component.translatable("aspect.the_long_travail." + aspect.id())));
    }
}
