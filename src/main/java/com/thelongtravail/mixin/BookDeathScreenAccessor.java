package com.thelongtravail.mixin;

import java.util.List;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.DeathScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(DeathScreen.class)
public interface BookDeathScreenAccessor {
    @Accessor("exitButtons") List<Button> travail$exitButtons();
    @Accessor("delayTicker") int travail$delayTicker();
}
