package server.handler;

import com.google.gson.Gson;
import io.javalin.http.Context;
import result.ListGamesResult;
import service.GameService;

public class ListGamesHandler {
    private final GameService service;

    public ListGamesHandler(GameService service) {
        this.service = service;
    }

    public void handle(Context ctx) throws Exception {
        String authToken = ctx.header("authorization");
        ListGamesResult res = service.listGames(authToken);

        ctx.status(200);
        ctx.result(new Gson().toJson(res));
    }
}