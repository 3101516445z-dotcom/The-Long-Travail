package com.thelongtravail.client.catalog;

import static com.thelongtravail.registry.ModRegistry.*;
import com.thelongtravail.TravailAspect;
import com.thelongtravail.abyss.RainTooltips;
import com.thelongtravail.boundless.BoundlessTooltips;
import com.thelongtravail.client.AzraelTooltips;
import com.thelongtravail.client.RainClient;
import com.thelongtravail.client.UnderworldClient;
import com.thelongtravail.helper.TravailCurios;
import com.thelongtravail.item.FarReachAccessoryItem;
import com.thelongtravail.item.UnderworldAccessoryItem;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.world.item.*;
import java.util.*;
import java.util.function.Supplier;

public final class CatalogEntries {
    public enum Category {
        AID("旅程助力"),
        STONE("解密之石"),
        ESSENCE("此世精粹"),
        THEME("主题物品");
        public final String label;

        Category(String label) {
            this.label = label;
        }

        public String label() {
            return Component.translatable(
                            "gui.the_long_travail.catalog.category."
                                    + name().toLowerCase(Locale.ROOT))
                    .getString();
        }
    }

    @FunctionalInterface
    public interface Details {
        List<Component> build(ItemStack stack);
    }

    public record Entry(
            String id,
            Supplier<? extends Item> item,
            Category category,
            TravailAspect theme,
            String aliases,
            Details details) {
        public ItemStack stack() {
            return new ItemStack(item.get());
        }

        public String name() {
            return ChatFormatting.stripFormatting(stack().getHoverName().getString());
        }

        public String subtitle() {
            return category.label() + (theme == null ? "" : " · " + themeName(theme));
        }

        private int score(String query) {
            String q = normalize(query), name = normalize(name());
            if (name.equals(q)) return 0;
            List<String> forms = new ArrayList<>(List.of(aliases.split("\\|")));
            forms.addAll(CatalogSearchNames.forKey(item.get().getDescriptionId()));
            forms.replaceAll(s -> ChatFormatting.stripFormatting(s));
            if (forms.stream().map(CatalogEntries::normalize).anyMatch(q::equals)) return 0;
            forms.add(name);
            forms.add(id);
            forms.add(subtitle());
            if (forms.stream().map(CatalogEntries::normalize).anyMatch(s -> s.startsWith(q)))
                return 1;
            if (forms.stream().map(CatalogEntries::normalize).anyMatch(s -> s.contains(q)))
                return 2;
            String all = normalize(String.join(" ", forms));
            return Arrays.stream(query.toLowerCase(Locale.ROOT).trim().split("\\s+"))
                            .allMatch(token -> all.contains(normalize(token)))
                    ? 3
                    : -1;
        }
    }

    private static final List<Entry> ENTRIES = build();

    public static List<Entry> all() {
        return ENTRIES;
    }

    public static Entry byId(String id) {
        return ENTRIES.stream().filter(e -> e.id.equals(id)).findFirst().orElseThrow();
    }

    public static Optional<Entry> forStack(ItemStack stack) {
        return ENTRIES.stream().filter(e -> stack.is(e.item.get())).findFirst();
    }

    public static List<Entry> search(String query) {
        if (normalize(query).isEmpty()) return List.of();
        return ENTRIES.stream()
                .filter(e -> e.score(query) >= 0)
                .sorted(Comparator.comparingInt(e -> e.score(query)))
                .toList();
    }

    private static String normalize(String s) {
        return s.toLowerCase(Locale.ROOT).replaceAll("[\\s'’_·-]", "");
    }

    public static String themeName(TravailAspect a) {
        return Component.translatable("aspect.the_long_travail." + a.id()).getString();
    }

    private static List<Entry> build() {
        List<Entry> entries = new ArrayList<>();
        add(
                entries,
                "homecoming",
                HOMECOMING,
                Category.AID,
                null,
                "gui xiang|guixiang|gx|homecoming",
                ignored -> CatalogUtilityDetails.aid("homecoming"));
        add(
                entries,
                "renewal",
                RENEWAL,
                Category.AID,
                null,
                "quan xiu|quanxiu|qx|renewal",
                ignored -> CatalogUtilityDetails.aid("renewal"));
        add(
                entries,
                "wayguide",
                WAYGUIDE,
                Category.AID,
                null,
                "lu yin|luyin|ly|wayguide",
                ignored -> CatalogUtilityDetails.aid("wayguide"));
        add(
                entries,
                "coloring",
                COLORING,
                Category.AID,
                null,
                "shise|ss|施色|coloring",
                stack -> CatalogUtilityDetails.aid("coloring"));
        String[] py = {
            "fanmao|fm|flourishing",
            "guixu|gx|abyss",
            "qiongxia|qx|farreach",
            "yougu|yg|deepvalley",
            "mingfu|mf|underworld",
            "wuyin|wy|boundless"
        };
        for (Category category : List.of(Category.STONE, Category.ESSENCE))
            for (TravailAspect a : TravailAspect.values()) {
                boolean stone = category == Category.STONE;
                String p = py[a.ordinal()].split("\\|")[0],
                        initials = py[a.ordinal()].split("\\|")[1];
                add(
                        entries,
                        a.id() + (stone ? "_stone" : "_essence"),
                        (stone ? REVELATION_STONES : ESSENCES).get(a.ordinal()),
                        category,
                        a,
                        py[a.ordinal()]
                                + "|"
                                + p
                                + (stone ? "jiemizhishi" : "jingcui")
                                + "|"
                                + p
                                + (stone ? "zhishi" : "jingcui")
                                + "|"
                                + initials
                                + (stone ? "zs" : "jc")
                                + "|"
                                + initials
                                + (stone ? "jmzs" : "jc")
                                + (stone
                                        ? "|jiemizhishi|jmzs|revelationstone"
                                        : "|cishijingcui|csjc|jingcui|jc|essence"),
                        s -> CatalogUtilityDetails.themed(stone, a, s));
            }
        add(
                entries,
                "spring_corner",
                SPRING_CORNER,
                Category.THEME,
                TravailAspect.FLOURISHING,
                "chunriyiyu|cryy|春日的游戏|chunrideyouxi|crdyx|springgame|springtimegame",
                CatalogEntries::ordinary);
        add(
                entries,
                "affection",
                AFFECTION,
                Category.THEME,
                TravailAspect.FLOURISHING,
                "xihuandexinqing|xhdxq|affection",
                CatalogEntries::ordinary);
        add(
                entries,
                "mistletoe",
                MISTLETOE,
                Category.THEME,
                TravailAspect.FLOURISHING,
                "hujisheng|hjs|mistletoe",
                s -> com.thelongtravail.flourishing.FlourishingItemText.details(false));
        add(
                entries,
                "everlasting_flower",
                EVERLASTING_FLOWER,
                Category.THEME,
                TravailAspect.FLOURISHING,
                "wuqionghua|wqh|everlastingflower",
                s -> com.thelongtravail.flourishing.FlourishingItemText.details(true));
        add(
                entries,
                "immortal_bud",
                IMMORTAL_BUD,
                Category.THEME,
                TravailAspect.FLOURISHING,
                "yongshengya|ysy|immortalbud",
                CatalogEntries::ordinary);
        add(
                entries,
                "eki",
                EKI,
                Category.THEME,
                TravailAspect.ABYSS,
                "yi|y|eki|驿",
                s ->
                        (RainTooltips.clientEki.isEmpty()
                                        ? RainTooltips.ekiSnapshot()
                                        : RainTooltips.clientEki)
                                .stream().map(RainTooltips::coloredLine).toList());
        add(
                entries,
                "tokaido",
                TOKAIDO,
                Category.THEME,
                TravailAspect.ABYSS,
                "donghaidaowushisanci|dhdwssc|tokaido|fiftythreestationsofthetokaido",
                CatalogEntries::ordinary);
        add(
                entries,
                "perfect_fluid",
                PERFECT_FLUID,
                Category.THEME,
                TravailAspect.ABYSS,
                "wanmeiliuti|wmlt|perfectfluid",
                CatalogEntries::ordinary);
        add(
                entries,
                "midnight_lafite",
                MIDNIGHT_LAFITE,
                Category.THEME,
                TravailAspect.ABYSS,
                "wuyelafei|wylf|midnightlafite",
                CatalogEntries::ordinary);
        add(
                entries,
                "liquefied_abundance",
                LIQUEFIED_ABUNDANCE,
                Category.THEME,
                TravailAspect.ABYSS,
                "yehuafengrao|yhfr|liquefiedabundance",
                CatalogEntries::ordinary);
        add(
                entries,
                "golden_age",
                GOLDEN_AGE,
                Category.THEME,
                TravailAspect.FAR_REACH,
                "huangjinshidai|hjsd|goldenage",
                CatalogEntries::far);
        add(
                entries,
                "icarus",
                ICARUS,
                Category.THEME,
                TravailAspect.FAR_REACH,
                "yikaluosi|ykls|icarus",
                CatalogEntries::far);
        add(
                entries,
                "don_quixote",
                DON_QUIXOTE,
                Category.THEME,
                TravailAspect.FAR_REACH,
                "tangjihede|tjhd|donquixote",
                CatalogEntries::ordinary);
        add(
                entries,
                "tower_of_babel",
                TOWER_OF_BABEL,
                Category.THEME,
                TravailAspect.FAR_REACH,
                "babieta|bbt|babel",
                CatalogEntries::ordinary);
        add(
                entries,
                "phosphophyllite",
                PHOSPHOPHYLLITE,
                Category.THEME,
                TravailAspect.DEEP_VALLEY,
                "linyeshi|lys|phosphophyllite",
                CatalogEntries::ordinary);
        add(
                entries,
                "high_frequency_tremor",
                HIGH_FREQUENCY_TREMOR,
                Category.THEME,
                TravailAspect.DEEP_VALLEY,
                "gaopinzhanli|gpzl|highfrequencytremor",
                CatalogEntries::ordinary);
        add(
                entries,
                "underground_velvet",
                UNDERGROUND_VELVET,
                Category.THEME,
                TravailAspect.DEEP_VALLEY,
                "dixiasirong|dxsr|undergroundvelvet",
                CatalogEntries::ordinary);
        add(
                entries,
                "azrael",
                AZRAEL,
                Category.THEME,
                TravailAspect.DEEP_VALLEY,
                "sigaotianshi|sgts|azrael",
                s -> AzraelTooltips.forViewer(Minecraft.getInstance().player, true));
        add(
                entries,
                "sword_and_lantern",
                SWORD_AND_LANTERN,
                Category.THEME,
                TravailAspect.DEEP_VALLEY,
                "jianyudeng|jyd|swordandlantern",
                CatalogEntries::ordinary);
        add(
                entries,
                "a_thousand_years_later",
                THOUSAND_YEARS,
                Category.THEME,
                TravailAspect.UNDERWORLD,
                "yiqiannianhou|yqnh|athousandyearslater",
                CatalogEntries::underworld);
        add(
                entries,
                "book_of_the_dead",
                BOOK_OF_DEAD,
                Category.THEME,
                TravailAspect.UNDERWORLD,
                "sizhezhishu|szzs|bookofthedead",
                CatalogEntries::underworld);
        add(
                entries,
                "demon_king_of_the_sixth_heaven",
                DEMON_KING,
                Category.THEME,
                TravailAspect.UNDERWORLD,
                "diliutianmowang|dltmw",
                s -> com.thelongtravail.underworld.UnderworldRelicText.details(false));
        add(
                entries,
                "myriad_souls_banner",
                SOUL_BANNER,
                Category.THEME,
                TravailAspect.UNDERWORLD,
                "wanhunfan|whf",
                s -> com.thelongtravail.underworld.UnderworldRelicText.details(true));
        add(
                entries,
                "free_and_easy_wandering",
                FREE_AND_EASY_WANDERING,
                Category.THEME,
                TravailAspect.BOUNDLESS,
                "xiaoyaoyou|xyy|freeandeasywandering",
                s ->
                        BoundlessTooltips.forViewer(
                                true,
                                Minecraft.getInstance().player,
                                BoundlessTooltips.dreamKey.get(),
                                true));
        add(
                entries,
                "star_voice",
                STAR_VOICE,
                Category.THEME,
                TravailAspect.BOUNDLESS,
                "xingzhisheng|xzs|starvoice|voiceofthestars",
                s ->
                        BoundlessTooltips.forViewer(
                                false,
                                Minecraft.getInstance().player,
                                BoundlessTooltips.dreamKey.get(),
                                true));
        add(
                entries,
                "little_prince",
                LITTLE_PRINCE,
                Category.THEME,
                TravailAspect.BOUNDLESS,
                "xiaowangzi|xwz|littleprince",
                CatalogEntries::ordinary);
        add(
                entries,
                "lorentz_fantasy",
                LORENTZ_FANTASY,
                Category.THEME,
                TravailAspect.BOUNDLESS,
                "luolunzihuanxiang|llzhx|lorentzfantasy",
                CatalogEntries::ordinary);
        if (entries.stream().map(Entry::id).distinct().count() != entries.size())
            throw new IllegalStateException("Duplicate catalogue entry");
        return List.copyOf(entries);
    }

    private static void add(
            List<Entry> list,
            String id,
            Supplier<? extends Item> item,
            Category category,
            TravailAspect theme,
            String aliases,
            Details details) {
        list.add(new Entry(id, item, category, theme, aliases, details));
    }

    private static List<Component> ordinary(ItemStack stack) {
        List<Component> lines = new ArrayList<>();
        if (stack.getItem() instanceof com.thelongtravail.item.ItemDescription detailed)
            detailed.appendDetails(stack, Minecraft.getInstance().level, lines, TooltipFlag.NORMAL);
        else
            stack.getItem()
                    .appendHoverText(
                            stack, Minecraft.getInstance().level, lines, TooltipFlag.NORMAL);
        lines.removeIf(
                line ->
                        line.getContents() instanceof TranslatableContents t
                                && t.getKey()
                                        .equals(com.thelongtravail.item.ItemDescription.CATALOG));
        lines.replaceAll(
                line ->
                        line.getContents() instanceof TranslatableContents t
                                        && t.getKey().equals(RainTooltips.CAST_KEY)
                                ? Component.translatable(
                                                RainTooltips.CAST_KEY,
                                                com.thelongtravail.client.ThemeTooltips.keyName(
                                                        RainClient.CAST))
                                        .withStyle(line.getStyle())
                                : line);
        return lines;
    }

    private static List<Component> far(ItemStack stack) {
        List<Component> out = new ArrayList<>();
        ((FarReachAccessoryItem) stack.getItem())
                .appendDetails(out, Minecraft.getInstance().player, stack, true);
        return out;
    }

    private static List<Component> underworld(ItemStack stack) {
        return UnderworldClient.details(
                (UnderworldAccessoryItem) stack.getItem(), Minecraft.getInstance().player, true);
    }

    public static List<Component> details(Entry entry, ItemStack actual) {
        List<Component> out = new ArrayList<>();
        var player = Minecraft.getInstance().player;
        if (player != null
                && TravailCurios.stack(player).isEmpty()
                && (entry.category == Category.ESSENCE
                        || Set.of(
                                        "demon_king_of_the_sixth_heaven",
                                        "myriad_souls_banner",
                                        "phosphophyllite",
                                        "don_quixote",
                                        "high_frequency_tremor",
                                        "tower_of_babel",
                                        "everlasting_flower",
                                        "eki",
                                        "golden_age",
                                        "icarus",
                                        "midnight_lafite",
                                        "azrael",
                                        "a_thousand_years_later",
                                        "book_of_the_dead",
                                        "star_voice",
                                        "lorentz_fantasy")
                                .contains(entry.id))) {
            out.add(
                    Component.translatable("gui.the_long_travail.catalog.missing_travail")
                            .withStyle(s -> s.withColor(CatalogTextStyle.WARNING)));
            out.add(Component.empty());
        }
        var body = new ArrayList<>(entry.details.build(entry.stack()));
        while (!body.isEmpty() && body.get(0).getString().isBlank()) body.remove(0);
        out.addAll(body);
        return CatalogTextStyle.apply(out);
    }

    private CatalogEntries() {}
}
