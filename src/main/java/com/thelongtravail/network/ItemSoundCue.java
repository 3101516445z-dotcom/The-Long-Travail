package com.thelongtravail.network;

import com.thelongtravail.TravailAspect;
/** Only successful server-side item actions send this S2C cue. No client class references. */
public enum ItemSoundCue {
    HOMECOMING, RENEWAL, FLOURISHING, ABYSS, FAR_REACH, DEEP_VALLEY, UNDERWORLD, BOUNDLESS;
    public static java.util.function.Consumer<ItemSoundCue> receiver = cue -> {};
    public static ItemSoundCue stone(TravailAspect aspect) {
        return switch (aspect) {
            case FLOURISHING -> FLOURISHING;
            case ABYSS -> ABYSS;
            case FAR_REACH -> FAR_REACH;
            case DEEP_VALLEY -> DEEP_VALLEY;
            case UNDERWORLD -> UNDERWORLD;
            case BOUNDLESS -> BOUNDLESS;
        };
    }
}
