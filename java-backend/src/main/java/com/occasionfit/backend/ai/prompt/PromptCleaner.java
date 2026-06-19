package com.occasionfit.backend.ai.prompt;

public class PromptCleaner {
    public static String clean(String text) {
        if (text == null) return "";

        return text
                .replaceAll("(?i)\\boccasionfit\\s*ai[:\\-]*\\s*", "")
                .replaceAll("(?i)\\boccasionfit[:\\-]*\\s*", "")
                .replaceAll("(?i)\\bassistant[:\\-]*\\s*", "")
                .replaceAll("(?i)\\bai[:\\-]*\\s*", "")
                .replaceAll("(?i)\\buser[:\\-]*\\s*", "")
                .trim();
    }
}
