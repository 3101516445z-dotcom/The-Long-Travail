package com.thelongtravail.config;

import com.electronwill.nightconfig.core.CommentedConfig;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.function.Predicate;

final class WeightedPresetEntries {
    static boolean apply(
            CommentedConfig config,
            String pool,
            List<String> preset,
            String marker,
            boolean present,
            Predicate<String> exists) {
        if (!present || preset.isEmpty() || Boolean.TRUE.equals(config.get(marker))) return false;
        List<String> entries = new ArrayList<>(config.<List<String>>get(pool));
        var configured = new HashSet<String>();
        for (String entry : entries) configured.add(entry.split("\\|", -1)[0].trim());
        for (String entry : preset) {
            String id = entry.split("\\|", -1)[0].trim();
            if (exists.test(id) && configured.add(id)) entries.add(entry);
        }
        config.set(pool, entries);
        config.set(marker, true);
        return true;
    }

    private WeightedPresetEntries() {}
}
