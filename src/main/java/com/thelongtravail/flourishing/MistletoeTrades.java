package com.thelongtravail.flourishing;

import com.thelongtravail.registry.ModRegistry;
import net.minecraft.world.entity.npc.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.trading.*;

@net.minecraftforge.fml.common.Mod.EventBusSubscriber(
        modid = com.thelongtravail.TheLongTravail.MODID)
public final class MistletoeTrades {
    public static final VillagerTrades.ItemListing LISTING = (entity, random) -> createOffer();

    private static MerchantOffer createOffer() {
        return new MerchantOffer(
                new ItemStack(Items.BONE_MEAL, 64),
                new ItemStack(Items.GOLDEN_APPLE),
                new ItemStack(ModRegistry.MISTLETOE.get()),
                1,
                30,
                0F);
    }

    @net.minecraftforge.eventbus.api.SubscribeEvent
    public static void register(net.minecraftforge.event.village.VillagerTradesEvent event) {
        if (event.getType() == VillagerProfession.FARMER
                && !event.getTrades().get(5).contains(LISTING))
            event.getTrades().get(5).add(LISTING);
    }

    @net.minecraftforge.eventbus.api.SubscribeEvent
    public static void loaded(net.minecraftforge.event.entity.EntityJoinLevelEvent event) {
        if (!event.getLevel().isClientSide
                && event.getEntity() instanceof Villager villager
                && villager.getVillagerData().getProfession() == VillagerProfession.FARMER
                && villager.getVillagerData().getLevel() >= 5) villager.getOffers();
    }

    public static void ensure(Villager villager, MerchantOffers offers) {
        if (villager.level().isClientSide
                || villager.getVillagerData().getProfession() != VillagerProfession.FARMER
                || villager.getVillagerData().getLevel() < 5) return;
        boolean found = false;
        for (var iterator = offers.listIterator(); iterator.hasNext(); ) {
            var offer = iterator.next();
            if (!offer.getResult().is(ModRegistry.MISTLETOE.get())) continue;
            if (found) iterator.remove();
            else {
                found = true;
                if (offer.getBaseCostA().is(Items.BONE_MEAL)
                        && offer.getBaseCostA().getCount() == 64
                        && offer.getCostB().is(Items.BONE_MEAL)
                        && offer.getCostB().getCount() == 64) {
                    var tag = offer.createTag();
                    tag.put(
                            "buyB",
                            new ItemStack(Items.GOLDEN_APPLE)
                                    .save(new net.minecraft.nbt.CompoundTag()));
                    iterator.set(new MerchantOffer(tag));
                }
            }
        }
        if (!found) offers.add(createOffer());
    }

    public static boolean fixed(MerchantOffer offer) {
        return offer.getResult().is(ModRegistry.MISTLETOE.get())
                && offer.getBaseCostA().is(Items.BONE_MEAL)
                && offer.getBaseCostA().getCount() == 64
                && offer.getCostB().is(Items.GOLDEN_APPLE)
                && offer.getCostB().getCount() == 1;
    }

    private MistletoeTrades() {}
}
