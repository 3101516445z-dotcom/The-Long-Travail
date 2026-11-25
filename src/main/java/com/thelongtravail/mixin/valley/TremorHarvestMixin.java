package com.thelongtravail.mixin.valley;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.thelongtravail.item.HighFrequencyTremorItem;
import com.thelongtravail.valley.tremor.TremorDropCollection;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerPlayerGameMode;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(ServerPlayerGameMode.class)
public abstract class TremorHarvestMixin {
    @Shadow @Final protected ServerPlayer player;

    @ModifyArg(
            method = "destroyBlock",
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Lnet/minecraft/world/level/block/Block;popExperience(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/core/BlockPos;I)V"),
            index = 1)
    private BlockPos travail$experiencePosition(BlockPos original) {
        return player.getMainHandItem().getItem() instanceof HighFrequencyTremorItem
                ? player.blockPosition()
                : original;
    }

    @WrapOperation(
            method = "destroyBlock",
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Lnet/minecraft/world/level/block/Block;playerDestroy(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/block/entity/BlockEntity;Lnet/minecraft/world/item/ItemStack;)V"))
    private void travail$collectHarvest(
            Block block,
            Level level,
            Player player,
            BlockPos pos,
            BlockState state,
            BlockEntity entity,
            ItemStack tool,
            Operation<Void> original) {
        TremorDropCollection.harvest(
                level,
                player,
                pos,
                tool,
                () -> original.call(block, level, player, pos, state, entity, tool));
    }
}
