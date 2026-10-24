package com.thelongtravail.config;

import com.electronwill.nightconfig.core.CommentedConfig;
import com.thelongtravail.TravailAspect;
import com.thelongtravail.compat.alexsmobs.AlexsMobsEffectDefaults;
import com.thelongtravail.compat.arsnouveau.ArsNouveauEffectDefaults;
import com.thelongtravail.compat.attributeslib.AttributesLibEffectDefaults;
import com.thelongtravail.compat.cataclysm.CataclysmEffectDefaults;
import com.thelongtravail.compat.enigmaticaddons.EnigmaticAddonsEffectDefaults;
import com.thelongtravail.compat.goety.GoetyEffectDefaults;
import com.thelongtravail.compat.goetyawaken.GoetyAwakenEffectDefaults;
import com.thelongtravail.compat.ironsspellbooks.IronsSpellbooksEffectDefaults;
import com.thelongtravail.compat.ironsspellsdelight.IronsSpellsDelightEffectDefaults;
import com.thelongtravail.compat.morepotioneffects.MorePotionEffectsDefaults;
import com.thelongtravail.compat.youkaisfeasts.YoukaisFeastsEffectDefaults;
import com.thelongtravail.compat.youkaishomecoming.YoukaisHomecomingEffectDefaults;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.fml.event.config.ModConfigEvent;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

public final class EffectCompatDefaults {
    public static final String POOL = "travail.witness.positiveEffects";

    record Preset(String namespace, String name, List<String> farReach, List<String> deepValley) {
        List<String> entries(TravailAspect aspect) {
            return switch (aspect) {
                case FAR_REACH -> farReach;
                case DEEP_VALLEY -> deepValley;
                default -> List.of();
            };
        }
    }

    private static final List<TravailAspect> ASPECTS =
            List.of(TravailAspect.FAR_REACH, TravailAspect.DEEP_VALLEY);
    private static final List<Preset> PRESETS =
            List.of(
                    new Preset(
                            "goety",
                            "Goety",
                            GoetyEffectDefaults.farReach(),
                            GoetyEffectDefaults.deepValley()),
                    new Preset(
                            "more_potion_effects",
                            "More Potion Effects",
                            MorePotionEffectsDefaults.farReach(),
                            MorePotionEffectsDefaults.deepValley()),
                    new Preset(
                            "youkaisfeasts",
                            "Youkai's Feasts",
                            YoukaisFeastsEffectDefaults.farReach(),
                            YoukaisFeastsEffectDefaults.deepValley()),
                    new Preset(
                            "youkaishomecoming",
                            "Youkai's Homecoming",
                            YoukaisHomecomingEffectDefaults.farReach(),
                            YoukaisHomecomingEffectDefaults.deepValley()),
                    new Preset(
                            "irons_spells_delight",
                            "Iron's Spells Delight",
                            IronsSpellsDelightEffectDefaults.farReach(),
                            IronsSpellsDelightEffectDefaults.deepValley()),
                    new Preset(
                            "goetyawaken",
                            "Goety Awaken",
                            GoetyAwakenEffectDefaults.farReach(),
                            GoetyAwakenEffectDefaults.deepValley()),
                    new Preset(
                            "cataclysm",
                            "Cataclysm",
                            CataclysmEffectDefaults.farReach(),
                            CataclysmEffectDefaults.deepValley()),
                    new Preset(
                            "enigmaticaddons",
                            "Enigmatic Addons",
                            EnigmaticAddonsEffectDefaults.farReach(),
                            EnigmaticAddonsEffectDefaults.deepValley()),
                    new Preset(
                            "irons_spellbooks",
                            "Iron's Spells and Spellbooks",
                            IronsSpellbooksEffectDefaults.farReach(),
                            IronsSpellbooksEffectDefaults.deepValley()),
                    new Preset(
                            "alexsmobs",
                            "Alex's Mobs",
                            AlexsMobsEffectDefaults.farReach(),
                            AlexsMobsEffectDefaults.deepValley()),
                    new Preset(
                            "attributeslib",
                            "AttributesLib",
                            AttributesLibEffectDefaults.farReach(),
                            AttributesLibEffectDefaults.deepValley()),
                    new Preset(
                            "ars_nouveau",
                            "Ars Nouveau",
                            ArsNouveauEffectDefaults.farReach(),
                            ArsNouveauEffectDefaults.deepValley()));
    private static final Map<TravailAspect, ManagedConfig> LOADED =
            new EnumMap<>(TravailAspect.class);

    private static String marker(Preset preset) {
        return "travail.witness.compatInitialization."
                + preset.namespace()
                + ".positiveEffectsAdded";
    }

    static void define(ForgeConfigSpec.Builder builder, TravailAspect aspect) {
        for (Preset preset : PRESETS) {
            if (preset.entries(aspect).isEmpty()) continue;
            builder.comment(preset.name()).define(marker(preset), false);
        }
    }

    static void loaded(ModConfigEvent.Loading event) {
        if (!(event.getConfig() instanceof ManagedConfig config)) return;
        for (TravailAspect aspect : ASPECTS)
            if (config.getSpec() == TravailConfig.SPECS.get("aspects/" + aspect.id() + ".toml"))
                LOADED.put(aspect, config);
    }

    static void starting(ServerStartingEvent event) {
        var namespaces = new HashSet<String>();
        ForgeRegistries.MOB_EFFECTS.getKeys().forEach(id -> namespaces.add(id.getNamespace()));
        var presets =
                PRESETS.stream().filter(preset -> namespaces.contains(preset.namespace())).toList();
        if (presets.isEmpty()) return;
        Predicate<String> eligible =
                id -> {
                    var key = new ResourceLocation(id);
                    if (!ForgeRegistries.MOB_EFFECTS.containsKey(key)) return false;
                    return ForgeRegistries.MOB_EFFECTS.getValue(key).getCategory()
                            == MobEffectCategory.BENEFICIAL;
                };
        for (TravailAspect aspect : ASPECTS) {
            ManagedConfig file = LOADED.get(aspect);
            if (file == null) continue;
            file.update(
                    config -> {
                        boolean changed = false;
                        for (Preset preset : presets)
                            changed = apply(config, aspect, preset, true, eligible) || changed;
                        return changed;
                    });
        }
    }

    static boolean apply(
            CommentedConfig config,
            TravailAspect aspect,
            Preset preset,
            boolean present,
            Predicate<String> eligible) {
        return WeightedPresetEntries.apply(
                config, POOL, preset.entries(aspect), marker(preset), present, eligible);
    }

    private EffectCompatDefaults() {}
}
