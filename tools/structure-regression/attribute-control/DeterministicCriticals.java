package travail.apotheosisfixture;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

// 精确伤害测试关闭默认随机暴击，保留显式100%暴击用例。
@Mod.EventBusSubscriber(modid = "travail_smoke")
public final class DeterministicCriticals {
    private static boolean logged;

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void hurt(LivingHurtEvent event) {
        if (!(event.getSource().getEntity() instanceof LivingEntity attacker)) return;
        var type =
                ForgeRegistries.ATTRIBUTES.getValue(
                        new ResourceLocation("attributeslib", "crit_chance"));
        var attribute = type == null ? null : attacker.getAttribute(type);
        if (attribute != null && Math.abs(attribute.getBaseValue() - 0.05) < 0.00001) {
            attribute.setBaseValue(0);
            if (!logged) {
                logged = true;
                System.out.println(
                        "APOTHEOSIS_TEST_CONTROL default random crit 0.05 -> 0; explicit crit cases preserved");
            }
        }
    }
}
