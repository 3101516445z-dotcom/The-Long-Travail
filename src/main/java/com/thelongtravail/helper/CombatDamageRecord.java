package com.thelongtravail.helper;

// 记录一次 hurt 调用的伤害损失；事件确认由 CombatContext 负责。
final class CombatDamageRecord {
    private boolean committed;
    private double recordedDamage;
    private double relicHealthLoss;

    boolean healthLost(float before, float after) {
        if (!(after < before)) return false;
        // 检测到生命值下降时先标记提交；非有限数值不计入损失总量。
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

    // 调用方须先确认服务端、上下文身份，以及伤害值为有限正数。
    void dummyDamage(float amount) {
        recordedDamage += amount;
    }

    boolean committed() {
        return committed;
    }

    double total() {
        return recordedDamage;
    }

    double healthLoss() {
        return relicHealthLoss;
    }
}
