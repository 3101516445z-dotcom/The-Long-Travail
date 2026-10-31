package com.thelongtravail.item;

import com.thelongtravail.AspectTheme;
import com.thelongtravail.abyss.RainTooltips;
import com.thelongtravail.config.FarReachItemsConfig;
import com.thelongtravail.helper.DependentAccessories;
import com.thelongtravail.network.TooltipConfigSync;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;

import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.type.capability.ICurioItem;

import java.math.BigDecimal;
import java.util.List;

public final class FarReachAccessoryItem extends Item implements ICurioItem {
    private final boolean gold;

    public FarReachAccessoryItem(boolean gold) {
        super(new Properties().stacksTo(1).rarity(Rarity.RARE));
        this.gold = gold;
    }

    @Override
    public void inventoryTick(
            ItemStack stack,
            Level level,
            net.minecraft.world.entity.Entity entity,
            int slot,
            boolean selected) {
        if (gold && entity instanceof net.minecraft.server.level.ServerPlayer p)
            com.thelongtravail.api.CarryingApi.inventoryTick(p, this, slot);
    }

    @Override
    public Component getName(ItemStack stack) {
        return AspectTheme.FAR_REACH.name(super.getName(stack));
    }

    @Override
    public boolean canEquip(SlotContext c, ItemStack s) {
        return !gold
                && !c.cosmetic()
                && c.identifier().equals("back")
                && DependentAccessories.unique(c, this);
    }

    @Override
    public boolean canEquipFromUse(SlotContext c, ItemStack s) {
        return !gold && !c.entity().isShiftKeyDown();
    }

    @Override
    public boolean canRightClickEquip(ItemStack s) {
        return !gold;
    }

    @Override
    public net.minecraft.world.InteractionResultHolder<ItemStack> use(
            Level level,
            net.minecraft.world.entity.player.Player player,
            net.minecraft.world.InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (gold || !player.isShiftKeyDown())
            return net.minecraft.world.InteractionResultHolder.pass(stack);
        if (!level.isClientSide) {
            boolean enabled = com.thelongtravail.farreach.IcarusFlight.toggle(stack);
            player.displayClientMessage(
                    com.thelongtravail.helper.ActionMessages.text(
                            "message.the_long_travail.icarus.flight_" + (enabled ? "on" : "off")),
                    true);
        }
        return net.minecraft.world.InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    private static String n(String k, double scale) {
        return BigDecimal.valueOf(
                        TooltipConfigSync.decimal("farItem." + k, FarReachItemsConfig.get(k))
                                * scale)
                .stripTrailingZeros()
                .toPlainString();
    }

    private static void line(List<Component> out, String key, Object... args) {
        out.add(
                RainTooltips.coloredLine(
                        Component.translatable("tooltip.the_long_travail." + key, args)
                                .withStyle(
                                        s ->
                                                s.withColor(
                                                        key.endsWith("heading")
                                                                ? RainTooltips.TITLE_COLOR
                                                                : RainTooltips.BODY_COLOR))));
    }

    @Override
    public void appendHoverText(
            ItemStack stack, Level level, List<Component> out, TooltipFlag flag) {
        ItemDescription.summary(out);
    }

    public void appendDetails(
            List<Component> out, net.minecraft.world.entity.player.Player viewer, ItemStack stack) {
        appendDetails(out, viewer, stack, false);
    }

    public void appendDetails(
            List<Component> out,
            net.minecraft.world.entity.player.Player viewer,
            ItemStack stack,
            boolean complete) {
        if (gold) {
            {
                line(out, "golden_age.dig_unlocked");
                line(out, "golden_age.dig_result");
            }
        } else {
            line(out, "icarus.flight");
            line(out, "icarus.flight_toggle");
            line(out, "icarus.immunity");
            line(
                    out,
                    "icarus.ignite",
                    TooltipConfigSync.integer(
                            "farItem.icarus.igniteSeconds",
                            FarReachItemsConfig.IGNITE_SECONDS.get()));
        }
        out.add(Component.empty());

        if (gold) {
            line(out, "golden_age.malice_heading");
            line(out, "golden_age.convert");
            line(out, "golden_age.malice");
            out.add(Component.empty());
            line(out, "golden_age.witness_heading");
            line(out, "golden_age.convert");
            line(out, "golden_age.witness");
        } else {
            line(out, "icarus.malice_heading");
            line(out, "icarus.malice", n("icarus.malicePeak", 100));
            line(
                    out,
                    "icarus.sunny",
                    BigDecimal.valueOf(
                                    TooltipConfigSync.decimal(
                                            "farItem.icarus.malicePeak",
                                            FarReachItemsConfig.get("icarus.malicePeak")))
                            .add(
                                    BigDecimal.valueOf(
                                            TooltipConfigSync.decimal(
                                                    "farItem.icarus.sunnyExtraPeak",
                                                    FarReachItemsConfig.get(
                                                            "icarus.sunnyExtraPeak"))))
                            .multiply(BigDecimal.valueOf(100))
                            .stripTrailingZeros()
                            .toPlainString());
            line(out, "icarus.dimensions");
            out.add(Component.empty());
            line(out, "icarus.witness_heading");
            line(out, "icarus.magic");
            line(out, "icarus.witness", n("icarus.witnessBonus", 100));
        }
    }
}
