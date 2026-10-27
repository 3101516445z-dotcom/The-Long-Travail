package com.thelongtravail.valley;

import com.thelongtravail.api.TravailStateApi.AspectState;
import com.thelongtravail.api.TravailStateApi;
import com.thelongtravail.TravailAspect;
import com.thelongtravail.boundless.BoundlessAcquisition;
import com.thelongtravail.boundless.TimeStopManager;
import com.thelongtravail.config.DeepValleyItemsConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.ForgeHooks;
import net.minecraftforge.registries.ForgeRegistries;
import java.util.List;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;

// 限定特殊破坏作用域；未确认移除成功时不主动结算战利品或获取奖励。
public final class PlayerBlockBreaker {
    // 仅限制本能力的有效时运，约束原版公式的输入与循环次数。
    private static final int MAX_EFFECTIVE_FORTUNE = 1024;

    private record Context(
            ServerPlayer player, BlockPos pos, BlockState state, boolean boostedLoot) {}

    private static final ThreadLocal<Context> ACTIVE = new ThreadLocal<>();
    private static final ThreadLocal<Boolean> HARVESTING = ThreadLocal.withInitial(() -> false);

    // 记录原目标信息，避免回调移除方块或转移玩家后丢失定位。
    public static void reportFailure(
            ServerLevel level, BlockPos pos, BlockState state, RuntimeException failure) {
        com.thelongtravail.TheLongTravail.LOGGER.error(
                "High Frequency Tremor stopped after block callback failure: block={}, dimension={}, pos={}; no retry or rollback",
                state == null ? "<unloaded>" : ForgeRegistries.BLOCKS.getKey(state.getBlock()),
                level.dimension().location(),
                pos,
                failure);
    }

    public static boolean blacklisted(BlockState state) {
        return DeepValleyItemsConfig.TREMOR_BLACKLIST
                .get()
                .contains(ForgeRegistries.BLOCKS.getKey(state.getBlock()).toString());
    }

    public static boolean active() {
        return ACTIVE.get() != null;
    }

    public static boolean breakBlock(ServerPlayer player, BlockPos pos, AspectState mode) {
        var level = player.serverLevel();
        if (active()
                || mode == AspectState.INACTIVE
                || !(player.getMainHandItem().getItem()
                        instanceof com.thelongtravail.item.HighFrequencyTremorItem)
                || !player.mayBuild()
                || player.isSpectator()
                || TimeStopManager.frozen(player)
                || !level.hasChunkAt(pos)
                || !level.getWorldBorder().isWithinBounds(pos)
                || !level.mayInteract(player, pos)
                || player.blockActionRestricted(level, pos, player.gameMode.getGameModeForPlayer()))
            return false;
        BlockState state = level.getBlockState(pos);
        if (state.isAir()) return false;
        boolean witness = mode == AspectState.WITNESS;
        if (witness ? blacklisted(state) : state.getDestroySpeed(level, pos) < 0) return false;
        ItemStack held = player.getMainHandItem();
        AspectState actorMode = TravailStateApi.state(player, TravailAspect.DEEP_VALLEY);
        BlockEntity entity = level.getBlockEntity(pos);
        Context context = new Context(player, pos.immutable(), state, !witness);
        ACTIVE.set(context);
        try {
            int xp =
                    ForgeHooks.onBlockBreakEvent(
                            level, player.gameMode.getGameModeForPlayer(), player, pos);
            if (xp < 0
                    || !sameActor(player, level, held, actorMode)
                    || level.getBlockState(pos) != state
                    || level.getBlockEntity(pos) != entity) return false;
            ItemStack tool = held.copy();
            boolean harvest = state.canHarvestBlock(level, pos, player);
            if (!sameActor(player, level, held, actorMode)
                    || level.getBlockState(pos) != state
                    || level.getBlockEntity(pos) != entity) return false;
            boolean removed =
                    state.onDestroyedByPlayer(
                            level, pos, player, harvest, level.getFluidState(pos));
            // 见证可绕过方块自身的移除拒绝，但仍遵守 Forge 事件取消。
            if (witness && level.getBlockState(pos) == state) {
                if (!sameActor(player, level, held, actorMode)
                        || level.getBlockEntity(pos) != entity) return false;
                removed = level.setBlock(pos, level.getFluidState(pos).createLegacyBlock(), 3);
            }
            if (!removed || level.getBlockState(pos) == state) return false;
            state.getBlock().destroy(level, pos, state);
            if (!player.isCreative()) {
                held.mineBlock(level, state, pos, player);
                if (harvest) {
                    HARVESTING.set(true);
                    try {
                        state.getBlock().playerDestroy(level, player, pos, state, entity, tool);
                    } finally {
                        HARVESTING.remove();
                    }
                }
                if (xp > 0) state.getBlock().popExperience(level, pos, xp);
            }
            level.levelEvent(2001, pos, net.minecraft.world.level.block.Block.getId(state));
            if (player.serverLevel() == level)
                BoundlessAcquisition.broken(player, pos, state, true);
            return true;
        } finally {
            ACTIVE.remove();
        }
    }

    // 玩家的操作上下文改变时，停止本次单块或连锁处理。
    public static boolean sameActor(
            ServerPlayer player, ServerLevel level, ItemStack held, AspectState mode) {
        return player.serverLevel() == level
                && player.isAlive()
                && player.mayBuild()
                && !player.isSpectator()
                && !TimeStopManager.frozen(player)
                && !held.isEmpty()
                && player.getMainHandItem() == held
                && TravailStateApi.state(player, TravailAspect.DEEP_VALLEY) == mode;
    }

    // 只调整当前方块的战利品输入，保留原有掉落与容器回调。
    public static List<ItemStack> resolveDrops(
            BlockState state, LootParams.Builder builder, Operation<List<ItemStack>> original) {
        Context c = ACTIVE.get();
        if (matchesBoostedHarvest(
                c,
                builder.getLevel(),
                state,
                builder.getOptionalParameter(LootContextParams.THIS_ENTITY),
                builder.getOptionalParameter(LootContextParams.ORIGIN))) {
            builder.withLuck(c.player.getLuck() * 2);
        }
        return original.call(state, builder);
    }

    public static int fortuneForLoot(int effective, LootContext loot) {
        if (!matchesBoostedHarvest(
                ACTIVE.get(),
                loot.getLevel(),
                loot.getParamOrNull(LootContextParams.BLOCK_STATE),
                loot.getParamOrNull(LootContextParams.THIS_ENTITY),
                loot.getParamOrNull(LootContextParams.ORIGIN))) return effective;
        return (int) Math.max(0L, Math.min(MAX_EFFECTIVE_FORTUNE, (long) effective * 2));
    }

    // 每次计算重新核对作用域，防止中途回调改变采收上下文。
    private static boolean matchesBoostedHarvest(
            Context c, ServerLevel level, BlockState state, Entity player, Vec3 origin) {
        return c != null
                && HARVESTING.get()
                && c.boostedLoot
                && c.player.serverLevel() == level
                && c.state == state
                && c.player == player
                && origin != null
                && c.pos.equals(BlockPos.containing(origin));
    }

    private PlayerBlockBreaker() {}
}
