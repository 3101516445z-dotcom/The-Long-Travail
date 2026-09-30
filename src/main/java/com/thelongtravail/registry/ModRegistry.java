package com.thelongtravail.registry;

import com.thelongtravail.TheLongTravail;
import com.thelongtravail.TravailAspect;
import com.thelongtravail.item.RevelationStoneItem;
import com.thelongtravail.item.HomecomingItem;
import com.thelongtravail.item.RenewalItem;
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
    private static final DeferredRegister<MobEffect> EFFECTS = DeferredRegister.create(ForgeRegistries.MOB_EFFECTS, TheLongTravail.MODID);
    private static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, TheLongTravail.MODID);

    public static final RegistryObject<Item> LONG_TRAVAIL = ITEMS.register("the_long_travail", LongTravailItem::new);
    public static final RegistryObject<Item> RENEWAL = ITEMS.register("renewal", RenewalItem::new);
    public static final RegistryObject<Item> HOMECOMING = ITEMS.register("homecoming", HomecomingItem::new);
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
                output.accept(HOMECOMING.get());
                output.accept(RENEWAL.get());
                REVELATION_STONES.forEach(stone -> output.accept(stone.get()));
            })
            .build());

    public static void register(IEventBus bus) {
        ITEMS.register(bus);
        EFFECTS.register(bus);
        TABS.register(bus);
    }

    private ModRegistry() {}
}
