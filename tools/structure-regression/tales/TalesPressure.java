package travail.tales;

import com.thelongtravail.boundless.EnchantmentTransfer;
import com.thelongtravail.registry.ModRegistry;
import java.nio.file.*;
import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.enchantment.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.registries.ForgeRegistries;

final class TalesPressure {
    private static int checks;

    private static void check(boolean ok, String label) {
        checks++;
        if (!ok) throw new AssertionError(label);
    }

    private static ItemStack enchanted(Item item, Enchantment enchantment, int level) {
        var stack = new ItemStack(item);
        if (level > 0) EnchantmentHelper.setEnchantments(Map.of(enchantment, level), stack);
        return stack;
    }

    static void run(MinecraftServer server) throws Exception {
        long began = System.nanoTime();
        var tales = ModRegistry.ANCIENT_STRANGE_TALES.get();
        var enchantments = new ArrayList<>(ForgeRegistries.ENCHANTMENTS.getValues());
        enchantments.sort(
                Comparator.comparing(e -> ForgeRegistries.ENCHANTMENTS.getKey(e).toString()));
        for (int n = 0; n < 100000; n++) {
            var e = enchantments.get(n % enchantments.size());
            int a = 1 + n % 255, b = (n * 37) % 256;
            boolean extract = n % 2 == 0;
            var source = enchanted(extract ? Items.ENCHANTED_BOOK : tales, e, a);
            var target = enchanted(extract ? tales : Items.ENCHANTED_BOOK, e, b);
            source.getOrCreateTag().putInt("pressureMarker", n);
            var before = source.copy();
            var beforeTarget = target.copy();
            var filter = EnchantmentTransfer.Filter.values()[(n / enchantments.size()) % 3];
            boolean accepted =
                    filter == EnchantmentTransfer.Filter.ALL
                            || e.isCurse() == (filter == EnchantmentTransfer.Filter.CURSE);
            var result =
                    EnchantmentTransfer.evaluate(
                            source,
                            target,
                            filter,
                            new EnchantmentTransfer.Materials(true, true, true));
            check(result.failure() == null, "no callback failure");
            check(
                    ItemStack.matches(source, before) && ItemStack.matches(target, beforeTarget),
                    "inputs immutable");
            if (!accepted || b == 255) check(result.result() == null, "filtered or capped");
            else {
                var output = result.result();
                check(output != null, "registered enchantment transfers");
                int expected = b == 0 ? a : Math.min(255, Math.max(a, b) + 1);
                check(
                        EnchantmentHelper.getEnchantments(output.target()).getOrDefault(e, 0)
                                == expected,
                        "merge level");
                check(
                        !EnchantmentHelper.getEnchantments(output.source()).containsKey(e),
                        "source consumed once");
                check(
                        output.source().getOrCreateTag().getInt("pressureMarker") == n,
                        "metadata retained");
                check(
                        ItemStack.matches(
                                ItemStack.of(output.target().save(new CompoundTag())),
                                output.target()),
                        "NBT roundtrip");
            }
        }
        int nonempty = 0;
        for (int n = 0; n < 10000; n++) {
            var result =
                    EnchantmentHelper.selectEnchantment(
                            net.minecraft.util.RandomSource.create(n),
                            new ItemStack(tales),
                            1 + n % 100,
                            n % 2 == 0);
            if (!result.isEmpty()) nonempty++;
            for (var e : result) check(e.level > 0, "valid selected level");
        }
        check(nonempty > 0, "candidate selection coverage");
        var world = server.overworld();
        var player = FakePlayerFactory.getMinecraft(world);
        var pos = new BlockPos(12, 100, 12);
        world.setBlockAndUpdate(pos, Blocks.ENCHANTING_TABLE.defaultBlockState());
        String menuClass = "";
        for (int n = 0; n < 1000; n++) {
            player.getInventory().clearContent();
            player.experienceLevel = 1000;
            player.totalExperience = 1000000;
            player.experienceProgress = 0;
            var menu =
                    (EnchantmentMenu)
                            world.getBlockState(pos)
                                    .getMenuProvider(world, pos)
                                    .createMenu(10, player.getInventory(), player);
            menuClass = menu.getClass().getName();
            menu.getSlot(0).set(new ItemStack(tales));
            menu.getSlot(1).set(new ItemStack(Items.LAPIS_LAZULI, 64));
            check(menu.costs[0] > 0 && menu.clickMenuButton(player, 0), "real enchanting action");
            check(
                    menu.getSlot(0).getItem().is(tales) && menu.getSlot(0).getItem().isEnchanted(),
                    "identity and enchantment retained");
            check(menu.getSlot(1).getItem().getCount() == 63, "fuel consumed once");
            var anvil = new AnvilMenu(11, player.getInventory());
            anvil.getSlot(0).set(new ItemStack(tales));
            anvil.getSlot(1).set(enchanted(Items.ENCHANTED_BOOK, Enchantments.SHARPNESS, 1));
            check(anvil.getSlot(2).getItem().is(tales), "anvil receiver");
            var crafting =
                    new CraftingMenu(
                            12, player.getInventory(), ContainerLevelAccess.create(world, pos));
            crafting.getSlot(1).set(new ItemStack(tales));
            crafting.getSlot(2).set(enchanted(Items.DIAMOND_SWORD, Enchantments.SHARPNESS, 1));
            crafting.getSlot(3).set(new ItemStack(Items.QUARTZ));
            var output = crafting.getSlot(0).remove(1);
            check(output.is(tales) && output.isEnchanted(), "real crafting output");
            crafting.getSlot(0).onTake(player, output);
            check(
                    crafting.getSlot(1).getItem().isEmpty()
                            && crafting.getSlot(3).getItem().isEmpty(),
                    "craft ingredients consumed");
        }
        boolean apoth = ModList.get().isLoaded("apotheosis");
        check(
                menuClass.contains(apoth ? "ApothEnchantmentMenu" : "EnchantmentMenu"),
                "correct active menu");
        String report =
                "PASS checks="
                        + checks
                        + " enchantments="
                        + enchantments.size()
                        + " transfers=100000 selections=10000 actualTables=1000 anvils=1000 crafts=1000"
                        + " menu="
                        + menuClass
                        + " seconds="
                        + (System.nanoTime() - began) / 1e9;
        Files.writeString(Path.of("tales-pressure-result.txt"), report);
        Files.writeString(
                Path.of("tales-loaded-mods.txt"),
                String.join(
                        "\n",
                        ModList.get().getMods().stream()
                                .map(m -> m.getModId() + " " + m.getVersion())
                                .sorted()
                                .toList()));
        System.out.println(report);
    }
}
