package com.thelongtravail.client.catalog;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.util.StringDecomposer;

import java.util.*;
import java.util.regex.Pattern;

public final class CatalogTextStyle {
    public static final int BODY = 0xB88750,
            CONDITION = 0xC9B8FF,
            GOLD = 0xFFD84D,
            VALUE = 0x55D6B0,
            WARNING = 0xFF5555,
            MUTED = 0x555555;
    private static final Pattern VALUES =
            Pattern.compile(
                    "[+-]?\\d+(?:\\.\\d+)?(?:[%％]|(?:\\s*|-)(?:seconds?|minutes?|blocks?|levels?(?:\\(s\\))?|points?|times?)(?!\\w)|秒|分钟|格|级|点|倍|朵|条|张|颗|本|个|份|件|种|種)?"
                            + "|[一二两三四五六七八九十]+(?:朵|条|张|级|格|秒|分钟|件|份|个)|[0-9一二三四五六七八九十]+[种種]|\\bnine(?="
                            + " special text styles\\b)|\\bone(?= Perfect"
                            + " Fluid\\b)|\\b(?:one|two|three|four|five|six)"
                            + " (?:flowers?|records?|levels?|blocks?|seconds?|items?|copies|copy)\\b",
                    Pattern.CASE_INSENSITIVE);
    private static final Pattern KEYS = Pattern.compile("(?<=\\[)[^\\[\\]\\r\\n]+(?=\\])");
    private static final Pattern STACK_SCOPE =
            Pattern.compile("整组物品|整組物品|整組之物|\\bentire stack\\b", Pattern.CASE_INSENSITIVE);
    private static final List<String> TERMS =
            List.of(
                    "施色",
                    "还原样式",
                    "還原樣式",
                    "Coloring",
                    "Restore Style",
                    "不会下坠",
                    "箭矢不下坠",
                    "弹射物伤害",
                    "do not fall",
                    "gravity-free arrows",
                    "projectile damage",
                    "piercing",
                    "创造飞行",
                    "虚空伤害",
                    "箭矢穿透",
                    "爆头",
                    "creative flight",
                    "void damage",
                    "headshot",
                    "见切",
                    "必杀",
                    "霸主",
                    "吸血",
                    "魂数",
                    "Insight",
                    "Critical Chance",
                    "Overlord",
                    "life steal",
                    "地下丝绒",
                    "Underground Velvet",
                    "所有属性",
                    "all attributes",
                    "伤害吸收",
                    "absorption",
                    "经验等级",
                    "experience level",
                    "真实伤害",
                    "true damage",
                    "仇恨保护",
                    "hostility protection",
                    "补货速度",
                    "restock",
                    "巴别塔",
                    "Tower of Babel",
                    "寄生剥离",
                    "Parasitic Separation",
                    "槲寄生",
                    "催生",
                    "抗性提升",
                    "追加伤害",
                    "Mistletoe",
                    "growth acceleration",
                    "Resistance",
                    "additional damage",
                    "判定冷却",
                    "反转条件",
                    "见证条件",
                    "旅程记录",
                    "入梦",
                    "时停",
                    "主花",
                    "配花",
                    "生命恢复",
                    "饱和",
                    "中毒",
                    "反胃",
                    "饥饿",
                    "漂浮",
                    "魔法伤害",
                    "完美流体",
                    "Regeneration",
                    "Saturation",
                    "Poison",
                    "Nausea",
                    "Hunger",
                    "Levitation",
                    "magic damage",
                    "Perfect Fluid",
                    "处决",
                    "绑定",
                    "淘金",
                    "暴雨",
                    "鞘翅飞行",
                    "照明功能",
                    "凋零",
                    "roll cooldown",
                    "reversal condition",
                    "Witness requirements",
                    "journey records",
                    "journey record",
                    "Enter Dream",
                    "time stop",
                    "main flower",
                    "supporting flower",
                    "execution",
                    "bind",
                    "elytra flight",
                    "rainstorm",
                    "Wither");
    // 逐日与籠釣瓶详情专用，复用通用句内限额与段内去重。
    private static final List<String> SUN_BLADE_TERMS =
            List.of(
                    "火焰、岩浆伤害",
                    "火焰、熔岩傷害",
                    "火焰、熔岩之傷",
                    "fire and lava damage",
                    "归墟之恶意",
                    "歸墟之惡意",
                    "歸墟惡意",
                    "Abyss Malice",
                    "流体伤害",
                    "流體傷害",
                    "fluid damage",
                    "反弹",
                    "反彈",
                    "Reflects",
                    "弹射物伤害",
                    "彈射物傷害",
                    "projectile damage",
                    "正常治疗",
                    "正常治療",
                    "常規治療",
                    "normal healing",
                    "无敌帧",
                    "無敵幀",
                    "damage invulnerability frames",
                    "直击伤害",
                    "直擊傷害",
                    "direct strike damage",
                    "凋零",
                    "Wither",
                    "见切",
                    "見切",
                    "Insight",
                    "霸主",
                    "Overlord");
    private static final Pattern SUN_BLADE_VALUES =
            Pattern.compile(
                    "(?<=凋零)Ⅰ|(?<=Wither )I\\b|\\b\\d+(?:\\.\\d+)?\\s+health\\b",
                    Pattern.CASE_INSENSITIVE);

    // 词表仅用于液化丰饶，不改变其他物品页面。
    private static final List<String> ABUNDANCE_TERMS =
            List.of(
                    "液化丰饶",
                    "液化豐饒",
                    "Liquefied Abundance",
                    "随机药水",
                    "隨機藥水",
                    "random potion",
                    "状态效果",
                    "狀態效果",
                    "status effect",
                    "status effects",
                    "普通药水",
                    "普通藥水",
                    "喷溅型药水",
                    "噴濺型藥水",
                    "飛濺型藥水",
                    "滞留型药水",
                    "滯留型藥水",
                    "splash",
                    "lingering",
                    "瞬时效果",
                    "瞬時效果",
                    "Instant",
                    "黑名单",
                    "黑名單",
                    "blacklist");
    private static final Pattern ABUNDANCE_VALUES =
            Pattern.compile(
                    "长按右键|長按右鍵|点按右键|點按右鍵|Ⅰ级|Ⅰ級|四分之一|无限持续|無限持續"
                            + "|right-click|level I\\b|one quarter|infinite|每次长按|每次長按",
                    Pattern.CASE_INSENSITIVE);
    // 仅用于永生芽条目，避免术语匹配改变其他页面的染色。
    private static final List<String> IMMORTAL_BUD_TERMS =
            List.of(
                    "永生芽",
                    "恒久长青",
                    "恆久長青",
                    "Immortal Bud",
                    "Evergreen",
                    "重复赋予",
                    "重複賦予",
                    "再授",
                    "receive it again",
                    "名称",
                    "原物之名",
                    "名稱",
                    "附魔",
                    "original name",
                    "enchantments",
                    "耐久保护",
                    "耐久保護",
                    "耐久不減",
                    "durability loss",
                    "一次性消耗",
                    "耗損過甚",
                    "single durability cost",
                    "防火",
                    "防岩浆",
                    "防岩漿",
                    "防爆",
                    "不畏火",
                    "岩漿",
                    "爆炸",
                    "fire",
                    "lava",
                    "explosions",
                    "自然消失",
                    "消散",
                    "naturally despawn",
                    "合成消耗",
                    "物品放置",
                    "使用消耗",
                    "合成用料",
                    "置物",
                    "使用之耗",
                    "crafting",
                    "placement",
                    "use",
                    "虚空",
                    "虛空",
                    "void");
    private static final Pattern IMMORTAL_BUD_SCOPE =
            Pattern.compile(
                    "有耐久的物品|有耐久之物|掉落物状态下|掉落物狀態下|落地|a damageable item|when dropped|\\bone(?="
                            + " Immortal Bud)|一個|一枚",
                    Pattern.CASE_INSENSITIVE);

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

    // 古今语怪限定词表，沿用句内限额与段内去重。
    private static final List<String> TALES_TERMS =
            List.of(
                    "下界石英",
                    "青金石",
                    "蜂蜜瓶",
                    "发酵蜘蛛眼",
                    "發酵蜘蛛眼",
                    "诅咒附魔",
                    "詛咒附魔",
                    "非诅咒附魔",
                    "非詛咒附魔",
                    "雕纹书架",
                    "雕紋書架",
                    "附魔台",
                    "铁砧",
                    "鐵砧",
                    "附魔书",
                    "附魔書",
                    "nether quartz",
                    "lapis lazuli",
                    "honey bottle",
                    "fermented spider eye",
                    "curse enchantments",
                    "chiseled bookshelf",
                    "enchanting table",
                    "anvil",
                    "enchanted book");
    private static final Pattern TALES_VALUES =
            Pattern.compile("无序合成|無序合成|等级直接相加|等級直接相加|任何类型|任何類型|改名");

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
                        tales
                                ? TALES_TERMS
                                : sunBlade
                                        ? SUN_BLADE_TERMS
                                        : abundance
                                                ? ABUNDANCE_TERMS
                                                : immortalBud ? IMMORTAL_BUD_TERMS : TERMS);
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
