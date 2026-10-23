package com.thelongtravail.item;

import com.thelongtravail.AspectTheme;
import com.thelongtravail.flourishing.EverlastingFlower;
import com.thelongtravail.helper.AccessoryEquipRules;
import net.minecraft.network.chat.Component;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.type.capability.ICurioItem;
import java.util.List;

public final class EverlastingFlowerItem extends Item implements ICurioItem {
    public EverlastingFlowerItem() {
        super(new Properties().stacksTo(1).rarity(Rarity.RARE));
    }

    @Override
    public Component getName(ItemStack stack) {
        return AspectTheme.FLOURISHING.name(super.getName(stack));
    }

    @Override
    public boolean canEquip(SlotContext c, ItemStack stack) {
        return !c.cosmetic() && AccessoryEquipRules.unique(c, this);
    }

    @Override
    public boolean canEquipFromUse(SlotContext c, ItemStack stack) {
        return !c.entity().isShiftKeyDown();
    }

    @Override
    public boolean canRightClickEquip(ItemStack stack) {
        return true;
    }

    @Override
    public void onUnequip(SlotContext c, ItemStack next, ItemStack stack) {
        if (!c.cosmetic() && !next.is(this)) EverlastingFlower.unequipped(c.entity());
        EverlastingFlower.update(c.entity());
    }

    @Override
    public InteractionResultHolder<ItemStack> use(
            Level level, Player player, InteractionHand hand) {
        var stack = player.getItemInHand(hand);
        if (!player.isShiftKeyDown()) return InteractionResultHolder.pass(stack);
        if (!level.isClientSide) {
            boolean enabled = !EverlastingFlower.growthEnabled(stack);
            stack.getOrCreateTag().putBoolean("TravailGrowth", enabled);
            player.displayClientMessage(
                    Component.translatable(
                            "message.the_long_travail.growth."
                                    + (enabled ? "enabled" : "disabled")),
                    true);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override
    public void appendHoverText(
            ItemStack stack, Level level, List<Component> lines, TooltipFlag flag) {
        ItemDescription.summary(lines);
    }
}
