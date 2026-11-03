package travail.smoke;

import static com.thelongtravail.abyss.copy.CopyStorage.*;

import com.mojang.authlib.GameProfile;
import com.thelongtravail.abyss.copy.*;
import com.thelongtravail.registry.ModRegistry;

import net.minecraft.nbt.*;
import net.minecraft.server.level.*;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.*;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.fml.common.Mod;

import java.nio.file.*;
import java.util.*;

@Mod("travail_smoke")
public final class FluidCoreSmoke {
    private static int checks;

    private static void check(boolean value, String label) {
        if (!value) throw new AssertionError(label);
        checks++;
    }

    private static void rejects(Throwing action, String label) throws Exception {
        try {
            action.run();
        } catch (Exception expected) {
            checks++;
            return;
        }
        throw new AssertionError(label);
    }

    interface Throwing {
        void run() throws Exception;
    }

    static final class Player extends ServerPlayer {
        Player(ServerLevel level) {
            super(level.getServer(), level, new GameProfile(UUID.randomUUID(), "FluidCore"));
            var wire =
                    new net.minecraft.network.Connection(
                            net.minecraft.network.protocol.PacketFlow.SERVERBOUND);
            connection =
                    new net.minecraft.server.network.ServerGamePacketListenerImpl(
                            level.getServer(), wire, this) {
                        @Override
                        public void send(net.minecraft.network.protocol.Packet<?> packet) {}
                    };
        }
    }

    public FluidCoreSmoke() {
        MinecraftForge.EVENT_BUS.addListener(this::started);
    }

    static ItemStack item(UUID id) {
        ItemStack s = new ItemStack(Items.STONE);
        s.getOrCreateTag().putUUID("external", id);
        return s;
    }

    static UUID id(ItemStack s) {
        return s.getTag().getUUID("external");
    }

    void started(ServerStartedEvent event) {
        var original = CopyStorage.snapshot();
        try {
            run(event.getServer().overworld());
            Files.writeString(Path.of("fluid-core-result.txt"), "PASS checks=" + checks);
        } catch (Throwable failure) {
            failure.printStackTrace();
            try {
                Files.writeString(Path.of("fluid-core-result.txt"), "FAIL " + failure);
            } catch (Exception ignored) {
            }
        } finally {
            CopyStorage.install(original);
            event.getServer().halt(false);
        }
    }

    static void run(ServerLevel level) throws Exception {
        Map<UUID, CompoundTag> store = new LinkedHashMap<>();
        List<String> trace = new ArrayList<>();
        int[] commits = {0};
        boolean[] fail = {false}, badRollback = {false};
        Entry entry =
                new Entry(
                        "fake",
                        0,
                        data ->
                                data.contains("Count", Tag.TAG_ANY_NUMERIC)
                                        && data.getCompound("tag").hasUUID("external"),
                        false,
                        new Adapter() {
                            public Reference reference(CompoundTag data) {
                                var tag = data.getCompound("tag");
                                return new Reference(
                                        new Key("fake", tag.getUUID("external")),
                                        tag,
                                        "external",
                                        "");
                            }

                            public Write prepare(Context context, Pending pending)
                                    throws Exception {
                                var source = store.get(pending.source().key().id());
                                if (source == null)
                                    throw new IllegalStateException("missing source");
                                CompoundTag copy = source.copy();
                                context.transform(copy, 0);
                                return new Write() {
                                    public void commit() throws Exception {
                                        trace.add("commit:" + pending.id());
                                        store.put(pending.id(), copy);
                                        if (++commits[0] == 2 && fail[0])
                                            throw new Exception("injected commit");
                                    }

                                    public void rollback() throws Exception {
                                        trace.add("rollback:" + pending.id());
                                        store.remove(pending.id());
                                        if (badRollback[0])
                                            throw new Exception("injected rollback");
                                    }
                                };
                            }
                        });
        CopyStorage.install(List.of(entry));
        UUID root = UUID.randomUUID(), child = UUID.randomUUID();
        CompoundTag leaf = new CompoundTag();
        leaf.putString("value", "unchanged");
        store.put(child, leaf);
        CompoundTag parent = new CompoundTag();
        parent.put("a", item(child).save(new CompoundTag()));
        parent.put("b", item(child).save(new CompoundTag()));
        store.put(root, parent);
        ItemStack source = item(root);
        CompoundTag before = source.save(new CompoundTag());
        CompoundTag preview = before.copy();
        FluidCopyGraph.reserveIds(preview);
        check(
                !preview.getCompound("tag").getUUID("external").equals(root),
                "preview remaps identity");
        check(store.size() == 2 && trace.isEmpty(), "preview has no writes");
        var prepared = FluidCopyGraph.prepare(level, source);
        check(store.size() == 2, "prepare has no writes");
        prepared.commit();
        UUID next = id(prepared.output());
        var copied = store.get(next);
        UUID first = copied.getCompound("a").getCompound("tag").getUUID("external");
        check(!next.equals(root) && !first.equals(child), "fresh identities");
        check(
                first.equals(copied.getCompound("b").getCompound("tag").getUUID("external")),
                "shared reference preserved");
        check(
                store.get(root).equals(parent) && source.save(new CompoundTag()).equals(before),
                "source unchanged");
        prepared.rollback();
        check(store.size() == 2, "rollback removes new inventories");
        trace.clear();
        commits[0] = 0;
        fail[0] = true;
        var failing = FluidCopyGraph.prepare(level, source);
        rejects(failing::commit, "commit failure");
        check(store.size() == 2, "partial writes restored");
        check(
                trace.size() == 4
                        && trace.get(2).substring(9).equals(trace.get(1).substring(7))
                        && trace.get(3).substring(9).equals(trace.get(0).substring(7)),
                "reverse rollback order");
        trace.clear();
        commits[0] = 0;
        badRollback[0] = true;
        var suppressed = FluidCopyGraph.prepare(level, source);
        try {
            suppressed.commit();
            throw new AssertionError("failure expected");
        } catch (Exception expected) {
            check(expected.getSuppressed().length == 1, "rollback failure retained");
        }
        check(store.size() == 2 && trace.size() == 4, "rollback continues after failure");
        badRollback[0] = false;
        fail[0] = false;
        parent.put("cycle", source.save(new CompoundTag()));
        store.put(root, parent);
        var cycle = FluidCopyGraph.prepare(level, source);
        cycle.commit();
        check(
                store.get(id(cycle.output()))
                        .getCompound("cycle")
                        .getCompound("tag")
                        .getUUID("external")
                        .equals(id(cycle.output())),
                "cycle points to new root");
        cycle.rollback();
        CompoundTag protectedTag =
                new ItemStack(ModRegistry.PERFECT_FLUID.get()).save(new CompoundTag());
        protectedTag.putByte("Slot", (byte) 5);
        CompoundTag nested = new CompoundTag();
        nested.put("item", protectedTag);
        FluidCopyGraph.reserveIds(nested);
        check(
                nested.getCompound("item").getString("id").equals("minecraft:air")
                        && nested.getCompound("item").getByte("Slot") == 5,
                "protected slot preserved");
        CompoundTag deep = new CompoundTag(), cursor = deep;
        for (int i = 0; i < 130; i++) {
            CompoundTag n = new CompoundTag();
            cursor.put("child", n);
            cursor = n;
        }
        rejects(() -> FluidCopyGraph.reserveIds(deep), "depth limit");
        var player = new Player(level);
        var menu = player.inventoryMenu;
        var grid = menu.getCraftSlots();
        grid.setItem(0, new ItemStack(ModRegistry.PERFECT_FLUID.get(), 2));
        grid.setItem(1, source.copy());
        menu.slotsChanged(grid);
        check(!menu.getSlot(0).getItem().isEmpty(), "valid external preview");
        CopyStorage.install(List.of(entry.withoutImplementation()));
        check(FluidCopyGraph.hasReferences(before), "disabled adapter still identified");
        rejects(() -> FluidCopyGraph.reserveIds(before.copy()), "disabled preview rejected");
        rejects(() -> FluidCopyGraph.prepare(level, source), "disabled prepare rejected");
        menu.clicked(0, 0, ClickType.PICKUP, player);
        check(
                grid.getItem(0).getCount() == 2
                        && grid.getItem(1).save(new CompoundTag()).equals(before)
                        && menu.getCarried().isEmpty(),
                "disabled pickup consumes nothing");
        menu.clicked(0, 0, ClickType.QUICK_MOVE, player);
        check(
                grid.getItem(0).getCount() == 2 && store.size() == 2,
                "disabled quick move consumes nothing");
        grid.setItem(1, new ItemStack(Items.DIAMOND));
        menu.slotsChanged(grid);
        check(
                menu.getSlot(0).getItem().getCount() == 2,
                "ordinary preview survives adapter removal");
        menu.clicked(0, 0, ClickType.PICKUP, player);
        check(
                menu.getCarried().is(Items.DIAMOND)
                        && menu.getCarried().getCount() == 2
                        && grid.getItem(0).getCount() == 1,
                "ordinary pickup remains functional");
        rejects(() -> CopyStorage.install(List.of(entry, entry)), "duplicate registration");
    }
}
