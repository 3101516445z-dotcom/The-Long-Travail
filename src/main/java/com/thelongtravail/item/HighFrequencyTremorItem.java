package com.thelongtravail.item;

import com.thelongtravail.AspectTheme;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import java.util.List;

public final class HighFrequencyTremorItem extends PickaxeItem implements ItemDescription {
    public HighFrequencyTremorItem() {
        super(Tiers.NETHERITE, 1, -2.8F, new Properties().fireResistant().rarity(Rarity.RARE));
    }

    @Override
    public Component getName(ItemStack stack) {
        return AspectTheme.DEEP_VALLEY.name(super.getName(stack));
    }

    @Override
    public void appendHoverText(ItemStack s, Level l, List<Component> out, TooltipFlag f) {
        ItemDescription.summary(out);
    }

    @Override
    public void appendDetails(ItemStack s, Level l, List<Component> out, TooltipFlag f) {
        String prefix = "tooltip.the_long_travail.high_frequency_tremor.";
        for (int i = 0; i < 8; i++) {
            if (i == 1 || i == 3 || i == 6) out.add(Component.empty());
            out.add(Component.translatable(prefix + i));
        }
        var ids =
                com.thelongtravail.network.TooltipConfigSync.pool(
                        "deepValley.tremorBlacklist",
                        com.thelongtravail.config.DeepValleyItemsConfig.TREMOR_BLACKLIST.get());
        var names = Component.empty();
        for (String value : ids) {
            if (!names.getSiblings().isEmpty())
                names.append(
                        Component.translatable("tooltip.the_long_travail.abyss_items.separator"));
            var id = net.minecraft.resources.ResourceLocation.tryParse(value);
            names.append(
                    (id != null
                                            && net.minecraftforge.registries.ForgeRegistries.BLOCKS
                                                    .containsKey(id)
                                    ? net.minecraftforge.registries.ForgeRegistries.BLOCKS
                                            .getValue(id)
                                            .getName()
                                    : Component.literal(value))
                            .withStyle(style -> style.withColor(0x55D6B0)));
        }
        if (ids.isEmpty())
            names.append(
                    Component.translatable(
                                    "tooltip.the_long_travail.abyss_items.fluid.blacklist_unrestricted")
                            .withStyle(style -> style.withColor(0x55D6B0)));
        out.add(Component.translatable(prefix + "8", names));
    }
}
