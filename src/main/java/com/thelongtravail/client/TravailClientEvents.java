package com.thelongtravail.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.thelongtravail.TheLongTravail;
import com.thelongtravail.client.diary.DiaryActionSounds;
import com.thelongtravail.client.diary.DiaryDialogue;
import com.thelongtravail.client.diary.DiaryInputRouter;
import com.thelongtravail.client.diary.DiarySession;
import com.thelongtravail.client.visual.VisualDeprivationClient;
import com.thelongtravail.client.visual.VisualDeprivationFog;
import com.thelongtravail.compat.sounds.EffectSounds;
import com.thelongtravail.network.JourneySync;

import net.minecraft.client.KeyMapping;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterClientTooltipComponentFactoriesEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.event.RenderTooltipEvent;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.client.settings.KeyConflictContext;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import org.lwjgl.glfw.GLFW;

public final class TravailClientEvents {
    public static final KeyMapping OPEN_DIARY =
            new KeyMapping(
                    "key.the_long_travail.open_diary",
                    KeyConflictContext.GUI,
                    InputConstants.Type.KEYSYM,
                    GLFW.GLFW_KEY_LEFT_SHIFT,
                    "key.categories.the_long_travail");

    @Mod.EventBusSubscriber(
            modid = TheLongTravail.MODID,
            value = Dist.CLIENT,
            bus = Mod.EventBusSubscriber.Bus.MOD)
    public static final class ModBus {
        @SubscribeEvent
        public static void registerEntityRenderers(
                net.minecraftforge.client.event.EntityRenderersEvent.RegisterRenderers event) {
            ClientSetup.registerEntityRenderers(event);
        }

        @SubscribeEvent
        public static void setup(net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent event) {
            ClientSetup.setup(event);
        }

        @SubscribeEvent
        public static void registerKeys(RegisterKeyMappingsEvent event) {
            ClientSetup.registerKeys(event);
        }

        @SubscribeEvent
        public static void registerTooltipComponents(
                RegisterClientTooltipComponentFactoriesEvent event) {
            ClientSetup.registerTooltipComponents(event);
        }
    }

    @Mod.EventBusSubscriber(
            modid = TheLongTravail.MODID,
            value = Dist.CLIENT,
            bus = Mod.EventBusSubscriber.Bus.FORGE)
    public static final class ForgeBus {
        @SubscribeEvent
        public static void tick(net.minecraftforge.event.TickEvent.ClientTickEvent event) {
            if (event.phase == net.minecraftforge.event.TickEvent.Phase.END) {
                EffectSounds.tick();
                DiaryActionSounds.tick();
                VisualDeprivationClient.tick();
            }
        }

        @SubscribeEvent(
                priority = net.minecraftforge.eventbus.api.EventPriority.LOWEST,
                receiveCanceled = true)
        public static void renderFog(
                net.minecraftforge.client.event.ViewportEvent.RenderFog event) {
            VisualDeprivationFog.renderFog(event);
        }

        @SubscribeEvent(priority = net.minecraftforge.eventbus.api.EventPriority.LOWEST)
        public static void fogColor(
                net.minecraftforge.client.event.ViewportEvent.ComputeFogColor event) {
            VisualDeprivationFog.color(event);
        }

        @SubscribeEvent
        public static void logout(
                net.minecraftforge.client.event.ClientPlayerNetworkEvent.LoggingOut event) {
            if (event.getPlayer() != null)
                com.thelongtravail.runtime.effects.StiffState.accept(event.getPlayer(), 0);
            JourneySync.reset();
            DiaryDialogue.clear();
            EffectSounds.clear();
            DiaryActionSounds.clear();
            com.thelongtravail.network.TooltipConfigSync.reset();
            VisualDeprivationClient.reset();
            DiaryInputRouter.clear();
            DiarySession.logout();
        }

        @SubscribeEvent
        public static void tooltipText(ItemTooltipEvent event) {
            DiarySession.tooltipText(event);
        }

        @SubscribeEvent(priority = EventPriority.HIGHEST)
        public static void render(ScreenEvent.Render.Pre event) {
            DiarySession.render(event);
        }

        @SubscribeEvent(priority = EventPriority.LOWEST)
        public static void renderReadingLayer(ScreenEvent.Render.Post event) {
            DiarySession.renderReadingLayer(event);
        }

        @SubscribeEvent(priority = EventPriority.LOWEST)
        public static void tooltip(RenderTooltipEvent.GatherComponents event) {
            DiarySession.tooltip(event);
        }

        @SubscribeEvent(priority = EventPriority.HIGHEST)
        public static void keyPressed(ScreenEvent.KeyPressed.Pre event) {
            DiaryInputRouter.keyPressed(event);
        }

        @SubscribeEvent(priority = EventPriority.HIGHEST, receiveCanceled = true)
        public static void keyReleased(ScreenEvent.KeyReleased.Pre event) {
            DiaryInputRouter.keyReleased(event);
        }

        @SubscribeEvent(priority = EventPriority.HIGHEST)
        public static void character(ScreenEvent.CharacterTyped.Pre event) {
            DiaryInputRouter.character(event);
        }

        @SubscribeEvent(priority = EventPriority.HIGHEST)
        public static void mousePressed(ScreenEvent.MouseButtonPressed.Pre event) {
            DiaryInputRouter.mousePressed(event);
        }

        @SubscribeEvent(priority = EventPriority.HIGHEST, receiveCanceled = true)
        public static void mouseReleased(ScreenEvent.MouseButtonReleased.Pre event) {
            DiaryInputRouter.mouseReleased(event);
        }

        @SubscribeEvent(priority = EventPriority.HIGHEST)
        public static void mouseDragged(ScreenEvent.MouseDragged.Pre event) {
            DiaryInputRouter.mouseDragged(event);
        }

        @SubscribeEvent(priority = EventPriority.HIGHEST)
        public static void scroll(ScreenEvent.MouseScrolled.Pre event) {
            DiaryInputRouter.scroll(event);
        }

        @SubscribeEvent
        public static void screenClosed(ScreenEvent.Closing event) {
            DiarySession.screenClosed(event);
        }
    }

    public static Component holdKeyLabel(KeyMapping binding) {
        return DiaryInputRouter.holdKeyLabel(binding);
    }

    public static boolean suppressBackgroundTooltip() {
        return DiarySession.suppressBackgroundTooltip();
    }

    public static boolean isReading() {
        return DiarySession.isReading();
    }

    private TravailClientEvents() {}
}
