package com.thelongtravail.registry;

import com.thelongtravail.TheLongTravail;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.RangedAttribute;
import net.minecraftforge.event.entity.EntityAttributeModificationEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import net.minecraftforge.fml.ModList;

public final class ModAttributes {
    private static final DeferredRegister<Attribute> ATTRIBUTES =
            DeferredRegister.create(ForgeRegistries.ATTRIBUTES, TheLongTravail.MODID);
    public static final RegistryObject<Attribute> INSIGHT = register("insight", Float.MAX_VALUE);
    public static final RegistryObject<Attribute> CRITICAL_CHANCE = registerPercent("critical_chance", 1);
    public static final RegistryObject<Attribute> OVERLORD = registerPercent("overlord", 1);

    private static RegistryObject<Attribute> register(String name, double maximum) {
        return ATTRIBUTES.register(
                name,
                () ->
                        new RangedAttribute("attribute.name." + TheLongTravail.MODID + "." + name, 0, 0, maximum)
                                .setSyncable(true));
    }

    private static RegistryObject<Attribute> registerPercent(String name, double maximum) {
        return ATTRIBUTES.register(
                name,
                () -> {
                    if (ModList.get().isLoaded("attributeslib")) {
                        try {
                            Class<?> type = Class.forName("dev.shadowsoffire.attributeslib.impl.PercentBasedAttribute");
                            return ((Attribute)
                                            type.getConstructor(String.class, double.class, double.class, double.class)
                                                    .newInstance(
                                                            "attribute.name." + TheLongTravail.MODID + "." + name,
                                                            0D,
                                                            0D,
                                                            maximum))
                                    .setSyncable(true);
                        } catch (ReflectiveOperationException | LinkageError error) {
                            TheLongTravail.LOGGER.warn(
                                    "Apothic Attributes percent formatter unavailable for {}", name, error);
                        }
                    }
                    return new RangedAttribute("attribute.name." + TheLongTravail.MODID + "." + name, 0, 0, maximum)
                            .setSyncable(true);
                });
    }

    public static void register(IEventBus bus) {
        ATTRIBUTES.register(bus);
        bus.addListener(ModAttributes::attach);
    }

    private static void attach(EntityAttributeModificationEvent event) {
        for (var type : event.getTypes()) {
            for (var entry : java.util.List.of(INSIGHT, CRITICAL_CHANCE, OVERLORD)) {
                if (!event.has(type, entry.get())) event.add(type, entry.get());
            }
        }
    }

    private ModAttributes() {}
}
