package com.thelongtravail.item;

import com.thelongtravail.AspectTheme;
import com.thelongtravail.helper.DependentAccessories;
import com.thelongtravail.valley.SwordLanternConfig;
import com.thelongtravail.network.TooltipConfigSync;
import com.thelongtravail.abyss.RainTooltips;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.type.capability.ICurioItem;
import java.util.List;

public final class SwordLanternItem extends Item implements ICurioItem, ItemDescription {
    public SwordLanternItem() {
        super(new Properties().stacksTo(1).rarity(Rarity.RARE));
    }

    @Override
    public Component getName(ItemStack s) {
        return AspectTheme.DEEP_VALLEY.name(super.getName(s));
    }

    @Override
    public boolean canEquip(SlotContext c, ItemStack s) {
        return !c.cosmetic()
                && c.identifier().equals("belt")
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
    public net.minecraft.world.InteractionResultHolder<ItemStack> use(
            Level level,
            net.minecraft.world.entity.player.Player player,
            net.minecraft.world.InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!player.isShiftKeyDown())
            return net.minecraft.world.InteractionResultHolder.pass(stack);
        if (!level.isClientSide) {
            boolean enabled = com.thelongtravail.valley.LanternSources.toggle(stack);
            player.displayClientMessage(
                    com.thelongtravail.helper.ActionMessages.text(
                            "message.the_long_travail.sword_lantern.light_"
                                    + (enabled ? "on" : "off")),
                    true);
        }
        return net.minecraft.world.InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    private static String num(String key, double fallback, double scale) {
        return java.math.BigDecimal.valueOf(
                        TooltipConfigSync.decimal("swordLantern." + key, fallback) * scale)
                .stripTrailingZeros()
                .toPlainString();
    }

    private static void line(List<Component> out, String key, Object... args) {
        for (String text :
                Component.translatable("tooltip.the_long_travail.sword_lantern." + key, args)
                        .getString()
                        .split("\\n"))
            out.add(
                    RainTooltips.coloredLine(
                            Component.literal(text)
                                    .withStyle(s -> s.withColor(RainTooltips.BODY_COLOR))));
    }

    @Override
    public void appendHoverText(ItemStack s, Level l, List<Component> out, TooltipFlag f) {
        ItemDescription.summary(out);
    }

    @Override
    public void appendDetails(ItemStack s, Level l, List<Component> out, TooltipFlag f) {
        out.add(Component.empty());
        line(out, "light");
        line(out, "light_toggle");
        out.add(Component.empty());
        line(out, "immunity");
        line(out, "bonus");
        line(out, "radius", num("radius", SwordLanternConfig.RADIUS.get(), 1));
        line(out, "alone", num("aloneBonus", SwordLanternConfig.ALONE_BONUS.get(), 100));
        line(
                out,
                "crowd",
                num("crowdMinimum", SwordLanternConfig.CROWD_MINIMUM.get(), 1),
                num("crowdBonus", SwordLanternConfig.CROWD_BONUS.get(), 100));
        line(out, "undead", num("undeadBonus", SwordLanternConfig.UNDEAD_BONUS.get(), 100));
        line(
                out,
                "duration",
                num("duration", SwordLanternConfig.DURATION.get(), 1D / 60),
                num("prolongedBonus", SwordLanternConfig.PROLONGED_BONUS.get(), 100));
        line(
                out,
                "health",
                num("health", SwordLanternConfig.HEALTH.get(), 100),
                num("lowHealthBonus", SwordLanternConfig.LOW_HEALTH_BONUS.get(), 100));
    }
}
