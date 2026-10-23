package com.thelongtravail.compat.quark;

import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fml.ModList;
import org.violetmoon.quark.base.Quark;
import org.violetmoon.quark.content.tools.base.RuneColor;
import org.violetmoon.quark.content.tools.module.ColorRunesModule;

// 未安装夸克时，不得解析其类。
public final class DiaryRunes {
    public static boolean foil(ItemStack stack) {
        return ModList.get().isLoaded("quark") && Access.foil(stack);
    }

    private static final class Access {
        static boolean foil(ItemStack stack) {
            if (!Quark.ZETA.modules.isEnabled(ColorRunesModule.class)) return false;
            var color = ColorRunesModule.getAppliedStackColor(stack);
            return color != null && color != RuneColor.BLANK;
        }
    }

    private DiaryRunes() {}
}
