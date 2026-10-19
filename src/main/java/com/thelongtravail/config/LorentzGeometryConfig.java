package com.thelongtravail.config;

import net.minecraftforge.common.ForgeConfigSpec;
import java.util.List;

public final class LorentzGeometryConfig {
    public static ForgeConfigSpec.ConfigValue<List<? extends String>> FIX;

    public static void define(ForgeConfigSpec.Builder b) {
        FIX =
                b.comment(
                                "洛伦兹幻想：对列表中碰撞箱异常放大的实体，使用注册时的高度判定爆头。\nLorentz Fantasy: use the registered height to determine headshots for listed entities with abnormally enlarged hitboxes.")
                        .defineListAllowEmpty(
                                "items.boundless.lorentz.headshotHitboxFixEntities",
                                List.of(),
                                v ->
                                        v instanceof String s
                                                && net.minecraft.resources.ResourceLocation.tryParse(s) != null);
    }

    private LorentzGeometryConfig() {}
}
