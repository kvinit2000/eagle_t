package com.eagle.http.handlers;

import com.eagle.dao.UserDao;
import com.eagle.model.request.LoginRequest;
import com.eagle.model.response.ErrorResponse;
import com.eagle.model.response.LoginResponse;
import com.eagle.util.JwtUtil;
import com.sun.net.httpserver.HttpExchange;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Refactored LoginHandler using BaseHandler utilities.
 */
public class LoginHandler extends BaseHandler {
    private static final Logger log = LogManager.getLogger(LoginHandler.class);
    private static final int EXPIRES_IN_SECONDS = 3600;

    private final UserDao userDao = new UserDao();

    @Override
    protected void doHandle(HttpExchange ex) throws Exception {
        if (!ensureMethod(ex, "POST", "OPTIONS")) return; // writes 405 with Allow
        if ("OPTIONS".equalsIgnoreCase(ex.getRequestMethod())) {
            ex.getResponseHeaders().set("Allow", "POST, OPTIONS");
            ex.sendResponseHeaders(204, -1);
            return;
        }

        if (!isJsonRequest(ex)) {
            writeJson(ex, 415, new ErrorResponse("unsupported_media_type", "Content-Type must be application/json"));
            return;
        }

        LoginRequest req;
        try {
            req = readJson(ex, LoginRequest.class);
        } catch (Exception parseErr) {
            writeJson(ex, 400, new ErrorResponse("bad_request", "Malformed JSON body"));
            return;
        }

        if (req == null || isBlank(req.getUsername()) || isBlank(req.getPassword())) {
            writeJson(ex, 400, new ErrorResponse("bad_request", "username and password required"));
            return;
        }

        boolean ok;
        try {
            ok = userDao.validateUser(req.getUsername(), req.getPassword());
        } catch (Exception dbErr) {
            log.error("Login DB error", dbErr);
            writeJson(ex, 500, new ErrorResponse("internal_error", "Auth backend error"));
            return;
        }

        if (!ok) {
            ex.getResponseHeaders().set("WWW-Authenticate", "Bearer realm=\"eagle\"");
            writeJson(ex, 401, new ErrorResponse("unauthorized", "Invalid credentials"));
            return;
        }

        // Issue JWT
        String token;
        try {
            token = JwtUtil.issue(req.getUsername());
        } catch (Exception jwtErr) {
            log.error("JWT issue error", jwtErr);
            writeJson(ex, 500, new ErrorResponse("internal_error", "Token issuance failed"));
            return;
        }

        LoginResponse resp = new LoginResponse(token, EXPIRES_IN_SECONDS, "Login successful");
        writeJson(ex, 200, resp);
    }

    private static boolean isBlank(String s) { return s == null || s.trim().isEmpty(); }
}
