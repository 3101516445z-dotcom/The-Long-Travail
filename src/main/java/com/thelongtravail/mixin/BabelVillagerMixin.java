package com.thelongtravail.mixin;

import com.thelongtravail.farreach.BabelTrades;
import com.thelongtravail.config.FarReachRelicsConfig;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;

@Mixin(Villager.class)
public abstract class BabelVillagerMixin {
    @com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod(method = "addAdditionalSaveData")
    private void travail$save(
            net.minecraft.nbt.CompoundTag tag,
            com.llamalad7.mixinextras.injector.wrapoperation.Operation<Void> original) {
        var v = (Villager) (Object) this;
        BabelTrades.remove(v);
        try {
            original.call(tag);
        } finally {
            if (v.getTradingPlayer() != null) BabelTrades.refresh(v, v.getTradingPlayer());
        }
    }

    @Shadow private long lastRestockGameTime;
    @Shadow private int numberOfRestocksToday;
    @Shadow private long lastRestockCheckDayTime;

    @Inject(method = "restock", at = @At("TAIL"))
    private void travail$recordRestock(CallbackInfo ci) {
        var v = (Villager) (Object) this;
        var n = v.getPersistentData();
        long day = Math.floorDiv(v.level().getDayTime(), 24000L);
        int count =
                n.contains("TravailBabelActualDay") && n.getLong("TravailBabelActualDay") == day
                        ? n.getInt("TravailBabelActualCount")
                        : 0;
        n.putLong("TravailBabelActualDay", day);
        n.putInt("TravailBabelActualCount", Math.min(Integer.MAX_VALUE - 1, count) + 1);
        n.putLong("TravailBabelActualTime", v.level().getGameTime());
    }

    @Inject(method = "updateSpecialPrices", at = @At("HEAD"))
    private void travail$remove(Player p, CallbackInfo ci) {
        BabelTrades.remove((Villager) (Object) this);
    }

    @Inject(method = "updateSpecialPrices", at = @At("TAIL"))
    private void travail$price(Player p, CallbackInfo ci) {
        BabelTrades.refresh((Villager) (Object) this, p);
    }

    @Inject(method = "resetSpecialPrices", at = @At("TAIL"))
    private void travail$reset(CallbackInfo ci) {
        BabelTrades.forget((Villager) (Object) this);
    }

    @Inject(method = "customServerAiStep", at = @At("HEAD"))
    private void travail$refresh(CallbackInfo ci) {
        var v = (Villager) (Object) this;
        if (v.getTradingPlayer() != null && BabelTrades.refresh(v, v.getTradingPlayer()))
            BabelTrades.sync(v);
    }

    @Inject(method = "shouldRestock", at = @At("HEAD"), cancellable = true)
    private void travail$restock(CallbackInfoReturnable<Boolean> cir) {
        var v = (Villager) (Object) this;
        var n = v.getPersistentData();
        long day = Math.floorDiv(v.level().getDayTime(), 24000L);
        boolean enhanced = BabelTrades.nearby(v);
        if (!enhanced
                && (!n.contains("TravailBabelRestockDay")
                        || n.getLong("TravailBabelRestockDay") != day)) return;
        if (!n.contains("TravailBabelRestockDay") || n.getLong("TravailBabelRestockDay") != day) {
            if (Math.floorDiv(lastRestockCheckDayTime, 24000L) != day) numberOfRestocksToday = 0;
            n.putLong("TravailBabelRestockDay", day);
        }
        lastRestockCheckDayTime = v.level().getDayTime();
        int actualCount =
                n.contains("TravailBabelActualDay") && n.getLong("TravailBabelActualDay") == day
                        ? n.getInt("TravailBabelActualCount")
                        : 0;
        int count = Math.max(numberOfRestocksToday, actualCount);
        long last =
                n.contains("TravailBabelActualTime")
                        ? Math.max(lastRestockGameTime, n.getLong("TravailBabelActualTime"))
                        : lastRestockGameTime;
        int limit = enhanced ? (int) FarReachRelicsConfig.value("babel.restocksPerDay") : 2;
        long interval = enhanced ? FarReachRelicsConfig.ticks("babel.restockSeconds") : 2400;
        cir.setReturnValue(
                count < limit
                        && v.level().getGameTime() - last >= interval
                        && v.getOffers().stream().anyMatch(o -> o.needsRestock()));
    }
}
