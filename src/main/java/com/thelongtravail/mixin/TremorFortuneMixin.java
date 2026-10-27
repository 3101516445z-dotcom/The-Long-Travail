package com.thelongtravail.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import com.thelongtravail.valley.PlayerBlockBreaker;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.functions.ApplyBonusCount;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(ApplyBonusCount.class)
public abstract class TremorFortuneMixin {
    @Shadow @Final private Enchantment enchantment;

    // 公式接收 Curios 已叠加饰品时运后的等级。
    @ModifyArg(
            method = "run",
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Lnet/minecraft/world/level/storage/loot/functions/ApplyBonusCount$Formula;calculateNewCount(Lnet/minecraft/util/RandomSource;II)I"),
            index = 2)
    private int travail$effectiveFortune(int level, @Local(argsOnly = true) LootContext context) {
        return enchantment == Enchantments.BLOCK_FORTUNE
                ? PlayerBlockBreaker.fortuneForLoot(level, context)
                : level;
    }
}
