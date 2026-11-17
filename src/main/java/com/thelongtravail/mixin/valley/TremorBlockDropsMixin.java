package com.thelongtravail.mixin.valley;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.thelongtravail.valley.tremor.PlayerBlockBreaker;
import com.thelongtravail.valley.tremor.TremorDropCollection;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.List;

@Mixin(Block.class)
public abstract class TremorBlockDropsMixin {
    // Forge 的七参数重载负责实际掉落；其中 getDrops 仍使用原版映射。
    @WrapOperation(
            method =
                    "dropResources(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/entity/BlockEntity;Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/item/ItemStack;Z)V",
            remap = false,
            at =
                    @At(
                            value = "INVOKE",
                            remap = true,
                            target =
                                    "Lnet/minecraft/world/level/block/Block;getDrops(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/entity/BlockEntity;Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/item/ItemStack;)Ljava/util/List;"))
    private static List<ItemStack> travail$collectDrops(
            BlockState state,
            ServerLevel level,
            BlockPos pos,
            BlockEntity entity,
            Entity player,
            ItemStack tool,
            Operation<List<ItemStack>> original) {
        return TremorDropCollection.collect(
                original.call(state, level, pos, entity, player, tool),
                state,
                level,
                pos,
                player,
                tool);
    }

    @WrapOperation(
            method =
                    "getDrops(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/entity/BlockEntity;Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/item/ItemStack;)Ljava/util/List;",
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Lnet/minecraft/world/level/block/state/BlockState;getDrops(Lnet/minecraft/world/level/storage/loot/LootParams$Builder;)Ljava/util/List;"))
    private static List<ItemStack> travail$lootInputs(
            BlockState state, LootParams.Builder builder, Operation<List<ItemStack>> original) {
        return PlayerBlockBreaker.resolveDrops(state, builder, original);
    }
}
