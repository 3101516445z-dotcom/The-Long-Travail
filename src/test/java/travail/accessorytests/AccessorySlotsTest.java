package travail.accessorytests;

import com.mojang.authlib.GameProfile;
import com.thelongtravail.TravailAspect;
import com.thelongtravail.boundless.BoundlessEquipment;
import com.thelongtravail.boundless.StarVoiceState;
import com.thelongtravail.boundless.TimeStopManager;
import com.thelongtravail.config.BoundlessItemsConfig;
import com.thelongtravail.data.LongTravailData;
import com.thelongtravail.farreach.DonQuixoteCombat;
import com.thelongtravail.farreach.DonQuixoteState;
import com.thelongtravail.helper.AccessoryEquipRules;
import com.thelongtravail.helper.AccessoryRestore;
import com.thelongtravail.helper.CurioLookup;
import com.thelongtravail.helper.DependentAccessories;
import com.thelongtravail.registry.ModRegistry;
import com.thelongtravail.valley.Phosphophyllite;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.fml.common.Mod;

import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.type.capability.ICurioItem;
import top.theillusivec4.curios.api.type.capability.ICuriosItemHandler;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/** 在隔离 Forge 服务端验证真实槽位校验、效果入口与恢复链。 */
@Mod("travail_smoke")
public final class AccessorySlotsTest {
    private static final String[] SLOTS = {
        "curio", "ring", "head", "hands", "back", "charm", "slot_probe"
    };
    private static final UUID PHOSPH_ID =
            UUID.nameUUIDFromBytes(
                    "the_long_travail:phosphophyllite".getBytes(StandardCharsets.UTF_8));
    private static int checks;
    private static String context = "initialization";
    private static final Deque<String> history = new ArrayDeque<>();
    private final List<Wearer> soak = new ArrayList<>();
    private MinecraftServer server;
    private int ticks;
    private boolean running;
    private long deadline;

    private static final class Wearer extends ServerPlayer {
        ItemStack diary;

        Wearer(ServerLevel level) {
            this(level, UUID.randomUUID());
        }

        Wearer(ServerLevel level, UUID identity) {
            super(level.getServer(), level, new GameProfile(identity, "SlotTest"));
            var wire =
                    new net.minecraft.network.Connection(
                            net.minecraft.network.protocol.PacketFlow.SERVERBOUND) {
                        @Override
                        public void send(net.minecraft.network.protocol.Packet<?> packet) {}
                    };
            connection =
                    new net.minecraft.server.network.ServerGamePacketListenerImpl(
                            level.getServer(), wire, this) {
                        @Override
                        public void send(net.minecraft.network.protocol.Packet<?> packet) {}
                    };
            setGameMode(GameType.SURVIVAL);
            diary = new ItemStack(ModRegistry.LONG_TRAVAIL.get());
            require(LongTravailData.tryInitialize(diary, this), "diary initialized");
            diary(true);
            for (Item item : targets()) getInventory().add(new ItemStack(item));
        }

        ICuriosItemHandler inv() {
            return CuriosApi.getCuriosInventory(this).resolve().orElseThrow();
        }

        void diary(boolean present) {
            inv().getStacksHandler("travel_diary")
                    .orElseThrow()
                    .getStacks()
                    .setStackInSlot(0, present ? diary : ItemStack.EMPTY);
        }

        void phase(boolean witness) {
            for (TravailAspect aspect : TravailAspect.values())
                LongTravailData.setWitness(diary, aspect, witness);
        }

        void move(Item item, String destination) {
            ItemStack moving = ItemStack.EMPTY;
            for (var handler : inv().getCurios().values()) {
                for (int i = 0; i < handler.getStacks().getSlots(); i++) {
                    if (handler.getStacks().getStackInSlot(i).is(item)) {
                        require(moving.isEmpty(), "no duplicate source");
                        moving = handler.getStacks().getStackInSlot(i);
                        handler.getStacks().setStackInSlot(i, ItemStack.EMPTY);
                    }
                }
            }
            for (int i = 0; i < getInventory().getContainerSize(); i++) {
                if (getInventory().getItem(i).is(item)) {
                    require(moving.isEmpty(), "no duplicate inventory source");
                    moving = getInventory().getItem(i);
                    getInventory().setItem(i, ItemStack.EMPTY);
                }
            }
            require(!moving.isEmpty(), "source conserved");
            if (destination != null) {
                var stacks = inv().getStacksHandler(destination).orElseThrow().getStacks();
                for (int i = 0; i < stacks.getSlots(); i++) {
                    var slot = new SlotContext(destination, this, i, false, true);
                    if (stacks.getStackInSlot(i).isEmpty()
                            && CuriosApi.isStackValid(slot, moving)
                            && ((ICurioItem) item).canEquip(slot, moving)) {
                        moving = stacks.insertItem(i, moving, false);
                        break;
                    }
                }
            }
            if (!moving.isEmpty()) require(getInventory().add(moving), "inventory accepts return");
        }

        void reload() {
            var tag = inv().writeTag();
            inv().readTag(tag);
            inv().handleInvalidStacks();
            var restored = inv().findFirstCurio(ModRegistry.LONG_TRAVAIL.get());
            if (restored.isPresent()) diary = restored.get().stack();
        }
    }

    private static List<Item> targets() {
        return List.of(
                ModRegistry.DON_QUIXOTE.get(),
                ModRegistry.STAR_VOICE.get(),
                ModRegistry.PHOSPHOPHYLLITE.get());
    }

    private static void require(boolean ok, String message) {
        checks++;
        if (!ok) throw new AssertionError(context + ": " + message + " history=" + history);
    }

    private static void near(double actual, double expected, String message) {
        require(
                Math.abs(actual - expected) < 0.00001,
                message + " expected=" + expected + " actual=" + actual);
    }

    // 独立遍历实际功能格，不使用待测查询工具构造预期。
    private static boolean functional(Wearer p, Item item, String slot) {
        for (var entry : p.inv().getCurios().entrySet()) {
            if (slot != null && !slot.equals(entry.getKey())) continue;
            var stacks = entry.getValue().getStacks();
            for (int i = 0; i < stacks.getSlots(); i++)
                if (stacks.getStackInSlot(i).is(item)) return true;
        }
        return false;
    }

    private static void checkState(Wearer p) {
        boolean diary = functional(p, ModRegistry.LONG_TRAVAIL.get(), "travel_diary");
        for (Item item : targets()) {
            boolean expected = functional(p, item, null);
            require(CurioLookup.hasFunctional(p, item) == expected, "functional query " + item);
            require(
                    AccessoryEquipRules.hasEquipped(p, item) == expected,
                    "restore-aware query " + item);
            require(
                    AccessoryEquipRules.hasEquipped(p, "curio", item)
                            == functional(p, item, "curio"),
                    "old fixed-slot query " + item);
            int count = p.getInventory().countItem(item);
            for (var h : p.inv().getCurios().values())
                for (int i = 0; i < h.getStacks().getSlots(); i++)
                    if (h.getStacks().getStackInSlot(i).is(item))
                        count += h.getStacks().getStackInSlot(i).getCount();
            require(count == 1, "quantity conserved " + item + " count=" + count);
        }
        require(
                DonQuixoteCombat.equipped(p) == functional(p, targets().get(0), null),
                "quixote detected");
        require(
                BoundlessEquipment.equipped(p, false)
                        == (diary && functional(p, targets().get(1), null)),
                "star detected with prerequisite");
        int expectedPhase =
                diary && functional(p, targets().get(2), null)
                        ? (LongTravailData.hasWitness(p.diary, TravailAspect.DEEP_VALLEY) ? 2 : 1)
                        : 0;
        require(Phosphophyllite.phase(p) == expectedPhase, "phosphophyllite phase");
        Phosphophyllite.tick(p);
        var modifier = p.getAttribute(Attributes.MAX_HEALTH).getModifier(PHOSPH_ID);
        require((modifier != null) == (expectedPhase != 0), "attribute cleanup/apply");
        if (modifier != null) {
            double expected =
                    com.thelongtravail.config.DeepValleyItemsConfig.get(
                            "phosphophyllite."
                                    + (expectedPhase == 2
                                            ? "witnessAttributes"
                                            : "maliceAttributes"));
            near(modifier.getAmount(), expected, "attribute amount does not accumulate");
        }
    }

    private static void matrix(ServerLevel level) {
        for (Item item : targets())
            for (String slot : SLOTS)
                for (int index = 0; index < 2; index++) {
                    context = "matrix item=" + item + " slot=" + slot + " index=" + index;
                    var p = new Wearer(level);
                    var stack = new ItemStack(item);
                    var target = new SlotContext(slot, p, index, false, true);
                    require(CuriosApi.isStackValid(target, stack), "Curios tag validation");
                    require(
                            ((ICurioItem) item).canEquip(target, stack),
                            "functional slot accepted");
                    require(
                            !((ICurioItem) item)
                                    .canEquip(new SlotContext(slot, p, index, true, true), stack),
                            "cosmetic slot rejected");
                    p.diary(false);
                    require(
                            ((ICurioItem) item).canEquip(target, stack)
                                    == (item == targets().get(0)),
                            "original diary prerequisite retained");
                    p.diary(true);
                    p.move(item, slot);
                    require(functional(p, item, slot), "actual insert accepted");
                    String other = slot.equals("ring") ? "head" : "ring";
                    require(
                            !((ICurioItem) item)
                                    .canEquip(new SlotContext(other, p, 0, false, true), stack),
                            "cross-slot duplicate rejected");
                    p.reload();
                    require(functional(p, item, slot), "real readTag preserves slot");
                    p.diary(false);
                    DependentAccessories.reconcile(p);
                    require(
                            functional(p, item, null) == (item == targets().get(0)),
                            "dependency removal");
                    checkState(p);
                    p.move(item, null);
                    Phosphophyllite.tick(p);
                }
        var p = new Wearer(level);
        for (Item item :
                List.of(
                        ModRegistry.FREE_AND_EASY_WANDERING.get(),
                        ModRegistry.EVERLASTING_FLOWER.get()))
            for (String slot : SLOTS)
                require(
                        ((ICurioItem) item)
                                .canEquip(
                                        new SlotContext(slot, p, 0, false, true),
                                        new ItemStack(item)),
                        "existing universal item unchanged");
        var azrael = ModRegistry.AZRAEL.get();
        require(
                !((ICurioItem) azrael)
                        .canEquip(
                                new SlotContext("ring", p, 0, false, true), new ItemStack(azrael)),
                "dedicated head item still rejects ring");
        for (Item item : targets()) {
            var denied = new SlotContext("slot_denied", p, 0, false, true);
            var reference = new ItemStack(ModRegistry.FREE_AND_EASY_WANDERING.get());
            boolean nativeResult = CuriosApi.isStackValid(denied, reference);
            require(
                    !CuriosApi.testCurioPredicates(
                            CuriosApi.getSlot("slot_denied", level).orElseThrow().getValidators(),
                            new top.theillusivec4.curios.api.SlotResult(denied, reference)),
                    "custom validator itself rejects");
            require(
                    CuriosApi.isStackValid(denied, new ItemStack(item)) == nativeResult,
                    "native universal-tag fallback matches wandering");
            require(
                    p.inv()
                                    .getStacksHandler("slot_denied")
                                    .orElseThrow()
                                    .getStacks()
                                    .insertItem(0, new ItemStack(item), true)
                                    .isEmpty()
                            == nativeResult,
                    "actual insertion follows native fallback");
        }
        for (Item item : targets()) {
            var cosmetic = p.inv().getStacksHandler("ring").orElseThrow().getCosmeticStacks();
            cosmetic.setStackInSlot(0, new ItemStack(item));
            require(!CurioLookup.hasFunctional(p, item), "forced cosmetic data ignored");
            try (var scope = new AccessoryRestore(p, p.inv().writeTag())) {
                require(!AccessoryEquipRules.hasEquipped(p, item), "snapshot excludes cosmetics");
            }
            cosmetic.setStackInSlot(0, ItemStack.EMPTY);
        }
        p.move(targets().get(2), "ring");
        try (var outer = new AccessoryRestore(p, p.inv().writeTag())) {
            p.move(targets().get(2), null);
            require(
                    AccessoryEquipRules.hasEquipped(p, targets().get(2)),
                    "incoming snapshot takes precedence");
            require(
                    !AccessoryEquipRules.hasEquipped(p, "curio", targets().get(2)),
                    "fixed-slot snapshot unchanged");
            try (var inner = new AccessoryRestore(p, new CompoundTag())) {
                require(
                        !AccessoryEquipRules.hasEquipped(p, targets().get(2)),
                        "nested snapshot overrides");
            }
            require(
                    AccessoryEquipRules.hasEquipped(p, targets().get(2)),
                    "outer snapshot restored");
            var other = new Wearer(level);
            require(
                    !AccessoryEquipRules.hasEquipped(other, targets().get(2)),
                    "snapshot isolated by wearer");
        }
        require(!AccessoryEquipRules.hasEquipped(p, targets().get(2)), "scope closed");
        try {
            try (var scope = new AccessoryRestore(p, new CompoundTag())) {
                throw new IllegalStateException("probe");
            }
        } catch (IllegalStateException expected) {
            require(AccessoryEquipRules.hasDiary(p), "scope closed after exception");
        }
        p.move(targets().get(0), "ring");
        p.move(targets().get(2), "head");
        p.setGameMode(GameType.SPECTATOR);
        require(
                !DonQuixoteCombat.equipped(p) && Phosphophyllite.phase(p) == 0,
                "spectator excluded");
        p.setGameMode(GameType.SURVIVAL);
        p.setHealth(0);
        require(
                !DonQuixoteCombat.equipped(p) && Phosphophyllite.phase(p) == 0,
                "dead wearer excluded");
        fullInventoryReturn(level);
        System.out.println("SLOT_MATRIX_PASS checks=" + checks);
    }

    private static void fullInventoryReturn(ServerLevel level) {
        context = "full inventory dependency return";
        var p = new Wearer(level);
        for (int i = 0; i < targets().size(); i++) p.move(targets().get(i), SLOTS[i]);
        String marker = p.getUUID().toString();
        for (Item item : targets().subList(1, 3))
            for (var result : p.inv().findCurios(item))
                result.stack().getOrCreateTag().putString("SlotReturnProbe", marker);
        for (int i = 0; i < 36; i++)
            p.getInventory().setItem(i, new ItemStack(net.minecraft.world.item.Items.STONE, 64));
        p.diary(false);
        DependentAccessories.reconcile(p);
        for (int i = 1; i < targets().size(); i++) {
            Item item = targets().get(i);
            int count = 0;
            for (var entity : level.getAllEntities()) {
                if (entity instanceof net.minecraft.world.entity.item.ItemEntity dropped
                        && dropped.getItem().is(item)
                        && dropped.getItem().hasTag()
                        && marker.equals(dropped.getItem().getTag().getString("SlotReturnProbe"))) {
                    count += dropped.getItem().getCount();
                    dropped.discard();
                }
            }
            require(
                    count == 1 && !functional(p, item, null),
                    "full inventory drops exactly one " + item);
        }
        require(functional(p, targets().get(0), null), "independent item retained without diary");
    }

    private static void effects(ServerLevel level) {
        var cow = EntityType.COW.create(level);
        require(cow != null, "combat target created");
        var chance = BoundlessItemsConfig.NUMBERS.get("star.witnessChance");
        double oldChance = chance.get();
        try {
            chance.set(1.0);
            DonQuixoteCombat.Snapshot baseline = null;
            double baseBonus = -1;
            for (String slot : SLOTS) {
                context = "effects slot=" + slot;
                var p = new Wearer(level);
                p.phase(true);
                p.move(targets().get(0), slot);
                var snapshot = DonQuixoteCombat.snapshot(cow, p.damageSources().playerAttack(p));
                require(snapshot != null, "combat snapshot created");
                if (baseline == null) baseline = snapshot;
                require(snapshot.equals(baseline), "combat snapshot matches curio baseline");
                DonQuixoteCombat.finish(cow, p.damageSources().playerAttack(p), snapshot, 100);
                require(
                        p.getPersistentData().getInt(DonQuixoteState.COOLDOWN) > 0,
                        "guard triggered");
                require(
                        DonQuixoteState.reduce(p, p.damageSources().mobAttack(cow), 10) < 10,
                        "guard reduces damage");
                p.move(targets().get(0), null);
                require(
                        DonQuixoteCombat.snapshot(cow, p.damageSources().playerAttack(p)) == null,
                        "unequip disables snapshot");
                near(
                        DonQuixoteState.reduce(p, p.damageSources().mobAttack(cow), 10),
                        10,
                        "unequip clears guard");
                p.move(targets().get(1), slot);
                StarVoiceState.confirmed(cow, p.damageSources().playerAttack(p));
                require(TimeStopManager.locked(p.getUUID()), "star creates real time-stop field");
                TimeStopManager.clear();
                p.move(targets().get(1), null);
                p.getPersistentData().remove(StarVoiceState.INTERVAL);
                p.getPersistentData().remove(StarVoiceState.COOLDOWN);
                StarVoiceState.confirmed(cow, p.damageSources().playerAttack(p));
                require(!TimeStopManager.locked(p.getUUID()), "unequipped star does not trigger");
                p.move(targets().get(2), slot);
                double bonus = Phosphophyllite.damageBonus(p);
                if (baseBonus < 0) baseBonus = bonus;
                near(bonus, baseBonus, "damage bonus equals curio baseline");
                require(bonus > 0, "phosphophyllite damage enabled");
                checkState(p);
                p.move(targets().get(2), null);
                checkState(p);
                near(Phosphophyllite.damageBonus(p), 0, "unequip removes damage bonus");
            }
        } finally {
            chance.set(oldChance);
            TimeStopManager.clear();
            cow.discard();
        }
        System.out.println("SLOT_EFFECTS_PASS checks=" + checks);
    }

    private static void transition(Wearer p, Random random, int step) {
        Item item = targets().get(random.nextInt(3));
        int action = random.nextInt(6);
        String slot = SLOTS[random.nextInt(SLOTS.length)];
        String trace =
                "step="
                        + step
                        + " player="
                        + p.getUUID()
                        + " action="
                        + action
                        + " item="
                        + item
                        + " slot="
                        + slot;
        history.addLast(trace);
        if (history.size() > 32) history.removeFirst();
        switch (action) {
            case 0, 1 -> p.move(item, slot);
            case 2 -> p.move(item, null);
            case 3 -> {
                p.diary(!functional(p, ModRegistry.LONG_TRAVAIL.get(), "travel_diary"));
                DependentAccessories.reconcile(p);
            }
            case 4 -> p.phase(random.nextBoolean());
            case 5 -> p.reload();
            default -> throw new AssertionError(action);
        }
        checkState(p);
    }

    private static void pressure(ServerLevel level) {
        for (long seed : new long[] {20261027L, 20261028L, 20261029L}) {
            var random = new Random(seed);
            var players = new ArrayList<Wearer>();
            for (int i = 0; i < 4; i++) players.add(new Wearer(level));
            for (int step = 0; step < 10000; step++) {
                context = "pressure seed=" + seed + " step=" + step;
                transition(players.get(random.nextInt(players.size())), random, step);
                if (step % 100 == 0) for (var player : players) checkState(player);
            }
            for (var player : players) {
                for (Item item : targets()) player.move(item, null);
                checkState(player);
            }
            System.out.println(
                    "SLOT_PRESSURE_PASS seed="
                            + seed
                            + " transitions=10000 players=4 checks="
                            + checks);
        }
    }

    private static void saveRestart(ServerLevel level) throws Exception {
        var p = new Wearer(level);
        p.phase(true);
        for (int i = 0; i < targets().size(); i++) p.move(targets().get(i), SLOTS[i + 1]);
        p.getPersistentData().putInt(StarVoiceState.COOLDOWN, 731);
        p.getPersistentData().putInt(DonQuixoteState.COOLDOWN, 419);
        var data = p.saveWithoutId(new CompoundTag());
        data.putLong("SlotTestProcess", ProcessHandle.current().pid());
        net.minecraft.nbt.NbtIo.writeCompressed(data, Path.of("slot-player.nbt").toFile());
        System.out.println("SLOT_RESTART_PREPARED uuid=" + p.getUUID());
    }

    private static void verifyRestart(ServerLevel level) throws Exception {
        context = "cross-process player NBT restore";
        var data = net.minecraft.nbt.NbtIo.readCompressed(Path.of("slot-player.nbt").toFile());
        require(data.getLong("SlotTestProcess") != ProcessHandle.current().pid(), "different JVM");
        var p = new Wearer(level, data.getUUID("UUID"));
        p.load(data);
        p.diary = p.inv().findFirstCurio(ModRegistry.LONG_TRAVAIL.get()).orElseThrow().stack();
        require(p.getUUID().equals(data.getUUID("UUID")), "player identity restored");
        for (int i = 0; i < targets().size(); i++)
            require(
                    functional(p, targets().get(i), SLOTS[i + 1]),
                    "non-curio position restored " + targets().get(i));
        require(
                p.getPersistentData().getInt(StarVoiceState.COOLDOWN) == 731,
                "star cooldown persisted");
        require(
                p.getPersistentData().getInt(DonQuixoteState.COOLDOWN) == 419,
                "quixote cooldown persisted");
        checkState(p);
        System.out.println("SLOT_RESTART_PASS checks=" + checks);
    }

    public AccessorySlotsTest() {
        CuriosApi.registerCurioPredicate(
                new net.minecraft.resources.ResourceLocation("travail_smoke", "deny"),
                result -> false);
        MinecraftForge.EVENT_BUS.addListener(this::started);
        MinecraftForge.EVENT_BUS.addListener(this::tick);
    }

    private void started(ServerStartedEvent event) {
        server = event.getServer();
        try {
            if (System.getProperty("travail.slots.stage", "full").equals("verify")) {
                verifyRestart(server.overworld());
                finish(null);
                return;
            }
            matrix(server.overworld());
            effects(server.overworld());
            pressure(server.overworld());
            for (int i = 0; i < 8; i++) soak.add(new Wearer(server.overworld()));
            deadline = System.nanoTime() + java.time.Duration.ofMinutes(5).toNanos();
            running = true;
        } catch (Throwable failure) {
            finish(failure);
        }
    }

    private void tick(TickEvent.ServerTickEvent event) {
        if (!running || event.phase != TickEvent.Phase.END) return;
        try {
            context = "real-tick soak tick=" + ticks;
            require(System.nanoTime() < deadline, "soak deadline");
            for (int i = 0; i < soak.size(); i++) {
                if (ticks % 20 == i)
                    transition(soak.get(i), new Random(20261027L + ticks * 8L + i), ticks);
                else checkState(soak.get(i));
            }
            if (++ticks >= 2400) {
                for (var player : soak) {
                    for (Item item : targets()) player.move(item, null);
                    checkState(player);
                }
                System.out.println("SLOT_SOAK_PASS ticks=" + ticks + " players=8 checks=" + checks);
                saveRestart(server.overworld());
                finish(null);
            }
        } catch (Throwable failure) {
            finish(failure);
        }
    }

    private void finish(Throwable failure) {
        running = false;
        try {
            String result =
                    failure == null
                            ? "PASS checks=" + checks + " ticks=" + ticks
                            : "FAIL " + failure;
            if (failure != null) failure.printStackTrace();
            Files.writeString(Path.of("slot-result.txt"), result);
            Files.writeString(
                    Path.of("slot-history.txt"), context + "\n" + String.join("\n", history));
        } catch (Exception error) {
            throw new RuntimeException(error);
        } finally {
            server.halt(false);
        }
    }
}
