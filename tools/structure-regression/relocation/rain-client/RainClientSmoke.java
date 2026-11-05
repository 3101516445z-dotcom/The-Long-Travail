package travail.raintest;

import com.thelongtravail.abyss.rain.PrecipitationScope;
import com.thelongtravail.abyss.rain.RainEvents;
import com.thelongtravail.abyss.rain.RainState;
import com.thelongtravail.abyss.rain.RainTooltips;
import com.thelongtravail.abyss.rain.RainWeather;

import com.mojang.blaze3d.platform.InputConstants;
import com.thelongtravail.client.RainClient;
import com.thelongtravail.config.RainConfig;
import com.thelongtravail.abyss.*;
import com.thelongtravail.registry.ModRegistry;
import net.minecraft.client.*;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.core.registries.Registries;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;
import java.nio.file.*;

@Mod("travail_rain_client")
public final class RainClientSmoke {
    int ticks, idle;
    boolean launched, finished;
    volatile Throwable failure;

    public RainClientSmoke() {
        MinecraftForge.EVENT_BUS.addListener(this::tick);
    }

    private static void check(boolean ok, String message) {
        if (!ok) throw new AssertionError(message);
    }

    private void server(
            java.util.function.Consumer<net.minecraft.server.level.ServerPlayer> action) {
        var mc = Minecraft.getInstance();
        var id = mc.player.getUUID();
        mc.getSingleplayerServer()
                .execute(
                        () -> {
                            try {
                                action.accept(
                                        mc.getSingleplayerServer().getPlayerList().getPlayer(id));
                            } catch (Throwable t) {
                                failure = t;
                            }
                        });
    }

    private void key(int action) throws Exception {
        var mc = Minecraft.getInstance();
        mc.setWindowActive(true);
        var method =
                KeyboardHandler.class.getDeclaredMethod(
                        "keyPress", long.class, int.class, int.class, int.class, int.class);
        method.setAccessible(true);
        method.invoke(
                mc.keyboardHandler, mc.getWindow().getWindow(), GLFW.GLFW_KEY_R, 0, action, 0);
    }

    private void tick(TickEvent.ClientTickEvent e) {
        if (e.phase != TickEvent.Phase.END || finished) return;
        var mc = Minecraft.getInstance();
        mc.options.pauseOnLostFocus = false;
        GLFW.glfwHideWindow(mc.getWindow().getWindow());
        try {
            if (failure != null) throw new AssertionError("server action failed", failure);
            if (mc.player == null) {
                idle++;
                if (!launched && mc.getOverlay() == null && idle > 40) {
                    launched = true;
                    mc.createWorldOpenFlows()
                            .loadLevel(
                                    new net.minecraft.client.gui.screens.TitleScreen(),
                                    "visual-world");
                }
                if (idle > 1600)
                    throw new AssertionError("client world load timeout: " + mc.screen);
                return;
            }
            ticks++;
            if (ticks == 20) {
                mc.setScreen(null);
                checkEkiVisibility();
                for (var item :
                        new net.minecraft.world.item.Item[] {
                            ModRegistry.EKI.get(), ModRegistry.TOKAIDO.get()
                        })
                    check(
                            mc.getItemRenderer()
                                            .getModel(new ItemStack(item), mc.level, mc.player, 0)
                                    != mc.getModelManager().getMissingModel(),
                            "item model missing");
                server(
                        p -> {
                            var h =
                                    top.theillusivec4.curios.api.CuriosApi.getCuriosInventory(p)
                                            .resolve()
                                            .orElseThrow();
                            h.getStacksHandler("feet")
                                    .orElseThrow()
                                    .getStacks()
                                    .setStackInSlot(0, new ItemStack(ModRegistry.TOKAIDO.get()));
                            p.getInventory().setItem(0, new ItemStack(ModRegistry.EKI.get()));
                            p.getInventory().setItem(1, new ItemStack(ModRegistry.TOKAIDO.get()));
                            p.getPersistentData().putInt(RainState.COOLDOWN_KEY, 0);
                            RainWeather.get(p.serverLevel()).discard(p.serverLevel());
                            p.serverLevel().setWeatherParameters(6000, 0, false, false);
                            p.serverLevel().setRainLevel(0);
                            p.serverLevel().setThunderLevel(0);
                            RainEvents.syncWorld(p.serverLevel());
                        });
            }
            if (ticks == 60) {
                mc.setScreen(new InventoryScreen(mc.player));
                key(GLFW.GLFW_PRESS);
            }
            if (ticks == 80) {
                server(p -> check(RainState.cooldown(p) == 0, "GUI press must not cast"));
                mc.setScreen(null);
                key(GLFW.GLFW_REPEAT);
            }
            if (ticks == 100) {
                server(p -> check(RainState.cooldown(p) == 0, "closing GUI must not replay"));
                key(GLFW.GLFW_RELEASE);
            }
            if (ticks == 110) key(GLFW.GLFW_PRESS);
            if (ticks == 140) {
                check(
                        mc.level.getRainLevel(1) > 0 && mc.level.getRainLevel(1) < .8F,
                        "client rain must fade in, actual=" + mc.level.getRainLevel(1));
                server(
                        p -> {
                            check(
                                    RainState.cooldown(p)
                                                    > Math.max(
                                                            0,
                                                            RainConfig.ticks(
                                                                            RainConfig.COOLDOWN
                                                                                    .get())
                                                                    - 200)
                                            && RainState.cooldown(p)
                                                    <= RainConfig.ticks(RainConfig.COOLDOWN.get()),
                                    "press casts and sets cooldown");
                            check(
                                    RainWeather.get(p.serverLevel()).active(),
                                    "world weather started");
                            p.getPersistentData().putInt(RainState.COOLDOWN_KEY, 0);
                        });
                check(RainState.clientWeather && RainState.clientFire, "weather/fire client sync");
                check(!RainTooltips.clientTokaido.isEmpty(), "server tooltip sync");
                var desert =
                        mc.level
                                .registryAccess()
                                .registryOrThrow(Registries.BIOME)
                                .getOrThrow(Biomes.DESERT);
                var snow =
                        mc.level
                                .registryAccess()
                                .registryOrThrow(Registries.BIOME)
                                .getOrThrow(Biomes.SNOWY_PLAINS);
                try (var scope = new PrecipitationScope(mc.level)) {
                    check(
                            desert.getPrecipitationAt(mc.player.blockPosition())
                                    == Biome.Precipitation.RAIN,
                            "client dry rain");
                    check(
                            snow.getPrecipitationAt(mc.player.blockPosition())
                                    == Biome.Precipitation.RAIN,
                            "client snow rain");
                }
                Screenshot.grab(
                        mc.gameDirectory, "rain-world.png", mc.getMainRenderTarget(), m -> {});
            }
            if (ticks == 145)
                server(p -> RainWeather.get(p.serverLevel()).restore(p.serverLevel()));
            if (ticks == 154)
                check(
                        mc.level.getRainLevel(1) > 0 && mc.level.getRainLevel(1) < .8F,
                        "client rain must fade out, not snap to clear");
            if (ticks == 155) key(GLFW.GLFW_REPEAT);
            if (ticks == 180) {
                server(p -> check(RainState.cooldown(p) == 0, "long press repeat must not cast"));
                mc.setScreen(new InventoryScreen(mc.player));
            }
            if (ticks == 190)
                Screenshot.grab(
                        mc.gameDirectory, "rain-items.png", mc.getMainRenderTarget(), m -> {});
            if (ticks == 200) {
                mc.setScreen(null);
                key(GLFW.GLFW_RELEASE);
                key(GLFW.GLFW_PRESS);
            }
            if (ticks == 225)
                server(
                        p ->
                                check(
                                        RainState.cooldown(p)
                                                        > Math.max(
                                                                0,
                                                                RainConfig.ticks(
                                                                                RainConfig.COOLDOWN
                                                                                        .get())
                                                                        - 200)
                                                && RainState.cooldown(p)
                                                        <= RainConfig.ticks(
                                                                RainConfig.COOLDOWN.get()),
                                        "release and press casts again"));
            if (ticks == 226)
                mc.setScreen(
                        new net.minecraft.client.gui.screens.Screen(
                                net.minecraft.network.chat.Component.literal("Aspect themes")) {
                            @Override
                            public boolean isPauseScreen() {
                                return false;
                            }

                            @Override
                            public void render(
                                    net.minecraft.client.gui.GuiGraphics g,
                                    int x,
                                    int y,
                                    float partial) {
                                g.fill(0, 0, width, height, 0xFF18151D);
                                String[] labels = {"繁茂", "归墟", "穷遐", "幽谷", "冥府", "无垠"};
                                for (var theme : com.thelongtravail.AspectTheme.values())
                                    g.drawString(
                                            font,
                                            theme.name(
                                                    net.minecraft.network.chat.Component.literal(
                                                            labels[theme.ordinal()]
                                                                    + " · "
                                                                    + theme.name())),
                                            30,
                                            20 + theme.ordinal() * 25,
                                            0xFFFFFF,
                                            true);
                                g.drawString(
                                        font,
                                        new ItemStack(ModRegistry.EKI.get()).getHoverName(),
                                        30,
                                        180,
                                        0xFFFFFF,
                                        true);
                                g.drawString(
                                        font,
                                        new ItemStack(ModRegistry.TOKAIDO.get()).getHoverName(),
                                        30,
                                        200,
                                        0xFFFFFF,
                                        true);
                            }
                        });
            if (ticks >= 227 && ticks <= 257 && (ticks - 227) % 10 == 0) {
                var quality =
                        com.thelongtravail.config.TravailClientConfig.HaloQuality.values()[
                                (ticks - 227) / 10];
                com.thelongtravail.config.TravailClientConfig.ASPECT_NAME_HALO_QUALITY.set(quality);
                check(
                        com.thelongtravail.config.TravailClientConfig.NAME_HALO_QUALITY.get()
                                == com.thelongtravail.config.TravailClientConfig.HaloQuality.HIGH,
                        "aspect setting must not change original halo quality");
            }
            if (ticks >= 235 && ticks <= 265 && (ticks - 235) % 10 == 0) {
                var quality =
                        com.thelongtravail.config.TravailClientConfig.ASPECT_NAME_HALO_QUALITY
                                .get();
                Screenshot.grab(
                        mc.gameDirectory,
                        "aspect-halo-" + quality + ".png",
                        mc.getMainRenderTarget(),
                        m -> {});
            }
            if (ticks == 270) {
                com.thelongtravail.config.TravailClientConfig.ASPECT_NAME_HALO_QUALITY.set(
                        com.thelongtravail.config.TravailClientConfig.HaloQuality.HIGH);
                finish(
                        true,
                        "models, rain, input, four aspect halo levels and independent original quality");
            }
        } catch (Throwable error) {
            error.printStackTrace();
            finish(false, error.toString());
        }
    }

    private void checkEkiVisibility() {
        var mc = Minecraft.getInstance();
        for (var item :
                new net.minecraft.world.item.Item[] {
                    ModRegistry.EKI.get(), ModRegistry.TOKAIDO.get()
                }) {
            var stack = new ItemStack(item);
            check(
                    com.thelongtravail.AspectTheme.fromMarker(
                                    stack.getHoverName().getStyle().getInsertion())
                            == com.thelongtravail.AspectTheme.ABYSS,
                    "rain item uses abyss name material");
            var lines =
                    stack.getTooltipLines(
                            mc.player, net.minecraft.world.item.TooltipFlag.Default.NORMAL);
            check(lines.size() >= 3, "summary and catalog entry present");
            for (var line : lines) {
                check(
                        !line.getString().contains("tooltip.the_long_travail."),
                        "summary translated and markers expanded");
                check(!line.getString().contains("§"), "no raw formatting markers");
                line.visit(
                        (style, segment) -> {
                            check(
                                    !style.isObfuscated(),
                                    "current summaries are readable without an equipped diary");
                            return java.util.Optional.empty();
                        },
                        net.minecraft.network.chat.Style.EMPTY);
            }
            var entry =
                    com.thelongtravail.client.catalog.CatalogEntries.forStack(stack).orElseThrow();
            var details = com.thelongtravail.client.catalog.CatalogEntries.details(entry, stack);
            for (String key :
                    item == ModRegistry.EKI.get()
                            ? new String[] {"eki.intro", "eki.malice", "eki.witness"}
                            : new String[] {"tokaido.intro", "tokaido.skill"}) {
                String expected =
                        com.thelongtravail.client.TooltipTranslations.text(
                                        "gui.the_long_travail.catalog.abyss." + key)
                                .getString();
                check(!expected.startsWith("gui.the_long_travail."), "catalog heading translated");
                check(
                        details.stream().anyMatch(c -> c.getString().equals(expected)),
                        "catalog mechanics section present: " + key);
            }
            check(
                    details.stream()
                            .noneMatch(
                                    c ->
                                            (c.getString().contains("tooltip.the_long_travail.")
                                                    || c.getString()
                                                            .contains("gui.the_long_travail."))),
                    "details translated");
        }
        var icon =
                mc.getTextureAtlas(net.minecraft.world.inventory.InventoryMenu.BLOCK_ATLAS)
                        .apply(
                                new net.minecraft.resources.ResourceLocation(
                                        "minecraft", "item/empty_armor_slot_boots"));
        check(
                !icon.contents()
                        .name()
                        .equals(
                                net.minecraft.client.renderer.texture.MissingTextureAtlasSprite
                                        .getLocation()),
                "vanilla feet icon exists in atlas");
        System.out.println(
                "RAIN_CURRENT_TOOLTIP_PASS: summaries, catalog details, material and boots icon");
    }

    private void finish(boolean ok, String message) {
        finished = true;
        System.out.println("RAIN_CLIENT_" + (ok ? "PASS" : "FAIL") + ": " + message);
        try {
            Files.writeString(Path.of("rain-client-result.txt"), ok ? "PASS" : "FAIL: " + message);
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            Minecraft.getInstance().stop();
        }
    }
}
