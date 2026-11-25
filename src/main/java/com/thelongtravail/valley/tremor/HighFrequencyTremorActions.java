package com.thelongtravail.valley.tremor;

import com.thelongtravail.TravailAspect;
import com.thelongtravail.api.TravailStateApi;
import com.thelongtravail.compat.ftbultimine.TremorUltimine;
import com.thelongtravail.item.HighFrequencyTremorItem;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.Event;

public final class HighFrequencyTremorActions {
    public static boolean eligible(Player player, InteractionHand hand) {
        return hand == InteractionHand.MAIN_HAND
                && player.isShiftKeyDown()
                && player.getMainHandItem().getItem() instanceof HighFrequencyTremorItem
                && TravailStateApi.state(player, TravailAspect.DEEP_VALLEY)
                        != TravailStateApi.AspectState.INACTIVE;
    }

    // 在 Forge 右键事件及保护监听器全部处理完毕后执行。
    public static void use(PlayerInteractEvent.RightClickBlock event) {
        Player player = event.getEntity();
        if (event.isCanceled()
                || event.getUseBlock() == Event.Result.DENY
                || event.getUseItem() == Event.Result.DENY
                || !eligible(player, event.getHand())) return;
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.sidedSuccess(player.level().isClientSide));
        if (player instanceof ServerPlayer server) {
            var level = server.serverLevel();
            var state =
                    level.hasChunkAt(event.getPos()) ? level.getBlockState(event.getPos()) : null;
            try {
                if (state != null
                        && TravailStateApi.state(server, TravailAspect.DEEP_VALLEY)
                                == TravailStateApi.AspectState.WITNESS
                        && PlayerBlockBreaker.blacklisted(state))
                    TremorFeedback.BLACKLISTED.send(server);
                if (TremorUltimine.tryBreak(
                        server, event.getHand(), event.getPos(), event.getFace())) return;
                PlayerBlockBreaker.breakBlock(
                        server,
                        event.getPos(),
                        TravailStateApi.state(server, TravailAspect.DEEP_VALLEY));
            } catch (RuntimeException failure) {
                PlayerBlockBreaker.reportFailure(level, event.getPos(), state, failure);
                TremorFeedback.FAILURE.send(server);
            }
        }
    }

    private HighFrequencyTremorActions() {}
}
