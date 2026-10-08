package com.thelongtravail.farreach;

import com.thelongtravail.config.FarReachItemsConfig;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.*;
import net.minecraft.world.item.enchantment.*;
import net.minecraftforge.registries.ForgeRegistries;
import java.util.*;

public final class GoldenAgePools {
    public static List<Enchantment> enchantments(){
        Set<String> blocked=new HashSet<>(FarReachItemsConfig.ENCHANTMENT_BLACKLIST.get());
        return ForgeRegistries.ENCHANTMENTS.getValues().stream().filter(e->!blocked.contains(Objects.requireNonNull(ForgeRegistries.ENCHANTMENTS.getKey(e)).toString())).toList();
    }
    public static boolean nutritious(FoodProperties food){
        return food!=null&&food.getNutrition()>=FarReachItemsConfig.get("golden_age.minNutrition")
                &&2.0*food.getNutrition()*food.getSaturationModifier()>=FarReachItemsConfig.get("golden_age.minSaturation")
                &&(FarReachItemsConfig.ALLOW_HARMFUL.get()||food.getEffects().stream().noneMatch(e->e.getFirst().getEffect().getCategory()==MobEffectCategory.HARMFUL));
    }
    public static List<ItemStack> foods(ServerPlayer player){
        Set<String> blocked=new HashSet<>(FarReachItemsConfig.FOOD_BLACKLIST.get());List<ItemStack> result=new ArrayList<>();
        for(Item item:ForgeRegistries.ITEMS.getValues()){
            if(blocked.contains(Objects.requireNonNull(ForgeRegistries.ITEMS.getKey(item)).toString()))continue;
            ItemStack stack=item.getDefaultInstance();if(!stack.isEmpty()&&nutritious(stack.getFoodProperties(player))){stack.setCount(1);result.add(stack);}
        }
        return result;
    }
    private GoldenAgePools(){}
}
