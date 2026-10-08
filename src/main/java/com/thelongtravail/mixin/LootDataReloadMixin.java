package com.thelongtravail.mixin;

import com.thelongtravail.loot.UnifiedLoot;
import net.minecraft.world.level.storage.loot.LootDataManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import java.util.concurrent.CompletableFuture;

@Mixin(LootDataManager.class)
public abstract class LootDataReloadMixin {
    @Inject(method = "reload", at = @At("HEAD"))
    private void travail$reloadRules(CallbackInfoReturnable<CompletableFuture<Void>> callback) {
        UnifiedLoot.reload();
    }
}
