package travail.smoke;

import com.thelongtravail.TravailAspect;
import com.thelongtravail.data.LongTravailData;
import com.thelongtravail.registry.ModRegistry;
import com.thelongtravail.underworld.*;

import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.*;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.*;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.fml.common.Mod;

import top.theillusivec4.curios.api.CuriosApi;

import java.nio.file.*;
import java.util.*;

@Mod("travail_smoke")
public class RevivalClientAcceptance {
    private int phase, ticks, wait, round;
    private int world = Integer.getInteger("revival.startWorld", 0);
    private int damageAttempts;
    private boolean done;
    private volatile boolean ready, deathTriggered;
    private long setupTime;
    private volatile Throwable error;
    private UUID selected, spare;
    private int clicks;
    private final StringBuilder report = new StringBuilder();

    public RevivalClientAcceptance() {
        MinecraftForge.EVENT_BUS.addListener(this::tick);
        MinecraftForge.EVENT_BUS.addListener(
                net.minecraftforge.eventbus.api.EventPriority.LOWEST,
                true,
                (net.minecraftforge.event.entity.living.LivingAttackEvent e) -> {
                    if (e.getEntity() instanceof ServerPlayer p
                            && p.getGameProfile().getName().equals("RevivalAcceptance"))
                        record("attack cancelled=" + e.isCanceled() + " amount=" + e.getAmount());
                });
    }

    private void check(boolean b, String s) {
        if (!b) throw new AssertionError(s);
    }

    private void record(String s) {
        System.out.println("REVIVAL_ACCEPTANCE " + s);
        report.append(s).append('\n');
    }

    private void server(Runnable job) {
        ready = false;
        Minecraft.getInstance()
                .getSingleplayerServer()
                .execute(
                        () -> {
                            try {
                                job.run();
                                ready = true;
                            } catch (Throwable e) {
                                error = e;
                            }
                        });
    }

    private ServerPlayer player() {
        var mc = Minecraft.getInstance();
        return mc.getSingleplayerServer().getPlayerList().getPlayer(mc.player.getUUID());
    }

    private void capture(String name) {
        var mc = Minecraft.getInstance();
        Screenshot.grab(mc.gameDirectory, name + ".png", mc.getMainRenderTarget(), ignored -> {});
    }

    private void setup() {
        var p = player();
        var level = p.serverLevel();
        setupTime = level.getGameTime();
        deathTriggered = false;
        damageAttempts = 0;
        check(p.server.isHardcore() == (world == 1), "world hardcore mismatch");
        p.setGameMode(GameType.SURVIVAL);
        p.getAbilities().invulnerable = false;
        p.onUpdateAbilities();
        p.removeAllEffects();
        p.setAbsorptionAmount(0);
        p.setHealth(p.getMaxHealth());
        p.getFoodData().setFoodLevel(20);
        p.getInventory().clearContent();
        p.getEnderChestInventory().clearContent();
        var inv = CuriosApi.getCuriosInventory(p).resolve().orElseThrow();
        for (var h : inv.getCurios().values())
            for (int i = 0; i < h.getSlots(); i++) {
                h.getStacks().setStackInSlot(i, ItemStack.EMPTY);
                h.getCosmeticStacks().setStackInSlot(i, ItemStack.EMPTY);
            }
        var diary = new ItemStack(ModRegistry.LONG_TRAVAIL.get());
        LongTravailData.tryInitialize(diary, p);
        for (var a : TravailAspect.values()) LongTravailData.setWitness(diary, a, true);
        inv.getStacksHandler("travel_diary").orElseThrow().getStacks().setStackInSlot(0, diary);
        var book = new ItemStack(ModRegistry.BOOK_OF_DEAD.get());
        var other = new ItemStack(ModRegistry.BOOK_OF_DEAD.get());
        selected = UnderworldLedger.identity(book);
        spare = UnderworldLedger.identity(other);
        int location = round >= 12 ? 2 : round % 3;
        if (location == 0) p.getInventory().setItem(10, book);
        if (location == 1) p.getInventory().offhand.set(0, book);
        if (location == 2) p.getEnderChestInventory().setItem(0, book);
        p.getEnderChestInventory().setItem(1, other);
        check(UnderworldItems.equipped(p, false) == book, "wrong selected book");
        level.getGameRules().getRule(GameRules.RULE_KEEPINVENTORY).set(round < 12, p.server);
        level.getGameRules().getRule(GameRules.RULE_DO_IMMEDIATE_RESPAWN).set(false, p.server);
        level.getGameRules().getRule(GameRules.RULE_DOMOBSPAWNING).set(false, p.server);
        for (int x = -4; x <= 4; x++)
            for (int z = -4; z <= 4; z++) {
                level.setBlockAndUpdate(new BlockPos(x, 99, z), Blocks.STONE.defaultBlockState());
                for (int y = 100; y < 104; y++)
                    level.setBlockAndUpdate(new BlockPos(x, y, z), Blocks.AIR.defaultBlockState());
            }
        p.connection.teleport(.5, 100, .5, 0, 0);
        record(
                "setup world="
                        + world
                        + " round="
                        + round
                        + " location="
                        + location
                        + " keep="
                        + (round < 12));
    }

    private void verify() {
        var p = player();
        check(p.isAlive() && !p.isSpectator(), "not revived as living player");
        check(
                p.position().distanceToSqr(new net.minecraft.world.phys.Vec3(.5, 100, .5)) < 16,
                "revival moved from death site");
        var ledger = UnderworldLedger.get(p.server);
        check(!ledger.deaths.containsKey(p.getUUID()), "pending death remained");
        check(ledger.spent(selected) == (world == 1), "wrong selected book consumption");
        check(!ledger.spent(spare), "spare book consumed");
        check(
                !p.getEnderChestInventory().getItem(1).isEmpty()
                        && UnderworldLedger.identity(p.getEnderChestInventory().getItem(1))
                                .equals(spare),
                "spare book missing");
        if (world == 1) {
            for (int i = 0; i < p.getInventory().getContainerSize(); i++)
                check(
                        !ledger.consumed(p.getInventory().getItem(i)),
                        "spent book left in inventory");
            for (int i = 0; i < p.getEnderChestInventory().getContainerSize(); i++)
                check(
                        !ledger.consumed(p.getEnderChestInventory().getItem(i)),
                        "spent book left in ender chest");
        }
        record(
                "revival PASS world="
                        + world
                        + " round="
                        + round
                        + " health="
                        + p.getHealth()
                        + " consumed="
                        + ledger.spent(selected));
    }

    private void tick(TickEvent.ClientTickEvent e) {
        if (done || e.phase != TickEvent.Phase.END) return;
        var mc = Minecraft.getInstance();
        try {
            if (error != null) throw new AssertionError("server fixture", error);
            if (++ticks > 60000) throw new AssertionError("global timeout");
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
                wait = 0;
                mc.createWorldOpenFlows()
                        .loadLevel(
                                new TitleScreen(),
                                world == 0 ? "revival-normal" : "revival-hardcore");
            } else if (phase == 1
                    && mc.player != null
                    && mc.level != null
                    && mc.getSingleplayerServer() != null) {
                phase = 2;
                wait = 0;
                server(
                        () -> {
                            if (world == 0) {
                                try {
                                    AcquisitionPressure.run(player().serverLevel());
                                    record("fullpack pressure PASS");
                                } catch (Exception ex) {
                                    throw new RuntimeException(ex);
                                }
                            }
                        });
            } else if (phase == 2 && ready) {
                phase = 3;
                wait = 0;
                server(this::setup);
            } else if (phase == 3 && ready) {
                if (deathTriggered) {
                    phase = 4;
                    wait = 0;
                } else if (++wait % 20 == 0)
                    server(
                            () -> {
                                var p = player();
                                if (p.serverLevel().getGameTime() - setupTime < 120) return;
                                p.setHealth(1);
                                p.invulnerableTime = 0;
                                record(
                                        "damage before ticks="
                                                + p.tickCount
                                                + " health="
                                                + p.getHealth()
                                                + " invulnerable="
                                                + p.isInvulnerable());
                                record("effects before damage=" + p.getActiveEffects());
                                boolean hit = p.hurt(p.damageSources().generic(), 10000);
                                if (!hit && !p.isDeadOrDying()) {
                                    record(
                                            "damage rejected attempt="
                                                    + (++damageAttempts)
                                                    + " health="
                                                    + p.getHealth()
                                                    + " effects="
                                                    + p.getActiveEffects()
                                                    + " invulnerableTo="
                                                    + p.isInvulnerableTo(
                                                            p.damageSources().generic()));
                                    check(damageAttempts < 5, "persistent damage rejection");
                                    setupTime = p.serverLevel().getGameTime();
                                    return;
                                }
                                check(p.isDeadOrDying(), "damage did not kill");
                                deathTriggered = true;
                            });
            } else if (phase == 4) {
                if (++wait > 600)
                    throw new AssertionError("death screen/button timeout screen=" + mc.screen);
                if (ready && mc.screen instanceof DeathScreen && wait > 50) {
                    var button =
                            mc.screen.children().stream()
                                    .filter(x -> x instanceof Button)
                                    .map(x -> (Button) x)
                                    .filter(b -> b.getMessage().getString().contains("死者苏生"))
                                    .findFirst()
                                    .orElse(null);
                    if (button != null && button.active) {
                        capture("death-" + world + "-" + round);
                        record("death screen button active world=" + world + " round=" + round);
                        var deathScreen = mc.screen;
                        check(
                                deathScreen.mouseClicked(
                                        button.getX() + button.getWidth() / 2.,
                                        button.getY() + button.getHeight() / 2.,
                                        0),
                                "button hit failed");
                        deathScreen.mouseReleased(
                                button.getX() + button.getWidth() / 2.,
                                button.getY() + button.getHeight() / 2.,
                                0);
                        clicks++;
                        phase = 5;
                        wait = 0;
                    }
                }
            } else if (phase == 5) {
                if (++wait > 600) throw new AssertionError("revival timeout");
                if (wait > 60
                        && mc.player != null
                        && mc.player.isAlive()
                        && !(mc.screen instanceof DeathScreen)) {
                    phase = 6;
                    server(this::verify);
                }
            } else if (phase == 6 && ready) {
                capture("revived-" + world + "-" + round);
                round++;
                if (round < 16) {
                    phase = 2;
                } else if (world == 0) {
                    world = 1;
                    round = 0;
                    phase = 7;
                    wait = 0;
                    mc.level.disconnect();
                    mc.clearLevel(new TitleScreen());
                    mc.setScreen(new TitleScreen());
                } else {
                    record(
                            "PASS clicks="
                                    + clicks
                                    + " startWorld="
                                    + Integer.getInteger("revival.startWorld", 0)
                                    + " finalWorld="
                                    + world);
                    finish(true);
                }
            } else if (phase == 7 && ++wait > 80 && mc.screen instanceof TitleScreen) {
                phase = 0;
            }
        } catch (Throwable ex) {
            ex.printStackTrace();
            record("FAIL " + ex);
            finish(false);
        }
    }

    private void finish(boolean ok) {
        done = true;
        try {
            Files.writeString(Path.of("client-checks.txt"), report);
            Files.writeString(Path.of("client-result.txt"), ok ? "PASS" : "FAIL");
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        Minecraft.getInstance().stop();
    }
}
