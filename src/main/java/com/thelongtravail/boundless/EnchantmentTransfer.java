package com.thelongtravail.boundless;

import com.thelongtravail.common.RuntimeFailures;
import com.thelongtravail.item.AncientStrangeTalesItem;

import net.minecraft.network.chat.Component;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.BooleanSupplier;

/** 在单件副本上计算附魔转移，合成输入保持不变。 */
public final class EnchantmentTransfer {
    private EnchantmentTransfer() {}

    public enum Filter {
        ALL,
        NON_CURSE,
        CURSE;

        boolean accepts(Enchantment enchantment) {
            return this == ALL || enchantment.isCurse() == (this == CURSE);
        }
    }

    public record Result(ItemStack source, ItemStack target) {}

    public record Failure(String enchantment, String stage) {
        public Component message() {
            return enchantment == null
                    ? Component.translatable("message.the_long_travail.transfer.failed")
                    : Component.translatable(
                            "message.the_long_travail.transfer.enchantment_failed", enchantment);
        }
    }

    public record Evaluation(Result result, Failure failure) {}

    private static final class CallbackFailure extends RuntimeException {
        final Failure detail;

        CallbackFailure(String enchantment, String stage, RuntimeException cause) {
            super(cause);
            detail = new Failure(enchantment, stage);
        }
    }

    private static String id(Enchantment enchantment) {
        return String.valueOf(ForgeRegistries.ENCHANTMENTS.getKey(enchantment));
    }

    private static boolean callback(
            Enchantment enchantment, Enchantment other, String stage, BooleanSupplier action) {
        try {
            return action.getAsBoolean();
        } catch (RuntimeException failure) {
            throw new CallbackFailure(
                    id(enchantment) + (other == null ? "" : " / " + id(other)), stage, failure);
        }
    }

    public static Evaluation evaluate(ItemStack source, ItemStack target, Filter filter) {
        try {
            return new Evaluation(compute(source, target, filter), null);
        } catch (RuntimeException failure) {
            Failure detail =
                    failure instanceof CallbackFailure callback
                            ? callback.detail
                            : new Failure(null, "transfer");
            RuntimeFailures.report(
                    "enchantment transfer "
                            + detail
                            + " source="
                            + (source == null
                                    ? "null"
                                    : ForgeRegistries.ITEMS.getKey(source.getItem()))
                            + " target="
                            + (target == null
                                    ? "null"
                                    : ForgeRegistries.ITEMS.getKey(target.getItem())),
                    failure);
            return new Evaluation(null, detail);
        }
    }

    public static ItemStack single(ItemStack stack) {
        ItemStack copy = stack.copy();
        copy.setCount(1);
        return copy;
    }

    public static boolean isTales(ItemStack stack) {
        return stack.getItem() instanceof AncientStrangeTalesItem;
    }

    public static Result transfer(ItemStack source, ItemStack target, Filter filter) {
        return evaluate(source, target, filter).result();
    }

    private static Result compute(ItemStack source, ItemStack target, Filter filter) {
        if (source.isEmpty() || target.isEmpty() || (isTales(source) && isTales(target)))
            return null;
        var from = read(source);
        var to = read(target);
        if (from == null || to == null) return null;
        boolean book = isTales(target) || target.is(Items.BOOK) || target.is(Items.ENCHANTED_BOOK);
        boolean changed = false;
        var iterator = from.entrySet().iterator();
        while (iterator.hasNext()) {
            var entry = iterator.next();
            Enchantment enchantment = entry.getKey();
            if (!callback(enchantment, null, "isCurse", () -> filter.accepts(enchantment)))
                continue;
            if (!book) {
                ItemStack candidate = target.copy();
                if (!callback(
                        enchantment, null, "canEnchant", () -> enchantment.canEnchant(candidate)))
                    continue;
            }
            if (to.keySet().stream()
                    .anyMatch(
                            other ->
                                    other != enchantment
                                            && !callback(
                                                    enchantment,
                                                    other,
                                                    "isCompatibleWith",
                                                    () -> enchantment.isCompatibleWith(other))))
                continue;
            int current = to.containsKey(enchantment) ? to.get(enchantment).getInt("lvl") : 0;
            int level = entry.getValue().getInt("lvl");
            int moved = Math.min(level, Math.max(0, 255 - current));
            if (moved == 0) continue;
            CompoundTag received =
                    to.containsKey(enchantment)
                            ? to.get(enchantment).copy()
                            : entry.getValue().copy();
            received.putShort("lvl", (short) (current + moved));
            to.put(enchantment, received);
            if (moved == level) iterator.remove();
            else entry.getValue().putInt("lvl", level - moved);
            changed = true;
        }
        if (!changed) return null;
        ItemStack sourceAfter = single(source);
        ItemStack targetAfter = single(target);
        if (target.is(Items.BOOK)) {
            targetAfter = new ItemStack(Items.ENCHANTED_BOOK);
            if (target.hasTag()) targetAfter.setTag(target.getTag().copy());
            targetAfter.removeTagKey("Enchantments");
        }
        write(sourceAfter, from);
        write(targetAfter, to);
        return new Result(sourceAfter, targetAfter);
    }

    private static String key(ItemStack stack) {
        return stack.is(Items.ENCHANTED_BOOK) ? "StoredEnchantments" : "Enchantments";
    }

    // 未知或非法条目使转移失效，保留原有附魔数据。
    private static Map<Enchantment, CompoundTag> read(ItemStack stack) {
        Map<Enchantment, CompoundTag> result = new LinkedHashMap<>();
        CompoundTag tag = stack.getTag();
        if (tag == null || !tag.contains(key(stack))) return result;
        if (!(tag.get(key(stack)) instanceof ListTag list)) return null;
        for (Tag raw : list) {
            if (!(raw instanceof CompoundTag entry)
                    || !entry.contains("id", Tag.TAG_STRING)
                    || !entry.contains("lvl", Tag.TAG_ANY_NUMERIC)
                    || entry.getInt("lvl") <= 0) return null;
            ResourceLocation id = ResourceLocation.tryParse(entry.getString("id"));
            if (id == null || !ForgeRegistries.ENCHANTMENTS.containsKey(id)) return null;
            Enchantment enchantment = ForgeRegistries.ENCHANTMENTS.getValue(id);
            if (enchantment == null || result.put(enchantment, entry.copy()) != null) return null;
        }
        return result;
    }

    private static void write(ItemStack stack, Map<Enchantment, CompoundTag> entries) {
        if (entries.isEmpty()) stack.removeTagKey(key(stack));
        else {
            ListTag list = new ListTag();
            entries.values().forEach(entry -> list.add(entry.copy()));
            stack.addTagElement(key(stack), list);
        }
    }
}
