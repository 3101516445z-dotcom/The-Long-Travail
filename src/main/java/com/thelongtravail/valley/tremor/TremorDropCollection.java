package com.thelongtravail.valley.tremor;

import com.thelongtravail.item.HighFrequencyTremorItem;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = "the_long_travail")
public final class TremorDropCollection {
    private record Harvest(ServerLevel level, ServerPlayer player, BlockPos pos) {}

    private static final ThreadLocal<Harvest> ACTIVE = new ThreadLocal<>();

    // 仅包围已成功破坏后的采收回调；嵌套调用和异常都恢复外层上下文。
    public static void harvest(
            Level level, Player player, BlockPos pos, ItemStack tool, Runnable action) {
        Harvest previous = ACTIVE.get();
        Harvest current =
                level instanceof ServerLevel server
                                && player instanceof ServerPlayer actor
                                && !actor.isCreative()
                                && tool.getItem() instanceof HighFrequencyTremorItem
                        ? new Harvest(server, actor, pos.immutable())
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

    @SubscribeEvent
    public static void moveDrop(EntityJoinLevelEvent event) {
        Harvest harvest = ACTIVE.get();
        if (harvest == null
                || event.getLevel() != harvest.level()
                || !(event.getEntity() instanceof ItemEntity drop)
                || !drop.blockPosition().equals(harvest.pos())) return;
        ServerPlayer player = harvest.player();
        if (player.isAlive() && player.serverLevel() == harvest.level())
            drop.setPos(player.getX(), player.getY(), player.getZ());
    }

    private TremorDropCollection() {}
}
