package com.thelongtravail.farreach;

import com.thelongtravail.config.FarReachItemsConfig;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.*;
import net.minecraft.world.item.enchantment.*;
import net.minecraftforge.registries.ForgeRegistries;
import java.util.*;

@net.minecraftforge.fml.common.Mod.EventBusSubscriber(modid = "the_long_travail")
public final class GoldenAgePools {
    private record Pool<T>(List<String> blocked, List<T> entries) {}

    private static final boolean DEFAULT_FOOD_LOOKUP = defaultFoodLookup();

    private static boolean defaultFoodLookup() {
        try {
            // 其他模组若重写基础查询，仍须使用包含完整物品堆叠的查询路径。
            return Item.class
                                    .getMethod(
                                            "getFoodProperties",
                                            ItemStack.class,
                                            LivingEntity.class)
                                    .getDeclaringClass()
                            == net.minecraftforge.common.extensions.IForgeItem.class
                    && ItemStack.class
                                    .getMethod("getFoodProperties", LivingEntity.class)
                                    .getDeclaringClass()
                            == net.minecraftforge.common.extensions.IForgeItemStack.class;
        } catch (ReflectiveOperationException | SecurityException ignored) {
            return false;
        }
    }

    private static volatile Pool<Item> foodCandidates;
    private static volatile Pool<Enchantment> enchantmentCandidates;

    public static void clear() {
        foodCandidates = null;
        enchantmentCandidates = null;
    }

    @net.minecraftforge.eventbus.api.SubscribeEvent
    public static void tagsUpdated(net.minecraftforge.event.TagsUpdatedEvent event) {
        clear();
    }

    @net.minecraftforge.eventbus.api.SubscribeEvent
    public static void stopped(net.minecraftforge.event.server.ServerStoppedEvent event) {
        clear();
    }

    public static List<Enchantment> enchantments() {
        var configured = FarReachItemsConfig.ENCHANTMENT_BLACKLIST.get();
        var cached = enchantmentCandidates;
        if (cached != null && cached.blocked.equals(configured)) return cached.entries;
        List<String> snapshot = List.copyOf(configured);
        Set<String> blocked = new HashSet<>(snapshot);
        var entries =
                ForgeRegistries.ENCHANTMENTS.getValues().stream()
                        .filter(
                                e ->
                                        !blocked.contains(
                                                Objects.requireNonNull(
                                                                ForgeRegistries.ENCHANTMENTS.getKey(
                                                                        e))
                                                        .toString()))
                        .toList();
        enchantmentCandidates = new Pool<>(snapshot, entries);
        return entries;
    }

    public static boolean nutritious(FoodProperties food) {
        if (food == null
                || !(food.getNutrition() >= FarReachItemsConfig.get("golden_age.minNutrition"))
                || !(2.0 * food.getNutrition() * food.getSaturationModifier()
                        >= FarReachItemsConfig.get("golden_age.minSaturation"))) return false;
        if (FarReachItemsConfig.ALLOW_HARMFUL.get()) return true;
        for (var effect : food.getEffects())
            if (effect.getFirst() != null
                    && effect.getFirst().getEffect().getCategory() == MobEffectCategory.HARMFUL)
                return false;
        return true;
    }

    public static List<ItemStack> foods(ServerPlayer player) {
        long started = com.thelongtravail.diagnostics.RuntimeDiagnostics.start();
        try {
            return collectFoods(player);
        } finally {
            com.thelongtravail.diagnostics.RuntimeDiagnostics.end(
                    com.thelongtravail.diagnostics.RuntimeDiagnostics.Metric.FOOD_POOL_NS, started);
        }
    }

    private static List<ItemStack> collectFoods(ServerPlayer player) {
        List<ItemStack> result = new ArrayList<>();
        for (Item item : foodCandidates()) {
            // 只缓存基础类型的非空结果，确保脚本修改食物属性后仍能立即生效。
            if (DEFAULT_FOOD_LOOKUP
                    && (item.getClass() == Item.class || item.getClass() == BlockItem.class)
                    && item.getFoodProperties() == null) continue;
            ItemStack stack = item.getDefaultInstance();
            if (!stack.isEmpty() && nutritious(stack.getFoodProperties(player))) {
                stack.setCount(1);
                result.add(stack);
            }
        }
        return result;
    }

    private static List<Item> foodCandidates() {
        var configured = FarReachItemsConfig.FOOD_BLACKLIST.get();
        var cached = foodCandidates;
        if (cached != null && cached.blocked.equals(configured)) return cached.entries;
        List<String> snapshot = List.copyOf(configured);
        Set<String> blocked = new HashSet<>(snapshot);
        List<Item> entries = new ArrayList<>();
        for (Item item : ForgeRegistries.ITEMS.getValues())
            if (!blocked.contains(
                    Objects.requireNonNull(ForgeRegistries.ITEMS.getKey(item)).toString()))
                entries.add(item);
        cached = new Pool<>(snapshot, List.copyOf(entries));
        foodCandidates = cached;
        return cached.entries;
    }

    private GoldenAgePools() {}
}
