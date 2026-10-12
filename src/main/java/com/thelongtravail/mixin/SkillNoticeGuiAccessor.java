package com.thelongtravail.mixin;

import net.minecraft.client.gui.Gui;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(Gui.class)
public interface SkillNoticeGuiAccessor {
    @Accessor("overlayMessageString")
    Component travail$getOverlayMessage();

    @Accessor("overlayMessageString")
    void travail$setOverlayMessage(Component message);

    @Accessor("overlayMessageTime")
    int travail$getOverlayMessageTime();
}
