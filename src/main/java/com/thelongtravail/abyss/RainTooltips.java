package com.thelongtravail.abyss;
import com.thelongtravail.config.RainConfig;
import net.minecraft.network.chat.Component;
import java.util.*;

// 文本组件保留翻译键，仅同步由服务端决定的参数。
public final class RainTooltips {
    // 服务端共享，只读使用；客户端需要改样式时应复制组件。
    record ServerSnapshot(long revision, List<Component> eki, List<Component> tokaido, List<String> slots) {}
    private static ServerSnapshot serverSnapshot;
    private static List<Object> serverValues;
    private static int checkedTick;
    private static long revision;
    static void invalidateServer() { serverSnapshot = null; serverValues = null; }
    static ServerSnapshot serverSnapshot(int tick) {
        if (serverSnapshot == null || tick - checkedTick >= 20 || tick - checkedTick < 0) {
            checkedTick = tick;
            // 每秒全服检查一次配置值，兼容不触发重载事件的 ConfigValue.set。
            var values = List.<Object>of(RainConfig.DAMAGE.get(), RainConfig.SPEED.get(), RainConfig.IMMUNITY.get(),
                    RainConfig.IMMUNE_SKIP.get(), RainConfig.FALLBACK.get(), RainConfig.EKI_WEATHER.get(),
                    RainConfig.EKI_LOCAL.get(), RainConfig.EKI_SKY.get(), RainConfig.INVENTORY.get(), RainConfig.OFFHAND.get(),
                    RainConfig.ARMOR.get(), RainConfig.ENDER.get(), RainConfig.CURIOS.get(), RainConfig.EXTERNAL_TICK.get(),
                    List.copyOf(RainConfig.IMMUNITY_EXCLUSIONS.get()), List.copyOf(RainConfig.SLOTS.get()),
                    RainConfig.TOKAIDO_WEATHER.get(), RainConfig.TOKAIDO_LOCAL.get(), RainConfig.TOKAIDO_SKY.get(),
                    RainConfig.HEAL.get(), RainConfig.HEAL_SECONDS.get(), RainConfig.FOOD.get(), RainConfig.FOOD_SECONDS.get(),
                    RainConfig.SATURATION.get(), RainConfig.FIRE.get(), RainConfig.SKILL_ENABLED.get(), RainConfig.DURATION.get(),
                    RainConfig.COOLDOWN.get(), RainConfig.DRY_RAIN.get(), RainConfig.SNOW_RAIN.get());
            if (!values.equals(serverValues)) {
                serverValues = values;
                serverSnapshot = new ServerSnapshot(++revision, ekiSnapshot(), tokaidoSnapshot(), List.copyOf(RainConfig.SLOTS.get()));
            }
        }
        return serverSnapshot;
    }
    public static List<Component> clientEki = List.of(), clientTokaido = List.of();
    public static final int BODY_COLOR = 0xB88750;
    public static final int TITLE_COLOR = 0xFFD84D;
    public static final int ACCENT_COLOR = 0x55D6B0;
    public static final int STATE_CONDITION_COLOR = 0xC9B8FF;
    private static boolean isTravailStateCondition(String key) {
        return switch (key) {
            case "tooltip.the_long_travail.golden_age.malice_heading",
                    "tooltip.the_long_travail.golden_age.witness_heading",
                    "tooltip.the_long_travail.icarus.malice_heading",
                    "tooltip.the_long_travail.icarus.witness_heading",
                    "tooltip.the_long_travail.eki.prose.malice_title",
                    "tooltip.the_long_travail.eki.prose.witness_title",
                    "tooltip.the_long_travail.azrael.malice_title",
                    "tooltip.the_long_travail.azrael.witness_title",
                    "tooltip.the_long_travail.star_voice.malice_heading",
                    "tooltip.the_long_travail.star_voice.witness_heading",
                    "tooltip.the_long_travail.thousand_years.malice_title",
                    "tooltip.the_long_travail.thousand_years.witness_title",
                    "tooltip.the_long_travail.book_of_dead.witness_condition" -> true;
            default -> false;
        };
    }
    private static Component num(double n) { return Component.literal(java.math.BigDecimal.valueOf(n).stripTrailingZeros().toPlainString()).withStyle(style -> style.withColor(ACCENT_COLOR)); }
    public static final String CAST_KEY = "tooltip.the_long_travail.rain.prose.cast";
    public static final String EKI_LOCKED_KEY = "tooltip.the_long_travail.eki.locked";
    private static Component text(String key, Object... args) {
        for (int i = 0; i < args.length; i++) if (args[i] instanceof Number) args[i] = Component.literal(args[i].toString()).withStyle(style -> style.withColor(ACCENT_COLOR));
        int color = key.endsWith("_title") ? TITLE_COLOR
                : key.startsWith("rain.prose.weather.") || key.startsWith("rain.location.") ? ACCENT_COLOR : BODY_COLOR;
        return Component.translatable("tooltip.the_long_travail." + key, args).withStyle(style -> style.withColor(color));
    }
    private static void condition(List<Component> out, RainConfig.Weather weather, boolean local, boolean sky, boolean tokaido) {
        out.add(text("rain.prose.condition", text("rain.prose.weather." + weather.name().toLowerCase(Locale.ROOT)), tokaido ? "——" : "。"));
        if (local) out.add(text("rain.prose.local"));
        if (sky) out.add(text("rain.prose.sky"));
    }
    public static List<Component> ekiSnapshot() {
        List<Component> out = new ArrayList<>();
        out.add(Component.empty());
        out.add(text("eki.prose.introduction", text("rain.prose.weather." + RainConfig.EKI_WEATHER.get().name().toLowerCase(Locale.ROOT))));
        out.add(text("eki.prose.unique"));
        if (RainConfig.EKI_LOCAL.get()) out.add(text("rain.prose.local"));
        if (RainConfig.EKI_SKY.get()) out.add(text("rain.prose.sky"));
        out.add(Component.empty());
        out.add(text("eki.prose.malice_title"));
        out.add(text("eki.prose.malice", num(RainConfig.DAMAGE.get()*100), num(RainConfig.SPEED.get()*100)));
        out.add(Component.empty());
        out.add(text("eki.prose.witness_title"));
        out.add(text(RainConfig.IMMUNE_SKIP.get() ? "eki.prose.witness" : "eki.prose.witness_damage", num(RainConfig.IMMUNITY.get()*100)));
        if (RainConfig.FALLBACK.get()) out.add(text("eki.prose.priority"));
        boolean[] values = { RainConfig.INVENTORY.get(), RainConfig.OFFHAND.get(), RainConfig.ENDER.get(), RainConfig.ARMOR.get(), RainConfig.CURIOS.get(), RainConfig.EXTERNAL_TICK.get() };
        String[] keys = {"inventory", "offhand", "ender", "armor", "curios", "external"};
        boolean all = true;
        var locations = Component.empty();
        for (int i=0;i<keys.length;i++) {
            all &= values[i];
            if (values[i]) {
                if (!locations.getSiblings().isEmpty()) locations.append(", ");
                locations.append(text("rain.location."+keys[i]));
            }
        }
        if (!all) out.add(text("eki.locations", locations.getSiblings().isEmpty() ? text("rain.prose.none") : locations));
        if (!RainConfig.IMMUNITY_EXCLUSIONS.get().isEmpty()) out.add(text("eki.exclusions", String.join(", ", RainConfig.IMMUNITY_EXCLUSIONS.get())));
        return List.copyOf(out);
    }
    public static List<Component> tokaidoSnapshot() {
        List<Component> out = new ArrayList<>();
        out.add(Component.empty());
        condition(out, RainConfig.TOKAIDO_WEATHER.get(), RainConfig.TOKAIDO_LOCAL.get(), RainConfig.TOKAIDO_SKY.get(), true);
        out.add(text("tokaido.prose.recovery", num(RainConfig.HEAL_SECONDS.get()), num(RainConfig.HEAL.get()), num(RainConfig.FOOD_SECONDS.get()), RainConfig.FOOD.get(), num(RainConfig.SATURATION.get())));
        if (RainConfig.FIRE.get()) out.add(text("tokaido.prose.fire"));
        out.add(Component.empty());
        if (RainConfig.SKILL_ENABLED.get()) {
            // 收到服务端同步的物品说明后，再读取本地按键绑定。
            out.add(Component.translatable(CAST_KEY).withStyle(style -> style.withColor(BODY_COLOR)));
            String climate = RainConfig.DRY_RAIN.get() ? (RainConfig.SNOW_RAIN.get() ? "both" : "dry") : (RainConfig.SNOW_RAIN.get() ? "cold" : "normal");
            out.add(text("tokaido.prose.skill", num(RainConfig.DURATION.get()), text("tokaido.prose.climate."+climate)));
            out.add(text("tokaido.prose.cooldown", num(RainConfig.COOLDOWN.get())));
        } else out.add(text("tokaido.skill_disabled"));
        return List.copyOf(out);
    }
    // 客户端根据当前查看者展开占位符，不读取物品 NBT。
    public static void eki(List<Component> lines) { lines.add(Component.translatable(EKI_LOCKED_KEY).withStyle(net.minecraft.ChatFormatting.DARK_RED)); }
    public static List<Component> ekiForViewer(net.minecraft.world.entity.player.Player player) {
        if (player != null && !com.thelongtravail.helper.TravailCurios.stack(player).isEmpty())
            return clientEki.isEmpty() ? ekiSnapshot() : clientEki;
        List<Component> out = new ArrayList<>();
        out.add(Component.empty());
        out.add(Component.translatable(EKI_LOCKED_KEY).withStyle(net.minecraft.ChatFormatting.DARK_RED));
        out.add(Component.empty());
        for (int i = 0; i < 3; i++) out.add(Component.literal("Travail Eki " + "?".repeat(18 - i * 4))
                .withStyle(style -> style.withColor(BODY_COLOR).withObfuscated(true)));
        return List.copyOf(out);
    }
    public static void tokaido(List<Component> lines) { lines.addAll(clientTokaido.isEmpty() ? tokaidoSnapshot() : clientTokaido); }
    public static Component coloredLine(Component line) {
        if (line.getContents() instanceof net.minecraft.network.chat.contents.TranslatableContents text
                && isTravailStateCondition(text.getKey())) {
            return Component.literal(net.minecraft.ChatFormatting.stripFormatting(line.getString()))
                    .withStyle(line.getStyle().withColor(STATE_CONDITION_COLOR));
        }
        var result = Component.empty();
        var run = new StringBuilder();
        net.minecraft.network.chat.Style[] previous = {null};
        net.minecraft.util.StringDecomposer.iterateFormatted(line.getString(), line.getStyle(), (index, style, codePoint) -> {
            var color = style.getColor();
            if (color != null && color.getValue() == 0x55FF55) style = style.withColor(ACCENT_COLOR);
            else if (color != null && color.getValue() == 0xFFAA00) style = style.withColor(TITLE_COLOR);
            if (!style.equals(previous[0]) && run.length() > 0) {
                result.append(Component.literal(run.toString()).withStyle(previous[0]));
                run.setLength(0);
            }
            previous[0] = style;
            run.appendCodePoint(codePoint);
            return true;
        });
        if (run.length() > 0) result.append(Component.literal(run.toString()).withStyle(previous[0]));
        return result;
    }
    private RainTooltips() {}
}
