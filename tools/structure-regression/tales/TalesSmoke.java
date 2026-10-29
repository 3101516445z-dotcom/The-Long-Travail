package travail.tales;

import com.thelongtravail.boundless.EnchantmentTransfer;
import com.thelongtravail.boundless.EnchantmentTransferRecipe;
import com.thelongtravail.registry.ModRegistry;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.enchantment.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChiseledBookShelfBlockEntity;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.fml.common.Mod;

@Mod("travail_smoke")
public final class TalesSmoke {
    private MinecraftServer server;
    private boolean done;
    private int checks;
    private static final Enchantment SHARP = Enchantments.SHARPNESS;
    private static final Enchantment CURSE = Enchantments.VANISHING_CURSE;

    private static String failStage = "";
    private static int callbackCount;
    private static final net.minecraftforge.registries.DeferredRegister<Enchantment> ENCHANTMENTS =
            net.minecraftforge.registries.DeferredRegister.create(
                    net.minecraftforge.registries.ForgeRegistries.ENCHANTMENTS, "travail_smoke");
    private static final net.minecraftforge.registries.RegistryObject<Enchantment> BROKEN =
            ENCHANTMENTS.register(
                    "broken",
                    () ->
                            new Enchantment(
                                    Enchantment.Rarity.COMMON,
                                    EnchantmentCategory.WEAPON,
                                    new net.minecraft.world.entity.EquipmentSlot[] {
                                        net.minecraft.world.entity.EquipmentSlot.MAINHAND
                                    }) {
                                private void probe(String stage) {
                                    callbackCount++;
                                    if (failStage.equals(stage))
                                        throw new IllegalStateException("injected " + stage);
                                }

                                @Override
                                public boolean isCurse() {
                                    probe("isCurse");
                                    return false;
                                }

                                @Override
                                public boolean canEnchant(ItemStack stack) {
                                    if (failStage.equals("canEnchant"))
                                        stack.getOrCreateTag()
                                                .putBoolean("mutated_by_callback", true);
                                    probe("canEnchant");
                                    return true;
                                }

                                @Override
                                protected boolean checkCompatibility(Enchantment other) {
                                    probe("isCompatibleWith");
                                    return true;
                                }
                            });

    public TalesSmoke() {
        ENCHANTMENTS.register(
                net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext.get().getModEventBus());
        MinecraftForge.EVENT_BUS.addListener(
                (ServerStartedEvent event) -> server = event.getServer());
        MinecraftForge.EVENT_BUS.addListener(this::tick);
    }

    private void check(boolean value, String label) {
        checks++;
        if (!value) throw new AssertionError(label);
    }

    private void tick(TickEvent.ServerTickEvent event) {
        if (server == null || done || event.phase != TickEvent.Phase.END) return;
        done = true;
        String result;
        try {
            run();
            result = "PASS checks=" + checks;
        } catch (Throwable error) {
            error.printStackTrace();
            result = "FAIL checks=" + checks + " " + error;
        }
        System.out.println("TALES " + result);
        try {
            Files.writeString(Path.of("tales-result.txt"), result);
        } catch (Exception error) {
            throw new RuntimeException(error);
        }
        server.halt(false);
    }

    private ItemStack tales() {
        return new ItemStack(ModRegistry.ANCIENT_STRANGE_TALES.get());
    }

    private ItemStack enchanted(Item item, Enchantment enchantment, int level) {
        ItemStack stack = new ItemStack(item);
        if (item == Items.ENCHANTED_BOOK)
            EnchantedBookItem.addEnchantment(stack, new EnchantmentInstance(enchantment, level));
        else EnchantmentHelper.setEnchantments(Map.of(enchantment, level), stack);
        return stack;
    }

    private int level(ItemStack stack, Enchantment enchantment) {
        return EnchantmentHelper.getEnchantments(stack).getOrDefault(enchantment, 0);
    }

    private ItemStack book(int level) {
        return enchanted(ModRegistry.ANCIENT_STRANGE_TALES.get(), SHARP, level);
    }

    private void run() throws Exception {
        var all = EnchantmentTransfer.Filter.ALL;
        ItemStack sword = enchanted(Items.DIAMOND_SWORD, SHARP, 10);
        sword.enchant(CURSE, 1);
        sword.setDamageValue(19);
        sword.setHoverName(Component.literal("preserved"));
        sword.getOrCreateTag().putString("custom", "untouched");
        ItemStack original = sword.copy();
        var r = EnchantmentTransfer.transfer(sword, book(250), all);
        check(
                r != null && level(r.target(), SHARP) == 255 && level(r.source(), SHARP) == 5,
                "overflow conservation source="
                        + sword.save(new CompoundTag())
                        + " result="
                        + (r == null
                                ? "null"
                                : r.source().save(new CompoundTag())
                                        + " -> "
                                        + r.target().save(new CompoundTag())));
        check(level(r.target(), CURSE) == 1 && level(r.source(), CURSE) == 0, "unfiltered curse");
        check(ItemStack.matches(sword, original), "pure inputs");
        check(
                r.source().getDamageValue() == 19
                        && r.source().getHoverName().getString().equals("preserved")
                        && r.source().getTag().getString("custom").equals("untouched"),
                "other NBT");
        check(
                EnchantmentTransfer.transfer(
                                enchanted(Items.DIAMOND_SWORD, SHARP, 5), book(255), all)
                        == null,
                "full destination");
        check(EnchantmentTransfer.transfer(book(1), tales(), all) == null, "two tales forbidden");
        check(
                EnchantmentTransfer.transfer(book(1), new ItemStack(Items.DIAMOND_HELMET), all)
                        == null,
                "equipment applicability");
        check(
                EnchantmentTransfer.transfer(
                                enchanted(Items.DIAMOND_SWORD, Enchantments.SMITE, 3), book(1), all)
                        == null,
                "book conflicts");
        r = EnchantmentTransfer.transfer(sword, tales(), EnchantmentTransfer.Filter.NON_CURSE);
        check(
                level(r.target(), SHARP) == 10
                        && level(r.target(), CURSE) == 0
                        && level(r.source(), CURSE) == 1,
                "honey filter");
        r = EnchantmentTransfer.transfer(sword, tales(), EnchantmentTransfer.Filter.CURSE);
        check(
                level(r.target(), SHARP) == 0
                        && level(r.target(), CURSE) == 1
                        && level(r.source(), SHARP) == 10,
                "eye filter");
        r =
                EnchantmentTransfer.transfer(
                        enchanted(Items.ENCHANTED_BOOK, SHARP, 10), book(250), all);
        check(
                level(r.source(), SHARP) == 5 && level(r.target(), SHARP) == 255,
                "stored enchantment decrease");
        r = EnchantmentTransfer.transfer(enchanted(Items.ENCHANTED_BOOK, SHARP, 10), tales(), all);
        check(
                r.source().is(Items.ENCHANTED_BOOK) && level(r.source(), SHARP) == 0,
                "stored enchantment removal");
        r = EnchantmentTransfer.transfer(book(10), new ItemStack(Items.BOOK), all);
        check(
                r.target().is(Items.ENCHANTED_BOOK)
                        && level(r.target(), SHARP) == 10
                        && level(r.source(), SHARP) == 0,
                "plain book conversion");
        ItemStack unknown = sword.copy();
        unknown.getEnchantmentTags().getCompound(0).putString("id", "missing:enchantment");
        check(
                EnchantmentTransfer.transfer(unknown, tales(), all) == null,
                "unknown data fails closed");
        ItemStack duplicate = sword.copy();
        duplicate.getEnchantmentTags().add(duplicate.getEnchantmentTags().getCompound(0).copy());
        check(
                EnchantmentTransfer.transfer(duplicate, tales(), all) == null,
                "duplicate data fails closed");
        for (int targetLevel : new int[] {0, 1, 5, 127, 250, 254, 255}) {
            for (int sourceLevel : new int[] {1, 2, 5, 128, 255}) {
                r =
                        EnchantmentTransfer.transfer(
                                enchanted(Items.DIAMOND_SWORD, SHARP, sourceLevel),
                                targetLevel == 0 ? tales() : book(targetLevel),
                                all);
                check(
                        targetLevel == 255
                                ? r == null
                                : r != null
                                        && level(r.source(), SHARP) + level(r.target(), SHARP)
                                                == targetLevel + sourceLevel
                                        && level(r.target(), SHARP) <= 255,
                        "level conservation matrix");
            }
        }

        var world = server.overworld();
        var player = FakePlayerFactory.getMinecraft(world);
        player.experienceLevel = 100;
        var recipe =
                new EnchantmentTransferRecipe(
                        ResourceLocation.fromNamespaceAndPath("the_long_travail", "test"),
                        CraftingBookCategory.MISC);
        for (boolean extraction : new boolean[] {true, false})
            for (Item filter :
                    new Item[] {Items.AIR, Items.HONEY_BOTTLE, Items.FERMENTED_SPIDER_EYE}) {
                var grid = new TransientCraftingContainer(player.inventoryMenu, 2, 2);
                ItemStack from = extraction ? sword.copy() : book(10);
                if (!extraction) from.enchant(CURSE, 1);
                grid.setItem(0, extraction ? tales() : from);
                grid.setItem(1, extraction ? from : new ItemStack(Items.DIAMOND_SWORD));
                grid.setItem(2, new ItemStack(extraction ? Items.QUARTZ : Items.LAPIS_LAZULI, 16));
                if (filter != Items.AIR) grid.setItem(3, new ItemStack(filter, 4));
                check(recipe.matches(grid, world), "six variants match");
                var out = recipe.assemble(grid, world.registryAccess());
                var remain = recipe.getRemainingItems(grid);
                check(
                        out.getCount() == 1 && remain.get(extraction ? 1 : 0).getCount() == 1,
                        "single outputs");
                check(
                        (filter == Items.HONEY_BOTTLE) == remain.get(3).is(Items.GLASS_BOTTLE),
                        "bottle remainder");
                check(
                        level(out, SHARP) == (filter == Items.FERMENTED_SPIDER_EYE ? 0 : 10)
                                && level(out, CURSE) == (filter == Items.HONEY_BOTTLE ? 0 : 1),
                        "six variant content");
            }
        var invalid = new TransientCraftingContainer(player.inventoryMenu, 3, 3);
        invalid.setItem(0, tales());
        invalid.setItem(1, sword.copy());
        invalid.setItem(2, new ItemStack(Items.QUARTZ));
        invalid.setItem(3, new ItemStack(Items.HONEY_BOTTLE));
        invalid.setItem(4, new ItemStack(Items.FERMENTED_SPIDER_EYE));
        check(!recipe.matches(invalid, world), "dual filters rejected");
        invalid.setItem(4, ItemStack.EMPTY);
        invalid.setItem(1, tales());
        check(!recipe.matches(invalid, world), "two tales recipe rejected");

        for (String stage : List.of("isCurse", "canEnchant", "isCompatibleWith")) {
            failStage = stage;
            ItemStack from = book(2);
            from.enchant(BROKEN.get(), 3);
            ItemStack to =
                    stage.equals("isCompatibleWith")
                            ? enchanted(Items.DIAMOND_SWORD, SHARP, 1)
                            : new ItemStack(Items.DIAMOND_SWORD);
            ItemStack fromBefore = from.copy(), toBefore = to.copy();
            var evaluation =
                    EnchantmentTransfer.evaluate(from, to, EnchantmentTransfer.Filter.NON_CURSE);
            check(
                    evaluation.result() == null
                            && evaluation.failure() != null
                            && evaluation.failure().stage().equals(stage),
                    "caught callback " + stage);
            check(
                    ItemStack.matches(from, fromBefore) && ItemStack.matches(to, toBefore),
                    "callback inputs preserved " + stage);
        }
        failStage = "canEnchant";
        var failedMenu =
                new CraftingMenu(
                        23,
                        player.getInventory(),
                        ContainerLevelAccess.create(world, new BlockPos(0, 100, 0)));
        failedMenu
                .getSlot(1)
                .set(enchanted(ModRegistry.ANCIENT_STRANGE_TALES.get(), BROKEN.get(), 3));
        failedMenu.getSlot(2).set(new ItemStack(Items.DIAMOND_SWORD));
        failedMenu.getSlot(3).set(new ItemStack(Items.LAPIS_LAZULI, 8));
        var failedGrid = (CraftingContainer) failedMenu.getSlot(1).container;
        int calls = callbackCount;
        for (int i = 0; i < 20; i++) {
            check(
                    !recipe.matches(failedGrid, world)
                            && recipe.assemble(failedGrid, world.registryAccess()).isEmpty(),
                    "failed preview empty");
            recipe.getRemainingItems(failedGrid);
        }
        check(callbackCount == calls, "failed input evaluated once");
        var notices = new ArrayList<Component>();
        var noticePlayer =
                new net.minecraftforge.common.util.FakePlayer(
                        world,
                        new com.mojang.authlib.GameProfile(UUID.randomUUID(), "FailureNotice")) {
                    @Override
                    public void displayClientMessage(Component message, boolean overlay) {
                        notices.add(message);
                    }
                };
        for (int i = 0; i < 20; i++)
            com.thelongtravail.boundless.TransferNotices.blocked(failedMenu, noticePlayer);
        check(notices.size() == 1, "same failed input notice emitted once");
        failedMenu.getSlot(3).set(new ItemStack(Items.LAPIS_LAZULI, 9));
        com.thelongtravail.boundless.TransferNotices.blocked(failedMenu, noticePlayer);
        check(notices.size() == 2, "changed input can report a new failure");
        var savedInputs = new ArrayList<ItemStack>();
        for (int i = 1; i <= 9; i++) savedInputs.add(failedMenu.getSlot(i).getItem().copy());
        for (ClickType click : List.of(ClickType.PICKUP, ClickType.QUICK_MOVE)) {
            failedMenu.getSlot(0).set(new ItemStack(Items.DIAMOND_SWORD));
            failedMenu.clicked(0, 0, click, player);
            check(
                    failedMenu.getSlot(0).getItem().isEmpty() && failedMenu.getCarried().isEmpty(),
                    "stale output blocked " + click);
            for (int i = 1; i <= 9; i++)
                check(
                        ItemStack.matches(savedInputs.get(i - 1), failedMenu.getSlot(i).getItem()),
                        "failed take conserves input " + i);
        }
        failStage = "";
        failedMenu.getSlot(3).set(new ItemStack(Items.LAPIS_LAZULI, 7));
        check(
                recipe.matches(failedGrid, world)
                        && EnchantmentTransferRecipe.failure(failedGrid) == null,
                "changed inputs retry successfully");

        // 通过实际 ResultSlot 检查输入消耗和剩余物返还。
        var menu =
                new CraftingMenu(
                        1,
                        player.getInventory(),
                        ContainerLevelAccess.create(world, new BlockPos(0, 100, 0)));
        menu.getSlot(1).set(tales());
        menu.getSlot(2).set(sword.copy());
        menu.getSlot(3).set(new ItemStack(Items.QUARTZ, 3));
        menu.getSlot(4).set(new ItemStack(Items.HONEY_BOTTLE));
        check(EnchantmentTransfer.isTales(menu.getSlot(0).getItem()), "real crafting preview");
        ItemStack output = menu.getSlot(0).remove(1);
        menu.getSlot(0).onTake(player, output);
        check(
                level(output, SHARP) == 10
                        && level(menu.getSlot(2).getItem(), SHARP) == 0
                        && level(menu.getSlot(2).getItem(), CURSE) == 1,
                "real extraction");
        check(
                menu.getSlot(1).getItem().isEmpty()
                        && menu.getSlot(3).getItem().getCount() == 2
                        && menu.getSlot(4).getItem().is(Items.GLASS_BOTTLE),
                "real material consumption");
        player.getInventory().clearContent();
        var stackedMenu =
                new CraftingMenu(
                        5,
                        player.getInventory(),
                        ContainerLevelAccess.create(world, new BlockPos(0, 100, 0)));
        ItemStack sticks = enchanted(Items.STICK, SHARP, 5);
        sticks.setCount(3);
        stackedMenu.getSlot(1).set(tales());
        stackedMenu.getSlot(2).set(sticks);
        stackedMenu.getSlot(3).set(new ItemStack(Items.QUARTZ, 3));
        ItemStack shifted = stackedMenu.quickMoveStack(player, 0);
        check(
                EnchantmentTransfer.isTales(shifted) && level(shifted, SHARP) == 5,
                "shift-click result");
        check(
                stackedMenu.getSlot(2).getItem().getCount() == 2
                        && level(stackedMenu.getSlot(2).getItem(), SHARP) == 5,
                "stacked source handles one item");
        check(
                player.getInventory().items.stream()
                                .filter(v -> v.is(Items.STICK) && level(v, SHARP) == 0)
                                .mapToInt(ItemStack::getCount)
                                .sum()
                        == 1,
                "changed remainder goes to inventory");
        check(
                player.getInventory().items.stream()
                                .filter(EnchantmentTransfer::isTales)
                                .mapToInt(ItemStack::getCount)
                                .sum()
                        == 1,
                "no duplicated book");
        player.getInventory().clearContent();
        var reverseMenu =
                new CraftingMenu(
                        6,
                        player.getInventory(),
                        ContainerLevelAccess.create(world, new BlockPos(0, 100, 0)));
        reverseMenu.getSlot(1).set(book(20));
        reverseMenu.getSlot(2).set(new ItemStack(Items.DIAMOND_SWORD));
        reverseMenu.getSlot(3).set(new ItemStack(Items.LAPIS_LAZULI, 3));
        ItemStack reverseOutput = reverseMenu.getSlot(0).remove(1);
        reverseMenu.getSlot(0).onTake(player, reverseOutput);
        check(
                level(reverseOutput, SHARP) == 20
                        && EnchantmentTransfer.isTales(reverseMenu.getSlot(1).getItem())
                        && level(reverseMenu.getSlot(1).getItem(), SHARP) == 0,
                "reverse real menu retains book");
        check(
                reverseMenu.getSlot(2).getItem().isEmpty()
                        && reverseMenu.getSlot(3).getItem().getCount() == 2,
                "reverse consumption");
        var anvil = new AnvilMenu(2, player.getInventory());
        anvil.getSlot(0).set(tales());
        anvil.getSlot(1)
                .set(enchanted(Items.ENCHANTED_BOOK, Enchantments.ALL_DAMAGE_PROTECTION, 4));
        check(
                EnchantmentTransfer.isTales(anvil.getSlot(2).getItem())
                        && level(anvil.getSlot(2).getItem(), Enchantments.ALL_DAMAGE_PROTECTION)
                                == 4,
                "anvil book reception");
        anvil.getSlot(0).set(book(10));
        anvil.getSlot(1).set(enchanted(Items.ENCHANTED_BOOK, SHARP, 1));
        check(level(anvil.getSlot(2).getItem(), SHARP) == SHARP.getMaxLevel(), "vanilla anvil cap");
        anvil.getSlot(1).set(ItemStack.EMPTY);
        anvil.setItemName("renamed");
        check(
                level(anvil.getSlot(2).getItem(), SHARP) == 10
                        && anvil.getSlot(2).getItem().getHoverName().getString().equals("renamed"),
                "rename preserves high level");
        anvil.getSlot(0).set(new ItemStack(Items.DIAMOND_SWORD));
        anvil.getSlot(1).set(book(5));
        anvil.setItemName("");
        check(anvil.getSlot(2).getItem().isEmpty(), "tales cannot be consumed on anvil");
        anvil.getSlot(0).set(book(5));
        anvil.getSlot(1).set(enchanted(Items.ENCHANTED_BOOK, Enchantments.SMITE, 5));
        check(anvil.getSlot(2).getItem().isEmpty(), "anvil conflict rejected");

        var enchanting = new EnchantmentMenu(3, player.getInventory());
        var selection =
                EnchantmentMenu.class.getDeclaredMethod(
                        "getEnchantmentList", ItemStack.class, int.class, int.class);
        selection.setAccessible(true);
        for (int cost = 1; cost <= 30; cost++) {
            @SuppressWarnings("unchecked")
            var vanilla =
                    (List<EnchantmentInstance>)
                            selection.invoke(enchanting, new ItemStack(Items.BOOK), 2, cost);
            @SuppressWarnings("unchecked")
            var custom = (List<EnchantmentInstance>) selection.invoke(enchanting, tales(), 2, cost);
            check(vanilla.size() == custom.size(), "table selection count");
            for (int i = 0; i < vanilla.size(); i++)
                check(
                        vanilla.get(i).enchantment == custom.get(i).enchantment
                                && vanilla.get(i).level == custom.get(i).level,
                        "table seeded parity");
        }
        check(tales().isEnchantable() && !book(1).isEnchantable(), "table only unenchanted");
        BlockPos pos = new BlockPos(8, 100, 8);
        world.setBlockAndUpdate(pos, Blocks.ENCHANTING_TABLE.defaultBlockState());
        var table =
                new EnchantmentMenu(
                        4, player.getInventory(), ContainerLevelAccess.create(world, pos));
        table.getSlot(0).set(tales());
        table.getSlot(1).set(new ItemStack(Items.LAPIS_LAZULI, 3));
        check(table.costs[0] > 0 && table.clickMenuButton(player, 0), "actual enchanting action");
        check(
                EnchantmentTransfer.isTales(table.getSlot(0).getItem())
                        && table.getSlot(0).getItem().isEnchanted(),
                "table retains tales identity");
        check(table.getSlot(1).getItem().getCount() == 2, "table lapis cost");
        check(tales().is(ItemTags.BOOKSHELF_BOOKS), "bookshelf tag loaded");
        world.setBlockAndUpdate(pos, Blocks.CHISELED_BOOKSHELF.defaultBlockState());
        var shelf = (ChiseledBookShelfBlockEntity) world.getBlockEntity(pos);
        check(shelf.canPlaceItem(0, book(200)), "shelf accepts tales");
        shelf.setItem(0, book(200));
        CompoundTag saved = shelf.saveWithFullMetadata();
        var restored = new ChiseledBookShelfBlockEntity(pos, world.getBlockState(pos));
        restored.setLevel(world);
        restored.load(saved);
        check(level(restored.removeItem(0, 1), SHARP) == 200, "shelf save and extraction");
        check(
                world.getRecipeManager()
                        .byKey(
                                ResourceLocation.fromNamespaceAndPath(
                                        "the_long_travail", "ancient_strange_tales"))
                        .isPresent(),
                "acquisition recipe loaded");
    }
}
