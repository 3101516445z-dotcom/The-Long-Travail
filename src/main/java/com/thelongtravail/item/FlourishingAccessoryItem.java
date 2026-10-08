package com.thelongtravail.item;

import com.thelongtravail.AspectTheme;
import com.thelongtravail.config.FlourishingItemsConfig;
import com.thelongtravail.flourishing.*;
import net.minecraft.world.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import net.minecraft.network.chat.Component;

import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.type.capability.ICurioItem;
import java.util.*;

public final class FlourishingAccessoryItem extends Item implements ICurioItem {
    private final boolean spring;
    public FlourishingAccessoryItem(boolean spring) {super(new Properties().stacksTo(1).rarity(Rarity.RARE));this.spring=spring;}
    @Override public Component getName(ItemStack stack) {return AspectTheme.FLOURISHING.name(super.getName(stack));}
    @Override public void onEquip(SlotContext context,ItemStack previous,ItemStack stack) {
        if(!spring&&context.entity() instanceof net.minecraft.server.level.ServerPlayer p)Affection.reset(p);
    }
    @Override public void onUnequip(SlotContext context,ItemStack next,ItemStack stack) {
        if(!spring&&context.entity() instanceof net.minecraft.server.level.ServerPlayer p)Affection.reset(p);
    }
    @Override public boolean canEquipFromUse(SlotContext context,ItemStack stack){return false;}
    @Override public boolean canRightClickEquip(ItemStack stack){return false;}
    @Override public void inventoryTick(ItemStack stack,Level level,Entity entity,int slot,boolean selected){
        if(!spring&&!level.isClientSide&&entity.tickCount%20==0)AffectionDeaths.get(level.getServer()).clean(stack);
    }
    @Override public boolean canEquip(SlotContext context,ItemStack stack) {
        if(!(context.entity() instanceof Player p)) return false;
        var slots=spring?FlourishingItemsConfig.SPRING_SLOTS.get():FlourishingItemsConfig.AFFECTION_SLOTS.get();
        if(p.level().isClientSide) slots=com.thelongtravail.network.TooltipConfigSync.pool(spring?"floral.springSlots":"floral.affectionSlots",slots);
        if(!slots.contains(context.identifier())||(!spring&&!Affection.ownerAllowed(stack,p)))return false;
        return CuriosApi.getCuriosInventory(p).map(inv->inv.findCurios(this).stream().noneMatch(found->
                !found.slotContext().identifier().equals(context.identifier())||found.slotContext().index()!=context.index())).orElse(false);
    }
    @Override public InteractionResult interactLivingEntity(ItemStack stack,Player p,LivingEntity target,InteractionHand hand) {
        if(spring)return InteractionResult.PASS;
        return Affection.bind(stack,p,target);
    }
    @Override public InteractionResultHolder<ItemStack> use(Level level,Player p,InteractionHand hand) {
        ItemStack stack=p.getItemInHand(hand);
        if(!spring&&p.isShiftKeyDown()) {
            if(!level.isClientSide) {Affection.clear(stack);p.displayClientMessage(Component.translatable("message.the_long_travail.affection.cleared"),true);}
            return InteractionResultHolder.sidedSuccess(stack,level.isClientSide);
        }
        return InteractionResultHolder.pass(stack);
    }
    @Override public void appendHoverText(ItemStack stack,Level level,List<Component> lines,TooltipFlag flag) {
        lines.add(Component.empty());
        addLines(lines,spring?"spring_game.rules":"affection.rules",spring?4:2);
        if(spring) {
            var flowers=FlowerData.read(stack);
            if(flowers.isEmpty())lines.add(FloralText.text("tooltip.the_long_travail.spring_game.unselected"));
            lines.add(Component.empty());
            for(int i=0;i<3;i++) {
                String symbol=i==0?"★ ":"☆ ";
                if(i>=flowers.size()) {lines.add(Component.empty().append(FloralText.gold(Component.literal(symbol))).append(FloralText.text("tooltip.the_long_travail.flower.empty")));continue;}
                Flower f=flowers.get(i);
                lines.add(Component.empty().withStyle(style->style.withColor(FloralText.BODY))
                        .append(FloralText.gold(Component.literal(symbol).append(f.item().getDescription())))
                        .append("：").append(FlourishingItemsConfig.shownEnabled(f)?FlowerDescriptions.describe(f):
                                Component.translatable("tooltip.the_long_travail.flower.disabled").withStyle(style->style.withColor(0xAA0000))));
            }
        } else {
            lines.add(Component.empty());
            addLines(lines,"affection.companion",3);
            lines.add(Component.empty());
            addLines(lines,"affection.owner",3);
            double fraction=FlourishingItemsConfig.shown("affection.shareFraction");
            lines.add(com.thelongtravail.network.TooltipConfigSync.decimal("floral.sharing",FlourishingItemsConfig.SHARING.get()?1:0)>0 ? FloralText.text("tooltip.the_long_travail.affection.sharing",FlowerDescriptions.percent(1-fraction),FlowerDescriptions.percent(1-FlourishingItemsConfig.shown("affection.servantShareMultiplier")),FlowerDescriptions.percent(fraction)) : FloralText.text("tooltip.the_long_travail.affection.no_sharing"));
            lines.add(Component.empty());
            lines.add(FloralText.text("tooltip.the_long_travail.affection.range",FlowerDescriptions.number(FlourishingItemsConfig.shown("affection.range"))));
            if(stack.hasTag()&&stack.getTag().hasUUID(Affection.TARGET)) lines.add(Component.empty().append(FloralText.gold(Component.translatable("tooltip.the_long_travail.affection.bound", ""))).append(Component.literal(stack.getTag().getString("BoundName")).withStyle(style->style.withColor(FloralText.BODY))));
        }
    }
    private static void addLines(List<Component> lines,String key,int count) {
        for(int i=0;i<count;i++) {
            lines.add(FloralText.text("tooltip.the_long_travail."+key+"."+i));
        }
    }

}
