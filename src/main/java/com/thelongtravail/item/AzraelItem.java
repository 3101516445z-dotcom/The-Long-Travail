package com.thelongtravail.item;

import com.thelongtravail.AspectTheme;
import com.thelongtravail.helper.*;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.type.capability.ICurioItem;
import java.util.List;

public final class AzraelItem extends Item implements ICurioItem {
    public AzraelItem() { super(new Properties().stacksTo(1).rarity(Rarity.RARE)); }
    @Override public Component getName(ItemStack stack) { return AspectTheme.DEEP_VALLEY.name(super.getName(stack)); }
    @Override public boolean canEquip(SlotContext context, ItemStack stack) {
        return !context.cosmetic() && context.identifier().equals("head") && context.entity() instanceof Player player
                && !TravailCurios.stack(player).isEmpty() && DependentAccessories.unique(context, this);
    }
    @Override public boolean canEquipFromUse(SlotContext context, ItemStack stack) { return false; }
    @Override public boolean canRightClickEquip(ItemStack stack) { return false; }
    @Override public void onUnequip(SlotContext context, ItemStack next, ItemStack stack) {
        if (!next.is(this) && context.entity() instanceof net.minecraft.server.level.ServerPlayer player)
            com.thelongtravail.valley.AzraelState.forget(player);
    }
    @Override public void appendHoverText(ItemStack stack, Level level, List<Component> lines, TooltipFlag flag) {
        lines.add(Component.translatable("tooltip.the_long_travail.azrael.locked").withStyle(net.minecraft.ChatFormatting.DARK_RED));
    }
}
