package com.eagle.dao;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class UserDao {
    private static final Logger log = LogManager.getLogger(UserDao.class);

    private final String url = "jdbc:derby:eagleDB;create=true"; // embedded Derby DB
    private final String dbUser = "";
    private final String dbPassword = "";

    public UserDao() {
        log.debug("Initializing UserDao with url={}", url);
        createTableIfNotExists();
    }

    private void createTableIfNotExists() {
        String sql = "CREATE TABLE users (" +
                "id INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY, " +
                "username VARCHAR(255) UNIQUE NOT NULL, " +
                "password VARCHAR(255) NOT NULL" +
                ")";
        try (Connection conn = DriverManager.getConnection(url, dbUser, dbPassword);
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            int r = stmt.executeUpdate();
            log.info("Table 'users' created (result={})", r);
        } catch (SQLException e) {
            // Derby SQLState X0Y32 => table already exists
            if ("X0Y32".equals(e.getSQLState())) {
                log.debug("Table 'users' already exists (SQLState={})", e.getSQLState());
            } else {
                log.error("Error creating table 'users' (SQLState={}, ErrorCode={}, Message={})",
                        e.getSQLState(), e.getErrorCode(), e.getMessage(), e);
            }
        }
    }

    public boolean saveUser(String username, String password) {
        String sql = "INSERT INTO users (username, password) VALUES (?, ?)";
        log.debug("saveUser called for username={}", username);
        try (Connection conn = DriverManager.getConnection(url, dbUser, dbPassword);
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, username);
            stmt.setString(2, password); // TODO: hash later
            int updated = stmt.executeUpdate();
            log.info("saveUser: inserted username={}, updated={}", username, updated);
            return updated > 0;
        } catch (SQLException e) {
            // Common: duplicate key constraint
            log.warn("saveUser failed for username={} (SQLState={}, ErrorCode={}, Message={})",
                    username, e.getSQLState(), e.getErrorCode(), e.getMessage());
            return false;
        }
    }

    public List<String> getAllUsers() {
        String sql = "SELECT username FROM users";
        log.debug("getAllUsers called");
        List<String> users = new ArrayList<>();
        try (Connection conn = DriverManager.getConnection(url, dbUser, dbPassword);
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                users.add(rs.getString("username"));
            }
            log.info("getAllUsers: fetched count={}", users.size());
        } catch (SQLException e) {
            log.error("Error fetching users (SQLState={}, ErrorCode={}, Message={})",
                    e.getSQLState(), e.getErrorCode(), e.getMessage(), e);
        }
        return users;
    }

    public boolean validateUser(String username, String password) {
        String sql = "SELECT COUNT(*) FROM users WHERE username=? AND password=?";
        log.debug("validateUser called for username={}", username);
        try (Connection conn = DriverManager.getConnection(url, dbUser, dbPassword);
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            ps.setString(2, password);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                int count = rs.getInt(1);
                boolean ok = count > 0;
                log.info("validateUser: username={}, matchCount={}, authenticated={}", username, count, ok);
                return ok;
            }
        } catch (SQLException e) {
            log.error("validateUser failed for username={} (SQLState={}, ErrorCode={}, Message={})",
                    username, e.getSQLState(), e.getErrorCode(), e.getMessage(), e);
            return false;
        }
    }
}
