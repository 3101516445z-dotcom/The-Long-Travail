package com.thelongtravail.compat.maid;

import java.util.List;

public final class MaidFluidDefaults {
    public record Rule(String mod, List<String> items, String flag) {}

    public static List<Rule> rules() {
        return List.of(
                new Rule(
                        "touhou_little_maid",
                        List.of(
                                "touhou_little_maid:smart_slab_init",
                                "touhou_little_maid:smart_slab_has_maid"),
                        "maidSoulAdded"),
                new Rule(
                        "touhou_little_maid_personality",
                        List.of("touhou_little_maid_personality:monster_battle_device"),
                        "monsterBattleDeviceAdded"));
    }

    private MaidFluidDefaults() {}
}
