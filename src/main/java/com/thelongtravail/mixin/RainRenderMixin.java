package com.thelongtravail.mixin;
import com.thelongtravail.abyss.PrecipitationScope;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.*;
import org.spongepowered.asm.mixin.Mixin;
@Mixin(LevelRenderer.class)
public abstract class RainRenderMixin {
    @WrapMethod(method="renderSnowAndRain")
    private void travail$render(LightTexture light,float tick,double x,double y,double z,Operation<Void> original) {
        try(var scope=new PrecipitationScope(Minecraft.getInstance().level)) { original.call(light,tick,x,y,z); }
    }
    @WrapMethod(method="tickRain")
    private void travail$particles(Camera camera,Operation<Void> original) {
        try(var scope=new PrecipitationScope(Minecraft.getInstance().level)) { original.call(camera); }
    }
}
