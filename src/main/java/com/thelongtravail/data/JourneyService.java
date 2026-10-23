package com.thelongtravail.data;

import com.thelongtravail.TravailAspect;
import com.thelongtravail.event.TravailEvents;
import com.thelongtravail.helper.TravailCurios;
import com.thelongtravail.registry.ModRegistry;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import top.theillusivec4.curios.api.CuriosApi;
import java.util.*;
import java.util.function.Function;

public final class JourneyService {
    public enum QueryPolicy {
        KEEP,
        INVALIDATE
    }

    public record OperationResult(
            JourneyOperations.Status status,
            Set<TravailAspect> changedAspects,
            Set<TravailAspect> resetAspects,
            int beforeMask,
            int afterMask,
            boolean wholeReset,
            QueryPolicy queryPolicy,
            Map<TravailAspect, JourneyState.AspectJourneyKey> beforeKeys,
            Map<TravailAspect, JourneyState.AspectJourneyKey> afterKeys,
            boolean requirementsChanged,
            boolean presentationChanged) {
        public OperationResult {
            changedAspects = Set.copyOf(changedAspects);
            resetAspects = Set.copyOf(resetAspects);
            beforeKeys = Map.copyOf(beforeKeys);
            afterKeys = Map.copyOf(afterKeys);
        }

        public boolean changed() {
            return status == JourneyOperations.Status.SUCCESS;
        }

        public boolean accepted() {
            return changed() || status == JourneyOperations.Status.NO_CHANGE;
        }
    }

    private static final Set<UUID> ACTIVE = new HashSet<>();

    public static OperationResult mutate(
            ServerPlayer player,
            Set<TravailAspect> aspects,
            Set<TravailAspect> resets,
            boolean whole,
            QueryPolicy queries,
            InteractionHand hand,
            Function<ItemStack, JourneyOperations.Status> action) {
        if (!player.server.isSameThread())
            throw new IllegalStateException("Journey writes require the server thread");
        if (!ACTIVE.add(player.getUUID())) return failure(JourneyOperations.Status.BUSY);
        try {
            var found = TravailCurios.find(player);
            if (found.isEmpty()) return failure(JourneyOperations.Status.NO_DIARY);
            var inventory = CuriosApi.getCuriosInventory(player).resolve();
            var handler =
                    inventory.flatMap(
                            value ->
                                    value.getStacksHandler(found.get().slotContext().identifier()));
            if (handler.isEmpty()) return failure(JourneyOperations.Status.NO_DIARY);
            int index = found.get().slotContext().index();
            var slots = handler.get().getStacks();
            ItemStack original = slots.getStackInSlot(index);
            if (!original.is(ModRegistry.LONG_TRAVAIL.get()))
                return failure(JourneyOperations.Status.NO_DIARY);
            if (!JourneyState.validStorage(original)
                    || !JourneyState.supported(JourneyState.root(original)))
                return failure(JourneyOperations.Status.INVALID_DATA);
            ItemStack baseline = original.copy(), replacement = original.copy();
            if (!replacement.hasTag()
                    || !replacement
                            .getTag()
                            .contains("LongTravail", net.minecraft.nbt.Tag.TAG_COMPOUND))
                replacement
                        .getOrCreateTag()
                        .put("LongTravail", new net.minecraft.nbt.CompoundTag());
            ItemStack used = hand == null ? ItemStack.EMPTY : player.getItemInHand(hand);
            ItemStack usedBaseline = used.copy();
            if (hand != null && used.isEmpty()) return failure(JourneyOperations.Status.STALE);
            int before = JourneyState.effectiveMask(JourneyState.root(original));
            var status = action.apply(replacement);
            if (status == JourneyOperations.Status.NO_CHANGE) {
                var keys =
                        new EnumMap<TravailAspect, JourneyState.AspectJourneyKey>(
                                TravailAspect.class);
                for (TravailAspect aspect : aspects) {
                    var key = JourneyState.identity(original, aspect);
                    if (key != null) keys.put(aspect, key);
                }
                return new OperationResult(
                        status,
                        Set.of(),
                        Set.of(),
                        before,
                        before,
                        false,
                        QueryPolicy.KEEP,
                        keys,
                        keys,
                        false,
                        false);
            }
            if (status != JourneyOperations.Status.SUCCESS) return failure(status);
            var currentInventory = CuriosApi.getCuriosInventory(player).resolve();
            var currentHandler =
                    currentInventory.flatMap(
                            value ->
                                    value.getStacksHandler(found.get().slotContext().identifier()));
            if (currentHandler.isEmpty()
                    || currentHandler.get().getStacks() != slots
                    || TravailCurios.stack(player) != original
                    || slots.getStackInSlot(index) != original
                    || !ItemStack.matches(original, baseline)
                    || hand != null
                            && (player.getItemInHand(hand) != used
                                    || !ItemStack.matches(used, usedBaseline)))
                return failure(JourneyOperations.Status.STALE);
            int after = JourneyState.effectiveMask(JourneyState.root(replacement));
            var beforeRoot = JourneyState.root(baseline);
            var afterRoot = JourneyState.root(replacement);
            var beforeKeys =
                    new EnumMap<TravailAspect, JourneyState.AspectJourneyKey>(TravailAspect.class);
            var afterKeys =
                    new EnumMap<TravailAspect, JourneyState.AspectJourneyKey>(TravailAspect.class);
            Set<TravailAspect> changed = EnumSet.noneOf(TravailAspect.class);
            for (TravailAspect aspect : aspects) {
                var previous = JourneyState.identity(beforeRoot, aspect);
                var next = JourneyState.identity(afterRoot, aspect);
                if (previous != null) beforeKeys.put(aspect, previous);
                if (next != null) afterKeys.put(aspect, next);
                boolean flagsChanged = false;
                for (String key : new String[] {"Witnesses", "ForcedWitnesses", "ForcedMalices"})
                    flagsChanged |=
                            ((beforeRoot.getInt(key) ^ afterRoot.getInt(key)) & aspect.mask()) != 0;
                if (whole
                        || resets.contains(aspect)
                        || flagsChanged
                        || aspect == TravailAspect.UNDERWORLD
                                && beforeRoot.getBoolean("UnderworldBookWitness")
                                        != afterRoot.getBoolean("UnderworldBookWitness")
                        || !beforeRoot
                                .getCompound("Requirements")
                                .getCompound(aspect.id())
                                .equals(
                                        afterRoot
                                                .getCompound("Requirements")
                                                .getCompound(aspect.id()))) changed.add(aspect);
            }
            if (queries == QueryPolicy.KEEP) {
                original.setTag(replacement.getTag());
                replacement = original;
            } else slots.setStackInSlot(index, replacement);
            // 当前版本的 Curios 会在游戏刻中执行装备回调；须先消耗原物品，再主动返还因槽位撤销而卸下的饰品。
            if (hand != null && !player.getAbilities().instabuild) used.shrink(1);
            var result =
                    new OperationResult(
                            status,
                            changed,
                            resets,
                            before,
                            after,
                            whole,
                            queries,
                            beforeKeys,
                            afterKeys,
                            whole
                                    || beforeRoot.getLong("RequirementsRevision")
                                            != afterRoot.getLong("RequirementsRevision"),
                            whole
                                    || beforeRoot.getLong("StateRevision")
                                            != afterRoot.getLong("StateRevision"));
            if (whole) TravailEvents.onDiaryReset(player, replacement);
            else TravailEvents.onJourneyChanged(player, replacement, result);
            player.getInventory().setChanged();
            player.containerMenu.broadcastChanges();
            return result;
        } finally {
            ACTIVE.remove(player.getUUID());
        }
    }

    private static OperationResult failure(JourneyOperations.Status status) {
        return new OperationResult(
                status,
                Set.of(),
                Set.of(),
                0,
                0,
                false,
                QueryPolicy.KEEP,
                Map.of(),
                Map.of(),
                false,
                false);
    }

    public static OperationResult resetJourney(ServerPlayer player, InteractionHand hand) {
        return mutate(
                player,
                Set.of(TravailAspect.values()),
                Set.of(TravailAspect.values()),
                true,
                QueryPolicy.INVALIDATE,
                hand,
                copy -> {
                    if (LongTravailData.isInitialized(copy))
                        for (TravailAspect aspect : TravailAspect.values())
                            if (JourneyState.identity(copy, aspect) == null)
                                return JourneyOperations.Status.INVALID_DATA;
                    ItemStack generated = LongTravailData.refreshedCopy(copy, player);
                    if (generated.isEmpty())
                        return JourneyState.supported(JourneyState.root(copy))
                                ? JourneyOperations.Status.INVALID_POOL
                                : JourneyOperations.Status.INVALID_DATA;
                    copy.setTag(generated.getTag());
                    return JourneyOperations.Status.SUCCESS;
                });
    }

    public static OperationResult essence(
            ServerPlayer player, TravailAspect aspect, InteractionHand hand) {
        boolean reset = LongTravailData.hasWitness(TravailCurios.stack(player), aspect);
        return mutate(
                player,
                Set.of(aspect),
                reset ? Set.of(aspect) : Set.of(),
                false,
                QueryPolicy.INVALIDATE,
                hand,
                copy ->
                        reset
                                ? JourneyOperations.resetAspect(
                                        JourneyState.root(copy), aspect, player)
                                : JourneyOperations.completeRandom(
                                        JourneyState.root(copy), aspect, player.getRandom()));
    }

    private JourneyService() {}
}
