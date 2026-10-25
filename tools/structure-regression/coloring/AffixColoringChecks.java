package travail.smoke;

import com.thelongtravail.item.NameColoring;
import com.thelongtravail.registry.ModRegistry;

import dev.shadowsoffire.apotheosis.adventure.affix.AffixHelper;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.*;

import java.nio.file.*;
import java.util.List;

final class AffixColoringChecks {
    static void preview(ItemStack stack) {
        stack.getOrCreateTagElement(AffixHelper.AFFIX_DATA)
                .putString(AffixHelper.RARITY, "apotheosis:mythic");
        AffixHelper.setName(
                stack,
                Component.translatable(
                        "misc.apotheosis.affix_name.three",
                        Component.literal("前缀"),
                        Component.literal("BASE"),
                        Component.literal("后缀")));
    }

    static void run(net.minecraft.server.level.ServerPlayer player) throws Exception {
        for (Item item : List.of(Items.DIAMOND_SWORD, ModRegistry.LONG_TRAVAIL.get())) {
            for (int effect = 0; effect < 10; effect++) {
                ItemStack stack = new ItemStack(item);
                NameColoring.apply(stack, effect);
                for (String rarity : List.of("common", "uncommon", "rare", "epic", "mythic")) {
                    stack.getOrCreateTagElement(AffixHelper.AFFIX_DATA)
                            .putString(AffixHelper.RARITY, "apotheosis:" + rarity);
                    AffixHelper.setName(
                            stack,
                            Component.translatable(
                                    "misc.apotheosis.affix_name.three",
                                    Component.literal("Prefix"),
                                    Component.literal("BASE"),
                                    Component.literal("Suffix")));
                    stack.setHoverName(Component.literal("Current"));
                    var name = stack.getHoverName();
                    if (!name.getString().contains("Current"))
                        throw new AssertionError("affix base name lost");
                    if (NameColoring.selection(stack) != effect)
                        throw new AssertionError("selection lost");
                    String marker = name.getStyle().getInsertion();
                    if (effect == 9
                            ? marker != null
                            : marker == null || marker.startsWith("the_long_travail:apotheosis:"))
                        throw new AssertionError(
                                "affix selection precedence: " + effect + " " + marker);
                    if (!AffixHelper.getRarity(stack).isBound())
                        throw new AssertionError("rarity fixture invalid: " + rarity);
                    if (effect == 9
                            && !name.getStyle()
                                    .getColor()
                                    .equals(AffixHelper.getRarity(stack).get().getColor()))
                        throw new AssertionError("clear must retain quality color");
                }
            }
        }
        var level = player.serverLevel();
        var pos = new net.minecraft.core.BlockPos(0, 80, 0);
        level.setBlockAndUpdate(
                pos,
                dev.shadowsoffire.apotheosis.adventure.Adventure.Blocks.REFORGING_TABLE
                        .get()
                        .defaultBlockState());
        int reforges = 0;
        for (int effect = 0; effect < 10; effect++) {
            ItemStack input = new ItemStack(Items.DIAMOND_SWORD);
            input.enchant(net.minecraft.world.item.enchantment.Enchantments.SHARPNESS, 3);
            input.setDamageValue(7);
            input.getOrCreateTag().putString("external_storage_id", "unchanged");
            NameColoring.apply(input, effect);
            for (int round = 0; round < 30; round++) {
                NameColoring.apply(input, (effect + round) % 10);
                var anvil =
                        new net.minecraft.world.inventory.AnvilMenu(
                                20,
                                player.getInventory(),
                                net.minecraft.world.inventory.ContainerLevelAccess.NULL);
                anvil.getSlot(0).set(input);
                anvil.setItemName("Round " + round);
                input = anvil.getSlot(2).getItem().copy();
                if (input.isEmpty()) throw new AssertionError("stress anvil empty");
                var menu =
                        new dev.shadowsoffire.apotheosis.adventure.affix.reforging.ReforgingMenu(
                                19, player.getInventory(), pos);
                player.containerMenu = menu;
                player.giveExperienceLevels(100);
                menu.getSlot(0).set(input);
                menu.getSlot(1)
                        .set(
                                new ItemStack(
                                        dev.shadowsoffire.apotheosis.adventure.Adventure.Items
                                                .MYTHIC_MATERIAL
                                                .get(),
                                        64));
                menu.getSlot(2)
                        .set(
                                new ItemStack(
                                        dev.shadowsoffire.apotheosis.adventure.Adventure.Items
                                                .SIGIL_OF_REBIRTH
                                                .get(),
                                        64));
                menu.slotsChanged(new net.minecraft.world.SimpleContainer(1));
                var output = menu.getSlot(3 + round % 3);
                if (!output.hasItem() || !output.mayPickup(player))
                    throw new AssertionError("real reforge output unavailable");
                ItemStack next = output.getItem().copy();
                if (NameColoring.selection(next) != (effect + round) % 10)
                    throw new AssertionError("real reforge lost selection");
                if (!next.getHoverName().getString().contains("Round " + round))
                    throw new AssertionError("real reforge lost custom name");
                if (next.getDamageValue() != 7
                        || !next.getTag().getString("external_storage_id").equals("unchanged"))
                    throw new AssertionError("real reforge changed unrelated data");
                output.onTake(player, next);
                if (menu.getSlot(0).hasItem())
                    throw new AssertionError("reforge input not consumed");
                menu.removed(player);
                input = next;
                reforges++;
            }
        }
        Files.writeString(
                Path.of("affix-checks.txt"),
                "PASS: 100 name/quality mutation cases; "
                        + reforges
                        + " actual ReforgingMenu preview/take cycles, thirty sequential"
                        + " recolor/anvil/reforge cycles per effect.\n");
    }
}
