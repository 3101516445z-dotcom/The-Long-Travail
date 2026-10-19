package com.thelongtravail.item;

import com.thelongtravail.AspectTheme;
import com.thelongtravail.flourishing.Mistletoe;
import net.minecraft.network.chat.Component;
import net.minecraft.world.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import java.util.List;

public final class MistletoeItem extends Item {
    public MistletoeItem() {
        super(new Properties());
    }

    @Override
    public Component getName(ItemStack stack) {
        return AspectTheme.FLOURISHING.name(super.getName(stack));
    }

    @Override
    public InteractionResult interactLivingEntity(
            ItemStack stack, Player player, LivingEntity target, InteractionHand hand) {
        return Mistletoe.apply(stack, player, target);
    }

    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> lines, TooltipFlag flag) {
        ItemDescription.summary(lines);
    }
}
