package com.eagle.http.handlers;

import com.eagle.dao.UserDao;
import com.eagle.model.response.ErrorResponse;
import com.eagle.model.response.UserListResponse;
import com.eagle.util.HttpIO;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.util.List;

public class ListUsersHandler implements HttpHandler {
    private final UserDao userDao = new UserDao();

    @Override
    public void handle(HttpExchange ex) {
        try {
            if (!"GET".equalsIgnoreCase(ex.getRequestMethod())) {
                HttpIO.writeJson(ex, 405, new ErrorResponse("method_not_allowed", "Use GET").toJson());
                return;
            }

            List<String> users = userDao.getAllUsers();
            UserListResponse resp = new UserListResponse("users list",users);
            HttpIO.writeJson(ex, 200, resp.toJson());

        } catch (Exception e) {
            try {
                HttpIO.writeJson(ex, 500, new ErrorResponse("internal_error", e.getMessage()).toJson());
            } catch (Exception ignore) {
                try { ex.sendResponseHeaders(500, -1); } catch (Exception ignored) {}
            }
        }
    }
}
