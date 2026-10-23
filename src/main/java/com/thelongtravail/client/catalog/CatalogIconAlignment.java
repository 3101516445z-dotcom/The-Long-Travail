package com.thelongtravail.client.catalog;

import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.SpriteContents;
import net.minecraft.world.item.ItemStack;

final class CatalogIconAlignment {
    record Offset(float x, float y) {}

    private static final Offset ZERO = new Offset(0, 0);
    private static final Map<SpriteContents, Offset> OFFSETS = new WeakHashMap<>();

    static Offset offset(ItemStack stack) {
        Minecraft mc = Minecraft.getInstance();
        var model = mc.getItemRenderer().getModel(stack, mc.level, mc.player, 0);
        if (model.isGui3d() || model.isCustomRenderer()) return ZERO;
        return OFFSETS.computeIfAbsent(
                model.getParticleIcon().contents(), CatalogIconAlignment::measure);
    }

    private static Offset measure(SpriteContents sprite) {
        int width = sprite.width(), height = sprite.height();
        int left = width, top = height, right = -1, bottom = -1;
        // 动画使用所有帧的联合边界，避免逐帧居中导致图标抖动。
        for (int frame : sprite.getUniqueFrames().toArray()) {
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    if (sprite.isTransparent(frame, x, y)) continue;
                    left = Math.min(left, x);
                    right = Math.max(right, x);
                    top = Math.min(top, y);
                    bottom = Math.max(bottom, y);
                }
            }
        }
        if (right < left) return ZERO;
        return new Offset(
                (width - left - right - 1) / (2f * width),
                (height - top - bottom - 1) / (2f * height));
    }

    private CatalogIconAlignment() {}
}
