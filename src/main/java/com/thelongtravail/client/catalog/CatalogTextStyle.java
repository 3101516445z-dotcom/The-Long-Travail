package com.thelongtravail.client.catalog;

import static com.thelongtravail.client.catalog.CatalogHighlightRules.ABUNDANCE_TERMS;
import static com.thelongtravail.client.catalog.CatalogHighlightRules.ABUNDANCE_VALUES;
import static com.thelongtravail.client.catalog.CatalogHighlightRules.IMMORTAL_BUD_SCOPE;
import static com.thelongtravail.client.catalog.CatalogHighlightRules.IMMORTAL_BUD_TERMS;
import static com.thelongtravail.client.catalog.CatalogHighlightRules.KEYS;
import static com.thelongtravail.client.catalog.CatalogHighlightRules.STACK_SCOPE;
import static com.thelongtravail.client.catalog.CatalogHighlightRules.SUN_BLADE_TERMS;
import static com.thelongtravail.client.catalog.CatalogHighlightRules.SUN_BLADE_VALUES;
import static com.thelongtravail.client.catalog.CatalogHighlightRules.TALES_TERMS;
import static com.thelongtravail.client.catalog.CatalogHighlightRules.TALES_VALUES;
import static com.thelongtravail.client.catalog.CatalogHighlightRules.TERMS;
import static com.thelongtravail.client.catalog.CatalogHighlightRules.UTILITY_TERMS;
import static com.thelongtravail.client.catalog.CatalogHighlightRules.UTILITY_VALUES;
import static com.thelongtravail.client.catalog.CatalogHighlightRules.VALUES;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.util.StringDecomposer;

import java.util.*;
import java.util.regex.Pattern;

public final class CatalogTextStyle {
    public static final int BODY = 0xB88750,
            BULLET = 0xB8B8B8,
            CONDITION = 0xC9B8FF,
            GOLD = 0xFFD84D,
            VALUE = 0x55D6B0,
            WARNING = 0xFF5555,
            MUTED = 0x555555;

    private record Flat(String text, List<Style> styles) {}

    private record TermMatch(int start, int end, String term) {}

    private static final class HighlightState {
        final Set<String> seen = new HashSet<>();
        int sentenceCount;
        boolean afterLineBreak;

        void reset() {
            seen.clear();
            sentenceCount = 0;
            afterLineBreak = false;
        }

        void advance(String text, int start, int end) {
            for (int i = start; i < end; i++) {
                char c = text.charAt(i);
                if (c == '\r') continue;
                if (c == '\n') {
                    if (afterLineBreak) reset();
                    afterLineBreak = true;
                } else if (!Character.isWhitespace(c)) {
                    afterLineBreak = false;
                }
                boolean decimal =
                        c == '.'
                                && i > 0
                                && i + 1 < text.length()
                                && Character.isDigit(text.charAt(i - 1))
                                && Character.isDigit(text.charAt(i + 1));
                if ("。！？!?".indexOf(c) >= 0 || c == '.' && !decimal) sentenceCount = 0;
            }
        }
    }

    private static void highlightTerms(
            String text,
            List<Style> original,
            Style[] result,
            HighlightState state,
            List<String> terms) {
        List<TermMatch> matches = new ArrayList<>();
        for (String term : terms) {
            var matcher =
                    Pattern.compile(
                                    (term.charAt(0) < 128 ? "\\b" : "")
                                            + Pattern.quote(term)
                                            + (term.charAt(0) < 128 ? "\\b" : ""),
                                    Pattern.CASE_INSENSITIVE)
                            .matcher(text);
            while (matcher.find())
                matches.add(
                        new TermMatch(
                                matcher.start(), matcher.end(), term.toLowerCase(Locale.ROOT)));
        }
        matches.sort(
                Comparator.comparingInt(TermMatch::start)
                        .thenComparing(Comparator.comparingInt(TermMatch::end).reversed()));
        int cursor = 0;
        for (TermMatch match : matches) {
            if (match.start < cursor) continue;
            state.advance(text, cursor, match.start);
            boolean protectedRange = false;
            for (int i = match.start; i < match.end; i++)
                protectedRange |= protectedName(original.get(i));
            if (!protectedRange && !state.seen.contains(match.term) && state.sentenceCount < 2) {
                paint(result, original, match.start, match.end, GOLD);
                state.seen.add(match.term);
                state.sentenceCount++;
            }
            state.advance(text, match.start, match.end);
            cursor = match.end;
        }
        state.advance(text, cursor, text.length());
    }

    private static Flat flatten(Component source) {
        StringBuilder text = new StringBuilder();
        List<Style> styles = new ArrayList<>();
        source.visit(
                (style, fragment) -> {
                    StringDecomposer.iterateFormatted(
                            fragment,
                            style,
                            (index, current, codePoint) -> {
                                text.appendCodePoint(codePoint);
                                for (int n = 0; n < Character.charCount(codePoint); n++)
                                    styles.add(current);
                                return true;
                            });
                    return Optional.empty();
                },
                Style.EMPTY);
        return new Flat(text.toString(), styles);
    }

    public static String plain(Component source) {
        return flatten(source).text;
    }

    private static int color(Style s) {
        return s.getColor() == null ? BODY : s.getColor().getValue();
    }

    private static boolean purple(Style s) {
        return color(s) == 0xC9B8FF || color(s) == CONDITION;
    }

    private static boolean protectedName(Style s) {
        return s.getInsertion() != null
                        && s.getInsertion().startsWith("the_long_travail:")
                        && !s.getInsertion().startsWith("the_long_travail:prose:")
                || color(s) == 0xDC143C;
    }

    private static boolean matchesParameterLine(String text, String key) {
        String template =
                plain(Component.translatable("tooltip.the_long_travail." + key, "\u0001"));
        int at = template.indexOf('\u0001');
        return at >= 0
                && text.startsWith(template.substring(0, at))
                && text.endsWith(template.substring(at + 1));
    }

    public static List<Component> apply(List<Component> paragraphs) {
        List<Component> out = new ArrayList<>();
        HighlightState highlights = new HighlightState();
        boolean blacklistNamesNext = false;
        for (Component source : paragraphs) {
            boolean abyss =
                    source.getContents()
                                    instanceof
                                    net.minecraft.network.chat.contents.TranslatableContents
                                                    abyssTranslation
                            && (abyssTranslation
                                            .getKey()
                                            .startsWith("gui.the_long_travail.catalog.abyss.")
                                    || abyssTranslation
                                            .getKey()
                                            .startsWith("gui.the_long_travail.catalog.far.")
                                    || abyssTranslation
                                            .getKey()
                                            .startsWith("gui.the_long_travail.catalog.valley.")
                                    || abyssTranslation
                                            .getKey()
                                            .startsWith("gui.the_long_travail.catalog.underworld.")
                                    || abyssTranslation
                                            .getKey()
                                            .startsWith("gui.the_long_travail.catalog.boundless."));
            boolean floral =
                    source.getContents()
                                    instanceof
                                    net.minecraft.network.chat.contents.TranslatableContents
                                                    floralTranslation
                            && floralTranslation
                                    .getKey()
                                    .startsWith("gui.the_long_travail.catalog.floral.");
            boolean utility =
                    source.getContents()
                                    instanceof
                                    net.minecraft.network.chat.contents.TranslatableContents
                                                    utilityTranslation
                            && utilityTranslation
                                    .getKey()
                                    .startsWith("gui.the_long_travail.catalog.details.");
            boolean sunBlade =
                    source.getContents()
                                    instanceof
                                    net.minecraft.network.chat.contents.TranslatableContents
                                                    sunBladeTranslation
                            && (sunBladeTranslation
                                            .getKey()
                                            .startsWith("tooltip.the_long_travail.sun_chaser.")
                                    || sunBladeTranslation
                                            .getKey()
                                            .startsWith("tooltip.the_long_travail.kagotsurube."));
            boolean abundance =
                    source.getContents()
                                    instanceof
                                    net.minecraft.network.chat.contents.TranslatableContents
                                                    abundanceTranslation
                            && abundanceTranslation
                                    .getKey()
                                    .startsWith("tooltip.the_long_travail.abundance.");
            boolean immortalBud =
                    source.getContents()
                                    instanceof
                                    net.minecraft.network.chat.contents.TranslatableContents
                                                    translation
                            && translation
                                    .getKey()
                                    .startsWith("tooltip.the_long_travail.immortal_bud.details.");
            boolean tales =
                    source.getContents()
                                    instanceof
                                    net.minecraft.network.chat.contents.TranslatableContents
                                                    talesTranslation
                            && (talesTranslation
                                            .getKey()
                                            .startsWith(
                                                    "tooltip.the_long_travail.ancient_strange_tales.")
                                    || talesTranslation
                                            .getKey()
                                            .startsWith(
                                                    "tooltip.the_long_travail.summary.ancient_strange_tales."));
            Flat f = flatten(source);
            // 高频战栗沿用译文的分段染色，不再套用通用关键词强调规则。
            if (source.getContents()
                            instanceof
                            net.minecraft.network.chat.contents.TranslatableContents tremor
                    && tremor.getKey()
                            .startsWith("tooltip.the_long_travail.high_frequency_tremor.")) {
                MutableComponent line = Component.empty();
                for (int i = 0; i < f.text.length(); i++) {
                    int original = color(f.styles.get(i));
                    int mapped =
                            original == 0xFF55FF || original == CONDITION
                                    ? CONDITION
                                    : original == 0x55FF55 || original == VALUE
                                            ? VALUE
                                            : original == 0xFFAA00
                                                    ? GOLD
                                                    : original == 0x555555 ? MUTED : BODY;
                    line.append(
                            Component.literal(f.text.substring(i, i + 1))
                                    .withStyle(
                                            Style.EMPTY
                                                    .withColor(mapped)
                                                    .withBold(mapped == CONDITION)));
                }
                out.add(line);
                highlights.reset();
                continue;
            }
            String text = f.text;
            boolean objectList =
                    blacklistNamesNext
                            || matchesParameterLine(text, "abyss_items.fluid.blacklist_current")
                            || abundance && matchesParameterLine(text, "abundance.effects");
            blacklistNamesNext =
                    text.equals(
                            plain(
                                    Component.translatable(
                                            "tooltip.the_long_travail.abyss_items.fluid.blacklist")));
            if (text.isBlank()) {
                out.add(Component.literal(text));
                highlights.reset();
                continue;
            }
            boolean condition = f.styles.stream().anyMatch(CatalogTextStyle::purple);
            if (condition) {
                highlights.reset();
                out.add(
                        Component.literal(text)
                                .withStyle(s -> s.withColor(CONDITION).withBold(true)));
                continue;
            }
            Style[] result = new Style[text.length()];
            for (int i = 0; i < result.length; i++) {
                Style old = f.styles.get(i);
                int c = color(old);
                result[i] =
                        protectedName(old)
                                ? old
                                : Style.EMPTY.withColor(
                                        c == WARNING ? WARNING : c == 0x555555 ? MUTED : BODY);
            }
            boolean heading =
                    (text.endsWith("：") || text.endsWith(":")) && !VALUES.matcher(text).find();
            if (heading) Arrays.fill(result, Style.EMPTY.withColor(GOLD));
            boolean supplement =
                    text.equals(
                                    plain(
                                            Component.translatable(
                                                    "tooltip.the_long_travail.star_voice.lock")))
                            || text.equals(
                                    plain(
                                            Component.translatable(
                                                    "tooltip.the_long_travail.star_voice.allies")));
            boolean numbersOnly =
                    matchesParameterLine(text, "star_voice.range")
                            || matchesParameterLine(text, "star_voice.group");
            if (!heading && !supplement && !numbersOnly && !objectList) {
                highlightTerms(
                        text,
                        f.styles,
                        result,
                        highlights,
                        abyss
                                ? java.util.stream.Stream.concat(
                                                TERMS.stream(),
                                                List.of(
                                                        "黄金时代", "伊卡洛斯", "堂吉诃德", "巴别塔", "逐日", "体型倍率",
                                                        "佳肴", "仇恨保护", "鞘翅飞行", "魔法伤害", "火焰、岩浆伤害",
                                                        "直击伤害", "駅", "东海道五十三次", "午夜拉菲", "液化丰饶",
                                                        "随机药水", "逍遥游", "逍遙遊", "小王子", "洛伦兹幻想",
                                                        "洛倫茲幻想", "古今语怪", "古今語怪", "回响碎片", "回響碎片",
                                                        "下界之星", "附魔之瓶", "高空", "死者之书", "死者之書",
                                                        "死者苏生", "死者蘇生", "万魂幡", "萬魂幡", "籠釣瓶", "火焰伤害",
                                                        "生命值", "唤雨", "伤害加成", "移动速度", "饱食度", "饱和度")
                                                        .stream())
                                        .toList()
                                : floral
                                        ? java.util.stream.Stream.concat(
                                                        TERMS.stream(),
                                                        List.of(
                                                                "春日一隅",
                                                                "喜欢的心情",
                                                                "永生芽",
                                                                "缔约",
                                                                "解除契约",
                                                                "恒久长青",
                                                                "已损失生命比例",
                                                                "伤害加成",
                                                                "受到的伤害",
                                                                "移动速度",
                                                                "攻击速度",
                                                                "游泳速度",
                                                                "最大生命值",
                                                                "治疗量",
                                                                "投射物伤害",
                                                                "摔落伤害",
                                                                "饱食度",
                                                                "饱和度",
                                                                "重力",
                                                                "击退抗性",
                                                                "护甲韧性",
                                                                "负面效果",
                                                                "方块交互距离",
                                                                "近战攻击距离",
                                                                "直击伤害",
                                                                "Spring Corner",
                                                                "Affection",
                                                                "Immortal Bud",
                                                                "Evergreen",
                                                                "damage bonus",
                                                                "movement speed",
                                                                "missing-health fraction")
                                                                .stream())
                                                .toList()
                                        : utility
                                                ? UTILITY_TERMS
                                                : tales
                                                        ? TALES_TERMS
                                                        : sunBlade
                                                                ? SUN_BLADE_TERMS
                                                                : abundance
                                                                        ? ABUNDANCE_TERMS
                                                                        : immortalBud
                                                                                ? IMMORTAL_BUD_TERMS
                                                                                : TERMS);
            } else if (heading) highlights.sentenceCount = 0;
            else highlights.advance(text, 0, text.length());
            if (!supplement && !objectList) {
                var values = VALUES.matcher(text);
                while (values.find()) paint(result, f.styles, values.start(), values.end(), VALUE);
                var keys = KEYS.matcher(text);
                while (keys.find()) paint(result, f.styles, keys.start(), keys.end(), VALUE);
                var scope = STACK_SCOPE.matcher(text);
                while (scope.find()) paint(result, f.styles, scope.start(), scope.end(), VALUE);
            }
            // 仅为这句并列药水类型补色，不放宽通用强调数量限制。
            if (source.getContents()
                            instanceof
                            net.minecraft.network.chat.contents.TranslatableContents translation
                    && translation.getKey().equals("tooltip.the_long_travail.abundance.switch")) {
                var lingering =
                        Pattern.compile("滞留型药水|滯留型藥水|\\blingering\\b", Pattern.CASE_INSENSITIVE)
                                .matcher(text);
                while (lingering.find())
                    paint(result, f.styles, lingering.start(), lingering.end(), GOLD);
            }
            if (sunBlade) {
                var values = SUN_BLADE_VALUES.matcher(text);
                while (values.find()) paint(result, f.styles, values.start(), values.end(), VALUE);
            }
            if (abundance && !objectList) {
                var values = ABUNDANCE_VALUES.matcher(text);
                while (values.find()) paint(result, f.styles, values.start(), values.end(), VALUE);
            }
            if (tales) {
                var values = TALES_VALUES.matcher(text);
                while (values.find()) paint(result, f.styles, values.start(), values.end(), VALUE);
            }
            if (immortalBud) {
                var scope = IMMORTAL_BUD_SCOPE.matcher(text);
                while (scope.find()) paint(result, f.styles, scope.start(), scope.end(), VALUE);
            }
            if (utility) {
                var values = UTILITY_VALUES.matcher(text);
                while (values.find()) paint(result, f.styles, values.start(), values.end(), VALUE);
            }
            if (floral || abyss) {
                var values =
                        Pattern.compile(
                                        "点按右键|长按右键|潜行右键|右键|无序合成|生命值未满|同一维度|白天且露天|在水中|一朵|两朵|三朵|\\b(?:right-click|sneak-right-click)\\b",
                                        Pattern.CASE_INSENSITIVE)
                                .matcher(text);
                while (values.find()) paint(result, f.styles, values.start(), values.end(), VALUE);
            }
            // 物品名苦旅保持强调，红色提示与整行紫色标题仍沿用原样式。
            var diaryName =
                    Pattern.compile("苦旅|The Long Travail", Pattern.CASE_INSENSITIVE).matcher(text);
            while (diaryName.find()) {
                if (color(f.styles.get(diaryName.start())) != WARNING)
                    paint(result, f.styles, diaryName.start(), diaryName.end(), GOLD);
            }
            // 明确等级属于效果名称的一部分，罗马数字与效果名整体标金。
            var rankedEffect =
                    Pattern.compile(
                                    "(?:凋零|中毒|抗性提升|Wither|Poison|Resistance)\\s*(?:[Ⅰ-Ⅿ]+|[IVXLCDM]+\\b)",
                                    Pattern.CASE_INSENSITIVE)
                            .matcher(text);
            while (rankedEffect.find())
                paint(result, f.styles, rankedEffect.start(), rankedEffect.end(), GOLD);
            MutableComponent line = Component.empty();
            for (int start = 0; start < result.length; ) {
                int end = start + 1;
                while (end < result.length && result[start].equals(result[end])) end++;
                line.append(Component.literal(text.substring(start, end)).withStyle(result[start]));
                start = end;
            }
            out.add(line);
        }
        return List.copyOf(out);
    }

    private static void paint(Style[] result, List<Style> original, int start, int end, int color) {
        for (int i = start; i < end; i++)
            if (!protectedName(original.get(i))) result[i] = Style.EMPTY.withColor(color);
    }

    private CatalogTextStyle() {}
}
