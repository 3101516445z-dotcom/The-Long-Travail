package com.thelongtravail.mixin.loot;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.thelongtravail.config.TravailConfig;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSet;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(LootTable.class)
public abstract class ArchaeologyLootMixin {
    @Shadow
    public abstract LootContextParamSet getParamSet();

    // 子表通过 getRandomItemsRaw 抽取，仍保留表内逻辑；只跳过最终 Forge 修改器调用。
    @WrapOperation(
            method =
                    "getRandomItems(Lnet/minecraft/world/level/storage/loot/LootContext;)Lit/unimi/dsi/fastutil/objects/ObjectArrayList;",
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Lnet/minecraftforge/common/ForgeHooks;modifyLoot(Lnet/minecraft/resources/ResourceLocation;Lit/unimi/dsi/fastutil/objects/ObjectArrayList;Lnet/minecraft/world/level/storage/loot/LootContext;)Lit/unimi/dsi/fastutil/objects/ObjectArrayList;",
                            remap = false))
    private ObjectArrayList<ItemStack> travail$protectArchaeology(
            ResourceLocation tableId,
            ObjectArrayList<ItemStack> generatedLoot,
            LootContext context,
            Operation<ObjectArrayList<ItemStack>> original) {
        if (getParamSet() == LootContextParamSets.ARCHAEOLOGY
                && TravailConfig.PROTECT_ARCHAEOLOGY_LOOT.get()) {
            context.setQueriedLootTableId(tableId);
            return generatedLoot;
        }
        return original.call(tableId, generatedLoot, context);
    }
}
