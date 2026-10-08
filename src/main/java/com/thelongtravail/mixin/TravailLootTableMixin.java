package com.thelongtravail.mixin;

import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraftforge.common.ForgeHooks;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

// 同时处理世界数据包中的战利品表；Forge 不会为这些表触发 LootTableLoadEvent。
@Mixin(value = ForgeHooks.class, remap = false)
public abstract class TravailLootTableMixin {
    @Redirect(method = "loadLootTable", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/level/storage/loot/LootTable;freeze()V"))
    private static void travail$addSharedPool(LootTable table) {
        com.thelongtravail.loot.UnifiedLoot.inject(table);
        table.freeze();
    }
}
