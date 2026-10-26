package travail.abundanceclient;

import com.thelongtravail.abyss.LiquefiedAbundance;
import com.thelongtravail.client.AbundanceUseClient;
import com.thelongtravail.config.AbyssItemsConfig;
import com.thelongtravail.mixin.SkillNoticeGuiAccessor;
import com.thelongtravail.network.TravailNetwork;
import com.thelongtravail.registry.ModRegistry;

import net.minecraft.client.*;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.*;
import net.minecraft.world.level.GameType;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.fml.common.Mod;

import java.nio.file.*;
import java.util.*;

@Mod("travail_smoke")
public final class AbundanceClientSmoke {
    private record Case(double seconds, int mode, String action) {}

    private final List<Case> cases = new ArrayList<>();
    private int ticks, phase, wait, index, checks;
    private boolean finished, protectedPlayer;
    private volatile boolean prepared;
    private volatile int restoredMode = -1;
    private Component other;

    public AbundanceClientSmoke() {
        for (double seconds :
                (Boolean.getBoolean("abundance.oneSecond")
                        ? List.of(1.0)
                        : List.of(0.5, 1.0, 2.0, 5.0, 10.0)))
            for (int mode = 0; mode < 3; mode++) cases.add(new Case(seconds, mode, "hold"));
        if (!Boolean.getBoolean("abundance.oneSecond"))
            for (String action : List.of("tap", "cancel", "screen", "other", "reload"))
                for (int mode = 0; mode < 3; mode++) cases.add(new Case(2.0, mode, action));
        MinecraftForge.EVENT_BUS.addListener(this::tick);
        MinecraftForge.EVENT_BUS.addListener(FullpackAbundanceStress::tick);
    }

    private void check(boolean value, String message) {
        checks++;
        if (!value) throw new AssertionError(message + " case=" + index);
    }

    private int potions(Minecraft mc) {
        return mc.player.getInventory().countItem(Items.POTION)
                + mc.player.getInventory().countItem(Items.SPLASH_POTION)
                + mc.player.getInventory().countItem(Items.LINGERING_POTION);
    }

    private void prepare(Minecraft mc) {
        prepared = false;
        var c = cases.get(index);
        mc.options.keyUse.setDown(false);
        mc.getSingleplayerServer()
                .execute(
                        () -> {
                            var p =
                                    mc.getSingleplayerServer()
                                            .getPlayerList()
                                            .getPlayer(mc.player.getUUID());
                            p.stopUsingItem();
                            p.setGameMode(GameType.SURVIVAL);
                            p.setInvulnerable(true);
                            p.getInventory().clearContent();
                            p.getInventory().selected = 0;
                            p.connection.send(
                                    new net.minecraft.network.protocol.game
                                            .ClientboundSetCarriedItemPacket(0));
                            p.setItemInHand(
                                    InteractionHand.MAIN_HAND,
                                    new ItemStack(ModRegistry.LIQUEFIED_ABUNDANCE.get(), 64));
                            p.getPersistentData().putInt(LiquefiedAbundance.MODE_KEY, c.mode());
                            AbyssItemsConfig.ABUNDANCE_HOLD.set(c.seconds());
                            LiquefiedAbundance.reload();
                            LiquefiedAbundance.sync(p);
                            TravailNetwork.broadcastTooltipConfig(mc.getSingleplayerServer());
                            p.inventoryMenu.broadcastChanges();
                            prepared = true;
                        });
    }

    private void tick(TickEvent.ClientTickEvent event) {
        if (finished || event.phase != TickEvent.Phase.END) return;
        var mc = Minecraft.getInstance();
        try {
            if (++ticks > 18000) throw new AssertionError("timeout phase=" + phase);
            mc.setWindowActive(true);
            if (ticks % 200 == 0)
                System.out.println("ABUNDANCE_MODE phase=" + phase + " case=" + index);
            if (phase == 0
                    && ticks > 100
                    && mc.getOverlay() == null
                    && mc.screen != null
                    && mc.screen
                            .getClass()
                            .getSimpleName()
                            .matches("AccessibilityOnboardingScreen|StartupAdviceScreen"))
                mc.setScreen(new TitleScreen());
            if (phase == 0
                    && ticks > 100
                    && mc.getOverlay() == null
                    && mc.screen instanceof TitleScreen) {
                mc.createWorldOpenFlows().loadLevel(new TitleScreen(), "abundance");
                phase = 1;
            } else if (phase == 1 && mc.player != null && mc.getSingleplayerServer() != null) {
                if (Boolean.getBoolean("abundance.persistenceOnly")
                        || Boolean.getBoolean("abundance.persistenceSeed")) {
                    if (mc.screen != null) return;
                    mc.getSingleplayerServer()
                            .execute(
                                    () -> {
                                        var player =
                                                mc.getSingleplayerServer()
                                                        .getPlayerList()
                                                        .getPlayer(mc.player.getUUID());
                                        if (Boolean.getBoolean("abundance.persistenceSeed")) {
                                            player.getPersistentData()
                                                    .putInt(LiquefiedAbundance.MODE_KEY, 2);
                                            LiquefiedAbundance.sync(player);
                                        }
                                        restoredMode = LiquefiedAbundance.mode(player);
                                    });
                    phase = 7;
                    wait = 0;
                    return;
                }
                if (mc.screen instanceof net.minecraft.client.gui.screens.DeathScreen) {
                    mc.player.respawn();
                    mc.setScreen(null);
                    protectedPlayer = false;
                    return;
                }
                if (!protectedPlayer && mc.player.isAlive()) {
                    if (Boolean.getBoolean("abundance.oneSecond")) {
                        check(
                                AbyssItemsConfig.ABUNDANCE_HOLD.get() == 1.0,
                                "missing config value initializes to one second");
                        check(
                                Component.translatable(
                                                "tooltip.the_long_travail.flavor.liquefied_abundance")
                                        .getString()
                                        .equals("今日和风细雨。干杯，我的朋友。"),
                                "approved flavor text");
                    }
                    protectedPlayer = true;
                    prepare(mc);
                }
                if (mc.screen == null && mc.getSingleplayerServer().getTickCount() > 400) {
                    prepare(mc);
                    phase = 2;
                    wait = 0;
                }
            } else if (phase == 2
                    && ++wait > 30
                    && prepared
                    && mc.player.getMainHandItem().is(ModRegistry.LIQUEFIED_ABUNDANCE.get())
                    && AbundanceUseClient.selectedMode() == cases.get(index).mode()
                    && LiquefiedAbundance.useTicks() == (int) (cases.get(index).seconds() * 20)) {
                var tooltip = new ArrayList<Component>();
                var held = mc.player.getMainHandItem();
                tooltip.add(held.getHoverName());
                com.thelongtravail.item.ItemDescription.summary(tooltip);
                com.thelongtravail.client.ThemeTooltips.finish(held, mc.player, tooltip);
                String state =
                        Component.translatable(
                                        "tooltip.the_long_travail.abundance.current",
                                        LiquefiedAbundance.modeName(cases.get(index).mode()))
                                .getString();
                check(
                        tooltip.stream().anyMatch(line -> line.getString().equals(state)),
                        "current mode in tooltip");
                check(
                        com.thelongtravail.abyss.AbundanceText.details().stream()
                                .noneMatch(line -> line.getString().equals(state)),
                        "personal mode absent from catalog");
                mc.player.setXRot(-90);
                mc.options.keyUse.setDown(true);
                KeyMapping.click(mc.options.keyUse.getKey());
                phase = 3;
                wait = 0;
            } else if (phase == 3) {
                var c = cases.get(index);
                int age = ++wait;
                int total = (int) (c.seconds() * 20);
                var gui = (SkillNoticeGuiAccessor) mc.gui;
                if (c.action().equals("hold")) {
                    if (age == Math.max(3, total / 2)) {
                        var text = gui.travail$getOverlayMessage();
                        check(
                                text != null
                                        && text.getString()
                                                .contains(
                                                        LiquefiedAbundance.modeName(c.mode())
                                                                .getString()),
                                "type in Action Bar");
                        check(
                                !text.getString().contains("▰")
                                        && !text.getString().contains("▱")
                                        && AbundanceUseClient.chargeProgress() > 0.2
                                        && AbundanceUseClient.chargeProgress() < 0.8,
                                "partial progress after configured half duration");
                        check(gui.travail$getOverlayMessageTime() > 20, "Action Bar stays alive");
                        if (c.seconds() == 10 && c.mode() == 2)
                            Screenshot.grab(
                                    mc.gameDirectory,
                                    "abundance-mode-actionbar.png",
                                    mc.getMainRenderTarget(),
                                    ignored -> {});
                    }
                    if (age >= total + 30) {
                        Item expected =
                                switch (c.mode()) {
                                    case 1 -> Items.SPLASH_POTION;
                                    case 2 -> Items.LINGERING_POTION;
                                    default -> Items.POTION;
                                };
                        check(
                                mc.player.getInventory().countItem(expected) == 1
                                        && potions(mc) == 1,
                                "one selected potion despite holding");
                        check(mc.player.getMainHandItem().getCount() == 63, "consume exactly one");
                        check(!mc.player.isUsingItem(), "hold lock");
                        mc.options.keyUse.setDown(false);
                        phase = 5;
                        wait = 0;
                    }
                } else if (c.action().equals("other") && age == 10) {
                    other = Component.literal("OTHER ACTION BAR");
                    mc.gui.setOverlayMessage(other, false);
                } else if (c.action().equals("other") && age == 15) {
                    check(
                            gui.travail$getOverlayMessage() == other,
                            "other message takes precedence");
                    check(
                            AbundanceUseClient.chargeProgress() < 0,
                            "other message hides progress bar");
                    mc.setScreen(new InventoryScreen(mc.player));
                    phase = 4;
                    wait = 0;
                } else if (age == (c.action().equals("tap") ? 3 : 10)
                        && !c.action().equals("other")) {
                    if (c.action().equals("screen")) mc.setScreen(new InventoryScreen(mc.player));
                    else if (c.action().equals("reload"))
                        mc.getSingleplayerServer()
                                .execute(
                                        () -> {
                                            AbyssItemsConfig.ABUNDANCE_HOLD.set(5.0);
                                            LiquefiedAbundance.reload();
                                            TravailNetwork.broadcastTooltipConfig(
                                                    mc.getSingleplayerServer());
                                        });
                    else mc.options.keyUse.setDown(false);
                    phase = 4;
                    wait = 0;
                }
            } else if (phase == 4 && ++wait > 30) {
                var c = cases.get(index);
                check(
                        potions(mc) == 0 && mc.player.getMainHandItem().getCount() == 64,
                        "tap/cancel preserves input");
                check(
                        AbundanceUseClient.selectedMode()
                                == (c.action().equals("tap") ? (c.mode() + 1) % 3 : c.mode()),
                        "mode changes only on tap");
                check(AbundanceUseClient.chargeProgress() < 0, "cancel hides progress bar");
                if (c.action().equals("other"))
                    check(
                            ((SkillNoticeGuiAccessor) mc.gui).travail$getOverlayMessage() == other,
                            "cancel preserves other message");
                mc.options.keyUse.setDown(false);
                mc.setScreen(null);
                phase = 5;
                wait = 0;
            } else if (phase == 5 && ++wait > 8) {
                if (++index < cases.size()) {
                    prepare(mc);
                    phase = 2;
                    wait = 0;
                } else if (Boolean.getBoolean("abundance.fullpack")) {
                    FullpackAbundanceStress.start(mc.getSingleplayerServer());
                    phase = 6;
                } else {
                    mc.getSingleplayerServer()
                            .execute(
                                    () -> {
                                        AbyssItemsConfig.ABUNDANCE_HOLD.set(1.0);
                                        AbyssItemsConfig.ABUNDANCE_DURATION.set(15.0);
                                        AbyssItemsConfig.ABUNDANCE_BLACKLIST.set(List.of());
                                        TravailNetwork.broadcastTooltipConfig(
                                                mc.getSingleplayerServer());
                                    });
                    mc.setScreen(new InventoryScreen(mc.player));
                    int x = (mc.getWindow().getGuiScaledWidth() - 176) / 2 + 12;
                    int y = (mc.getWindow().getGuiScaledHeight() - 166) / 2 + 146;
                    org.lwjgl.glfw.GLFW.glfwSetCursorPos(
                            mc.getWindow().getWindow(),
                            x * mc.getWindow().getGuiScale(),
                            y * mc.getWindow().getGuiScale());
                    phase = 8;
                    wait = 0;
                }
            } else if (phase == 8 && ++wait > 30) {
                Screenshot.grab(
                        mc.gameDirectory,
                        "abundance-approved-tooltip.png",
                        mc.getMainRenderTarget(),
                        ignored -> {});
                com.thelongtravail.client.catalog.CatalogSession.openDetail(
                        mc.screen,
                        com.thelongtravail.client.catalog.CatalogEntries.byId(
                                "liquefied_abundance"),
                        new ItemStack(ModRegistry.LIQUEFIED_ABUNDANCE.get()));
                phase = 9;
                wait = 0;
            } else if (phase == 9 && ++wait > 30) {
                Screenshot.grab(
                        mc.gameDirectory,
                        "abundance-approved-catalog-top.png",
                        mc.getMainRenderTarget(),
                        ignored -> {});
                com.thelongtravail.client.catalog.CatalogSession.active()
                        .page()
                        .mouseScrolled(300, 200, -30);
                phase = 10;
                wait = 0;
            } else if (phase == 10 && ++wait > 30) {
                Screenshot.grab(
                        mc.gameDirectory,
                        "abundance-approved-catalog-bottom.png",
                        mc.getMainRenderTarget(),
                        ignored -> {});
                mc.getSingleplayerServer()
                        .execute(
                                () -> {
                                    AbyssItemsConfig.ABUNDANCE_HOLD.set(3.0);
                                    AbyssItemsConfig.ABUNDANCE_DURATION.set(7.5);
                                    AbyssItemsConfig.ABUNDANCE_BLACKLIST.set(
                                            List.of(
                                                    "minecraft:poison",
                                                    "minecraft:wither",
                                                    "minecraft:instant_health",
                                                    "minecraft:slowness",
                                                    "minecraft:weakness",
                                                    "minecraft:speed"));
                                    AbyssItemsConfig.ABUNDANCE_BLACKLIST.save();
                                    LiquefiedAbundance.reload();
                                    TravailNetwork.broadcastTooltipConfig(
                                            mc.getSingleplayerServer());
                                });
                phase = 11;
                wait = 0;
            } else if (phase == 11 && ++wait > 40) {
                check(
                        com.thelongtravail.network.TooltipConfigSync.pool(
                                                "abundance.blacklist", List.of())
                                        .size()
                                == 6,
                        "blacklist sync");
                com.thelongtravail.client.catalog.CatalogSession.active()
                        .detail(
                                com.thelongtravail.client.catalog.CatalogEntries.byId(
                                        "liquefied_abundance"),
                                new ItemStack(ModRegistry.LIQUEFIED_ABUNDANCE.get()));
                phase = 12;
                wait = 0;
            } else if (phase == 12 && ++wait > 30) {
                Screenshot.grab(
                        mc.gameDirectory,
                        "abundance-configured-top.png",
                        mc.getMainRenderTarget(),
                        ignored -> {});
                com.thelongtravail.client.catalog.CatalogSession.active()
                        .page()
                        .mouseScrolled(300, 200, -30);
                phase = 13;
                wait = 0;
            } else if (phase == 13 && ++wait > 30) {
                Screenshot.grab(
                        mc.gameDirectory,
                        "abundance-configured-bottom.png",
                        mc.getMainRenderTarget(),
                        ignored -> {});
                mc.getSingleplayerServer()
                        .execute(
                                () -> {
                                    AbyssItemsConfig.ABUNDANCE_HOLD.set(1.0);
                                    AbyssItemsConfig.ABUNDANCE_DURATION.set(15.0);
                                    AbyssItemsConfig.ABUNDANCE_BLACKLIST.set(List.of());
                                    AbyssItemsConfig.ABUNDANCE_BLACKLIST.save();
                                    LiquefiedAbundance.reload();
                                    TravailNetwork.broadcastTooltipConfig(
                                            mc.getSingleplayerServer());
                                });
                phase = 14;
                wait = 0;
            } else if (phase == 14 && ++wait > 40) {
                check(
                        com.thelongtravail.network.TooltipConfigSync.pool(
                                        "abundance.blacklist", List.of())
                                .isEmpty(),
                        "blacklist restored");
                finish(
                        mc,
                        "PASS checks="
                                + checks
                                + " cases="
                                + index
                                + "; layout and configured blacklist screenshots; isolated defaults"
                                + " restored");
            } else if (phase == 7 && ++wait > 60 && restoredMode >= 0) {
                boolean seed = Boolean.getBoolean("abundance.persistenceSeed");
                check(
                        restoredMode == 2,
                        seed
                                ? "seed selection is set"
                                : "selection survives full JVM restart on server");
                check(
                        AbundanceUseClient.selectedMode() == 2,
                        seed
                                ? "seed selection synchronizes"
                                : "login synchronizes persisted selection");
                finish(
                        mc,
                        (seed ? "PASS persistence seed checks=" : "PASS persistence checks=")
                                + checks
                                + (seed
                                        ? " mode=2; preparing normal save"
                                        : " mode=2; full JVM restart and login sync"));
            } else if (phase == 6 && FullpackAbundanceStress.done) {
                if (FullpackAbundanceStress.failure != null)
                    throw new AssertionError("server stress", FullpackAbundanceStress.failure);
                finish(
                        mc,
                        "PASS checks="
                                + checks
                                + " cases="
                                + index
                                + " plus full-pack server stress");
            }
        } catch (Throwable error) {
            error.printStackTrace();
            finish(mc, "FAIL " + error);
        }
    }

    private void finish(Minecraft mc, String result) {
        finished = true;
        mc.options.keyUse.setDown(false);
        try {
            Files.writeString(Path.of("client-result.txt"), result);
        } catch (Exception e) {
            e.printStackTrace();
        }
        System.out.println("ABUNDANCE_MODE " + result);
        mc.stop();
    }
}
