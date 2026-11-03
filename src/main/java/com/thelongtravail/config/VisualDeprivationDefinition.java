package com.thelongtravail.config;

import net.minecraftforge.common.ForgeConfigSpec;

// 视觉剥夺的画面参数，装配到幽谷配置文件。
record VisualDeprivationDefinition(
        ForgeConfigSpec.DoubleValue visualDarkening,
        ForgeConfigSpec.DoubleValue visualOpacity,
        ForgeConfigSpec.DoubleValue visualRadius,
        ForgeConfigSpec.DoubleValue visualSoftness,
        ForgeConfigSpec.DoubleValue visualDistanceStart,
        ForgeConfigSpec.DoubleValue visualDistanceEnd,
        ForgeConfigSpec.DoubleValue visualPulsePeriod,
        ForgeConfigSpec.DoubleValue visualPulseDepth,
        ForgeConfigSpec.BooleanValue visualDistanceEnabled,
        ForgeConfigSpec.BooleanValue visualPulseRadius,
        ForgeConfigSpec.IntValue visualRefreshTicks,
        ForgeConfigSpec.IntValue visualClearTicks) {
    static VisualDeprivationDefinition define(ForgeConfigSpec.Builder builder) {
        ForgeConfigSpec.DoubleValue visualDarkening =
                builder.comment(
                                "世界画面的整体变暗比例；0表示不变暗，1表示全黑。",
                                "Overall world darkening fraction; 0 leaves the view unchanged, and"
                                        + " 1 makes it completely black.")
                        .defineInRange(
                                "travail.visualDeprivation.worldDarkening", 0.65D, 0.0D, 1.0D);
        ForgeConfigSpec.DoubleValue visualOpacity =
                builder.comment(
                                "屏幕外围的遮蔽强度；0表示关闭外围遮蔽。",
                                "Strength of the peripheral screen mask; 0 disables it.")
                        .defineInRange(
                                "travail.visualDeprivation.peripheralOpacity", 0.98D, 0.0D, 1.0D);
        ForgeConfigSpec.DoubleValue visualRadius =
                builder.comment(
                                "屏幕中央清晰区域的椭圆半径；1表示横向和纵向均延伸至屏幕边缘，不改变实际视野角度（FOV）。",
                                "Elliptical radius of the clear central area. 1 reaches the screen"
                                        + " edges horizontally and vertically. Does not change the"
                                        + " actual field of view (FOV).")
                        .defineInRange("travail.visualDeprivation.clearRadius", 0.45D, 0.0D, 2.0D);
        ForgeConfigSpec.DoubleValue visualSoftness =
                builder.comment(
                                "外围遮蔽边缘的过渡宽度。",
                                "Transition width at the edge of the peripheral mask.")
                        .defineInRange(
                                "travail.visualDeprivation.edgeSoftness", 0.45D, 0.01D, 2.0D);
        ForgeConfigSpec.DoubleValue visualDistanceStart =
                builder.comment(
                                "距离雾的起始距离，单位：方块；自动限制为不超过结束距离。",
                                "Distance at which the distance veil starts, in blocks,"
                                        + " automatically capped at its end distance.")
                        .defineInRange(
                                "travail.visualDeprivation.distanceVeilStart",
                                1.25D,
                                0.0D,
                                1024.0D);
        ForgeConfigSpec.DoubleValue visualDistanceEnd =
                builder.comment(
                                "距离雾的结束距离，单位：方块；使用光影包时，显示效果可能不同。",
                                "Distance at which the distance veil ends, in blocks. Shader packs"
                                        + " may change its appearance.")
                        .defineInRange(
                                "travail.visualDeprivation.distanceVeilEnd", 5.0D, 0.1D, 4096.0D);
        ForgeConfigSpec.DoubleValue visualPulsePeriod =
                builder.comment(
                                "视觉强度的波动周期，单位：秒。",
                                "Period of visual intensity fluctuations, in seconds.")
                        .defineInRange(
                                "travail.visualDeprivation.pulsePeriodSeconds", 4.0D, 0.1D, 120.0D);
        ForgeConfigSpec.DoubleValue visualPulseDepth =
                builder.comment(
                                "视觉强度的波动幅度；0表示不波动，0.2表示在80%至100%强度之间波动。",
                                "Amplitude of visual intensity fluctuations; 0 disables"
                                        + " fluctuations, and 0.2 varies intensity between 80% and"
                                        + " 100%.")
                        .defineInRange("travail.visualDeprivation.pulseDepth", 0.0D, 0.0D, 1.0D);
        ForgeConfigSpec.BooleanValue visualDistanceEnabled =
                builder.comment(
                                "启用距离雾；关闭后仍保留屏幕遮蔽。",
                                "Enable the distance veil. Disabling it preserves screen masking.")
                        .define("travail.visualDeprivation.distanceVeilEnabled", true);
        ForgeConfigSpec.BooleanValue visualPulseRadius =
                builder.comment(
                                "视觉强度减弱时，同步扩大屏幕中央的清晰区域。",
                                "Expand the clear central area as visual intensity decreases.")
                        .define("travail.visualDeprivation.pulseAffectsRadius", false);
        ForgeConfigSpec.IntValue visualRefreshTicks =
                builder.comment(
                                "淡出期间再次触发时，恢复遮蔽强度所需的游戏刻数。",
                                "Game ticks needed to restore mask intensity when the effect is"
                                        + " triggered again during fade-out.")
                        .defineInRange("travail.visualDeprivation.refreshBlendTicks", 5, 0, 100);
        ForgeConfigSpec.IntValue visualClearTicks =
                builder.comment(
                                "通过饮用牛奶等方式主动解除视觉剥夺后，遮蔽画面逐渐消退所需的时间，单位：游戏刻。视觉剥夺状态本身会立即解除。",
                                "Time for the visual mask to fade after Visual Deprivation is"
                                        + " actively removed, for example by drinking milk, in game"
                                        + " ticks. The status effect itself is removed immediately.")
                        .defineInRange("travail.visualDeprivation.clearFadeOutTicks", 4, 0, 100);
        return new VisualDeprivationDefinition(
                visualDarkening,
                visualOpacity,
                visualRadius,
                visualSoftness,
                visualDistanceStart,
                visualDistanceEnd,
                visualPulsePeriod,
                visualPulseDepth,
                visualDistanceEnabled,
                visualPulseRadius,
                visualRefreshTicks,
                visualClearTicks);
    }
}
