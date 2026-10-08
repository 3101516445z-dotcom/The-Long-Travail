package com.thelongtravail.item;
import com.thelongtravail.config.RainConfig;
import com.thelongtravail.abyss.RainTooltips;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import net.minecraft.network.chat.Component;
import top.theillusivec4.curios.api.type.capability.ICurioItem;
import top.theillusivec4.curios.api.SlotContext;
import java.util.List;

public final class TokaidoItem extends Item implements ICurioItem {
    public TokaidoItem() { super(new Properties().stacksTo(1).rarity(Rarity.RARE)); }
    @Override public Component getName(ItemStack stack) {
        return com.thelongtravail.AspectTheme.ABYSS.name(super.getName(stack));
    }
    @Override public boolean canEquip(SlotContext context, ItemStack stack) {
        var slots = context.entity().level().isClientSide && com.thelongtravail.abyss.RainState.clientSlots != null
                ? com.thelongtravail.abyss.RainState.clientSlots : RainConfig.SLOTS.get();
        return slots.contains(context.identifier()) && com.thelongtravail.helper.DependentAccessories.unique(context, this);
    }
    @Override public void appendHoverText(ItemStack stack, Level level, List<Component> lines, TooltipFlag flag) { RainTooltips.tokaido(lines); }
}
