package server;
import handler.*;
import dataaccess.*;
import io.javalin.Javalin;
import server.handler.ClearHandler;
import server.handler.RegisterHandler;
import service.*;

public class Server {
    // 1. Create the DAOs once
    private final UserDAO userDAO = new MemoryUserDAO();
    private final AuthDAO authDAO = new MemoryAuthDAO();
    private final GameDAO gameDAO = new MemoryGameDAO();

    // 2. Create the Services
    private final ClearService clearService = new ClearService(userDAO, authDAO, gameDAO);
    private final UserService userService = new UserService(userDAO, authDAO);
    private final GameService gameService = new GameService(gameDAO, authDAO);

    public int run(int port) {
        Javalin app = Javalin.create(config -> {
            config.staticFiles.add("/web");
        }).start(port);

        // --- GLOBAL ERROR HANDLING ---
        app.exception(DataAccessException.class, (e, ctx) -> {
            String msg = e.getMessage();
            if (msg.contains("bad request")) ctx.status(400);
            else if (msg.contains("unauthorized")) ctx.status(401);
            else if (msg.contains("already taken")) ctx.status(403);
            else ctx.status(500);

            ctx.result(new com.google.gson.Gson().toJson(java.util.Map.of("message", msg)));
        });

        // --- ENDPOINTS ---
        app.delete("/db", new ClearHandler(clearService)::handle);
        app.post("/user", new RegisterHandler(userService)::handle);
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