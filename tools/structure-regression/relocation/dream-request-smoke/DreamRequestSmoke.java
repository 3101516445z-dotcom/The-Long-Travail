package travail.smoke;

import com.mojang.authlib.GameProfile;
import com.thelongtravail.TravailAspect;
import com.thelongtravail.boundless.dream.DreamState;
import com.thelongtravail.config.*;
import com.thelongtravail.journey.LongTravailData;
import com.thelongtravail.network.BoundlessPacket;
import com.thelongtravail.registry.ModRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.network.*;
import net.minecraft.network.protocol.*;
import net.minecraft.network.protocol.game.ClientboundCustomPayloadPacket;
import net.minecraft.server.level.*;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.*;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.type.capability.ICurio;
import top.theillusivec4.curios.common.inventory.CurioStacksHandler;
import java.util.*;

@Mod("travail_smoke")
public final class DreamRequestSmoke {
    private static void check(boolean ok, String why) {
        if (!ok) throw new AssertionError(why);
    }

    private static final class Wire extends Connection {
        boolean open = true;
        final List<String> sent = new ArrayList<>();

        Wire() {
            super(PacketFlow.SERVERBOUND);
        }

        @Override
        public boolean isConnected() {
            return open;
        }

        @Override
        public void send(Packet<?> packet, net.minecraft.network.PacketSendListener listener) {
            send(packet);
        }

        @Override
        public void send(Packet<?> packet) {
            if (packet instanceof ClientboundCustomPayloadPacket) {
                var buffer = new FriendlyByteBuf(io.netty.buffer.Unpooled.buffer());
                try {
                    packet.write(buffer);
                    byte[] bytes = new byte[buffer.readableBytes()];
                    buffer.readBytes(bytes);
                    sent.add(Base64.getEncoder().encodeToString(bytes));
                } finally {
                    buffer.release();
                }
            }
        }
    }

    private static final class Player extends ServerPlayer {
        final Wire wire = new Wire();
        final List<String> sent = wire.sent;

        Player(ServerLevel level, UUID id) {
            super(level.getServer(), level, new GameProfile(id, "DreamPacketTest"));
            connection =
                    new ServerGamePacketListenerImpl(level.getServer(), wire, this) {
                        @Override
                        public void send(Packet<?> packet) {
                            wire.send(packet);
                        }
                    };
            wire.setListener(connection);
            setPos(.5, 80, .5);
            var inv = CuriosApi.getCuriosInventory(this).resolve().orElseThrow();
            var diarySlots =
                    new CurioStacksHandler(
                            inv, "travel_diary", 1, true, false, true, ICurio.DropRule.DEFAULT);
            var ringSlots =
                    new CurioStacksHandler(
                            inv, "ring", 1, true, false, true, ICurio.DropRule.DEFAULT);
            inv.setCurios(new HashMap<>(Map.of("travel_diary", diarySlots, "ring", ringSlots)));
            var diary = new ItemStack(ModRegistry.LONG_TRAVAIL.get());
            LongTravailData.initialize(diary, this);
            LongTravailData.setWitness(diary, TravailAspect.BOUNDLESS, true);
            diarySlots.getStacks().setStackInSlot(0, diary);
            ringSlots
                    .getStacks()
                    .setStackInSlot(0, new ItemStack(ModRegistry.FREE_AND_EASY_WANDERING.get()));
        }
    }

    private ServerLevel level;
    private Map<UUID, ServerPlayer> players;
    private final List<Player> fixtures = new ArrayList<>();
    private final Deque<Runnable> cases = new ArrayDeque<>();
    private Runnable verify;
    private int completed;

    public DreamRequestSmoke() {
        MinecraftForge.EVENT_BUS.addListener(this::start);
        MinecraftForge.EVENT_BUS.addListener(this::tick);
    }

    @SuppressWarnings("unchecked")
    private void start(ServerStartedEvent event) {
        try {
            TravailConfig.SPECS
                    .values()
                    .forEach(
                            spec ->
                                    spec.setConfig(
                                            com.electronwill.nightconfig.core.CommentedConfig
                                                    .inMemory()));
            level = event.getServer().overworld();
            level.getChunk(0, 0);
            level.setBlock(new BlockPos(0, 79, 0), Blocks.STONE.defaultBlockState(), 3);
            var f = net.minecraft.server.players.PlayerList.class.getDeclaredField("playersByUUID");
            f.setAccessible(true);
            players = (Map<UUID, ServerPlayer>) f.get(event.getServer().getPlayerList());
            cases.add(this::universalSlots);
            cases.add(this::countdown);
            cases.add(this::timeStopCommand);
            cases.add(() -> valid(60, 0, 3)); // 同刻进入、退出、冷却提示。
            cases.add(() -> valid(0, 0, 4)); // 零冷却下每次按键均须生效。
            cases.add(() -> valid(60, 123, 8)); // 重复冷却提示不可合并。
            for (String mode :
                    List.of(
                            "disconnect",
                            "logout",
                            "respawn",
                            "connection-replaced",
                            "removed",
                            "late-login")) cases.add(() -> invalid(mode));
            cases.add(this::equipmentChanged);
        } catch (Throwable failure) {
            finish(failure);
        }
    }

    private void universalSlots() {
        var p = player();
        var inv = CuriosApi.getCuriosInventory(p).resolve().orElseThrow();
        var item =
                (com.thelongtravail.item.BoundlessAccessoryItem)
                        ModRegistry.FREE_AND_EASY_WANDERING.get();
        var stack = new ItemStack(item);
        inv.getStacksHandler("ring").orElseThrow().getStacks().setStackInSlot(0, ItemStack.EMPTY);
        int checked = 0;
        for (String slot : CuriosApi.getEntitySlots(p).keySet()) {
            var context = new top.theillusivec4.curios.api.SlotContext(slot, p, 0, false, true);
            check(CuriosApi.isStackValid(context, stack), "universal tag accepts " + slot);
            check(item.canEquip(context, stack), "item allows " + slot);
            checked++;
        }
        check(checked >= 9, "all configured accessory slot types checked");
        check(
                !item.canEquip(
                        new top.theillusivec4.curios.api.SlotContext("ring", p, 0, true, true),
                        stack),
                "cosmetic rejected");
        inv.getStacksHandler("ring").orElseThrow().getStacks().setStackInSlot(0, stack);
        check(
                !item.canEquip(
                        new top.theillusivec4.curios.api.SlotContext("head", p, 0, false, true),
                        stack),
                "duplicate across slots rejected");
        check(
                com.thelongtravail.boundless.BoundlessEquipment.equipped(p, true),
                "ring equipment activates skill");
        DreamState.cast(p);
        check(
                DreamState.active(p) && p.gameMode.getGameModeForPlayer() == GameType.SPECTATOR,
                "skill casts from ring slot");
        DreamState.finish(p);
        var star = (com.thelongtravail.item.BoundlessAccessoryItem) ModRegistry.STAR_VOICE.get();
        check(
                star.canEquip(
                        new top.theillusivec4.curios.api.SlotContext("ring", p, 0, false, true),
                        new ItemStack(star)),
                "star voice accepts a functional ring slot under the universal-slot contract");
        System.out.println(
                "FREE_WANDERING_SLOTS_PASS: "
                        + checked
                        + " slot types, ring skill, cosmetic and duplicate restrictions");
    }

    private List<Integer> countdowns(Player p) {
        var result = new ArrayList<Integer>();
        for (String encoded : p.sent) {
            var buffer =
                    new FriendlyByteBuf(
                            io.netty.buffer.Unpooled.wrappedBuffer(
                                    Base64.getDecoder().decode(encoded)));
            try {
                buffer.readResourceLocation();
                if (buffer.readUnsignedByte() == 15) {
                    var message = BoundlessPacket.Message.decode(buffer);
                    if (message.key().equals("message.the_long_travail.dream.countdown"))
                        result.add(message.cooldownTicks());
                }
            } finally {
                buffer.release();
            }
        }
        return result;
    }

    private void countdown() {
        double duration = BoundlessItemsConfig.NUMBERS.get("dream.duration").get();
        try {
            for (int total : new int[] {200, 100, 90, 60, 1}) {
                BoundlessItemsConfig.NUMBERS.get("dream.duration").set(total / 20D);
                var p = player();
                reset(p, 0);
                DreamState.cast(p);
                var expected = new ArrayList<Integer>();
                if (total <= 100) expected.add(total);
                check(countdowns(p).equals(expected), "initial short dream countdown " + total);
                for (int left = total - 1; left >= 0; left--) {
                    DreamState.tick(p);
                    if (left > 0 && left <= 100 && left % 20 == 0) expected.add(left);
                    check(
                            countdowns(p).equals(expected),
                            "countdown boundary " + total + "/" + left);
                }
                check(!DreamState.active(p), "natural wake " + total);
                DreamState.tick(p);
                check(countdowns(p).equals(expected), "no countdown after wake");
            }
            BoundlessItemsConfig.NUMBERS.get("dream.duration").set(10D);
            var p = player();
            reset(p, 0);
            DreamState.cast(p);
            DreamState.finish(p, true);
            for (int i = 0; i < 200; i++) DreamState.tick(p);
            check(countdowns(p).isEmpty(), "early wake cancels countdown");
            System.out.println(
                    "DREAM_COUNTDOWN_PASS: every boundary, 5/4/3/2/1, short durations, no zero, early wake");
        } finally {
            BoundlessItemsConfig.NUMBERS.get("dream.duration").set(duration);
        }
    }

    private void timeStopCommand() {
        var p = player();
        var manager = com.thelongtravail.boundless.timestop.TimeStopManager.get(level);
        var dispatcher = level.getServer().getCommands().getDispatcher();
        var source = p.createCommandSourceStack().withPermission(2);
        var cow =
                new net.minecraft.world.entity.animal.Cow(
                        net.minecraft.world.entity.EntityType.COW, level);
        cow.setPos(p.getX() + 2, p.getY(), p.getZ());
        try {
            CuriosApi.getCuriosInventory(p).resolve().orElseThrow().setCurios(new HashMap<>());
            for (String command : List.of("tlt timestop 0", "tlt timestop 16")) {
                boolean rejected = false;
                try {
                    dispatcher.execute(command, source);
                } catch (com.mojang.brigadier.exceptions.CommandSyntaxException expected) {
                    rejected = true;
                }
                check(rejected, "invalid duration rejected");
            }
            boolean denied = false;
            try {
                dispatcher.execute("tlt timestop", source.withPermission(1));
            } catch (com.mojang.brigadier.exceptions.CommandSyntaxException expected) {
                denied = true;
            }
            check(denied, "permission level 1 denied");
            p.setGameMode(GameType.SPECTATOR);
            check(dispatcher.execute("tlt timestop", source) == 0, "spectator denied");
            p.setGameMode(GameType.SURVIVAL);
            check(
                    dispatcher.execute("tlt timestop 2", source) == 1,
                    "level 2 starts without equipment");
            check(
                    com.thelongtravail.boundless.timestop.TimeStopManager.frozen(cow),
                    "nearby enemy frozen");
            check(
                    !com.thelongtravail.boundless.timestop.TimeStopManager.frozen(p),
                    "caster exempt");
            check(dispatcher.execute("tlt timestop", source) == 0, "group lock respected");
            manager.expire(level.getGameTime() + 39);
            check(
                    com.thelongtravail.boundless.timestop.TimeStopManager.frozen(cow),
                    "two seconds not expired early");
            manager.expire(level.getGameTime() + 40);
            check(
                    !com.thelongtravail.boundless.timestop.TimeStopManager.frozen(cow),
                    "two seconds releases");
            check(
                    dispatcher.execute("tlt timestop", source) == 1,
                    "default command starts despite skill cooldown");
            int ticks = BoundlessItemsConfig.ticks("star.witnessDuration");
            manager.expire(level.getGameTime() + ticks - 1);
            check(
                    com.thelongtravail.boundless.timestop.TimeStopManager.frozen(cow),
                    "default duration from config");
            manager.expire(level.getGameTime() + ticks);
            check(
                    !com.thelongtravail.boundless.timestop.TimeStopManager.frozen(cow),
                    "default duration expires");
            System.out.println(
                    "TIMESTOP_COMMAND_PASS: permission, bounds, spectator, no equipment, freeze, exemption, group lock, durations, cooldown bypass");
        } catch (Exception failure) {
            throw new RuntimeException(failure);
        } finally {
            com.thelongtravail.boundless.timestop.TimeStopManager.clear();
            cow.discard();
        }
    }

    private Player player() {
        var p = new Player(level, UUID.randomUUID());
        fixtures.add(p);
        players.put(p.getUUID(), p);
        return p;
    }

    private static void reset(Player p, int cooldown) {
        DreamState.finish(p);
        p.setGameMode(GameType.SURVIVAL);
        p.setPos(.5, 80, .5);
        p.getPersistentData().putInt(DreamState.COOLDOWN, cooldown);
        p.sent.clear();
    }

    private static void enqueue(Wire wire, int count) {
        var error = new java.util.concurrent.atomic.AtomicReference<Throwable>();
        Thread network =
                new Thread(
                        () -> {
                            try {
                                var ctor =
                                        NetworkEvent.Context.class.getDeclaredConstructor(
                                                Connection.class,
                                                NetworkDirection.class,
                                                int.class);
                                ctor.setAccessible(true);
                                for (int i = 0; i < count; i++) {
                                    var context =
                                            ctor.newInstance(
                                                    wire, NetworkDirection.PLAY_TO_SERVER, 9);
                                    BoundlessPacket.Cast.handle(
                                            new BoundlessPacket.Cast(), () -> context);
                                    check(context.getPacketHandled(), "packet acknowledged");
                                }
                            } catch (Throwable failure) {
                                error.set(failure);
                            }
                        },
                        "dream-packet-test");
        network.start();
        try {
            network.join(3000);
        } catch (InterruptedException failure) {
            throw new AssertionError(failure);
        }
        check(!network.isAlive(), "network handler never waits for server execution");
        if (error.get() != null) throw new AssertionError(error.get());
    }

    private void valid(double cooldown, int initial, int presses) {
        BoundlessItemsConfig.NUMBERS.get("dream.cooldown").set(cooldown);
        var p = player();
        reset(p, initial);
        for (int i = 0; i < presses; i++) DreamState.cast(p);
        var expected = List.copyOf(p.sent);
        var data = p.getPersistentData().copy();
        var mode = p.gameMode.getGameModeForPlayer();
        check(!expected.isEmpty(), "baseline emits state/messages");
        if (initial > 0)
            check(
                    expected.size() == presses,
                    "baseline refreshes cooldown message for every press");
        reset(p, initial);
        enqueue(p.wire, presses);
        check(
                p.sent.isEmpty() && !DreamState.active(p),
                "work remains queued until server resumes");
        verify =
                () -> {
                    check(
                            p.sent.equals(expected),
                            "outbound payload bytes and order match direct casts: " + presses);
                    check(
                            p.getPersistentData().equals(data)
                                    && p.gameMode.getGameModeForPlayer() == mode,
                            "state and cooldown match direct casts");
                    System.out.println(
                            "DREAM_VALID_REQUEST_PASS cooldown="
                                    + cooldown
                                    + " initial="
                                    + initial
                                    + " presses="
                                    + presses
                                    + " payloads="
                                    + expected.size());
                };
    }

    private void invalid(String mode) {
        BoundlessItemsConfig.NUMBERS.get("dream.cooldown").set(0D);
        var p = player();
        reset(p, 0);
        Player replacement = null;
        Wire source = mode.equals("late-login") ? new Wire() : p.wire;
        enqueue(source, 1);
        switch (mode) {
            case "disconnect" -> p.wire.open = false;
            case "logout" -> players.remove(p.getUUID());
            case "respawn" -> {
                replacement = new Player(level, p.getUUID());
                fixtures.add(replacement);
                replacement.connection = p.connection;
                p.connection.player = replacement;
                players.put(p.getUUID(), replacement);
            }
            case "connection-replaced" -> {
                var other = new Wire();
                p.connection =
                        new ServerGamePacketListenerImpl(level.getServer(), other, p) {
                            @Override
                            public void send(Packet<?> packet) {}
                        };
                other.setListener(p.connection);
            }
            case "removed" -> p.discard();
            case "late-login" -> source.setListener(p.connection);
        }
        var next = replacement;
        var before = p.getPersistentData().copy();
        verify =
                () -> {
                    check(
                            !DreamState.active(p)
                                    && before.equals(p.getPersistentData())
                                    && p.sent.isEmpty(),
                            "stale request has no effect: " + mode);
                    if (next != null)
                        check(
                                !DreamState.active(next) && next.sent.isEmpty(),
                                "old packet never redirected to replacement player");
                    System.out.println("DREAM_STALE_REQUEST_PASS " + mode);
                };
    }

    private void equipmentChanged() {
        var p = player();
        reset(p, 0);
        enqueue(p.wire, 1);
        CuriosApi.getCuriosInventory(p)
                .resolve()
                .orElseThrow()
                .getStacksHandler("ring")
                .orElseThrow()
                .getStacks()
                .setStackInSlot(0, ItemStack.EMPTY);
        verify =
                () -> {
                    check(
                            !DreamState.active(p) && p.sent.isEmpty(),
                            "equipment checked at execution, never cached at receipt");
                    System.out.println("DREAM_EXECUTION_VALIDATION_PASS");
                };
    }

    private void tick(TickEvent.ServerTickEvent event) {
        if (level == null || event.phase != TickEvent.Phase.END) return;
        try {
            if (verify != null) {
                verify.run();
                verify = null;
                completed++;
            }
            if (cases.isEmpty()) {
                finish(null);
                return;
            }
            cases.removeFirst().run();
        } catch (Throwable failure) {
            finish(failure);
        }
    }

    private void finish(Throwable failure) {
        if (failure != null) failure.printStackTrace();
        else System.out.println("DREAM_REQUEST_PASS cases=" + completed);
        if (players != null) for (var p : fixtures) players.remove(p.getUUID(), p);
        try {
            java.nio.file.Files.writeString(
                    java.nio.file.Path.of("dream-request-result.txt"),
                    failure == null ? "PASS" : "FAIL");
        } catch (Exception error) {
            throw new RuntimeException(error);
        }
        var server = net.minecraftforge.server.ServerLifecycleHooks.getCurrentServer();
        level = null;
        server.halt(false);
    }
}
