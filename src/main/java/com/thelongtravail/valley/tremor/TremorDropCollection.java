package com.thelongtravail.valley.tremor;

import com.thelongtravail.item.HighFrequencyTremorItem;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.items.ItemHandlerHelper;
import net.minecraftforge.items.wrapper.PlayerMainInvWrapper;

import java.util.ArrayList;
import java.util.List;

public final class TremorDropCollection {
    private record Harvest(
            ServerLevel level,
            ServerPlayer player,
            BlockPos pos,
            BlockState state,
            ItemStack tool) {}

    private static final ThreadLocal<Harvest> ACTIVE = new ThreadLocal<>();

    // 仅包围已成功破坏后的采收回调；嵌套调用和异常都恢复外层上下文。
    public static void harvest(
            Level level,
            Player player,
            BlockPos pos,
            BlockState state,
            ItemStack tool,
            Runnable action) {
        Harvest previous = ACTIVE.get();
        Harvest current =
                level instanceof ServerLevel server
                                && player instanceof ServerPlayer actor
                                && !actor.isCreative()
                                && tool.getItem() instanceof HighFrequencyTremorItem
                        ? new Harvest(server, actor, pos.immutable(), state, tool)
                        : null;
        if (current == null) ACTIVE.remove();
        else ACTIVE.set(current);
        try {
            action.run();
        } finally {
            if (previous == null) ACTIVE.remove();
            else ACTIVE.set(previous);
        }
    }

    // 由 dropResources 的最终战利品列表调用；单独 getDrops 查询不产生入包副作用。
    public static List<ItemStack> collect(
            List<ItemStack> drops,
            BlockState state,
            ServerLevel level,
            BlockPos pos,
            Entity actor,
            ItemStack tool) {
        Harvest c = ACTIVE.get();
        if (c == null
                || c.level != level
                || c.player != actor
                || c.state != state
                || c.tool != tool
                || !c.pos.equals(pos)
                || c.player.serverLevel() != level
                || !c.player.isAlive()
                || level.restoringBlockSnapshots
                || !level.getGameRules().getBoolean(GameRules.RULE_DOBLOCKDROPS)) return drops;
        var inventory = new PlayerMainInvWrapper(c.player.getInventory());
        List<ItemStack> remainder = new ArrayList<>();
        for (ItemStack stack : drops) {
            ItemStack left = ItemHandlerHelper.insertItemStacked(inventory, stack, false);
            if (!left.isEmpty()) remainder.add(left);
        }
        return remainder;
    }

    private TremorDropCollection() {}
}
