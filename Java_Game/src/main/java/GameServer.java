import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.*;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.*;

public class GameServer {
    private static final Map<String, GuessingGame> sessions = new ConcurrentHashMap<>();
    private static final ScoreManager scoreManager = new ScoreManager();

    public static void main(String[] args) throws IOException {
        int port = 8080;
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);

        server.createContext("/api/new", GameServer::handleNewGame);
        server.createContext("/api/guess", GameServer::handleGuess);
        server.createContext("/api/leaderboard", GameServer::handleLeaderboard);
        server.createContext("/", GameServer::handleStatic);

        server.setExecutor(null);
        server.start();
        System.out.println("Number Guessing Game server running at http://localhost:" + port);
    }

    // ---- Route handlers ----

    private static void handleNewGame(HttpExchange ex) throws IOException {
        if (!"POST".equals(ex.getRequestMethod())) {
            sendJson(ex, 405, "{\"error\":\"Method not allowed\"}");
            return;
        }
        String body = readBody(ex);
        String name = extractString(body, "name", "Player");
        Difficulty difficulty = Difficulty.fromString(extractString(body, "difficulty", "MEDIUM"));

        String sessionId = UUID.randomUUID().toString();
        GuessingGame game = new GuessingGame(sessionId, name, difficulty);
        sessions.put(sessionId, game);

        String json = String.format(
                "{\"sessionId\":\"%s\",\"min\":%d,\"max\":%d,\"maxTries\":%d,\"difficulty\":\"%s\"}",
                sessionId, difficulty.getMin(), difficulty.getMax(), difficulty.getMaxTries(), difficulty.name());
        sendJson(ex, 200, json);
    }

    private static void handleGuess(HttpExchange ex) throws IOException {
        if (!"POST".equals(ex.getRequestMethod())) {
            sendJson(ex, 405, "{\"error\":\"Method not allowed\"}");
            return;
        }
        String body = readBody(ex);
        String sessionId = extractString(body, "sessionId", "");
        Integer guess = extractInt(body, "guess");

        GuessingGame game = sessions.get(sessionId);
        if (game == null) {
            sendJson(ex, 404, "{\"error\":\"Session not found. Please start a new game.\"}");
            return;
        }
        if (guess == null) {
            sendJson(ex, 400, "{\"error\":\"Please send a valid whole number.\"}");
            return;
        }

        try {
            String message = game.makeGuess(guess);
            int score = (game.isOver() && game.hasWon()) ? game.calculateScore() : 0;

            if (game.isOver() && game.hasWon()) {
                scoreManager.addScore(game.getPlayerName(), score, game.getDifficultyName());
            }

            String json = String.format(
                    "{\"message\":\"%s\",\"tries\":%d,\"maxTries\":%d,\"over\":%b,\"won\":%b,\"answer\":%s,\"score\":%d,\"history\":%s}",
                    escape(message), game.getTries(), game.getDifficulty().getMaxTries(),
                    game.isOver(), game.hasWon(),
                    game.isOver() ? String.valueOf(game.getAnswer()) : "null",
                    score, game.getGuessHistory().toString());
            sendJson(ex, 200, json);

            if (game.isOver()) {
                sessions.remove(sessionId);
            }
        } catch (InvalidGuessException e) {
            sendJson(ex, 400, "{\"error\":\"" + escape(e.getMessage()) + "\"}");
        }
    }

    private static void handleLeaderboard(HttpExchange ex) throws IOException {
        List<String[]> top = scoreManager.getTopScores(10);
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < top.size(); i++) {
            String[] e = top.get(i);
            sb.append(String.format("{\"name\":\"%s\",\"score\":%s,\"difficulty\":\"%s\"}",
                    escape(e[0]), e[1], e[2]));
            if (i < top.size() - 1) sb.append(",");
        }
        sb.append("]");
        sendJson(ex, 200, sb.toString());
    }

    private static void handleStatic(HttpExchange ex) throws IOException {
        String path = ex.getRequestURI().getPath();
        if (path.equals("/")) path = "/index.html";
        Path filePath = Paths.get("public", path).normalize();
        if (!filePath.startsWith(Paths.get("public")) || !Files.exists(filePath) || Files.isDirectory(filePath)) {
            sendText(ex, 404, "Not found");
            return;
        }

        byte[] bytes = Files.readAllBytes(filePath);
        ex.getResponseHeaders().set("Content-Type", guessContentType(path));
        ex.sendResponseHeaders(200, bytes.length);
        try (OutputStream os = ex.getResponseBody()) {
            os.write(bytes);
        }
    }

    // ---- Helpers ----

    private static String guessContentType(String path) {
        if (path.endsWith(".html")) return "text/html";
        if (path.endsWith(".css")) return "text/css";
        if (path.endsWith(".js")) return "application/javascript";
        return "text/plain";
    }

    private static String readBody(HttpExchange ex) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        InputStream is = ex.getRequestBody();
        byte[] buf = new byte[1024];
        int n;
        while ((n = is.read(buf)) != -1) baos.write(buf, 0, n);
        return baos.toString(StandardCharsets.UTF_8);
    }

    // Minimal hand-rolled JSON field extraction — fine for this fixed, simple schema.
    private static String extractString(String json, String key, String defaultVal) {
        Matcher m = Pattern.compile("\"" + key + "\"\\s*:\\s*\"([^\"]*)\"").matcher(json);
        return m.find() ? m.group(1) : defaultVal;
    }

    private static Integer extractInt(String json, String key) {
        Matcher m = Pattern.compile("\"" + key + "\"\\s*:\\s*(-?\\d+)").matcher(json);
        return m.find() ? Integer.parseInt(m.group(1)) : null;
    }

    private static String escape(String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private static void sendJson(HttpExchange ex, int status, String json) throws IOException {
        ex.getResponseHeaders().set("Content-Type", "application/json");
        ex.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        ex.sendResponseHeaders(status, bytes.length);
        try (OutputStream os = ex.getResponseBody()) {
            os.write(bytes);
        }
    }

    private static void sendText(HttpExchange ex, int status, String text) throws IOException {
        byte[] bytes = text.getBytes(StandardCharsets.UTF_8);
        ex.sendResponseHeaders(status, bytes.length);
        try (OutputStream os = ex.getResponseBody()) {
            os.write(bytes);
        }
    }
}