package travail.smoke;

import com.mojang.authlib.GameProfile;
import com.thelongtravail.config.FlourishingItemsConfig;
import com.thelongtravail.flourishing.*;
import com.thelongtravail.registry.ModRegistry;
import net.minecraft.server.level.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraftforge.common.util.FakePlayer;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.common.inventory.CurioStacksHandler;
import java.util.*;

public final class FloralItemsSmoke {
    private static class Player extends FakePlayer {
        Player(ServerLevel level){super(level,new GameProfile(UUID.randomUUID(),"FloralSmoke"));
            var wire=new net.minecraft.network.Connection(net.minecraft.network.protocol.PacketFlow.SERVERBOUND){@Override public void send(net.minecraft.network.protocol.Packet<?> p){}};
            connection=new net.minecraft.server.network.ServerGamePacketListenerImpl(level.getServer(),wire,this){@Override public void send(net.minecraft.network.protocol.Packet<?> p){}};
        }
        @Override public boolean isInvulnerableTo(DamageSource s){return false;}
    }
    private static void check(boolean b,String m){if(!b)throw new AssertionError("Floral: "+m);}
    private static void near(double actual,double expected,String m){check(Math.abs(actual-expected)<.002,m+" actual="+actual+" expected="+expected);}
    private static void health(ServerPlayer p,float value)throws Exception{var f=ServerPlayer.class.getDeclaredField("spawnInvulnerableTime");f.setAccessible(true);f.setInt(p,0);p.invulnerableTime=0;p.setHealth(value);p.setAbsorptionAmount(0);}
    private static void equip(Player p,ItemStack head,ItemStack body){
        var inv=CuriosApi.getCuriosInventory(p).resolve().orElseThrow();var map=new HashMap<String,top.theillusivec4.curios.api.type.inventory.ICurioStacksHandler>();
        for(String name:List.of("head","body")){var h=new CurioStacksHandler(inv,name,1,true,false,true,top.theillusivec4.curios.api.type.capability.ICurio.DropRule.DEFAULT);map.put(name,h);h.getStacks().setStackInSlot(0,name.equals("head")?head:body);}inv.setCurios(map);Affection.reset(p);
    }
    private static CraftingContainer grid(int width,int height){return new TransientCraftingContainer(new AbstractContainerMenu(null,0){@Override public ItemStack quickMoveStack(net.minecraft.world.entity.player.Player p,int i){return ItemStack.EMPTY;}@Override public boolean stillValid(net.minecraft.world.entity.player.Player p){return true;}},width,height);}
    public static void run(ServerLevel level)throws Exception {
        CraftingContainer c=grid(3,3);for(int i:new int[]{0,1,2,6,8})c.setItem(i,new ItemStack(Items.WHEAT));c.setItem(3,new ItemStack(Items.STRING));c.setItem(5,new ItemStack(Items.STRING));c.setItem(4,new ItemStack(Items.CORNFLOWER));
        var recipe=level.getRecipeManager().getRecipeFor(RecipeType.CRAFTING,c,level).orElseThrow();
        ItemStack crown=recipe.assemble(c,level.registryAccess());check(FlowerData.read(crown).equals(List.of(Flower.CORNFLOWER)),"recipe records primary");
        var change=new SpringRecipes.Change(new net.minecraft.resources.ResourceLocation("travail_smoke","change"),CraftingBookCategory.EQUIPMENT);
        var pair=grid(2,2);pair.setItem(0,crown);pair.setItem(1,new ItemStack(Items.CORNFLOWER));check(!change.matches(pair,level),"duplicate primary rejected");
        pair.setItem(1,new ItemStack(Items.ROSE_BUSH));check(change.matches(pair,level),"secondary accepted");crown=change.assemble(pair,level.registryAccess());
        pair.setItem(0,crown);pair.setItem(1,new ItemStack(Items.PEONY));crown=change.assemble(pair,level.registryAccess());check(FlowerData.read(crown).size()==3,"three flowers");
        pair.setItem(0,crown);pair.setItem(1,new ItemStack(Items.POPPY));check(!change.matches(pair,level),"fourth rejected");
        FlourishingItemsConfig.ENABLED.get(Flower.CORNFLOWER).set(false);check(!recipe.matches(c,level),"disabled primary recipe rejected");check(FlowerData.read(crown).get(0)==Flower.CORNFLOWER,"disabled primary retained");
        pair.setItem(1,new ItemStack(Items.WATER_BUCKET));check(change.matches(pair,level),"disabled primary can be washed");var washed=change.assemble(pair,level.registryAccess());check(FlowerData.read(washed).equals(List.of(Flower.CORNFLOWER)),"wash preserves primary");check(change.getRemainingItems(pair).get(1).is(Items.BUCKET),"wash returns empty bucket");
        FlourishingItemsConfig.ENABLED.get(Flower.CORNFLOWER).set(true);
        ItemStack blank=new ItemStack(ModRegistry.SPRING_GAME.get());blank.setHoverName(net.minecraft.network.chat.Component.literal("Blank crown"));blank.getOrCreateTag().putString("SmokeCustomData","preserved");
        for(Flower primary:Flower.values()) {
            check(!FlowerData.active(blank,primary),"blank crown has no flower effects");
            pair.setItem(0,blank);pair.setItem(1,new ItemStack(primary.item()));
            check(change.matches(pair,level),"blank selects "+primary.id);
            var selected=change.assemble(pair,level.registryAccess());check(FlowerData.read(selected).equals(List.of(primary)),"first flower becomes primary "+primary.id);
            check(selected.getHoverName().getString().equals("Blank crown")&&selected.getTag().getString("SmokeCustomData").equals("preserved"),"selection preserves custom NBT");
            check(FlowerData.read(blank).isEmpty(),"selection does not mutate ingredient");
            pair.setItem(0,selected);check(!change.matches(pair,level),"selected primary cannot be duplicated");
            pair.setItem(1,new ItemStack(Items.WATER_BUCKET));check(!change.matches(pair,level)&&change.assemble(pair,level.registryAccess()).isEmpty(),"water cannot erase only primary");
        }
        pair.setItem(0,blank);pair.setItem(1,new ItemStack(Items.WATER_BUCKET));check(!change.matches(pair,level)&&change.assemble(pair,level.registryAccess()).isEmpty(),"blank cannot be washed");
        pair.setItem(1,new ItemStack(Items.POPPY));FlourishingItemsConfig.ENABLED.get(Flower.POPPY).set(false);
        try{check(!change.matches(pair,level),"disabled flower cannot become initial primary");}finally{FlourishingItemsConfig.ENABLED.get(Flower.POPPY).set(true);}
        System.out.println("FLORAL_UNSELECTED_PASS: all 20 primary choices, no effects, disabled flower, no primary reset, NBT preservation");
        Player p=new Player(level);p.setPos(level.getSharedSpawnPos().getX(),100,level.getSharedSpawnPos().getZ());
        Wolf pet=new Wolf(EntityType.WOLF,level);pet.setTame(true);pet.setOwnerUUID(p.getUUID());pet.setPos(p.position());pet.getAttribute(Attributes.MAX_HEALTH).setBaseValue(20);pet.setHealth(20);level.getChunkAt(p.blockPosition());level.addFreshEntity(pet);
        var mapField=net.minecraft.server.players.PlayerList.class.getDeclaredField("playersByUUID");mapField.setAccessible(true);
        var playerMap=(Map<UUID,ServerPlayer>)mapField.get(level.getServer().getPlayerList());playerMap.put(p.getUUID(),p);
        try {
            ItemStack heart=new ItemStack(ModRegistry.AFFECTION.get());Affection.bind(heart,p,pet);check(heart.getTag().getUUID(Affection.OWNER).equals(p.getUUID()),"owner UUID captured");
            equip(p,ItemStack.EMPTY,heart);health(p,20);check(Affection.target(p)==pet,"active bound target");
            p.hurt(p.damageSources().generic(),8);near(p.getHealth(),16,"player raw half");near(pet.getHealth(),18,"companion raw quarter");
            health(p,20);pet.setHealth(20);pet.invulnerableTime=0;p.getAttribute(Attributes.ARMOR).setBaseValue(10);pet.getAttribute(Attributes.ARMOR).setBaseValue(20);
            p.hurt(p.damageSources().cactus(),8);
            near(p.getHealth(),20-net.minecraft.world.damagesource.CombatRules.getDamageAfterAbsorb(4,10,0),"player independently applies armor");
            near(pet.getHealth(),20-net.minecraft.world.damagesource.CombatRules.getDamageAfterAbsorb(2,20,0),"companion independently applies armor");
            p.getAttribute(Attributes.ARMOR).setBaseValue(0);pet.getAttribute(Attributes.ARMOR).setBaseValue(0);
            pet.invulnerableTime=0;pet.setHealth(20);health(p,10);pet.hurt(p.damageSources().generic(),10);near(pet.getHealth(),13.5,"companion reduction at half owner health");
            Zombie z=new Zombie(EntityType.ZOMBIE,level);z.setPos(p.position());z.getAttribute(Attributes.ARMOR).setBaseValue(0);z.setHealth(20);
            z.hurt(p.damageSources().mobAttack(pet),4);near(z.getHealth(),13,"companion +75 percent damage");
            pet.setHealth(10);health(p,20);z.invulnerableTime=0;z.setHealth(20);z.hurt(p.damageSources().playerAttack(p),4);near(z.getHealth(),15,"owner +25 percent damage");
            FloralEvents.tick(new net.minecraftforge.event.TickEvent.PlayerTickEvent(net.minecraftforge.event.TickEvent.Phase.END,p));
            near(p.getAttributeValue(Attributes.MOVEMENT_SPEED),p.getAttribute(Attributes.MOVEMENT_SPEED).getBaseValue()*1.375,"half-health companion speed bonus");
            pet.setPos(p.getX()+33,p.getY(),p.getZ());check(Affection.target(p)==null,"range boundary");pet.setPos(p.position());
            ItemStack rose=SpringRecipes.crown(Flower.ROSE_BUSH);equip(p,rose,ItemStack.EMPTY);health(p,20);z.setHealth(20);z.setAbsorptionAmount(5);z.getAttribute(Attributes.ARMOR).setBaseValue(30);z.invulnerableTime=0;
            FloralEvents.tick(new net.minecraftforge.event.TickEvent.PlayerTickEvent(net.minecraftforge.event.TickEvent.Phase.END,p));
            near(p.getAttributeValue(Attributes.MOVEMENT_SPEED),p.getAttribute(Attributes.MOVEMENT_SPEED).getBaseValue(),"unequip removes speed bonus");
            p.hurt(p.damageSources().thorns(z),2);near(z.getHealth(),20,"thorns are not melee retaliation triggers");p.invulnerableTime=0;
            p.hurt(p.damageSources().mobAttack(z),2);near(z.getHealth(),18,"rose true damage bypasses armor and absorption");near(z.getAbsorptionAmount(),5,"rose preserves absorption");
            p.invulnerableTime=0;p.hurt(p.damageSources().mobAttack(z),2);near(z.getHealth(),18,"rose shared cooldown");
            var amplifier=new Object(){@net.minecraftforge.eventbus.api.SubscribeEvent public void amplify(net.minecraftforge.event.entity.living.LivingDamageEvent event){if(FloralCombat.rose(event.getSource()))event.setAmount(event.getAmount()*10);}};
            net.minecraftforge.common.MinecraftForge.EVENT_BUS.register(amplifier);
            try{z.invulnerableTime=0;FloralCombat.roseHit(p,z);near(z.getHealth(),16,"rose ignores external damage amplification");}finally{net.minecraftforge.common.MinecraftForge.EVENT_BUS.unregister(amplifier);}
            equip(p,SpringRecipes.crown(Flower.DANDELION),ItemStack.EMPTY);p.getFoodData().setFoodLevel(0);p.getFoodData().setSaturation(0);p.getPersistentData().remove("TravailFoodFraction");
            p.getFoodData().eat(Items.APPLE,new ItemStack(Items.APPLE),p);near(p.getFoodData().getFoodLevel(),4,"food first fraction");
            p.getFoodData().eat(Items.APPLE,new ItemStack(Items.APPLE),p);near(p.getFoodData().getFoodLevel(),9,"food accumulated fraction");near(p.getFoodData().getSaturationLevel(),5.76,"independent saturation bonus");
            equip(p,SpringRecipes.crown(Flower.ALLIUM),ItemStack.EMPTY);health(p,20);net.minecraft.world.effect.MobEffects.POISON.applyEffectTick(p,0);near(p.getHealth(),19.75,"poison-only reduction");
            p.invulnerableTime=0;p.hurt(p.damageSources().magic(),1);near(p.getHealth(),18.75,"ordinary magic not reduced as poison");
            health(p,20);
            var nestedMagic=new Object(){boolean inside;
                @net.minecraftforge.eventbus.api.SubscribeEvent public void poisonNestedHurt(net.minecraftforge.event.entity.living.LivingHurtEvent event){
                    if(event.getEntity()==p&&!inside){inside=true;try{p.invulnerableTime=0;p.hurt(p.damageSources().magic(),2);}finally{inside=false;}}
                }
            };
            net.minecraftforge.common.MinecraftForge.EVENT_BUS.register(nestedMagic);
            try{net.minecraft.world.effect.MobEffects.POISON.applyEffectTick(p,0);near(p.getHealth(),17.75,"nested magic in poison event is not poison damage");}
            finally{net.minecraftforge.common.MinecraftForge.EVENT_BUS.unregister(nestedMagic);}
            equip(p,SpringRecipes.crown(Flower.WHITE_TULIP),ItemStack.EMPTY);health(p,5);com.thelongtravail.helper.TrueDamage.hurtFluid(p,2);near(p.getHealth(),3,"existing enforced fluid damage remains unavoidable");
            equip(p,SpringRecipes.crown(Flower.PINK_TULIP),ItemStack.EMPTY);health(p,10);p.heal(5);near(p.getHealth(),16,"healing bonus");
            var flowers=SpringRecipes.crown(Flower.LILY_OF_THE_VALLEY);FlowerData.write(flowers,List.of(Flower.LILY_OF_THE_VALLEY,Flower.LILAC));equip(p,flowers,ItemStack.EMPTY);
            z=new Zombie(EntityType.ZOMBIE,level);
            var cow=new net.minecraft.world.entity.animal.Cow(EntityType.COW,level);cow.setHealth(10);cow.hurt(p.damageSources().playerAttack(p),1);check(cow.getEffect(net.minecraft.world.effect.MobEffects.POISON).getDuration()==100,"lilac extends attributed poison once");
            cow.removeEffect(net.minecraft.world.effect.MobEffects.POISON);cow.invulnerableTime=0;cow.hurt(p.damageSources().playerAttack(p),1);check(!cow.hasEffect(net.minecraft.world.effect.MobEffects.POISON),"per-target flower cooldown");
            var reusable=new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.WEAKNESS,80);
            var firstTarget=new net.minecraft.world.entity.animal.Cow(EntityType.COW,level);var secondTarget=new net.minecraft.world.entity.animal.Cow(EntityType.COW,level);
            firstTarget.addEffect(reusable,p);secondTarget.addEffect(reusable,p);
            check(reusable.getDuration()==80,"lilac preserves caller effect instance");
            check(firstTarget.getEffect(net.minecraft.world.effect.MobEffects.WEAKNESS).getDuration()==100&&secondTarget.getEffect(net.minecraft.world.effect.MobEffects.WEAKNESS).getDuration()==100,"reused spell effect extends once per target");
            equip(p,SpringRecipes.crown(Flower.POPPY),heart);health(p,20);pet.setHealth(20);pet.invulnerableTime=0;Affection.target(p);
            var nestedVictim=new net.minecraft.world.entity.animal.Cow(EntityType.COW,level);nestedVictim.setHealth(10);
            var nested=new Object(){@net.minecraftforge.eventbus.api.SubscribeEvent public void hurt(net.minecraftforge.event.entity.living.LivingHurtEvent event){
                if(event.getEntity()==pet)nestedVictim.hurt(p.damageSources().playerAttack(p),4);
            }};
            net.minecraftforge.common.MinecraftForge.EVENT_BUS.register(nested);
            try{p.hurt(p.damageSources().generic(),8);near(nestedVictim.getHealth(),5.68,"nested attack keeps its own outgoing bonus");}
            finally{net.minecraftforge.common.MinecraftForge.EVENT_BUS.unregister(nested);}
            equip(p,SpringRecipes.crown(Flower.PITCHER_PLANT),ItemStack.EMPTY);health(p,10);
            var doomed=new Zombie(EntityType.ZOMBIE,level);doomed.setHealth(1);
            var cancelDeath=new Object(){@net.minecraftforge.eventbus.api.SubscribeEvent(priority=net.minecraftforge.eventbus.api.EventPriority.LOWEST) public void death(net.minecraftforge.event.entity.living.LivingDeathEvent event){
                if(event.getEntity()==doomed){event.setCanceled(true);doomed.setHealth(5);}
            }};
            var endTick=new net.minecraftforge.event.TickEvent.ServerTickEvent(net.minecraftforge.event.TickEvent.Phase.END,()->true,level.getServer());
            net.minecraftforge.common.MinecraftForge.EVENT_BUS.register(cancelDeath);
            try{doomed.hurt(p.damageSources().playerAttack(p),100);FloralEvents.endTick(endTick);near(p.getHealth(),10,"cancelled death grants no healing");}
            finally{net.minecraftforge.common.MinecraftForge.EVENT_BUS.unregister(cancelDeath);}
            doomed.invulnerableTime=0;doomed.hurt(p.damageSources().playerAttack(p),100);FloralEvents.endTick(endTick);near(p.getHealth(),12,"confirmed kill heals without cancelled kill consuming cooldown");
            var secondKill=new Zombie(EntityType.ZOMBIE,level);secondKill.hurt(p.damageSources().playerAttack(p),100);FloralEvents.endTick(endTick);near(p.getHealth(),12,"confirmed kills obey shared cooldown");
            check(!AffectionDeaths.get(level.getServer()).died(UUID.randomUUID()),"unbound deaths skip inventory scan");
            extendedChecks(p,pet,heart,level);
            AffectionDeaths.get(level.getServer()).died(pet.getUUID());AffectionDeaths.get(level.getServer()).clean(heart);check(!heart.getTag().hasUUID(Affection.OWNER),"stored item clears confirmed dead binding");
            Affection.bind(heart,p,pet);Affection.clear(heart);check(!heart.getTag().hasUUID(Affection.OWNER),"unbind removes equip restriction");
        } finally {playerMap.remove(p.getUUID());pet.discard();Affection.reset(p);}
        compactJeiChecks(level);
        System.out.println("FLORAL_ITEMS_PASS: recipes, disabled primary, bucket, binding, split, bonuses, true retaliation, cooldown, food fractions, poison, lilac");
    }
    private static void extendedChecks(Player p,Wolf pet,ItemStack heart,ServerLevel level)throws Exception {
        var end=new net.minecraftforge.event.TickEvent.PlayerTickEvent(net.minecraftforge.event.TickEvent.Phase.END,p);
        ItemStack attrs=SpringRecipes.crown(Flower.PEONY);FlowerData.write(attrs,List.of(Flower.PEONY,Flower.ORANGE_TULIP,Flower.BLUE_ORCHID));
        equip(p,attrs,ItemStack.EMPTY);health(p,20);FloralEvents.tick(end);
        near(p.getMaxHealth(),22,"peony max health");near(p.getAttributeValue(Attributes.ATTACK_SPEED),4.4,"orange tulip attack speed");
        near(p.getAttributeValue(net.minecraftforge.common.ForgeMod.SWIM_SPEED.get()),1.25,"orchid swim speed");
        FlourishingItemsConfig.ENABLED.get(Flower.PEONY).set(false);
        try{FloralEvents.tick(end);near(p.getMaxHealth(),20,"disabled equipped flower removes attribute");check(FlowerData.read(attrs).get(0)==Flower.PEONY,"disabled main flower stays stored");}
        finally{FlourishingItemsConfig.ENABLED.get(Flower.PEONY).set(true);}
        equip(p,SpringRecipes.crown(Flower.PINK_PETALS),ItemStack.EMPTY);FloralEvents.tick(end);
        near(p.getAttributeValue(Attributes.MOVEMENT_SPEED),p.getAttribute(Attributes.MOVEMENT_SPEED).getBaseValue()*1.1,"petals speed");
        near(p.getAttributeValue(Attributes.ATTACK_SPEED),4,"removed flower resets attack speed");
        near(p.getAttributeValue(net.minecraftforge.common.ForgeMod.SWIM_SPEED.get()),1,"removed flower resets swim speed");
        equip(p,SpringRecipes.crown(Flower.AZURE_BLUET),ItemStack.EMPTY);health(p,20);p.hurt(p.damageSources().fall(),4);near(p.getHealth(),18,"azure fall reduction");
        equip(p,SpringRecipes.crown(Flower.TORCHFLOWER),ItemStack.EMPTY);health(p,20);p.hurt(p.damageSources().onFire(),4);near(p.getHealth(),17.6,"torchflower fire reduction");
        equip(p,SpringRecipes.crown(Flower.WHITE_TULIP),ItemStack.EMPTY);health(p,9);p.hurt(p.damageSources().generic(),4);near(p.getHealth(),5.48,"white tulip low-health reduction");
        long originalTime=level.getDayTime();var originalPos=p.position();
        try {
            p.setPos(p.getX(),level.getMaxBuildHeight()-2,p.getZ());level.setDayTime(6000);level.updateSkyBrightness();
            equip(p,SpringRecipes.crown(Flower.SUNFLOWER),ItemStack.EMPTY);FloralEvents.tick(end);
            near(p.getAttributeValue(Attributes.MOVEMENT_SPEED),p.getAttribute(Attributes.MOVEMENT_SPEED).getBaseValue()*1.1,"sunflower daytime open-sky speed");
            near(FloralCombat.bonus(pet,p.damageSources().playerAttack(p)),.1,"sunflower daytime damage");
            level.setDayTime(18000);level.updateSkyBrightness();FloralEvents.tick(end);
            near(p.getAttributeValue(Attributes.MOVEMENT_SPEED),p.getAttribute(Attributes.MOVEMENT_SPEED).getBaseValue(),"sunflower night removes speed");
            near(FloralCombat.bonus(pet,p.damageSources().playerAttack(p)),0,"sunflower night removes damage");
        } finally {level.setDayTime(originalTime);level.updateSkyBrightness();p.setPos(originalPos);}
        var victim=new net.minecraft.world.entity.animal.Cow(EntityType.COW,level);victim.getAttribute(Attributes.MAX_HEALTH).setBaseValue(40);victim.setHealth(40);
        equip(p,SpringRecipes.crown(Flower.RED_TULIP),ItemStack.EMPTY);health(p,9);victim.hurt(p.damageSources().playerAttack(p),4);near(victim.getHealth(),35.52,"red tulip low-health damage");
        var arrow=new net.minecraft.world.entity.projectile.Arrow(level,p);equip(p,SpringRecipes.crown(Flower.CORNFLOWER),ItemStack.EMPTY);victim.invulnerableTime=0;victim.setHealth(40);
        victim.hurt(p.damageSources().arrow(arrow,p),4);near(victim.getHealth(),35.52,"cornflower projectile damage");
        equip(p,SpringRecipes.crown(Flower.WITHER_ROSE),ItemStack.EMPTY);victim.invulnerableTime=0;victim.hurt(p.damageSources().arrow(arrow,p),1);
        check(victim.hasEffect(net.minecraft.world.effect.MobEffects.WITHER),"attributed projectile applies wither");
        equip(p,SpringRecipes.crown(Flower.OXEYE_DAISY),ItemStack.EMPTY);health(p,10);p.getFoodData().setFoodLevel(0);
        for(int i=0;i<100;i++)FloralEvents.tick(end);near(p.getHealth(),11,"daisy heals without hunger requirement");
        equip(p,ItemStack.EMPTY,heart);health(p,20);pet.setHealth(20);pet.invulnerableTime=0;Affection.target(p);
        FlourishingItemsConfig.SHARING.set(false);
        try{p.hurt(p.damageSources().generic(),4);near(p.getHealth(),16,"sharing switch disables split");near(pet.getHealth(),20,"sharing switch leaves pet unharmed");}
        finally{FlourishingItemsConfig.SHARING.set(true);}
        health(p,20);pet.setHealth(0);check(Affection.target(p)==null,"zero-health target pauses bonuses");
        check(heart.getTag().hasUUID(Affection.TARGET),"unconfirmed death retains binding for cancelled death or revival");
        pet.setHealth(20);check(Affection.target(p)==pet,"revival restores same binding");
        Player other=new Player(level);check(!Affection.ownerAllowed(heart,other),"other player cannot equip bound heart");
        UUID targetId=heart.getTag().getUUID(Affection.TARGET);var otherPet=new Wolf(EntityType.WOLF,level);otherPet.setTame(true);otherPet.setOwnerUUID(p.getUUID());
        Affection.bind(heart,p,otherPet);check(heart.getTag().getUUID(Affection.TARGET).equals(targetId),"binding another target cannot overwrite current binding");
        pet.setOwnerUUID(other.getUUID());Affection.reset(p);check(Affection.target(p)==null,"ownership change suspends effects");pet.setOwnerUUID(p.getUUID());Affection.reset(p);check(Affection.target(p)==pet,"restored owner resumes effects");
        var heartItem=(com.thelongtravail.item.FlourishingAccessoryItem)heart.getItem();
        var body=new top.theillusivec4.curios.api.SlotContext("body",p,0,false,true);
        pet.setOwnerUUID(other.getUUID());heartItem.onUnequip(body,ItemStack.EMPTY,heart);heartItem.onEquip(body,ItemStack.EMPTY,heart);
        check(Affection.target(p)==null,"re-equip invalidates same-tick owner cache");pet.setOwnerUUID(p.getUUID());Affection.reset(p);Affection.target(p);
        check(!heartItem.canEquip(new top.theillusivec4.curios.api.SlotContext("body",other,0,false,true),heart),"canEquip rejects wrong recorded owner");
        check(!heartItem.canEquip(new top.theillusivec4.curios.api.SlotContext("body",p,1,false,true),heart.copy()),"canEquip rejects second heart");
        var crownItem=(com.thelongtravail.item.FlourishingAccessoryItem)ModRegistry.SPRING_GAME.get();var sample=SpringRecipes.crown(Flower.POPPY);equip(p,sample,heart);
        check(!crownItem.canEquip(new top.theillusivec4.curios.api.SlotContext("head",p,1,false,true),sample.copy()),"canEquip rejects second crown");
        check(!crownItem.canEquip(body,sample),"crown wrong slot rejected");
        ItemStack held=heart.copy();other.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,held);other.setShiftKeyDown(true);
        heartItem.use(level,other,net.minecraft.world.InteractionHand.MAIN_HAND);check(!held.getTag().hasUUID(Affection.TARGET)&&!held.getTag().hasUUID(Affection.OWNER),"any holder can sneak-air unbind");
        equip(p,ItemStack.EMPTY,heart);health(p,20);pet.setHealth(20);pet.invulnerableTime=0;Affection.target(p);
        var fraction=FlourishingItemsConfig.NUMBERS.get("affection.shareFraction");var multiplier=FlourishingItemsConfig.NUMBERS.get("affection.servantShareMultiplier");double oldFraction=fraction.get(),oldMultiplier=multiplier.get();
        try{fraction.set(.25);multiplier.set(.2);p.hurt(p.damageSources().generic(),8);near(p.getHealth(),14,"custom share fraction affects owner");near(pet.getHealth(),19.6,"custom transfer multiplier affects pet");}
        finally{fraction.set(oldFraction);multiplier.set(oldMultiplier);}
        int recipes=0;
        for(var r:level.getRecipeManager().getAllRecipesFor(RecipeType.CRAFTING))if(r instanceof SpringRecipes.Crown original) {
            var buffer=new net.minecraft.network.FriendlyByteBuf(io.netty.buffer.Unpooled.buffer());
            try{var serializer=new SpringRecipes.CrownSerializer();serializer.toNetwork(buffer,original);var decoded=serializer.fromNetwork(original.getId(),buffer);
                check(decoded.flower==original.flower&&FlowerData.read(decoded.getResultItem(level.registryAccess())).equals(List.of(original.flower)),"network recipe preserves primary flower");recipes++;
            }finally{buffer.release();}
        }
        check(recipes==20,"all 20 flower recipes survived network serialization");
        System.out.println("FLORAL_BOUNDARIES_PASS: re-equip ownership, unique slots, manual unbind, configured sharing, 20 network recipes");
        for(int i=0;i<travail.smoke.LegacyLootBaseline.AFFECTION.size();i++) {
            String id=travail.smoke.LegacyLootBaseline.AFFECTION.get(i).split("\\|")[0];
            var table=level.getServer().getLootData().getLootTable(new net.minecraft.resources.ResourceLocation(id));
            check(table.getPool("the_long_travail:shared_items")!=null,"loot injection "+id);
        }
        var dispenser=level.getServer().getLootData().getLootTable(new net.minecraft.resources.ResourceLocation("minecraft:chests/jungle_temple_dispenser"));
        for(int i=0;i<17;i++)check(dispenser.getPool("the_long_travail:shared_items")==null,"temple dispenser excluded");
        equip(p,ItemStack.EMPTY,heart);health(p,20);pet.setHealth(20);pet.invulnerableTime=0;Affection.target(p);
        var cancelledAttack=new Object(){@net.minecraftforge.eventbus.api.SubscribeEvent public void rejectIncoming(net.minecraftforge.event.entity.living.LivingAttackEvent event){if(event.getEntity()==p)event.setCanceled(true);}};
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.register(cancelledAttack);
        try{p.hurt(p.damageSources().generic(),8);near(p.getHealth(),20,"cancelled attack leaves player unharmed");near(pet.getHealth(),20,"cancelled attack never reaches sharing");}
        finally{net.minecraftforge.common.MinecraftForge.EVENT_BUS.unregister(cancelledAttack);}
        var saved=new AffectionDeaths();UUID storedId=UUID.randomUUID();saved.watch(storedId);check(saved.died(storedId),"watched death recorded");
        var loader=AffectionDeaths.class.getDeclaredMethod("load",net.minecraft.nbt.CompoundTag.class);loader.setAccessible(true);
        var restored=(AffectionDeaths)loader.invoke(null,saved.save(new net.minecraft.nbt.CompoundTag()));
        var storedHeart=heart.copy();storedHeart.getOrCreateTag().putUUID(Affection.TARGET,storedId);restored.clean(storedHeart);
        check(!storedHeart.getTag().hasUUID(Affection.TARGET)&&!storedHeart.getTag().hasUUID(Affection.OWNER),"death record survives save and load");
        var untouched=heart.copy();restored.clean(untouched);check(untouched.getTag().hasUUID(Affection.TARGET),"unrecorded target survives stored-item cleanup");
        var lootId=new net.minecraft.resources.ResourceLocation("minecraft:chests/jungle_temple");
        var params=new net.minecraft.world.level.storage.loot.LootParams.Builder(level).withParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.ORIGIN,p.position()).create(net.minecraft.world.level.storage.loot.parameters.LootContextParamSets.CHEST);
        var table=net.minecraft.world.level.storage.loot.LootTable.lootTable().build();table.setLootTableId(lootId);com.thelongtravail.loot.UnifiedLoot.inject(table);
        var pool=table.getPool("the_long_travail:shared_items");com.thelongtravail.loot.UnifiedLoot.inject(table);
        check(pool!=null&&table.getPool("the_long_travail:shared_items")==pool,"fixed loot injection is idempotent");
        int hits=0;
        for(int seed=1;seed<=5000;seed++) {
            int count=table.getRandomItems(params,seed*7919L).stream().filter(stack->stack.is(ModRegistry.AFFECTION.get())).mapToInt(ItemStack::getCount).sum();
            check(count<=1,"fixed loot grants at most one accessory");hits+=count;
        }
        check(Math.abs(hits-250)<80,"fixed temple probability remains 5%");
        var empty=net.minecraft.world.level.storage.loot.LootTable.lootTable().build();empty.setLootTableId(new net.minecraft.resources.ResourceLocation("travail_smoke:unrelated"));com.thelongtravail.loot.UnifiedLoot.inject(empty);
        check(empty.getRandomItems(params,1).isEmpty(),"unrelated table receives no injection");
        System.out.println("FLORAL_LIFECYCLE_PASS: cancelled attack, persisted binding deaths, fixed loot chance, count, idempotence");
        System.out.println("FLORAL_EXTENDED_PASS: attributes, disabled flower, defense, projectiles, wither, daisy, sharing switch, revival, owner restriction, loot tables");
    }

    private static void compactJeiChecks(ServerLevel level) {
        var enabledBefore=new EnumMap<Flower,Boolean>(Flower.class);for(Flower f:Flower.values())enabledBefore.put(f,FlourishingItemsConfig.ENABLED.get(f).get());
        try {
            int all=(1<<Flower.values().length)-1;
            for(int mask:new int[]{all,1<<Flower.CORNFLOWER.ordinal(),0}) {
                for(Flower f:Flower.values())FlourishingItemsConfig.ENABLED.get(f).set((mask&(1<<f.ordinal()))!=0);
                var snapshot=FloralRecipeDisplays.build(mask);
                check(snapshot.changes().size()==(mask==0?1:3),"compact operation recipe count");
                check((snapshot.crown()!=null)==(mask!=0),"compact crafting visibility");
                var change=new SpringRecipes.Change(new net.minecraft.resources.ResourceLocation("travail_smoke:compact"),CraftingBookCategory.EQUIPMENT);
                var pair=grid(2,2);
                for(var d:snapshot.changes()) {
                    check(d.inputs().size()==d.materials().size()&&d.inputs().size()==d.outputs().size(),"linked slot lists have equal lengths");
                    for(int i=0;i<d.inputs().size();i++) {
                        pair.setItem(0,d.inputs().get(i));pair.setItem(1,d.materials().get(i));
                        check(change.matches(pair,level),"every compact display row is a valid server recipe");
                        check(ItemStack.isSameItemSameTags(change.assemble(pair,level.registryAccess()),d.outputs().get(i)),"linked display output matches real crafting");
                    }
                    if(!d.operation().equals("wash"))for(Flower f:Flower.values())if((mask&(1<<f.ordinal()))!=0)
                        check(d.materials().stream().anyMatch(stack->stack.is(f.item())),"enabled flower indexed for usage lookup");
                }
                if(snapshot.crown()!=null)for(Flower f:snapshot.crown().flowers) {
                    var table=grid(3,3);for(int i:new int[]{0,1,2,6,8})table.setItem(i,new ItemStack(Items.WHEAT));table.setItem(3,new ItemStack(Items.STRING));table.setItem(5,new ItemStack(Items.STRING));table.setItem(4,new ItemStack(f.item()));
                    check(snapshot.crown().matches(table,level),"merged shaped ingredients match displayed flower");
                    var actual=level.getRecipeManager().getRecipeFor(RecipeType.CRAFTING,table,level).orElseThrow();
                    check(FlowerData.read(actual.assemble(table,level.registryAccess())).equals(List.of(f)),"real shaped output corresponds to rotating flower");
                }
            }
        } finally {enabledBefore.forEach((f,v)->FlourishingItemsConfig.ENABLED.get(f).set(v));}
        System.out.println("FLORAL_JEI_COMPACT_PASS: 1 crafting + 3 operations, linked rows match real recipes, flower lookup coverage, single/no enabled flowers");
    }

}
