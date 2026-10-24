package com.thelongtravail.config;

import com.electronwill.nightconfig.core.CommentedConfig;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.function.Predicate;

final class ExplorationPresetEntries {
    static boolean apply(
            CommentedConfig config,
            String pool,
            List<String> preset,
            String marker,
            String kind,
            boolean present,
            Predicate<String> exists) {
        if (!present || preset.isEmpty() || Boolean.TRUE.equals(config.get(marker))) return false;
        List<String> entries = new ArrayList<>(config.<List<String>>get(pool));
        var configured = new HashSet<String>();
        for (String entry : entries) configured.add(entry.split("\\|", -1)[0].trim());
        for (String id : preset) {
            if (exists.test(id) && configured.add(id))
                entries.add(id + "|@" + kind + "." + id.replace(':', '.') + "|1");
        }
        config.set(pool, entries);
        config.set(marker, true);
        return true;
    }

    private ExplorationPresetEntries() {}
}
