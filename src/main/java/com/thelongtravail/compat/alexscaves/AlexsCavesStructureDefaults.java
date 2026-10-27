package com.thelongtravail.compat.alexscaves;

import com.thelongtravail.TravailAspect;

import java.util.List;

public final class AlexsCavesStructureDefaults {
    public static List<String> structures(TravailAspect aspect) {
        // 姜饼镇会在放置阶段调整位置，当前不纳入结构预设。
        return List.of();
    }

    private AlexsCavesStructureDefaults() {}
}
