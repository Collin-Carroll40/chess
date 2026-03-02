package handler;

import io.javalin.http.Context;
import service.UserService;

public class LogoutHandler {
    private final UserService service;

    public LogoutHandler(UserService service) {
        this.service = service;
    }

    public void handle(Context ctx) throws Exception {
        // Read the authToken out of the HTTP header
        String authToken = ctx.header("authorization");

        service.logout(authToken);

        ctx.status(200);
        ctx.result("{}");
    }
}