public enum Difficulty {
    EASY(1, 50, 10, 1.0),
    MEDIUM(1, 100, 7, 1.5),
    HARD(1, 200, 5, 2.5);

    private final int min;
    private final int max;
    private final int maxTries;
    private final double scoreMultiplier;

    Difficulty(int min, int max, int maxTries, double scoreMultiplier) {
        this.min = min;
        this.max = max;
        this.maxTries = maxTries;
        this.scoreMultiplier = scoreMultiplier;
    }

    public int getMin() { return min; }
    public int getMax() { return max; }
    public int getMaxTries() { return maxTries; }
    public double getScoreMultiplier() { return scoreMultiplier; }

    public static Difficulty fromString(String s) {
        if (s == null) return MEDIUM;
        try {
            return Difficulty.valueOf(s.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return MEDIUM;
        }
    }
}