package com.thelongtravail.combat;

/** 单次伤害的分派资格；实例认领仍由 CombatContext 按原顺序完成。 */
public record DamagePolicy(
        boolean sunStrike,
        boolean quixoteStrike,
        boolean replay,
        boolean insightRetaliation,
        boolean lorentzStrike,
        boolean execution) {

    public boolean settled() {
        return replay || quixoteStrike;
    }

    public boolean commonConfirmation() {
        return !sunStrike && !quixoteStrike && !replay && !insightRetaliation && !lorentzStrike;
    }

    public boolean commonFinish() {
        return !execution && commonConfirmation();
    }

    public boolean attackAttempt(boolean rose) {
        return !rose && commonFinish();
    }

    public boolean quixoteSnapshot() {
        return commonFinish();
    }

    // 逐日与洛伦兹仍采样，但在各自收尾分支返回，不执行吸血。
    public boolean relicSnapshot(boolean rose) {
        return !execution && !replay && !quixoteStrike && !insightRetaliation && !rose;
    }

    public boolean outgoingBonus() {
        return !sunStrike && !quixoteStrike && !insightRetaliation && !lorentzStrike;
    }

    // 保持逐日优先于处决/重放早退的顺序，避免合并判断改变重叠身份行为。
    public boolean finishAttributes() {
        return sunStrike || !execution && !replay && !quixoteStrike && !insightRetaliation;
    }
}
