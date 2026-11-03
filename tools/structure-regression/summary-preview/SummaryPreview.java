package travail.summarypreview;

import com.thelongtravail.client.*;
import com.thelongtravail.client.catalog.*;
import com.thelongtravail.item.ItemDescription;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.*;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.fml.common.Mod;

import java.nio.file.*;
import java.util.*;

@Mod("travail_smoke")
public class SummaryPreview {
    boolean done;
    int checks;

    public SummaryPreview() {
        MinecraftForge.EVENT_BUS.addListener(this::tick);
    }

    List<String> text(List<Component> v) {
        return v.stream().map(Component::getString).toList();
    }

    void check(boolean ok, String message) {
        checks++;
        if (!ok) throw new AssertionError(message);
    }

    List<String> styles(List<Component> lines) {
        var out = new ArrayList<String>();
        for (var line : lines) {
            line.visit(
                    (style, fragment) -> {
                        out.add(
                                fragment
                                        + "|"
                                        + style.getColor()
                                        + "|"
                                        + style.isBold()
                                        + "|"
                                        + style.isItalic());
                        return Optional.empty();
                    },
                    net.minecraft.network.chat.Style.EMPTY);
            out.add("END");
        }
        return out.stream().filter(v -> !v.startsWith("|")).toList();
    }

    void run() {
        for (var entry : CatalogEntries.all()) {
            var stack = entry.stack();
            var direct = new ArrayList<Component>();
            stack.getItem().appendHoverText(stack, null, direct, TooltipFlag.Default.NORMAL);
            if (direct.isEmpty()
                    || direct.stream()
                            .noneMatch(
                                    c ->
                                            "the_long_travail:summary_block"
                                                    .equals(c.getStyle().getInsertion()))) continue;
            check(
                    direct.stream().noneMatch(c -> c.getString().contains("summary_marker")),
                    entry.id() + " direct marker");
            var expected = new ArrayList<Component>();
            expected.add(Component.translatable(ItemDescription.SUMMARY));
            LegacyThemeTooltips.finish(stack, null, expected);
            check(
                    text(direct).equals(text(expected)),
                    entry.id() + " baseline " + text(direct) + " != " + text(expected));
            check(styles(direct).equals(styles(expected)), entry.id() + " styles");
            var supplied = new ArrayList<Component>();
            supplied.add(Component.translatable("curios.modifiers.head"));
            supplied.add(
                    Component.translatable(
                            "attribute.modifier.plus.0", 2, Component.literal("Test")));
            supplied.add(Component.translatable("tooltip.the_long_travail.maid_compat.test"));
            var oldOrder = new ArrayList<Component>();
            oldOrder.add(Component.translatable(ItemDescription.SUMMARY));
            oldOrder.addAll(supplied);
            LegacyThemeTooltips.finish(stack, null, oldOrder);
            var newOrder = new ArrayList<Component>(direct);
            newOrder.addAll(supplied);
            ThemeTooltips.finish(stack, null, newOrder);
            check(text(oldOrder).equals(text(newOrder)), entry.id() + " attribute/maid order");
            var ordered = text(newOrder);
            ThemeTooltips.finish(stack, null, newOrder);
            check(ordered.equals(text(newOrder)), entry.id() + " attribute repeat");
            check(
                    direct.stream()
                            .anyMatch(
                                    c ->
                                            com.thelongtravail.client.diary.DiaryHintTooltip
                                                            .findPrompt(c)
                                                    != null),
                    entry.id() + " hold prompt");
            var before = text(direct);
            ThemeTooltips.finish(stack, null, direct);
            ThemeTooltips.finish(stack, null, direct);
            check(before.equals(text(direct)), entry.id() + " repeat");
            var flat = new ArrayList<Component>();
            for (var c : direct) flat.add(Component.literal(c.getString()));
            ThemeTooltips.finish(stack, null, flat);
            check(before.equals(text(flat)), entry.id() + " flattened");
            var legacy = new ArrayList<Component>();
            legacy.add(Component.literal(ItemDescription.SUMMARY));
            ThemeTooltips.finish(stack, null, legacy);
            check(before.equals(text(legacy)), entry.id() + " literal legacy");
            var details = entry.details().build(stack);
            check(
                    details.stream()
                            .noneMatch(
                                    c ->
                                            c.getString()
                                                    .matches(
                                                            "(?s).*\\b(?:tooltip|gui)\\.the_long_travail\\.[a-zA-Z0-9_.]+.*")),
                    entry.id() + " detail key leak");
            var normal = stack.getTooltipLines(null, TooltipFlag.Default.NORMAL);
            check(
                    normal.stream().noneMatch(c -> c.getString().contains("summary_marker")),
                    entry.id() + " full tooltip");
            var elements =
                    new ArrayList<
                            com.mojang.datafixers.util.Either<
                                    net.minecraft.network.chat.FormattedText,
                                    net.minecraft.world.inventory.tooltip.TooltipComponent>>();
            for (var line : normal) elements.add(com.mojang.datafixers.util.Either.left(line));
            var gather =
                    new net.minecraftforge.client.event.RenderTooltipEvent.GatherComponents(
                            stack, 1280, 900, elements, 400);
            com.thelongtravail.client.diary.DiarySession.tooltip(gather);
            check(
                    elements.stream()
                            .anyMatch(
                                    e ->
                                            e.right().orElse(null)
                                                    instanceof
                                                    com.thelongtravail.client.diary
                                                            .DiaryProgressTooltip),
                    entry.id() + " actual progress component");
        }
        var unknown = new ArrayList<Component>();
        unknown.add(Component.literal(ItemDescription.SUMMARY));
        ThemeTooltips.finish(new ItemStack(Items.STONE), null, unknown);
        check(unknown.isEmpty(), "unknown cleanup");
    }

    void readable(String label, List<Component> lines) {
        check(!lines.isEmpty(), label + " empty");
        check(
                lines.stream()
                        .noneMatch(
                                c ->
                                        c.getString()
                                                        .matches(
                                                                "(?s).*(?:tooltip|gui)\\.the_long_travail\\.[a-zA-Z0-9_.]+.*")
                                                || c.getString()
                                                        .contains("Description unavailable")),
                label + " unresolved");
    }

    List<String> variant(String id, java.util.function.Consumer<ItemStack> edit) {
        var entry = CatalogEntries.byId(id);
        var stack = entry.stack();
        edit.accept(stack);
        readable(id + " state summary", stack.getTooltipLines(null, TooltipFlag.NORMAL));
        readable(id + " state details", entry.details().build(stack));
        return text(stack.getTooltipLines(null, TooltipFlag.NORMAL));
    }

    void states() throws Exception {
        var ids = CatalogGlossary.class.getDeclaredField("IDS");
        ids.setAccessible(true);
        for (Object id : (List<?>) ids.get(null))
            readable("glossary " + id, List.of(CatalogGlossary.body((String) id)));

        var growthStates = new HashSet<List<String>>();
        for (boolean enabled : new boolean[] {false, true})
            growthStates.add(
                    variant(
                            "everlasting_flower",
                            s -> s.getOrCreateTag().putBoolean("TravailGrowth", enabled)));
        check(growthStates.size() == 2, "growth on/off text differs");
        for (int souls : new int[] {-1, 0, 1, Integer.MAX_VALUE})
            variant(
                    "myriad_souls_banner",
                    s -> {
                        var n = new net.minecraft.nbt.CompoundTag();
                        n.putInt("Souls", souls);
                        s.getOrCreateTag().put("the_long_travail:soul_banner", n);
                    });
        var flowers = com.thelongtravail.flourishing.Flower.values();
        for (int count : new int[] {0, 1, flowers.length})
            variant(
                    "spring_corner",
                    s ->
                            com.thelongtravail.flourishing.FlowerData.write(
                                    s, Arrays.asList(flowers).subList(0, count)));
        variant(
                "star_voice",
                s ->
                        s.getOrCreateTag()
                                .putBoolean(com.thelongtravail.flourishing.ImmortalBud.TAG, true));
        var modes = new HashSet<List<String>>();
        for (int mode = 0; mode < 3; mode++) {
            com.thelongtravail.network.AbundanceModePacket.receiver.accept(mode);
            modes.add(variant("liquefied_abundance", s -> {}));
        }
        check(modes.size() == 3, "all three abundance labels differ");
        com.thelongtravail.network.AbundanceModePacket.receiver.accept(0);
        var accept =
                com.thelongtravail.network.TooltipConfigSync.class.getDeclaredMethod(
                        "accept", Map.class, Map.class);
        accept.setAccessible(true);
        try {
            for (double value : new double[] {0, 1}) {
                accept.invoke(
                        null,
                        Map.of(
                                "floral.growthDefault",
                                value,
                                "boundlessItem.star.maliceChance",
                                value,
                                "boundlessItem.star.witnessChance",
                                value),
                        Map.of());
                variant("everlasting_flower", s -> {});
                variant("star_voice", s -> {});
            }
        } finally {
            com.thelongtravail.network.TooltipConfigSync.reset();
        }
    }

    void reload(int index) {
        var mc = Minecraft.getInstance();
        String[] locales = {"en_us", "zh_cn"};
        if (index == locales.length) {
            finish(null);
            return;
        }
        mc.getLanguageManager().setSelected(locales[index]);
        mc.reloadResourcePacks()
                .whenComplete(
                        (unused, error) ->
                                mc.execute(
                                        () -> {
                                            if (error != null) {
                                                finish(error);
                                                return;
                                            }
                                            try {
                                                run();
                                                states();
                                                String value =
                                                        TooltipTranslations.text(
                                                                        "gui.the_long_travail.catalog.glossary.other_entity_damage.title")
                                                                .getString();
                                                check(
                                                        value.equals(
                                                                index == 0
                                                                        ? "Damage from Others"
                                                                        : "他者伤害"),
                                                        "resource reload locale " + locales[index]);
                                                reload(index + 1);
                                            } catch (Throwable failure) {
                                                finish(failure);
                                            }
                                        }));
    }

    void finish(Throwable failure) {
        var mc = Minecraft.getInstance();
        try {
            if (failure != null) failure.printStackTrace();
            Files.writeString(
                    mc.gameDirectory.toPath().resolve("result.txt"),
                    failure == null
                            ? "PASS checks="
                                    + checks
                                    + "; state variants and two actual resource reloads"
                            : "FAIL " + failure);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        mc.stop();
    }

    void tick(TickEvent.ClientTickEvent e) {
        if (done || e.phase != TickEvent.Phase.END) return;
        var mc = Minecraft.getInstance();
        if (mc.getOverlay() != null || !(mc.screen instanceof TitleScreen)) return;
        done = true;
        try {
            for (String locale : List.of("zh_cn", "zh_tw", "zh_hk", "lzh", "en_us")) {
                net.minecraft.locale.Language.inject(
                        net.minecraft.client.resources.language.ClientLanguage.loadFrom(
                                mc.getResourceManager(), List.of("en_us", locale), false));
                run();
                states();
            }
            var original = net.minecraft.locale.Language.getInstance();
            net.minecraft.locale.Language.inject(
                    new net.minecraft.locale.Language() {
                        public String getOrDefault(String key, String fallback) {
                            return fallback;
                        }

                        public boolean has(String key) {
                            return false;
                        }

                        public boolean isDefaultRightToLeft() {
                            return false;
                        }

                        public net.minecraft.util.FormattedCharSequence getVisualOrder(
                                net.minecraft.network.chat.FormattedText text) {
                            return original.getVisualOrder(text);
                        }
                    });
            String fallback =
                    TooltipTranslations.text(
                                    "tooltip.the_long_travail.summary.ancient_strange_tales.0")
                            .getString();
            check(
                    fallback.contains("Shapeless")
                            && !fallback.contains("tooltip.the_long_travail"),
                    "missing language fallback");
            check(
                    TooltipTranslations.text(
                                    "gui.the_long_travail.catalog.glossary.other_entity_damage.body")
                            .getString()
                            .contains("Damage"),
                    "glossary fallback");
            check(
                    TooltipTranslations.text("tooltip.the_long_travail.nonexistent_test")
                            .getString()
                            .equals("Description unavailable"),
                    "unknown key fallback");
            net.minecraft.locale.Language.inject(original);
            reload(0);
        } catch (Throwable t) {
            finish(t);
        }
    }
}
