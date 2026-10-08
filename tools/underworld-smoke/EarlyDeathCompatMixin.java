package travail.smoke.mixin;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraftforge.common.ForgeHooks;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 测试夹具模拟 RevelationFix 在 HEAD 处可取消、优先级为 888 的拦截。
 * 此测试不加载外部模组，也不执行其完整仪式。
 */
@Mixin(value=ForgeHooks.class,priority=888,remap=false)
public abstract class EarlyDeathCompatMixin {
    @Inject(method="onLivingDeath",at=@At("HEAD"),cancellable=true,remap=false)
    private static void test$earlyRescue(LivingEntity entity,DamageSource source,CallbackInfoReturnable<Boolean> ci){
        if(entity==travail.smoke.UnderworldSmoke.earlyRescue){
            travail.smoke.UnderworldSmoke.earlyRescue=null;
            entity.setHealth(entity.getMaxHealth());
            ci.setReturnValue(true);
        }
    }
}
