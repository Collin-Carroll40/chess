package handler;

import com.google.gson.Gson;
import io.javalin.http.Context;
import request.LoginRequest;
import result.LoginResult;
import service.UserService;

public class LoginHandler {
    private final UserService service;

    public LoginHandler(UserService service) {
        this.service = service;
    }

    public void handle(Context ctx) throws Exception {
        Gson gson = new Gson();
        LoginRequest req = gson.fromJson(ctx.body(), LoginRequest.class);

        LoginResult res = service.login(req);

        ctx.status(200);
        ctx.result(gson.toJson(res));
    }
}