package com.thelongtravail.farreach;

import com.thelongtravail.api.TravailStateApi;
import com.thelongtravail.config.FarReachRelicsConfig;
import com.thelongtravail.registry.ModRegistry;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.npc.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.trading.*;
import java.util.*;

@net.minecraftforge.fml.common.Mod.EventBusSubscriber(modid = com.thelongtravail.TheLongTravail.MODID)
public final class BabelTrades {
    public static final VillagerTrades.ItemListing LISTING = (entity, random) -> createOffer();

    private static MerchantOffer createOffer() {
        return new MerchantOffer(
                new ItemStack(Items.SADDLE),
                new ItemStack(Items.GOAT_HORN),
                new ItemStack(ModRegistry.TOWER_OF_BABEL.get()),
                1,
                30,
                0);
    }

    @net.minecraftforge.eventbus.api.SubscribeEvent
    public static void register(net.minecraftforge.event.village.VillagerTradesEvent event) {
        if (event.getType() == VillagerProfession.LEATHERWORKER && !event.getTrades().get(5).contains(LISTING))
            event.getTrades().get(5).add(LISTING);
    }

    private static final Map<Villager, Map<MerchantOffer, Integer>> DISCOUNTS = new WeakHashMap<>();

    public static boolean malice(Player p) {
        return p instanceof ServerPlayer s
                && TravailStateApi.hasFarReachMalice(s)
                && !TravailStateApi.hasFarReachWitness(s)
                && BabelCarrying.carrying(s);
    }

    public static boolean eligible(Villager v, Player p) {
        return malice(p) && !BabelProtection.provoked(v, p.getUUID());
    }

    public static boolean nearby(Villager v) {
        double r = FarReachRelicsConfig.value("babel.restockRange");
        return v.level().players().stream().anyMatch(p -> p.distanceToSqr(v) <= r * r && eligible(v, p));
    }

    @net.minecraftforge.eventbus.api.SubscribeEvent
    public static void loaded(net.minecraftforge.event.entity.EntityJoinLevelEvent e) {
        if (!e.getLevel().isClientSide
                && e.getEntity() instanceof Villager v
                && v.getVillagerData().getProfession() == VillagerProfession.LEATHERWORKER
                && v.getVillagerData().getLevel() >= 5) v.getOffers();
    }

    public static void ensure(Villager v, MerchantOffers offers) {
        if (v.level().isClientSide
                || v.getVillagerData().getProfession() != VillagerProfession.LEATHERWORKER
                || v.getVillagerData().getLevel() < 5) return;
        boolean found = false;
        for (var iterator = offers.iterator(); iterator.hasNext(); ) {
            if (!iterator.next().getResult().is(ModRegistry.TOWER_OF_BABEL.get())) continue;
            if (found) iterator.remove();
            else found = true;
        }
        if (!found) offers.add(createOffer());
    }

    public static boolean fixed(MerchantOffer o) {
        return o.getResult().is(ModRegistry.TOWER_OF_BABEL.get())
                && o.getBaseCostA().is(Items.SADDLE)
                && o.getCostB().is(Items.GOAT_HORN);
    }

    public static void forget(Villager v) {
        DISCOUNTS.remove(v);
    }

    public static void remove(Villager v) {
        var old = DISCOUNTS.remove(v);
        if (old != null) old.forEach((o, d) -> o.addToSpecialPriceDiff(-d));
    }

    public static boolean refresh(Villager v, Player p) {
        Map<MerchantOffer, Integer> before = new IdentityHashMap<>();
        for (var o : v.getOffers()) before.put(o, o.getCostA().getCount());
        remove(v);
        if (eligible(v, p)) {
            Map<MerchantOffer, Integer> applied = new IdentityHashMap<>();
            for (var o : v.getOffers()) {
                if (fixed(o) || com.thelongtravail.flourishing.MistletoeTrades.fixed(o)) continue;
                int price = o.getCostA().getCount(),
                        desired = Math.max(1, (int) Math.ceil(price * FarReachRelicsConfig.value("babel.priceFactor")));
                int raw =
                        o.getBaseCostA().getCount()
                                + Math.max(
                                        0,
                                        net.minecraft.util.Mth.floor(
                                                o.getBaseCostA().getCount() * o.getDemand() * o.getPriceMultiplier()))
                                + o.getSpecialPriceDiff();
                int delta = desired - raw;
                o.addToSpecialPriceDiff(delta);
                applied.put(o, delta);
            }
            DISCOUNTS.put(v, applied);
        }
        return before.entrySet().stream().anyMatch(e -> e.getValue() != e.getKey().getCostA().getCount());
    }

    public static void sync(Villager v) {
        if (v.getTradingPlayer() instanceof ServerPlayer p) {
            if (p.containerMenu instanceof com.thelongtravail.mixin.BabelMerchantAccess menu
                    && menu.travail$merchant() == v) {
                menu.travail$container().updateSellItem();
                p.containerMenu.broadcastChanges();
            }
            p.sendMerchantOffers(
                    p.containerMenu.containerId,
                    v.getOffers(),
                    v.getVillagerData().getLevel(),
                    v.getVillagerXp(),
                    v.showProgressBar(),
                    v.canRestock());
        }
    }

    private BabelTrades() {}
}
