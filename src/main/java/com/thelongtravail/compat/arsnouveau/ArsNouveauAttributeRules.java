package com.thelongtravail.compat.arsnouveau;

import java.util.Set;

public final class ArsNouveauAttributeRules {
    private static final Set<String> PHOSPHOPHYLLITE_EXCLUSIONS =
            Set.of("ars_nouveau:ars_nouveau.perk.wixie");

    // 药水持续时间属性不参与磷叶石的两个属性处理阶段。
    public static Set<String> phosphophylliteExclusions() {
        return PHOSPHOPHYLLITE_EXCLUSIONS;
    }

    private ArsNouveauAttributeRules() {}
}
