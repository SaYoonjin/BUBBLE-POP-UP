package com.ssafy.S14P21A205.game.day.policy;

public enum PopulationLevel {
    VERY_CROWDED(17, 20, "\uB9E4\uC6B0 \uD63C\uC7A1"),
    CROWDED(13, 16, "\uD63C\uC7A1"),
    NORMAL(9, 12, "\uBCF4\uD1B5"),
    RELAXED(5, 8, "\uC5EC\uC720"),
    VERY_RELAXED(0, 4, "\uB9E4\uC6B0 \uC5EC\uC720");

    private static final int MIN_SCORE = 0;
    private static final int MAX_SCORE = 20;

    private final int minInclusive;
    private final int maxInclusive;
    private final String label;

    PopulationLevel(int minInclusive, int maxInclusive, String label) {
        this.minInclusive = minInclusive;
        this.maxInclusive = maxInclusive;
        this.label = label;
    }

    public String label() {
        return label;
    }

    public static PopulationLevel fromScore(Integer score) {
        int normalizedScore = score == null ? MIN_SCORE : Math.max(MIN_SCORE, Math.min(score, MAX_SCORE));
        for (PopulationLevel level : values()) {
            if (normalizedScore >= level.minInclusive && normalizedScore <= level.maxInclusive) {
                return level;
            }
        }
        return VERY_RELAXED;
    }
}