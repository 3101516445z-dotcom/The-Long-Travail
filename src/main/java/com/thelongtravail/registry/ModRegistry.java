package com.thelongtravail.registry;

import com.thelongtravail.TheLongTravail;
import com.thelongtravail.TravailAspect;
import com.thelongtravail.item.RevelationStoneItem;
import com.thelongtravail.item.HomecomingItem;
import com.thelongtravail.item.RenewalItem;
import com.thelongtravail.item.WayguideItem;
import com.thelongtravail.entity.WayguideEntity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import com.thelongtravail.effect.StiffEffect;
import com.thelongtravail.effect.VisualDeprivationEffect;
import com.thelongtravail.item.LongTravailItem;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModRegistry {
    private static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, TheLongTravail.MODID);
    private static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, TheLongTravail.MODID);
    private static final DeferredRegister<MobEffect> EFFECTS = DeferredRegister.create(ForgeRegistries.MOB_EFFECTS, TheLongTravail.MODID);
    private static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, TheLongTravail.MODID);

    public static final RegistryObject<Item> LONG_TRAVAIL = ITEMS.register("the_long_travail", LongTravailItem::new);
    public static final RegistryObject<Item> RENEWAL = ITEMS.register("renewal", RenewalItem::new);
    public static final RegistryObject<Item> HOMECOMING = ITEMS.register("homecoming", HomecomingItem::new);
    public static final RegistryObject<Item> WAYGUIDE = ITEMS.register("wayguide", WayguideItem::new);
    public static final RegistryObject<Item> SPRING_GAME = ITEMS.register("spring_game", () -> new com.thelongtravail.item.FlourishingAccessoryItem(true));
    public static final RegistryObject<Item> AFFECTION = ITEMS.register("affection", () -> new com.thelongtravail.item.FlourishingAccessoryItem(false));
    private static final DeferredRegister<net.minecraft.world.item.crafting.RecipeSerializer<?>> RECIPES = DeferredRegister.create(ForgeRegistries.RECIPE_SERIALIZERS, TheLongTravail.MODID);
    public static final RegistryObject<net.minecraft.world.item.crafting.RecipeSerializer<com.thelongtravail.flourishing.SpringRecipes.Crown>> SPRING_CROWN_RECIPE = RECIPES.register("spring_crown", com.thelongtravail.flourishing.SpringRecipes.CrownSerializer::new);
    public static final RegistryObject<net.minecraft.world.item.crafting.SimpleCraftingRecipeSerializer<com.thelongtravail.flourishing.SpringRecipes.Change>> SPRING_CHANGE_RECIPE = RECIPES.register("spring_change", () -> new net.minecraft.world.item.crafting.SimpleCraftingRecipeSerializer<>(com.thelongtravail.flourishing.SpringRecipes.Change::new));
    public static final RegistryObject<Item> EKI = ITEMS.register("eki", com.thelongtravail.item.EkiItem::new);
    public static final RegistryObject<Item> TOKAIDO = ITEMS.register("tokaido", com.thelongtravail.item.TokaidoItem::new);
    public static final RegistryObject<Item> DAYDREAM = ITEMS.register("daydream", () -> new com.thelongtravail.item.BoundlessAccessoryItem(true));
    public static final RegistryObject<Item> STAR_VOICE = ITEMS.register("star_voice", () -> new com.thelongtravail.item.BoundlessAccessoryItem(false));
    public static final RegistryObject<Item> SWORD_AND_LANTERN = ITEMS.register("sword_and_lantern", com.thelongtravail.item.SwordLanternItem::new);
    public static final RegistryObject<Item> THOUSAND_YEARS = ITEMS.register("a_thousand_years_later", () -> new com.thelongtravail.item.UnderworldAccessoryItem(true));
    public static final RegistryObject<Item> BOOK_OF_DEAD = ITEMS.register("book_of_the_dead", () -> new com.thelongtravail.item.UnderworldAccessoryItem(false));
    public static final RegistryObject<Item> AZRAEL = ITEMS.register("azrael", com.thelongtravail.item.AzraelItem::new);
    public static final RegistryObject<Item> GOLDEN_AGE = ITEMS.register("golden_age", () -> new com.thelongtravail.item.FarReachAccessoryItem(true));
    public static final RegistryObject<Item> ICARUS = ITEMS.register("icarus", () -> new com.thelongtravail.item.FarReachAccessoryItem(false));
    public static final RegistryObject<EntityType<WayguideEntity>> WAYGUIDE_ENTITY = ENTITIES.register("wayguide",
            () -> EntityType.Builder.<WayguideEntity>of(WayguideEntity::new, MobCategory.MISC)
                    .sized(0.25F, 0.25F).clientTrackingRange(8).updateInterval(5).build(TheLongTravail.MODID + ":wayguide"));
    public static final java.util.List<RegistryObject<Item>> REVELATION_STONES = java.util.Arrays.stream(TravailAspect.values())
            .map(aspect -> ITEMS.<Item>register(aspect.id() + "_stone", () -> new RevelationStoneItem(aspect))).toList();
    public static final RegistryObject<MobEffect> STIFF = EFFECTS.register("stiff", StiffEffect::new);
    public static final RegistryObject<MobEffect> VISUAL_DEPRIVATION =
            EFFECTS.register("visual_deprivation", VisualDeprivationEffect::new);
    public static final RegistryObject<CreativeModeTab> TAB = TABS.register("main", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.the_long_travail"))
            .icon(() -> new ItemStack(LONG_TRAVAIL.get()))
            .displayItems((parameters, output) -> {
                output.accept(LONG_TRAVAIL.get());
                output.accept(RENEWAL.get());
                output.accept(HOMECOMING.get());
                output.accept(WAYGUIDE.get());
                REVELATION_STONES.forEach(stone -> output.accept(stone.get()));
                // 与苦旅书页共用主题顺序；JEI 的 CREATIVE_MENU 排序也使用此列表。
                for (TravailAspect aspect : TravailAspect.values()) {
                    var items = switch (aspect) {
                        case FLOURISHING -> java.util.List.of(SPRING_GAME, AFFECTION);
                        case ABYSS -> java.util.List.of(EKI, TOKAIDO);
                        case FAR_REACH -> java.util.List.of(GOLDEN_AGE, ICARUS);
                        case DEEP_VALLEY -> java.util.List.of(AZRAEL, SWORD_AND_LANTERN);
                        case UNDERWORLD -> java.util.List.of(THOUSAND_YEARS, BOOK_OF_DEAD);
                        case BOUNDLESS -> java.util.List.of(DAYDREAM, STAR_VOICE);
                    };
                    items.forEach(item -> output.accept(item.get()));
                }
            })
            .build());

    public static void register(IEventBus bus) {
        com.thelongtravail.valley.LanternLight.register(bus);
        ITEMS.register(bus);
        RECIPES.register(bus);
        ENTITIES.register(bus);
        EFFECTS.register(bus);
        TABS.register(bus);
    }

    private ModRegistry() {}
}
