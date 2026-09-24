import java.io.*;
import java.util.*;

public class ScoreManager {
    private static final String FILE_NAME = "leaderboard.txt";

    public synchronized void addScore(String name, int score, String difficulty) {
        try (FileWriter fw = new FileWriter(FILE_NAME, true);
             BufferedWriter bw = new BufferedWriter(fw)) {
            bw.write(name + "," + score + "," + difficulty);
            bw.newLine();
        } catch (IOException e) {
            System.err.println("Could not save score: " + e.getMessage());
        }
    }

    public synchronized List<String[]> getTopScores(int limit) {
        List<String[]> entries = new ArrayList<>();
        File file = new File(FILE_NAME);
        if (!file.exists()) return entries;

        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = br.readLine()) != null) {
                if (line.isBlank()) continue;
                String[] parts = line.split(",");
                if (parts.length == 3) entries.add(parts);
            }
        } catch (IOException e) {
            System.err.println("Could not read leaderboard: " + e.getMessage());
        }

        entries.sort((a, b) -> Integer.compare(Integer.parseInt(b[1]), Integer.parseInt(a[1])));
        return entries.size() > limit ? entries.subList(0, limit) : entries;
    }
}