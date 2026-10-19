package com.thelongtravail.item;

import com.thelongtravail.AspectTheme;
import com.thelongtravail.boundless.*;
import com.thelongtravail.api.CarryingApi;
import com.thelongtravail.helper.*;
import com.thelongtravail.network.TooltipConfigSync;
import com.thelongtravail.config.BoundlessItemsConfig;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.type.capability.ICurioItem;
import java.util.*;

public final class BoundlessRelicItem extends Item implements ItemDescription, ICurioItem {
    public final boolean prince;

    public BoundlessRelicItem(boolean prince) {
        super(new Properties().stacksTo(1).rarity(Rarity.RARE));
        this.prince = prince;
    }

    @Override
    public Component getName(ItemStack s) {
        return AspectTheme.BOUNDLESS.name(super.getName(s));
    }

    @Override
    public void inventoryTick(ItemStack s, Level l, Entity e, int slot, boolean selected) {
        if (prince && e instanceof ServerPlayer p) CarryingApi.inventoryTick(p, this, slot);
    }

    @Override
    public boolean canEquip(SlotContext c, ItemStack s) {
        return !prince
                && !c.cosmetic()
                && c.identifier().equals("bracelet")
                && c.entity() instanceof Player p
                && AccessoryEquipRules.hasDiary(p)
                && DependentAccessories.unique(c, this);
    }

    @Override
    public boolean canEquipFromUse(SlotContext c, ItemStack s) {
        return !c.entity().isShiftKeyDown();
    }

    @Override
    public boolean canRightClickEquip(ItemStack s) {
        return true;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level l, Player p, InteractionHand hand) {
        ItemStack s = p.getItemInHand(hand);
        if (!prince && p.isShiftKeyDown()) {
            if (!l.isClientSide) {
                boolean disabled = !s.getOrCreateTag().getBoolean("LorentzGravityDisabled");
                s.getOrCreateTag().putBoolean("LorentzGravityDisabled", disabled);
                p.displayClientMessage(BoundlessRelicText.toggleMessage(!disabled), true);
            }
            return InteractionResultHolder.sidedSuccess(s, l.isClientSide);
        }
        return super.use(l, p, hand);
    }

    @Override
    public void appendHoverText(ItemStack s, Level l, List<Component> out, TooltipFlag f) {
        ItemDescription.summary(out);
    }

    @Override
    public void appendDetails(ItemStack s, Level l, List<Component> out, TooltipFlag f) {
        out.addAll(BoundlessRelicText.details(prince));
    }
}
