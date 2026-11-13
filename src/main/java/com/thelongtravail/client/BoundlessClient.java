package com.thelongtravail.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.thelongtravail.boundless.BoundlessEquipment;
import com.thelongtravail.boundless.timestop.TimeStopManager;
import com.thelongtravail.network.BoundlessPacket;
import com.thelongtravail.network.TravailNetwork;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.settings.KeyConflictContext;

import org.lwjgl.glfw.GLFW;

public final class BoundlessClient {
    public static final KeyMapping DREAM =
            new KeyMapping(
                    "key.the_long_travail.dream",
                    KeyConflictContext.IN_GAME,
                    InputConstants.Type.KEYSYM,
                    GLFW.GLFW_KEY_T,
                    "key.categories.the_long_travail");
    private static InputConstants.Key handledDreamKey;
    public static boolean dreaming, cooling;
    public static int cooldown;

    public static boolean press(InputConstants.Key key, int action) {
        if (key.equals(handledDreamKey)) {
            if (action == GLFW.GLFW_REPEAT) return true;
            if (action == GLFW.GLFW_RELEASE) {
                handledDreamKey = null;
                return true;
            }
        }
        var mc = Minecraft.getInstance();
        if (action != GLFW.GLFW_PRESS
                || mc.screen != null
                || !mc.isWindowActive()
                || mc.player == null
                || !mc.player.isAlive()
                || TimeStopManager.frozen(mc.player)
                || !DREAM.isActiveAndMatches(key)) return false;
        if (!dreaming
                && (mc.player.isSpectator()
                        || (!BoundlessEquipment.equipped(mc.player, true)
                                && !com.thelongtravail.boundless.HolyFool.equipped(mc.player))))
            return false;
        handledDreamKey = key;
        TravailNetwork.requestDream();
        return true;
    }

    private static void message(BoundlessPacket.Message packet) {
        if (packet.key().startsWith("message.the_long_travail.holy_fool.")) {
            SkillCooldownNotice.show(packet.key(), packet.cooldownTicks(), false);
            return;
        }
        var player = Minecraft.getInstance().player;
        if (player != null && com.thelongtravail.config.TravailClientConfig.DREAM_MESSAGES.get())
            SkillCooldownNotice.show(packet.key(), packet.cooldownTicks(), true);
    }

    static void register(RegisterKeyMappingsEvent e) {
        e.register(DREAM);
        BoundlessPacket.Message.receiver = BoundlessClient::message;
        com.thelongtravail.boundless.BoundlessTooltips.viewer =
                () -> Minecraft.getInstance().player;
        com.thelongtravail.boundless.BoundlessTooltips.dreamKey =
                () -> ThemeTooltips.keyName(DREAM);
        BoundlessPacket.receiver =
                p -> {
                    dreaming = p.active();
                    cooldown = Math.max(0, p.cooldown());
                    cooling = cooldown > 0;
                };
    }

    static void tick(net.minecraftforge.event.TickEvent.ClientTickEvent e) {
        if (e.phase == net.minecraftforge.event.TickEvent.Phase.END
                && cooldown > 0
                && Minecraft.getInstance().player != null
                && !TimeStopManager.frozen(Minecraft.getInstance().player))
            cooldown = Math.max(cooling ? 1 : 0, cooldown - 1);
    }

    static void clear() {
        dreaming = false;
        cooling = false;
        cooldown = 0;
        handledDreamKey = null;
    }

    private BoundlessClient() {}
}
