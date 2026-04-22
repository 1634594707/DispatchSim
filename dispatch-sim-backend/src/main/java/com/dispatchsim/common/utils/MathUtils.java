package com.dispatchsim.common.utils;

public final class MathUtils {

    private MathUtils() {
    }

    public static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(value, max));
    }
}
