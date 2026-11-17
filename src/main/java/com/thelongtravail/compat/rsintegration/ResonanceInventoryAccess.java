package com.thelongtravail.compat.rsintegration;

import com.huanghuang.rsintegration.api.ResourceMatchMode;
import com.huanghuang.rsintegration.api.resonance.RSIResonanceApi;
import com.huanghuang.rsintegration.api.resonance.ResonanceDiagnosticCode;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

// 仅由可选联动入口加载，不跨查询保留库存快照或玩家引用。
final class ResonanceInventoryAccess {
    static boolean contains(ServerPlayer player, Item item) {
        var api =
                RSIResonanceApi.get()
                        .orElseThrow(
                                () ->
                                        new IllegalStateException(
                                                "RSI resonance API is unavailable"));
        if (!api.version().supports(1, 0, 0))
            throw new IllegalStateException("Unsupported RSI resonance API: " + api.version());
        var result = api.openInventory(player);
        if (result.successful())
            return result.value().hasItem(new ItemStack(item), ResourceMatchMode.ANY_IDENTITY);
        if (!result.diagnostics().isEmpty()
                && result.diagnostics().stream()
                        .allMatch(
                                diagnostic ->
                                        diagnostic.code()
                                                == ResonanceDiagnosticCode.STORAGE_UNAVAILABLE))
            return false;
        throw new IllegalStateException("RSI resonance query failed: " + result.diagnostics());
    }

    private ResonanceInventoryAccess() {}
}
