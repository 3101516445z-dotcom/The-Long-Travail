package com.thelongtravail.client;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import java.util.List;
import java.util.ArrayList;
import java.util.Optional;

/** Client-only prose with shared sweep bounds and a uniform brown-gold material. */
public final class DiaryDialogue {
    private record Key(String prefix, int count, String arguments, int width, Object language, long generation) {}
    private static final java.util.Map<Key, List<Component>> CACHE = new java.util.LinkedHashMap<>();
    public static void clear() { CACHE.clear(); }
    private DiaryDialogue() {}

    public static void decorate(List<Component> tooltip, String prefix, int count, boolean usePlayerName) {
        int first = -1;
        for (int index = 0; index < tooltip.size(); index++) {
            if (tooltip.get(index).getContents() instanceof net.minecraft.network.chat.contents.TranslatableContents text
                    && text.getKey().equals(prefix + "0")) { first = index; break; }
        }
        if (first < 0 || first + count > tooltip.size()) return;
        for (int index = 0; index < count; index++) {
            if (!(tooltip.get(first + index).getContents() instanceof net.minecraft.network.chat.contents.TranslatableContents text)
                    || !text.getKey().equals(prefix + index)) return;
        }
        Minecraft client = Minecraft.getInstance();
        String name = client.player != null ? client.player.getGameProfile().getName() : client.getUser().getName();
        List<Component> rendered = new ArrayList<>();
        append(rendered, prefix, count, usePlayerName ? new Object[]{Component.literal(name)} : new Object[0]);
        tooltip.subList(first, first + count).clear();
        tooltip.addAll(first, rendered);
    }

    public static void append(List<Component> tooltip, String prefix, int count, Object... arguments) {
        Minecraft client = Minecraft.getInstance();
        int width = Math.min(260, Math.max(100, client.getWindow().getGuiScaledWidth() - 40));
        Key key = new Key(prefix, count, java.util.Arrays.toString(arguments), width,
                net.minecraft.locale.Language.getInstance(), DiaryFontEffects.resourceGeneration());
        List<Component> cached = CACHE.get(key);
        if (cached != null) { cached.forEach(row -> tooltip.add(row.copy())); return; }
        List<net.minecraft.network.chat.MutableComponent> rows = new ArrayList<>();
        for (int index = 0; index < count; index++) {
            Component sentence = Component.translatable(prefix + index, arguments)
                    .withStyle(style -> style.withColor(0xBC995E));
            for (var line : client.font.getSplitter().splitLines(sentence, width, Style.EMPTY)) {
                var row = Component.empty();
                line.visit((style, segment) -> {
                    row.append(Component.literal(segment).setStyle(style));
                    return Optional.empty();
                }, Style.EMPTY);
                rows.add(row);
            }
        }
        int sweepWidth = Math.max(1, rows.stream().mapToInt(client.font::width).max().orElse(1));
        List<Component> rendered = new ArrayList<>();
        for (var row : rows) rendered.add(row.withStyle(style -> style.withInsertion("the_long_travail:prose:" + sweepWidth)));
        if (CACHE.size() >= 8) CACHE.remove(CACHE.keySet().iterator().next());
        CACHE.put(key, List.copyOf(rendered));
        rendered.forEach(row -> tooltip.add(row.copy()));
    }
}
