package server.handler;

import com.google.gson.Gson;
import io.javalin.http.Context;
import request.JoinGameRequest;
import service.GameService;

public class JoinGameHandler {
    private final GameService service;

    public JoinGameHandler(GameService service) {
        this.service = service;
    }

    public void handle(Context ctx) throws Exception {
        String authToken = ctx.header("authorization");
        Gson gson = new Gson();
        JoinGameRequest req = gson.fromJson(ctx.body(), JoinGameRequest.class);

        service.joinGame(authToken, req);

        ctx.status(200);
        ctx.result("{}");
    }
}