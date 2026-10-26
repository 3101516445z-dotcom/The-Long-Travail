package com.thelongtravail.boundless;

import com.thelongtravail.TheLongTravail;
import com.thelongtravail.config.BoundlessItemsConfig;
import com.thelongtravail.registry.ModRegistry;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.npc.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.trading.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.village.VillagerTradesEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = TheLongTravail.MODID)
public final class BoundlessAcquisition {
    private static final String PRINCE_PROGRESS = "the_long_travail:prince_progress";

    public static double chance(BlockState state) {
        return state.is(Blocks.POPPY)
                ? BoundlessItemsConfig.get("prince.poppyChance")
                : state.is(Blocks.ROSE_BUSH) ? BoundlessItemsConfig.get("prince.roseChance") : 0;
    }

    public static void broken(ServerPlayer p, BlockPos pos, BlockState before, boolean success) {
        if (!success
                || p.isCreative()
                || p.isSpectator()
                || p.level().getBlockState(pos).equals(before)) return;
        double chance = chance(before);
        boolean guaranteed = recordPrinceBreak(p, before);
        if (guaranteed || (chance > 0 && p.getRandom().nextDouble() < chance))
            Block.popResource(p.level(), pos, new ItemStack(ModRegistry.LITTLE_PRINCE.get()));
    }

    private static boolean recordPrinceBreak(ServerPlayer player, BlockState state) {
        String counter;
        int limit;
        if (state.is(Blocks.ROSE_BUSH)) {
            counter = "roses";
            limit = 300;
        } else if (state.is(Blocks.POPPY)) {
            counter = "poppies";
            limit = 600;
        } else return false;

        CompoundTag data = player.getPersistentData();
        CompoundTag progress = data.getCompound(PRINCE_PROGRESS);
        if (progress.getBoolean("completed")) return false;
        int count = Math.min(limit, Math.max(0, progress.getInt(counter))) + 1;
        boolean guaranteed = count >= limit;
        if (guaranteed) {
            progress.remove("roses");
            progress.remove("poppies");
            progress.putBoolean("completed", true);
        } else progress.putInt(counter, count);
        data.put(PRINCE_PROGRESS, progress);
        return guaranteed;
    }

    @SubscribeEvent
    public static void clone(PlayerEvent.Clone event) {
        CompoundTag original = event.getOriginal().getPersistentData();
        if (original.contains(PRINCE_PROGRESS))
            event.getEntity()
                    .getPersistentData()
                    .put(PRINCE_PROGRESS, original.getCompound(PRINCE_PROGRESS).copy());
    }

    public static final VillagerTrades.ItemListing LISTING = (entity, random) -> offer();

    private static MerchantOffer offer() {
        return new MerchantOffer(
                new ItemStack(Items.DRAGON_BREATH, 16),
                new ItemStack(Items.DRAGON_HEAD),
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
        for (var it = offers.listIterator(); it.hasNext(); ) {
            var current = it.next();
            if (!current.getResult().is(ModRegistry.LORENTZ_FANTASY.get())) continue;
            if (found) it.remove();
            else {
                found = true;
                if (!current.getBaseCostA().is(Items.DRAGON_BREATH)
                        || current.getBaseCostA().getCount() != 16
                        || !current.getCostB().is(Items.DRAGON_HEAD)
                        || current.getCostB().getCount() != 1) {
                    var tag = current.createTag();
                    tag.put("buy", new ItemStack(Items.DRAGON_BREATH, 16).save(new CompoundTag()));
                    tag.put("buyB", new ItemStack(Items.DRAGON_HEAD).save(new CompoundTag()));
                    it.set(new MerchantOffer(tag));
                }
            }
        }
        if (!found) offers.add(offer());
    }

    private BoundlessAcquisition() {}
}
