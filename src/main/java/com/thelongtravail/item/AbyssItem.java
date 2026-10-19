package com.thelongtravail.item;

import com.thelongtravail.AspectTheme;
import com.thelongtravail.abyss.AbyssItemText;
import com.thelongtravail.helper.AccessoryEquipRules;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.type.capability.ICurioItem;
import java.util.List;

public class AbyssItem extends Item implements ItemDescription {
    private final boolean accessory;

    public AbyssItem(boolean accessory) {
        super(new Properties().stacksTo(accessory ? 1 : 64).rarity(Rarity.RARE));
        this.accessory = accessory;
    }

    @Override
    public Component getName(ItemStack stack) {
        return AspectTheme.ABYSS.name(super.getName(stack));
    }

    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> lines, TooltipFlag flag) {
        ItemDescription.summary(lines);
    }

    @Override
    public void appendDetails(ItemStack stack, Level level, List<Component> lines, TooltipFlag flag) {
        lines.addAll(AbyssItemText.details(accessory));
    }

    public static final class Lafite extends AbyssItem implements ICurioItem {
        public Lafite() {
            super(true);
        }

        @Override
        public boolean canEquip(SlotContext c, ItemStack stack) {
            return !c.cosmetic() && c.identifier().equals("hands") && AccessoryEquipRules.unique(c, this);
        }

        @Override
        public boolean canEquipFromUse(SlotContext c, ItemStack stack) {
            return true;
        }

        @Override
        public boolean canRightClickEquip(ItemStack stack) {
            return true;
        }
    }
}
