package com.thelongtravail.farreach;

import com.thelongtravail.TheLongTravail;
import com.thelongtravail.api.TravailStateApi;
import com.thelongtravail.boundless.TimeStopManager;
import com.thelongtravail.config.FarReachItemsConfig;
import com.thelongtravail.helper.TravailCurios;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.enchantment.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.event.entity.item.ItemTossEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.*;
import net.minecraftforge.fml.common.Mod;
import java.util.*;

@Mod.EventBusSubscriber(modid=TheLongTravail.MODID)
public final class GoldenAgeActions {
    private static final String OWNER="LongTravailGoldThrower",WET="LongTravailGoldWetTicks";
    private static final ThreadLocal<UUID> MANUAL=new ThreadLocal<>();
    private static final Map<Player,Long> DIG_COOLDOWNS=new WeakHashMap<>();
    private static final Map<ServerPlayer,Batch> BATCHES=new WeakHashMap<>();
    private static final Set<UUID> PROCESSING=new HashSet<>();
    private static final class Batch {
        final long tick;int used;List<Enchantment> enchantments;List<ItemStack> foods;
        Batch(long tick){this.tick=tick;}
    }
    public static final class ManualScope implements AutoCloseable {
        private final UUID previous;
        private ManualScope(Player player){previous=MANUAL.get();MANUAL.set(player.getUUID());}
        @Override public void close(){if(previous==null)MANUAL.remove();else MANUAL.set(previous);}
    }
    public static ManualScope manual(Player p){return new ManualScope(p);}
    @SubscribeEvent(priority=EventPriority.LOWEST)
    public static void toss(ItemTossEvent event){
        if(event.getPlayer() instanceof ServerPlayer player&&player.getUUID().equals(MANUAL.get())&&FarReachEquipment.equipped(player,true)&&!TimeStopManager.frozen(player))register(event.getEntity(),player);
    }
    // 仅在手动丢弃通过权限检查后调用。
    public static void register(ItemEntity item,ServerPlayer player){
        if(item.getItem().is(Items.GOLD_BLOCK)){item.getPersistentData().putUUID(OWNER,player.getUUID());item.getPersistentData().putInt(WET,0);}
    }
    public static boolean pending(ItemEntity item){return item.getItem().is(Items.GOLD_BLOCK)&&item.getPersistentData().hasUUID(OWNER);}
    // 在 Forge 完成 RightClickBlock 事件分发后调用，确保后执行的保护监听器也已处理。
    public static void dig(PlayerInteractEvent.RightClickBlock event){
        Player p=event.getEntity();
        if(event.isCanceled()||event.getHand()!=InteractionHand.MAIN_HAND||!p.getMainHandItem().isEmpty()||!FarReachEquipment.equipped(p,true)||TimeStopManager.frozen(p)
                ||event.getUseBlock()==Event.Result.DENY||event.getUseItem()==Event.Result.DENY||!p.mayBuild()||!p.level().mayInteract(p,event.getPos()))return;
        var block=p.level().getBlockState(event.getPos());if(!block.is(Blocks.SAND)&&!block.is(Blocks.RED_SAND)&&!block.is(Blocks.GRAVEL))return;
        // 两端均拦截符合条件的交互，包括冷却期间的交互，避免继续触发副手操作。
        event.setCanceled(true);event.setCancellationResult(InteractionResult.sidedSuccess(p.level().isClientSide));
        if(!(p instanceof ServerPlayer player))return;
        long now=p.level().getGameTime();if(now<DIG_COOLDOWNS.getOrDefault(p,Long.MIN_VALUE))return;
        DIG_COOLDOWNS.put(p,now+FarReachItemsConfig.ticks("golden_age.digCooldown"));
        TravailCurios.returnToInventoryOrDrop(player,new ItemStack(player.getRandom().nextBoolean()?Items.GOLD_NUGGET:Items.FLINT));
    }
    public static void tick(ItemEntity item){
        if(item.level().isClientSide||!item.isAlive()||!pending(item)||TimeStopManager.frozen(item))return;
        UUID owner=item.getPersistentData().getUUID(OWNER);
        if(!PROCESSING.add(owner))return;
        try{tickRegistered(item);}finally{PROCESSING.remove(owner);}
    }
    private static void tickRegistered(ItemEntity item){
        CompoundTag data=item.getPersistentData();
        if(!item.isInWater()){data.putInt(WET,0);return;}
        int required=FarReachItemsConfig.ticks("golden_age.waterTime");int wet=Math.min(required,data.getInt(WET)+1);data.putInt(WET,wet);if(wet<required)return;
        Player owner=item.level().getPlayerByUUID(data.getUUID(OWNER));
        if(!(owner instanceof ServerPlayer player)||!FarReachEquipment.equipped(player,true)||TimeStopManager.frozen(player)
                ||item.distanceToSqr(player)>Math.pow(FarReachItemsConfig.get("golden_age.range"),2))return;
        boolean witness=TravailStateApi.hasFarReachWitness(player);if(!witness&&!TravailStateApi.hasFarReachMalice(player))return;
        long now=item.level().getGameTime();Batch batch=BATCHES.get(player);if(batch==null||batch.tick!=now){batch=new Batch(now);BATCHES.put(player,batch);}
        int available=(int)FarReachItemsConfig.get("golden_age.batchSize")-batch.used;if(available<=0)return;
        if(witness&&batch.foods==null)batch.foods=GoldenAgePools.foods(player);
        if(!witness&&batch.enchantments==null)batch.enchantments=GoldenAgePools.enchantments();
        if(witness?batch.foods.isEmpty():batch.enchantments.isEmpty())return;
        // 模组回调中的重载仅使下一批次重新获取物品池，当前批次继续使用不可变快照。
        List<ItemStack> foods=batch.foods;List<Enchantment> enchantments=batch.enchantments;
        int count=Math.min(available,item.getItem().getCount());
        for(int i=0;i<count&&batch.used<(int)FarReachItemsConfig.get("golden_age.batchSize");i++){
            ItemStack output;
            if(player.getRandom().nextBoolean())output=witness?foods.get(player.getRandom().nextInt(foods.size())).copy():EnchantedBookItem.createForEnchantment(new EnchantmentInstance(enchantments.get(player.getRandom().nextInt(enchantments.size())),1));
            else output=new ItemStack(witness?Items.HONEY_BLOCK:Items.LAPIS_BLOCK);
            ItemEntity result=new ItemEntity(item.level(),item.getX(),item.getY(),item.getZ(),output);
            result.setDeltaMovement(item.getDeltaMovement());result.setDefaultPickUpDelay();
            batch.used++; // 生成失败也占用本刻的处理额度，但不消耗原料。
            if(!item.level().addFreshEntity(result))break;
            ItemStack remainder=item.getItem().copy();remainder.shrink(1);item.setItem(remainder);
            if(remainder.isEmpty()){item.discard();break;}
        }
    }
    public static void reload(){for(Batch batch:BATCHES.values()){batch.foods=null;batch.enchantments=null;}}
    private GoldenAgeActions(){}
}
