package com.occasionfit.backend.util;

public class TokenEstimator {
    public static int estimateTokens(String text) {
        if (text == null) return 0;
        return text.length() / 4;
    }
}
