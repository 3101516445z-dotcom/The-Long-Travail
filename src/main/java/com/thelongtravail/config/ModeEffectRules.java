package com.thelongtravail.config;

import com.thelongtravail.network.TooltipConfigSync;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.ForgeConfigSpec;
import java.util.Map;

public enum ModeEffectRules {
    DIARY(
            "general.toml",
            "malice.affectCreativeAndSpectator",
            "苦旅的恶意惩罚",
            "Malice penalties from The Long Travail"),
    PHOSPHOPHYLLITE_DAMAGE(
            "aspects/deep_valley.toml",
            "items.phosphophyllite.damageAffectsCreativeAndSpectator",
            "磷叶石的受伤倍率",
            "Phosphophyllite incoming damage multiplier"),
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
            "items.spring_corner.harmfulEffectsAffectCreativeAndSpectator",
            "春日一隅施加的中毒、凋零及丁香对负面效果的延长",
            "Poison, Wither, and harmful-effect extensions from A Corner of Spring"),
    FLOWER_THORNS(
            "aspects/flourishing.toml",
            "items.spring_corner.retaliationAffectsCreativeAndSpectator",
            "春日一隅中玫瑰丛的反伤",
            "Rose Bush retaliation from A Corner of Spring"),
    LAFITE_NAUSEA(
            "aspects/abyss.toml",
                    "items.midnight_lafite.nauseaExtensionAffectsCreativeAndSpectator",
            "午夜拉菲的反胃时长延长", "Nausea extension from Midnight Lafite"),
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
                                            + "是否对创造模式或观察者模式的玩家生效。\nfalse：不对这些玩家生效（默认）；true：对这些玩家生效。\n其他生效条件保持不变。",
                                    rule.descriptionEnglish
                                            + ": apply this effect to players in Creative or Spectator mode.\nfalse: these players are exempt (default); true: the effect also applies to them.\nAll other activation conditions still apply.")
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
