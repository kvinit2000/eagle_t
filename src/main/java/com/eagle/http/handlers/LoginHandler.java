package com.eagle.http.handlers;

import com.eagle.dao.UserDao;
import com.eagle.model.request.LoginRequest;
import com.eagle.model.response.ErrorResponse;
import com.eagle.model.response.LoginResponse;
import com.eagle.util.HttpIO;
import com.eagle.util.JwtUtil;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class LoginHandler implements HttpHandler {
    private static final Logger log = LogManager.getLogger(LoginHandler.class);
    private final UserDao userDao = new UserDao();

    @Override
    public void handle(HttpExchange ex) {
        try {
            if (!"POST".equalsIgnoreCase(ex.getRequestMethod())) {
                safeWriteJson(ex, 405, new ErrorResponse("method_not_allowed", "Use POST"));
                return;
            }

            String body = HttpIO.readBody(ex);
            LoginRequest req = LoginRequest.fromJson(body);
            if (req == null || isBlank(req.getUsername()) || isBlank(req.getPassword())) {
                safeWriteJson(ex, 400, new ErrorResponse("bad_request", "username and password required"));
                return;
            }

            // Validate against DB (or your auth source)
            boolean ok = userDao.validateUser(req.getUsername(), req.getPassword());
            if (!ok) {
                // Optional: set WWW-Authenticate for bearer flows
                safeWriteJson(ex, 401, new ErrorResponse("unauthorized", "Invalid credentials"));
                return;
            }

            // Issue JWT
            String token = JwtUtil.issue(req.getUsername());
            LoginResponse resp = new LoginResponse(token, 3600, "Login successful");
            safeWriteJson(ex, 200, resp);

        } catch (Exception e) {
            log.error("Login error", e);
            safeWriteJson(ex, 500, new ErrorResponse("internal_error", e.getMessage()));
        }
    }

    private static boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }

    /** Never throw from writing the response; fall back to status-only if needed. */
    private void safeWriteJson(HttpExchange ex, int status, Object payload) {
        try {
            if (payload instanceof String s) {
                HttpIO.writeJson(ex, status, s);
            } else if (payload instanceof ErrorResponse er) {
                HttpIO.writeJson(ex, status, er.toJson());
            } else if (payload instanceof LoginResponse lr) {
                HttpIO.writeJson(ex, status, lr.toJson());
            } else {
                // Generic: rely on toString if someone passes the wrong type
                HttpIO.writeJson(ex, status, String.valueOf(payload));
            }
        } catch (Exception writeErr) {
            log.error("Failed to send JSON response", writeErr);
            try {
                ex.sendResponseHeaders(status, -1);
            } catch (Exception ignore) {
                // nothing else we can do
            }
        }
    }
}
