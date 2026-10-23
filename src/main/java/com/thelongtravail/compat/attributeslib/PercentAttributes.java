package com.thelongtravail.compat.attributeslib;

import com.thelongtravail.TheLongTravail;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.RangedAttribute;
import net.minecraftforge.fml.ModList;

public final class PercentAttributes {
    public static Attribute create(String name, double maximum) {
        if (ModList.get().isLoaded("attributeslib")) {
            try {
                Class<?> type =
                        Class.forName(
                                "dev.shadowsoffire.attributeslib.impl.PercentBasedAttribute");
                return ((Attribute)
                                type.getConstructor(
                                                String.class,
                                                double.class,
                                                double.class,
                                                double.class)
                                        .newInstance(
                                                "attribute.name."
                                                        + TheLongTravail.MODID
                                                        + "."
                                                        + name,
                                                0D,
                                                0D,
                                                maximum))
                        .setSyncable(true);
            } catch (ReflectiveOperationException | LinkageError error) {
                TheLongTravail.LOGGER.warn(
                        "Apothic Attributes percent formatter unavailable for {}",
                        name,
                        error);
            }
        }
        return new RangedAttribute(
                        "attribute.name." + TheLongTravail.MODID + "." + name,
                        0,
                        0,
                        maximum)
                .setSyncable(true);
    }

    private PercentAttributes() {}
}
