package com.thelongtravail.item;

import com.thelongtravail.TravailAspect;
import com.thelongtravail.abyss.RainTooltips;
import com.thelongtravail.data.JourneyService;
import com.thelongtravail.helper.ActionMessages;
import com.thelongtravail.network.*;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;
import java.util.List;

public final class AspectEssenceItem extends Item {
    private final TravailAspect aspect;

    public AspectEssenceItem(TravailAspect aspect) {
        super(new Properties().stacksTo(64).rarity(Rarity.RARE));
        this.aspect = aspect;
    }

    @Override
    public Component getName(ItemStack stack) {
        return com.thelongtravail.AspectTheme.forAspect(aspect).name(super.getName(stack));
    }

    @Override
    public void appendHoverText(
            ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        Component theme = Component.translatable("aspect.the_long_travail." + aspect.id());
        for (String line : List.of("malice", "witness")) {
            tooltip.add(
                    RainTooltips.coloredLine(
                            Component.translatable(
                                            "tooltip.the_long_travail.essence." + line + "_heading",
                                            theme)
                                    .withStyle(s -> s.withColor(RainTooltips.BODY_COLOR))));
            tooltip.add(
                    RainTooltips.coloredLine(
                            Component.translatable("tooltip.the_long_travail.essence." + line)
                                    .withStyle(s -> s.withColor(RainTooltips.BODY_COLOR))));
        }
    }

    @Override
    public InteractionResultHolder<ItemStack> use(
            Level level, Player player, InteractionHand hand) {
        if (player.getCooldowns().isOnCooldown(this))
            return InteractionResultHolder.fail(player.getItemInHand(hand));
        if (player instanceof ServerPlayer serverPlayer) {
            var result = JourneyService.essence(serverPlayer, aspect, hand);
            if (!result.changed()) {
                player.displayClientMessage(
                        ActionMessages.text(
                                "message.the_long_travail.journey."
                                        + result.status()
                                                .name()
                                                .toLowerCase(java.util.Locale.ROOT)),
                        true);
                return InteractionResultHolder.fail(player.getItemInHand(hand));
            }
            player.getCooldowns().addCooldown(this, 20);
            TravailNetwork.sendItemSound(
                    serverPlayer,
                    result.resetAspects().isEmpty()
                            ? ItemSoundCue.stone(aspect)
                            : ItemSoundCue.RENEWAL);
            player.displayClientMessage(
                    ActionMessages.text(
                            result.resetAspects().isEmpty()
                                    ? "message.the_long_travail.essence.completed"
                                    : "message.the_long_travail.essence.reset",
                            Component.translatable("aspect.the_long_travail." + aspect.id())),
                    true);
        }
        return InteractionResultHolder.sidedSuccess(player.getItemInHand(hand), level.isClientSide);
    }
}
