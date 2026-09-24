import java.util.ArrayList;
import java.util.List;

public class GuessingGame {
    private final String sessionId;
    private final String playerName;
    private final Difficulty difficulty;
    private final int answer;
    private int tries;
    private boolean won;
    private boolean over;
    private final List<Integer> guessHistory;

    public GuessingGame(String sessionId, String playerName, Difficulty difficulty) {
        this.sessionId = sessionId;
        this.playerName = playerName;
        this.difficulty = difficulty;
        this.answer = difficulty.getMin() + (int) (Math.random() * (difficulty.getMax() - difficulty.getMin() + 1));
        this.tries = 0;
        this.won = false;
        this.over = false;
        this.guessHistory = new ArrayList<>();
    }

    public String makeGuess(int num) throws InvalidGuessException {
        if (over) {
            throw new InvalidGuessException("This game is already over. Start a new one.");
        }
        if (num < difficulty.getMin() || num > difficulty.getMax()) {
            throw new InvalidGuessException(
                    "Please enter a number between " + difficulty.getMin() + " and " + difficulty.getMax() + ".");
        }

        tries++;
        guessHistory.add(num);

        String message;
        if (num == answer) {
            won = true;
            over = true;
            message = "Correct! The number was " + answer + ".";
        } else {
            int distance = Math.abs(num - answer);
            String proximity = distance <= 3 ? "Scorching hot! "
                    : distance <= 10 ? "Very close! "
                    : distance <= 25 ? "Getting warmer. "
                    : "Cold. ";
            message = proximity + (num > answer ? "Too high." : "Too low.");
        }

        if (!won && tries >= difficulty.getMaxTries()) {
            over = true;
        }

        return message;
    }

    public int calculateScore() {
        if (!won) return 0;
        int base = (difficulty.getMaxTries() - tries + 1) * 100;
        return (int) Math.round(base * difficulty.getScoreMultiplier());
    }

    public boolean isOver() { return over; }
    public boolean hasWon() { return won; }
    public int getTries() { return tries; }
    public int getAnswer() { return answer; }
    public String getSessionId() { return sessionId; }
    public String getPlayerName() { return playerName; }
    public String getDifficultyName() { return difficulty.name(); }
    public Difficulty getDifficulty() { return difficulty; }
    public List<Integer> getGuessHistory() { return guessHistory; }
}