package com.thelongtravail.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.thelongtravail.farreach.GoldenAgeActions;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.common.ForgeHooks;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import org.spongepowered.asm.mixin.Mixin;

// 等待所有保护监听器执行完毕，再发放奖励或拦截交互。
@Mixin(value=ForgeHooks.class,remap=false)
public abstract class GoldenAgeInteractionMixin {
    @WrapMethod(method="onRightClickBlock",remap=false)
    private static PlayerInteractEvent.RightClickBlock travail$dig(Player player,InteractionHand hand,BlockPos pos,BlockHitResult hit,
            Operation<PlayerInteractEvent.RightClickBlock> original){
        var event=original.call(player,hand,pos,hit);
        GoldenAgeActions.dig(event);
        return event;
    }
}
