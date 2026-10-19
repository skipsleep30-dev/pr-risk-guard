package com.ankit.prguard.shared;

public enum RiskLevel {
    LOW,
    MEDIUM,
    HIGH,
    CRITICAL;

    public boolean requiresHumanReview() {
        return this == HIGH || this == CRITICAL;
    }
}
