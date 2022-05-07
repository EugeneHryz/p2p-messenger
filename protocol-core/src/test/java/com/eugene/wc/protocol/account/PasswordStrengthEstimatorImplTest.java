package com.eugene.wc.protocol.account;

import com.eugene.wc.protocol.api.account.PasswordStrengthEstimator;
import com.eugene.wc.protocol.api.account.PasswordStrengthEstimator.Strength;

import org.junit.Assert;
import org.junit.Test;

public class PasswordStrengthEstimatorImplTest {

    PasswordStrengthEstimator strengthEstimator = new PasswordStrengthEstimatorImpl();

    @Test
    public void testStrengthShouldBeStrong() {
        String password = "acccccaakLo_O";

        Strength expected = Strength.STRONG;
        Strength actual = strengthEstimator.estimateStrength(password);
        Assert.assertEquals(expected, actual);
    }

    @Test
    public void testStrengthShouldBeWeak() {
        String password = "asdasdasd";

        Strength expected = Strength.WEAK;
        Strength actual = strengthEstimator.estimateStrength(password);
        Assert.assertEquals(expected, actual);
    }

    @Test
    public void testStrengthShouldBeVeryStrong() {
        String password = "jmqiX-162";

        Strength expected = Strength.VERY_STRONG;
        Strength actual = strengthEstimator.estimateStrength(password);
        Assert.assertEquals(expected, actual);
    }

    @Test
    public void testStrengthShouldBeVeryWeak() {
        String password = "aaaappppppp";

        Strength expected = Strength.VERY_WEAK;
        Strength actual = strengthEstimator.estimateStrength(password);
        Assert.assertEquals(expected, actual);
    }
}
