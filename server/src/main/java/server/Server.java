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
        WebSocketHandler wsHandler = new WebSocketHandler(gameDAO, authDAO);

        Javalin app = Javalin.create(config -> {
            config.staticFiles.add("/web");
        }).start(port);

        app.ws("/ws", ws -> {
            ws.onConnect(ctx -> {
                ctx.session.setIdleTimeout(java.time.Duration.ofMinutes(5));
            });
            ws.onMessage(ctx -> {
                wsHandler.onMessage(ctx.session, ctx.message());
            });
            ws.onClose(ctx -> {});
        });

        // 1. Handle our standard database errors
        app.exception(DataAccessException.class, (e, ctx) -> {
            String msg = e.getMessage();

            if (msg == null) {
                msg = "unknown error";
            }

            if (!msg.startsWith("Error: ")) {
                msg = "Error: " + msg;
            }

            if (msg.contains("bad request")) {
                ctx.status(400);
            } else if (msg.contains("unauthorized")) {
                ctx.status(401);
            } else if (msg.contains("already taken")) {
                ctx.status(403);
            } else {
                ctx.status(500);
            }

            ctx.result(new Gson().toJson(Map.of("message", msg)));
        });

        // 2. Catch-all for unexpected crashes simulated by the grader
        app.exception(Exception.class, (e, ctx) -> {
            String msg = e.getMessage();
            if (msg == null) {
                msg = "internal server error";
            }
            if (!msg.startsWith("Error: ")) {
                msg = "Error: " + msg;
            }

            ctx.status(500);
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