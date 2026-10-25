package travail.smoke;

import com.thelongtravail.item.NameColoring;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.*;
import net.minecraftforge.registries.ForgeRegistries;

import java.nio.file.*;
import java.util.*;

// 通过反射调用可选模组的公共接口，使基础夹具可独立加载。
final class ExtraColoringChecks {
    static void run(ServerPlayer player) throws Exception {
        var lore = Class.forName("com.aizistral.enigmaticlegacy.helpers.ItemLoreHelper");
        var merge = lore.getMethod("mergeDisplayData", ItemStack.class, ItemStack.class);
        var rune = Class.forName("org.violetmoon.quark.content.tools.module.ColorRunesModule");
        var getRune = rune.getMethod("getAppliedStackColor", ItemStack.class);
        var build = Class.forName("slimeknights.tconstruct.library.tools.helper.ToolBuildHandler");
        var buildRandom =
                Arrays.stream(build.getMethods())
                        .filter(
                                m ->
                                        m.getName().equals("buildItemRandomMaterials")
                                                && m.getParameterCount() == 2)
                        .findFirst()
                        .orElseThrow();
        var pickaxe = ForgeRegistries.ITEMS.getValue(new ResourceLocation("tconstruct", "pickaxe"));
        if (pickaxe == Items.AIR) throw new AssertionError("Tinkers fixture missing");
        for (int effect = 0; effect < 10; effect++) {
            ItemStack stack =
                    (ItemStack)
                            buildRandom.invoke(
                                    null, pickaxe, net.minecraft.util.RandomSource.create(10));
            String nativeName = stack.getHoverName().getString();
            NameColoring.apply(stack, effect);
            if (!stack.getHoverName().getString().equals(nativeName))
                throw new AssertionError("material name changed by coloring");
            var snapshot = stack.save(new net.minecraft.nbt.CompoundTag());
            for (int count = 0; count < 50; count++) stack.getHoverName().getVisualOrderText();
            if (!snapshot.equals(stack.save(new net.minecraft.nbt.CompoundTag())))
                throw new AssertionError("dynamic name mutates tool");
            ItemStack fragment = new ItemStack(Items.PAPER);
            fragment.setHoverName(Component.literal("Lore merged 星河"));
            var lines = new net.minecraft.nbt.ListTag();
            lines.add(
                    net.minecraft.nbt.StringTag.valueOf(
                            Component.Serializer.toJson(Component.literal("Preserved lore"))));
            fragment.getOrCreateTagElement("display").put("Lore", lines);
            stack = (ItemStack) merge.invoke(null, fragment, stack.copy());
            if (NameColoring.selection(stack) != effect
                    || !stack.getHoverName().getString().contains("Lore merged"))
                throw new AssertionError("Enigmatic display merge lost selection/name");
            for (String color : List.of("red", "rainbow")) {
                stack.getOrCreateTag().putString("quark:RuneColor", color);
                if (getRune.invoke(null, stack) == null)
                    throw new AssertionError("Quark rune fixture invalid");
                NameColoring.apply(stack, effect);
                stack.getHoverName().getString();
                if (!color.equals(stack.getTag().getString("quark:RuneColor")))
                    throw new AssertionError("rune modified");
            }
            var anvil = new AnvilMenu(20, player.getInventory(), ContainerLevelAccess.NULL);
            anvil.getSlot(0).set(stack);
            anvil.setItemName("Renamed again");
            if (anvil.getSlot(2).hasItem()
                    && NameColoring.selection(anvil.getSlot(2).getItem()) != effect)
                throw new AssertionError("combined anvil lost coloring");
        }
        Files.writeString(
                Path.of("extra-checks.txt"),
                "PASS: 10 Tinkers material-name stacks + Enigmatic display merge + red/rainbow"
                    + " Quark rune API combinations. Workstation material replacement, full render"
                    + " stack and remote multiplayer NOT_RUN.\n");
    }
}
