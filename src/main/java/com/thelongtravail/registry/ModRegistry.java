package com.thelongtravail.registry;

import com.thelongtravail.TheLongTravail;
import com.thelongtravail.TravailAspect;
import com.thelongtravail.abyss.LiquefiedAbundanceRecipe;
import com.thelongtravail.effect.StiffEffect;
import com.thelongtravail.effect.VisualDeprivationEffect;
import com.thelongtravail.entity.WayguideEntity;
import com.thelongtravail.item.ColoringItem;
import com.thelongtravail.item.HomecomingItem;
import com.thelongtravail.item.LongTravailItem;
import com.thelongtravail.item.RenewalItem;
import com.thelongtravail.item.RevelationStoneItem;
import com.thelongtravail.item.WayguideItem;
import com.thelongtravail.menu.ColoringMenu;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModRegistry {
    private static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, TheLongTravail.MODID);
    private static final DeferredRegister<EntityType<?>> ENTITIES =
            DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, TheLongTravail.MODID);
    private static final DeferredRegister<MobEffect> EFFECTS =
            DeferredRegister.create(ForgeRegistries.MOB_EFFECTS, TheLongTravail.MODID);
    private static final DeferredRegister<CreativeModeTab> TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, TheLongTravail.MODID);

    private static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(ForgeRegistries.MENU_TYPES, TheLongTravail.MODID);
    public static final RegistryObject<MenuType<ColoringMenu>> COLORING_MENU =
            MENUS.register(
                    "coloring",
                    () -> new MenuType<>(ColoringMenu::new, FeatureFlags.DEFAULT_FLAGS));
    public static final RegistryObject<Item> ANCIENT_STRANGE_TALES =
            ITEMS.register(
                    "ancient_strange_tales", com.thelongtravail.item.AncientStrangeTalesItem::new);

    public static final RegistryObject<Item> COLORING =
            ITEMS.register("coloring", ColoringItem::new);

    public static final RegistryObject<Item> LONG_TRAVAIL =
            ITEMS.register("the_long_travail", LongTravailItem::new);
    public static final RegistryObject<Item> RENEWAL = ITEMS.register("renewal", RenewalItem::new);
    public static final RegistryObject<Item> HOMECOMING =
            ITEMS.register("homecoming", HomecomingItem::new);
    public static final RegistryObject<Item> WAYGUIDE =
            ITEMS.register("wayguide", WayguideItem::new);
    public static final RegistryObject<Item> SPRING_CORNER =
            ITEMS.register(
                    "spring_corner",
                    () -> new com.thelongtravail.item.FlourishingAccessoryItem(true));
    public static final RegistryObject<Item> MISTLETOE =
            ITEMS.register("mistletoe", com.thelongtravail.item.MistletoeItem::new);
    public static final RegistryObject<Item> EVERLASTING_FLOWER =
            ITEMS.register(
                    "everlasting_flower", com.thelongtravail.item.EverlastingFlowerItem::new);
    public static final RegistryObject<Item> IMMORTAL_BUD =
            ITEMS.register("immortal_bud", com.thelongtravail.item.ImmortalBudItem::new);
    public static final RegistryObject<MobEffect> MISTLETOE_EFFECT =
            EFFECTS.register("mistletoe", com.thelongtravail.effect.MistletoeEffect::new);
    public static final RegistryObject<Item> AFFECTION =
            ITEMS.register(
                    "affection", () -> new com.thelongtravail.item.FlourishingAccessoryItem(false));
    private static final DeferredRegister<RecipeSerializer<?>> RECIPES =
            DeferredRegister.create(ForgeRegistries.RECIPE_SERIALIZERS, TheLongTravail.MODID);
    public static final RegistryObject<RecipeSerializer<LiquefiedAbundanceRecipe>>
            LIQUEFIED_ABUNDANCE_RECIPE =
                    RECIPES.register(
                            "liquefied_abundance", LiquefiedAbundanceRecipe.Serializer::new);
    public static final RegistryObject<
                    RecipeSerializer<com.thelongtravail.flourishing.SpringRecipes.Crown>>
            SPRING_CROWN_RECIPE =
                    RECIPES.register(
                            "spring_crown",
                            com.thelongtravail.flourishing.SpringRecipes.CrownSerializer::new);
    public static final RegistryObject<
                    net.minecraft.world.item.crafting.SimpleCraftingRecipeSerializer<
                            com.thelongtravail.flourishing.SpringRecipes.Change>>
            SPRING_CHANGE_RECIPE =
                    RECIPES.register(
                            "spring_change",
                            () ->
                                    new net.minecraft.world.item.crafting
                                            .SimpleCraftingRecipeSerializer<>(
                                            com.thelongtravail.flourishing.SpringRecipes.Change
                                                    ::new));
    public static final RegistryObject<
                    net.minecraft.world.item.crafting.SimpleCraftingRecipeSerializer<
                            com.thelongtravail.flourishing.ImmortalBudRecipe>>
            IMMORTAL_BUD_APPLY =
                    RECIPES.register(
                            "immortal_bud_apply",
                            () ->
                                    new net.minecraft.world.item.crafting
                                            .SimpleCraftingRecipeSerializer<>(
                                            com.thelongtravail.flourishing.ImmortalBudRecipe::new));
    public static final RegistryObject<Item> LIQUEFIED_ABUNDANCE =
            ITEMS.register(
                    "liquefied_abundance", com.thelongtravail.item.LiquefiedAbundanceItem::new);
    public static final RegistryObject<Item> PERFECT_FLUID =
            ITEMS.register("perfect_fluid", () -> new com.thelongtravail.item.AbyssItem(false));
    public static final RegistryObject<Item> MIDNIGHT_LAFITE =
            ITEMS.register("midnight_lafite", com.thelongtravail.item.AbyssItem.Lafite::new);
    public static final RegistryObject<
                    net.minecraft.world.item.crafting.SimpleCraftingRecipeSerializer<
                            com.thelongtravail.abyss.PerfectFluidRecipe>>
            PERFECT_FLUID_COPY =
                    RECIPES.register(
                            "perfect_fluid_copy",
                            () ->
                                    new net.minecraft.world.item.crafting
                                            .SimpleCraftingRecipeSerializer<>(
                                            com.thelongtravail.abyss.PerfectFluidRecipe::new));
    public static final RegistryObject<Item> EKI =
            ITEMS.register("eki", com.thelongtravail.item.EkiItem::new);
    public static final RegistryObject<Item> TOKAIDO =
            ITEMS.register("tokaido", com.thelongtravail.item.TokaidoItem::new);
    public static final RegistryObject<Item> FREE_AND_EASY_WANDERING =
            ITEMS.register(
                    "free_and_easy_wandering",
                    () -> new com.thelongtravail.item.BoundlessAccessoryItem(true));
    public static final RegistryObject<Item> LITTLE_PRINCE =
            ITEMS.register(
                    "little_prince", () -> new com.thelongtravail.item.BoundlessRelicItem(true));
    public static final RegistryObject<
                    RecipeSerializer<com.thelongtravail.boundless.EnchantmentTransferRecipe>>
            ENCHANTMENT_TRANSFER =
                    RECIPES.register(
                            "enchantment_transfer",
                            () ->
                                    new net.minecraft.world.item.crafting
                                            .SimpleCraftingRecipeSerializer<>(
                                            com.thelongtravail.boundless.EnchantmentTransferRecipe
                                                    ::new));

    public static final RegistryObject<Item> LORENTZ_FANTASY =
            ITEMS.register(
                    "lorentz_fantasy", () -> new com.thelongtravail.item.BoundlessRelicItem(false));
    public static final RegistryObject<Item> STAR_VOICE =
            ITEMS.register(
                    "star_voice", () -> new com.thelongtravail.item.BoundlessAccessoryItem(false));
    public static final RegistryObject<Item> SWORD_AND_LANTERN =
            ITEMS.register("sword_and_lantern", com.thelongtravail.item.SwordLanternItem::new);
    public static final RegistryObject<Item> THOUSAND_YEARS =
            ITEMS.register(
                    "a_thousand_years_later",
                    () -> new com.thelongtravail.item.UnderworldAccessoryItem(true));
    public static final RegistryObject<Item> BOOK_OF_DEAD =
            ITEMS.register(
                    "book_of_the_dead",
                    () -> new com.thelongtravail.item.UnderworldAccessoryItem(false));
    public static final RegistryObject<Item> DEMON_KING =
            ITEMS.register(
                    "demon_king_of_the_sixth_heaven",
                    () -> new com.thelongtravail.item.UnderworldRelicItem(false));
    public static final RegistryObject<Item> SOUL_BANNER =
            ITEMS.register(
                    "myriad_souls_banner",
                    () -> new com.thelongtravail.item.UnderworldRelicItem(true));
    public static final RegistryObject<Item> PHOSPHOPHYLLITE =
            ITEMS.register("phosphophyllite", com.thelongtravail.item.PhosphophylliteItem::new);
    public static final RegistryObject<Item> UNDERGROUND_VELVET =
            ITEMS.register(
                    "underground_velvet", com.thelongtravail.item.UndergroundVelvetItem::new);
    public static final RegistryObject<Item> HIGH_FREQUENCY_TREMOR =
            ITEMS.register(
                    "high_frequency_tremor", com.thelongtravail.item.HighFrequencyTremorItem::new);
    public static final RegistryObject<
                    RecipeSerializer<com.thelongtravail.valley.NbtInheritingShapedRecipe>>
            TREMOR_RECIPE =
                    RECIPES.register(
                            "nbt_inheriting_shaped",
                            com.thelongtravail.valley.NbtInheritingShapedRecipe.Serializer::new);
    public static final RegistryObject<Item> AZRAEL =
            ITEMS.register("azrael", com.thelongtravail.item.AzraelItem::new);
    public static final RegistryObject<Item> DON_QUIXOTE =
            ITEMS.register("don_quixote", com.thelongtravail.item.DonQuixoteItem::new);
    public static final RegistryObject<Item> TOWER_OF_BABEL =
            ITEMS.register("tower_of_babel", com.thelongtravail.item.BabelItem::new);
    public static final RegistryObject<Item> GOLDEN_AGE =
            ITEMS.register(
                    "golden_age", () -> new com.thelongtravail.item.FarReachAccessoryItem(true));
    public static final RegistryObject<Item> SUN_CHASER =
            ITEMS.register("sun_chaser", com.thelongtravail.item.SunChaserItem::new);
    public static final RegistryObject<Item> KAGOTSURUBE =
            ITEMS.register("kagotsurube", com.thelongtravail.item.KagotsurubeItem::new);
    public static final RegistryObject<Item> ICARUS =
            ITEMS.register(
                    "icarus", () -> new com.thelongtravail.item.FarReachAccessoryItem(false));
    public static final RegistryObject<EntityType<WayguideEntity>> WAYGUIDE_ENTITY =
            ENTITIES.register(
                    "wayguide",
                    () ->
                            EntityType.Builder.<WayguideEntity>of(
                                            WayguideEntity::new, MobCategory.MISC)
                                    .sized(0.25F, 0.25F)
                                    .clientTrackingRange(8)
                                    .updateInterval(5)
                                    .build(TheLongTravail.MODID + ":wayguide"));
    public static final java.util.List<RegistryObject<Item>> REVELATION_STONES =
            java.util.Arrays.stream(TravailAspect.values())
                    .map(
                            aspect ->
                                    ITEMS.<Item>register(
                                            aspect.id() + "_stone",
                                            () -> new RevelationStoneItem(aspect)))
                    .toList();
    public static final java.util.List<RegistryObject<Item>> ESSENCES =
            java.util.Arrays.stream(TravailAspect.values())
                    .map(
                            aspect ->
                                    ITEMS.<Item>register(
                                            aspect.id() + "_essence",
                                            () ->
                                                    new com.thelongtravail.item.AspectEssenceItem(
                                                            aspect)))
                    .toList();
    public static final RegistryObject<MobEffect> STIFF =
            EFFECTS.register("stiff", StiffEffect::new);
    public static final RegistryObject<MobEffect> VISUAL_DEPRIVATION =
            EFFECTS.register("visual_deprivation", VisualDeprivationEffect::new);
    public static final RegistryObject<CreativeModeTab> TAB =
            TABS.register(
                    "main",
                    () ->
                            CreativeModeTab.builder()
                                    .title(Component.translatable("itemGroup.the_long_travail"))
                                    .icon(() -> new ItemStack(LONG_TRAVAIL.get()))
                                    .displayItems(
                                            (parameters, output) -> {
                                                output.accept(LONG_TRAVAIL.get());
                                                output.accept(RENEWAL.get());
                                                output.accept(HOMECOMING.get());
                                                output.accept(WAYGUIDE.get());
                                                output.accept(COLORING.get());
                                                for (TravailAspect aspect :
                                                        TravailAspect.values()) {
                                                    output.accept(
                                                            REVELATION_STONES
                                                                    .get(aspect.ordinal())
                                                                    .get());
                                                    output.accept(
                                                            ESSENCES.get(aspect.ordinal()).get());
                                                    var items =
                                                            switch (aspect) {
                                                                case FLOURISHING ->
                                                                        java.util.List.of(
                                                                                SPRING_CORNER,
                                                                                AFFECTION,
                                                                                MISTLETOE,
                                                                                EVERLASTING_FLOWER,
                                                                                IMMORTAL_BUD);
                                                                case ABYSS ->
                                                                        java.util.List.of(
                                                                                EKI,
                                                                                TOKAIDO,
                                                                                PERFECT_FLUID,
                                                                                MIDNIGHT_LAFITE,
                                                                                LIQUEFIED_ABUNDANCE);
                                                                case FAR_REACH ->
                                                                        java.util.List.of(
                                                                                GOLDEN_AGE,
                                                                                ICARUS,
                                                                                DON_QUIXOTE,
                                                                                TOWER_OF_BABEL,
                                                                                SUN_CHASER);
                                                                case DEEP_VALLEY ->
                                                                        java.util.List.of(
                                                                                AZRAEL,
                                                                                SWORD_AND_LANTERN,
                                                                                PHOSPHOPHYLLITE,
                                                                                UNDERGROUND_VELVET,
                                                                                HIGH_FREQUENCY_TREMOR);
                                                                case UNDERWORLD ->
                                                                        java.util.List.of(
                                                                                THOUSAND_YEARS,
                                                                                BOOK_OF_DEAD,
                                                                                DEMON_KING,
                                                                                SOUL_BANNER,
                                                                                KAGOTSURUBE);
                                                                case BOUNDLESS ->
                                                                        java.util.List.of(
                                                                                FREE_AND_EASY_WANDERING,
                                                                                STAR_VOICE,
                                                                                LITTLE_PRINCE,
                                                                                LORENTZ_FANTASY,
                                                                                ANCIENT_STRANGE_TALES);
                                                            };
                                                    items.forEach(
                                                            item -> output.accept(item.get()));
                                                }
                                            })
                                    .build());

    public static void register(IEventBus bus) {
        ModAttributes.register(bus);
        com.thelongtravail.valley.LanternLight.register(bus);
        ITEMS.register(bus);
        MENUS.register(bus);
        RECIPES.register(bus);
        ENTITIES.register(bus);
        EFFECTS.register(bus);
        TABS.register(bus);
    }

    private ModRegistry() {}
}
