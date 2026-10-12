package com.thelongtravail.config;

import com.thelongtravail.network.TooltipConfigSync;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.ForgeConfigSpec;
import java.util.Map;

// 只控制负面效果是否作用于特殊模式玩家，不改变日记状态和饰品资格。
public enum ModeEffectRules {
    DIARY(
            "general.toml",
            "malice.affectCreativeAndSpectator",
            "苦旅的恶意惩罚",
            "Malice penalties from The Long Travail"),
    AZRAEL_SELF(
            "aspects/deep_valley.toml",
            "items.azrael.selfExecutionAffectsCreativeAndSpectator",
            "死告天使对佩戴者自身的处决",
            "Azrael executing its wearer"),
    AZRAEL_TARGET(
            "aspects/deep_valley.toml",
            "items.azrael.targetExecutionAffectsCreativeAndSpectator",
            "死告天使对其他玩家的处决",
            "Azrael executing other players"),
    STAR_FREEZE(
            "aspects/boundless.toml",
            "items.boundless.star.affectCreativeAndSpectator",
            "星之声对范围内玩家的时停",
            "Star Voice freezing players within its area"),
    FLOWER_EFFECTS(
            "aspects/flourishing.toml",
            "items.spring_game.harmfulEffectsAffectCreativeAndSpectator",
            "春日的游戏施加的中毒、凋零及丁香对负面效果的延长",
            "Poison, Wither, and harmful-effect extensions from Springtime Game"),
    FLOWER_THORNS(
            "aspects/flourishing.toml",
            "items.spring_game.retaliationAffectsCreativeAndSpectator",
            "春日的游戏中玫瑰丛的反伤",
            "Rose Bush retaliation from Springtime Game"),
    ICARUS_FIRE(
            "aspects/far_reach.toml",
            "items.icarus.ignitionAffectsCreativeAndSpectator",
            "伊卡洛斯对目标的点燃",
            "Icarus igniting its target");
    public final String file, key;
    private final String description, descriptionEnglish;
    public ForgeConfigSpec.BooleanValue config;

    ModeEffectRules(String file, String key, String description, String descriptionEnglish) {
        this.file = file;
        this.key = key;
        this.description = description;
        this.descriptionEnglish = descriptionEnglish;
    }

    public static void define(Map<String, ForgeConfigSpec.Builder> builders) {
        for (var rule : values())
            rule.config =
                    builders.get(rule.file)
                            .comment(
                                    rule.description
                                            + "是否作用于创造模式或观察者模式玩家。\nfalse：豁免（默认）；true：不豁免。\n不改变其他生效条件。",
                                    rule.descriptionEnglish
                                            + ": whether this affects Creative or Spectator players.\nfalse: exempt (default); true: not exempt.\nOther activation conditions still apply.")
                            .define(rule.key, false);
    }

    public boolean applies(Entity target) {
        if (!(target instanceof Player player) || !player.isCreative() && !player.isSpectator())
            return true;
        return player.level().isClientSide
                ? TooltipConfigSync.integer("mode." + name(), config.get() ? 1 : 0) != 0
                : config.get();
    }

    public static boolean diaryApplies(Player player) {
        return DIARY.applies(player);
    }
}
