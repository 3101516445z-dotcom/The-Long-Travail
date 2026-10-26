package travail.abundanceclient;

import com.mojang.authlib.GameProfile;
import com.thelongtravail.abyss.AbundanceEffectPool;
import com.thelongtravail.abyss.LiquefiedAbundance;
import com.thelongtravail.registry.ModRegistry;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.*;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.registries.ForgeRegistries;

import java.nio.file.*;
import java.util.*;

// 在整包集成服务器中按有限批次执行压力检查。
public final class FullpackAbundanceStress {
    private static MinecraftServer server;
    private static ServerLevel world;
    private static final List<FakePlayer> players = new ArrayList<>();
    private static final Set<String> seen = new TreeSet<>();
    private static final RandomSource random = RandomSource.create(20261027);
    private static int draws, conversions, cancels, full, checks;
    private static long maxBatchNanos, began;
    public static volatile boolean done;
    public static volatile Throwable failure;

    public static void start(MinecraftServer value) {
        server = value;
        server.execute(
                () -> {
                    try {
                        world = server.overworld();
                        world.getChunk(5, 5);
                        began = System.nanoTime();
                        for (int i = 0; i < 4; i++) {
                            var player =
                                    FakePlayerFactory.get(
                                            world,
                                            new GameProfile(
                                                    UUID.nameUUIDFromBytes(
                                                            ("AbundanceStress" + i)
                                                                    .getBytes(
                                                                            java.nio.charset
                                                                                    .StandardCharsets
                                                                                    .UTF_8)),
                                                    "AbundanceStress" + i));
                            var observer = server.getPlayerList().getPlayers().get(0);
                            player.setPos(
                                    observer.getX() + i * 0.5, observer.getY(), observer.getZ());
                            players.add(player);
                        }
                        if (AbundanceEffectPool.ids().isEmpty())
                            throw new AssertionError("Full-pack effect pool empty");
                        Files.writeString(
                                Path.of("stress-pool.txt"),
                                "registered="
                                        + ForgeRegistries.MOB_EFFECTS.getValues().size()
                                        + " eligible="
                                        + AbundanceEffectPool.ids().size()
                                        + "\n"
                                        + String.join("\n", AbundanceEffectPool.ids()));
                    } catch (Throwable error) {
                        fail(error);
                    }
                });
    }

    private static void check(boolean condition, String message) {
        checks++;
        if (!condition) throw new AssertionError(message);
    }

    private static void fail(Throwable error) {
        failure = error;
        done = true;
        error.printStackTrace();
    }

    public static void tick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || world == null || done) return;
        long start = System.nanoTime();
        try {
            if (draws < 100000) {
                for (int i = 0; i < 2000 && draws < 100000; i++, draws++) {
                    var effect = AbundanceEffectPool.sample(random);
                    check(effect != null, "nonempty sample");
                    seen.add(ForgeRegistries.MOB_EFFECTS.getKey(effect.getEffect()).toString());
                    var bottle = LiquefiedAbundance.potion(Items.LINGERING_POTION, effect);
                    var loaded = ItemStack.of(bottle.save(new net.minecraft.nbt.CompoundTag()));
                    var effects = PotionUtils.getMobEffects(loaded);
                    check(effects.size() == 1, "one effect after NBT roundtrip");
                    var restored = effects.get(0);
                    check(
                            restored.getEffect() == effect.getEffect()
                                    && restored.getDuration() == effect.getDuration()
                                    && restored.getAmplifier() == effect.getAmplifier(),
                            "full-pack effect roundtrip");
                    var cloud = LiquefiedAbundance.cloudEffect(loaded, restored);
                    check(
                            cloud.getDuration()
                                    == (restored.getEffect().isInstantenous()
                                            ? restored.getDuration()
                                            : restored.isInfiniteDuration()
                                                    ? -1
                                                    : Math.max(1, restored.getDuration() / 4)),
                            "cloud duration");
                }
            } else {
                for (int i = 0; i < 20 && conversions < 10000; i++, conversions++)
                    convert(conversions);
                if (conversions >= 10000) {
                    check(
                            seen.equals(new TreeSet<>(AbundanceEffectPool.ids())),
                            "all eligible effects reached");
                    Files.writeString(
                            Path.of("server-stress-result.txt"),
                            "PASS checks="
                                    + checks
                                    + " draws="
                                    + draws
                                    + " effects="
                                    + seen.size()
                                    + " conversions="
                                    + conversions
                                    + " cancellations="
                                    + cancels
                                    + " fullInventory="
                                    + full
                                    + " elapsedSeconds="
                                    + (System.nanoTime() - began) / 1e9
                                    + " maxFixtureBatchMs="
                                    + maxBatchNanos / 1e6
                                    + "\nNot a whole-pack TPS/FPS benchmark.");
                    done = true;
                }
            }
            maxBatchNanos = Math.max(maxBatchNanos, System.nanoTime() - start);
        } catch (Throwable error) {
            fail(error);
        }
    }

    private static void convert(int n) {
        FakePlayer player = players.get(n % players.size());
        player.stopUsingItem();
        player.getInventory().clearContent();
        player.getAbilities().instabuild = n % 7 == 0;
        InteractionHand hand = n % 2 == 0 ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND;
        player.setShiftKeyDown(n % 3 == 1);
        player.getPersistentData().putInt(LiquefiedAbundance.MODE_KEY, n % 3);
        Item item = ModRegistry.LIQUEFIED_ABUNDANCE.get();
        ItemStack input = new ItemStack(item, 64);
        player.setItemInHand(hand, input);
        boolean filled = n % 20 == 3;
        if (filled) {
            for (int j = 0; j < player.getInventory().items.size(); j++)
                player.getInventory().items.set(j, new ItemStack(Items.STONE, 64));
            player.setItemInHand(hand, input);
        }
        var previous =
                world.getEntitiesOfClass(ItemEntity.class, player.getBoundingBox().inflate(5));
        item.use(world, player, hand);
        if (n % 10 == 9) {
            LiquefiedAbundance.cancel(player);
            item.releaseUsing(input, world, player, 35);
            check(input.getCount() == 64, "cancel preserves input");
            cancels++;
        } else {
            Item expected =
                    n % 3 == 2
                            ? Items.LINGERING_POTION
                            : n % 3 == 1 ? Items.SPLASH_POTION : Items.POTION;
            item.finishUsingItem(input, world, player);
            check(
                    input.getCount() == (player.getAbilities().instabuild ? 64 : 63),
                    "consumption conservation");
            if (filled) {
                var dropped =
                        new ArrayList<>(
                                world.getEntitiesOfClass(
                                        ItemEntity.class, player.getBoundingBox().inflate(5)));
                dropped.removeAll(previous);
                check(
                        dropped.size() == 1 && dropped.get(0).getItem().is(expected),
                        "full inventory result n="
                                + n
                                + " drops="
                                + dropped.size()
                                + " inventory="
                                + player.getInventory().countItem(expected)
                                + " expected="
                                + expected);
                dropped.forEach(ItemEntity::discard);
                full++;
            } else check(player.getInventory().countItem(expected) == 1, "one output");
            item.releaseUsing(input, world, player, 20);
            check(
                    input.getCount() == (player.getAbilities().instabuild ? 64 : 63),
                    "no duplicate settlement");
        }
        player.stopUsingItem();
        if (n > 0 && n % 2000 == 0) LiquefiedAbundance.reload();
    }
}
