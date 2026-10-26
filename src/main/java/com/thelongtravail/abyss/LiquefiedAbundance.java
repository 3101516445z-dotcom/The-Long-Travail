package com.thelongtravail.abyss;

import com.thelongtravail.TheLongTravail;
import com.thelongtravail.config.AbyssItemsConfig;
import com.thelongtravail.network.TooltipConfigSync;

import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.util.thread.EffectiveSide;

import java.util.*;
import java.util.function.Consumer;

@Mod.EventBusSubscriber(modid = TheLongTravail.MODID)
public final class LiquefiedAbundance {
    public static final int TAP_TICKS = 5;
    public static final String MODE_KEY = "the_long_travail:abundance_mode";

    private record Session(ItemStack stack, InteractionHand hand, int mode, int duration) {}

    public static int mode(Player player) {
        int value = player.getPersistentData().getInt(MODE_KEY);
        return value >= 0 && value < 3 ? value : 0;
    }

    public static Component modeName(int mode) {
        return Component.translatable("tooltip.the_long_travail.abundance.mode." + mode);
    }

    public static void sync(Player player) {
        if (player instanceof net.minecraft.server.level.ServerPlayer serverPlayer)
            com.thelongtravail.network.TravailNetwork.syncAbundance(serverPlayer, mode(player));
    }

    @SubscribeEvent
    public static void login(
            net.minecraftforge.event.entity.player.PlayerEvent.PlayerLoggedInEvent event) {
        sync(event.getEntity());
    }

    @SubscribeEvent
    public static void respawn(
            net.minecraftforge.event.entity.player.PlayerEvent.PlayerRespawnEvent event) {
        sync(event.getEntity());
    }

    @SubscribeEvent
    public static void clonePlayer(net.minecraftforge.event.entity.player.PlayerEvent.Clone event) {
        event.getEntity().getPersistentData().putInt(MODE_KEY, mode(event.getOriginal()));
    }

    private static final Map<Player, Session> sessions = new IdentityHashMap<>();
    public static Consumer<Player> clientBegin = player -> {};
    private static final String MARKER = "the_long_travail:liquefied_abundance";

    public static int useTicks() {
        double seconds = AbyssItemsConfig.ABUNDANCE_HOLD.get();
        if (EffectiveSide.get().isClient())
            seconds = TooltipConfigSync.decimal("abundance.hold", seconds);
        return Math.max(10, AbundanceEffectPool.ticks(seconds));
    }

    public static void begin(Player player, InteractionHand hand) {
        if (player.level().isClientSide) clientBegin.accept(player);
        else
            sessions.put(
                    player,
                    new Session(player.getItemInHand(hand), hand, mode(player), useTicks()));
        player.startUsingItem(hand);
    }

    public static void cancel(Player player) {
        Session session = sessions.remove(player);
        if (session != null && player.getUseItem() == session.stack()) player.stopUsingItem();
    }

    public static void reload() {
        for (Player player : List.copyOf(sessions.keySet())) player.stopUsingItem();
        sessions.clear();
        AbundanceEffectPool.reload();
    }

    public static ItemStack complete(
            Player player, ItemStack stack, boolean finished, int remaining) {
        if (player.level().isClientSide) return stack;
        Session session = sessions.remove(player);
        if (session == null
                || session.stack() != stack
                || !player.isAlive()
                || player.getItemInHand(session.hand()) != stack
                || stack.isEmpty()) return stack;
        if (!finished && remaining > 0) {
            int elapsed = Math.max(0, session.duration() - remaining);
            if (elapsed <= TAP_TICKS) {
                player.getPersistentData().putInt(MODE_KEY, (session.mode() + 1) % 3);
                sync(player);
            }
            return stack;
        }
        MobEffectInstance effect = AbundanceEffectPool.sample(player.getRandom());
        if (effect == null) {
            player.displayClientMessage(
                    Component.translatable("message.the_long_travail.abundance.empty"), true);
            return stack;
        }
        Item item =
                switch (session.mode()) {
                    case 1 -> Items.SPLASH_POTION;
                    case 2 -> Items.LINGERING_POTION;
                    default -> Items.POTION;
                };
        ItemStack output = potion(item, effect);
        ItemStack result;
        if (player.getAbilities().instabuild) {
            // 创造模式的 Inventory.add 可能丢弃溢出物品，改用统一交付入口。
            com.thelongtravail.common.ItemDelivery.give(player, output);
            result = stack;
        } else result = ItemUtils.createFilledResult(stack, player, output, false);
        player.level()
                .playSound(
                        null,
                        player.blockPosition(),
                        SoundEvents.BREWING_STAND_BREW,
                        SoundSource.PLAYERS,
                        .5F,
                        1.2F);
        return result;
    }

    public static ItemStack potion(Item item, MobEffectInstance effect) {
        ItemStack output = PotionUtils.setPotion(new ItemStack(item), Potions.EMPTY);
        PotionUtils.setCustomEffects(output, List.of(effect));
        output.getOrCreateTag().putInt(MARKER, 1);
        String form =
                item == Items.LINGERING_POTION
                        ? "lingering"
                        : item == Items.SPLASH_POTION ? "splash" : "drink";
        output.setHoverName(
                Component.translatable(
                                "item.the_long_travail.abundance." + form,
                                effect.getEffect().getDisplayName())
                        .withStyle(style -> style.withItalic(false)));
        return output;
    }

    public static MobEffectInstance cloudEffect(ItemStack stack, MobEffectInstance effect) {
        if (!stack.is(Items.LINGERING_POTION)
                || !stack.hasTag()
                || stack.getTag().getInt(MARKER) != 1
                || effect.getEffect().isInstantenous()) return effect;
        int duration = effect.isInfiniteDuration() ? -1 : Math.max(1, effect.getDuration() / 4);
        return new MobEffectInstance(
                effect.getEffect(),
                duration,
                effect.getAmplifier(),
                effect.isAmbient(),
                effect.isVisible(),
                effect.showIcon());
    }

    @SubscribeEvent
    public static void started(ServerStartedEvent event) {
        reload();
    }

    @SubscribeEvent
    public static void stopped(ServerStoppedEvent event) {
        sessions.clear();
        AbundanceEffectPool.clear();
    }

    @SubscribeEvent
    public static void tick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.player.level().isClientSide) return;
        Session session = sessions.get(event.player);
        if (session != null
                && (!event.player.isAlive()
                        || !event.player.isUsingItem()
                        || event.player.getItemInHand(session.hand()) != session.stack()))
            cancel(event.player);
    }

    @SubscribeEvent
    public static void logout(
            net.minecraftforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent event) {
        cancel(event.getEntity());
    }

    @SubscribeEvent
    public static void dimension(
            net.minecraftforge.event.entity.player.PlayerEvent.PlayerChangedDimensionEvent event) {
        cancel(event.getEntity());
        sync(event.getEntity());
    }

    private LiquefiedAbundance() {}
}
