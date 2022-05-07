package com.eugene.wc.protocol.account;

import com.eugene.wc.protocol.api.account.PasswordStrengthEstimator;
import com.eugene.wc.protocol.api.account.UserConstants;

public class PasswordStrengthEstimatorImpl implements PasswordStrengthEstimator {

    @Override
    public Strength estimateStrength(String password) {
        int uniqueCharacters = 0;

        for (int i = 0; i < password.length(); i++) {
            String previousChars = password.substring(0, i);
            if (previousChars.indexOf(password.charAt(i)) == -1) {
                uniqueCharacters++;
            }
        }

        double uniqueCharRatio = (double) uniqueCharacters / UserConstants.MIN_PASSWORD_LENGTH;
        Strength strength;
        if (Double.compare(uniqueCharRatio, 0.25) <= 0) {
            strength = Strength.VERY_WEAK;
        } else if (Double.compare(uniqueCharRatio, 0.5) <= 0) {
            strength = Strength.WEAK;
        } else if (Double.compare(uniqueCharRatio, 0.75) <= 0) {
            strength = Strength.MEDIUM;
        } else if (Double.compare(uniqueCharRatio, 1.0) <= 0) {
            strength = Strength.STRONG;
        } else {
            strength = Strength.VERY_STRONG;
        }
        return strength;
    }
}
