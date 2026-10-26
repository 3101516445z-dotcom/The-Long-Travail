package travail.smoke;

import com.mojang.authlib.GameProfile;
import com.thelongtravail.*;
import com.thelongtravail.config.*;
import com.thelongtravail.data.*;
import com.thelongtravail.event.TravailEvents;
import com.thelongtravail.helper.CombatContext;
import com.thelongtravail.registry.ModRegistry;
import net.minecraft.server.level.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.effect.*;
import net.minecraft.world.level.GameType;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.living.*;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import top.theillusivec4.curios.api.*;
import top.theillusivec4.curios.api.type.capability.*;
import top.theillusivec4.curios.common.inventory.CurioStacksHandler;
import java.util.*;

@Mod("travail_smoke")
public final class AccessoryRestoreSmoke {
    private static void check(boolean ok, String text) {
        if (!ok) throw new AssertionError(text);
    }

    private static void near(float actual, float expected, String text) {
        check(Math.abs(actual - expected) < .001, text + ": " + actual);
    }

    private static final class TestPlayer extends ServerPlayer {
        TestPlayer(ServerLevel level) {
            super(level.getServer(), level, new GameProfile(UUID.randomUUID(), "ModeFixTest"));
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
        }
    }

    private static boolean legacy(TestPlayer p, Item item, String slot) {
        return CuriosApi.getCuriosInventory(p)
                .map(
                        inv ->
                                inv.findCurios(item).stream()
                                        .anyMatch(
                                                r ->
                                                        !r.slotContext().cosmetic()
                                                                && (slot == null
                                                                        || r.slotContext()
                                                                                .identifier()
                                                                                .equals(slot))))
                .orElse(false);
    }

    private static void queryPressure(ServerLevel level) {
        var p = new TestPlayer(level);
        equip(p);
        var inv = CuriosApi.getCuriosInventory(p).resolve().orElseThrow();
        var random = new Random(20261024L);
        Item[] items = {
            ModRegistry.STAR_VOICE.get(),
            ModRegistry.FREE_AND_EASY_WANDERING.get(),
            ModRegistry.GOLDEN_AGE.get(),
            ModRegistry.ICARUS.get(),
            ModRegistry.LONG_TRAVAIL.get()
        };
        String[] slots = {"travel_diary", "ring", "curio", "charm", "back"};
        int comparisons = 0;
        for (int cycle = 0; cycle < 2000; cycle++) {
            for (String slot : slots) {
                var handler = inv.getStacksHandler(slot).orElseThrow();
                for (int index = 0; index < 2; index++) {
                    int choice = random.nextInt(items.length + 1);
                    handler.getStacks()
                            .setStackInSlot(
                                    index,
                                    choice == items.length
                                            ? ItemStack.EMPTY
                                            : new ItemStack(items[choice]));
                    choice = random.nextInt(items.length + 1);
                    handler.getCosmeticStacks()
                            .setStackInSlot(
                                    index,
                                    choice == items.length
                                            ? ItemStack.EMPTY
                                            : new ItemStack(items[choice]));
                }
            }
            for (Item item : items)
                for (String slot : slots) {
                    check(
                            com.thelongtravail.helper.AccessoryEquipRules.hasEquipped(p, slot, item)
                                    == legacy(p, item, slot),
                            "query matches legacy");
                    comparisons++;
                }
            for (boolean mode : new boolean[] {false, true}) {
                boolean diary = !com.thelongtravail.helper.TravailCurios.stack(p).isEmpty();
                boolean expected =
                        (mode || diary)
                                && legacy(p, mode ? items[1] : items[0], mode ? null : "curio");
                check(
                        com.thelongtravail.boundless.BoundlessEquipment.equipped(p, mode)
                                == expected,
                        "boundless qualification unchanged");
                expected =
                        p.isAlive()
                                && !p.isSpectator()
                                && legacy(p, mode ? items[2] : items[3], mode ? null : "back");
                check(
                        com.thelongtravail.farreach.FarReachEquipment.equipped(p, mode) == expected,
                        "far reach qualification unchanged");
                comparisons += 2;
            }
        }
        p.setHealth(0);
        check(
                !com.thelongtravail.farreach.FarReachEquipment.equipped(p, true),
                "dead player rejected");
        System.out.println(
                "CURIO_QUERY_PRESSURE_PASS comparisons="
                        + comparisons
                        + " layouts=2000 seed=20261024");
    }

    public AccessoryRestoreSmoke() {
        MinecraftForge.EVENT_BUS.register(this);
    }

    private static ItemStack equip(TestPlayer p) {
        var inventory = CuriosApi.getCuriosInventory(p).resolve().orElseThrow();
        var slots =
                new HashMap<
                        String, top.theillusivec4.curios.api.type.inventory.ICurioStacksHandler>();
        for (String id :
                List.of(
                        "travel_diary",
                        "head",
                        "charm",
                        "back",
                        "body",
                        "belt",
                        "ring",
                        "feet",
                        "curio"))
            slots.put(
                    id,
                    new CurioStacksHandler(
                            inventory, id, 2, true, false, true, ICurio.DropRule.DEFAULT));
        inventory.setCurios(slots);
        var diary = new ItemStack(ModRegistry.LONG_TRAVAIL.get());
        check(LongTravailData.tryInitialize(diary, p), "initialize diary");
        for (var a : TravailAspect.values()) LongTravailData.setWitness(diary, a, false);
        slots.get("travel_diary").getStacks().setStackInSlot(0, diary);
        return diary;
    }

    private static final Map<Item, String> ITEMS = new LinkedHashMap<>();

    private static boolean dependent(Item i) {
        return i == ModRegistry.AZRAEL.get()
                || i == ModRegistry.STAR_VOICE.get()
                || i == ModRegistry.THOUSAND_YEARS.get()
                || i == ModRegistry.BOOK_OF_DEAD.get();
    }

    private static void verify(ServerLevel level, Item item, String slot, boolean diary) {
        var old = new TestPlayer(level);
        equip(old);
        var inv = CuriosApi.getCuriosInventory(old).resolve().orElseThrow();
        if (!diary)
            inv.getStacksHandler("travel_diary")
                    .orElseThrow()
                    .getStacks()
                    .setStackInSlot(0, ItemStack.EMPTY);
        var stack = new ItemStack(item);
        stack.getOrCreateTag().putString("RestoreProbe", "preserved");
        inv.getStacksHandler(slot).orElseThrow().getStacks().setStackInSlot(0, stack);
        var fresh = new TestPlayer(level);
        var restored = CuriosApi.getCuriosInventory(fresh).resolve().orElseThrow();
        new top.theillusivec4.curios.common.event.CuriosEventHandler()
                .playerClone(new PlayerEvent.Clone(fresh, old, true));
        boolean carried =
                item == ModRegistry.GOLDEN_AGE.get() || item == ModRegistry.BOOK_OF_DEAD.get();
        boolean expected = !carried && (diary || !dependent(item));
        check(
                restored.findFirstCurio(item).isPresent() == expected,
                "clone " + item + " diary=" + diary);
        restored.handleInvalidStacks();
        check(
                fresh.getInventory().countItem(item) == (expected ? 0 : 1),
                "inventory count " + item);
        if (expected) {
            check(
                    restored.findFirstCurio(item)
                            .orElseThrow()
                            .stack()
                            .getTag()
                            .getString("RestoreProbe")
                            .equals("preserved"),
                    "NBT preserved");
            restored.readTag(restored.writeTag());
            check(restored.findFirstCurio(item).isPresent(), "same-player reload " + item);
            restored.handleInvalidStacks();
            check(fresh.getInventory().countItem(item) == 0, "no duplicate returns");
            var c = new SlotContext(slot, fresh, 0, false, true);
            check(((ICurioItem) item).canEquip(c, stack), "same slot replacement");
            var second = new SlotContext(slot, fresh, 1, false, true);
            check(!((ICurioItem) item).canEquip(second, stack), "live duplicate rejected");
            try (var scope =
                    new com.thelongtravail.helper.AccessoryRestore(fresh, restored.writeTag())) {
                check(((ICurioItem) item).canEquip(c, stack), "restore first accepted");
                check(!((ICurioItem) item).canEquip(second, stack), "restore duplicate rejected");
                check(((ICurioItem) item).canEquip(c, stack), "repeat validation idempotent");
            }
        }
        System.out.println(
                "ACCESSORY_RESTORE_CASE: " + item + " diary=" + diary + " restored=" + expected);
    }

    @SubscribeEvent
    public void started(ServerStartedEvent event) {
        boolean passed = false;
        try {
            var level = event.getServer().overworld();
            level.getGameRules()
                    .getRule(net.minecraft.world.level.GameRules.RULE_KEEPINVENTORY)
                    .set(true, event.getServer());
            ITEMS.put(ModRegistry.AZRAEL.get(), "head");
            ITEMS.put(ModRegistry.STAR_VOICE.get(), "curio");
            ITEMS.put(ModRegistry.DON_QUIXOTE.get(), "curio");
            ITEMS.put(ModRegistry.GOLDEN_AGE.get(), "charm");
            ITEMS.put(ModRegistry.BOOK_OF_DEAD.get(), "charm");
            ITEMS.put(ModRegistry.THOUSAND_YEARS.get(), "ring");
            ITEMS.put(ModRegistry.SPRING_CORNER.get(), "head");
            ITEMS.put(ModRegistry.AFFECTION.get(), "body");
            ITEMS.put(ModRegistry.TOKAIDO.get(), "feet");
            ITEMS.put(ModRegistry.FREE_AND_EASY_WANDERING.get(), "ring");
            ITEMS.put(ModRegistry.SWORD_AND_LANTERN.get(), "belt");
            ITEMS.put(ModRegistry.ICARUS.get(), "back");
            for (var entry : ITEMS.entrySet())
                for (boolean diary : new boolean[] {true, false})
                    verify(level, entry.getKey(), entry.getValue(), diary);
            // 恢复前置读取输入快照，不回退到旧日记。
            var p = new TestPlayer(level);
            equip(p);
            try (var scope =
                    new com.thelongtravail.helper.AccessoryRestore(
                            p, new net.minecraft.nbt.CompoundTag())) {
                check(
                        !com.thelongtravail.helper.AccessoryEquipRules.hasDiary(p),
                        "missing incoming prerequisite overrides live inventory");
                var other = new TestPlayer(level);
                equip(other);
                check(
                        com.thelongtravail.helper.AccessoryEquipRules.hasDiary(other),
                        "scope isolated per wearer");
            }
            check(com.thelongtravail.helper.AccessoryEquipRules.hasDiary(p), "scope cleaned up");
            var inv = CuriosApi.getCuriosInventory(p).resolve().orElseThrow();
            try (var scope = new com.thelongtravail.helper.AccessoryRestore(p, inv.writeTag())) {
                var azrael = new ItemStack(ModRegistry.AZRAEL.get());
                check(
                        !((ICurioItem) azrael.getItem())
                                .canEquip(new SlotContext("feet", p, 0, false, true), azrael),
                        "wrong slot rejected");
                check(
                        !((ICurioItem) azrael.getItem())
                                .canEquip(new SlotContext("head", p, 0, true, true), azrael),
                        "cosmetic rejected");
                check(
                        ((ICurioItem) azrael.getItem())
                                .canEquip(new SlotContext("head", p, 0, false, true), azrael),
                        "rejected candidate does not reserve uniqueness");
                var affection = new ItemStack(ModRegistry.AFFECTION.get());
                affection
                        .getOrCreateTag()
                        .putUUID(com.thelongtravail.flourishing.Affection.OWNER, UUID.randomUUID());
                check(
                        !((ICurioItem) affection.getItem())
                                .canEquip(new SlotContext("body", p, 0, false, true), affection),
                        "wrong owner rejected");
                affection
                        .getTag()
                        .putUUID(com.thelongtravail.flourishing.Affection.OWNER, p.getUUID());
                check(
                        ((ICurioItem) affection.getItem())
                                .canEquip(new SlotContext("body", p, 0, false, true), affection),
                        "matching owner accepted");
                var book = new ItemStack(ModRegistry.BOOK_OF_DEAD.get());
                var id = com.thelongtravail.underworld.UnderworldLedger.identity(book);
                com.thelongtravail.underworld.UnderworldLedger.get(p.server).consume(id);
                check(
                        !((ICurioItem) book.getItem())
                                .canEquip(new SlotContext("charm", p, 0, false, true), book),
                        "consumed book rejected");
                var validBook = new ItemStack(ModRegistry.BOOK_OF_DEAD.get());
                check(
                        !((ICurioItem) validBook.getItem())
                                .canEquip(new SlotContext("charm", p, 0, false, true), validBook),
                        "carried book cannot be equipped");
                try (var nested =
                        new com.thelongtravail.helper.AccessoryRestore(
                                p, new net.minecraft.nbt.CompoundTag())) {
                    check(
                            !com.thelongtravail.helper.AccessoryEquipRules.hasDiary(p),
                            "nested snapshot active");
                }
                check(
                        com.thelongtravail.helper.AccessoryEquipRules.hasDiary(p),
                        "outer snapshot restored");
            }

            System.out.println(
                    "ACCESSORY_RESTORE_PASS: 12 items, clone, same-player reload, prerequisites, NBT, duplicate rejection, scope isolation");
            queryPressure(level);
            passed = true;
        } catch (Throwable failure) {
            failure.printStackTrace();
        } finally {
            try {
                java.nio.file.Files.writeString(
                        java.nio.file.Path.of("accessory-restore-result.txt"),
                        passed ? "PASS" : "FAIL");
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            event.getServer().halt(false);
        }
    }
}
