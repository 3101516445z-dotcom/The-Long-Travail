package com.thelongtravail.visualtest;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import com.thelongtravail.client.VisualDeprivationClient;
@Mod("travail_visual_smoke")
public class VisualClientSmoke {
    int ticks, idle; boolean launched;
    public VisualClientSmoke() { MinecraftForge.EVENT_BUS.addListener(this::tick); }
    void tick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        var mc = Minecraft.getInstance();
        mc.options.pauseOnLostFocus = false;
        org.lwjgl.glfw.GLFW.glfwHideWindow(mc.getWindow().getWindow());
        if (mc.player == null) {
            if (++idle % 100 == 0) System.out.println("VISUAL_SCREEN: " + mc.screen);
            if (!launched && mc.getOverlay() == null && idle > 40) {
                launched = true;
                mc.createWorldOpenFlows().loadLevel(new net.minecraft.client.gui.screens.TitleScreen(), "visual-world");
            }
            if (idle > 1200) { System.out.println("VISUAL_CLIENT_FAIL: world did not open"); mc.stop(); }
            return;
        }
        ticks++;
        if (ticks == 20) { mc.setScreen(null); mc.player.setYRot(0); mc.player.setXRot(0); }
        if (ticks == 40) shot("baseline");
        if (ticks == 50) VisualDeprivationClient.accept(160,160,false);
        if (ticks == 55) shot("fade-in");
        if (ticks == 90) shot("full");
        if (ticks == 91) mc.options.hideGui = true;
        if (ticks == 94) shot("hidden-hud");
        if (ticks == 95) { mc.options.hideGui = false; mc.setScreen(new net.minecraft.client.gui.screens.inventory.InventoryScreen(mc.player)); }
        if (ticks == 98) shot("inventory");
        if (ticks == 99) mc.setScreen(null);
        if (ticks == 100) VisualDeprivationClient.accept(0,0,false);
        if (ticks == 120) shot("released");
        if (ticks == 130) {
            System.out.println("VISUAL_CLIENT_PASS: world render, shader registration, release, HUD render survived");
            mc.stop();
        }
    }
    void shot(String name) {
        var mc = Minecraft.getInstance();
        Screenshot.grab(mc.gameDirectory, "visual-"+name+".png", mc.getMainRenderTarget(), message -> {});
    }
}
