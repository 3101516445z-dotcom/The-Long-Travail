package com.thelongtravail.boundless;

import com.thelongtravail.TheLongTravail;
import com.thelongtravail.config.BoundlessItemsConfig;
import com.thelongtravail.registry.ModRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.npc.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.trading.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.village.VillagerTradesEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = TheLongTravail.MODID)
public final class BoundlessAcquisition {
    public static double chance(BlockState state) {
        return state.is(Blocks.POPPY)
                ? BoundlessItemsConfig.get("prince.poppyChance")
                : state.is(Blocks.ROSE_BUSH) ? BoundlessItemsConfig.get("prince.roseChance") : 0;
    }

    public static void broken(ServerPlayer p, BlockPos pos, BlockState before, boolean success) {
        if (!success || p.isCreative() || p.isSpectator() || p.level().getBlockState(pos).equals(before)) return;
        double chance = chance(before);
        if (chance > 0 && p.getRandom().nextDouble() < chance)
            Block.popResource(p.level(), pos, new ItemStack(ModRegistry.LITTLE_PRINCE.get()));
    }

    public static final VillagerTrades.ItemListing LISTING = (entity, random) -> offer();

    private static MerchantOffer offer() {
        return new MerchantOffer(
                new ItemStack(Items.EMERALD, 64),
                new ItemStack(Items.DRAGON_BREATH, 64),
                new ItemStack(ModRegistry.LORENTZ_FANTASY.get()),
                1,
                30,
                0F);
    }

    @SubscribeEvent
    public static void trades(VillagerTradesEvent e) {
        if (e.getType() == VillagerProfession.FLETCHER && !e.getTrades().get(5).contains(LISTING))
            e.getTrades().get(5).add(LISTING);
    }

    @SubscribeEvent
    public static void join(EntityJoinLevelEvent e) {
        if (!e.getLevel().isClientSide
                && e.getEntity() instanceof Villager v
                && v.getVillagerData().getProfession() == VillagerProfession.FLETCHER
                && v.getVillagerData().getLevel() >= 5) v.getOffers();
    }

    public static void ensure(Villager v, MerchantOffers offers) {
        if (v.level().isClientSide
                || v.getVillagerData().getProfession() != VillagerProfession.FLETCHER
                || v.getVillagerData().getLevel() < 5) return;
        boolean found = false;
        for (var it = offers.iterator(); it.hasNext(); )
            if (it.next().getResult().is(ModRegistry.LORENTZ_FANTASY.get())) {
                if (found) it.remove();
                else found = true;
            }
        if (!found) offers.add(offer());
    }

    private BoundlessAcquisition() {}
}
