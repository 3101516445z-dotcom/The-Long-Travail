package com.thelongtravail.flourishing;

import com.thelongtravail.common.ItemDelivery;
import com.thelongtravail.registry.ModRegistry;

import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.animal.frog.Frog;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = "the_long_travail")
public final class MoonToadHarvest {
    @SubscribeEvent
    public static void interact(PlayerInteractEvent.EntityInteract event) {
        if (!(event.getTarget() instanceof Frog frog)
                || !frog.isAlive()
                || !event.getItemStack().is(Items.SHEARS)
                || !ModRegistry.MOON_TOAD_PASTRY.isPresent()) return;
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.sidedSuccess(event.getLevel().isClientSide));
        if (event.getLevel().isClientSide) return;
        var player = event.getEntity();
        if (frog.hurt(frog.damageSources().generic(), 1)) {
            event.getItemStack()
                    .hurtAndBreak(1, player, p -> p.broadcastBreakEvent(event.getHand()));
            ItemDelivery.give(player, new ItemStack(ModRegistry.MOON_TOAD_PASTRY.get()));
        }
    }

    private MoonToadHarvest() {}
}
