package com.thelongtravail.loot;

import com.thelongtravail.TheLongTravail;
import com.thelongtravail.registry.ModRegistry;

import net.minecraft.world.entity.npc.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.trading.*;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.village.VillagerTradesEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = TheLongTravail.MODID)
public final class ArchaeologyTrades {
    public static final VillagerTrades.ItemListing SHEPHERD = (entity, random) -> offer(true);
    public static final VillagerTrades.ItemListing MASON = (entity, random) -> offer(false);

    private static Item result(boolean shepherd) {
        return shepherd ? ModRegistry.DON_QUIXOTE.get() : ModRegistry.PHOSPHOPHYLLITE.get();
    }

    private static MerchantOffer offer(boolean shepherd) {
        return new MerchantOffer(
                shepherd ? new ItemStack(Items.SHIELD) : new ItemStack(Items.LAPIS_LAZULI, 64),
                new ItemStack(shepherd ? Items.NETHERITE_SWORD : Items.GREEN_DYE),
                new ItemStack(result(shepherd)),
                1,
                30,
                0F);
    }

    @SubscribeEvent
    public static void register(VillagerTradesEvent event) {
        var profession = event.getType();
        if (profession != VillagerProfession.SHEPHERD && profession != VillagerProfession.MASON)
            return;
        var listing = profession == VillagerProfession.SHEPHERD ? SHEPHERD : MASON;
        if (!event.getTrades().get(5).contains(listing)) event.getTrades().get(5).add(listing);
    }

    @SubscribeEvent
    public static void loaded(EntityJoinLevelEvent event) {
        if (!event.getLevel().isClientSide
                && event.getEntity() instanceof Villager villager
                && eligible(villager)) villager.getOffers();
    }

    private static boolean eligible(Villager villager) {
        var profession = villager.getVillagerData().getProfession();
        return villager.getVillagerData().getLevel() >= 5
                && (profession == VillagerProfession.SHEPHERD
                        || profession == VillagerProfession.MASON);
    }

    public static void ensure(Villager villager, MerchantOffers offers) {
        if (villager.level().isClientSide || !eligible(villager)) return;
        boolean shepherd =
                villager.getVillagerData().getProfession() == VillagerProfession.SHEPHERD;
        boolean found = false;
        for (var iterator = offers.listIterator(); iterator.hasNext(); ) {
            var current = iterator.next();
            if (!current.getResult().is(result(shepherd))) continue;
            if (found) iterator.remove();
            else {
                found = true;
                if (!shepherd && !fixed(current)) {
                    var tag = current.createTag();
                    tag.put(
                            "buy",
                            new ItemStack(Items.LAPIS_LAZULI, 64)
                                    .save(new net.minecraft.nbt.CompoundTag()));
                    tag.put(
                            "buyB",
                            new ItemStack(Items.GREEN_DYE)
                                    .save(new net.minecraft.nbt.CompoundTag()));
                    iterator.set(new MerchantOffer(tag));
                }
            }
        }
        if (!found) offers.add(offer(shepherd));
    }

    public static boolean fixed(MerchantOffer offer) {
        return (offer.getResult().is(ModRegistry.DON_QUIXOTE.get())
                        && offer.getBaseCostA().is(Items.SHIELD)
                        && offer.getBaseCostA().getCount() == 1
                        && offer.getCostB().is(Items.NETHERITE_SWORD)
                        && offer.getCostB().getCount() == 1)
                || (offer.getResult().is(ModRegistry.PHOSPHOPHYLLITE.get())
                        && offer.getBaseCostA().is(Items.LAPIS_LAZULI)
                        && offer.getBaseCostA().getCount() == 64
                        && offer.getCostB().is(Items.GREEN_DYE)
                        && offer.getCostB().getCount() == 1);
    }

    private ArchaeologyTrades() {}
}
