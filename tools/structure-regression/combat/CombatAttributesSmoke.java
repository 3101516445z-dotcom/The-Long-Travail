package travail.smoke;

import com.mojang.authlib.GameProfile;
import com.thelongtravail.TravailAspect;
import com.thelongtravail.combat.*;
import com.thelongtravail.config.TravailConfig;
import com.thelongtravail.data.LongTravailData;
import com.thelongtravail.helper.TrueDamage;
import com.thelongtravail.registry.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.*;
import net.minecraft.world.damagesource.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.item.*;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.*;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.fml.common.Mod;
import java.util.*;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.common.inventory.CurioStacksHandler;
import top.theillusivec4.curios.api.type.capability.ICurio.DropRule;

@Mod("travail_smoke")
public final class CombatAttributesSmoke {
    private static final List<String> checks = new ArrayList<>();
    private final List<LivingEntity> entities = new ArrayList<>();
    private ServerLevel level;
    private LivingEntity cancelTarget, amplifyTarget, replayImmune, replayNormal;
    private int cancelStage, reflectionCount, verifyTick;
    private LivingEntity replaceTarget;
    private float replacement;
    private LivingEntity executionVictim, nestedVictim;
    private DamageSource reflected;
    private final List<String> auditFailures = new ArrayList<>();
    private java.util.function.Consumer<LivingDamageEvent> damageProbe;
    private java.util.function.Consumer<LivingAttackEvent> attackProbe;

    public CombatAttributesSmoke() {
        MinecraftForge.EVENT_BUS.addListener(this::start);
        MinecraftForge.EVENT_BUS.addListener(this::tick);
        MinecraftForge.EVENT_BUS.addListener(this::attack);
        MinecraftForge.EVENT_BUS.addListener(this::hurt);
        MinecraftForge.EVENT_BUS.addListener(this::damage);
    }
    private void attack(LivingAttackEvent e) {
        if (attackProbe != null) attackProbe.accept(e);
        if (e.getEntity()==executionVictim) {
            executionVictim=null;
            nestedVictim.hurt(nestedVictim.damageSources().generic(),5);
        }
        if (InsightRetaliation.isInsight(e.getSource())) { reflectionCount++; reflected=e.getSource(); }
        if (e.getEntity()==cancelTarget && cancelStage==1) e.setCanceled(true);
    }
    private void hurt(LivingHurtEvent e) {
        if (e.getEntity()==cancelTarget && cancelStage==2) e.setCanceled(true);
    }
    private void damage(LivingDamageEvent e) {
        if (e.getEntity()==cancelTarget && cancelStage==3) e.setCanceled(true);
        if (e.getEntity()==replaceTarget) e.setAmount(replacement);
        if (e.getEntity()==amplifyTarget) e.setAmount(e.getAmount()*10);
        if (damageProbe != null) damageProbe.accept(e);
    }
    private static void near(double got,double want,String label) {
        if (!Double.isFinite(got) || Math.abs(got-want)>.002) throw new AssertionError(label+": got="+got+" want="+want);
        checks.add(label);
    }
    private static void check(boolean ok,String label) {
        if(!ok)throw new AssertionError(label); checks.add(label);
    }
    private <T extends LivingEntity> T init(T e) {
        e.setPos(0,200,0); e.getAttribute(Attributes.MAX_HEALTH).setBaseValue(100); e.setHealth(100);
        if(e instanceof net.minecraft.world.entity.Mob m)m.setNoAi(true);
        entities.add(e); return e;
    }
    private Cow cow() { return init(new Cow(EntityType.COW,level)); }
    private ServerPlayer player() { return init(new P(level)); }
    private static class P extends ServerPlayer {
        P(ServerLevel level) {
            super(level.getServer(),level,new GameProfile(UUID.randomUUID(),"AttributeTest"));
            var wire=new net.minecraft.network.Connection(net.minecraft.network.protocol.PacketFlow.SERVERBOUND) {
                @Override public void send(net.minecraft.network.protocol.Packet<?> packet) {}
            };
            connection=new net.minecraft.server.network.ServerGamePacketListenerImpl(level.getServer(),wire,this) {
                @Override public void send(net.minecraft.network.protocol.Packet<?> packet) {}
            };
            try { var protection=ServerPlayer.class.getDeclaredField("spawnInvulnerableTime"); protection.setAccessible(true); protection.setInt(this,0); }
            catch(ReflectiveOperationException error){throw new RuntimeException(error);}
        }
    }
    private static void attr(LivingEntity e,Attribute a,double value) { e.getAttribute(a).setBaseValue(value); }
    private DamageSource source(net.minecraft.resources.ResourceKey<DamageType> type,Entity owner) {
        return new DamageSource(level.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(type),owner);
    }
    private static void hit(LivingEntity victim,DamageSource source,float amount) {
        victim.invulnerableTime=0; victim.hurt(source,amount);
    }
    private ItemStack diary(ServerPlayer p) {
        var inv=CuriosApi.getCuriosInventory(p).resolve().orElseThrow();
        var slots=new HashMap<String,top.theillusivec4.curios.api.type.inventory.ICurioStacksHandler>();
        slots.put("travel_diary",new CurioStacksHandler(inv,"travel_diary",1,true,false,true,DropRule.DEFAULT));
        inv.setCurios(slots);
        var diary=new ItemStack(ModRegistry.LONG_TRAVAIL.get());
        LongTravailData.initialize(diary,p);
        for(var aspect:TravailAspect.values())LongTravailData.setWitness(diary,aspect,true);
        inv.getStacksHandler("travel_diary").orElseThrow().getStacks().setStackInSlot(0,diary);
        return diary;
    }
    private void start(ServerStartedEvent event) {
        level=event.getServer().overworld();
        event.getServer().setDifficulty(net.minecraft.world.Difficulty.NORMAL, true);
        try { run(); verifyTick=event.getServer().getTickCount()+5; }
        catch(Throwable error) { done(error); }
    }
    private void run() {
        contextChecks();
        LivingEntity a=cow(), b=cow();
        near(CombatAttributes.insight(a),0,"default insight");
        near(CombatAttributes.criticalChance(a),0,"default critical");
        near(CombatAttributes.overlord(player()),0,"player default overlord");
        attr(a,ModAttributes.CRITICAL_CHANCE.get(),2); near(CombatAttributes.criticalChance(a),1,"chance capped");
        attr(a,ModAttributes.CRITICAL_CHANCE.get(),-2); near(CombatAttributes.criticalChance(a),0,"chance lower bound");
        hit(b,source(DamageTypes.MOB_ATTACK,a),10); near(b.getHealth(),90,"zero attributes preserve damage");
        attr(a,ModAttributes.CRITICAL_CHANCE.get(),1);
        b=cow(); hit(b,source(DamageTypes.MOB_ATTACK,a),10); near(b.getHealth(),80,"critical doubles once");
        b=cow(); attr(b,Attributes.ARMOR,20); hit(b,source(DamageTypes.MOB_ATTACK,a),10);
        near(b.getHealth(),100-net.minecraft.world.damagesource.CombatRules.getDamageAfterAbsorb(20,20,0),"critical before armor");
        attr(a,ModAttributes.CRITICAL_CHANCE.get(),0);
        b=cow(); attr(b,ModAttributes.INSIGHT.get(),5); int count=reflectionCount;
        hit(b,source(DamageTypes.MOB_ATTACK,a),3);
        near(b.getHealth(),100,"insight fully blocks small hit"); near(a.getHealth(),95,"full insight reflected");
        check(reflectionCount==count+1 && reflected.getEntity()==b && reflected.is(DamageTypes.FALLING_ANVIL),"anvil owner and one reflection");
        a=cow(); b=cow(); attr(b,ModAttributes.INSIGHT.get(),5); attr(b,ModAttributes.CRITICAL_CHANCE.get(),1);
        hit(b,source(DamageTypes.MOB_ATTACK,a),12); near(b.getHealth(),93,"insight subtracts fixed damage"); near(a.getHealth(),90,"reflection can critical");
        a=cow(); b=cow(); attr(a,ModAttributes.INSIGHT.get(),2); attr(b,ModAttributes.INSIGHT.get(),5);
        count=reflectionCount; hit(b,source(DamageTypes.MOB_ATTACK,a),12);
        near(a.getHealth(),97,"reflection defended by insight"); check(reflectionCount==count+1,"no reflection recursion");
        b=cow(); attr(b,ModAttributes.INSIGHT.get(),5); hit(b,b.damageSources().fall(),8); near(b.getHealth(),92,"sourceless skips insight");
        b=cow(); attr(b,ModAttributes.INSIGHT.get(),5); attr(b,ModAttributes.CRITICAL_CHANCE.get(),1);
        hit(b,source(DamageTypes.MOB_ATTACK,b),8); near(b.getHealth(),92,"self source skips insight and critical");
        for(boolean isPlayer:new boolean[]{false,true}) {
            LivingEntity v=isPlayer?player():cow(); attr(v,ModAttributes.OVERLORD.get(),.1); v.setAbsorptionAmount(20);
            hit(v,v.damageSources().generic(),10); near(v.getHealth(),100,"overlord equality health "+isPlayer); near(v.getAbsorptionAmount(),20,"overlord preserves absorption "+isPlayer);
            hit(v,v.damageSources().generic(),11); near(v.getAbsorptionAmount(),9,"above threshold full absorption cost "+isPlayer);
            v.setAbsorptionAmount(0); hit(v,v.damageSources().magic(),10); near(v.getHealth(),100,"magic immunity "+isPlayer);
            hit(v,v.damageSources().fellOutOfWorld(),10); near(v.getHealth(),100,"void immunity "+isPlayer);
            hit(v,v.damageSources().genericKill(),10); near(v.getHealth(),100,"generic kill type threshold "+isPlayer);
        }
        a=cow(); b=cow(); attr(b,ModAttributes.INSIGHT.get(),5); attr(b,ModAttributes.OVERLORD.get(),.1);
        hit(b,source(DamageTypes.MOB_ATTACK,a),14); near(b.getHealth(),100,"insight before overlord"); near(a.getHealth(),95,"immune hit still retaliates");
        for(int stage=1;stage<=3;stage++) {
            a=cow(); b=cow(); attr(b,ModAttributes.INSIGHT.get(),5); cancelTarget=b; cancelStage=stage;
            hit(b,source(DamageTypes.MOB_ATTACK,a),12); near(a.getHealth(),100,"cancelled hit no reflection stage "+stage); near(b.getHealth(),100,"cancelled hit no damage stage "+stage);
        }
        cancelTarget=null;
        a=cow(); b=cow(); attr(a,ModAttributes.CRITICAL_CHANCE.get(),1); attr(b,ModAttributes.OVERLORD.get(),.1);
        hit(b,source(com.thelongtravail.flourishing.FloralCombat.ROSE,a),4); near(b.getHealth(),100,"forced rose remains zero after overlord");
        b=cow(); hit(b,source(com.thelongtravail.flourishing.FloralCombat.ROSE,a),4); near(b.getHealth(),92,"rose can critical");
        b=cow(); amplifyTarget=b; hit(b,source(com.thelongtravail.flourishing.FloralCombat.ROSE,a),4); near(b.getHealth(),92,"rose critical still ignores external damage amplification"); amplifyTarget=null;
        b=cow(); attr(b,ModAttributes.INSIGHT.get(),2); hit(b,source(com.thelongtravail.flourishing.FloralCombat.ROSE,a),4); near(b.getHealth(),94,"forced rose insight not restored");
        var p=player(); attr(p,ModAttributes.INSIGHT.get(),99); attr(p,ModAttributes.OVERLORD.get(),1); attr(p,ModAttributes.CRITICAL_CHANCE.get(),1);
        for(var mode:TravailConfig.FluidProtection.values()) {
            var old=TravailConfig.FLUID_PROTECTION.get();
            try { TravailConfig.FLUID_PROTECTION.set(mode); p.setHealth(100); p.invulnerableTime=0; TrueDamage.hurtFluid(p,7); near(p.getHealth(),93,"curse bypass "+mode); }
            finally { TravailConfig.FLUID_PROTECTION.set(old); }
        }
        p.setHealth(100); a=cow(); attr(a,ModAttributes.CRITICAL_CHANCE.get(),1); int before=reflectionCount;
        hit(p,source(com.thelongtravail.helper.TrueDamage.FLUID,a),7);
        near(p.getHealth(),93,"curse with entity source bypasses all attributes"); check(reflectionCount==before,"curse cannot retaliate");
        p=player(); var d=diary(p); LongTravailData.setWitness(d,TravailAspect.FLOURISHING,false);
        attr(p,ModAttributes.INSIGHT.get(),5); attr(p,ModAttributes.OVERLORD.get(),.1);
        a=cow(); var old=TravailConfig.FLOURISHING_DAMAGE_MULTIPLIER.get();
        try {
            TravailConfig.FLOURISHING_DAMAGE_MULTIPLIER.set(3.0);
            hit(p,source(DamageTypes.MOB_ATTACK,a),6); near(p.getHealth(),87,"curse amplification before attributes");
            p.setHealth(100); attr(a,ModAttributes.CRITICAL_CHANCE.get(),1);
            hit(p,source(DamageTypes.MOB_ATTACK,a),6); near(p.getHealth(),69,"curse amplification then critical then insight");
        }
        finally { TravailConfig.FLOURISHING_DAMAGE_MULTIPLIER.set(old); }
        p=player(); d=diary(p); LongTravailData.setWitness(d,TravailAspect.ABYSS,false); attr(p,ModAttributes.OVERLORD.get(),.1);
        old=TravailConfig.ABYSS_ENVIRONMENT_MULTIPLIER.get();
        try { TravailConfig.ABYSS_ENVIRONMENT_MULTIPLIER.set(3.0); hit(p,p.damageSources().drown(),4); near(p.getHealth(),88,"environment curse amplified before overlord"); }
        finally { TravailConfig.ABYSS_ENVIRONMENT_MULTIPLIER.set(old); }
        a=cow(); b=cow(); attr(b,ModAttributes.INSIGHT.get(),5); attr(b,ModAttributes.CRITICAL_CHANCE.get(),1);
        var arrow=new net.minecraft.world.entity.projectile.Arrow(level,a); hit(b,new DamageSource(level.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(DamageTypes.ARROW),arrow,a),8);
        near(a.getHealth(),90,"projectile retaliates against shooter");
        a=cow(); b=player(); attr(a,ModAttributes.INSIGHT.get(),5); hit(a,source(DamageTypes.PLAYER_ATTACK,b),8); near(b.getHealth(),95,"mob can retaliate against player");
        b=cow(); attr(b,ModAttributes.OVERLORD.get(),.1); attr(b,ModAttributes.INSIGHT.get(),50);
        hit(b,source(com.thelongtravail.farreach.DonQuixoteCombat.STRIKE,a),11); near(b.getHealth(),89,"settled strike no insight subtraction");
        b=cow(); attr(b,ModAttributes.OVERLORD.get(),.1); hit(b,source(com.thelongtravail.farreach.DonQuixoteCombat.STRIKE,a),10); near(b.getHealth(),100,"settled strike checks overlord");
        b=cow(); attr(b,ModAttributes.INSIGHT.get(),5); attr(b,ModAttributes.CRITICAL_CHANCE.get(),.5); attr(b,ModAttributes.OVERLORD.get(),.1);
        com.thelongtravail.common.AttributeModifierSet.reconcile(b,new net.minecraft.resources.ResourceLocation("travail_smoke","all"),.2,Set.of());
        near(CombatAttributes.insight(b),6,"all-attribute insight"); near(CombatAttributes.criticalChance(b),.6,"all-attribute critical"); near(b.getMaxHealth()*CombatAttributes.overlord(b),14.4,"all-attribute overlord double scaling");
        a=cow(); b=cow(); attr(b,ModAttributes.INSIGHT.get(),5);
        hit(b,source(DamageTypes.MOB_ATTACK,a),200);
        near(a.getHealth(),100,"lethal hit does not retaliate");
        for (boolean isPlayer : new boolean[]{false,true}) {
            a=cow(); b=isPlayer?player():cow(); attr(b,ModAttributes.OVERLORD.get(),.1);
            var state=(com.thelongtravail.mixin.DamageReplayStateAccessor)b;
            b.invulnerableTime=7; state.travail$lastHurt(3);
            b.hurt(source(DamageTypes.MOB_ATTACK,a),8);
            near(b.invulnerableTime,7,"immune restores cooldown "+isPlayer);
            near(state.travail$lastHurt(),3,"immune restores last hurt "+isPlayer);
            b.hurt(source(DamageTypes.MOB_ATTACK,a),11);
            near(b.getHealth(),89,"immune does not suppress next hit "+isPlayer);
            for (float value:new float[]{0,-5,Float.NaN,Float.POSITIVE_INFINITY}) {
                a=cow(); b=isPlayer?player():cow(); attr(b,ModAttributes.INSIGHT.get(),2);
                replaceTarget=b; replacement=value;
                hit(b,source(DamageTypes.MOB_ATTACK,a),10);
                near(b.getHealth(),100,"invalid final amount safe "+isPlayer+" "+value);
                near(a.getHealth(),100,"invalid final amount no retaliation "+isPlayer+" "+value);
                near(b.getAbsorptionAmount(),0,"invalid final amount no absorption gain "+isPlayer+" "+value);
            }
        }
        replaceTarget=null;
        a=cow(); b=cow(); attr(b,ModAttributes.INSIGHT.get(),2); b.setAbsorptionAmount(20);
        replaceTarget=b; replacement=0; hit(b,source(DamageTypes.MOB_ATTACK,a),10);
        near(a.getHealth(),98,"absorption loss retains retaliation despite final zero"); replaceTarget=null;
        a=cow(); attr(a,ModAttributes.CRITICAL_CHANCE.get(),1);
        for (var type:List.of(com.thelongtravail.valley.AzraelExecution.TYPE,com.thelongtravail.valley.AzraelExecution.SELF)) {
            b=cow(); attr(b,ModAttributes.INSIGHT.get(),100); attr(b,ModAttributes.OVERLORD.get(),1);
            hit(b,source(type,a),10); near(b.getHealth(),90,"execution type bypass all attributes "+type.location());
        }
        a=cow(); b=cow(); attr(a,ModAttributes.CRITICAL_CHANCE.get(),1);
        attr(b,ModAttributes.INSIGHT.get(),100); attr(b,ModAttributes.OVERLORD.get(),1);
        executionVictim=b; nestedVictim=cow(); attr(nestedVictim,ModAttributes.OVERLORD.get(),1);
        com.thelongtravail.valley.AzraelExecution.executeForCommand(a,b);
        check(!b.isAlive(),"actual execution ignores defensive attributes");
        near(a.getHealth(),100,"execution no retaliation");
        near(nestedVictim.getHealth(),100,"unrelated nested damage retains overlord during execution");
        var bonusPlayer=player(); bonusPlayer.setHealth(20);
        var inv=CuriosApi.getCuriosInventory(bonusPlayer).resolve().orElseThrow();
        var head=new CurioStacksHandler(inv,"head",1,true,false,true,DropRule.DEFAULT);
        head.getStacks().setStackInSlot(0,com.thelongtravail.flourishing.SpringRecipes.crown(com.thelongtravail.flourishing.Flower.RED_TULIP));
        inv.setCurios(new HashMap<>(Map.of("head",head)));
        com.thelongtravail.flourishing.Affection.reset(bonusPlayer);
        float baseline=0;
        for (double overlord:new double[]{0,.000001}) {
            b=cow(); attr(b,Attributes.ARMOR,20); attr(b,ModAttributes.OVERLORD.get(),overlord);
            hit(b,source(DamageTypes.PLAYER_ATTACK,bonusPlayer),10);
            if(overlord==0) baseline=b.getHealth();
            else near(b.getHealth(),baseline,"tiny overlord does not move output bonus before armor");
        }
        check(baseline<96,"output bonus fixture effective");
        attr(bonusPlayer,ModAttributes.INSIGHT.get(),5); attr(bonusPlayer,ModAttributes.CRITICAL_CHANCE.get(),1);
        a=cow(); hit(bonusPlayer,source(DamageTypes.MOB_ATTACK,a),6);
        near(a.getHealth(),90,"insight critical does not inherit red tulip bonus");
        auditStates();
        registrationChecks();
        a=cow(); replayImmune=cow(); replayNormal=cow(); level.addFreshEntity(a);level.addFreshEntity(replayImmune);level.addFreshEntity(replayNormal);
        attr(a,ModAttributes.CRITICAL_CHANCE.get(),1); attr(replayImmune,ModAttributes.OVERLORD.get(),.1); attr(replayNormal,ModAttributes.INSIGHT.get(),50);
        var policy=new DamageReplay.Policy(){public boolean valid(LivingEntity x,LivingEntity y){return true;}public boolean continueChain(LivingEntity x,LivingEntity y){return false;}};
        DamageReplay.enqueue(a,replayImmune,source(DamageTypes.MOB_ATTACK,a),10,policy);
        DamageReplay.enqueue(a,replayNormal,source(DamageTypes.MOB_ATTACK,a),11,policy);
    }

    private static Object recorded(com.thelongtravail.helper.CombatContext context, String name) {
        try {
            Object owner=context;
            java.lang.reflect.Field field;
            try {field=context.getClass().getDeclaredField(name);}
            catch(NoSuchFieldException moved) {
                var holder=context.getClass().getDeclaredField("damageRecord");holder.setAccessible(true);
                owner=holder.get(context);field=owner.getClass().getDeclaredField(name);
            }
            field.setAccessible(true);return field.get(owner);
        } catch(ReflectiveOperationException e){throw new RuntimeException(e);}
    }
    private void contextChecks() {
        var target=cow();var source=target.damageSources().generic();var other=target.damageSources().magic();
        for(int i=0;i<100;i++) {
            try(var outer=com.thelongtravail.helper.CombatContext.open(target,source)) {
                check(com.thelongtravail.helper.CombatContext.current(target,source)==outer,"scope outer");
                try(var inner=com.thelongtravail.helper.CombatContext.open(target,source)) {
                    check(inner!=outer&&com.thelongtravail.helper.CombatContext.current(target,source)==inner,"same-source nesting independent");
                    try(var deeper=com.thelongtravail.helper.CombatContext.open(target,other)) {throw new IllegalStateException("fixture");}
                    catch(IllegalStateException expected){check(com.thelongtravail.helper.CombatContext.current(target,source)==inner,"exception restores immediate scope");}
                }
                check(com.thelongtravail.helper.CombatContext.current(target,source)==outer,"inner close restores outer");
            }
            check(com.thelongtravail.helper.CombatContext.current(target,source)==null,"outer close clears scope");
        }
        var random=new java.util.Random(20261024L);
        float[] edges={0,-0.0f,-1,1,Float.MAX_VALUE,-Float.MAX_VALUE,Float.NaN,Float.POSITIVE_INFINITY,Float.NEGATIVE_INFINITY};
        for(int i=0;i<1000;i++) {
            float before=i<81?edges[i%9]:Float.intBitsToFloat(random.nextInt());
            float after=i<81?edges[i/9]:Float.intBitsToFloat(random.nextInt());
            try(var context=com.thelongtravail.helper.CombatContext.open(target,source)) {
                com.thelongtravail.helper.CombatContext.recordHealthLoss(target,before,after);
                com.thelongtravail.helper.CombatContext.recordAbsorptionLoss(target,before,after);
                com.thelongtravail.helper.CombatContext.recordDummyDamage(target,source,after);
                double total=0,health=0;
                boolean finiteLoss=Float.isFinite(before)&&Float.isFinite(after)&&after<before;
                if(finiteLoss){health+=Math.max(0,Math.max(0,before)-Math.max(0,after));total+=before-after;total+=before-after;}
                if(Float.isFinite(after)&&after>0)total+=after;
                check((boolean)recorded(context,"committed")== (after<before),"health commitment preserves nonfinite boundary");
                check(Double.doubleToLongBits((double)recorded(context,"recordedDamage"))==Double.doubleToLongBits(total),"recorded damage frozen arithmetic");
                check(Double.doubleToLongBits((double)recorded(context,"relicHealthLoss"))==Double.doubleToLongBits(health),"health-only healing arithmetic");
            }
        }
        audit("event exception context cleanup",()->{
            var a=cow();var b=cow();var damage=source(DamageTypes.MOB_ATTACK,a);
            attackProbe=e->{if(e.getEntity()==b)throw new IllegalStateException("injected event failure");};
            boolean threw=false;
            try {b.hurt(damage,5);}catch(IllegalStateException expected){threw=true;}
            check(threw,"event exception escapes");
            check(com.thelongtravail.helper.CombatContext.current(b,damage)==null,"real hurt exception clears context");
        });
    }
    private void audit(String label, Runnable test) {
        try { test.run(); }
        catch (Throwable error) { auditFailures.add(label+": "+error); }
        finally { damageProbe=null; attackProbe=null; cancelTarget=null; replaceTarget=null; amplifyTarget=null; }
    }
    private void auditStates() {
        for (boolean player:new boolean[]{false,true})
        for (int crit:new int[]{0,1})
        for (int insight:new int[]{0,5})
        for (double overlord:new double[]{0,.1})
        for (int armor:new int[]{0,20})
        for (int absorption:new int[]{0,6})
        for (int raw:new int[]{3,10,25}) {
            String label="matrix player="+player+" critical="+crit+" insight="+insight+" overlord="+overlord+" armor="+armor+" absorption="+absorption+" raw="+raw;
            audit(label,()->{
                var a=cow(); LivingEntity b=player?player():cow();
                attr(a,ModAttributes.CRITICAL_CHANCE.get(),crit); attr(b,ModAttributes.INSIGHT.get(),insight);
                attr(b,ModAttributes.OVERLORD.get(),overlord); attr(b,Attributes.ARMOR,armor); b.setAbsorptionAmount(absorption);
                float defended=net.minecraft.world.damagesource.CombatRules.getDamageAfterAbsorb(raw*(crit==1?2:1),armor,0);
                defended=Math.max(0,defended-insight); if(defended<=100*overlord)defended=0;
                hit(b,source(DamageTypes.MOB_ATTACK,a),raw);
                near(b.getHealth(),100-Math.max(0,defended-absorption),label+" HP");
                near(b.getAbsorptionAmount(),Math.max(0,absorption-defended),label+" absorption");
                near(a.getHealth(),100-insight,label+" reflection");
            });
        }
        audit("resistance and protection",()->{
            var a=cow(); var b=cow(); attr(b,ModAttributes.INSIGHT.get(),2); attr(b,ModAttributes.OVERLORD.get(),.1);
            b.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.DAMAGE_RESISTANCE,200,0));
            hit(b,source(DamageTypes.MOB_ATTACK,a),15);
            near(b.getHealth(),100,"resistance before insight and overlord"); near(a.getHealth(),98,"resistance does not remove retaliation");
        });
        audit("totem survival",()->{
            var a=cow(); var b=cow(); attr(b,ModAttributes.INSIGHT.get(),5);
            b.setItemSlot(EquipmentSlot.OFFHAND,new ItemStack(Items.TOTEM_OF_UNDYING));
            hit(b,source(DamageTypes.MOB_ATTACK,a),200);
            check(b.isAlive() && b.getOffhandItem().isEmpty(),"totem consumed and victim alive");
            near(a.getHealth(),95,"observation: totem survivor retaliates");
        });
        audit("cooldown differential",()->{
            var a=cow(); var b=cow(); attr(b,ModAttributes.INSIGHT.get(),5);
            var state=(com.thelongtravail.mixin.DamageReplayStateAccessor)b;
            b.invulnerableTime=15;state.travail$lastHurt(10);
            b.hurt(source(DamageTypes.MOB_ATTACK,a),14);
            near(b.getHealth(),100,"insight subtracts cooldown differential");near(a.getHealth(),95,"cooldown differential full retaliation");
            near(state.travail$lastHurt(),10,"immune differential restores old lastHurt");near(b.invulnerableTime,15,"immune differential keeps cooldown");
            b.hurt(source(DamageTypes.MOB_ATTACK,a),9);near(a.getHealth(),95,"rejected cooldown hit no retaliation");
        });
        audit("nested same victim",()->{
            var a=cow(); var b=cow(); attr(b,ModAttributes.OVERLORD.get(),.1);
            damageProbe=e->{if(e.getEntity()==b){damageProbe=null;b.invulnerableTime=0;b.hurt(b.damageSources().generic(),20);}};
            hit(b,source(DamageTypes.MOB_ATTACK,a),5);
            near(b.getHealth(),80,"nested valid damage retained");near(b.invulnerableTime,20,"outer immunity preserves nested cooldown");
            near(((com.thelongtravail.mixin.DamageReplayStateAccessor)b).travail$lastHurt(),20,"outer immunity preserves nested lastHurt");
        });
        audit("third party synchronous retaliation",()->{
            var a=cow(); var b=cow();attr(a,ModAttributes.INSIGHT.get(),2);attr(b,ModAttributes.INSIGHT.get(),5);
            int before=reflectionCount;
            attackProbe=e->{if(InsightRetaliation.isInsight(e.getSource())){attackProbe=null;b.invulnerableTime=0;b.hurt(source(DamageTypes.MOB_ATTACK,a),10);}};
            hit(b,source(DamageTypes.MOB_ATTACK,a),10);
            check(reflectionCount==before+1,"unmarked synchronous reflection cannot recurse");
            a.invulnerableTime=0;b.invulnerableTime=0;b.hurt(source(DamageTypes.MOB_ATTACK,a),10);
            check(reflectionCount==before+2,"reflection chain scope clears after return");
        });
        audit("ordinary falling anvil",()->{
            var a=cow();var b=cow();attr(b,ModAttributes.INSIGHT.get(),5);
            hit(b,source(DamageTypes.FALLING_ANVIL,a),10);near(a.getHealth(),95,"ordinary anvil still triggers insight");
        });
        audit("thorns marker",()->{
            var a=cow();var b=cow();attr(b,ModAttributes.INSIGHT.get(),5);
            hit(b,source(DamageTypes.THORNS,a),10);near(a.getHealth(),100,"thorns no reflection");near(b.getHealth(),95,"thorns still reduced by insight");
        });
        audit("anvil helmet",()->{
            var a=cow();var b=cow();attr(b,ModAttributes.INSIGHT.get(),8);a.setItemSlot(EquipmentSlot.HEAD,new ItemStack(Items.LEATHER_HELMET));
            hit(b,source(DamageTypes.MOB_ATTACK,a),10);
            var control=cow(); control.setItemSlot(EquipmentSlot.HEAD,new ItemStack(Items.LEATHER_HELMET));
            hit(control,source(DamageTypes.FALLING_ANVIL,b),8);
            near(a.getHealth(),control.getHealth(),"reflection helmet behavior matches ordinary Forge anvil");
            near(a.getHealth(),92,"observation: Forge helmet does not apply additional 25 percent HP reduction");
        });
        audit("later external reduction",()->{
            var a=cow();var b=cow();attr(b,ModAttributes.OVERLORD.get(),.1);replaceTarget=b;replacement=5;
            hit(b,source(DamageTypes.MOB_ATTACK,a),20);near(b.getHealth(),95,"observation: overlord not rechecked after final event");
        });
        audit("later external amplification of immune damage",()->{
            var a=cow();var b=cow();attr(b,ModAttributes.OVERLORD.get(),.1);replaceTarget=b;replacement=50;
            hit(b,source(DamageTypes.MOB_ATTACK,a),5);near(b.getHealth(),100,"external event cannot restore attribute immune damage");
        });
        audit("cancel after absorption",()->{
            var a=cow();var b=cow();attr(b,ModAttributes.INSIGHT.get(),2);b.setAbsorptionAmount(20);cancelTarget=b;cancelStage=3;
            hit(b,source(DamageTypes.MOB_ATTACK,a),10);near(b.getHealth(),100,"cancel after absorption preserves HP");near(b.getAbsorptionAmount(),12,"observation: final cancellation does not refund absorption");near(a.getHealth(),100,"cancel after absorption prevents reflection");
        });
        audit("protection enchantment",()->{
            var a=cow();var b=cow();var helmet=new ItemStack(Items.LEATHER_HELMET);
            helmet.enchant(net.minecraft.world.item.enchantment.Enchantments.ALL_DAMAGE_PROTECTION,4);
            b.setItemSlot(EquipmentSlot.HEAD,helmet);attr(b,ModAttributes.INSIGHT.get(),5);attr(b,ModAttributes.OVERLORD.get(),.1);
            hit(b,source(DamageTypes.MOB_ATTACK,a),20);near(b.getHealth(),88.2,"protection IV before insight and overlord");
        });
        audit("partial critical probability",()->{
            var a=cow();attr(a,ModAttributes.CRITICAL_CHANCE.get(),.5);a.getRandom().setSeed(12345);int criticals=0;
            for(int n=0;n<64;n++){var b=cow();hit(b,source(DamageTypes.MOB_ATTACK,a),10);check(b.getHealth()==90||b.getHealth()==80,"fractional critical produces only one or two times damage "+n);if(b.getHealth()==80)criticals++;}
            check(criticals>10&&criticals<54,"seeded half-chance produces both outcomes");
        });
        audit("PvP retaliation policy",()->{
            var a=player();var b=player();boolean old=level.getServer().isPvpAllowed();
            try{level.getServer().setPvpAllowed(false);InsightRetaliation.hit(b,a,5);near(a.getHealth(),100,"PvP disabled prevents retaliation");
                level.getServer().setPvpAllowed(true);InsightRetaliation.hit(b,a,5);near(a.getHealth(),95,"PvP enabled permits retaliation");}
            finally{level.getServer().setPvpAllowed(old);}
        });
        audit("team friendly fire policy",()->{
            var a=player();var b=player();var board=level.getScoreboard();var team=board.addPlayerTeam("attr_audit");
            boolean old=level.getServer().isPvpAllowed();
            try{level.getServer().setPvpAllowed(true);team.setAllowFriendlyFire(false);board.addPlayerToTeam(a.getScoreboardName(),team);board.addPlayerToTeam(b.getScoreboardName(),team);
                InsightRetaliation.hit(b,a,5);near(a.getHealth(),100,"team disabled friendly fire prevents retaliation");}
            finally{board.removePlayerTeam(team);level.getServer().setPvpAllowed(old);}
        });
        audit("shield block",()->{
            var a=cow();var b=player();a.setPos(0,200,4);b.setYRot(0);attr(b,ModAttributes.INSIGHT.get(),5);
            b.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,new ItemStack(Items.SHIELD));
            b.startUsingItem(net.minecraft.world.InteractionHand.MAIN_HAND);
            for(int n=0;n<6;n++)b.doTick();
            check(b.isBlocking(),"shield fixture actively blocking");
            hit(b,source(DamageTypes.MOB_ATTACK,a),10);near(b.getHealth(),100,"shield blocks HP damage");near(a.getHealth(),100,"shield blocked hit does not retaliate");
        });
        for (boolean player:new boolean[]{false,true}) {
            audit("decimal equality player="+player,()->{
                LivingEntity b=player?player():cow();attr(b,Attributes.MAX_HEALTH,3);b.setHealth(3);attr(b,ModAttributes.OVERLORD.get(),.1);
                hit(b,b.damageSources().generic(),.3f);
                near(b.getHealth(),3,"decimal equal threshold must be immune player="+player);
            });
            for (float raw:new float[]{0,-5,Float.NaN,Float.POSITIVE_INFINITY,Float.NEGATIVE_INFINITY}) {
                audit("malformed initial amount player="+player+" raw="+raw,()->{
                    var a=cow();LivingEntity b=player?player():cow();attr(b,ModAttributes.INSIGHT.get(),2);
                    var state=(com.thelongtravail.mixin.DamageReplayStateAccessor)b;
                    state.travail$lastHurt(4); int before=reflectionCount; int[] events={0};
                    attackProbe=e->{if(e.getEntity()==b)events[0]++;};
                    check(!b.hurt(source(DamageTypes.MOB_ATTACK,a),raw),"invalid input rejected "+player+" "+raw);
                    near(b.invulnerableTime,0,"invalid input no cooldown "+player+" "+raw);
                    near(state.travail$lastHurt(),4,"invalid input preserves lastHurt "+player+" "+raw);
                    check(events[0]==0 && reflectionCount==before,"invalid input no events or retaliation "+player+" "+raw);
                    attackProbe=null;
                    near(b.getHealth(),100,"malformed input HP safe "+player+" "+raw);
                    b.hurt(source(DamageTypes.MOB_ATTACK,a),10);
                    near(b.getHealth(),92,"malformed input must not change next valid hit "+player+" "+raw);
                });
            }
        }
        audit("float next representable threshold",()->{
            var b=cow(); attr(b,Attributes.MAX_HEALTH,3);b.setHealth(3);attr(b,ModAttributes.OVERLORD.get(),.1);
            float above=Math.nextUp(.3f);hit(b,b.damageSources().generic(),above);
            check(b.getHealth()<3,"next representable float above threshold is not immune");
        });
        for (boolean player:new boolean[]{false,true}) audit("execution input exemption player="+player,()->{
            var actor=cow();LivingEntity victim=player?player():cow();var unrelated=cow();
            attr(victim,ModAttributes.INSIGHT.get(),100);attr(victim,ModAttributes.OVERLORD.get(),1);
            boolean[] observed={false};
            attackProbe=e->{if(e.getEntity()==victim && com.thelongtravail.valley.AzraelExecution.matches(victim,e.getSource())){
                attackProbe=null;observed[0]=true;
                check(Float.isInfinite(e.getAmount()),"real execution infinity enters hurt "+player);
                check(!unrelated.hurt(e.getSource(),Float.NaN),"execution source does not exempt another target "+player);
                check(!victim.hurt(victim.damageSources().generic(),Float.NaN),"execution target does not exempt another source "+player);
                near(unrelated.invulnerableTime,0,"nested invalid attack leaves unrelated cooldown unchanged "+player);
            }};
            com.thelongtravail.valley.AzraelExecution.executeForCommand(actor,victim);
            check(observed[0] && !victim.isAlive(),"actual execution still kills with numeric guard "+player);
        });
        audit("creative immunity",()->{
            var a=cow();var b=player();b.getAbilities().invulnerable=true;attr(b,ModAttributes.INSIGHT.get(),5);
            hit(b,source(DamageTypes.MOB_ATTACK,a),10);near(b.getHealth(),100,"creative immune HP");near(a.getHealth(),100,"creative immune no retaliation");
        });
    }
    private LivingEntity registeredMaid() {
        var type=net.minecraftforge.registries.ForgeRegistries.ENTITY_TYPES.getValue(
                net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("touhou_little_maid","maid"));
        check(type!=null && net.minecraftforge.registries.ForgeRegistries.ENTITY_TYPES.getKey(type).toString().equals("touhou_little_maid:maid"),"maid entity type registered");
        var entity=type.create(level);
        check(entity instanceof LivingEntity && entity.getClass().getName().equals("com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid"),"real maid entity constructed");
        return init((LivingEntity)entity);
    }
    private void registrationChecks() {
        boolean loaded=net.minecraftforge.fml.ModList.get().isLoaded("touhou_little_maid");
        check(loaded==Boolean.getBoolean("travail.attributes.expectMaid"),"optional maid dependency matches requested test mode");
        check(loaded || !net.minecraftforge.registries.ForgeRegistries.ENTITY_TYPES.containsKey(
                net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("touhou_little_maid","maid")),"absent maid creates no placeholder entity");
        audit("player attribute registration and save",()->attributeRoundTrip(player(),"player"));
        if(!loaded)return;
        audit("maid attribute registration and save",()->attributeRoundTrip(registeredMaid(),"maid"));
        audit("maid insight retaliation critical",()->{
            var maid=registeredMaid();var attacker=cow();attr(maid,ModAttributes.INSIGHT.get(),5);attr(maid,ModAttributes.CRITICAL_CHANCE.get(),1);
            hit(maid,source(DamageTypes.MOB_ATTACK,attacker),12);
            near(maid.getHealth(),93,"real maid insight reduces damage");near(attacker.getHealth(),90,"real maid retaliation can critical");
        });
        audit("maid overlord",()->{
            var maid=registeredMaid();attr(maid,ModAttributes.OVERLORD.get(),.1);maid.setAbsorptionAmount(20);
            hit(maid,maid.damageSources().generic(),10);near(maid.getHealth(),100,"real maid overlord equality HP");near(maid.getAbsorptionAmount(),20,"real maid overlord protects absorption");
            hit(maid,maid.damageSources().generic(),11);near(maid.getAbsorptionAmount(),9,"real maid above overlord threshold full absorption cost");
        });
        audit("maid outgoing critical",()->{
            var maid=registeredMaid();var victim=cow();attr(maid,ModAttributes.CRITICAL_CHANCE.get(),1);
            hit(victim,source(DamageTypes.MOB_ATTACK,maid),10);near(victim.getHealth(),80,"real maid outgoing critical");
        });
        audit("maid mutual retaliation",()->{
            var first=registeredMaid();var second=registeredMaid();attr(first,ModAttributes.INSIGHT.get(),2);attr(second,ModAttributes.INSIGHT.get(),5);int before=reflectionCount;
            hit(second,source(DamageTypes.MOB_ATTACK,first),12);
            near(first.getHealth(),97,"real maid reflection defended by other maid");check(reflectionCount==before+1,"real maids do not recurse");
        });
        audit("maid execution bypass",()->{
            var maid=registeredMaid();attr(maid,ModAttributes.INSIGHT.get(),100);attr(maid,ModAttributes.OVERLORD.get(),1);
            com.thelongtravail.valley.AzraelExecution.executeForCommand(cow(),maid);
            check(!maid.isAlive(),"real maid execution bypasses attributes");
        });
    }
    private void attributeRoundTrip(LivingEntity entity,String label) {
        var attrs=List.of(ModAttributes.INSIGHT.get(),ModAttributes.CRITICAL_CHANCE.get(),ModAttributes.OVERLORD.get());
        double[] values={5,.25,.1};
        for(int i=0;i<attrs.size();i++) {
            var attribute=attrs.get(i);var instance=entity.getAttribute(attribute);
            check(instance!=null,label+" has attribute "+i);near(instance.getBaseValue(),0,label+" attribute default zero "+i);
            check(attribute.isClientSyncable(),label+" attribute syncable "+i);
            var id=net.minecraftforge.registries.ForgeRegistries.ATTRIBUTES.getKey(attribute);
            int success=level.getServer().getCommands().performPrefixedCommand(entity.createCommandSourceStack().withPermission(2).withSuppressedOutput(),"attribute @s "+id+" base set "+values[i]);
            check(success>0,label+" attribute command succeeds "+i);near(instance.getValue(),values[i],label+" command effective "+i);
        }
        var tag=entity.saveWithoutId(new net.minecraft.nbt.CompoundTag());
        LivingEntity restored=label.equals("maid")?registeredMaid():player();restored.load(tag);
        for(int i=0;i<attrs.size();i++)near(restored.getAttributeValue(attrs.get(i)),values[i],label+" attribute survives NBT reload "+i);
    }
    private void tick(TickEvent.ServerTickEvent e) {
        if(verifyTick==0 || e.phase!=TickEvent.Phase.END || e.getServer().getTickCount()<verifyTick)return;
        verifyTick=0;
        try {near(replayImmune.getHealth(),100,"replay current overlord");near(replayNormal.getHealth(),89,"replay no reroll or insight");done(auditFailures.isEmpty()?null:new AssertionError(String.join("\n",auditFailures)));}
        catch(Throwable error){done(error);}
    }
    private void done(Throwable error) {
        if(error!=null)error.printStackTrace();
        try {java.nio.file.Files.writeString(java.nio.file.Path.of("combat-attributes-result.txt"),(error==null?"PASS":"FAIL")+" checks="+checks.size()+"\n"+String.join("\n",checks)+(error==null?"":"\n"+error));}
        catch(Exception failure){throw new RuntimeException(failure);}
        System.out.println("COMBAT_ATTRIBUTES_"+(error==null?"PASS":"FAIL")+" checks="+checks.size());
        for(var e:entities)e.discard(); level.getServer().halt(false);
    }
}
