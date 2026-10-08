package com.thelongtravail.farreach;

import com.thelongtravail.registry.ModRegistry;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import top.theillusivec4.curios.api.CuriosApi;

// 飞行能力取决于背部功能槽的装备状态，显示开关仅控制模型。
public final class IcarusFlight {
    private IcarusFlight() {}
    public static boolean equipped(LivingEntity entity) {
        return entity instanceof Player player && FarReachEquipment.equipped(player, false);
    }
    public static boolean allowed(LivingEntity entity) {
        return !net.minecraftforge.fml.ModList.get().isLoaded("caelus") || !IcarusCaelusCompat.denied(entity);
    }
    public static ItemStack visibleStack(LivingEntity entity) {
        if (!(entity instanceof Player player) || !equipped(player) || player.isInvisible()) return ItemStack.EMPTY;
        return CuriosApi.getCuriosInventory(player).map(inv -> inv.findCurios(ModRegistry.ICARUS.get()).stream()
                .filter(r -> !r.slotContext().cosmetic() && r.slotContext().identifier().equals("back") && r.slotContext().visible())
                .map(r -> r.stack()).findFirst().orElse(ItemStack.EMPTY)).orElse(ItemStack.EMPTY);
    }
}
