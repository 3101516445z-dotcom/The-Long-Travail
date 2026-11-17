package com.thelongtravail.mixin.anvil;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.thelongtravail.TravailAspect;
import com.thelongtravail.equipment.TravailCurios;
import com.thelongtravail.journey.LongTravailData;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AnvilScreen;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(AnvilScreen.class)
public abstract class AnvilScreenMixin {
    @ModifyExpressionValue(
            method = "renderLabels",
            at = @At(value = "CONSTANT", args = "intValue=40"))
    private int theLongTravail$showWitnessCost(int original) {
        var player = Minecraft.getInstance().player;
        if (player == null) return original;
        var stack = TravailCurios.stack(player);
        return !stack.isEmpty() && LongTravailData.hasWitness(stack, TravailAspect.UNDERWORLD)
                ? Integer.MAX_VALUE
                : original;
    }
}
