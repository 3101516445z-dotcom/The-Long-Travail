package com.thelongtravail.item;

import com.thelongtravail.AspectTheme;
import com.thelongtravail.abyss.AbundanceText;
import com.thelongtravail.abyss.LiquefiedAbundance;

import net.minecraft.network.chat.Component;
import net.minecraft.world.*;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;

import java.util.List;

public final class LiquefiedAbundanceItem extends Item implements ItemDescription {
    public LiquefiedAbundanceItem() {
        super(new Properties().stacksTo(64).rarity(Rarity.RARE));
    }

    @Override
    public Component getName(ItemStack stack) {
        return AspectTheme.ABYSS.name(super.getName(stack));
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.BOW;
    }

    @Override
    public int getUseDuration(ItemStack stack) {
        return LiquefiedAbundance.useTicks();
    }

    @Override
    public InteractionResultHolder<ItemStack> use(
            Level level, Player player, InteractionHand hand) {
        LiquefiedAbundance.begin(player, hand);
        return InteractionResultHolder.consume(player.getItemInHand(hand));
    }

    @Override
    public void releaseUsing(ItemStack stack, Level level, LivingEntity entity, int remaining) {
        if (entity instanceof Player player && !level.isClientSide) {
            InteractionHand hand = player.getUsedItemHand();
            ItemStack result = LiquefiedAbundance.complete(player, stack, false, remaining);
            if (player.getItemInHand(hand) == stack) player.setItemInHand(hand, result);
        }
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        return entity instanceof Player player
                ? LiquefiedAbundance.complete(player, stack, true, 0)
                : stack;
    }

    @Override
    public void appendHoverText(
            ItemStack stack, Level level, List<Component> lines, TooltipFlag flag) {
        ItemDescription.summary(lines);
    }

    @Override
    public void appendDetails(
            ItemStack stack, Level level, List<Component> lines, TooltipFlag flag) {
        lines.addAll(AbundanceText.details());
    }
}
