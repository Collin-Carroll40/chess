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

    public Server() {
        try {
            userDAO = new SqlUserDAO();
            authDAO = new SqlAuthDAO();
            gameDAO = new SqlGameDAO();
        } catch (DataAccessException e) {
            throw new RuntimeException(e);
        }
    }

    private final ClearService clearService = new ClearService(userDAO, authDAO, gameDAO);
    private final UserService userService = new UserService(userDAO, authDAO);
    private final GameService gameService = new GameService(gameDAO, authDAO);

    public int run(int port) {
        Javalin app = Javalin.create(config -> {
            config.staticFiles.add("/web");
        }).start(port);

        // This handles the "Database Error Handling" test by ensuring a JSON body is returned
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

            // The test fails if this body is null; this line ensures the "message" field exists
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
        Javalin.create().stop();
    }
}