package com.thelongtravail.abyss;

import com.thelongtravail.config.AbyssItemsConfig;
import com.thelongtravail.network.TooltipConfigSync;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.registries.ForgeRegistries;
import java.util.*;

public final class AbundanceText {
    private static Component text(String key, Object... args) {
        return Component.translatable("tooltip.the_long_travail.abundance." + key, args);
    }

    public static List<Component> summary() {
        return List.of(
                text("summary").copy().withStyle(style -> style.withColor(RainTooltips.BODY_COLOR)),
                text("summary_switch")
                        .copy()
                        .withStyle(style -> style.withColor(RainTooltips.BODY_COLOR)));
    }

    public static List<Component> details() {
        List<Component> lines = new ArrayList<>();
        lines.add(
                text(
                        "use",
                        TooltipConfigSync.decimal(
                                "abundance.hold", AbyssItemsConfig.ABUNDANCE_HOLD.get())));
        lines.add(text("switch"));
        lines.add(Component.empty());
        lines.add(
                text(
                        "pool",
                        TooltipConfigSync.decimal(
                                "abundance.fallback", AbyssItemsConfig.ABUNDANCE_DURATION.get())));
        lines.add(text("lingering"));
        lines.add(Component.empty());
        lines.add(text("cost"));
        var allowed =
                TooltipConfigSync.pool(
                        "abundance.whitelist", AbyssItemsConfig.ABUNDANCE_WHITELIST.get());
        if (!allowed.isEmpty()) lines.add(text("whitelist", names(allowed)));
        lines.add(
                text(
                        "effects",
                        names(
                                TooltipConfigSync.pool(
                                        "abundance.blacklist",
                                        AbyssItemsConfig.ABUNDANCE_BLACKLIST.get()))));
        return lines;
    }

    private static Component names(List<? extends String> ids) {
        var names = Component.empty();
        for (String id : ids) {
            if (!names.getSiblings().isEmpty())
                names.append(
                        Component.translatable("tooltip.the_long_travail.abyss_items.separator"));
            ResourceLocation key = ResourceLocation.tryParse(id);
            names.append(
                    (key != null && ForgeRegistries.MOB_EFFECTS.containsKey(key)
                                    ? ForgeRegistries.MOB_EFFECTS
                                            .getValue(key)
                                            .getDisplayName()
                                            .copy()
                                    : Component.literal(id))
                            .withStyle(
                                    style ->
                                            style.withColor(RainTooltips.ACCENT_COLOR)
                                                    .withInsertion(
                                                            "the_long_travail:configured_effect")));
        }
        if (ids.isEmpty())
            names.append(
                    Component.translatable(
                                    "tooltip.the_long_travail.abyss_items.fluid.blacklist_unrestricted")
                            .withStyle(
                                    style ->
                                            style.withColor(RainTooltips.ACCENT_COLOR)
                                                    .withInsertion(
                                                            "the_long_travail:configured_effect")));
        return names;
    }

    private AbundanceText() {}
}
