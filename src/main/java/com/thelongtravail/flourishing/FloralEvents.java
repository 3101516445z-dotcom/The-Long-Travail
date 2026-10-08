package com.thelongtravail.flourishing;

import com.thelongtravail.TheLongTravail;
import com.thelongtravail.config.FlourishingItemsConfig;
import com.thelongtravail.mixin.EffectDurationAccessor;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.effect.*;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraftforge.common.ForgeMod;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.*;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import java.util.*;

@Mod.EventBusSubscriber(modid=TheLongTravail.MODID)
public final class FloralEvents {
    @SubscribeEvent(priority=net.minecraftforge.eventbus.api.EventPriority.HIGHEST)
    public static void interact(net.minecraftforge.event.entity.player.PlayerInteractEvent.EntityInteract e) {
        if(e.getItemStack().is(com.thelongtravail.registry.ModRegistry.AFFECTION.get())&&e.getTarget() instanceof LivingEntity target) {
            e.setCancellationResult(Affection.bind(e.getItemStack(),e.getEntity(),target));e.setCanceled(true);
        }
    }
    private static final UUID HEALTH=UUID.fromString("52bb64ee-8834-4703-9e01-43f2f7aac3d1"),ATTACK=UUID.fromString("52bb64ee-8834-4703-9e01-43f2f7aac3d2"),SPEED=UUID.fromString("52bb64ee-8834-4703-9e01-43f2f7aac3d3"),SWIM=UUID.fromString("52bb64ee-8834-4703-9e01-43f2f7aac3d4");
    private static final Map<ServerPlayer,State> STATES=new WeakHashMap<>();
    private static final Map<LivingEntity,Map<String,Long>> TARGET_COOLDOWNS=new WeakHashMap<>();
    private record PendingDeath(LivingEntity victim,ServerPlayer killer,double healing,LivingDeathEvent event) {}
    private static final Map<LivingEntity,PendingDeath> PENDING_DEATHS=new IdentityHashMap<>();
    public static final class PoisonScope {
        final LivingEntity target; boolean claimed;
        PoisonScope(LivingEntity target){this.target=target;}
    }
    private static final ThreadLocal<PoisonScope> POISON=new ThreadLocal<>();
    private static final class State {int heal;long rose=Long.MIN_VALUE,kill=Long.MIN_VALUE;}
    public static boolean claimPoison(LivingEntity target,DamageSource source){
        PoisonScope scope=POISON.get();
        if(scope==null||scope.claimed||scope.target!=target||!source.is(net.minecraft.world.damagesource.DamageTypes.MAGIC))return false;
        scope.claimed=true;return true;
    }
    public static PoisonScope poisonScope(LivingEntity target){PoisonScope old=POISON.get();if(target==null)POISON.remove();else POISON.set(new PoisonScope(target));return old;}
    public static void restorePoison(PoisonScope old){if(old!=null)POISON.set(old);else POISON.remove();}
    @SubscribeEvent public static void tick(TickEvent.PlayerTickEvent e) {
        if(e.phase!=TickEvent.Phase.END||!(e.player instanceof ServerPlayer p))return;
        ItemStack crown=p.isAlive()&&!p.isSpectator()?Affection.equipped(p,true):ItemStack.EMPTY;
        State s=STATES.computeIfAbsent(p,k->new State());
        attribute(p,Attributes.MAX_HEALTH,HEALTH,FloralCombat.value(crown,Flower.PEONY));
        attribute(p,Attributes.ATTACK_SPEED,ATTACK,FloralCombat.value(crown,Flower.ORANGE_TULIP));
        attribute(p,ForgeMod.SWIM_SPEED.get(),SWIM,FloralCombat.value(crown,Flower.BLUE_ORCHID));
        double speed=FloralCombat.value(crown,Flower.PINK_PETALS);
        if(FlowerData.active(crown,Flower.SUNFLOWER)&&FloralCombat.sunny(p))speed+=FlourishingItemsConfig.extra(Flower.SUNFLOWER,"speedBonus");
        LivingEntity pet=p.isSpectator()?null:Affection.target(p);
        if(pet!=null)speed+=FlourishingItemsConfig.get("affection.playerSpeedBonus")*Affection.lost(pet);
        attribute(p,Attributes.MOVEMENT_SPEED,SPEED,speed);
        if(FlowerData.active(crown,Flower.OXEYE_DAISY)) {
            if(++s.heal>=FlourishingItemsConfig.ticks(FlourishingItemsConfig.extra(Flower.OXEYE_DAISY,"intervalSeconds"))) {s.heal=0;if(p.getHealth()<p.getMaxHealth())p.heal((float)FlourishingItemsConfig.value(Flower.OXEYE_DAISY));}
        }else s.heal=0;
    }
    private static void attribute(ServerPlayer p,Attribute a,UUID id,double value) {
        AttributeInstance instance=p.getAttribute(a);if(instance==null)return;
        AttributeModifier old=instance.getModifier(id);
        if(old!=null&&Double.compare(old.getAmount(),value)==0)return;
        if(old!=null)instance.removeModifier(id);
        if(value>0)instance.addTransientModifier(new AttributeModifier(id,"Flourishing accessory",value,AttributeModifier.Operation.MULTIPLY_TOTAL));
        if(a==Attributes.MAX_HEALTH&&old!=null&&p.getHealth()>p.getMaxHealth())p.setHealth(p.getMaxHealth());
    }
    @SubscribeEvent public static void heal(LivingHealEvent e) {
        if(e.getEntity() instanceof ServerPlayer p&&e.getAmount()>0)e.setAmount(FloralCombat.safe(e.getAmount()*(1+FloralCombat.value(Affection.equipped(p,true),Flower.PINK_TULIP))));
    }
    public static void successful(LivingEntity victim,DamageSource source,boolean transfer) {
        if(transfer||FloralCombat.rose(source))return;
        ServerPlayer p=Affection.attacker(source.getEntity());if(p==null)p=Affection.attacker(source.getDirectEntity());
        if(p!=null&&p!=victim) {
            ItemStack crown=Affection.equipped(p,true);
            apply(p,victim,crown,Flower.LILY_OF_THE_VALLEY,MobEffects.POISON);
            apply(p,victim,crown,Flower.WITHER_ROSE,MobEffects.WITHER);
        }
        if(victim instanceof ServerPlayer player&&source.getDirectEntity() instanceof LivingEntity attacker&&attacker!=player&&!source.is(net.minecraft.world.damagesource.DamageTypes.THORNS)&&!source.is(DamageTypeTags.IS_PROJECTILE)&&!source.is(DamageTypeTags.IS_EXPLOSION)) {
            ItemStack crown=Affection.equipped(player,true);State state=STATES.computeIfAbsent(player,k->new State());long now=player.level().getGameTime();
            if(FlowerData.active(crown,Flower.ROSE_BUSH)&&ready(now,state.rose,Flower.ROSE_BUSH)) {state.rose=now;FloralCombat.roseHit(player,attacker);}
        }
    }
    private static boolean ready(long now,long last,Flower f) {return last==Long.MIN_VALUE||now-last>=FlourishingItemsConfig.ticks(FlourishingItemsConfig.extra(f,"cooldownSeconds"));}
    private static void apply(ServerPlayer p,LivingEntity victim,ItemStack crown,Flower f,MobEffect effect) {
        if(!FlowerData.active(crown,f))return;
        long now=p.level().getGameTime();var cooldowns=TARGET_COOLDOWNS.computeIfAbsent(victim,k->new HashMap<>());
        cooldowns.entrySet().removeIf(e->e.getValue()<=now);
        String key=p.getUUID()+":"+f.id;if(cooldowns.containsKey(key))return;
        if(cooldowns.size()>=256)return;
        cooldowns.put(key,now+FlourishingItemsConfig.ticks(FlourishingItemsConfig.extra(f,"cooldownSeconds")));
        victim.addEffect(new MobEffectInstance(effect,FlourishingItemsConfig.ticks(FlourishingItemsConfig.extra(f,"durationSeconds")),(int)FlourishingItemsConfig.value(f)-1),p);
    }
    public static MobEffectInstance extendedEffect(LivingEntity target,MobEffectInstance incoming,Entity source) {
        if(target.level().isClientSide)return incoming;
        ServerPlayer p=Affection.attacker(source);if(p==null)return incoming;
        var effect=incoming.getEffect();
        if(effect!=MobEffects.POISON&&effect!=MobEffects.WITHER&&effect!=MobEffects.MOVEMENT_SLOWDOWN&&effect!=MobEffects.WEAKNESS)return incoming;
        double bonus=FloralCombat.value(Affection.equipped(p,true),Flower.LILAC);
        if(bonus<=0||incoming.isInfiniteDuration())return incoming;
        // 外部法术可能向多个目标复用同一实例，不能改写调用者的数据。
        var copy=new MobEffectInstance(incoming);
        ((EffectDurationAccessor)copy).travail$duration((int)Math.min(Integer.MAX_VALUE,Math.ceil(incoming.getDuration()*(1+bonus))));
        return copy;
    }
    @SubscribeEvent public static void death(LivingDeathEvent e) {
        if(e.getEntity().level().isClientSide)return;
        ServerPlayer p=Affection.attacker(e.getSource().getEntity());
        if(p==null)p=Affection.attacker(e.getSource().getDirectEntity());
        double healing=p!=null&&e.getEntity() instanceof Enemy&&!FloralCombat.transfer(e.getEntity(),e.getSource())
                ?FloralCombat.value(Affection.equipped(p,true),Flower.PITCHER_PLANT):0;
        PENDING_DEATHS.put(e.getEntity(),new PendingDeath(e.getEntity(),p,healing,e));
    }
    @SubscribeEvent public static void logout(PlayerEvent.PlayerLoggedOutEvent e){if(e.getEntity() instanceof ServerPlayer p){STATES.remove(p);Affection.reset(p);}}
    @SubscribeEvent public static void endTick(TickEvent.ServerTickEvent e) {
        if(e.phase!=TickEvent.Phase.END)return;
        var pending=new ArrayList<>(PENDING_DEATHS.values());PENDING_DEATHS.clear();
        for(PendingDeath entry:pending)if(!entry.event().isCanceled()&&!entry.victim().isAlive()) {
            LivingEntity dead=entry.victim();ServerPlayer p=entry.killer();
            if(p!=null&&p.isAlive()&&entry.healing()>0) {
                State state=STATES.computeIfAbsent(p,k->new State());long now=p.level().getGameTime();
                if(ready(now,state.kill,Flower.PITCHER_PLANT)) {state.kill=now;p.heal(FloralCombat.safe(p.getMaxHealth()*entry.healing()));}
            }
            if(!AffectionDeaths.get(dead.getServer()).died(dead.getUUID()))continue;
            for(ServerPlayer holder:dead.getServer().getPlayerList().getPlayers()) {
            java.util.function.Consumer<ItemStack> clear=s->{if(s.is(com.thelongtravail.registry.ModRegistry.AFFECTION.get())&&s.hasTag()&&s.getTag().hasUUID(Affection.TARGET)&&s.getTag().getUUID(Affection.TARGET).equals(dead.getUUID()))Affection.clear(s);};
            for(int i=0;i<holder.getInventory().getContainerSize();i++)clear.accept(holder.getInventory().getItem(i));
            for(int i=0;i<holder.getEnderChestInventory().getContainerSize();i++)clear.accept(holder.getEnderChestInventory().getItem(i));
            top.theillusivec4.curios.api.CuriosApi.getCuriosInventory(holder).ifPresent(inv->inv.findCurios(com.thelongtravail.registry.ModRegistry.AFFECTION.get()).forEach(r->clear.accept(r.stack())));
            }
        }
    }
    @SubscribeEvent public static void stop(net.minecraftforge.event.server.ServerStoppedEvent e){STATES.clear();TARGET_COOLDOWNS.clear();PENDING_DEATHS.clear();Affection.resetAll();}
    public static void food(net.minecraft.world.food.FoodData data,ItemStack stack,LivingEntity entity,Runnable original) {
        if(!(entity instanceof ServerPlayer p)||!FlowerData.active(Affection.equipped(p,true),Flower.DANDELION)){original.run();return;}
        var props=stack.getFoodProperties(entity);if(props==null){original.run();return;}
        double stored=p.getPersistentData().getDouble("TravailFoodFraction")+props.getNutrition()*FlourishingItemsConfig.value(Flower.DANDELION);
        int bonus=(int)Math.min(1000000,Math.floor(stored));p.getPersistentData().putDouble("TravailFoodFraction",stored-Math.floor(stored));
        int food=props.getNutrition()+bonus;
        float saturation=(float)(props.getNutrition()*props.getSaturationModifier()*2*(1+FlourishingItemsConfig.extra(Flower.DANDELION,"saturationBonus")));
        if(food>0)data.eat(food,saturation/(2*food));
        else if(saturation>0)data.setSaturation(Math.min(data.getFoodLevel(),data.getSaturationLevel()+saturation));
    }
}
