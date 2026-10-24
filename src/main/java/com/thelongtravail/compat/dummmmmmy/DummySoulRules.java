package com.thelongtravail.compat.dummmmmmy;

import net.minecraft.resources.ResourceLocation;

public final class DummySoulRules {
    public static boolean excludesSoulCapture(ResourceLocation entityType) {
        return entityType != null && entityType.getNamespace().equals("dummmmmmy");
    }

    private DummySoulRules() {}
}
