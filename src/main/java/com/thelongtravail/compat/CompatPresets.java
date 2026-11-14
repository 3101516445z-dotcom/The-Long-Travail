package com.thelongtravail.compat;

import com.thelongtravail.TravailAspect;
import com.thelongtravail.compat.alexscaves.AlexsCavesBiomeDefaults;
import com.thelongtravail.compat.alexsmobs.AlexsMobsEffectDefaults;
import com.thelongtravail.compat.apprenticecodex.ApprenticeCodexRewardDefaults;
import com.thelongtravail.compat.aquamirae.AquamiraeStructureDefaults;
import com.thelongtravail.compat.arselemental.ArsElementalFishingDefaults;
import com.thelongtravail.compat.arsnouveau.ArsNouveauEffectDefaults;
import com.thelongtravail.compat.arsnouveau.ArsNouveauFishingDefaults;
import com.thelongtravail.compat.arsnouveau.ArsNouveauStructureDefaults;
import com.thelongtravail.compat.attributeslib.AttributesLibEffectDefaults;
import com.thelongtravail.compat.cataclysm.CataclysmEffectDefaults;
import com.thelongtravail.compat.cataclysm.CataclysmStructureDefaults;
import com.thelongtravail.compat.celestialcore.CelestialCoreRewardDefaults;
import com.thelongtravail.compat.celestialenchantments.CelestialEnchantmentsGoldenAgeDefaults;
import com.thelongtravail.compat.curseofpandora.CurseOfPandoraRewardDefaults;
import com.thelongtravail.compat.eeeabsmobs.EEEABMobsStructureDefaults;
import com.thelongtravail.compat.enigmaticaddons.EnigmaticAddonsEffectDefaults;
import com.thelongtravail.compat.enigmaticaddons.EnigmaticAddonsRewardDefaults;
import com.thelongtravail.compat.enigmaticdelicacy.EnigmaticDelicacyGoldenAgeDefaults;
import com.thelongtravail.compat.enigmaticdelicacy.EnigmaticDelicacyRewardDefaults;
import com.thelongtravail.compat.enigmaticlegacy.EnigmaticLegacyGoldenAgeDefaults;
import com.thelongtravail.compat.enigmaticlegacy.EnigmaticLegacyRewardDefaults;
import com.thelongtravail.compat.goety.GoetyEffectDefaults;
import com.thelongtravail.compat.goety.GoetyStructureDefaults;
import com.thelongtravail.compat.goetyawaken.GoetyAwakenEffectDefaults;
import com.thelongtravail.compat.goetyawaken.GoetyAwakenStructureDefaults;
import com.thelongtravail.compat.incendium.IncendiumBiomeDefaults;
import com.thelongtravail.compat.incendium.IncendiumStructureDefaults;
import com.thelongtravail.compat.ironsspellbooks.IronsSpellbooksEffectDefaults;
import com.thelongtravail.compat.ironsspellbooks.IronsSpellbooksRewardDefaults;
import com.thelongtravail.compat.ironsspellbooks.IronsSpellbooksStructureDefaults;
import com.thelongtravail.compat.ironsspellsdelight.IronsSpellsDelightEffectDefaults;
import com.thelongtravail.compat.l2complements.L2ComplementsGoldenAgeDefaults;
import com.thelongtravail.compat.l2complements.L2ComplementsRewardDefaults;
import com.thelongtravail.compat.legendarymonsters.LegendaryMonstersStructureDefaults;
import com.thelongtravail.compat.maid.MaidFishingDefaults;
import com.thelongtravail.compat.maidspell.MaidSpellStructureDefaults;
import com.thelongtravail.compat.morepotioneffects.MorePotionEffectsDefaults;
import com.thelongtravail.compat.mowziesmobs.MowziesMobsStructureDefaults;
import com.thelongtravail.compat.terralith.TerralithBiomeDefaults;
import com.thelongtravail.compat.terralith.TerralithStructureDefaults;
import com.thelongtravail.compat.townsandtowers.TownsAndTowersStructureDefaults;
import com.thelongtravail.compat.traveloptics.TravelOpticsStructureDefaults;
import com.thelongtravail.compat.youkaisfeasts.YoukaisFeastsEffectDefaults;
import com.thelongtravail.compat.youkaisfeasts.YoukaisFeastsFishingDefaults;
import com.thelongtravail.compat.youkaishomecoming.YoukaisHomecomingEffectDefaults;
import com.thelongtravail.compat.youkaishomecoming.YoukaisHomecomingFishingDefaults;

import java.util.List;
import java.util.function.Function;

// 仅装配第三方预设，顺序与初始化标记由原有配置契约保持。
public final class CompatPresets {
    @FunctionalInterface
    public interface SpatialFactory<T> {
        T create(
                String namespace,
                String name,
                String marker,
                Function<TravailAspect, List<String>> entries);
    }

    @FunctionalInterface
    public interface PairFactory<T> {
        T create(String namespace, String name, List<String> first, List<String> second);
    }

    @FunctionalInterface
    public interface EntriesFactory<T> {
        T create(String namespace, String name, List<String> entries);
    }

    @FunctionalInterface
    public interface GoldenFactory<T, P> {
        T create(String mod, String name, P pool, List<String> entries);
    }

    public record FluidRule(String mod, List<String> items, String flag) {}

    public static List<FluidRule> fluid() {
        return com.thelongtravail.compat.maid.MaidFluidDefaults.rules().stream()
                .map(rule -> new FluidRule(rule.mod(), rule.items(), rule.flag()))
                .toList();
    }

    public static <T> List<T> biomes(SpatialFactory<T> factory) {
        return List.of(
                factory.create(
                        "terralith",
                        "Terralith",
                        "presets.exploration.terralithBiomesAdded",
                        TerralithBiomeDefaults::biomes),
                factory.create(
                        "incendium",
                        "Incendium",
                        "presets.exploration.incendiumBiomesAdded",
                        IncendiumBiomeDefaults::biomes),
                factory.create(
                        "alexscaves",
                        "Alex's Caves",
                        "presets.exploration.alexsCavesBiomesAdded",
                        AlexsCavesBiomeDefaults::biomes));
    }

    public static <T> List<T> structures(SpatialFactory<T> factory) {
        return List.of(
                factory.create(
                        "eeeabsmobs",
                        "EEEAB's Mobs",
                        "presets.exploration.eeeabsmobsStructuresAdded",
                        EEEABMobsStructureDefaults::structures),
                factory.create(
                        "legendary_monsters",
                        "Legendary Monsters",
                        "presets.exploration.legendarymonstersStructuresAdded",
                        LegendaryMonstersStructureDefaults::structures),
                factory.create(
                        "cataclysm",
                        "Cataclysm",
                        "presets.exploration.cataclysmStructuresAdded",
                        CataclysmStructureDefaults::structures),
                factory.create(
                        "terralith",
                        "Terralith",
                        "presets.exploration.terralithStructuresAdded",
                        TerralithStructureDefaults::structures),
                factory.create(
                        "goetyawaken",
                        "Goety Awaken",
                        "presets.exploration.goetyAwakenStructuresAdded",
                        GoetyAwakenStructureDefaults::structures),
                factory.create(
                        "towns_and_towers",
                        "Towns and Towers",
                        "presets.exploration.townsAndTowersStructuresAdded",
                        TownsAndTowersStructureDefaults::structures),
                factory.create(
                        "goety",
                        "Goety",
                        "presets.exploration.goetyStructuresAdded",
                        GoetyStructureDefaults::structures),
                factory.create(
                        "incendium",
                        "Incendium",
                        "presets.exploration.incendiumStructuresAdded",
                        IncendiumStructureDefaults::structures),
                factory.create(
                        "irons_spellbooks",
                        "Iron's Spells and Spellbooks",
                        "presets.exploration.ironsSpellbooksStructuresAdded",
                        IronsSpellbooksStructureDefaults::structures),
                factory.create(
                        "touhou_little_maid_spell",
                        "Touhou Little Maid: Spell",
                        "presets.exploration.maidSpellStructuresAdded",
                        MaidSpellStructureDefaults::structures),
                factory.create(
                        "ars_nouveau",
                        "Ars Nouveau",
                        "presets.exploration.arsNouveauStructuresAdded",
                        ArsNouveauStructureDefaults::structures),
                factory.create(
                        "aquamirae",
                        "Aquamirae",
                        "presets.exploration.aquamiraeStructuresAdded",
                        AquamiraeStructureDefaults::structures),
                factory.create(
                        "mowziesmobs",
                        "Mowzie's Mobs",
                        "presets.exploration.mowziesMobsStructuresAdded",
                        MowziesMobsStructureDefaults::structures),
                factory.create(
                        "traveloptics",
                        "T.O Magic and Extras",
                        "presets.exploration.travelOpticsStructuresAdded",
                        TravelOpticsStructureDefaults::structures));
    }

    public static <T> List<T> effects(PairFactory<T> factory) {
        return List.of(
                factory.create(
                        "goety",
                        "Goety",
                        GoetyEffectDefaults.farReach(),
                        GoetyEffectDefaults.deepValley()),
                factory.create(
                        "more_potion_effects",
                        "More Potion Effects",
                        MorePotionEffectsDefaults.farReach(),
                        MorePotionEffectsDefaults.deepValley()),
                factory.create(
                        "youkaisfeasts",
                        "Youkai's Feasts",
                        YoukaisFeastsEffectDefaults.farReach(),
                        YoukaisFeastsEffectDefaults.deepValley()),
                factory.create(
                        "youkaishomecoming",
                        "Youkai's Homecoming",
                        YoukaisHomecomingEffectDefaults.farReach(),
                        YoukaisHomecomingEffectDefaults.deepValley()),
                factory.create(
                        "irons_spells_delight",
                        "Iron's Spells Delight",
                        IronsSpellsDelightEffectDefaults.farReach(),
                        IronsSpellsDelightEffectDefaults.deepValley()),
                factory.create(
                        "goetyawaken",
                        "Goety Awaken",
                        GoetyAwakenEffectDefaults.farReach(),
                        GoetyAwakenEffectDefaults.deepValley()),
                factory.create(
                        "cataclysm",
                        "Cataclysm",
                        CataclysmEffectDefaults.farReach(),
                        CataclysmEffectDefaults.deepValley()),
                factory.create(
                        "enigmaticaddons",
                        "Enigmatic Addons",
                        EnigmaticAddonsEffectDefaults.farReach(),
                        EnigmaticAddonsEffectDefaults.deepValley()),
                factory.create(
                        "irons_spellbooks",
                        "Iron's Spells and Spellbooks",
                        IronsSpellbooksEffectDefaults.farReach(),
                        IronsSpellbooksEffectDefaults.deepValley()),
                factory.create(
                        "alexsmobs",
                        "Alex's Mobs",
                        AlexsMobsEffectDefaults.farReach(),
                        AlexsMobsEffectDefaults.deepValley()),
                factory.create(
                        "attributeslib",
                        "AttributesLib",
                        AttributesLibEffectDefaults.farReach(),
                        AttributesLibEffectDefaults.deepValley()),
                factory.create(
                        "ars_nouveau",
                        "Ars Nouveau",
                        ArsNouveauEffectDefaults.farReach(),
                        ArsNouveauEffectDefaults.deepValley()));
    }

    public static <T> List<T> rewards(PairFactory<T> factory) {
        return List.of(
                factory.create(
                        "l2complements",
                        "L2 Complements",
                        L2ComplementsRewardDefaults.fishingItems(),
                        L2ComplementsRewardDefaults.phantomDrops()),
                factory.create(
                        "curseofpandora",
                        "Curse of Pandora",
                        CurseOfPandoraRewardDefaults.fishingItems(),
                        CurseOfPandoraRewardDefaults.phantomDrops()),
                factory.create(
                        "celestial_core",
                        "Celestial Core",
                        CelestialCoreRewardDefaults.fishingItems(),
                        CelestialCoreRewardDefaults.phantomDrops()),
                factory.create(
                        "irons_spellbooks",
                        "Iron's Spells and Spellbooks",
                        IronsSpellbooksRewardDefaults.fishingItems(),
                        IronsSpellbooksRewardDefaults.phantomDrops()),
                factory.create(
                        "apprenticecodex",
                        "Apprentice Codex",
                        ApprenticeCodexRewardDefaults.fishingItems(),
                        ApprenticeCodexRewardDefaults.phantomDrops()),
                factory.create(
                        "enigmaticlegacy",
                        "Enigmatic Legacy",
                        EnigmaticLegacyRewardDefaults.fishingItems(),
                        EnigmaticLegacyRewardDefaults.phantomDrops()),
                factory.create(
                        "enigmaticaddons",
                        "Enigmatic Addons",
                        EnigmaticAddonsRewardDefaults.fishingItems(),
                        EnigmaticAddonsRewardDefaults.phantomDrops()),
                factory.create(
                        "enigmaticdelicacy",
                        "Enigmatic Delicacy",
                        EnigmaticDelicacyRewardDefaults.fishingItems(),
                        EnigmaticDelicacyRewardDefaults.phantomDrops()));
    }

    public static <T> List<T> fishing(EntriesFactory<T> factory) {
        return List.of(
                factory.create(
                        "youkaisfeasts",
                        "Youkai's Feasts",
                        YoukaisFeastsFishingDefaults.entities()),
                factory.create(
                        "youkaishomecoming",
                        "Youkai's Homecoming",
                        YoukaisHomecomingFishingDefaults.entities()),
                factory.create("ars_nouveau", "Ars Nouveau", ArsNouveauFishingDefaults.entities()),
                factory.create(
                        "ars_elemental", "Ars Elemental", ArsElementalFishingDefaults.entities()),
                factory.create(
                        "touhou_little_maid",
                        "Touhou Little Maid",
                        MaidFishingDefaults.entities()));
    }

    public static <T, P> List<T> goldenAge(GoldenFactory<T, P> factory, P food, P enchantment) {
        return List.of(
                factory.create(
                        "enigmaticlegacy",
                        "Enigmatic Legacy",
                        food,
                        EnigmaticLegacyGoldenAgeDefaults.foodBlacklist()),
                factory.create(
                        "enigmaticdelicacy",
                        "Enigmatic Delicacy",
                        food,
                        EnigmaticDelicacyGoldenAgeDefaults.foodBlacklist()),
                factory.create(
                        "l2complements",
                        "L2 Complements",
                        enchantment,
                        L2ComplementsGoldenAgeDefaults.enchantmentBlacklist()),
                factory.create(
                        "celestial_enchantments",
                        "Celestial Enchantments",
                        enchantment,
                        CelestialEnchantmentsGoldenAgeDefaults.enchantmentBlacklist()));
    }

    private CompatPresets() {}
}
