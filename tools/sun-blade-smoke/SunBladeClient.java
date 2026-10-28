package travail.smoke;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.gui.LoadingErrorScreen;
import net.minecraftforge.common.ForgeI18n;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.nio.file.*;

@Mod.EventBusSubscriber(modid = "travail_smoke", value = Dist.CLIENT)
public final class SunBladeClient {
    private static boolean launched, recorded, inspected;
    private static long completed;

    @SubscribeEvent
    public static void tick(TickEvent.ClientTickEvent e) {
        if (e.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getInstance();
        mc.options.pauseOnLostFocus = false;
        try {
            if (!inspected && mc.player != null && mc.level != null) {
                inspected = true;
                StringBuilder report = new StringBuilder();
                for (var item :
                        java.util.List.of(
                                com.thelongtravail.registry.ModRegistry.SUN_CHASER.get(),
                                com.thelongtravail.registry.ModRegistry.KAGOTSURUBE.get())) {
                    var stack = new net.minecraft.world.item.ItemStack(item);
                    report.append(stack.getHoverName().getString()).append("\n");
                    for (var line :
                            stack.getTooltipLines(
                                    mc.player, net.minecraft.world.item.TooltipFlag.Default.NORMAL))
                        report.append(line.getString()).append("\n");
                    String id =
                            net.minecraftforge.registries.ForgeRegistries.ITEMS
                                    .getKey(item)
                                    .getPath();
                    if (mc.getResourceManager()
                            .getResource(
                                    net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(
                                            "the_long_travail", "textures/item/" + id + ".png"))
                            .isEmpty()) throw new AssertionError("missing texture " + id);
                }
                for (boolean sun : new boolean[] {true, false})
                    for (var line : com.thelongtravail.item.SunBladeText.details(sun))
                        report.append(line.getString()).append("\n");
                if (net.minecraftforge.fml.ModList.get().isLoaded("jei")) {
                    var recipe =
                            net.minecraft.client.Minecraft.getInstance()
                                    .level
                                    .getRecipeManager()
                                    .byKey(
                                            new net.minecraft.resources.ResourceLocation(
                                                    "the_long_travail", "kagotsurube"))
                                    .orElseThrow();
                    if (recipe.getIngredients().size() != 9
                            || recipe.getIngredients().get(1).getItems().length == 0)
                        throw new AssertionError("JEI display ingredients");
                    report.append(
                            "JEI display recipe: 9 slots, curse books="
                                    + recipe.getIngredients().get(1).getItems().length
                                    + "\n");
                }
                Files.writeString(
                        Path.of("sun-blade-client-inspection.txt"),
                        "PASS resource/tooltip/display data (not visual inspection)\n" + report);
            }
            String result =
                    Files.exists(Path.of("sun-blade-result.txt"))
                            ? Files.readString(Path.of("sun-blade-result.txt"))
                            : "";
            if (result.startsWith("PASS") || result.startsWith("FAIL")) {
                if (completed == 0) completed = System.currentTimeMillis();
                if (System.currentTimeMillis() - completed > 15000) {
                    mc.stop();
                    return;
                }
            }
            if (mc.screen instanceof LoadingErrorScreen screen && !recorded) {
                recorded = true;
                StringBuilder report = new StringBuilder();
                for (String name : new String[] {"modLoadErrors", "modLoadWarnings"}) {
                    var f = LoadingErrorScreen.class.getDeclaredField(name);
                    f.setAccessible(true);
                    report.append(name).append('=').append(f.get(screen)).append('\n');
                }
                for (var child : screen.children())
                    if (child instanceof Button b)
                        report.append("button=").append(b.getMessage().getString()).append('\n');
                Files.writeString(Path.of("sun-blade-loading-screen.txt"), report);
                String proceed = ForgeI18n.parseMessage("fml.button.continue.launch");
                for (var child : screen.children())
                    if (child instanceof Button b
                            && b.getMessage().getString().equals(proceed)
                            && b.active) {
                        b.onPress();
                        break;
                    }
            }
            if (!launched && mc.level == null && mc.screen instanceof TitleScreen) {
                launched = true;
                mc.createWorldOpenFlows()
                        .loadLevel(
                                mc.screen,
                                System.getProperty("travail.sun.world", "sun-blade-test"));
            }
        } catch (Throwable ex) {
            ex.printStackTrace();
            try {
                Files.writeString(Path.of("sun-blade-client-inspection.txt"), "FAIL " + ex);
            } catch (Exception ignored) {
            }
            recorded = true;
        }
    }
}
