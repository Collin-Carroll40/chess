package server.handler;

import com.google.gson.Gson;
import io.javalin.http.Context;
import request.CreateGameRequest;
import result.CreateGameResult;
import service.GameService;

public class CreateGameHandler {
    private final GameService service;

    public CreateGameHandler(GameService service) {
        this.service = service;
    }

    public void handle(Context ctx) throws Exception {
        String authToken = ctx.header("authorization");
        Gson gson = new Gson();
        CreateGameRequest req = gson.fromJson(ctx.body(), CreateGameRequest.class);

        CreateGameResult res = service.createGame(authToken, req);

        ctx.status(200);
        ctx.result(gson.toJson(res));
    }
}