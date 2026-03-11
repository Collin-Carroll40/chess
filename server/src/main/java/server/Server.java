package server;

import handler.*;
import dataaccess.*;
import io.javalin.Javalin;
import service.*;
import com.google.gson.Gson;
import java.util.Map;

public class Server {
    private final UserDAO userDAO;
    private final AuthDAO authDAO;
    private final GameDAO gameDAO;
    private final ClearService clearService;
    private final UserService userService;
    private final GameService gameService;

    public Server() {
        try {
            userDAO = new SqlUserDAO();
            authDAO = new SqlAuthDAO();
            gameDAO = new SqlGameDAO();

            clearService = new ClearService(userDAO, authDAO, gameDAO);
            userService = new UserService(userDAO, authDAO);
            gameService = new GameService(gameDAO, authDAO);
        } catch (DataAccessException e) {
            throw new RuntimeException(e);
        }
    }

    public int run(int port) {
        Javalin app = Javalin.create(config -> {
            config.staticFiles.add("/web");
        }).start(port);

        // This block is what the "Database Error Handling" test is checking
        app.exception(DataAccessException.class, (e, ctx) -> {
            String msg = e.getMessage();
            if (msg.contains("bad request")) {
                ctx.status(400);
            } else if (msg.contains("unauthorized")) {
                ctx.status(401);
            } else if (msg.contains("already taken")) {
                ctx.status(403);
            } else {
                ctx.status(500);
            }

            // Ensure the response body ALWAYS contains the JSON message
            ctx.result(new Gson().toJson(Map.of("message", msg)));
        });

        // ENDPOINTS
        app.delete("/db", new ClearHandler(clearService)::handle);
        app.post("/user", new server.handler.RegisterHandler(userService)::handle);
        app.post("/session", new LoginHandler(userService)::handle);
        app.delete("/session", new LogoutHandler(userService)::handle);
        app.get("/game", new server.handler.ListGamesHandler(gameService)::handle);
        app.post("/game", new server.handler.CreateGameHandler(gameService)::handle);
        app.put("/game", new server.handler.JoinGameHandler(gameService)::handle);

        return app.port();
    }

    public void stop() {
    }
}