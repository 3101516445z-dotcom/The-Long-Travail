package travail.smoke;

import com.thelongtravail.combat.DamageReplay;
import com.thelongtravail.registry.ModRegistry;

import net.minecraft.server.level.*;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;

import top.theillusivec4.curios.common.inventory.CurioStacksHandler;

import java.nio.file.*;
import java.util.*;

final class SunBladeSoak {
    final ServerLevel level;
    final ServerPlayer player;
    final CurioStacksHandler belt;
    final List<Cow> targets = new ArrayList<>();
    final Map<String, List<Double>> timings = new LinkedHashMap<>();
    int age, attacks;
    long started, tickStart;
    final int duration;

    SunBladeSoak(ServerLevel l, ServerPlayer p, CurioStacksHandler b, int ticks) {
        level = l;
        player = p;
        belt = b;
        duration = ticks;
        started = System.nanoTime();
        MinecraftForge.EVENT_BUS.addListener(this::tick);
    }

    void tick(TickEvent.ServerTickEvent e) {
        if (e.getServer() != level.getServer() || age < 0) return;
        if (e.phase == TickEvent.Phase.START) {
            tickStart = System.nanoTime();
            return;
        }
        String stage =
                age < 600
                        ? "warmup"
                        : age < 1800 ? "baseline" : age < duration - 600 ? "equipped" : "cooldown";
        try {
            if (age == 0)
                for (int i = 0; i < 500; i++) {
                    Cow t = new Cow(EntityType.COW, level);
                    t.setPos(4 + (i % 25) * 3, 100, 4 + (i / 25) * 3);
                    level.setChunkForced(
                            t.blockPosition().getX() >> 4, t.blockPosition().getZ() >> 4, true);
                    t.setNoAi(true);
                    t.setNoGravity(true);
                    t.getAttribute(Attributes.MAX_HEALTH).setBaseValue(1000);
                    t.setHealth(1000);
                    level.addFreshEntity(t);
                    targets.add(t);
                }
            if (age == 0) {
                player.getInventory().setItem(0, ItemStack.EMPTY);
                belt.getStacks().setStackInSlot(0, ItemStack.EMPTY);
            }
            if (age == 1800) {
                player.getInventory().setItem(0, new ItemStack(ModRegistry.SUN_CHASER.get()));
                belt.getStacks().setStackInSlot(0, new ItemStack(ModRegistry.KAGOTSURUBE.get()));
            }
            if (age == duration - 600) {
                for (Cow t : targets) t.discard();
                targets.clear();
            }
            if (!stage.equals("cooldown"))
                for (int n = 0; n < 25; n++) {
                    Cow t = targets.get(attacks % targets.size());
                    t.setHealth(1000);
                    t.invulnerableTime = 0;
                    t.setSecondsOnFire(10);
                    t.hurt(player.damageSources().playerAttack(player), 1);
                    attacks++;
                    float expected = stage.equals("equipped") ? 974 : 999;
                    if (Math.abs(t.getHealth() - expected) > .001)
                        throw new AssertionError(
                                "soak damage age="
                                        + age
                                        + " actual="
                                        + t.getHealth()
                                        + " expected="
                                        + expected);
                }
            timings.computeIfAbsent(stage, k -> new ArrayList<>())
                    .add((System.nanoTime() - tickStart) / 1e6);
            age++;
            if (age % 1200 == 0) {
                Files.writeString(
                        Path.of("sun-blade-soak-progress.txt"),
                        "ticks="
                                + age
                                + "/"
                                + duration
                                + " attacks="
                                + attacks
                                + " heap="
                                + (Runtime.getRuntime().totalMemory()
                                        - Runtime.getRuntime().freeMemory()));
            }
            if (age >= duration) {
                var f = DamageReplay.class.getDeclaredField("QUEUES");
                f.setAccessible(true);
                var queue = ((Map<?, ?>) f.get(null)).get(level.getServer());
                if (queue instanceof Collection<?> c && !c.isEmpty())
                    throw new AssertionError("replay queue did not drain");
                List<String> lines = new ArrayList<>();
                lines.add(
                        "PASS ticks="
                                + age
                                + " attacks="
                                + attacks
                                + " wallSeconds="
                                + (System.nanoTime() - started) / 1e9);
                for (var entry : timings.entrySet()) {
                    var v = entry.getValue();
                    Collections.sort(v);
                    lines.add(
                            entry.getKey()
                                    + " count="
                                    + v.size()
                                    + " median="
                                    + v.get(v.size() / 2)
                                    + " p95="
                                    + v.get((int) (v.size() * .95))
                                    + " p99="
                                    + v.get((int) (v.size() * .99)));
                }
                Files.write(Path.of("sun-blade-soak-result.txt"), lines);
                Files.writeString(
                        Path.of("sun-blade-result.txt"),
                        "PASS soak ticks=" + age + " attacks=" + attacks);
                age = -1;
                level.getServer().halt(false);
            }
        } catch (Throwable ex) {
            ex.printStackTrace();
            try {
                Files.writeString(Path.of("sun-blade-result.txt"), "FAIL soak " + ex);
            } catch (Exception ignored) {
            }
            age = -1;
            level.getServer().halt(false);
        }
    }
}
