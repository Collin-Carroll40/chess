package client;

import com.google.gson.Gson;
import model.GameData;

import java.io.*;
import java.net.*;
import java.util.Collection;
import java.util.Map;

public class ServerFacade {

    private final String serverUrl;
    private final Gson gson = new Gson();

    public ServerFacade(int port) {
        this.serverUrl = "http://localhost:" + port;
    }

    public ServerFacade(String url) {
        this.serverUrl = url;
    }

    public AuthResult register(String username, String password, String email) throws Exception {
        var body = Map.of("username", username, "password", password, "email", email);
        var resp = makeRequest("POST", "/user", body, null);
        return gson.fromJson(resp, AuthResult.class);
    }

    public AuthResult login(String username, String password) throws Exception {
        var body = Map.of("username", username, "password", password);
        var resp = makeRequest("POST", "/session", body, null);
        return gson.fromJson(resp, AuthResult.class);
    }

    public void logout(String authToken) throws Exception {
        makeRequest("DELETE", "/session", null, authToken);
    }

    public GameResult createGame(String authToken, String gameName) throws Exception {
        var body = Map.of("gameName", gameName);
        var resp = makeRequest("POST", "/game", body, authToken);
        return gson.fromJson(resp, GameResult.class);
    }

    public GameData[] listGames(String authToken) throws Exception {
        var resp = makeRequest("GET", "/game", null, authToken);
        record ListResult(GameData[] games) {}
        return gson.fromJson(resp, ListResult.class).games();
    }

    public void joinGame(String authToken, int gameID, String playerColor) throws Exception {
        var body = Map.of("playerColor", playerColor, "gameID", gameID);
        makeRequest("PUT", "/game", body, authToken);
    }

    public void clear() throws Exception {
        makeRequest("DELETE", "/db", null, null);
    }

    private String makeRequest(String method, String path, Object body, String authToken) throws Exception {
        URL url = new URI(serverUrl + path).toURL();
        HttpURLConnection http = (HttpURLConnection) url.openConnection();
        http.setRequestMethod(method);
        http.setRequestProperty("Content-Type", "application/json");

        if (authToken != null) {
            http.setRequestProperty("authorization", authToken);
        }

        if (body != null) {
            http.setDoOutput(true);
            try (var out = new OutputStreamWriter(http.getOutputStream())) {
                out.write(gson.toJson(body));
            }
        }

        http.connect();

        if (http.getResponseCode() >= 400) {
            String errBody = readStream(http.getErrorStream());
            var errMap = gson.fromJson(errBody, Map.class);
            String msg = errMap != null && errMap.containsKey("message")
                    ? (String) errMap.get("message") : "Error: unknown";
            throw new Exception(msg);
        }

        return readStream(http.getInputStream());
    }

    private String readStream(InputStream stream) throws IOException {
        if (stream == null) {
            return "";
        }
        try (var reader = new BufferedReader(new InputStreamReader(stream))) {
            var sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
            return sb.toString();
        }
    }

    // Simple records for deserialization
    public record AuthResult(String username, String authToken) {}
    public record GameResult(int gameID) {}
}