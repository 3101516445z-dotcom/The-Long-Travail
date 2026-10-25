package com.thelongtravail.item;

import com.thelongtravail.menu.ColoringMenu;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraftforge.network.NetworkHooks;

import org.jetbrains.annotations.Nullable;

import java.util.List;

public final class ColoringItem extends Item {
    public ColoringItem() {
        super(new Properties().stacksTo(1));
    }

    @Override
    public Component getName(ItemStack stack) {
        return super.getName(stack)
                .copy()
                .withStyle(s -> s.withColor(0xC5AA77).withInsertion("the_long_travail:name"));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(
            Level level, Player player, InteractionHand hand) {
        if (player instanceof ServerPlayer server)
            NetworkHooks.openScreen(
                    server,
                    new SimpleMenuProvider(
                            (id, inventory, viewer) -> new ColoringMenu(id, inventory),
                            Component.translatable("item.the_long_travail.coloring")));
        return InteractionResultHolder.sidedSuccess(player.getItemInHand(hand), level.isClientSide);
    }

    @Override
    public void appendHoverText(
            ItemStack stack, @Nullable Level level, List<Component> lines, TooltipFlag flag) {
        for (int i = 0; i < 2; i++)
            lines.add(
                    Component.translatable("tooltip.the_long_travail.coloring." + i)
                            .withStyle(s -> s.withColor(0xBC995E)));
        lines.add(Component.translatable(ItemDescription.CATALOG));
    }
}
