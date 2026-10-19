package com.thelongtravail.item;

import com.thelongtravail.AspectTheme;
import com.thelongtravail.helper.AccessoryEquipRules;
import com.thelongtravail.underworld.UnderworldRelics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.type.capability.ICurioItem;
import java.util.List;

public final class UnderworldRelicItem extends Item implements ICurioItem {
    public final boolean banner;

    public UnderworldRelicItem(boolean banner) {
        super(new Properties().stacksTo(1).rarity(Rarity.RARE));
        this.banner = banner;
    }

    @Override
    public Component getName(ItemStack s) {
        return AspectTheme.UNDERWORLD.name(super.getName(s));
    }

    @Override
    public boolean canEquip(SlotContext c, ItemStack s) {
        return !c.cosmetic()
                && c.identifier().equals(banner ? "charm" : "bracelet")
                && c.entity() instanceof Player p
                && AccessoryEquipRules.hasDiary(p)
                && AccessoryEquipRules.unique(c, this);
    }

    @Override
    public boolean canEquipFromUse(SlotContext c, ItemStack s) {
        return true;
    }

    @Override
    public boolean canRightClickEquip(ItemStack s) {
        return true;
    }

    @Override
    public void onEquip(SlotContext c, ItemStack previous, ItemStack s) {
        UnderworldRelics.refresh(c.entity());
    }

    @Override
    public void onUnequip(SlotContext c, ItemStack next, ItemStack s) {
        UnderworldRelics.refresh(c.entity());
    }

    @Override
    public void appendHoverText(ItemStack s, Level l, List<Component> out, TooltipFlag flag) {
        ItemDescription.summary(out);
    }
}
