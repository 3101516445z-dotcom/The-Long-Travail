package com.thelongtravail.compat;

import net.minecraft.network.chat.Component;

// 客户端提示分类的装配入口；具体第三方键名保存在对应适配中。
public final class CompatTooltips {
    public static boolean isMaidTooltip(Component component) {
        return com.thelongtravail.compat.maid.MaidTooltipSupport.isMaidTooltip(component);
    }

    private CompatTooltips() {}
}
