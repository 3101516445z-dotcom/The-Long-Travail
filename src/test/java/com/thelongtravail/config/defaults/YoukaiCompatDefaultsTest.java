package com.thelongtravail.config.defaults;

import com.electronwill.nightconfig.core.CommentedConfig;
import com.thelongtravail.TravailAspect;
import com.thelongtravail.compat.youkaishomecoming.YoukaisHomecomingEffectDefaults;

import java.util.List;

public final class YoukaiCompatDefaultsTest {
    private static int checks;

    private static void expect(boolean value, String label) {
        if (!value) throw new AssertionError(label);
        checks++;
    }

    public static void main(String[] args) {
        var home =
                new EffectCompatDefaults.Preset(
                        "youkaishomecoming",
                        "Youkai's Homecoming",
                        YoukaisHomecomingEffectDefaults.farReach(),
                        YoukaisHomecomingEffectDefaults.deepValley());
        for (TravailAspect aspect : List.of(TravailAspect.FAR_REACH, TravailAspect.DEEP_VALLEY)) {
            var effects = CommentedConfig.inMemory();
            var existing = List.of("youkaisfeasts:craby|8|6", "youkaishomecoming:craby|7|9");
            effects.set(EffectCompatDefaults.POOL, existing);
            effects.set("presets.witness.youkaisfeasts.positiveEffectsAdded", true);
            expect(
                    EffectCompatDefaults.apply(effects, aspect, home, true, id -> true),
                    "Other version marker cannot block effect preset");
            List<String> entries = effects.get(EffectCompatDefaults.POOL);
            expect(
                    entries.subList(0, 2).equals(existing),
                    "Distinct namespace levels and weights preserved");
            expect(
                    entries.subList(2, 6).equals(home.entries(aspect).subList(1, 5)),
                    "Only missing Homecoming effects appended");
            expect(entries.size() == 6, "Full ID deduplication");
        }
        System.out.println("YOUKAI_NAMESPACE_PASS checks=" + checks);
    }

    private YoukaiCompatDefaultsTest() {}
}
