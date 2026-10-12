package com.thelongtravail.loot;

import com.thelongtravail.TheLongTravail;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = TheLongTravail.MODID)
public final class LootReloadEvents {
    @SubscribeEvent
    public static void beforeResourceReload(AddReloadListenerEvent event) {
        // 此事件早于资源重载任务启动；同步读取，确保战利品表反序列化使用本次规则。
        // 不能放进后置重载监听器，否则可能晚于战利品表加载。
        UnifiedLoot.reload();
    }

    private LootReloadEvents() {}
}
