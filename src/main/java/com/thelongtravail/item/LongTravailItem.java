package com.thelongtravail.item;

import com.thelongtravail.TravailAspect;
import com.thelongtravail.data.FlourishingBonus;
import com.thelongtravail.network.JourneySync;
import com.thelongtravail.data.LongTravailData;
import com.thelongtravail.config.TravailConfig;
import com.thelongtravail.helper.TravailCurios;
import com.thelongtravail.network.TooltipConfigSync;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.type.capability.ICurio;
import top.theillusivec4.curios.api.type.capability.ICurioItem;

import java.util.List;
import java.util.Locale;

public class LongTravailItem extends Item implements ICurioItem {
    public LongTravailItem() {
        super(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC).fireResistant());
    }

    @Override
    public boolean canEquip(SlotContext slotContext, ItemStack stack) {
        return "travel_diary".equals(slotContext.identifier());
    }

    @Override
    public boolean canEquipFromUse(SlotContext slotContext, ItemStack stack) {
        return false;
    }

    @Override
    public boolean canRightClickEquip(ItemStack stack) {
        return false;
    }

    @Override
    public boolean canUnequip(SlotContext slotContext, ItemStack stack) {
        return slotContext.entity() instanceof Player player && player.isCreative();
    }

    @Override
    public void onEquip(SlotContext slotContext, ItemStack prevStack, ItemStack stack) {
        if (slotContext.entity() instanceof Player player) com.thelongtravail.data.AltitudePenalty.forget(player);
        if (slotContext.entity() instanceof ServerPlayer player) {
            if (!LongTravailData.tryInitialize(stack, player))
                player.displayClientMessage(Component.translatable("message.the_long_travail.journey.invalid_pool"), true);
            TravailCurios.syncExtraSlots(player, LongTravailData.hasWitness(stack, TravailAspect.BOUNDLESS)
                    ? TravailConfig.BOUNDLESS_EXTRA_CURIO_SLOTS.get() : 0);
        }
    }

    @Override
    public void onUnequip(SlotContext slotContext, ItemStack newStack, ItemStack stack) {
        if (slotContext.entity() instanceof Player player) com.thelongtravail.data.AltitudePenalty.forget(player);
        // Curios 在 NBT 变化时也会调用此方法，包括攻击冷却时间戳更新。
        // 替换日记由 onEquip 校正，只有真正卸下时才清理槽位。
        if (slotContext.entity() instanceof ServerPlayer player && !newStack.is(this))
            TravailCurios.syncExtraSlots(player, 0);
    }

    @Override
    public ICurio.DropRule getDropRule(SlotContext slotContext, DamageSource source, int lootingLevel, boolean recentlyHit, ItemStack stack) {
        return ICurio.DropRule.ALWAYS_KEEP;
    }

    @Override
    public int getFortuneLevel(SlotContext slotContext, net.minecraft.world.level.storage.loot.LootContext lootContext, ItemStack stack) {
        return LongTravailData.hasWitness(stack, TravailAspect.UNDERWORLD) ? TravailConfig.UNDERWORLD_FORTUNE_BONUS.get() : 0;
    }

    @Override
    public int getLootingLevel(SlotContext slotContext, DamageSource source, net.minecraft.world.entity.LivingEntity target, int baseLooting, ItemStack stack) {
        return LongTravailData.hasWitness(stack, TravailAspect.UNDERWORLD) ? TravailConfig.UNDERWORLD_LOOTING_BONUS.get() : 0;
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return false;
    }

    @Override
    public Component getName(ItemStack stack) {
        return super.getName(stack).copy().withStyle(style -> style
                .withColor(0xC5AA77).withInsertion("the_long_travail:name"));
    }

    @Override
    public List<Component> getSlotsTooltip(List<Component> tooltip, ItemStack stack) {
        return List.of();
    }

    @Override
    public List<Component> getAttributesTooltip(List<Component> tooltip, ItemStack stack) {
        return List.of();
    }

    @Override
    public List<Component> getTagsTooltip(List<Component> tooltip, ItemStack stack) {
        return List.of();
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.the_long_travail.slot",
                Component.translatable("gui.the_long_travail.diary.title").withStyle(ChatFormatting.YELLOW))
                .withStyle(ChatFormatting.GOLD));
        tooltip.add(Component.empty());
        for (int index = 0; index < 12; index++)
            tooltip.add(Component.translatable("tooltip.the_long_travail.dialogue." + index, Component.translatable("text.the_long_travail.traveler"))
                    .withStyle(style -> style.withColor(0xBC995E)));
        tooltip.add(Component.empty());
        tooltip.add(Component.translatable("tooltip.the_long_travail.open_diary",
                Component.keybind("key.the_long_travail.open_diary")
                        .withStyle(style -> style.withColor(com.thelongtravail.abyss.RainTooltips.ACCENT_COLOR)))
                .withStyle(style -> style.withColor(com.thelongtravail.abyss.RainTooltips.BODY_COLOR)));
    }

    public static Component flourishingProgress() {
        int count = JourneySync.biomeCount();
        if (count < 0) return Component.translatable("tooltip.the_long_travail.flourishing.progress_pending");
        double bonus = FlourishingBonus.calculate(count,
                d("flourishing.bonusPerBiome", TravailConfig.FLOURISHING_BONUS_PER_BIOME.get()),
                i("flourishing.maxHealingBonus", TravailConfig.MAX_HEALING_BONUS.get()));
        return Component.translatable("tooltip.the_long_travail.flourishing.progress", count, percent(bonus));
    }

    public static Component aspectEffect(TravailAspect aspect, boolean completed) {
        return switch (aspect) {
            case FLOURISHING -> completed
                    ? Component.translatable("tooltip.the_long_travail.flourishing.witness.effect",
                    percent(d("flourishing.bonusPerBiome", TravailConfig.FLOURISHING_BONUS_PER_BIOME.get())))
                    : Component.translatable("tooltip.the_long_travail.flourishing.malice.effect",
                    percent(d("flourishing.healingReduction", TravailConfig.FLOURISHING_HEALING_REDUCTION.get())),
                    percent(d("flourishing.healthThreshold", TravailConfig.FLOURISHING_HEALTH_THRESHOLD.get())),
                    number(d("flourishing.damageMultiplier", TravailConfig.FLOURISHING_DAMAGE_MULTIPLIER.get())));
            case ABYSS -> completed
                    ? Component.translatable("tooltip.the_long_travail.abyss.witness.effect",
                    percent(d("abyss.cancelChance", TravailConfig.ABYSS_CANCEL_CHANCE.get())))
                    : Component.translatable("tooltip.the_long_travail.abyss.malice.effect",
                    number(d("abyss.fluidDamage", TravailConfig.ABYSS_FLUID_DAMAGE.get())),
                    number(d("abyss.fluidInterval", TravailConfig.ABYSS_FLUID_INTERVAL_SECONDS.get())),
                    percent(d("abyss.miningReduction", TravailConfig.ABYSS_MINING_REDUCTION.get())),
                    number(d("abyss.environmentMultiplier", TravailConfig.ABYSS_ENVIRONMENT_MULTIPLIER.get())));
            case FAR_REACH -> completed
                    ? Component.translatable("tooltip.the_long_travail.far_reach.witness.effect",
                    i("farReach.levelBonus", TravailConfig.FAR_WITNESS_LEVEL_BONUS.get()),
                    number(d("farReach.interval", TravailConfig.FAR_WITNESS_INTERVAL_SECONDS.get())),
                    i("farReach.actionCount", TravailConfig.FAR_WITNESS_ACTION_COUNT.get()),
                    number(d("farReach.effectDuration", TravailConfig.FAR_WITNESS_POSITIVE_DURATION_SECONDS.get())))
                    : Component.translatable("tooltip.the_long_travail.far_reach.malice.effect",
                    percent(d("farReach.clearAllChance", TravailConfig.FAR_ALL_CLEAR_CHANCE.get())),
                    percent(d("farReach.attackClearChance", TravailConfig.FAR_ATTACK_CLEAR_CHANCE.get())),
                    number(d("farReach.attackCooldown", TravailConfig.FAR_ATTACK_COOLDOWN_SECONDS.get())));
            case DEEP_VALLEY -> completed
                    ? Component.translatable("tooltip.the_long_travail.deep_valley.witness.effect",
                    effectLevel(i("deepValley.swiftnessLevel", TravailConfig.VALLEY_SWIFTNESS_LEVEL.get())),
                    number(d("deepValley.swiftnessDuration", TravailConfig.VALLEY_SWIFTNESS_DURATION_SECONDS.get())),
                    i("deepValley.randomEffectCount", TravailConfig.VALLEY_RANDOM_EFFECT_COUNT.get()),
                    number(d("deepValley.randomEffectDuration", TravailConfig.VALLEY_RANDOM_EFFECT_DURATION_SECONDS.get())),
                    number(d("deepValley.witnessAttackCooldown", TravailConfig.VALLEY_WITNESS_ATTACK_COOLDOWN_SECONDS.get())))
                    : Component.translatable("tooltip.the_long_travail.deep_valley.malice.effect",
                    percent(d("deepValley.visualDeprivationChance", TravailConfig.VALLEY_VISUAL_DEPRIVATION_CHANCE.get())),
                    number(d("deepValley.visualDeprivationDuration", TravailConfig.VALLEY_VISUAL_DEPRIVATION_DURATION_SECONDS.get())),
                    percent(d("deepValley.stiffChance", TravailConfig.VALLEY_STIFF_CHANCE.get())),
                    number(d("deepValley.stiffDuration", TravailConfig.VALLEY_STIFF_DURATION_SECONDS.get())));
            case UNDERWORLD -> completed
                    ? Component.translatable("tooltip.the_long_travail.underworld.witness.effect",
                    i("underworld.anvilCap", TravailConfig.UNDERWORLD_ANVIL_LEVEL_CAP.get()),
                    i("underworld.fortuneBonus", TravailConfig.UNDERWORLD_FORTUNE_BONUS.get()),
                    i("underworld.lootingBonus", TravailConfig.UNDERWORLD_LOOTING_BONUS.get()),
                    percent(effectiveFishingChance(true)), percent(effectiveFishingChance(false)))
                    : Component.translatable("tooltip.the_long_travail.underworld.malice.effect",
                    percent(d("underworld.damageReduction", TravailConfig.UNDERWORLD_DAMAGE_REDUCTION.get())),
                    i("underworld.experienceCap", TravailConfig.UNDERWORLD_EXPERIENCE_LEVEL_CAP.get()));
            case BOUNDLESS -> completed
                    ? Component.translatable("tooltip.the_long_travail.boundless.witness.effect",
                    i("boundless.extraSlots", TravailConfig.BOUNDLESS_EXTRA_CURIO_SLOTS.get()),
                    i("boundless.phantomRange", TravailConfig.PHANTOM_RANGE.get()),
                    number(d("boundless.phantomInterval", TravailConfig.PHANTOM_CHECK_INTERVAL_SECONDS.get())))
                    : Component.translatable("tooltip.the_long_travail.boundless.malice.effect",
                    number(d("boundless.heightThreshold", TravailConfig.BOUNDLESS_HEIGHT_THRESHOLD.get())),
                    percent(d("boundless.speedReduction", TravailConfig.BOUNDLESS_SPEED_REDUCTION.get())));
        };
    }

    private static double effectiveFishingChance(boolean specialItem) {
        double itemChance = d("underworld.fishingItemChance", TravailConfig.FISH_SPECIAL_CHANCE.get());
        double entityChance = d("underworld.fishingEntityChance", TravailConfig.FISH_ENTITY_CHANCE.get());
        double total = itemChance + entityChance;
        if (total > 1.0D) return (specialItem ? itemChance : entityChance) / total;
        return specialItem ? itemChance : entityChance;
    }

    private static double d(String key, double fallback) {
        return TooltipConfigSync.decimal(key, fallback);
    }

    private static int i(String key, int fallback) {
        return TooltipConfigSync.integer(key, fallback);
    }

    private static String effectLevel(int level) {
        String[] roman = {"", "Ⅰ", "Ⅱ", "Ⅲ", "Ⅳ", "Ⅴ", "Ⅵ", "Ⅶ", "Ⅷ", "Ⅸ", "Ⅹ"};
        return level > 0 && level < roman.length ? roman[level] : Integer.toString(level);
    }

    private static String percent(double fraction) {
        return number(fraction * 100.0D) + "%";
    }

    private static String number(double value) {
        if (value == Math.rint(value)) return Long.toString((long) value);
        String formatted = String.format(Locale.ROOT, "%.4f", value);
        return formatted.replaceAll("0+$", "").replaceAll("\\.$", "");
    }
}
