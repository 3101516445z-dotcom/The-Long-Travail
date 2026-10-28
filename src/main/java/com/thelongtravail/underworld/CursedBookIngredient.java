package com.thelongtravail.underworld;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.EnchantmentInstance;
import net.minecraftforge.common.crafting.AbstractIngredient;
import net.minecraftforge.common.crafting.CraftingHelper;
import net.minecraftforge.common.crafting.IIngredientSerializer;
import net.minecraftforge.registries.ForgeRegistries;

/** 匹配含已注册诅咒的附魔书，允许同时含有其他附魔。 */
public final class CursedBookIngredient extends AbstractIngredient {
    public static final ResourceLocation ID =
            ResourceLocation.fromNamespaceAndPath("the_long_travail", "cursed_book");
    private static final Serializer SERIALIZER = new Serializer();

    public static void register() {
        CraftingHelper.register(ID, SERIALIZER);
    }

    @Override
    public boolean test(ItemStack stack) {
        return stack != null
                && stack.is(Items.ENCHANTED_BOOK)
                && EnchantmentHelper.getEnchantments(stack).keySet().stream()
                        .anyMatch(e -> e.isCurse());
    }

    @Override
    public ItemStack[] getItems() {
        // 候选用于配方展示；实际资格由 test() 根据附魔书 NBT 判断。
        return ForgeRegistries.ENCHANTMENTS.getValues().stream()
                .filter(e -> e.isCurse())
                .map(e -> EnchantedBookItem.createForEnchantment(new EnchantmentInstance(e, 1)))
                .toArray(ItemStack[]::new);
    }

    @Override
    public boolean isEmpty() {
        return false;
    }

    @Override
    public boolean isSimple() {
        return false;
    }

    @Override
    public IIngredientSerializer<? extends Ingredient> getSerializer() {
        return SERIALIZER;
    }

    @Override
    public JsonElement toJson() {
        var json = new JsonObject();
        json.addProperty("type", ID.toString());
        return json;
    }

    private static final class Serializer implements IIngredientSerializer<CursedBookIngredient> {
        @Override
        public CursedBookIngredient parse(JsonObject json) {
            return new CursedBookIngredient();
        }

        @Override
        public CursedBookIngredient parse(FriendlyByteBuf buffer) {
            return new CursedBookIngredient();
        }

        @Override
        public void write(FriendlyByteBuf buffer, CursedBookIngredient ingredient) {
            // 材料条件无额外状态，序列化器 ID 由 Forge 写入。
        }
    }
}
