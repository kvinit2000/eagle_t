package com.eagle.http.handlers;

import com.eagle.model.response.ErrorResponse;
import com.eagle.model.response.PingResponse;
import com.eagle.util.HttpIO;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

public class HelloHandler implements HttpHandler {

    @Override
    public void handle(HttpExchange ex) {
        try {
            if (!"GET".equalsIgnoreCase(ex.getRequestMethod())) {
                HttpIO.writeJson(ex, 405, new ErrorResponse("method_not_allowed", "Use GET").toJson());
                return;
            }

            PingResponse resp = new PingResponse("Hello from server", System.currentTimeMillis());
            HttpIO.writeJson(ex, 200, resp.toJson());

        } catch (Exception e) {
            // Best-effort error write; fall back to status-only if stream is broken
            try {
                HttpIO.writeJson(ex, 500, new ErrorResponse("internal_error", e.getMessage()).toJson());
            } catch (Exception ignore) {
                try { ex.sendResponseHeaders(500, -1); } catch (Exception ignored) {}
            }
        }
    }
}
