package com.eagle.http.handlers;

import com.eagle.model.response.ErrorResponse;
import com.eagle.model.response.PingResponse;
import com.sun.net.httpserver.HttpExchange;

/**
 * Refactored to use BaseHandler utilities.
 */
public class HelloHandler extends BaseHandler {

    @Override
    protected void doHandle(HttpExchange ex) throws Exception {
        if (!ensureMethod(ex, "GET", "OPTIONS")) return; // writes 405 + Allow on mismatch
        if ("OPTIONS".equalsIgnoreCase(ex.getRequestMethod())) {
            ex.getResponseHeaders().set("Allow", "GET, OPTIONS");
            ex.sendResponseHeaders(204, -1);
            return;
        }

        try {
            PingResponse resp = new PingResponse("Hello from server", System.currentTimeMillis());
            writeJson(ex, 200, resp);
        } catch (Exception e) {
            // Return JSON error body; BaseHandler has a 500 safety net as well
            writeJson(ex, 500, new ErrorResponse("internal_error", e.getMessage()));
        }
    }
}
