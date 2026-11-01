package com.thelongtravail.client.catalog;

import com.thelongtravail.config.FarReachItemsConfig;
import com.thelongtravail.network.TooltipConfigSync;
import net.minecraft.network.chat.Component;
import java.math.BigDecimal;

// 穷遐补充卡片使用与物品描述相同的服务端配置快照。
public final class CatalogFarReachCards {
    private static String value(String key, int scale) {
        return BigDecimal.valueOf(
                        TooltipConfigSync.decimal("farItem." + key, FarReachItemsConfig.get(key)))
                .multiply(BigDecimal.valueOf(scale))
                .stripTrailingZeros()
                .toPlainString();
    }

    public static Component body(String id) {
        String prefix = "gui.the_long_travail.catalog.glossary.";
        if (id.equals("gold_panning"))
            return Component.translatable(
                    prefix + id + ".body", value("golden_age.digGoldChance", 100));
        boolean harmful =
                TooltipConfigSync.integer(
                                "farItem.golden_age.allowHarmfulFood",
                                FarReachItemsConfig.ALLOW_HARMFUL.get() ? 1 : 0)
                        != 0;
        return Component.translatable(
                prefix + "delicacy." + (harmful ? "allowed" : "body"),
                value("golden_age.minNutrition", 1),
                value("golden_age.minSaturation", 1));
    }

    private CatalogFarReachCards() {}
}
