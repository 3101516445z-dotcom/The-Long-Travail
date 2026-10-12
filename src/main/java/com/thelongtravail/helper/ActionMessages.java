package com.thelongtravail.helper;

import com.thelongtravail.abyss.RainTooltips;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

// 保留翻译键，让服务端发送的消息按接收者的语言显示。
public final class ActionMessages {
    public static MutableComponent text(String key, Object... arguments) {
        String nameKey =
                switch (key) {
                    case "message.the_long_travail.sword_lantern.light_on",
                                    "message.the_long_travail.sword_lantern.light_off" ->
                            "item.the_long_travail.sword_and_lantern";
                    case "message.the_long_travail.homecoming.success",
                                    "message.the_long_travail.homecoming.unavailable",
                                    "message.the_long_travail.renewal.success",
                                    "message.the_long_travail.wayguide.unavailable",
                                    "message.the_long_travail.wayguide.invalid",
                                    "message.the_long_travail.wayguide.no_targets" ->
                            "item.the_long_travail.the_long_travail";
                    case "message.the_long_travail.wayguide.searching",
                                    "message.the_long_travail.wayguide.busy",
                                    "message.the_long_travail.wayguide.not_found",
                                    "message.the_long_travail.wayguide.timeout",
                                    "message.the_long_travail.wayguide.cancelled",
                                    "message.the_long_travail.wayguide.error" ->
                            "item.the_long_travail.wayguide";
                    default -> null;
                };
        boolean ineligible =
                key.equals("message.the_long_travail.homecoming.unavailable")
                        || key.equals("message.the_long_travail.journey.no_diary")
                        || key.equals("message.the_long_travail.wayguide.unavailable")
                        || key.equals("message.the_long_travail.affection.invalid");
        Object[] args =
                nameKey == null
                        ? arguments.clone()
                        : new Object[] {Component.translatable(nameKey)};
        for (int i = 0; i < args.length; i++) {
            if (args[i] instanceof Component name) {
                args[i] =
                        name.copy()
                                .withStyle(
                                        style ->
                                                style.withColor(
                                                        ineligible
                                                                ? 0xAA0000
                                                                : RainTooltips.TITLE_COLOR));
            }
        }
        return Component.translatable(key, args)
                .withStyle(
                        style -> style.withColor(ineligible ? 0xAA0000 : RainTooltips.BODY_COLOR));
    }

    private ActionMessages() {}
}
