package com.eagle.http;

import com.eagle.dao.UserDao;
import com.eagle.model.request.SignupRequest;
import com.eagle.model.response.SignupResponse;
import com.google.gson.Gson;
import com.sun.net.httpserver.*;

import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

public class SignupHandler implements HttpHandler {
    private static final Gson GSON = new Gson();
    private final UserDao userDao = new UserDao();

    @Override public void handle(HttpExchange ex) {
        try {
            if (!"POST".equalsIgnoreCase(ex.getRequestMethod())) {
                ex.getResponseHeaders().add("Allow", "POST");
                ex.sendResponseHeaders(405, -1); return;
            }

            SignupRequest req = GSON.fromJson(
                    new InputStreamReader(ex.getRequestBody(), StandardCharsets.UTF_8), SignupRequest.class);

            if (req == null || req.getUsername()==null || req.getPassword()==null ||
                    req.getUsername().isBlank() || req.getPassword().isBlank()) {
                writeJson(ex, 400, "{\"message\":\"username and password required\"}"); return;
            }

            boolean ok = userDao.saveUser(req.getUsername(), req.getPassword());
            int status = ok ? 201 : 409; // 409 if duplicate, etc.
            SignupResponse body = new SignupResponse(ok,
                    ok ? "Signup successful" : "User already exists", req.getUsername());
            writeJson(ex, status, GSON.toJson(body));

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
