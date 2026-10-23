package com.thelongtravail.loot;

import com.thelongtravail.TheLongTravail;
import com.thelongtravail.TravailAspect;
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

    private static Item stone() {
        return ModRegistry.REVELATION_STONES.get(TravailAspect.DEEP_VALLEY.ordinal()).get();
    }

    private static Item result(boolean shepherd) {
        return shepherd ? ModRegistry.DON_QUIXOTE.get() : ModRegistry.PHOSPHOPHYLLITE.get();
    }

    private static MerchantOffer offer(boolean shepherd) {
        return new MerchantOffer(
                shepherd ? new ItemStack(Items.SHIELD) : new ItemStack(Items.LAPIS_LAZULI, 64),
                new ItemStack(shepherd ? Items.NETHERITE_SWORD : stone()),
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
        for (var iterator = offers.iterator(); iterator.hasNext(); ) {
            if (!iterator.next().getResult().is(result(shepherd))) continue;
            if (found) iterator.remove();
            else found = true;
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
                        && offer.getCostB().is(stone())
                        && offer.getCostB().getCount() == 1);
    }

    private ArchaeologyTrades() {}
}
