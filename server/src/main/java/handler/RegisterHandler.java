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

        // 1. Turn the JSON body of the web request into a Java object
        RegisterRequest req = gson.fromJson(ctx.body(), RegisterRequest.class);

        // 2. Pass it to your tested service
        RegisterResult res = service.register(req);

        // 3. Send the success status and the result back as JSON
        ctx.status(200);
        ctx.result(gson.toJson(res));
    }
}