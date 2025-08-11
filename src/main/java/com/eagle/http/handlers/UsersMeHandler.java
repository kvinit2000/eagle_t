package com.eagle.http.handlers;

import com.eagle.dao.UserDao;
import com.eagle.dao.UserDao.UserRecord;
import com.eagle.model.response.ErrorResponse;
import com.eagle.model.response.UserProfileResponse;
import com.google.gson.JsonSyntaxException;
import com.sun.net.httpserver.HttpExchange;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.time.LocalDate;
import java.util.Map;

/**
 * UsersMeHandler refactored to use BaseHandler utilities.
 * Supports GET and PATCH (with X-HTTP-Method-Override: PATCH fallback).
 */
public class UsersMeHandler extends BaseHandler {
    private static final Logger log = LogManager.getLogger(UsersMeHandler.class);
    private final UserDao userDao = new UserDao();

    @Override
    protected void doHandle(HttpExchange ex) throws Exception {
        String method = effectiveMethod(ex);
        switch (method) {
            case "GET" -> handleGet(ex);
            case "PATCH" -> handlePatch(ex);
            case "OPTIONS" -> { ex.getResponseHeaders().set("Allow", "GET, PATCH, OPTIONS"); ex.sendResponseHeaders(204, -1); }
            default -> { ex.getResponseHeaders().set("Allow", "GET, PATCH, OPTIONS"); ex.sendResponseHeaders(405, -1); }
        }
    }

    private void handleGet(HttpExchange ex) throws Exception {
        String token = parseBearer(ex.getRequestHeaders().getFirst("Authorization"));
        if (token == null) { writeJson(ex, 401, new ErrorResponse("unauthorized", "Missing or invalid Authorization header")); return; }

        UserRecord u = userDao.getUserByAuthToken(token);
        if (u == null) { writeJson(ex, 401, new ErrorResponse("unauthorized", "Invalid token")); return; }

        UserProfileResponse resp = new UserProfileResponse(
                u.username,
                u.email,
                (u.dob == null ? null : u.dob.toString()),
                u.address,
                u.pin,
                u.phone
        );
        writeJson(ex, 200, resp);
    }

    private void handlePatch(HttpExchange ex) throws Exception {
        if (!isJsonRequest(ex)) {
            writeJson(ex, 415, new ErrorResponse("unsupported_media_type", "Content-Type must be application/json"));
            return;
        }

        String token = parseBearer(ex.getRequestHeaders().getFirst("Authorization"));
        if (token == null) { writeJson(ex, 401, new ErrorResponse("unauthorized", "Missing or invalid Authorization header")); return; }

        UserRecord authed = userDao.getUserByAuthToken(token);
        if (authed == null) { writeJson(ex, 401, new ErrorResponse("unauthorized", "Invalid token")); return; }

        Map<?, ?> req;
        try {
            req = readJson(ex, Map.class);
        } catch (JsonSyntaxException jse) {
            writeJson(ex, 400, new ErrorResponse("bad_request", "Malformed JSON body"));
            return;
        }

        String email   = strOrNull(req, "email");
        String address = strOrNull(req, "address");
        String pin     = strOrNull(req, "pin");
        String phone   = strOrNull(req, "phone");
        String dobRaw  = strOrNull(req, "dob");

        LocalDate dob = null;
        if (dobRaw != null) {
            try { dob = LocalDate.parse(dobRaw); }
            catch (Exception e) { writeJson(ex, 400, new ErrorResponse("bad_request", "Invalid dob format (yyyy-MM-dd)")); return; }
        }

        if (email == null && address == null && pin == null && phone == null && dob == null) {
            writeJson(ex, 400, new ErrorResponse("bad_request", "No fields to update"));
            return;
        }

        boolean ok = userDao.patchUserByAuthToken(token, email, dob, address, pin, phone);
        if (!ok) { writeJson(ex, 404, new ErrorResponse("not_found", "User not found or nothing changed")); return; }

        UserRecord u = userDao.getUserByAuthToken(token);
        UserProfileResponse resp = new UserProfileResponse(
                u.username, u.email, (u.dob == null ? null : u.dob.toString()), u.address, u.pin, u.phone
        );
        writeJson(ex, 200, resp);
    }

    // ---- helpers ----

    private static String effectiveMethod(HttpExchange ex) {
        String method = ex.getRequestMethod();
        String override = ex.getRequestHeaders().getFirst("X-HTTP-Method-Override");
        if (override != null && !override.isBlank()) {
            method = override.trim().toUpperCase();
        }
        return method;
    }

    private static String strOrNull(Map<?,?> m, String key) {
        if (m == null) return null;
        Object v = m.get(key);
        if (v == null) return null;
        String s = String.valueOf(v).trim();
        return s.isEmpty() ? null : s;
    }
}
