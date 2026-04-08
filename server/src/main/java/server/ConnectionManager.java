package server;

import com.google.gson.Gson;
import org.eclipse.jetty.websocket.api.Session;
import websocket.messages.ServerMessage;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;

public class ConnectionManager {
    private final ConcurrentHashMap<Integer, ConcurrentHashMap<String, Connection>> games = new ConcurrentHashMap<>();
    private final Gson gson = new Gson();

    public record Connection(String username, Session session) {}

    public void add(int gameID, String authToken, String username, Session session) {
        games.computeIfAbsent(gameID, k -> new ConcurrentHashMap<>())
                .put(authToken, new Connection(username, session));
    }

    public void remove(int gameID, String authToken) {
        var game = games.get(gameID);
        if (game != null) {
            game.remove(authToken);
        }
    }

    public void broadcast(int gameID, String excludeAuth, ServerMessage message) throws IOException {
        var game = games.get(gameID);
        if (game == null) {
            return;
        }
        String json = gson.toJson(message);
        for (var entry : game.entrySet()) {
            if (!entry.getKey().equals(excludeAuth)) {
                var conn = entry.getValue();
                if (conn.session().isOpen()) {
                    conn.session().getRemote().sendString(json);
                }
            }
        }
    }

    public void sendToUser(String authToken, int gameID, ServerMessage message) throws IOException {
        var game = games.get(gameID);
        if (game == null) {
            return;
        }
        var conn = game.get(authToken);
        if (conn != null && conn.session().isOpen()) {
            conn.session().getRemote().sendString(gson.toJson(message));
        }
    }
}