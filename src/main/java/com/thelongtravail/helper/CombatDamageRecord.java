package com.thelongtravail.helper;

/** Numeric loss accounting for one hurt invocation. Event confirmation stays in CombatContext. */
final class CombatDamageRecord {
    private boolean committed;
    private double recordedDamage;
    private double relicHealthLoss;

    boolean healthLost(float before, float after) {
        if (!(after < before)) return false;
        // Preserve the existing commitment flag even for a nonfinite transition.
        committed = true;
        if (!Float.isFinite(before) || !Float.isFinite(after)) return false;
        relicHealthLoss += Math.max(0, Math.max(0, before) - Math.max(0, after));
        recordedDamage += before - after;
        return true;
    }

    boolean absorptionLost(float before, float after) {
        if (!Float.isFinite(before) || !Float.isFinite(after) || !(after < before)) return false;
        recordedDamage += before - after;
        return true;
    }

    // The caller has already checked server side, context identity and finite positive amount.
    void dummyDamage(float amount) {
        recordedDamage += amount;
    }

    boolean committed() { return committed; }
    double total() { return recordedDamage; }
    double healthLoss() { return relicHealthLoss; }
}
