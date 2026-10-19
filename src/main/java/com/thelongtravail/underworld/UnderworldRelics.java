package com.thelongtravail.underworld;

import com.thelongtravail.TravailAspect;
import com.thelongtravail.api.TravailStateApi;
import com.thelongtravail.common.AttributeModifierSet;
import com.thelongtravail.config.UnderworldItemsConfig;
import com.thelongtravail.helper.AccessoryEquipRules;
import com.thelongtravail.registry.ModRegistry;
import com.thelongtravail.registry.ModAttributes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import top.theillusivec4.curios.api.CuriosApi;
import java.util.*;

@Mod.EventBusSubscriber(modid = "the_long_travail")
public final class UnderworldRelics {
    private static final ResourceLocation SOURCE = new ResourceLocation("the_long_travail", "underworld_relics");
    private static final Map<LivingEntity, Map<Attribute, AttributeModifierSet.Modifier>> APPLIED = new WeakHashMap<>();

    public static int phase(LivingEntity e) {
        if (!e.isAlive() || e.isRemoved() || e.isSpectator()) return 0;
        if (e instanceof Player p)
            return switch (TravailStateApi.state(p, TravailAspect.UNDERWORLD)) {
                case MALICE -> 1;
                case WITNESS -> 2;
                default -> 0;
            };
        return com.thelongtravail.compat.maid.MaidAccessories.relicPhase(e);
    }

    public static ItemStack equipped(LivingEntity e, boolean banner) {
        if (phase(e) == 0) return ItemStack.EMPTY;
        var item = banner ? ModRegistry.SOUL_BANNER.get() : ModRegistry.DEMON_KING.get();
        if (!(e instanceof Player p)) return com.thelongtravail.compat.maid.MaidAccessories.relicStack(e, banner);
        return CuriosApi.getCuriosInventory(p)
                .map(
                        inv ->
                                inv.findCurios(item).stream()
                                        .filter(
                                                r ->
                                                        !r.slotContext().cosmetic()
                                                                && r.slotContext()
                                                                        .identifier()
                                                                        .equals(banner ? "charm" : "bracelet"))
                                        .map(r -> r.stack())
                                        .findFirst()
                                        .orElse(ItemStack.EMPTY))
                .orElse(ItemStack.EMPTY);
    }

    public static int souls(ItemStack stack) {
        if (!stack.is(ModRegistry.SOUL_BANNER.get()) || !stack.hasTag()) return 0;
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
        Map<Attribute, AttributeModifierSet.Modifier> wanted = new IdentityHashMap<>();
        int phase = phase(e), n = souls(equipped(e, true));
        if (phase == 2 && !equipped(e, false).isEmpty())
            put(wanted, ModAttributes.OVERLORD.get(), value("king.overlord"));
        if (n > 0 && phase != 0) {
            put(
                    wanted,
                    ModAttributes.CRITICAL_CHANCE.get(),
                    n * value(phase == 1 ? "banner.maliceCriticalPerSoul" : "banner.witnessCriticalPerSoul"));
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

    private static void put(Map<Attribute, AttributeModifierSet.Modifier> out, Attribute a, double amount) {
        if (amount > 0) out.put(a, new AttributeModifierSet.Modifier(amount, AttributeModifier.Operation.ADDITION));
    }

    public static double value(String key) {
        return UnderworldItemsConfig.RELICS.get(key).get();
    }

    @SubscribeEvent
    public static void tick(LivingEvent.LivingTickEvent event) {
        var e = event.getEntity();
        if (!e.level().isClientSide
                && (e instanceof Player || com.thelongtravail.compat.maid.MaidAccessories.isMaid(e))) refresh(e);
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
