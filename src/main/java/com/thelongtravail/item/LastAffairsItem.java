package com.thelongtravail.item;

import com.thelongtravail.AspectTheme;
import com.thelongtravail.config.ThemeItemConfig;
import com.thelongtravail.registry.ModRegistry;

import net.minecraft.core.GlobalPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;

import java.util.List;

public final class LastAffairsItem extends Item implements ItemDescription {
    public LastAffairsItem() {
        super(new Properties());
    }

    public static boolean spawn(ItemStack stack) {
        return stack.hasTag() && stack.getTag().getBoolean("ReturnToSpawn");
    }

    public static Component message(String suffix) {
        return Component.translatable("message.the_long_travail.last_affairs." + suffix);
    }

    @Override
    public Component getName(ItemStack stack) {
        return AspectTheme.DEEP_VALLEY.name(super.getName(stack));
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.BOW;
    }

    @Override
    public int getUseDuration(ItemStack stack) {
        return Math.max(
                1,
                (int)
                        Math.round(
                                (net.minecraftforge.fml.util.thread.EffectiveSide.get().isClient()
                                                ? ThemeItemConfig.display(
                                                        "last_affairs.holdSeconds")
                                                : ThemeItemConfig.get("last_affairs.holdSeconds"))
                                        * 20));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(
            Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player.isShiftKeyDown()) {
            if (!level.isClientSide) {
                stack.getOrCreateTag().putBoolean("ReturnToSpawn", !spawn(stack));
                player.displayClientMessage(message(spawn(stack) ? "spawn" : "death"), true);
            }
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
        }
        player.startUsingItem(hand);
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        if (!(entity instanceof ServerPlayer player)) return stack;
        GlobalPos destination;
        if (spawn(stack)) {
            var pos = player.getRespawnPosition();
            destination =
                    pos == null
                            ? GlobalPos.of(
                                    Level.OVERWORLD, player.server.overworld().getSharedSpawnPos())
                            : GlobalPos.of(player.getRespawnDimension(), pos);
        } else {
            destination = player.getLastDeathLocation().orElse(null);
            if (destination == null) {
                player.displayClientMessage(message("no_death"), true);
                return stack;
            }
        }
        var target = player.server.getLevel(destination.dimension());
        if (target == null) {
            player.displayClientMessage(message("dimension"), true);
            return stack;
        }
        var pos = destination.pos();
        player.teleportTo(
                target,
                pos.getX() + .5,
                pos.getY(),
                pos.getZ() + .5,
                player.getYRot(),
                player.getXRot());
        if (player.serverLevel() != target
                || player.position().distanceToSqr(pos.getX() + .5, pos.getY(), pos.getZ() + .5)
                        > .01) {
            player.displayClientMessage(message("failed"), true);
            return stack;
        }
        player.fallDistance = 0;
        if (!player.getAbilities().instabuild) stack.shrink(1);
        int ticks = ThemeItemConfig.ticks("last_affairs.revivalSeconds");
        if (ticks > 0) player.addEffect(new MobEffectInstance(ModRegistry.REVIVAL.get(), ticks));
        return stack;
    }

    @Override
    public void appendHoverText(
            ItemStack stack, Level level, List<Component> lines, TooltipFlag flag) {
        ItemDescription.summary(stack, lines);
    }

    @Override
    public void appendDetails(
            ItemStack stack, Level level, List<Component> lines, TooltipFlag flag) {
        ThemeItemText.details(stack, lines);
    }
}
