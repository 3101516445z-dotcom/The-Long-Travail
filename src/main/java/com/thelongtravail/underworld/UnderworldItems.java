package com.thelongtravail.underworld;

import com.thelongtravail.TravailAspect;
import com.thelongtravail.config.UnderworldItemsConfig;
import com.thelongtravail.data.LongTravailData;
import com.thelongtravail.helper.TravailCurios;
import com.thelongtravail.registry.ModRegistry;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import top.theillusivec4.curios.api.CuriosApi;

public final class UnderworldItems {
    public static ItemStack equipped(Player p, boolean ring) {
        if (TravailCurios.stack(p).isEmpty()) return ItemStack.EMPTY;
        return CuriosApi.getCuriosInventory(p).map(inv -> inv.findCurios(ring?ModRegistry.THOUSAND_YEARS.get():ModRegistry.BOOK_OF_DEAD.get()).stream()
                .filter(r -> !r.slotContext().cosmetic() && r.slotContext().identifier().equals(ring?"ring":"charm") && !consumed(p,r.stack()))
                .map(r -> r.stack()).findFirst().orElse(ItemStack.EMPTY)).orElse(ItemStack.EMPTY);
    }
    public static boolean consumed(Player p, ItemStack s) {
        return s.hasTag()&&s.getTag().hasUUID(UnderworldLedger.BOOK_ID)&&p instanceof ServerPlayer sp && UnderworldLedger.get(sp.server).consumed(s);
    }
    public static boolean seesUndead(LivingEntity attacker) {
        return attacker instanceof Player p && !equipped(p,true).isEmpty()
                && LongTravailData.hasWitness(TravailCurios.stack(p),TravailAspect.UNDERWORLD);
    }
    public static MobType attackType(LivingEntity attacker, LivingEntity target) {
        return attacker!=target && seesUndead(attacker)?MobType.UNDEAD:target.getMobType();
    }
    public static double bonus(LivingEntity target, DamageSource source) {
        if (!(source.getEntity() instanceof ServerPlayer p) || equipped(p,true).isEmpty()
                || LongTravailData.hasWitness(TravailCurios.stack(p),TravailAspect.UNDERWORLD)) return 0;
        var effect=target.getEffect(MobEffects.WITHER);
        return effect==null?0:((double)effect.getAmplifier()+1)*UnderworldItemsConfig.WITHER_BONUS.get();
    }
    private UnderworldItems() {}
}
