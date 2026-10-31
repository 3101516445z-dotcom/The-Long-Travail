package com.thelongtravail.abyss;

import com.thelongtravail.config.RainConfig;
import net.minecraft.network.chat.Component;
import java.util.*;

public final class RainTooltips {
    // 服务端共用的组件只供读取；客户端修改样式前须先复制。
    record ServerSnapshot(
            long revision, List<Component> eki, List<Component> tokaido, List<String> slots) {}

    private static ServerSnapshot serverSnapshot;
    private static List<Object> serverValues;
    private static int checkedTick;
    private static long revision;

    static void invalidateServer() {
        serverSnapshot = null;
        serverValues = null;
    }

    static ServerSnapshot serverSnapshot(int tick) {
        if (serverSnapshot == null || tick - checkedTick >= 20 || tick - checkedTick < 0) {
            checkedTick = tick;
            var values =
                    List.<Object>of(
                            RainConfig.DAMAGE.get(),
                            RainConfig.SPEED.get(),
                            RainConfig.IMMUNITY.get(),
                            RainConfig.IMMUNE_SKIP.get(),
                            RainConfig.FALLBACK.get(),
                            RainConfig.EKI_LOCAL.get(),
                            RainConfig.EKI_SKY.get(),
                            com.thelongtravail.api.CarryingApi.allowed(
                                    com.thelongtravail.registry.ModRegistry.EKI.get(), "inventory"),
                            com.thelongtravail.api.CarryingApi.allowed(
                                    com.thelongtravail.registry.ModRegistry.EKI.get(), "offhand"),
                            com.thelongtravail.api.CarryingApi.allowed(
                                    com.thelongtravail.registry.ModRegistry.EKI.get(), "armor"),
                            com.thelongtravail.api.CarryingApi.allowed(
                                    com.thelongtravail.registry.ModRegistry.EKI.get(),
                                    "enderChest"),
                            com.thelongtravail.api.CarryingApi.allowed(
                                    com.thelongtravail.registry.ModRegistry.EKI.get(), "curios"),
                            com.thelongtravail.api.CarryingApi.allowed(
                                    com.thelongtravail.registry.ModRegistry.EKI.get(),
                                    "externalInventoryTick"),
                            List.copyOf(RainConfig.IMMUNITY_EXCLUSIONS.get()),
                            List.copyOf(RainConfig.SLOTS.get()),
                            RainConfig.TOKAIDO_LOCAL.get(),
                            RainConfig.TOKAIDO_SKY.get(),
                            RainConfig.HEAL.get(),
                            RainConfig.HEAL_SECONDS.get(),
                            RainConfig.FOOD.get(),
                            RainConfig.FOOD_SECONDS.get(),
                            RainConfig.SATURATION.get(),
                            RainConfig.FIRE.get(),
                            RainConfig.DURATION.get(),
                            RainConfig.COOLDOWN.get(),
                            RainConfig.DRY_RAIN.get(),
                            RainConfig.SNOW_RAIN.get());
            if (!values.equals(serverValues)) {
                serverValues = values;
                serverSnapshot =
                        new ServerSnapshot(
                                ++revision,
                                ekiSnapshot(),
                                tokaidoSnapshot(),
                                List.copyOf(RainConfig.SLOTS.get()));
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
            case "tooltip.the_long_travail.lorentz.malice_heading",
                            "tooltip.the_long_travail.lorentz.witness_heading",
                            "tooltip.the_long_travail.abyss_items.malice_heading",
                            "tooltip.the_long_travail.abyss_items.witness_heading",
                            "tooltip.the_long_travail.golden_age.malice_heading",
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
                            "tooltip.the_long_travail.book_of_dead.witness_condition",
                            "tooltip.the_long_travail.essence.malice_heading",
                            "tooltip.the_long_travail.essence.witness_heading" ->
                    true;
            default -> false;
        };
    }

    private static Component num(double n) {
        return Component.literal(
                        java.math.BigDecimal.valueOf(n).stripTrailingZeros().toPlainString())
                .withStyle(style -> style.withColor(ACCENT_COLOR));
    }

    public static final String CAST_KEY = "tooltip.the_long_travail.rain.prose.cast";

    private static Component text(String key, Object... args) {
        for (int i = 0; i < args.length; i++)
            if (args[i] instanceof Number)
                args[i] =
                        Component.literal(args[i].toString())
                                .withStyle(style -> style.withColor(ACCENT_COLOR));
        int color =
                key.endsWith("_title")
                        ? TITLE_COLOR
                        : key.startsWith("rain.prose.weather.") || key.startsWith("rain.location.")
                                ? ACCENT_COLOR
                                : BODY_COLOR;
        return Component.translatable("tooltip.the_long_travail." + key, args)
                .withStyle(style -> style.withColor(color));
    }

    private static void condition(
            List<Component> out, boolean local, boolean sky, boolean tokaido) {
        out.add(
                text(
                        "rain.prose.condition",
                        text("rain.prose.weather.rain_or_thunder"),
                        tokaido ? "——" : "。"));
        if (local) out.add(text("rain.prose.local"));
        if (sky) out.add(text("rain.prose.sky"));
    }

    public static List<Component> ekiSnapshot() {
        List<Component> out = new ArrayList<>();
        out.add(Component.empty());
        out.add(text("eki.prose.introduction", text("rain.prose.weather.rain_or_thunder")));
        out.add(text("eki.prose.unique"));
        if (RainConfig.EKI_LOCAL.get()) out.add(text("rain.prose.local"));
        if (RainConfig.EKI_SKY.get()) out.add(text("rain.prose.sky"));
        out.add(Component.empty());
        out.add(text("eki.prose.malice_title"));
        out.add(
                text(
                        "eki.prose.malice",
                        num(RainConfig.DAMAGE.get() * 100),
                        num(RainConfig.SPEED.get() * 100)));
        out.add(Component.empty());
        out.add(text("eki.prose.witness_title"));
        out.add(
                text(
                        RainConfig.IMMUNE_SKIP.get()
                                ? "eki.prose.witness"
                                : "eki.prose.witness_damage",
                        num(RainConfig.IMMUNITY.get() * 100)));
        if (RainConfig.FALLBACK.get()) out.add(text("eki.prose.priority"));
        boolean[] values = {
            com.thelongtravail.api.CarryingApi.allowed(
                    com.thelongtravail.registry.ModRegistry.EKI.get(), "inventory"),
            com.thelongtravail.api.CarryingApi.allowed(
                    com.thelongtravail.registry.ModRegistry.EKI.get(), "offhand"),
            com.thelongtravail.api.CarryingApi.allowed(
                    com.thelongtravail.registry.ModRegistry.EKI.get(), "enderChest"),
            com.thelongtravail.api.CarryingApi.allowed(
                    com.thelongtravail.registry.ModRegistry.EKI.get(), "armor"),
            com.thelongtravail.api.CarryingApi.allowed(
                    com.thelongtravail.registry.ModRegistry.EKI.get(), "curios"),
            com.thelongtravail.api.CarryingApi.allowed(
                    com.thelongtravail.registry.ModRegistry.EKI.get(), "externalInventoryTick")
        };
        String[] keys = {"inventory", "offhand", "ender", "armor", "curios", "external"};
        boolean all = true;
        var locations = Component.empty();
        for (int i = 0; i < keys.length; i++) {
            all &= values[i];
            if (values[i]) {
                if (!locations.getSiblings().isEmpty()) locations.append(", ");
                locations.append(text("rain.location." + keys[i]));
            }
        }
        if (!all)
            out.add(
                    text(
                            "eki.locations",
                            locations.getSiblings().isEmpty()
                                    ? text("rain.prose.none")
                                    : locations));
        if (!RainConfig.IMMUNITY_EXCLUSIONS.get().isEmpty())
            out.add(
                    text(
                            "eki.exclusions",
                            String.join(", ", RainConfig.IMMUNITY_EXCLUSIONS.get())));
        return List.copyOf(out);
    }

    public static List<Component> tokaidoSnapshot() {
        List<Component> out = new ArrayList<>();
        out.add(Component.empty());
        condition(out, RainConfig.TOKAIDO_LOCAL.get(), RainConfig.TOKAIDO_SKY.get(), true);
        out.add(
                text(
                        "tokaido.prose.recovery",
                        num(RainConfig.HEAL_SECONDS.get()),
                        num(RainConfig.HEAL.get()),
                        num(RainConfig.FOOD_SECONDS.get()),
                        RainConfig.FOOD.get(),
                        num(RainConfig.SATURATION.get())));
        if (RainConfig.FIRE.get()) out.add(text("tokaido.prose.fire"));
        out.add(Component.empty());
        out.add(Component.translatable(CAST_KEY).withStyle(style -> style.withColor(BODY_COLOR)));
        String climate =
                RainConfig.DRY_RAIN.get()
                        ? (RainConfig.SNOW_RAIN.get() ? "both" : "dry")
                        : (RainConfig.SNOW_RAIN.get() ? "cold" : "normal");
        out.add(
                text(
                        "tokaido.prose.skill",
                        num(RainConfig.DURATION.get()),
                        text("tokaido.prose.climate." + climate)));
        out.add(text("tokaido.prose.cooldown", num(RainConfig.COOLDOWN.get())));
        return List.copyOf(out);
    }

    public static void tokaido(List<Component> lines) {
        lines.addAll(clientTokaido.isEmpty() ? tokaidoSnapshot() : clientTokaido);
    }

    public static Component coloredLine(Component line) {
        if (line.getContents()
                        instanceof net.minecraft.network.chat.contents.TranslatableContents text
                && isTravailStateCondition(text.getKey())) {
            return Component.literal(net.minecraft.ChatFormatting.stripFormatting(line.getString()))
                    .withStyle(line.getStyle().withColor(STATE_CONDITION_COLOR));
        }
        var result = Component.empty();
        var run = new StringBuilder();
        net.minecraft.network.chat.Style[] previous = {null};
        net.minecraft.util.StringDecomposer.iterateFormatted(
                line.getString(),
                line.getStyle(),
                (index, style, codePoint) -> {
                    var color = style.getColor();
                    if (color != null && color.getValue() == 0x55FF55)
                        style = style.withColor(ACCENT_COLOR);
                    else if (color != null && color.getValue() == 0xFFAA00)
                        style = style.withColor(TITLE_COLOR);
                    else if (color != null && color.getValue() == 0xFF55FF)
                        style = style.withColor(STATE_CONDITION_COLOR);
                    if (!style.equals(previous[0]) && run.length() > 0) {
                        result.append(Component.literal(run.toString()).withStyle(previous[0]));
                        run.setLength(0);
                    }
                    previous[0] = style;
                    run.appendCodePoint(codePoint);
                    return true;
                });
        if (run.length() > 0)
            result.append(Component.literal(run.toString()).withStyle(previous[0]));
        return result;
    }

    private RainTooltips() {}
}
