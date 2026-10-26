package travail.smoke;

import com.thelongtravail.client.catalog.*;
import com.thelongtravail.registry.ModRegistry;
import com.thelongtravail.data.LongTravailData;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.*;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.world.item.*;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.fml.common.Mod;
import top.theillusivec4.curios.api.CuriosApi;
import java.nio.file.*;
import java.util.*;
import org.lwjgl.glfw.GLFW;

@Mod("travail_smoke")
public class CarryingPreview {
    private static final String[] IDS = {
        "golden_age",
        "book_of_the_dead",
        "don_quixote",
        "star_voice",
        "little_prince",
        "tower_of_babel",
        "eki"
    };
    private int phase, ticks, wait, index, page, saved;
    private volatile boolean ready;
    private volatile Throwable error;
    private boolean done;
    private final List<String> report = new ArrayList<>();

    public CarryingPreview() {
        MinecraftForge.EVENT_BUS.addListener(this::tick);
    }

    private void record(String text) {
        report.add(text);
        System.out.println("CARRYING_PREVIEW " + text);
    }

    private void capture(String suffix) {
        var mc = Minecraft.getInstance();
        Screenshot.grab(
                mc.gameDirectory,
                "carrying-" + IDS[index] + "-" + suffix + ".png",
                mc.getMainRenderTarget(),
                message -> saved++);
        record("screenshot " + IDS[index] + " " + suffix);
    }

    private void prepare() {
        var mc = Minecraft.getInstance();
        ready = false;
        mc.getSingleplayerServer()
                .execute(
                        () -> {
                            try {
                                var p =
                                        mc.getSingleplayerServer()
                                                .getPlayerList()
                                                .getPlayer(mc.player.getUUID());
                                p.getInventory().clearContent();
                                p.removeAllEffects();
                                p.setHealth(p.getMaxHealth());
                                p.getAbilities().invulnerable = true;
                                p.onUpdateAbilities();
                                var curios =
                                        CuriosApi.getCuriosInventory(p).resolve().orElseThrow();
                                for (var h : curios.getCurios().values())
                                    for (int i = 0; i < h.getSlots(); i++) {
                                        h.getStacks().setStackInSlot(i, ItemStack.EMPTY);
                                        h.getCosmeticStacks().setStackInSlot(i, ItemStack.EMPTY);
                                    }
                                var diary = new ItemStack(ModRegistry.LONG_TRAVAIL.get());
                                LongTravailData.tryInitialize(diary, p);
                                curios.getStacksHandler("travel_diary")
                                        .orElseThrow()
                                        .getStacks()
                                        .setStackInSlot(0, diary);
                                for (int i = 0; i < IDS.length; i++)
                                    p.getInventory()
                                            .setItem(9 + i, CatalogEntries.byId(IDS[i]).stack());
                                p.inventoryMenu.broadcastChanges();
                                ready = true;
                            } catch (Throwable ex) {
                                error = ex;
                            }
                        });
    }

    private static class HoverInventory extends InventoryScreen {
        private final int column;

        HoverInventory(int column) {
            super(Minecraft.getInstance().player);
            this.column = column;
        }

        @Override
        public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
            super.render(g, leftPos + 8 + column * 18 + 8, topPos + 84 + 8, partialTick);
        }
    }

    private void tick(TickEvent.ClientTickEvent event) {
        if (done || event.phase != TickEvent.Phase.END) return;
        var mc = Minecraft.getInstance();
        try {
            if (error != null) throw new AssertionError(error);
            if (++ticks > 12000) throw new AssertionError("timeout phase=" + phase);
            if (ticks % 200 == 0)
                record(
                        "phase="
                                + phase
                                + " screen="
                                + (mc.screen == null
                                        ? "none"
                                        : mc.screen.getClass().getSimpleName()));
            if (phase == 0
                    && mc.getOverlay() == null
                    && mc.screen != null
                    && mc.screen
                            .getClass()
                            .getSimpleName()
                            .matches("AccessibilityOnboardingScreen|StartupAdviceScreen"))
                mc.setScreen(new TitleScreen());
            if (phase == 0 && mc.screen instanceof TitleScreen && mc.getOverlay() == null) {
                phase = 1;
                mc.createWorldOpenFlows().loadLevel(new TitleScreen(), "revival-normal");
            } else if (phase == 1 && mc.player != null && mc.level != null) {
                phase = 2;
                wait = 0;
                prepare();
            } else if (phase == 2 && ready && ++wait > 80) {
                phase = 3;
                wait = 0;
                mc.getToasts().clear();
                mc.setScreen(new HoverInventory(index));
                var stack = CatalogEntries.byId(IDS[index]).stack();
                record(
                        "tooltip "
                                + IDS[index]
                                + "\n"
                                + stack.getTooltipLines(mc.player, TooltipFlag.NORMAL).stream()
                                        .map(x -> x.getString())
                                        .toList());
            } else if (phase == 3 && ++wait > 35) {
                capture("tooltip");
                phase = 4;
                wait = 0;
            } else if (phase == 4 && ++wait > 10) {
                var owner = new InventoryScreen(mc.player);
                mc.setScreen(owner);
                var entry = CatalogEntries.byId(IDS[index]);
                CatalogSession.openDetail(owner, entry, entry.stack());
                page = 1;
                phase = 5;
                wait = 0;
                record(
                        "details "
                                + IDS[index]
                                + "\n"
                                + CatalogEntries.details(entry, entry.stack()).stream()
                                        .map(x -> x.getString())
                                        .toList());
            } else if (phase == 5 && ++wait > 30) {
                capture("detail-" + page);
                phase = 6;
                wait = 0;
            } else if (phase == 6 && ++wait > 10) {
                var screen = (CatalogDetailScreen) CatalogSession.active().page();
                double before = screen.scrollPosition();
                screen.keyPressed(GLFW.GLFW_KEY_PAGE_DOWN, 0, 0);
                if (screen.scrollPosition() > before) {
                    page++;
                    phase = 5;
                    wait = 0;
                } else {
                    CatalogSession.active().close();
                    index++;
                    if (index < IDS.length) {
                        phase = 2;
                        wait = 80;
                    } else {
                        phase = 7;
                        wait = 0;
                    }
                }
            } else if (phase == 7 && ++wait > 40) {
                record("PASS screenshots=" + saved);
                done = true;
                Files.write(Path.of("carrying-preview.txt"), report);
                Files.writeString(Path.of("client-result.txt"), "PASS");
                mc.stop();
            }
        } catch (Throwable ex) {
            ex.printStackTrace();
            record("FAIL " + ex);
            done = true;
            try {
                Files.write(Path.of("carrying-preview.txt"), report);
                Files.writeString(Path.of("client-result.txt"), "FAIL");
            } catch (Exception ignored) {
            }
            mc.stop();
        }
    }
}
