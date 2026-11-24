package com.thelongtravail.item;

import com.thelongtravail.AspectTheme;
import com.thelongtravail.api.CarryingApi;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import com.thelongtravail.equipment.AccessoryEquipRules;
import com.thelongtravail.boundless.*;

import net.minecraft.network.chat.Component;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;

import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.type.capability.ICurioItem;

import java.util.*;

public class BoundlessRelicItem extends Item implements ItemDescription {
    public static final class Prince extends BoundlessRelicItem {
        public Prince() {
            super(true);
        }

        @Override
        public void inventoryTick(
                ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
            if (entity instanceof ServerPlayer player)
                CarryingApi.inventoryTick(player, this, slot);
        }
    }

    public static final class Lorentz extends BoundlessRelicItem implements ICurioItem {
        public Lorentz() {
            super(false);
        }

        @Override
        public boolean canEquip(SlotContext c, ItemStack s) {
            return AccessoryEquipRules.canEquipUnique(c, this, "bracelet");
        }

        @Override
        public boolean canEquipFromUse(SlotContext c, ItemStack s) {
            return !c.entity().isShiftKeyDown();
        }

        @Override
        public boolean canRightClickEquip(ItemStack s) {
            return true;
        }
    }

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
        ItemDescription.summary(s, out);
    }

    @Override
    public void appendDetails(ItemStack s, Level l, List<Component> out, TooltipFlag f) {
        out.addAll(BoundlessRelicText.details(prince));
    }
}
