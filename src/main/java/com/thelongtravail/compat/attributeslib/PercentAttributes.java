package com.thelongtravail.compat.attributeslib;

import com.thelongtravail.TheLongTravail;

import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.RangedAttribute;
import net.minecraftforge.fml.ModList;

public final class PercentAttributes {
    public static Attribute create(String name, double maximum) {
        if (ModList.get().isLoaded("attributeslib")) {
            try {
                return com.thelongtravail.compat.DirectCalls.call(
                        () -> Direct.create(name, maximum));
            } catch (ReflectiveOperationException | LinkageError error) {
                TheLongTravail.LOGGER.warn(
                        "Apothic Attributes percent formatter unavailable for {}", name, error);
            }
        }
        return new RangedAttribute(
                        "attribute.name." + TheLongTravail.MODID + "." + name, 0, 0, maximum)
                .setSyncable(true);
    }

    private static final class Direct {
        static Attribute create(String name, double maximum) {
            return new dev.shadowsoffire.attributeslib.impl.PercentBasedAttribute(
                            "attribute.name." + TheLongTravail.MODID + "." + name, 0D, 0D, maximum)
                    .setSyncable(true);
        }
    }

    private PercentAttributes() {}
}
