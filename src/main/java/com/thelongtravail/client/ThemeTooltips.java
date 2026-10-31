package com.thelongtravail.client;

import com.thelongtravail.abyss.RainTooltips;
import com.thelongtravail.client.catalog.*;
import com.thelongtravail.compat.maid.client.MaidAccessoryTooltips;
import com.thelongtravail.farreach.IcarusFlight;
import com.thelongtravail.flourishing.*;
import com.thelongtravail.helper.AccessoryEquipRules;
import com.thelongtravail.item.ItemDescription;
import com.thelongtravail.valley.LanternSources;

import net.minecraft.client.KeyMapping;
import net.minecraft.network.chat.*;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.*;
import java.util.function.Predicate;

public final class ThemeTooltips {
    private static final String PREFIX = "tooltip.the_long_travail.summary.";
    private static final Set<String> FLOURISHING_SUMMARIES =
            Set.of("spring_corner", "affection", "mistletoe", "everlasting_flower", "immortal_bud");
    private static final Set<String> BOUNDLESS_SUMMARIES =
            Set.of(
                    "free_and_easy_wandering",
                    "star_voice",
                    "little_prince",
                    "lorentz_fantasy",
                    "ancient_strange_tales");
    private static final Set<String> UNDERWORLD_SUMMARIES =
            Set.of(
                    "a_thousand_years_later",
                    "book_of_the_dead",
                    "demon_king_of_the_sixth_heaven",
                    "myriad_souls_banner",
                    "kagotsurube");
    private static final Set<String> VALLEY_SUMMARIES =
            Set.of(
                    "azrael",
                    "sword_and_lantern",
                    "phosphophyllite",
                    "underground_velvet",
                    "high_frequency_tremor");
    private static final Set<String> FAR_REACH_SUMMARIES =
            Set.of("golden_age", "icarus", "don_quixote", "tower_of_babel", "sun_chaser");
    private static final Set<String> ABYSS_SUMMARIES =
            Set.of("eki", "tokaido", "perfect_fluid", "midnight_lafite", "liquefied_abundance");
    private static final Set<String> NON_STACKING_CARRIED_ITEMS =
            Set.of("eki", "tower_of_babel", "little_prince", "golden_age", "sun_chaser");

    private static boolean utility(String id) {
        return Set.of("homecoming", "renewal", "wayguide", "coloring").contains(id)
                || id.endsWith("_stone")
                || id.endsWith("_essence");
    }

    private static boolean key(Component c, Predicate<String> test) {
        return c.getContents() instanceof TranslatableContents t && test.test(t.getKey())
                || c.getSiblings().stream().anyMatch(s -> key(s, test));
    }

    private static boolean blank(Component c) {
        return c.getString().isBlank();
    }

    private static List<Component> take(List<Component> lines, Predicate<Component> predicate) {
        List<Component> found = new ArrayList<>();
        for (int i = 0; i < lines.size(); ) {
            if (!predicate.test(lines.get(i))) {
                i++;
                continue;
            }
            found.add(lines.remove(i));
            if (i > 0 && blank(lines.get(i - 1))) lines.remove(--i);
        }
        return found;
    }

    public static void finish(ItemStack stack, Player viewer, List<Component> lines) {
        if (ImmortalBud.marked(stack)) {
            lines.add(
                    Math.min(1, lines.size()),
                    com.thelongtravail.AspectTheme.FLOURISHING.name(
                            Component.translatable("tooltip.the_long_travail.immortal_bud.mark")));
        }
        lines.replaceAll(
                line -> key(line, ItemDescription.CATALOG::equals) ? catalogPrompt() : line);
        if (lines.stream().noneMatch(c -> key(c, ItemDescription.SUMMARY::equals))) return;
        var entry = CatalogEntries.forStack(stack).orElse(null);
        if (entry == null) return;
        var maid = take(lines, MaidAccessoryTooltips::isMaidTooltip);
        Set<Component> attributeLines = Collections.newSetFromMap(new IdentityHashMap<>());
        boolean curiosGroup = false;
        for (Component line : lines) {
            if (key(line, k -> k.startsWith("curios.modifiers."))) {
                curiosGroup = true;
                attributeLines.add(line);
            } else if (curiosGroup && key(line, k -> k.startsWith("attribute.modifier."))) {
                attributeLines.add(line);
            } else if (!blank(line)) curiosGroup = false;
        }
        var attributes = take(lines, attributeLines::contains);
        int at = 0;
        while (at < lines.size() && !key(lines.get(at), ItemDescription.SUMMARY::equals)) at++;
        lines.remove(at);
        while (at > 0 && blank(lines.get(at - 1))) lines.remove(--at);
        var block = new ArrayList<Component>();
        String flavorKey = "tooltip.the_long_travail.flavor." + entry.id();
        if (net.minecraft.locale.Language.getInstance().has(flavorKey)) {
            for (String prose : Component.translatable(flavorKey).getString().split("\\n")) {
                var flavor =
                        Component.literal(prose)
                                .withStyle(
                                        style ->
                                                style.withColor(0xC4AD75)
                                                        .withBold(false)
                                                        .withItalic(false));
                int width =
                        Math.max(
                                1, net.minecraft.client.Minecraft.getInstance().font.width(flavor));
                block.add(
                        flavor.withStyle(
                                style ->
                                        style.withInsertion(
                                                "the_long_travail:prose:flavor:" + width)));
            }
        }
        block.addAll(summary(entry.id(), stack, viewer));
        if (entry.id().equals("liquefied_abundance"))
            block.add(
                    text(
                            "liquefied_abundance.state",
                            com.thelongtravail.abyss.LiquefiedAbundance.modeName(
                                    AbundanceUseClient.selectedMode())));
        block.addAll(attributes);
        if (NON_STACKING_CARRIED_ITEMS.contains(entry.id())) block.add(carryingLimitHint());
        block.add(
                (FLOURISHING_SUMMARIES.contains(entry.id()) || utility(entry.id()))
                        ? catalogPrompt(
                                entry.id().equals("spring_corner")
                                        ? "spring_corner.open"
                                        : "flourishing.open")
                        : BOUNDLESS_SUMMARIES.contains(entry.id())
                                        || UNDERWORLD_SUMMARIES.contains(entry.id())
                                        || VALLEY_SUMMARIES.contains(entry.id())
                                        || FAR_REACH_SUMMARIES.contains(entry.id())
                                        || ABYSS_SUMMARIES.contains(entry.id())
                                        || NON_STACKING_CARRIED_ITEMS.contains(entry.id())
                                ? catalogPrompt("abyss.open")
                                : catalogPrompt());
        if (!maid.isEmpty()) {
            block.add(Component.empty());
            block.addAll(maid);
        }
        lines.addAll(at, block);
    }

    public static Component catalogPrompt() {
        return catalogPrompt("open");
    }

    private static Component carryingLimitHint() {
        return Component.translatable(PREFIX + "carrying_non_stacking")
                .withStyle(style -> style.withColor(0x555555).withBold(false).withItalic(false));
    }

    private static Component catalogPrompt(String suffix) {
        return Component.translatable(
                        PREFIX + suffix,
                        keyName(CatalogSession.OPEN)
                                .copy()
                                .withStyle(s -> s.withColor(RainTooltips.ACCENT_COLOR)))
                .withStyle(s -> s.withColor(0x555555));
    }

    public static Component keyName(KeyMapping binding) {
        return binding.isUnbound()
                ? Component.translatable(PREFIX + "unbound")
                : TravailClientEvents.holdKeyLabel(binding);
    }

    public static Component text(String suffix, Object... args) {
        String value = Component.translatable(PREFIX + suffix, args).getString();
        var result = Component.empty();
        var run = new StringBuilder();
        Style[] previous = {null};
        net.minecraft.util.StringDecomposer.iterateFormatted(
                value,
                Style.EMPTY.withColor(suffix.equals("open") ? 0x555555 : RainTooltips.BODY_COLOR),
                (i, style, point) -> {
                    if (style.getColor() != null) {
                        int color = style.getColor().getValue();
                        if (color == 0x55FF55) style = style.withColor(RainTooltips.ACCENT_COLOR);
                        else if (color == 0xFFAA00)
                            style = style.withColor(RainTooltips.TITLE_COLOR);
                        else if (color == 0xFF55FF)
                            style = style.withColor(RainTooltips.STATE_CONDITION_COLOR);
                    }
                    style = style.withBold(false).withObfuscated(false);
                    if (!style.equals(previous[0]) && run.length() > 0) {
                        appendRun(result, run.toString(), previous[0]);
                        run.setLength(0);
                    }
                    previous[0] = style;
                    run.appendCodePoint(point);
                    return true;
                });
        if (run.length() > 0) appendRun(result, run.toString(), previous[0]);
        return result;
    }

    private static void appendRun(MutableComponent target, String text, Style style) {
        String revival =
                Component.translatable("tooltip.the_long_travail.book_of_dead.revival_name")
                        .getString();
        int at = text.indexOf(revival);
        if (at < 0) {
            target.append(Component.literal(text).setStyle(style));
            return;
        }
        target.append(Component.literal(text.substring(0, at)).setStyle(style));
        target.append(
                Component.literal(revival).setStyle(style.withColor(0xDC143C).withBold(true)));
        target.append(Component.literal(text.substring(at + revival.length())).setStyle(style));
    }

    public static List<Component> summary(String id, ItemStack stack, Player viewer) {
        var lines = summaryParts(id, stack, viewer);
        if (!FLOURISHING_SUMMARIES.contains(id)
                && !BOUNDLESS_SUMMARIES.contains(id)
                && !UNDERWORLD_SUMMARIES.contains(id)
                && !VALLEY_SUMMARIES.contains(id)
                && !FAR_REACH_SUMMARIES.contains(id)
                && !ABYSS_SUMMARIES.contains(id)
                && !NON_STACKING_CARRIED_ITEMS.contains(id)
                && !utility(id)) return lines;
        // 将文案中的显式换行拆成独立悬浮行，同时保留各片段的语义染色。
        var result = new ArrayList<Component>();
        for (Component line : lines) {
            MutableComponent[] current = {Component.empty()};
            line.visit(
                    (style, fragment) -> {
                        String[] parts = fragment.split("\\n", -1);
                        for (int i = 0; i < parts.length; i++) {
                            if (i > 0) {
                                result.add(current[0]);
                                current[0] = Component.empty();
                            }
                            if (!parts[i].isEmpty())
                                current[0].append(Component.literal(parts[i]).setStyle(style));
                        }
                        return Optional.empty();
                    },
                    Style.EMPTY);
            result.add(current[0]);
        }
        return result;
    }

    private static List<Component> summaryParts(String id, ItemStack stack, Player viewer) {
        var out = new ArrayList<Component>();
        if (utility(id)) {
            if (id.endsWith("_stone") || id.endsWith("_essence")) {
                String theme = id.substring(0, id.lastIndexOf('_'));
                out.add(
                        text(
                                id.endsWith("_stone") ? "utility.stone" : "utility.essence",
                                Component.translatable("aspect.the_long_travail." + theme)
                                        .getString()));
            } else if (id.equals("wayguide")) {
                out.add(
                        text(
                                "utility.wayguide",
                                com.thelongtravail.network.TooltipConfigSync.integer(
                                        "wayguide.radius",
                                        com.thelongtravail.config.TravailConfig.WAYGUIDE_RADIUS
                                                .get())));
            } else out.add(text("utility." + id));
            return out;
        }
        if (id.equals("immortal_bud")) {
            out.add(text(id + ".0"));
            out.add(text(id + ".1"));
            return out;
        }
        if (viewer != null && !AccessoryEquipRules.hasDiary(viewer)) {
            if (Set.of(
                            "demon_king_of_the_sixth_heaven",
                            "myriad_souls_banner",
                            "phosphophyllite",
                            "azrael",
                            "a_thousand_years_later",
                            "star_voice",
                            "lorentz_fantasy")
                    .contains(id)) out.add(text("requires_diary_equip"));
            else if (id.equals("eki")) out.add(text("eki.requires_diary_effect"));
            else if (id.equals("book_of_the_dead")) out.add(text("requires_diary_effect"));
        }
        switch (id) {
            case "sun_chaser", "kagotsurube" -> out.add(text(id + ".0"));
            case "demon_king_of_the_sixth_heaven", "myriad_souls_banner" -> {
                out.add(text(id + ".0"));
                out.add(text(id + ".1"));
                if (id.equals("myriad_souls_banner")) out.add(text(id + ".2"));
                if (id.equals("myriad_souls_banner"))
                    out.add(
                            text(
                                    id + ".state",
                                    Math.min(
                                            stack.hasTag()
                                                    ? Math.max(
                                                            0,
                                                            stack.getTag()
                                                                    .getCompound(
                                                                            "the_long_travail:soul_banner")
                                                                    .getInt("Souls"))
                                                    : 0,
                                            (int)
                                                    com.thelongtravail.underworld
                                                            .UnderworldRelicText.shown(
                                                            "banner.capacity")),
                                    com.thelongtravail.underworld.UnderworldRelicText.number(
                                            com.thelongtravail.underworld.UnderworldRelicText.shown(
                                                    "banner.capacity"))));
            }
            case "mistletoe" -> {
                for (int i = 0; i < 3; i++) out.add(text(id + "." + i));
            }
            case "everlasting_flower" -> {
                for (int i = 0; i < 3; i++) out.add(text(id + "." + i));
                out.add(
                        text(
                                id + ".state",
                                Component.translatable(
                                        PREFIX
                                                + (EverlastingFlower.growthEnabled(stack)
                                                        ? "enabled"
                                                        : "disabled"))));
            }
            case "perfect_fluid" -> {
                out.add(text("perfect_fluid.0"));
                out.add(text("perfect_fluid.1"));
            }
            case "high_frequency_tremor" -> {
                for (int i = 0; i < 3; i++) out.add(text(id + "." + i));
            }
            case "phosphophyllite", "underground_velvet" -> {
                for (int i = 0; i < (id.equals("underground_velvet") ? 3 : 2); i++)
                    out.add(text(id + "." + i));
            }
            case "ancient_strange_tales" -> {
                out.add(text(id + ".0"));
                out.add(text(id + ".1"));
            }
            case "liquefied_abundance" ->
                    out.addAll(
                            com.thelongtravail.abyss.AbundanceText.summary().stream()
                                    .map(RainTooltips::coloredLine)
                                    .toList());
            case "midnight_lafite" -> out.addAll(com.thelongtravail.abyss.AbyssItemText.summary());
            case "spring_corner" -> {
                out.add(text(id + ".0"));
                out.add(text(id + ".1"));
                var flowers = FlowerData.read(stack);
                if (flowers.isEmpty()) out.add(text("main_flower_missing"));
                else {
                    out.add(text("main_flower", flowers.get(0).item().getDescription()));
                    var secondary = Component.empty();
                    for (int i = 1; i < flowers.size(); i++) {
                        if (i > 1)
                            secondary.append(Component.translatable(PREFIX + "list_separator"));
                        secondary.append(flowers.get(i).item().getDescription());
                    }
                    out.add(
                            text(
                                    "secondary_flower",
                                    flowers.size() == 1
                                            ? Component.translatable(PREFIX + "none")
                                            : secondary));
                }
            }
            case "affection" -> {
                out.add(text(id + ".0"));
                out.add(text(id + ".1"));
                if (viewer != null && !Affection.ownerAllowed(stack, viewer))
                    out.add(text("owner_mismatch"));
                else if (!stack.hasTag() || !stack.getTag().hasUUID(Affection.TARGET))
                    out.add(text("companion_unbound"));
                if (stack.hasTag() && stack.getTag().hasUUID(Affection.TARGET))
                    out.add(text("bound", stack.getTag().getString("BoundName")));
            }
            case "tokaido" -> {
                out.add(text(id + ".0"));
                out.add(text(id + ".1", keyName(RainClient.CAST)));
            }
            case "icarus", "sword_and_lantern" -> {
                out.add(text(id + ".0"));
                out.add(text(id + ".1"));
                if (id.equals("icarus")) out.add(text(id + ".witness"));
                boolean enabled =
                        id.equals("icarus")
                                ? IcarusFlight.enabled(stack)
                                : LanternSources.enabled(stack);
                out.add(
                        text(
                                id + ".state",
                                Component.translatable(
                                        PREFIX + (enabled ? "enabled" : "disabled"))));
            }
            case "free_and_easy_wandering" -> {
                out.add(
                        text(
                                id + ".0",
                                keyName(BoundlessClient.DREAM),
                                com.thelongtravail.boundless.BoundlessRelicText.dreamDuration()));
                out.add(text(id + ".1"));
            }
            case "little_prince" -> {
                out.add(
                        text(
                                id + ".0",
                                com.thelongtravail.boundless.BoundlessRelicText.princeHeight()));
            }
            case "lorentz_fantasy" -> {
                for (int i = 0; i < 3; i++) out.add(text(id + "." + i));
                boolean enabled =
                        !stack.hasTag() || !stack.getTag().getBoolean("LorentzGravityDisabled");
                out.add(
                        text(
                                id + ".state",
                                Component.translatable(
                                        PREFIX + (enabled ? "enabled" : "disabled"))));
            }
            case "don_quixote", "tower_of_babel" -> {
                out.add(text(id + ".0"));
                out.add(text(id + ".1"));
                out.add(text(id + ".2"));
            }
            default -> {
                out.add(text(id + ".0"));
                out.add(text(id + ".1"));
            }
        }
        if (VALLEY_SUMMARIES.contains(id)
                && !out.isEmpty()
                && key(out.get(0), (PREFIX + "requires_diary_equip")::equals)) {
            out.add(out.remove(0));
        }
        return out;
    }

    private ThemeTooltips() {}
}
