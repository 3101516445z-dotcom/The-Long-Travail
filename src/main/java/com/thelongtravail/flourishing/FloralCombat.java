package com.thelongtravail.flourishing;

import com.thelongtravail.config.FlourishingItemsConfig;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.item.ItemStack;
import java.util.*;

public final class FloralCombat {
    public static final ResourceKey<DamageType> ROSE=ResourceKey.create(Registries.DAMAGE_TYPE,new ResourceLocation("the_long_travail","rose_retaliation"));
    private static final class Transfer {
        final LivingEntity target; final DamageSource source; final double bonus; boolean claimed;
        Transfer(LivingEntity target,DamageSource source,double bonus){this.target=target;this.source=source;this.bonus=bonus;}
    }
    private static final ThreadLocal<Transfer> TRANSFER=new ThreadLocal<>();
    // 每次分担只认领一个 hurt 上下文，事件中的嵌套攻击保持独立。
    public static Double claimTransfer(LivingEntity target,DamageSource source){
        Transfer frame=TRANSFER.get();
        if(frame==null||frame.claimed||frame.target!=target||frame.source!=source)return null;
        frame.claimed=true;return frame.bonus;
    }
    public static boolean transfer(LivingEntity target,DamageSource source){
        var context=com.thelongtravail.helper.CombatContext.current(target,source);
        return context!=null&&context.isFloralTransfer();
    }
    public static boolean rose(DamageSource s){return s.is(ROSE);}
    public static float split(LivingEntity target,DamageSource source,float amount) {
        if (com.thelongtravail.valley.AzraelExecution.matches(target, source)) return amount;
        if(transfer(target,source)||rose(source)||!(target instanceof ServerPlayer p)||amount<=0||!FlourishingItemsConfig.SHARING.get()||source.is(DamageTypes.GENERIC_KILL))return amount;
        String id=source.typeHolder().unwrapKey().map(k->k.location().toString()).orElse("");
        if(FlourishingItemsConfig.EXCLUSIONS.get().contains(id))return amount;
        LivingEntity servant=Affection.target(p);if(servant==null)return amount;
        double part=FlourishingItemsConfig.get("affection.shareFraction");
        float shared=safe(amount*part*FlourishingItemsConfig.get("affection.servantShareMultiplier"));
        if(shared>0){
            Transfer old=TRANSFER.get();
            var context=com.thelongtravail.helper.CombatContext.current(target,source);
            TRANSFER.set(new Transfer(servant,source,context==null?0:context.transferableBonus()));
            try{servant.hurt(source,shared);}finally{
                if(old==null)TRANSFER.remove();else TRANSFER.set(old);
            }
        }
        return safe(amount*(1-part));
    }
    public static double bonus(LivingEntity victim,DamageSource source) {
        if(rose(source))return 0;
        Entity direct=source.getEntity();
        ServerPlayer player=Affection.attacker(direct);
        if(player==null)player=Affection.attacker(source.getDirectEntity());
        double result=0;
        if(player!=null&&player!=victim) {
            ItemStack crown=Affection.equipped(player,true);
            result+=value(crown,Flower.POPPY);
            if(player.getHealth()<player.getMaxHealth()*FlourishingItemsConfig.extra(Flower.RED_TULIP,"healthThreshold"))result+=value(crown,Flower.RED_TULIP);
            if(source.is(DamageTypeTags.IS_PROJECTILE))result+=value(crown,Flower.CORNFLOWER);
            if(sunny(player))result+=value(crown,Flower.SUNFLOWER);
            if(direct==player){LivingEntity pet=Affection.target(player);if(pet!=null)result+=FlourishingItemsConfig.get("affection.playerDamageBonus")*Affection.lost(pet);}
        }
        if(direct instanceof LivingEntity living&&!(living instanceof ServerPlayer)) {
            ServerPlayer master=Affection.master(living);if(master!=null)result+=FlourishingItemsConfig.get("affection.servantDamageBonus")*Affection.lost(master);
        }
        return result;
    }
    public static float reduce(LivingEntity victim,DamageSource source,float amount) {
        if(rose(source))return amount;
        double multiplier=1;
        if(victim instanceof ServerPlayer p) {
            ItemStack crown=Affection.equipped(p,true);
            var context=com.thelongtravail.helper.CombatContext.current(victim,source);
            if(context!=null&&context.isFloralPoison())multiplier*=1-value(crown,Flower.ALLIUM);
            if(source.is(DamageTypeTags.IS_FALL))multiplier*=1-value(crown,Flower.AZURE_BLUET);
            if(source.is(DamageTypeTags.IS_FIRE))multiplier*=1-value(crown,Flower.TORCHFLOWER);
            if(p.getHealth()<p.getMaxHealth()*FlourishingItemsConfig.extra(Flower.WHITE_TULIP,"healthThreshold"))multiplier*=1-value(crown,Flower.WHITE_TULIP);
        } else {ServerPlayer master=Affection.master(victim);if(master!=null)multiplier*=1-FlourishingItemsConfig.get("affection.servantDamageReduction")*Affection.lost(master);}
        return safe(amount*multiplier);
    }
    public static void roseHit(ServerPlayer player,LivingEntity attacker) {
        DamageSource source=new DamageSource(player.serverLevel().registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(ROSE),player);
        attacker.hurt(source,safe(player.getMaxHealth()*FlourishingItemsConfig.value(Flower.ROSE_BUSH)));
    }
    public static double value(ItemStack crown,Flower f){return FlowerData.active(crown,f)?FlourishingItemsConfig.value(f):0;}
    public static boolean sunny(ServerPlayer p){return p.level().isDay()&&p.level().canSeeSky(p.blockPosition());}
    public static float safe(double amount){return (float)Math.max(0,Math.min(Float.MAX_VALUE,amount));}
    private FloralCombat(){}
}
