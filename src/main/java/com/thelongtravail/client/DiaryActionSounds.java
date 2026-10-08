package com.thelongtravail.client;

import com.thelongtravail.network.ItemSoundCue;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import java.util.ArrayDeque;

// 仅在本地播放无位置的原版界面音效，不广播到世界，也不预测物品使用结果。
public final class DiaryActionSounds {
    private record Delayed(Object player, Object level, long due) {}
    private static final ArrayDeque<Delayed> DELAYED = new ArrayDeque<>();
    private static long tick, lastSwitch;
    private static boolean switched;
    public static void clear() { DELAYED.clear(); tick = 0; switched = false; }
    private static void play(SoundEvent event, float volume, float pitch) {
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(event, pitch, volume));
    }
    public static void open() { switched = false; play(SoundEvents.BOOK_PAGE_TURN, 0.35F, 0.9F); }
    public static void close() { play(SoundEvents.BOOK_PAGE_TURN, 0.35F, 0.9F); }
    public static void switchPage(boolean bookmark) {
        long now = System.nanoTime();
        if (switched && now - lastSwitch < 80_000_000L) return;
        switched = true; lastSwitch = now;
        if (bookmark) play(SoundEvents.BOOK_PAGE_TURN, 0.25F, 1.15F);
        else play(SoundEvents.UI_BUTTON_CLICK.value(), 0.12F, 1.3F);
    }
    public static void receive(ItemSoundCue cue) {
        var client = Minecraft.getInstance();
        if (client.player == null || client.level == null) return;
        switch (cue) {
            case HOMECOMING -> play(SoundEvents.ARMOR_EQUIP_GENERIC, 0.80F, 1.10F);
            case RENEWAL -> {
                play(SoundEvents.BOOK_PAGE_TURN, 0.35F, 1.2F);
                if (DELAYED.size() >= 16) DELAYED.removeFirst();
                DELAYED.addLast(new Delayed(client.player, client.level, tick + 2));
            }
            default -> play(SoundEvents.AMETHYST_BLOCK_HIT, 0.35F, switch (cue) {
                case FLOURISHING -> 1.15F;
                case ABYSS -> 0.90F;
                case FAR_REACH -> 1.05F;
                case DEEP_VALLEY -> 0.95F;
                case UNDERWORLD -> 0.80F;
                case BOUNDLESS -> 1.25F;
                default -> 1F;
            });
        }
    }
    public static void tick() {
        var client = Minecraft.getInstance();
        if (client.player == null || client.level == null) { clear(); return; }
        if (client.isPaused()) return;
        tick++;
        DELAYED.removeIf(entry -> entry.player != client.player || entry.level != client.level);
        while (!DELAYED.isEmpty() && DELAYED.peekFirst().due <= tick) {
            DELAYED.removeFirst(); play(SoundEvents.BOOK_PUT, 0.40F, 1.1F);
        }
    }
    private DiaryActionSounds() {}
}
