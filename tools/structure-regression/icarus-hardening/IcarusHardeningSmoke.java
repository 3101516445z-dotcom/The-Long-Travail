package travail.icarustest;

import com.mojang.blaze3d.platform.NativeImage;
import com.thelongtravail.client.IcarusWingTexture;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.fml.common.Mod;
import java.nio.file.*;
import java.util.Arrays;

@Mod("travail_smoke")
public final class IcarusHardeningSmoke {
    private boolean started;
    private int checks;

    public IcarusHardeningSmoke() {
        MinecraftForge.EVENT_BUS.addListener(this::tick);
    }

    private void check(boolean value, String label) {
        checks++;
        if (!value) throw new AssertionError(label);
    }

    private void finish(Throwable failure) {
        String result = failure == null ? "PASS checks=" + checks : "FAIL " + failure;
        if (failure != null) failure.printStackTrace();
        try {
            Files.writeString(Path.of("icarus-result.txt"), result);
        } catch (Exception error) {
            throw new RuntimeException(error);
        }
        System.out.println("ICARUS " + result);
        Minecraft.getInstance().stop();
    }

    private void tick(TickEvent.ClientTickEvent event) {
        Minecraft mc = Minecraft.getInstance();
        if (started
                || event.phase != TickEvent.Phase.END
                || !(mc.screen instanceof TitleScreen)
                || mc.getOverlay() != null) return;
        started = true;
        try {
            for (int[] size : new int[][] {{1, 1}, {45, 21}, {128, 64}}) {
                try (var image = new NativeImage(size[0], size[1], true)) {
                    boolean rejected = false;
                    try {
                        IcarusWingTexture.readOutline(image);
                    } catch (java.io.IOException expected) {
                        rejected = true;
                    }
                    check(rejected, "reject wrong dimensions");
                }
            }
            try (var image = new NativeImage(NativeImage.Format.RGB, 64, 32, true)) {
                boolean rejected = false;
                try {
                    IcarusWingTexture.readOutline(image);
                } catch (java.io.IOException expected) {
                    rejected = true;
                }
                check(rejected, "reject RGB without pixel access crash");
            }
            ResourceLocation original =
                    ResourceLocation.fromNamespaceAndPath(
                            "the_long_travail", "textures/entity/icarus_elytra.png");
            boolean[][] fallback = IcarusWingTexture.outline();
            check(
                    !IcarusWingTexture.texture().equals(original),
                    "bad resource pack uses dynamic fallback");
            check(IcarusWingTexture.outline() == fallback, "failed resource cached");
            byte[] bundled;
            try (var stream =
                    IcarusWingTexture.class.getResourceAsStream(
                            "/assets/the_long_travail/textures/entity/icarus_elytra.png")) {
                bundled = stream.readAllBytes();
            }
            try (var pixels = NativeImage.read(new java.io.ByteArrayInputStream(bundled))) {
                check(
                        Arrays.deepEquals(fallback, IcarusWingTexture.readOutline(pixels)),
                        "fallback retains bundled silhouette");
            }
            long before = IcarusWingTexture.generation();
            Files.write(
                    Path.of(
                            "resourcepacks/icarus-broken/assets/the_long_travail/textures/entity/icarus_elytra.png"),
                    bundled);
            mc.reloadResourcePacks()
                    .whenComplete(
                            (unused, error) ->
                                    mc.execute(
                                            () -> {
                                                if (error != null) {
                                                    finish(error);
                                                    return;
                                                }
                                                try {
                                                    check(
                                                            IcarusWingTexture.generation() > before,
                                                            "resource reload invalidates cache");
                                                    check(
                                                            IcarusWingTexture.texture()
                                                                    .equals(original),
                                                            "fixed resource recovered");
                                                    check(
                                                            Arrays.deepEquals(
                                                                    fallback,
                                                                    IcarusWingTexture.outline()),
                                                            "reloaded silhouette matches");
                                                    finish(null);
                                                } catch (Throwable failure) {
                                                    finish(failure);
                                                }
                                            }));
        } catch (Throwable failure) {
            finish(failure);
        }
    }
}
