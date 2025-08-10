package com.eagle.dao;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.sql.*;
import java.time.LocalDate;
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
        // New columns added, all nullable to stay backward compatible with your current clients.
        String sql = "CREATE TABLE users (" +
                "id INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY, " +
                "username VARCHAR(255) UNIQUE NOT NULL, " +
                "password VARCHAR(255) NOT NULL, " +
                "email VARCHAR(255), " +
                "dob DATE, " +
                "address VARCHAR(500), " +
                "pin VARCHAR(20), " +
                "phone VARCHAR(20)" +
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

    /** Backward-compatible: existing signup that only has username/password. */
    public boolean saveUser(String username, String password) {
        String sql = "INSERT INTO users (username, password) VALUES (?, ?)";
        log.debug("saveUser(username,password) called for username={}", username);
        try (Connection conn = DriverManager.getConnection(url, dbUser, dbPassword);
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, username);
            stmt.setString(2, password); // TODO: hash later
            int updated = stmt.executeUpdate();
            log.info("saveUser: inserted username={}, updated={}", username, updated);
            return updated > 0;
        } catch (SQLException e) {
            log.warn("saveUser failed for username={} (SQLState={}, ErrorCode={}, Message={})",
                    username, e.getSQLState(), e.getErrorCode(), e.getMessage());
            return false;
        }
    }

    /** New: full-save with extended fields. Any nullable param may be null. */
    public boolean saveUser(String username,
                            String password,
                            String email,
                            LocalDate dob,
                            String address,
                            String pin,
                            String phone) {
        String sql = "INSERT INTO users (username, password, email, dob, address, pin, phone) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?)";
        log.debug("saveUser(full) called for username={}", username);
        try (Connection conn = DriverManager.getConnection(url, dbUser, dbPassword);
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, username);
            stmt.setString(2, password); // TODO: hash later
            if (email != null) stmt.setString(3, email); else stmt.setNull(3, Types.VARCHAR);
            if (dob != null) stmt.setDate(4, Date.valueOf(dob)); else stmt.setNull(4, Types.DATE);
            if (address != null) stmt.setString(5, address); else stmt.setNull(5, Types.VARCHAR);
            if (pin != null) stmt.setString(6, pin); else stmt.setNull(6, Types.VARCHAR);
            if (phone != null) stmt.setString(7, phone); else stmt.setNull(7, Types.VARCHAR);

            int updated = stmt.executeUpdate();
            log.info("saveUser(full): inserted username={}, updated={}", username, updated);
            return updated > 0;
        } catch (SQLException e) {
            log.warn("saveUser(full) failed for username={} (SQLState={}, ErrorCode={}, Message={})",
                    username, e.getSQLState(), e.getErrorCode(), e.getMessage());
            return false;
        }
    }

    /** Update extended fields for an existing user. */
    public boolean updateUserDetails(String username,
                                     String email,
                                     LocalDate dob,
                                     String address,
                                     String pin,
                                     String phone) {
        String sql = "UPDATE users SET email=?, dob=?, address=?, pin=?, phone=? WHERE username=?";
        log.debug("updateUserDetails called for username={}", username);
        try (Connection conn = DriverManager.getConnection(url, dbUser, dbPassword);
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            if (email != null) stmt.setString(1, email); else stmt.setNull(1, Types.VARCHAR);
            if (dob != null) stmt.setDate(2, Date.valueOf(dob)); else stmt.setNull(2, Types.DATE);
            if (address != null) stmt.setString(3, address); else stmt.setNull(3, Types.VARCHAR);
            if (pin != null) stmt.setString(4, pin); else stmt.setNull(4, Types.VARCHAR);
            if (phone != null) stmt.setString(5, phone); else stmt.setNull(5, Types.VARCHAR);
            stmt.setString(6, username);

            int updated = stmt.executeUpdate();
            log.info("updateUserDetails: username={}, updated={}", username, updated);
            return updated > 0;
        } catch (SQLException e) {
            log.error("updateUserDetails failed for username={} (SQLState={}, ErrorCode={}, Message={})",
                    username, e.getSQLState(), e.getErrorCode(), e.getMessage(), e);
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
