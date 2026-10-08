package com.thelongtravail.mixin;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.thelongtravail.flourishing.FloralEvents;
import net.minecraft.world.food.FoodData;
import net.minecraft.world.item.*;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
@Mixin(FoodData.class)
public abstract class FloralFoodMixin {
    @WrapMethod(method="eat(Lnet/minecraft/world/item/Item;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/entity/LivingEntity;)V", remap=false)
    private void travail$food(Item item,ItemStack stack,LivingEntity entity,Operation<Void> original) {
        FloralEvents.food((FoodData)(Object)this,stack,entity,()->original.call(item,stack,entity));
    }
}
