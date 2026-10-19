package com.thelongtravail.client;

import com.thelongtravail.abyss.RainTooltips;
import com.thelongtravail.client.catalog.*;
import com.thelongtravail.config.FlourishingItemsConfig;
import com.thelongtravail.farreach.IcarusFlight;
import com.thelongtravail.flourishing.*;
import com.thelongtravail.helper.AccessoryEquipRules;
import com.thelongtravail.item.ItemDescription;
import com.thelongtravail.network.TooltipConfigSync;
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
        lines.replaceAll(line -> key(line, ItemDescription.CATALOG::equals) ? catalogPrompt() : line);
        if (lines.stream().noneMatch(c -> key(c, ItemDescription.SUMMARY::equals))) return;
        var entry = CatalogEntries.forStack(stack).orElse(null);
        if (entry == null || entry.category() != CatalogEntries.Category.THEME) return;
        var maid =
                take(
                        lines,
                        c ->
                                key(
                                        c,
                                        k ->
                                                k.equals("tooltips.touhou_little_maid.bauble.desc")
                                                        || k.equals("tooltips.touhou_little_maid.bauble.usage")
                                                        || k.startsWith("tooltip.the_long_travail.maid_compat.")));
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
        block.addAll(summary(entry.id(), stack, viewer));
        block.addAll(attributes);
        block.add(catalogPrompt());
        if (!maid.isEmpty()) {
            block.add(Component.empty());
            block.addAll(maid);
        }
        lines.addAll(at, block);
    }

    public static Component catalogPrompt() {
        return Component.translatable(
                        PREFIX + "open",
                        keyName(CatalogSession.OPEN).copy().withStyle(s -> s.withColor(RainTooltips.ACCENT_COLOR)))
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
                        else if (color == 0xFFAA00) style = style.withColor(RainTooltips.TITLE_COLOR);
                        else if (color == 0xFF55FF) style = style.withColor(RainTooltips.STATE_CONDITION_COLOR);
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
        String revival = Component.translatable("tooltip.the_long_travail.book_of_dead.revival_name").getString();
        int at = text.indexOf(revival);
        if (at < 0) {
            target.append(Component.literal(text).setStyle(style));
            return;
        }
        target.append(Component.literal(text.substring(0, at)).setStyle(style));
        target.append(Component.literal(revival).setStyle(style.withColor(0xDC143C).withBold(true)));
        target.append(Component.literal(text.substring(at + revival.length())).setStyle(style));
    }

    public static List<Component> summary(String id, ItemStack stack, Player viewer) {
        var out = new ArrayList<Component>();
        if (viewer != null && !AccessoryEquipRules.hasDiary(viewer)) {
            if (Set.of(
                            "demon_king_of_the_sixth_heaven",
                            "myriad_souls_banner",
                            "phosphophyllite",
                            "azrael",
                            "a_thousand_years_later",
                            "book_of_the_dead",
                            "star_voice",
                            "lorentz_fantasy")
                    .contains(id)) out.add(text("requires_diary_equip"));
            else if (id.equals("eki")) out.add(text("requires_diary_effect"));
        }
        switch (id) {
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
                                                                    .getCompound("the_long_travail:soul_banner")
                                                                    .getInt("Souls"))
                                                    : 0,
                                            (int)
                                                    com.thelongtravail.underworld.UnderworldRelicText.shown(
                                                            "banner.capacity")),
                                    com.thelongtravail.underworld.UnderworldRelicText.number(
                                            com.thelongtravail.underworld.UnderworldRelicText.shown(
                                                    "banner.capacity"))));
            }
            case "mistletoe" -> {
                for (int i = 0; i < 3; i++) out.add(text(id + "." + i));
            }
            case "everlasting_flower" -> {
                for (int i = 0; i < 4; i++) out.add(text(id + "." + i));
                out.add(
                        text(
                                id + ".state",
                                Component.translatable(
                                        PREFIX + (EverlastingFlower.growthEnabled(stack) ? "enabled" : "disabled"))));
            }
            case "perfect_fluid" -> {
                out.add(text("perfect_fluid.0"));
                out.add(text("perfect_fluid.1"));
            }
            case "phosphophyllite", "underground_velvet" -> {
                for (int i = 0; i < (id.equals("underground_velvet") ? 3 : 2); i++) out.add(text(id + "." + i));
            }
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
                        if (i > 1) secondary.append(Component.translatable(PREFIX + "list_separator"));
                        secondary.append(flowers.get(i).item().getDescription());
                    }
                    out.add(
                            text(
                                    "secondary_flower",
                                    flowers.size() == 1 ? Component.translatable(PREFIX + "none") : secondary));
                }
            }
            case "affection" -> {
                out.add(text(id + ".0"));
                out.add(text(id + ".1"));
                if (viewer != null && !Affection.ownerAllowed(stack, viewer)) out.add(text("owner_mismatch"));
                else if (!stack.hasTag() || !stack.getTag().hasUUID(Affection.TARGET))
                    out.add(text("companion_unbound"));
                if (stack.hasTag() && stack.getTag().hasUUID(Affection.TARGET))
                    out.add(text("bound", stack.getTag().getString("BoundName")));
            }
            case "tokaido" -> {
                out.add(text(id + ".0"));
                var snapshot =
                        RainTooltips.clientTokaido.isEmpty()
                                ? RainTooltips.tokaidoSnapshot()
                                : RainTooltips.clientTokaido;
                if (snapshot.stream().anyMatch(c -> key(c, RainTooltips.CAST_KEY::equals)))
                    out.add(text(id + ".1", keyName(RainClient.CAST)));
            }
            case "icarus", "sword_and_lantern" -> {
                out.add(text(id + ".0"));
                out.add(text(id + ".1"));
                if (id.equals("icarus")) out.add(text(id + ".witness"));
                out.add(text(id + ".2"));
                boolean enabled = id.equals("icarus") ? IcarusFlight.enabled(stack) : LanternSources.enabled(stack);
                out.add(text(id + ".state", Component.translatable(PREFIX + (enabled ? "enabled" : "disabled"))));
            }
            case "free_and_easy_wandering" -> {
                out.add(text(id + ".0", keyName(BoundlessClient.DREAM)));
                out.add(text(id + ".1"));
            }
            case "little_prince" -> {
                out.add(text(id + ".0", com.thelongtravail.boundless.BoundlessRelicText.princeHeight()));
                out.add(text(id + ".1"));
            }
            case "lorentz_fantasy" -> {
                for (int i = 0; i < 4; i++) out.add(text(id + "." + i));
                boolean enabled = !stack.hasTag() || !stack.getTag().getBoolean("LorentzGravityDisabled");
                out.add(text(id + ".state", Component.translatable(PREFIX + (enabled ? "enabled" : "disabled"))));
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
        return out;
    }

    private ThemeTooltips() {}
}
