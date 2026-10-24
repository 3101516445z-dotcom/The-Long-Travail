package travail.smoke;

import com.mojang.authlib.GameProfile;
import com.thelongtravail.*;
import com.thelongtravail.api.*;
import com.thelongtravail.data.*;
import com.thelongtravail.registry.ModRegistry;
import com.thelongtravail.underworld.UnderworldLedger;
import net.minecraft.nbt.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.*;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.*;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.type.capability.ICurio;
import top.theillusivec4.curios.common.inventory.CurioStacksHandler;
import java.util.*;

@Mod("travail_smoke")
public final class JourneyApiSmoke {
    private static int assertions;

    private static void check(boolean value, String message) {
        assertions++;
        if (!value) throw new AssertionError(message);
    }

    private static final class P extends ServerPlayer {
        @Override
        public boolean hasDisconnected() {
            return false;
        }

        P(ServerLevel level) {
            super(level.getServer(), level, new GameProfile(UUID.randomUUID(), "JourneyApiTest"));
            var wire =
                    new net.minecraft.network.Connection(net.minecraft.network.protocol.PacketFlow.SERVERBOUND) {
                        @Override
                        public void send(net.minecraft.network.protocol.Packet<?> packet) {}
                    };
            connection =
                    new net.minecraft.server.network.ServerGamePacketListenerImpl(level.getServer(), wire, this) {
                        @Override
                        public void send(net.minecraft.network.protocol.Packet<?> packet) {}
                    };
        }
    }

    private static ListTag list(String... ids) {
        ListTag result = new ListTag();
        for (String id : ids) result.add(StringTag.valueOf(id));
        return result;
    }

    private static ItemStack equipped(P p) {
        return com.thelongtravail.helper.TravailCurios.stack(p);
    }

    private static ItemStack equip(P p, boolean initialize) {
        var inv = CuriosApi.getCuriosInventory(p).resolve().orElseThrow();
        inv.setCurios(
                new HashMap<>(
                        Map.of(
                                "travel_diary",
                                new CurioStacksHandler(
                                        inv, "travel_diary", 1, true, false, true, ICurio.DropRule.DEFAULT),
                                "curio",
                                new CurioStacksHandler(inv, "curio", 1, true, false, true, ICurio.DropRule.DEFAULT))));
        ItemStack diary = new ItemStack(ModRegistry.LONG_TRAVAIL.get());
        if (initialize) check(LongTravailData.tryInitialize(diary, p), "initialize");
        inv.getStacksHandler("travel_diary").orElseThrow().getStacks().setStackInSlot(0, diary);
        return diary;
    }

    private static void requirements(ItemStack diary) {
        var root = JourneyState.root(diary);
        root.putInt("Witnesses", 0);
        root.remove("ForcedWitnesses");
        root.remove("ForcedMalices");
        for (var a : TravailAspect.values()) {
            var theme = root.getCompound("Requirements").getCompound(a.id());
            theme.put("Biomes", list("minecraft:plains", "minecraft:forest"));
            theme.put("Structures", list("minecraft:village_plains"));
            theme.putString("BiomesStatus", "READY");
            theme.putString("StructuresStatus", "READY");
            theme.remove("CompletedBiomes");
            theme.remove("CompletedStructures");
        }
        LongTravailData.requirementsChanged(diary);
    }

    private static int remaining(ItemStack diary, TravailAspect a) {
        var theme = JourneyState.root(diary).getCompound("Requirements").getCompound(a.id());
        return theme.getList("Biomes", Tag.TAG_STRING).size() + theme.getList("Structures", Tag.TAG_STRING).size();
    }

    private static void test(ServerLevel level) throws Exception {
        var p = new P(level);
        var diary = equip(p, true);
        requirements(diary);
        var untouched = JourneyState.root(diary).copy();
        for (var a : TravailAspect.values()) {
            Item item = ModRegistry.ESSENCES.get(a.ordinal()).get();
            p.getCooldowns().removeCooldown(item);
            p.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(item, 2));
            check(item.use(level, p, InteractionHand.MAIN_HAND).getResult().consumesAction(), "six items work " + a);
            check(p.getMainHandItem().getCount() == 1, "consume one " + a);
            check(remaining(equipped(p), a) == 2, "complete exactly one " + a);
            for (var other : TravailAspect.values())
                if (other.ordinal() > a.ordinal())
                    check(
                            JourneyState.root(equipped(p))
                                    .getCompound("Requirements")
                                    .getCompound(other.id())
                                    .equals(untouched.getCompound("Requirements").getCompound(other.id())),
                            "same targets do not complete another theme");
        }
        var a = TravailAspect.FLOURISHING;
        check(TravailJourneyApi.completeRandomRequirement(p, a).changed(), "second record");
        check(TravailJourneyApi.completeRandomRequirement(p, a).changed(), "last record");
        check(LongTravailData.hasWitness(equipped(p), a), "last record reverses naturally");
        check(
                TravailJourneyApi.completeRandomRequirement(p, a).status() == JourneyOperations.Status.NO_RECORDS,
                "no records");
        JourneyState.root(equipped(p)).putLong("AttackCleanseCooldown", 98765);
        var old = JourneyState.identity(equipped(p), a);
        var other = JourneyState.root(equipped(p)).getCompound("Requirements").getCompound("abyss").copy();
        Item item = ModRegistry.ESSENCES.get(a.ordinal()).get();
        p.getCooldowns().removeCooldown(item);
        p.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(item));
        check(item.use(level, p, InteractionHand.OFF_HAND).getResult().consumesAction(), "witness essence resets");
        check(
                p.getOffhandItem().isEmpty() && !LongTravailData.hasWitness(equipped(p), a),
                "offhand consumed, malice restored");
        check(
                !old.equals(JourneyState.identity(equipped(p), a))
                        && old.journeyId().equals(JourneyState.identity(equipped(p), a).journeyId()),
                "only theme epoch changes");
        check(
                other.equals(JourneyState.root(equipped(p)).getCompound("Requirements").getCompound("abyss")),
                "other theme retained");
        check(LongTravailData.getLong(equipped(p), "AttackCleanseCooldown") == 98765, "cooldown kept");

        var key = JourneyState.identity(equipped(p), TravailAspect.UNDERWORLD);
        var ledger = UnderworldLedger.load(new CompoundTag());
        ledger.witness(key, JourneyState.BOOK_SOURCE);
        check(ledger.hasChanges(), "new-only ledger active");
        ledger.reconcile(equipped(p));
        check(
                LongTravailData.hasWitness(equipped(p), TravailAspect.UNDERWORLD)
                        && remaining(equipped(p), TravailAspect.UNDERWORLD) == 2,
                "book grants without completing exploration");
        long revision = LongTravailData.queryIdentity(equipped(p)).revision();
        var tag = equipped(p).getTag().copy();
        ledger.reconcile(equipped(p));
        check(tag.equals(equipped(p).getTag()), "reconcile idempotent");
        check(TravailJourneyApi.resetAspect(p, TravailAspect.UNDERWORLD).changed(), "underworld reset");
        check(
                TravailJourneyApi.grantWitness(p, key, JourneyState.BOOK_SOURCE).status()
                        == JourneyOperations.Status.STALE,
                "old death key rejected");
        ledger = UnderworldLedger.load(ledger.save(new CompoundTag()));
        ledger.reconcile(equipped(p));
        check(
                !LongTravailData.hasWitness(equipped(p), TravailAspect.UNDERWORLD),
                "old grant cannot restore new epoch after reload");
        var nested = new CompoundTag();
        // 日记完整序列化副本必须仍绑定原轮次。
        var previousDiary = new ItemStack(ModRegistry.LONG_TRAVAIL.get());
        previousDiary.setTag(tag.copy());
        nested.put("SavedItem", previousDiary.save(new CompoundTag()));
        ledger.reconcileTag(nested, 0);
        check(
                JourneyState.root(ItemStack.of(nested.getCompound("SavedItem"))).getBoolean("UnderworldBookWitness"),
                "nested old snapshot preserved");

        diary = equip(p, true);
        requirements(diary);
        var root = JourneyState.root(diary);
        root.putInt("Version", 1);
        for (var aspect : TravailAspect.values()) {
            var theme = root.getCompound("Requirements").getCompound(aspect.id());
            theme.remove("ThemeFormat");
            theme.remove("Epoch");
            theme.remove("ConditionVersion");
            theme.remove("BiomesStatus");
            theme.remove("StructuresStatus");
        }
        ledger = UnderworldLedger.load(new CompoundTag());
        ledger.witness(root.getUUID("JourneyId"));
        ledger.reconcile(diary);
        check(LongTravailData.hasWitness(diary, TravailAspect.UNDERWORLD), "legacy ledger matches legacy diary");
        var legacyOther = root.getCompound("Requirements").getCompound("abyss").copy();
        check(TravailJourneyApi.resetAspect(p, TravailAspect.UNDERWORLD).changed(), "v1 selective reset");
        ledger.reconcile(equipped(p));
        check(
                !LongTravailData.hasWitness(equipped(p), TravailAspect.UNDERWORLD),
                "legacy ledger blocked for new theme");
        check(
                JourneyState.root(equipped(p)).getInt("Version") == 1
                        && legacyOther.equals(
                                JourneyState.root(equipped(p)).getCompound("Requirements").getCompound("abyss")),
                "v1 other theme unchanged");
        var corrupt = equipped(p).copy();
        JourneyState.root(corrupt).getCompound("Requirements").getCompound("underworld").putString("Epoch", "bad");
        check(JourneyState.identity(corrupt, TravailAspect.UNDERWORLD) == null, "corrupt epoch has no legacy fallback");
        var before = corrupt.getTag().copy();
        ledger.reconcile(corrupt);
        check(before.equals(corrupt.getTag()), "corrupt diary untouched by ledger");
        var disabled = new RequirementPools.Pool(RequirementPools.Status.DISABLED, List.of(), 0);
        root = JourneyState.root(equipped(p));
        before = root.copy();
        check(
                JourneyOperations.resetAspect(
                                root,
                                TravailAspect.UNDERWORLD,
                                new RequirementPools.AspectPools(disabled, disabled),
                                p.getRandom())
                        == JourneyOperations.Status.NO_CONDITIONS,
                "empty reset rejected");
        check(before.equals(root), "failed reset atomic");
        var invalid = new RequirementPools.Pool(RequirementPools.Status.INVALID, List.of(), 0);
        check(
                JourneyOperations.resetAspect(
                                root,
                                TravailAspect.UNDERWORLD,
                                new RequirementPools.AspectPools(invalid, disabled),
                                p.getRandom())
                        == JourneyOperations.Status.INVALID_POOL,
                "invalid pool rejected");

        var cmd = p.server.getCommands().getDispatcher();
        var source = p.createCommandSourceStack().withPermission(2);
        requirements(equipped(p));
        check(cmd.execute("tlt reverse", source) == 1, "all command");
        check(cmd.execute("tlt reverse", source) == 1, "duplicate command succeeds");
        check(cmd.execute("tlt reverse -5", source) == 1, "force underworld malice");
        key = JourneyState.identity(equipped(p), TravailAspect.UNDERWORLD);
        revision = LongTravailData.queryIdentity(equipped(p)).revision();
        check(TravailJourneyApi.grantWitness(p, key, JourneyState.BOOK_SOURCE).changed(), "grant under forced malice");
        check(!LongTravailData.hasWitness(equipped(p), TravailAspect.UNDERWORLD), "force priority");
        check(
                LongTravailData.queryIdentity(equipped(p)).revision() == revision,
                "grant does not invalidate exploration");
        check(
                cmd.execute("tlt reverse clear 5", source) == 1
                        && LongTravailData.hasWitness(equipped(p), TravailAspect.UNDERWORLD),
                "clear restores special witness");
        check(cmd.execute("tlt reverse clear 0", source) == 1, "clear zero means all");
        check(cmd.execute("tlt reverse 0", source) == 0, "reverse zero fails");
        check(cmd.execute("tlt reverse clear", source) == 1, "duplicate clear succeeds");
        for (var aspect : TravailAspect.values()) {
            int number = aspect.ordinal() + 1;
            var unchangedKey = JourneyState.identity(equipped(p), aspect);
            check(
                    cmd.execute("tlt reverse " + number, source) == 1
                            && LongTravailData.hasWitness(equipped(p), aspect),
                    "positive command " + number);
            check(
                    cmd.execute("tlt reverse -" + number, source) == 1
                            && !LongTravailData.hasWitness(equipped(p), aspect),
                    "negative command " + number);
            check(cmd.execute("tlt reverse clear " + number, source) == 1, "single clear " + number);
            check(unchangedKey.equals(JourneyState.identity(equipped(p), aspect)), "commands retain epoch " + number);
        }
        diary = equip(p, false);
        check(cmd.execute("tlt reverse 1", source) == 1, "uninitialized command supported");
        check(!LongTravailData.isInitialized(diary), "command does not initialize");
        check(
                LongTravailData.tryInitialize(diary, p) && LongTravailData.hasWitness(diary, TravailAspect.FLOURISHING),
                "initialize retains override");
        var clean = equip(p, true);
        requirements(clean);
        var observationClass = Class.forName("com.thelongtravail.data.JourneyVisits$Observation");
        var constructor = observationClass.getDeclaredConstructor(ServerPlayer.class, ItemStack.class, Map.class);
        constructor.setAccessible(true);
        var pendingField = JourneyVisits.class.getDeclaredField("PENDING");
        pendingField.setAccessible(true);
        @SuppressWarnings("unchecked")
        var pending = (Deque<Object>) pendingField.get(null);
        var valid = observationClass.getDeclaredMethod("valid");
        valid.setAccessible(true);
        var observation = constructor.newInstance(p, clean, Map.of());
        pending.add(observation);
        check((boolean) valid.invoke(observation), "queued observation starts valid");
        var identityBefore = LongTravailData.queryIdentity(clean);
        check(
                LongTravailData.visitBiome(clean, new ResourceLocation("minecraft:plains")),
                "natural shared target completes");
        for (var theme : TravailAspect.values())
            check(remaining(clean, theme) == 2, "natural visit updates all matching themes");
        JourneyVisits.rebase(p, identityBefore, clean);
        check(
                pending.contains(observation) && (boolean) valid.invoke(observation),
                "natural progress retains pending observation");
        check(
                TravailJourneyApi.grantWitness(
                                p, JourneyState.identity(clean, TravailAspect.DEEP_VALLEY), JourneyState.BOOK_SOURCE)
                        .changed(),
                "source-only grant");
        check(
                pending.contains(observation) && (boolean) valid.invoke(observation),
                "state-only grant retains exploration");
        check(TravailJourneyApi.completeRandomRequirement(p, TravailAspect.ABYSS).changed(), "external progress");
        check(
                !pending.contains(observation) && !(boolean) valid.invoke(observation),
                "external progress invalidates old observation");
        com.thelongtravail.data.StiffState.start(p, 100);
        check(TravailJourneyApi.resetAspect(p, TravailAspect.DEEP_VALLEY).changed(), "deep valley reset");
        check(com.thelongtravail.data.StiffState.remaining(p) > 0, "local reset preserves existing stiff");
        requirements(equipped(p));
        p.setPos(.5, 80, .5);
        p.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModRegistry.WAYGUIDE.get()));
        check(WayguideSearch.use(p, InteractionHand.MAIN_HAND), "wayguide starts before external progress");
        var taskField = WayguideSearch.class.getDeclaredField("TASKS");
        taskField.setAccessible(true);
        var tasks = (Map<?, ?>) taskField.get(null);
        check(tasks.containsKey(p.getUUID()), "wayguide owns active task");
        check(
                TravailJourneyApi.completeRandomRequirement(p, TravailAspect.ABYSS).changed(),
                "external progress while searching");
        check(!tasks.containsKey(p.getUUID()), "old wayguide task cancelled");
        // 无效数据使用不能消耗精粹或增加冷却。
        var broken = equipped(p);
        JourneyState.root(broken).getCompound("Requirements").getCompound("abyss").putString("Epoch", "invalid");
        item = ModRegistry.ESSENCES.get(TravailAspect.ABYSS.ordinal()).get();
        p.getCooldowns().removeCooldown(item);
        p.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(item));
        before = broken.getTag().copy();
        check(!item.use(level, p, InteractionHand.MAIN_HAND).getResult().consumesAction(), "invalid essence use fails");
        check(
                p.getMainHandItem().getCount() == 1
                        && !p.getCooldowns().isOnCooldown(item)
                        && before.equals(broken.getTag()),
                "failure keeps item, cooldown and NBT");
        var future = equipped(p).copy();
        JourneyState.root(future).putInt("Version", 99);
        check(LongTravailData.refreshedCopy(future, p).isEmpty(), "future schema cannot be reset");
        var missingStatus = equip(p, true).copy();
        JourneyState.root(missingStatus).getCompound("Requirements").getCompound("abyss").remove("BiomesStatus");
        check(
                JourneyState.identity(missingStatus, TravailAspect.ABYSS) == null,
                "new condition metadata must be complete");
        var unknownRecord = new CompoundTag();
        unknownRecord.putInt("RecordFormat", 99);
        var savedLedger = new CompoundTag();
        var unknownList = new ListTag();
        unknownList.add(unknownRecord);
        savedLedger.put("ThemeWitnesses", unknownList);
        check(
                UnderworldLedger.load(savedLedger)
                        .save(new CompoundTag())
                        .getList("ThemeWitnesses", Tag.TAG_COMPOUND)
                        .equals(unknownList),
                "unsupported ledger rows preserved");
        var fresh = equip(p, true);
        requirements(fresh);
        var duplicate = TravailJourneyApi.clearAllOverrides(p);
        check(
                duplicate.accepted() && !duplicate.changed() && duplicate.beforeMask() == duplicate.afterMask(),
                "no-change result retains actual state");
        check(
                TravailJourneyApi.setOverride(p, TravailAspect.ABYSS, JourneyOperations.Override.MALICE).accepted(),
                "forced malice before natural completion");
        for (int i = 0; i < 3; i++)
            check(
                    TravailJourneyApi.completeRandomRequirement(p, TravailAspect.ABYSS).changed(),
                    "complete under override");
        check(!LongTravailData.hasWitness(equipped(p), TravailAspect.ABYSS), "last record does not clear override");
        check(
                TravailJourneyApi.clearOverride(p, TravailAspect.ABYSS).changed()
                        && LongTravailData.hasWitness(equipped(p), TravailAspect.ABYSS),
                "clear restores completed journey");
        // 损坏条件不能通过定向完成绕过随机完成的校验。
        fresh = equip(p, true);
        requirements(fresh);
        root = JourneyState.root(fresh);
        var damagedTheme = root.getCompound("Requirements").getCompound("abyss");
        damagedTheme.put("CompletedBiomes", list("minecraft:plains"));
        before = root.copy();
        check(
                TravailJourneyApi.completeRequirement(
                                        p,
                                        TravailAspect.ABYSS,
                                        new JourneyOperations.RequirementRef(
                                                JourneyOperations.Kind.BIOME, new ResourceLocation("minecraft:plains")))
                                .status()
                        == JourneyOperations.Status.INVALID_DATA,
                "overlap rejected by explicit completion");
        check(
                TravailJourneyApi.completeRandomRequirement(p, TravailAspect.ABYSS).status()
                                == JourneyOperations.Status.INVALID_DATA
                        && before.equals(root),
                "overlap failure leaves data unchanged");
        damagedTheme.remove("CompletedBiomes");
        damagedTheme.put("Biomes", list("minecraft:plains", "minecraft:plains"));
        before = root.copy();
        check(
                TravailJourneyApi.completeRandomRequirement(p, TravailAspect.ABYSS).status()
                                == JourneyOperations.Status.INVALID_DATA
                        && before.equals(root),
                "duplicate pending rows rejected");
        damagedTheme.put("Biomes", list("plains"));
        before = root.copy();
        check(
                TravailJourneyApi.completeRandomRequirement(p, TravailAspect.ABYSS).status()
                                == JourneyOperations.Status.INVALID_DATA
                        && before.equals(root),
                "noncanonical stored ID rejected instead of unfinishable random selection");
        requirements(fresh);
        before = root.copy();
        check(
                TravailJourneyApi.setOverride(p, TravailAspect.ABYSS, null).status()
                                == JourneyOperations.Status.INVALID_DATA
                        && before.equals(root),
                "null override is not an implicit clear");
        // 原槽位对象虽仍存在，但 Curios 已换成新容器时必须拒绝提交和消耗。
        item = ModRegistry.ESSENCES.get(0).get();
        p.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(item));
        var oldContainerDiary = fresh;
        var stale =
                JourneyService.mutate(
                        p,
                        Set.of(TravailAspect.ABYSS),
                        Set.of(),
                        false,
                        JourneyService.QueryPolicy.INVALIDATE,
                        InteractionHand.MAIN_HAND,
                        copy -> {
                            equip(p, false);
                            return JourneyOperations.completeRandom(
                                    JourneyState.root(copy), TravailAspect.ABYSS, p.getRandom());
                        });
        check(
                stale.status() == JourneyOperations.Status.STALE && p.getMainHandItem().getCount() == 1,
                "replaced Curios container rejects commit and consumption");
        check(
                before.equals(JourneyState.root(oldContainerDiary)) && !LongTravailData.isInitialized(equipped(p)),
                "stale replacement changes neither diary");
        unknownRecord = new CompoundTag();
        unknownRecord.putByte("RecordFormat", (byte) 1);
        unknownRecord.putUUID("JourneyId", UUID.randomUUID());
        unknownRecord.putUUID("Epoch", UUID.randomUUID());
        unknownRecord.putString("Aspect", "underworld");
        unknownRecord.putString("Source", JourneyState.BOOK_SOURCE.toString());
        unknownList = new ListTag();
        unknownList.add(unknownRecord);
        savedLedger = new CompoundTag();
        savedLedger.put("ThemeWitnesses", unknownList);
        ledger = UnderworldLedger.load(savedLedger);
        check(
                !ledger.hasChanges()
                        && ledger.save(new CompoundTag())
                                .getList("ThemeWitnesses", Tag.TAG_COMPOUND)
                                .equals(unknownList),
                "wrong ledger format type preserved but inactive");
        System.out.println(
                "JOURNEY_API_PASS: "
                        + assertions
                        + " assertions; six items, local reset, epochs, legacy/nested ledger, corrupt data, commands, cooldowns");
    }

    public JourneyApiSmoke() {
        MinecraftForge.EVENT_BUS.register(this);
    }

    @SubscribeEvent
    public void started(ServerStartedEvent e) {
        boolean pass = false;
        try {
            StateApiSmoke.run(e.getServer().overworld());
            RenewalSmoke.run(e.getServer().overworld());
            for (int cycle = 0; cycle < 25; cycle++) test(e.getServer().overworld());
            JourneyQuerySmoke.run(e.getServer().overworld());
            pass = true;
        } catch (Throwable failure) {
            failure.printStackTrace();
        } finally {
            try {
                java.nio.file.Files.writeString(
                        java.nio.file.Path.of("journey-api-result.txt"), pass ? "PASS" : "FAIL");
            } catch (Exception failure) {
                throw new RuntimeException(failure);
            }
            e.getServer().halt(false);
        }
    }
}
