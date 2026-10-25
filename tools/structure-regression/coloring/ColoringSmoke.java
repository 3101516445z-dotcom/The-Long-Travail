package travail.smoke;

import com.mojang.authlib.GameProfile;
import com.thelongtravail.item.NameColoring;
import com.thelongtravail.menu.ColoringMenu;
import com.thelongtravail.registry.ModRegistry;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.*;
import net.minecraft.server.level.*;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;

import java.nio.file.*;
import java.util.*;

@Mod("travail_smoke")
public final class ColoringSmoke {
    private static int checks;

    public ColoringSmoke() {
        MinecraftForge.EVENT_BUS.register(this);
    }

    private static void check(boolean ok, String message) {
        checks++;
        if (!ok) throw new AssertionError(message);
    }

    private static final class TestPlayer extends ServerPlayer {
        TestPlayer(ServerLevel level) {
            super(level.getServer(), level, new GameProfile(UUID.randomUUID(), "ColoringTest"));
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
            giveExperiencePoints(100000);
        }
    }

    private static String marker(ItemStack stack) {
        return stack.getHoverName().getStyle().getInsertion();
    }

    private static CompoundTag withoutSelection(ItemStack stack) {
        var tag = stack.save(new CompoundTag());
        tag.getCompound("tag").remove(NameColoring.KEY);
        return tag;
    }

    private static ColoringMenu menu(TestPlayer player, ItemStack input) {
        var menu = new ColoringMenu(7, player.getInventory());
        player.containerMenu = menu;
        menu.getSlot(0).set(input);
        return menu;
    }

    private static void names(TestPlayer player) {
        for (Item item :
                List.of(
                        Items.DIAMOND_SWORD,
                        ModRegistry.LONG_TRAVAIL.get(),
                        ModRegistry.LITTLE_PRINCE.get())) {
            for (int before = -1; before < 10; before++)
                for (int after = 0; after < 10; after++) {
                    ItemStack stack = new ItemStack(item);
                    stack.setHoverName(
                            Component.translatable("item.minecraft.diamond_sword")
                                    .append(
                                            Component.literal(" 嵌套")
                                                    .withStyle(
                                                            s ->
                                                                    s.withColor(0x123456)
                                                                            .withBold(true))));
                    stack.enchant(Enchantments.UNBREAKING, 2);
                    stack.getOrCreateTag().putString("external_storage_id", "unchanged");
                    NameColoring.apply(stack, before);
                    CompoundTag baseline = withoutSelection(stack);
                    NameColoring.apply(stack, after);
                    check(NameColoring.selection(stack) == after, "selection transition");
                    for (int n = 0; n < 3; n++) stack.getHoverName().getString();
                    check(
                            baseline.equals(withoutSelection(stack)),
                            "display read must not mutate original data");
                    check(
                            NameColoring.selection(ItemStack.of(stack.save(new CompoundTag())))
                                    == after,
                            "persistence");
                    check(
                            after == 9 ? marker(stack) == null : marker(stack) != null,
                            "mixin marker applied");
                    var anvil = new AnvilMenu(8, player.getInventory(), ContainerLevelAccess.NULL);
                    anvil.getSlot(0).set(stack);
                    anvil.setItemName("Renamed 星河");
                    ItemStack renamed = anvil.getSlot(2).getItem();
                    check(
                            !renamed.isEmpty() && NameColoring.selection(renamed) == after,
                            "anvil retains selection");
                    check(
                            renamed.getHoverName().getString().contains("Renamed 星河"),
                            "anvil uses current name");
                    renamed.resetHoverName();
                    check(
                            NameColoring.selection(renamed) == after,
                            "clear custom name retains selection");
                }
        }
        ItemStack source = new ItemStack(ModRegistry.LONG_TRAVAIL.get());
        NameColoring.apply(source, 9);
        check(marker(source) == null, "clear native TLT name");
        source.getOrCreateTag().putString(NameColoring.KEY, "unknown");
        check(
                NameColoring.selection(source) == -1 && marker(source) != null,
                "unknown selection fallback");
        source.getOrCreateTag().putInt(NameColoring.KEY, 999);
        check(NameColoring.selection(source) == -1, "wrong type fallback");
        NameColoring.apply(source, 8);
        var saved = source.save(new CompoundTag());
        for (int i = 0; i < 1000; i++) source.getHoverName().getVisualOrderText();
        check(
                saved.equals(source.save(new CompoundTag())),
                "1000 name reads do not accumulate data");
    }

    private static void containers(TestPlayer player) {
        var menu = menu(player, new ItemStack(Items.DIAMOND, 64));
        check(!menu.getSlot(1).hasItem(), "no result before selection");
        check(
                !menu.clickMenuButton(player, -1) && !menu.clickMenuButton(player, 10),
                "reject invalid buttons");
        menu.clickMenuButton(player, 6);
        check(
                menu.getSlot(0).getItem().getCount() == 64
                        && NameColoring.selection(menu.getSlot(0).getItem()) == -1,
                "preview is non-mutating");
        menu.clicked(1, 0, ClickType.SWAP, player);
        menu.clicked(1, 0, ClickType.THROW, player);
        check(menu.getSlot(0).getItem().getCount() == 64, "output bypass blocked");
        menu.clicked(1, 1, ClickType.PICKUP, player);
        check(
                menu.getCarried().getCount() == 1 && menu.getSlot(0).getItem().getCount() == 63,
                "right click takes exactly one");
        menu.clicked(1, 0, ClickType.PICKUP, player);
        check(
                menu.getCarried().getCount() == 64
                        && !menu.getSlot(0).hasItem()
                        && !menu.getSlot(1).hasItem(),
                "whole stack conservation");
        menu.setCarried(ItemStack.EMPTY);
        player.getInventory().clearContent();
        menu = menu(player, new ItemStack(Items.DIAMOND, 32));
        menu.clickMenuButton(player, 7);
        menu.clicked(1, 0, ClickType.QUICK_MOVE, player);
        check(
                player.getInventory().countItem(Items.DIAMOND) == 32 && !menu.getSlot(0).hasItem(),
                "shift transfer conservation");
        player.getInventory().clearContent();
        for (int i = 0; i < 36; i++)
            player.getInventory().setItem(i, new ItemStack(Items.COBBLESTONE, 64));
        menu = menu(player, new ItemStack(Items.DIAMOND, 32));
        menu.clickMenuButton(player, 0);
        menu.clicked(1, 0, ClickType.QUICK_MOVE, player);
        check(menu.getSlot(0).getItem().getCount() == 32, "full inventory leaves input");
        player.getInventory().clearContent();
        menu.removed(player);
        check(
                player.getInventory().countItem(Items.DIAMOND) == 32 && !menu.getSlot(1).hasItem(),
                "close returns only input");
        menu.removed(player);
        check(
                player.getInventory().countItem(Items.DIAMOND) == 32,
                "repeated close cannot duplicate");
    }

    private static void edgeCases(TestPlayer player) {
        for (int effect = 0; effect < 10; effect++) {
            player.getInventory().clearContent();
            for (int i = 0; i < 36; i++)
                player.getInventory().setItem(i, new ItemStack(Items.COBBLESTONE, 64));
            ItemStack partial = new ItemStack(Items.DIAMOND, 63);
            NameColoring.apply(partial, effect);
            player.getInventory().setItem(35, partial);
            var active = menu(player, new ItemStack(Items.DIAMOND, 64));
            active.clickMenuButton(player, effect);
            active.clicked(1, 0, ClickType.QUICK_MOVE, player);
            check(
                    player.getInventory().getItem(35).getCount() == 64
                            && active.getSlot(0).getItem().getCount() == 63,
                    "partial shift capacity");
            ItemStack cursor = new ItemStack(Items.DIAMOND, 63);
            NameColoring.apply(cursor, effect);
            active.setCarried(cursor);
            active.clicked(1, 0, ClickType.PICKUP, player);
            check(
                    active.getCarried().getCount() == 64
                            && active.getSlot(0).getItem().getCount() == 62,
                    "partial cursor capacity");
            active.clicked(1, 0, ClickType.PICKUP, player);
            check(active.getSlot(0).getItem().getCount() == 62, "full cursor rejection");
            active.setCarried(new ItemStack(Items.EMERALD));
            active.clicked(1, 0, ClickType.PICKUP, player);
            check(
                    active.getSlot(0).getItem().getCount() == 62
                            && active.getCarried().is(Items.EMERALD),
                    "different cursor rejection");
            active.setCarried(ItemStack.EMPTY);
            player.getInventory().clearContent();
            var stranger = new TestPlayer(player.serverLevel());
            check(!active.clickMenuButton(stranger, 0), "non-owner selection rejected");
            active.clicked(1, 0, ClickType.PICKUP, stranger);
            check(active.getSlot(0).getItem().getCount() == 62, "non-owner output rejected");
            active.removed(player);
            check(player.getInventory().countItem(Items.DIAMOND) == 62, "remaining input returned");
        }
        for (int effect = 0; effect < 10; effect++) {
            var stack = new ItemStack(Items.DIAMOND_SWORD);
            net.minecraft.network.chat.MutableComponent name = Component.literal("Root");
            for (int depth = 0; depth < 64; depth++)
                name =
                        Component.translatable("%s", name)
                                .append(Component.literal(" Unicode 中文 Ω "));
            stack.setHoverName(name);
            NameColoring.apply(stack, effect);
            var original = stack.save(new CompoundTag());
            for (int repeat = 0; repeat < 100; repeat++)
                check(stack.getHoverName().getString().contains("Root"), "nested name retained");
            check(original.equals(stack.save(new CompoundTag())), "nested reads preserve NBT");
        }
    }

    @SubscribeEvent
    public void started(ServerStartedEvent event) {
        if (!event.getServer().isDedicatedServer()) return;
        boolean success = false;
        try {
            var player = new TestPlayer(event.getServer().overworld());
            long started = System.nanoTime();
            edgeCases(player);
            for (int round = 0; round < 30; round++) {
                names(player);
                containers(player);
                player.getInventory().clearContent();
            }
            // Exercise partial merges and mixed output actions with a fixed reproducible seed.
            var random = new Random(20261025L);
            for (int round = 0; round < 2000; round++) {
                player.getInventory().clearContent();
                int count = 1 + random.nextInt(64);
                var active = menu(player, new ItemStack(Items.DIAMOND, count));
                int selected = random.nextInt(10);
                active.clickMenuButton(player, selected);
                for (int action = 0; action < 20; action++) {
                    if (random.nextBoolean()) active.clickMenuButton(player, random.nextInt(10));
                    active.clicked(
                            1,
                            random.nextInt(2),
                            random.nextBoolean() ? ClickType.PICKUP : ClickType.QUICK_MOVE,
                            player);
                    int total =
                            active.getSlot(0).getItem().getCount()
                                    + active.getCarried().getCount()
                                    + player.getInventory().countItem(Items.DIAMOND);
                    check(total == count, "random operation conservation round=" + round);
                }
                active.removed(player);
                check(
                        player.getInventory().countItem(Items.DIAMOND) == count,
                        "random close conservation");
            }
            Files.writeString(
                    Path.of("stress-timing.txt"),
                    "server functional stress elapsedMs="
                            + (System.nanoTime() - started) / 1000000);
            if (ModList.get().isLoaded("apotheosis")) AffixColoringChecks.run(player);
            if (ModList.get().isLoaded("tconstruct")) ExtraColoringChecks.run(player);
            var recipe =
                    event.getServer()
                            .getRecipeManager()
                            .byKey(
                                    new net.minecraft.resources.ResourceLocation(
                                            "the_long_travail", "coloring"));
            check(recipe.isPresent(), "recipe loaded");
            var grid =
                    new net.minecraft.world.inventory.TransientCraftingContainer(
                            player.inventoryMenu, 3, 3);
            for (int slot = 0; slot < 9; slot++)
                grid.setItem(slot, new ItemStack(slot == 4 ? Items.BRUSH : Items.GLOWSTONE_DUST));
            var crafting = (net.minecraft.world.item.crafting.CraftingRecipe) recipe.orElseThrow();
            check(
                    crafting.matches(grid, player.level()),
                    "eight glowstone dust surrounding vanilla brush");
            check(
                    crafting.assemble(grid, player.level().registryAccess())
                            .is(ModRegistry.COLORING.get()),
                    "recipe result");
            grid.setItem(0, new ItemStack(Items.GLOWSTONE));
            check(!crafting.matches(grid, player.level()), "glowstone block is not dust");
            success = true;
            Files.writeString(
                    Path.of("checks.txt"),
                    "PASS "
                            + checks
                            + " assertions; 9900 transitions; 30000 reads; 40000 randomized menu"
                            + " actions; vanilla anvil, persistence, menu conservation.\n"
                            + "Actual client/full pack/multiplayer: NOT_RUN\n");
        } catch (Throwable failure) {
            failure.printStackTrace();
        } finally {
            try {
                Files.writeString(Path.of("result.txt"), success ? "PASS" : "FAIL");
            } catch (Exception failure) {
                failure.printStackTrace();
            }
            event.getServer().halt(false);
        }
    }
}
