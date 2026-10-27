package com.thelongtravail.config;

import com.thelongtravail.abyss.RainEvents;
import com.thelongtravail.data.RuntimePools;
import com.thelongtravail.farreach.GoldenAgeActions;
import com.thelongtravail.flourishing.Affection;
import com.thelongtravail.network.TravailNetwork;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.fml.event.config.ModConfigEvent;
import net.minecraftforge.server.ServerLifecycleHooks;

// 配置发布后，在服务端线程按顺序刷新运行状态，最后同步客户端提示快照。
public final class ConfigReloadCoordinator {
    public static void onConfigReloading(ModConfigEvent.Reloading event) {
        if (!ConfigFiles.isCommonSpec(event.getConfig().getSpec())) return;
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server != null)
            server.execute(
                    () -> {
                        if (server.isStopped()) return;
                        DeepValleyItemsConfig.checkTremorBlacklist();
                        RuntimePools.reload();
                        com.thelongtravail.abyss.LiquefiedAbundance.reload();
                        GoldenAgeActions.reload();
                        Affection.resetAll();
                        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                            player.inventoryMenu.slotsChanged(player.getInventory());
                            if (player.containerMenu != player.inventoryMenu)
                                player.containerMenu.slotsChanged(player.getInventory());
                        }
                        RainEvents.configReloaded(server);
                        TravailNetwork.broadcastTooltipConfig(server);
                    });
    }

    private ConfigReloadCoordinator() {}
}
