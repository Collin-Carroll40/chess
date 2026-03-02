package server.handler;

import com.google.gson.Gson;
import io.javalin.http.Context;
import request.RegisterRequest;
import result.RegisterResult;
import service.UserService;

public class RegisterHandler {
    private final UserService service;

    public RegisterHandler(UserService service) {
        this.service = service;
    }

    public void handle(Context ctx) throws Exception {
        Gson gson = new Gson();

        RegisterRequest req = gson.fromJson(ctx.body(), RegisterRequest.class);

        RegisterResult res = service.register(req);

        ctx.status(200);
        ctx.result(gson.toJson(res));
    }
}