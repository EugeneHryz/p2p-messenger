package com.eugene.wc.protocol.api.account;

public interface PasswordStrengthEstimator {

    enum Strength {
        VERY_WEAK,
        WEAK,
        MEDIUM,
        STRONG,
        VERY_STRONG
    }

    Strength estimateStrength(String password);
}
