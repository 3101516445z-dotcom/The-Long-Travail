package com.thelongtravail.mixin;
import com.thelongtravail.abyss.RainWeather;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.commands.WeatherCommand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(WeatherCommand.class)
public abstract class RainCommandMixin {
    @Inject(method={"setClear","setRain","setThunder"},at=@At("HEAD"))
    private static void travail$override(CommandSourceStack source,int duration,CallbackInfoReturnable<Integer> cir) {
        var level=source.getServer().overworld();
        if(RainWeather.get(level).active()) RainWeather.get(level).discard(level);
    }
}
