package com.thelongtravail.item;
import com.thelongtravail.abyss.RainState;
import com.thelongtravail.abyss.RainTooltips;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import net.minecraft.network.chat.Component;
import java.util.List;

public final class EkiItem extends Item {
    public EkiItem() { super(new Properties().stacksTo(1).rarity(Rarity.RARE)); }
    @Override public Component getName(ItemStack stack) {
        return com.thelongtravail.AspectTheme.ABYSS.name(super.getName(stack));
    }
    @Override public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
        if (entity instanceof ServerPlayer p) RainState.inventoryTick(p, slot);
    }
    @Override public void appendHoverText(ItemStack stack, Level level, List<Component> lines, TooltipFlag flag) { RainTooltips.eki(lines); }
}
