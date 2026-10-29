package travail.smoke;

import com.thelongtravail.api.TravailStateApi.AspectState;
import com.thelongtravail.config.DeepValleyItemsConfig;
import com.thelongtravail.registry.ModRegistry;
import com.thelongtravail.valley.*;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.*;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.common.*;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.nio.file.*;
import java.util.*;

@Mod("travail_smoke")
public final class TremorSmoke {
    private int checks;
    private final BlockPos pos = new BlockPos(0, 100, 0);
    private boolean cancel;
    private java.util.function.Consumer<BlockEvent.BreakEvent> mutation;

    static final class TestPlayer extends ServerPlayer {
        final List<String> feedback = new ArrayList<>();

        @Override
        public void displayClientMessage(
                net.minecraft.network.chat.Component message, boolean overlay) {
            if (!overlay) throw new AssertionError("Tremor feedback must use action bar");
            var content = message.getContents();
            feedback.add(
                    content
                                    instanceof
                                    net.minecraft.network.chat.contents.TranslatableContents
                                                    translated
                            ? translated.getKey()
                            : message.getString());
        }

        TestPlayer(ServerLevel level) {
            super(
                    level.getServer(),
                    level,
                    new com.mojang.authlib.GameProfile(UUID.randomUUID(), "TremorTest"));
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

    public TremorSmoke() {
        TremorFaultBlocks.BLOCKS.register(
                net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext.get().getModEventBus());
        MinecraftForge.EVENT_BUS.addListener(this::started);
        MinecraftForge.EVENT_BUS.register(this);
    }

    @SubscribeEvent
    public void protect(BlockEvent.BreakEvent e) {
        if (cancel && e.getPos().equals(pos)) e.setCanceled(true);
        if (mutation != null && e.getPos().equals(pos)) {
            var action = mutation;
            mutation = null;
            action.accept(e);
        }
    }

    private void check(boolean value, String message) {
        checks++;
        if (!value) throw new AssertionError(message);
    }

    private void started(ServerStartedEvent event) {
        if (Boolean.getBoolean("travail.tremor.restart")) {
            try {
                var level = event.getServer().overworld();
                var saved = (ChestBlockEntity) level.getBlockEntity(new BlockPos(128, 100, 148));
                check(saved != null, "saved chest exists");
                var tool = saved.getItem(0);
                check(tool.is(ModRegistry.HIGH_FREQUENCY_TREMOR.get()), "saved item identity");
                check(tool.getDamageValue() == 347, "saved damage");
                check(
                        "tremor-20261027".equals(tool.getOrCreateTag().getString("persistence")),
                        "saved NBT");
                int round = tool.getOrCreateTag().getInt("restartRound") + 1;
                tool.getOrCreateTag().putInt("restartRound", round);
                saved.setChanged();
                run(level);
                Files.writeString(
                        Path.of("tremor-result.txt"),
                        "PASS restart=" + round + " assertions=" + checks);
            } catch (Throwable failure) {
                TremorPressure.failed(event.getServer(), failure);
            } finally {
                event.getServer().halt(false);
            }
            return;
        }
        if (Boolean.getBoolean("travail.tremor.pressure")) {
            try {
                run(event.getServer().overworld());
                TremorPressure.begin(event.getServer(), checks);
            } catch (Throwable failure) {
                TremorPressure.failed(event.getServer(), failure);
            }
            return;
        }
        String result = "FAIL";
        try {
            run(event.getServer().overworld());
            result = "PASS " + checks + " tremor integration checks";
        } catch (Throwable e) {
            e.printStackTrace();
        } finally {
            try {
                Files.writeString(Path.of("tremor-result.txt"), result);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            System.out.println(result);
            event.getServer().halt(false);
        }
    }

    private List<ItemStack> drops(ServerLevel level) {
        return level.getEntitiesOfClass(ItemEntity.class, new AABB(pos).inflate(3)).stream()
                .map(ItemEntity::getItem)
                .toList();
    }

    private int count(ServerLevel level, Item item) {
        return drops(level).stream().filter(s -> s.is(item)).mapToInt(ItemStack::getCount).sum();
    }

    private void clear(ServerLevel level) {
        level.getEntitiesOfClass(ItemEntity.class, new AABB(pos).inflate(3))
                .forEach(ItemEntity::discard);
        level.removeBlock(pos, false);
    }

    static net.minecraft.nbt.CompoundTag repairedTag(ItemStack source) {
        var tag = source.getTag().copy();
        tag.putInt("Damage", 0);
        return tag;
    }

    private void run(ServerLevel level) throws Exception {
        var player = new TestPlayer(level);
        player.setGameMode(GameType.SURVIVAL);
        player.setPos(0.5, 101, 2.5);
        ItemStack tool = new ItemStack(ModRegistry.HIGH_FREQUENCY_TREMOR.get());
        player.setItemInHand(InteractionHand.MAIN_HAND, tool);
        check(tool.is(net.minecraft.tags.ItemTags.PICKAXES), "pickaxe tag");
        check(tool.getMaxDamage() == 2031, "current netherite maximum durability");
        tool.getOrCreateTag().putInt("Damage", 43);
        tool.hurtAndBreak(3000, player, p -> {});
        check(!tool.isEmpty() && tool.getDamageValue() == 43, "hurt cannot break or damage");
        tool.setDamageValue(90);
        check(tool.getDamageValue() == 43, "direct damage protected");
        tool.setDamageValue(4);
        check(tool.getDamageValue() == 4, "repair allowed");

        var recipe =
                level.getRecipeManager()
                        .byKey(new ResourceLocation("the_long_travail:high_frequency_tremor"))
                        .orElseThrow();
        var menu =
                new AbstractContainerMenu(null, 0) {
                    public ItemStack quickMoveStack(
                            net.minecraft.world.entity.player.Player p, int i) {
                        return ItemStack.EMPTY;
                    }

                    public boolean stillValid(net.minecraft.world.entity.player.Player p) {
                        return true;
                    }
                };
        var grid = new TransientCraftingContainer(menu, 3, 3);
        grid.setItem(0, new ItemStack(Items.ECHO_SHARD));
        grid.setItem(1, new ItemStack(ModRegistry.AZRAEL.get()));
        grid.setItem(2, new ItemStack(Items.ECHO_SHARD));
        ItemStack source = new ItemStack(Items.NETHERITE_PICKAXE);
        source.setDamageValue(81);
        source.enchant(Enchantments.BLOCK_FORTUNE, 3);
        source.getOrCreateTag().putString("custom_probe", "retained");
        grid.setItem(4, source);
        grid.setItem(7, new ItemStack(ModRegistry.IMMORTAL_BUD.get()));
        var shaped = (NbtInheritingShapedRecipe) recipe;
        check(shaped.matches(grid, level), "current T-shaped echo-shard recipe");
        var unchangedRecipe =
                new NbtInheritingShapedRecipe(
                        shaped.getId(),
                        shaped,
                        net.minecraft.world.item.crafting.Ingredient.of(Items.NETHERITE_PICKAXE),
                        false);
        check(
                unchangedRecipe
                        .assemble(grid, level.registryAccess())
                        .getTag()
                        .equals(source.getTag()),
                "reusable recipe without repair preserves all tags");
        ItemStack result = shaped.assemble(grid, level.registryAccess());
        check(
                result.is(tool.getItem())
                        && result.getCount() == 1
                        && repairedTag(source).equals(result.getTag())
                        && result.getDamageValue() == 0
                        && source.getDamageValue() == 81,
                "inherited tags except repaired damage; input unchanged");
        result.getTag().putString("custom_probe", "changed");
        check(
                source.getTag().getString("custom_probe").equals("retained"),
                "deep independent tag copy");
        var buffer = new net.minecraft.network.FriendlyByteBuf(io.netty.buffer.Unpooled.buffer());
        try {
            var serializer = new NbtInheritingShapedRecipe.Serializer();
            serializer.toNetwork(buffer, shaped);
            var decoded = serializer.fromNetwork(shaped.getId(), buffer);
            check(
                    decoded.matches(grid, level)
                            && decoded.assemble(grid, level.registryAccess())
                                    .getTag()
                                    .equals(repairedTag(source)),
                    "recipe network roundtrip");
        } finally {
            buffer.release();
        }

        for (int fortune : new int[] {0, 1, 3, 5}) {
            for (double luck : new double[] {-2, 0, 3.5}) {
                for (AspectState mode :
                        new AspectState[] {AspectState.MALICE, AspectState.WITNESS}) {
                    clear(level);
                    tool.removeTagKey("Enchantments");
                    if (fortune > 0) tool.enchant(Enchantments.BLOCK_FORTUNE, fortune);
                    player.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.LUCK)
                            .setBaseValue(luck);
                    var before = tool.getTag().copy();
                    level.setBlockAndUpdate(
                            pos, TremorFaultBlocks.LOOT_PROBE.get().defaultBlockState());
                    check(
                            PlayerBlockBreaker.breakBlock(player, pos, mode),
                            "loot input probe removal");
                    int expectedFortune = mode == AspectState.MALICE ? fortune * 2 : fortune;
                    check(
                            TremorFaultBlocks.observedFortune == expectedFortune,
                            "effective fortune " + mode);
                    check(
                            TremorFaultBlocks.observedLuck
                                    == (mode == AspectState.MALICE ? luck * 2 : 0),
                            "effective loot luck " + mode);
                    check(
                            count(level, Items.DIAMOND) == expectedFortune + 1,
                            "original loot result emitted once");
                    check(
                            before.equals(tool.getTag()) && player.getLuck() == luck,
                            "original tool and player unchanged");
                }
            }
        }
        tool.removeTagKey("Enchantments");
        player.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.LUCK)
                .setBaseValue(0);
        clear(level);
        level.setBlockAndUpdate(pos, Blocks.DIAMOND_ORE.defaultBlockState());
        check(PlayerBlockBreaker.breakBlock(player, pos, AspectState.MALICE), "malice ore break");
        check(count(level, Items.DIAMOND) == 1, "unenchanted diamond normal drop");
        clear(level);
        tool.enchant(Enchantments.SILK_TOUCH, 1);
        tool.enchant(Enchantments.BLOCK_FORTUNE, 3);
        level.setBlockAndUpdate(pos, Blocks.DIAMOND_ORE.defaultBlockState());
        check(PlayerBlockBreaker.breakBlock(player, pos, AspectState.MALICE), "silk break");
        check(count(level, Items.DIAMOND_ORE) == 1, "silk results unchanged");
        tool.removeTagKey("Enchantments");
        clear(level);
        level.setBlockAndUpdate(pos, Blocks.BEDROCK.defaultBlockState());
        check(
                !PlayerBlockBreaker.breakBlock(player, pos, AspectState.MALICE),
                "malice rejects negative hardness");
        check(
                PlayerBlockBreaker.breakBlock(player, pos, AspectState.WITNESS),
                "default witness permits bedrock");
        check(drops(level).isEmpty(), "default bedrock produces no invented drop");
        level.setBlockAndUpdate(pos, Blocks.BARRIER.defaultBlockState());
        check(
                !PlayerBlockBreaker.breakBlock(player, pos, AspectState.WITNESS)
                        && level.getBlockState(pos).is(Blocks.BARRIER),
                "default barrier blacklist");
        var old = DeepValleyItemsConfig.TREMOR_BLACKLIST.get();
        DeepValleyItemsConfig.TREMOR_BLACKLIST.set(List.of());
        check(
                PlayerBlockBreaker.breakBlock(player, pos, AspectState.WITNESS),
                "witness breaks barrier after removal from blacklist");
        check(drops(level).isEmpty(), "barrier has no invented drop");
        DeepValleyItemsConfig.TREMOR_BLACKLIST.set(old);
        clear(level);
        level.setBlockAndUpdate(pos, Blocks.STONE.defaultBlockState());
        cancel = true;
        check(
                !PlayerBlockBreaker.breakBlock(player, pos, AspectState.WITNESS)
                        && level.getBlockState(pos).is(Blocks.STONE),
                "protection cancellation");
        check(!PlayerBlockBreaker.active() && drops(level).isEmpty(), "cancelled scope cleaned");
        cancel = false;
        clear(level);
        level.setBlockAndUpdate(pos, Blocks.CHEST.defaultBlockState());
        ((ChestBlockEntity) level.getBlockEntity(pos)).setItem(0, new ItemStack(Items.DIAMOND, 7));
        check(PlayerBlockBreaker.breakBlock(player, pos, AspectState.MALICE), "chest break");
        check(
                count(level, Items.CHEST) == 1 && count(level, Items.DIAMOND) == 7,
                "chest and contents once");
        clear(level);
        level.setBlockAndUpdate(pos, Blocks.SHULKER_BOX.defaultBlockState());
        ((ShulkerBoxBlockEntity) level.getBlockEntity(pos))
                .setItem(0, new ItemStack(Items.DIAMOND, 7));
        check(PlayerBlockBreaker.breakBlock(player, pos, AspectState.MALICE), "shulker break");
        check(count(level, Items.SHULKER_BOX) == 1, "one shulker box");
        int contents = 0;
        for (ItemStack stack : drops(level))
            if (stack.is(Items.SHULKER_BOX) && stack.hasTag()) {
                var items = stack.getTag().getCompound("BlockEntityTag").getList("Items", 10);
                for (int i = 0; i < items.size(); i++)
                    contents += ItemStack.of(items.getCompound(i)).getCount();
            }
        check(contents == 7, "embedded contents not duplicated");
        clear(level);
        level.setBlockAndUpdate(pos.below(), Blocks.STONE.defaultBlockState());
        level.setBlockAndUpdate(pos, Blocks.WHITE_BANNER.defaultBlockState());
        var banner = level.getBlockEntity(pos);
        var bannerTag = banner.saveWithoutMetadata();
        var patterns = new net.minecraft.nbt.ListTag();
        var pattern = new net.minecraft.nbt.CompoundTag();
        pattern.putString("Pattern", "bs");
        pattern.putInt("Color", 14);
        patterns.add(pattern);
        bannerTag.put("Patterns", patterns);
        banner.load(bannerTag);
        check(
                PlayerBlockBreaker.breakBlock(player, pos, AspectState.MALICE)
                        && count(level, Items.WHITE_BANNER) == 1,
                "non-storage vanilla block entity normal drop");
        check(
                drops(level).stream()
                        .filter(s -> s.is(Items.WHITE_BANNER))
                        .allMatch(
                                s ->
                                        s.hasTag()
                                                && s.getTag()
                                                                .getCompound("BlockEntityTag")
                                                                .getList("Patterns", 10)
                                                                .size()
                                                        == 1),
                "banner cosmetic data retained");
        clear(level);
        level.setBlockAndUpdate(pos, Blocks.END_PORTAL_FRAME.defaultBlockState());
        check(
                PlayerBlockBreaker.breakBlock(player, pos, AspectState.WITNESS),
                "witness end portal frame");
        clear(level);
        level.setBlockAndUpdate(pos, Blocks.COMMAND_BLOCK.defaultBlockState());
        check(
                PlayerBlockBreaker.breakBlock(player, pos, AspectState.WITNESS),
                "witness command block");
        clear(level);
        level.setBlockAndUpdate(pos, Blocks.DIAMOND_ORE.defaultBlockState());
        check(
                PlayerBlockBreaker.breakBlock(player, pos, AspectState.WITNESS)
                        && count(level, Items.DIAMOND) == 1,
                "witness ordinary loot once");
        var speed =
                new net.minecraftforge.event.entity.player.PlayerEvent.BreakSpeed(
                        player, Blocks.STONE.defaultBlockState(), 23F, pos);
        HighFrequencyTremorActions.speed(speed);
        check(speed.getNewSpeed() == 46F, "speed multiplies computed value");
        var inv =
                top.theillusivec4.curios.api.CuriosApi.getCuriosInventory(player)
                        .resolve()
                        .orElseThrow();
        var slots =
                new top.theillusivec4.curios.common.inventory.CurioStacksHandler(
                        inv,
                        "travel_diary",
                        1,
                        true,
                        false,
                        true,
                        top.theillusivec4.curios.api.type.capability.ICurio.DropRule.DEFAULT);
        inv.setCurios(Map.of("travel_diary", slots));
        slots.getStacks().setStackInSlot(0, new ItemStack(ModRegistry.LONG_TRAVAIL.get()));
        var diary = slots.getStacks().getStackInSlot(0);
        com.thelongtravail.data.LongTravailData.setWitness(
                diary, com.thelongtravail.TravailAspect.UNDERWORLD, true);
        int savedBonus = com.thelongtravail.config.TravailConfig.UNDERWORLD_FORTUNE_BONUS.get();
        try {
            for (int bonus : new int[] {6, 300, 511, 512, 513, 10000}) {
                com.thelongtravail.config.TravailConfig.UNDERWORLD_FORTUNE_BONUS.set(bonus);
                for (int base : new int[] {0, 3}) {
                    tool.removeTagKey("Enchantments");
                    if (base > 0) tool.enchant(Enchantments.BLOCK_FORTUNE, base);
                    for (int kind = 0; kind < 3; kind++) {
                        clear(level);
                        level.setBlockAndUpdate(
                                pos, TremorFaultBlocks.LOOT_PROBE.get().defaultBlockState());
                        var originalTag = tool.getTag().copy();
                        boolean broken =
                                kind == 2
                                        ? player.gameMode.destroyBlock(pos)
                                        : PlayerBlockBreaker.breakBlock(
                                                player,
                                                pos,
                                                kind == 0
                                                        ? AspectState.MALICE
                                                        : AspectState.WITNESS);
                        int expected =
                                kind == 0 ? Math.min(1024, (base + bonus) * 2) : base + bonus;
                        check(
                                TremorFaultBlocks.extremeFortune
                                        == (kind == 0 ? 1024 : Integer.MAX_VALUE),
                                "extreme fortune bounded only in malice");
                        check(
                                TremorFaultBlocks.negativeFortune == (kind == 0 ? 0 : -1),
                                "invalid negative fortune normalized only in malice");
                        check(
                                broken && TremorFaultBlocks.observedFortune == expected,
                                "combined Curios fortune base="
                                        + base
                                        + " bonus="
                                        + bonus
                                        + " kind="
                                        + kind);
                        check(
                                count(level, Items.DIAMOND) == expected + 1,
                                "combined fortune drives actual loot formula");
                        check(
                                originalTag.equals(tool.getTag()),
                                "combined fortune does not rewrite enchantments");
                    }
                }
            }
        } finally {
            com.thelongtravail.config.TravailConfig.UNDERWORLD_FORTUNE_BONUS.set(savedBonus);
            com.thelongtravail.data.LongTravailData.setWitness(
                    diary, com.thelongtravail.TravailAspect.UNDERWORLD, false);
            tool.removeTagKey("Enchantments");
        }
        checks += TremorConfigChecks.run();
        guardChecks(level, player, diary, null);
        failureChecks(level, player, null);
        feedbackChecks(level, player, diary);
        player.setShiftKeyDown(false);
        check(
                !HighFrequencyTremorActions.eligible(player, InteractionHand.MAIN_HAND),
                "ordinary click not special");
        player.setShiftKeyDown(true);
        check(
                HighFrequencyTremorActions.eligible(player, InteractionHand.MAIN_HAND),
                "sneak mainhand eligible");
        check(
                !HighFrequencyTremorActions.eligible(player, InteractionHand.OFF_HAND),
                "no duplicate offhand trigger");
        clear(level);
        level.setBlockAndUpdate(pos, Blocks.STONE.defaultBlockState());
        var event =
                new net.minecraftforge.event.entity.player.PlayerInteractEvent.RightClickBlock(
                        player,
                        InteractionHand.MAIN_HAND,
                        pos,
                        new net.minecraft.world.phys.BlockHitResult(
                                net.minecraft.world.phys.Vec3.atCenterOf(pos),
                                net.minecraft.core.Direction.UP,
                                pos,
                                false));
        event.setCanceled(true);
        HighFrequencyTremorActions.use(event);
        check(level.getBlockState(pos).is(Blocks.STONE), "right-click cancellation respected");
        if (net.minecraftforge.fml.ModList.get().isLoaded("ftbultimine")) ftb(level, player, diary);
    }

    private void failureChecks(ServerLevel level, ServerPlayer player, Object batchData) {
        player.setShiftKeyDown(true);
        for (boolean serious : new boolean[] {false, true}) {
            player.tickCount += 40;
            var messages = ((TestPlayer) player).feedback;
            int before = messages.size();
            clear(level);
            for (BlockPos target : List.of(pos.west(), pos, pos.east()))
                level.setBlockAndUpdate(target, Blocks.STONE.defaultBlockState());
            var fatal = new AssertionError("TREMOR_INJECTED_SERIOUS_ERROR");
            mutation =
                    event -> {
                        if (serious) throw fatal;
                        throw new IllegalStateException("TREMOR_INJECTED_BOUNDARY_FAILURE");
                    };
            try {
                if (batchData == null) {
                    ForgeHooks.onRightClickBlock(
                            player,
                            InteractionHand.MAIN_HAND,
                            pos,
                            new net.minecraft.world.phys.BlockHitResult(
                                    net.minecraft.world.phys.Vec3.atCenterOf(pos),
                                    net.minecraft.core.Direction.UP,
                                    pos,
                                    false));
                } else {
                    int completed =
                            com.thelongtravail.compat.ftbultimine.TremorUltimine.batch(
                                    player, batchData);
                    check(completed == 1, "FTB preserves success count before later failure");
                }
                check(!serious, "serious Error must propagate");
            } catch (AssertionError failure) {
                if (failure != fatal) throw failure;
                check(serious, "same Error propagated");
            } finally {
                check(mutation == null, "fault callback actually ran");
                mutation = null;
            }
            check(
                    messages.size() == before + (serious ? 0 : 1),
                    "ordinary failure notifies once; Error does not notify");
            if (!serious)
                check(
                        messages.get(messages.size() - 1).endsWith(".failure"),
                        "failure translation key");
            check(level.getBlockState(pos).is(Blocks.STONE), "faulted target not retried");
            check(level.getBlockState(pos.east()).is(Blocks.STONE), "later target untouched");
            check(
                    count(level, Items.COBBLESTONE) == (batchData == null ? 0 : 1),
                    "no duplicate or invented drops");
            check(!PlayerBlockBreaker.active(), "failure scope cleaned");
            level.removeBlock(pos.west(), false);
            level.removeBlock(pos.east(), false);
            clear(level);
        }
    }

    private void feedbackChecks(ServerLevel level, ServerPlayer player, ItemStack diary) {
        var messages = ((TestPlayer) player).feedback;
        var hit =
                new net.minecraft.world.phys.BlockHitResult(
                        net.minecraft.world.phys.Vec3.atCenterOf(pos),
                        net.minecraft.core.Direction.UP,
                        pos,
                        false);
        com.thelongtravail.data.LongTravailData.setWitness(
                diary, com.thelongtravail.TravailAspect.DEEP_VALLEY, true);
        player.tickCount += 40;
        level.setBlockAndUpdate(pos, Blocks.BARRIER.defaultBlockState());
        int before = messages.size();
        ForgeHooks.onRightClickBlock(player, InteractionHand.MAIN_HAND, pos, hit);
        check(
                messages.size() == before + 1 && messages.get(before).endsWith(".blacklisted"),
                "direct blacklisted target notifies");
        ForgeHooks.onRightClickBlock(player, InteractionHand.MAIN_HAND, pos, hit);
        check(messages.size() == before + 1, "same tick duplicate suppressed");
        player.tickCount += 39;
        ForgeHooks.onRightClickBlock(player, InteractionHand.MAIN_HAND, pos, hit);
        check(messages.size() == before + 1, "39 tick interval suppressed");
        player.tickCount++;
        ForgeHooks.onRightClickBlock(player, InteractionHand.MAIN_HAND, pos, hit);
        check(messages.size() == before + 2, "40 tick interval permits next notice");
        before = messages.size();
        player.tickCount += 40;
        cancel = true;
        ForgeHooks.onRightClickBlock(player, InteractionHand.MAIN_HAND, pos, hit);
        cancel = false;
        // BreakEvent 取消晚于交互黑名单提示，此处直接检查最终交互是否取消。
        before = messages.size();
        var denied =
                new net.minecraftforge.event.entity.player.PlayerInteractEvent.RightClickBlock(
                        player, InteractionHand.MAIN_HAND, pos, hit);
        denied.setCanceled(true);
        HighFrequencyTremorActions.use(denied);
        check(messages.size() == before, "cancelled interaction remains silent");
        check(
                !PlayerBlockBreaker.breakBlock(player, pos, AspectState.WITNESS)
                        && messages.size() == before,
                "internal blacklist skip is silent");
        level.setBlockAndUpdate(pos, Blocks.STONE.defaultBlockState());
        mutation =
                event -> {
                    throw new IllegalStateException("TREMOR_FEEDBACK_INDEPENDENT_LIMIT");
                };
        ForgeHooks.onRightClickBlock(player, InteractionHand.MAIN_HAND, pos, hit);
        check(
                messages.size() == before + 1 && messages.get(before).endsWith(".failure"),
                "blacklist notice does not suppress exception notice");
        mutation =
                event -> {
                    throw new IllegalStateException("TREMOR_FEEDBACK_REPEAT");
                };
        ForgeHooks.onRightClickBlock(player, InteractionHand.MAIN_HAND, pos, hit);
        check(messages.size() == before + 1, "repeated exception feedback throttled");
        com.thelongtravail.data.LongTravailData.setWitness(
                diary, com.thelongtravail.TravailAspect.DEEP_VALLEY, false);
        clear(level);
    }

    private void guardChecks(
            ServerLevel level, ServerPlayer player, ItemStack diary, Object batchData) {
        var held = player.getMainHandItem();
        var slots =
                top.theillusivec4.curios.api.CuriosApi.getCuriosInventory(player)
                        .resolve()
                        .orElseThrow()
                        .getCurios()
                        .get("travel_diary")
                        .getStacks();
        var other = level.getServer().getLevel(net.minecraft.world.level.Level.NETHER);
        check(other != null, "second dimension available for mutation probe");
        for (boolean witness : new boolean[] {false, true}) {
            var mode = witness ? AspectState.WITNESS : AspectState.MALICE;
            for (int variant = 0; variant < 5; variant++) {
                final int kind = variant;
                level.removeBlock(pos, false);
                clear(level);
                player.setItemInHand(InteractionHand.MAIN_HAND, held);
                slots.setStackInSlot(0, diary);
                com.thelongtravail.data.LongTravailData.setWitness(
                        diary, com.thelongtravail.TravailAspect.DEEP_VALLEY, witness);
                level.setBlockAndUpdate(pos, Blocks.CHEST.defaultBlockState());
                var original = (ChestBlockEntity) level.getBlockEntity(pos);
                original.setItem(0, new ItemStack(Items.DIAMOND, 7));
                level.setBlockAndUpdate(pos.east(), Blocks.STONE.defaultBlockState());
                other.setBlockAndUpdate(pos.east(), Blocks.STONE.defaultBlockState());
                mutation =
                        event -> {
                            switch (kind) {
                                case 0 ->
                                        player.setItemInHand(
                                                InteractionHand.MAIN_HAND, held.copy());
                                case 1 -> slots.setStackInSlot(0, ItemStack.EMPTY);
                                case 2 ->
                                        com.thelongtravail.data.LongTravailData.setWitness(
                                                diary,
                                                com.thelongtravail.TravailAspect.DEEP_VALLEY,
                                                !witness);
                                case 3 -> {
                                    var replacement =
                                            new ChestBlockEntity(pos, level.getBlockState(pos));
                                    replacement.setItem(0, new ItemStack(Items.DIAMOND, 13));
                                    level.setBlockEntity(replacement);
                                }
                                case 4 -> player.setServerLevel(other);
                            }
                        };
                try {
                    int success =
                            batchData == null
                                    ? (PlayerBlockBreaker.breakBlock(player, pos, mode) ? 1 : 0)
                                    : com.thelongtravail.compat.ftbultimine.TremorUltimine.batch(
                                            player, batchData);
                    check(mutation == null, "mutation callback executed");
                    check(
                            success == (batchData != null && kind == 3 ? 1 : 0),
                            "only unchanged batch targets may complete");
                    check(
                            level.getBlockState(pos).is(Blocks.CHEST),
                            "original position preserved after mutation " + kind);
                    var remaining = (ChestBlockEntity) level.getBlockEntity(pos);
                    check(
                            remaining.getItem(0).getCount() == (kind == 3 ? 13 : 7),
                            "replacement container inventory preserved");
                    check(count(level, Items.DIAMOND) == 0, "no stale inventory drops");
                    check(
                            other.getBlockState(pos.east()).is(Blocks.STONE),
                            "new dimension untouched");
                    if (kind != 3)
                        check(
                                level.getBlockState(pos.east()).is(Blocks.STONE),
                                "remaining chain stopped on actor change");
                    check(!PlayerBlockBreaker.active(), "mutation scope cleaned");
                } finally {
                    mutation = null;
                    player.setServerLevel(level);
                    player.setItemInHand(InteractionHand.MAIN_HAND, held);
                    slots.setStackInSlot(0, diary);
                    com.thelongtravail.data.LongTravailData.setWitness(
                            diary, com.thelongtravail.TravailAspect.DEEP_VALLEY, false);
                    level.removeBlock(pos, false);
                    clear(level);
                    level.removeBlock(pos.east(), false);
                    other.removeBlock(pos.east(), false);
                }
            }
        }
        for (int variant = 0; variant < 2; variant++) {
            int kind = variant;
            clear(level);
            level.setBlockAndUpdate(
                    pos, TremorFaultBlocks.ACTOR_MUTATION.get().defaultBlockState());
            level.setBlockAndUpdate(pos.east(), Blocks.STONE.defaultBlockState());
            other.setBlockAndUpdate(pos.east(), Blocks.STONE.defaultBlockState());
            var replacementTool = new ItemStack(Items.NETHERITE_PICKAXE);
            TremorFaultBlocks.afterRemoval =
                    () -> {
                        if (kind == 0)
                            player.setItemInHand(InteractionHand.MAIN_HAND, replacementTool);
                        else player.setServerLevel(other);
                    };
            try {
                int success =
                        batchData == null
                                ? (PlayerBlockBreaker.breakBlock(player, pos, AspectState.MALICE)
                                        ? 1
                                        : 0)
                                : com.thelongtravail.compat.ftbultimine.TremorUltimine.batch(
                                        player, batchData);
                check(
                        success == 1 && level.getBlockState(pos).isAir(),
                        "completed removal counted once");
                check(
                        count(level, Items.COBBLESTONE) == 1,
                        "completed block settles in original world");
                check(
                        replacementTool.getDamageValue() == 0,
                        "replacement tool not used for old block");
                check(
                        level.getBlockState(pos.east()).is(Blocks.STONE)
                                && other.getBlockState(pos.east()).is(Blocks.STONE),
                        "post-removal change stops remaining chain in both worlds");
            } finally {
                TremorFaultBlocks.afterRemoval = null;
                player.setServerLevel(level);
                player.setItemInHand(InteractionHand.MAIN_HAND, held);
                clear(level);
                level.removeBlock(pos.east(), false);
                other.removeBlock(pos.east(), false);
            }
        }
        level.setBlockAndUpdate(pos, Blocks.STONE.defaultBlockState());
        check(
                PlayerBlockBreaker.breakBlock(player, pos, AspectState.MALICE),
                "next normal action recovers after mutations");
        clear(level);
    }

    private void ftb(ServerLevel level, ServerPlayer player, ItemStack diary) throws Exception {
        Class<?> dataClass = Class.forName("dev.ftb.mods.ftbultimine.FTBUltiminePlayerData");
        Object data = dataClass.getConstructor(UUID.class).newInstance(player.getUUID());
        var cached = dataClass.getDeclaredField("cachedBlocks");
        cached.setAccessible(true);
        cached.set(data, new ArrayList<>(List.of(pos, pos.east(), pos)));
        clear(level);
        level.setBlockAndUpdate(pos, Blocks.DIAMOND_ORE.defaultBlockState());
        level.setBlockAndUpdate(pos.east(), Blocks.DIAMOND_ORE.defaultBlockState());
        Class<?> platform =
                Class.forName("dev.ftb.mods.ftbultimine.utils.forge.PlatformMethodsImpl");
        var method =
                Arrays.stream(platform.getMethods())
                        .filter(m -> m.getName().equals("blockRightClick"))
                        .findFirst()
                        .orElseThrow();
        var field =
                com.thelongtravail.compat.ftbultimine.TremorUltimine.class.getDeclaredField(
                        "DISPATCH");
        field.setAccessible(true);
        @SuppressWarnings("unchecked")
        var dispatch = (ThreadLocal<Boolean>) field.get(null);
        dispatch.set(true);
        try {
            int n =
                    (Integer)
                            method.invoke(
                                    null,
                                    null,
                                    player,
                                    InteractionHand.MAIN_HAND,
                                    pos,
                                    net.minecraft.core.Direction.UP,
                                    data);
            check(
                    n == 2 && count(level, Items.DIAMOND) == 2,
                    "FTB real platform adapter, deduplication and normal drops");
        } finally {
            dispatch.remove();
        }
        guardChecks(level, player, diary, data);
        cached.set(data, new ArrayList<>(List.of(pos.west(), pos, pos.east())));
        failureChecks(level, player, data);
        com.thelongtravail.data.LongTravailData.setWitness(
                diary, com.thelongtravail.TravailAspect.DEEP_VALLEY, true);
        cached.set(data, new ArrayList<>(List.of(pos, pos.east())));
        level.setBlockAndUpdate(pos, Blocks.STONE.defaultBlockState());
        level.setBlockAndUpdate(pos.east(), Blocks.BARRIER.defaultBlockState());
        int notices = ((TestPlayer) player).feedback.size();
        check(
                com.thelongtravail.compat.ftbultimine.TremorUltimine.batch(player, data) == 1,
                "FTB skips blacklisted neighbor");
        check(
                ((TestPlayer) player).feedback.size() == notices,
                "FTB internal blacklist does not notify");
        level.removeBlock(pos.east(), false);
        com.thelongtravail.data.LongTravailData.setWitness(
                diary, com.thelongtravail.TravailAspect.DEEP_VALLEY, false);

        Class<?> ftbClass = Class.forName("dev.ftb.mods.ftbultimine.FTBUltimine");
        Object ftb = ftbClass.getField("instance").get(null);
        var rightClick =
                ftbClass.getMethod(
                        "blockRightClick",
                        net.minecraft.world.entity.player.Player.class,
                        InteractionHand.class,
                        BlockPos.class,
                        net.minecraft.core.Direction.class);
        clear(level);
        level.setBlockAndUpdate(pos, Blocks.STONE.defaultBlockState());
        rightClick.invoke(
                ftb, player, InteractionHand.MAIN_HAND, pos, net.minecraft.core.Direction.UP);
        check(level.getBlockState(pos).is(Blocks.STONE), "FTB early right-click deferred");
        Object liveData =
                ftbClass.getMethod(
                                "getOrCreatePlayerData",
                                net.minecraft.world.entity.player.Player.class)
                        .invoke(ftb, player);
        dataClass.getMethod("setPressed", boolean.class).invoke(liveData, true);
        com.thelongtravail.data.LongTravailData.setWitness(
                diary, com.thelongtravail.TravailAspect.UNDERWORLD, true);
        player.getMainHandItem().enchant(Enchantments.BLOCK_FORTUNE, 3);
        player.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.LUCK)
                .setBaseValue(2);
        clear(level);
        level.setBlockAndUpdate(pos, TremorFaultBlocks.LOOT_PROBE.get().defaultBlockState());
        level.setBlockAndUpdate(pos.east(), TremorFaultBlocks.LOOT_PROBE.get().defaultBlockState());
        player.setPos(0.5, 101, 0.5);
        player.setXRot(90);
        player.setYRot(0);
        var hit =
                new net.minecraft.world.phys.BlockHitResult(
                        net.minecraft.world.phys.Vec3.atCenterOf(pos),
                        net.minecraft.core.Direction.UP,
                        pos,
                        false);
        var interaction =
                net.minecraftforge.common.ForgeHooks.onRightClickBlock(
                        player, InteractionHand.MAIN_HAND, pos, hit);
        check(
                interaction.isCanceled()
                        && level.getBlockState(pos).isAir()
                        && level.getBlockState(pos.east()).isAir(),
                "real Forge interaction to FTB selected chain");
        check(
                count(level, Items.DIAMOND) == 38,
                "full FTB chain combined fortune eighteen, no duplicate drops");
        check(
                TremorFaultBlocks.observedFortune == 18 && TremorFaultBlocks.observedLuck == 4,
                "FTB boosted inputs");
        check(
                net.minecraft.world.item.enchantment.EnchantmentHelper.getItemEnchantmentLevel(
                                        Enchantments.BLOCK_FORTUNE, player.getMainHandItem())
                                == 3
                        && player.getLuck() == 2,
                "FTB preserves original attributes");
        clear(level);
        level.setBlockAndUpdate(pos, TremorFaultBlocks.LOOT_PROBE.get().defaultBlockState());
        level.setBlockAndUpdate(pos.east(), TremorFaultBlocks.LOOT_PROBE.get().defaultBlockState());
        var fatal = new AssertionError("TREMOR_REFLECTED_ERROR");
        mutation =
                event -> {
                    throw fatal;
                };
        try {
            ForgeHooks.onRightClickBlock(player, InteractionHand.MAIN_HAND, pos, hit);
            throw new AssertionError("reflected Error swallowed");
        } catch (AssertionError failure) {
            if (failure != fatal) throw failure;
            check(true, "FTB reflection propagates original Error");
        } finally {
            check(mutation == null, "reflected error callback executed");
            mutation = null;
        }
        check(
                !com.thelongtravail.compat.ftbultimine.TremorUltimine.dispatching()
                        && !PlayerBlockBreaker.active(),
                "FTB Error clears both scopes");
        clear(level);
        level.removeBlock(pos.east(), false);
    }
}
