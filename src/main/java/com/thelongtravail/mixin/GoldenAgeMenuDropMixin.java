package com.thelongtravail.mixin;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.thelongtravail.farreach.GoldenAgeActions;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import org.spongepowered.asm.mixin.Mixin;
@Mixin(AbstractContainerMenu.class)
public abstract class GoldenAgeMenuDropMixin {
    @WrapMethod(method="clicked")
    private void travail$menuDrop(int slot,int button,ClickType type,Player p,Operation<Void> original){
        if(type==ClickType.THROW||(slot==-999&&type==ClickType.PICKUP)){try(var scope=GoldenAgeActions.manual(p)){original.call(slot,button,type,p);}}
        else original.call(slot,button,type,p);
    }
}
