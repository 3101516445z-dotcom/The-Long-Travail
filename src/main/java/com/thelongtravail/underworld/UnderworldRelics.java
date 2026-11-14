package com.thelongtravail.underworld;

import com.thelongtravail.TravailAspect;
import com.thelongtravail.api.TravailStateApi;
import com.thelongtravail.common.AttributeModifierSet;
import com.thelongtravail.config.UnderworldItemsConfig;
import com.thelongtravail.registry.ModAttributes;
import com.thelongtravail.registry.ModRegistry;
import com.thelongtravail.registry.RegisteredItems;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import top.theillusivec4.curios.api.CuriosApi;

import java.util.*;

@Mod.EventBusSubscriber(modid = "the_long_travail")
public final class UnderworldRelics {
    private static final AttributeModifierSet.Source SOURCE =
            new AttributeModifierSet.Source(
                    new ResourceLocation("the_long_travail", "underworld_relics"));
    private static final Map<LivingEntity, Map<Attribute, AttributeModifierSet.Modifier>> APPLIED =
            new WeakHashMap<>();

    public static int phase(LivingEntity e) {
        if (!e.isAlive() || e.isRemoved() || e.isSpectator()) return 0;
        if (e instanceof Player p)
            return switch (TravailStateApi.state(p, TravailAspect.UNDERWORLD)) {
                case MALICE -> 1;
                case WITNESS -> 2;
                default -> 0;
            };
        return com.thelongtravail.integration.MaidIntegration.relicPhase(e);
    }

    public static ItemStack equipped(LivingEntity e, boolean banner) {
        if (phase(e) == 0) return ItemStack.EMPTY;
        return wornStack(e, banner);
    }

    // 魂数积累只检查实际佩戴，属性加成仍由 equipped 的冥府状态控制。
    public static ItemStack soulBanner(LivingEntity e) {
        if (!e.isAlive() || e.isRemoved() || e.isSpectator()) return ItemStack.EMPTY;
        return wornStack(e, true);
    }

    private static ItemStack wornStack(LivingEntity e, boolean banner) {
        var reference = banner ? ModRegistry.SOUL_BANNER : ModRegistry.DEMON_KING;
        if (!reference.isPresent()) return ItemStack.EMPTY;
        var item = reference.get();
        if (!(e instanceof Player p))
            return com.thelongtravail.integration.MaidIntegration.relicStack(e, banner);
        return CuriosApi.getCuriosInventory(p)
                .map(
                        inv ->
                                inv.findCurios(item).stream()
                                        .filter(
                                                r ->
                                                        !r.slotContext().cosmetic()
                                                                && r.slotContext()
                                                                        .identifier()
                                                                        .equals(
                                                                                banner
                                                                                        ? "charm"
                                                                                        : "bracelet"))
                                        .map(r -> r.stack())
                                        .findFirst()
                                        .orElse(ItemStack.EMPTY))
                .orElse(ItemStack.EMPTY);
    }

    public static int souls(ItemStack stack) {
        if (!RegisteredItems.is(stack, ModRegistry.SOUL_BANNER) || !stack.hasTag()) return 0;
        var tag = stack.getTag().getCompound("the_long_travail:soul_banner");
        return Math.min(capacity(), Math.max(0, tag.getInt("Souls")));
    }

    public static int capacity() {
        return UnderworldItemsConfig.SOUL_CAPACITY.get();
    }

    public static boolean addSoul(ItemStack stack) {
        int n = souls(stack);
        if (n >= capacity()) return false;
        var tag = stack.getOrCreateTagElement("the_long_travail:soul_banner");
        tag.putInt("Souls", n + 1);
        return true;
    }

    public static void refresh(LivingEntity e) {
        if (e.level().isClientSide) return;
        // Modifier 是值对象；身份比较会把每次新建的等值修饰符误判为变化。
        Map<Attribute, AttributeModifierSet.Modifier> wanted = new HashMap<>();
        int phase = phase(e), n = souls(equipped(e, true));
        if (phase == 2 && !equipped(e, false).isEmpty())
            put(wanted, ModAttributes.OVERLORD.get(), value("king.overlord"));
        if (n > 0 && phase != 0) {
            put(
                    wanted,
                    ModAttributes.CRITICAL_CHANCE.get(),
                    n
                            * value(
                                    phase == 1
                                            ? "banner.maliceCriticalPerSoul"
                                            : "banner.witnessCriticalPerSoul"));
            if (phase == 2)
                put(
                        wanted,
                        ModAttributes.INSIGHT.get(),
                        Math.min(value("banner.insightCap"), n * value("banner.insightPerSoul")));
        }
        if (wanted.isEmpty() && !APPLIED.containsKey(e)) return;
        if (!wanted.equals(APPLIED.get(e)) || e.tickCount % 20 == 0 && !wanted.isEmpty()) {
            AttributeModifierSet.reconcile(e, SOURCE, wanted);
            if (wanted.isEmpty()) APPLIED.remove(e);
            else APPLIED.put(e, wanted);
        }
    }

    private static void put(
            Map<Attribute, AttributeModifierSet.Modifier> out, Attribute a, double amount) {
        if (amount > 0)
            out.put(
                    a,
                    new AttributeModifierSet.Modifier(
                            amount, AttributeModifier.Operation.ADDITION));
    }

    public static double value(String key) {
        return UnderworldItemsConfig.RELICS.get(key).get();
    }

    @SubscribeEvent
    public static void tick(LivingEvent.LivingTickEvent event) {
        var e = event.getEntity();
        if (!e.level().isClientSide
                && (e instanceof Player
                        || com.thelongtravail.integration.MaidIntegration.isMaid(e))) refresh(e);
    }

    @SubscribeEvent
    public static void death(net.minecraftforge.event.entity.living.LivingDeathEvent event) {
        var e = event.getEntity();
        if (!e.level().isClientSide && APPLIED.containsKey(e)) {
            AttributeModifierSet.clear(e, SOURCE);
            APPLIED.remove(e);
        }
    }

    @SubscribeEvent
    public static void stop(net.minecraftforge.event.server.ServerStoppedEvent event) {
        APPLIED.clear();
    }

    private UnderworldRelics() {}
}
