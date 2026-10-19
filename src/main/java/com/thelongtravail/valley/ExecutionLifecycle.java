package com.thelongtravail.valley;

import com.thelongtravail.TheLongTravail;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = TheLongTravail.MODID)
public final class ExecutionLifecycle {
    @SubscribeEvent(priority = EventPriority.LOWEST, receiveCanceled = true)
    public static void joining(EntityJoinLevelEvent event) {
        if (ExecutionCleanup.quarantined(event.getEntity())) event.setCanceled(true);
    }

    private ExecutionLifecycle() {}
}
