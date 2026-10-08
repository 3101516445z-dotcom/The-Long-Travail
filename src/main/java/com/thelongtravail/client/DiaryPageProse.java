package com.thelongtravail.client;

import com.thelongtravail.TravailAspect;
import net.minecraft.network.chat.Component;

// 正文由语言文件保留作者设定的换行和标点。
public final class DiaryPageProse {
    private DiaryPageProse() {}

    public static String text(TravailAspect aspect, boolean witness) {
        return Component.translatable("gui.the_long_travail.diary.prose." + aspect.id()
                + (witness ? ".witness" : ".malice")).getString();
    }
}
