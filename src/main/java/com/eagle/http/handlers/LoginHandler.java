package com.eagle.http.handlers;

import com.eagle.dao.UserDao;
import com.eagle.model.request.LoginRequest;
import com.eagle.model.response.LoginResponse;
import com.eagle.util.JwtUtil;
import com.google.gson.Gson;
import com.sun.net.httpserver.*;

import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

public class LoginHandler implements HttpHandler {
    private static final Gson GSON = new Gson();
    private final UserDao userDao = new UserDao();

    @Override public void handle(HttpExchange ex) {
        try {
            if (!"POST".equalsIgnoreCase(ex.getRequestMethod())) {
                ex.getResponseHeaders().add("Allow", "POST");
                ex.sendResponseHeaders(405, -1); return;
            }

            LoginRequest req = GSON.fromJson(
                    new InputStreamReader(ex.getRequestBody(), StandardCharsets.UTF_8), LoginRequest.class);

            if (req == null || req.getUsername()==null || req.getPassword()==null) {
                writeJson(ex, 400, "{\"message\":\"username and password required\"}"); return;
            }

            boolean ok = userDao.validateUser(req.getUsername(), req.getPassword());
            if (!ok) { writeJson(ex, 401, GSON.toJson(new LoginResponse(null, 0, "Invalid credentials"))); return; }

            String token = JwtUtil.issue(req.getUsername());
            writeJson(ex, 200, GSON.toJson(new LoginResponse(token, JwtUtil.ttlSeconds(), "Login successful")));

        } catch (Exception e) {
            e.printStackTrace();
            try { writeJson(ex, 500, "{\"message\":\"internal error\"}"); } catch (Exception ignore) {}
        }
    }

    private void writeJson(HttpExchange ex, int code, String body) throws Exception {
        ex.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
        byte[] b = body.getBytes(StandardCharsets.UTF_8);
        ex.sendResponseHeaders(code, b.length);
        try (OutputStream os = ex.getResponseBody()) { os.write(b); }
    }
}
