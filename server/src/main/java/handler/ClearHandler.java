package handler;

import dataaccess.DataAccessException;
import io.javalin.http.Context;
import service.ClearService;

public class ClearHandler {
    private final ClearService service;

    public ClearHandler(ClearService service) {
        this.service = service;
    }

    public void handle(Context ctx) {
        try {
            service.clear();
        } catch (DataAccessException e) {
            throw new RuntimeException(e);
        }
        ctx.status(200);
        ctx.result("{}"); // Success always returns an empty JSON object for this endpoint
    }
}