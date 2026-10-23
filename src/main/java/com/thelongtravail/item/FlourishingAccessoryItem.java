package com.thelongtravail.item;

import com.thelongtravail.AspectTheme;
import com.thelongtravail.config.FlourishingItemsConfig;
import com.thelongtravail.flourishing.*;
import net.minecraft.world.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import net.minecraft.network.chat.Component;

import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.type.capability.ICurioItem;
import java.util.*;

public final class FlourishingAccessoryItem extends Item implements ICurioItem, ItemDescription {
    private final boolean spring;

    public FlourishingAccessoryItem(boolean spring) {
        super(new Properties().stacksTo(1).rarity(Rarity.RARE));
        this.spring = spring;
    }

    @Override
    public Component getName(ItemStack stack) {
        return AspectTheme.FLOURISHING.name(super.getName(stack));
    }

    @Override
    public void onEquip(SlotContext context, ItemStack previous, ItemStack stack) {
        if (!spring && context.entity() instanceof net.minecraft.server.level.ServerPlayer p)
            Affection.reset(p);
    }

    @Override
    public void onUnequip(SlotContext context, ItemStack next, ItemStack stack) {
        if (!spring && context.entity() instanceof net.minecraft.server.level.ServerPlayer p)
            Affection.reset(p);
    }

    @Override
    public boolean canEquipFromUse(SlotContext context, ItemStack stack) {
        return spring || !context.entity().isShiftKeyDown();
    }

    @Override
    public boolean canRightClickEquip(ItemStack stack) {
        return true;
    }

    @Override
    public void inventoryTick(
            ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
        if (!spring && !level.isClientSide && entity.tickCount % 20 == 0)
            AffectionDeaths.get(level.getServer()).clean(stack);
    }

    @Override
    public boolean canEquip(SlotContext context, ItemStack stack) {
        if (!(context.entity() instanceof Player p)) return false;
        var slots =
                spring
                        ? FlourishingItemsConfig.SPRING_SLOTS.get()
                        : FlourishingItemsConfig.AFFECTION_SLOTS.get();
        if (p.level().isClientSide)
            slots =
                    com.thelongtravail.network.TooltipConfigSync.pool(
                            spring ? "floral.springSlots" : "floral.affectionSlots", slots);
        if (!slots.contains(context.identifier()) || (!spring && !Affection.ownerAllowed(stack, p)))
            return false;
        return com.thelongtravail.helper.AccessoryEquipRules.unique(context, this, true);
    }

    @Override
    public InteractionResult interactLivingEntity(
            ItemStack stack, Player p, LivingEntity target, InteractionHand hand) {
        if (spring) return InteractionResult.PASS;
        return Affection.bind(stack, p, target);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player p, InteractionHand hand) {
        ItemStack stack = p.getItemInHand(hand);
        if (!spring && p.isShiftKeyDown()) {
            if (!level.isClientSide) {
                Affection.clear(stack);
                p.displayClientMessage(
                        com.thelongtravail.helper.ActionMessages.text(
                                "message.the_long_travail.affection.cleared"),
                        true);
            }
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
        }
        return InteractionResultHolder.pass(stack);
    }

    @Override
    public void appendHoverText(
            ItemStack stack, Level level, List<Component> lines, TooltipFlag flag) {
        ItemDescription.summary(lines);
    }

    @Override
    public void appendDetails(
            ItemStack stack, Level level, List<Component> lines, TooltipFlag flag) {
        lines.add(Component.empty());
        addLines(lines, spring ? "spring_corner.rules" : "affection.rules", spring ? 4 : 2);
        if (spring) {
            lines.add(Component.empty());
            for (Flower flower : Flower.values()) {
                Component name =
                        FloralText.gold(flower.item().getDescription())
                                .copy()
                                .withStyle(
                                        style ->
                                                style.withInsertion(
                                                        "the_long_travail:flower_name"));
                var row =
                        Component.translatable(
                                        "tooltip.the_long_travail.flower.catalog_entry",
                                        name,
                                        FlowerDescriptions.describe(flower))
                                .withStyle(style -> style.withColor(FloralText.BODY));
                if (!FlourishingItemsConfig.shownEnabled(flower))
                    row.append(
                            Component.translatable(
                                            "tooltip.the_long_travail.flower.catalog_disabled")
                                    .withStyle(style -> style.withColor(0xC7A474)));
                lines.add(row);
            }
        } else {
            lines.add(Component.empty());
            addLines(lines, "affection.companion", 2);
            affectionFormula(lines, "companion.2", "servantDamageBonus");
            affectionFormula(lines, "companion.3", "servantDamageReduction");
            lines.add(Component.empty());
            addLines(lines, "affection.owner", 2);
            affectionFormula(lines, "owner.2", "playerDamageBonus");
            affectionFormula(lines, "owner.3", "playerDamageReduction");
            affectionFormula(lines, "owner.4", "playerSpeedBonus");
            lines.add(Component.empty());
            lines.add(FloralText.text("tooltip.the_long_travail.affection.calculation"));
            lines.add(Component.empty());
            lines.add(
                    FloralText.text(
                            "tooltip.the_long_travail.affection.range",
                            FlowerDescriptions.number(
                                    FlourishingItemsConfig.shown("affection.range"))));
        }
    }

    private static void affectionFormula(List<Component> lines, String key, String config) {
        lines.add(
                FloralText.text(
                        "tooltip.the_long_travail.affection." + key,
                        FlowerDescriptions.percent(
                                FlourishingItemsConfig.shown("affection." + config))));
    }

    private static void addLines(List<Component> lines, String key, int count) {
        for (int i = 0; i < count; i++) {
            lines.add(FloralText.text("tooltip.the_long_travail." + key + "." + i));
        }
    }
}
