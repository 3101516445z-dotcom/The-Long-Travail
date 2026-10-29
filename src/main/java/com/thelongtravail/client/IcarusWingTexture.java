package com.thelongtravail.client;

import com.mojang.blaze3d.platform.NativeImage;
import com.thelongtravail.TheLongTravail;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterClientReloadListenersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import java.io.IOException;

/** 每次资源重载只尝试一次；资源包损坏时从模组自身读取默认贴图。 */
@Mod.EventBusSubscriber(
        modid = TheLongTravail.MODID,
        bus = Mod.EventBusSubscriber.Bus.MOD,
        value = Dist.CLIENT)
public final class IcarusWingTexture {
    private static final ResourceLocation ORIGINAL =
            new ResourceLocation(TheLongTravail.MODID, "textures/entity/icarus_elytra.png");
    private static long generation;
    private static boolean[][] outline;
    private static ResourceLocation texture = ORIGINAL;
    private static ResourceLocation dynamic;

    public static long generation() {
        return generation;
    }

    public static ResourceLocation texture() {
        load();
        return texture;
    }

    public static boolean[][] outline() {
        load();
        return outline;
    }

    @SubscribeEvent
    public static void register(RegisterClientReloadListenersEvent event) {
        event.registerReloadListener(
                (ResourceManagerReloadListener)
                        resources -> {
                            if (dynamic != null)
                                Minecraft.getInstance().getTextureManager().release(dynamic);
                            dynamic = null;
                            outline = null;
                            texture = ORIGINAL;
                            generation++;
                        });
    }

    public static boolean[][] readOutline(NativeImage image) throws IOException {
        if (image.getWidth() != 64
                || image.getHeight() != 32
                || image.format() != NativeImage.Format.RGBA)
            throw new IOException("Expected a 64x32 RGBA Icarus texture");
        boolean[][] result = new boolean[20][10];
        for (int y = 0; y < 20; y++)
            for (int x = 0; x < 10; x++)
                result[y][x] = (image.getPixelRGBA(36 + x, 2 + y) >>> 24) != 0;
        return result;
    }

    private static void load() {
        if (outline != null) return;
        try (var stream =
                        Minecraft.getInstance()
                                .getResourceManager()
                                .getResourceOrThrow(ORIGINAL)
                                .open();
                var pixels = NativeImage.read(stream)) {
            outline = readOutline(pixels);
            return;
        } catch (IOException | RuntimeException failure) {
            TheLongTravail.LOGGER.warn(
                    "Cannot read Icarus resource-pack texture; using bundled fallback", failure);
        }
        try (var stream =
                IcarusWingTexture.class.getResourceAsStream(
                        "/assets/the_long_travail/textures/entity/icarus_elytra.png")) {
            if (stream == null) throw new IOException("Bundled Icarus texture missing");
            NativeImage pixels = NativeImage.read(stream);
            try {
                boolean[][] fallback = readOutline(pixels);
                dynamic =
                        Minecraft.getInstance()
                                .getTextureManager()
                                .register("icarus_fallback", new DynamicTexture(pixels));
                texture = dynamic;
                outline = fallback;
            } catch (IOException | RuntimeException failure) {
                pixels.close();
                throw failure;
            }
        } catch (IOException | RuntimeException failure) {
            TheLongTravail.LOGGER.warn(
                    "Cannot load bundled Icarus texture; using missing-texture fallback", failure);
            texture = MissingTextureAtlasSprite.getLocation();
            outline = new boolean[20][10];
            for (boolean[] row : outline) java.util.Arrays.fill(row, true);
        }
    }

    private IcarusWingTexture() {}
}
