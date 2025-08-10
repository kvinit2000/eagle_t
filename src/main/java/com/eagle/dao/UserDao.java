package com.eagle.dao;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class UserDao {
    private static final Logger log = LogManager.getLogger(UserDao.class);

    private final String url = "jdbc:derby:memory:eagleDB;create=true";
    private final String dbUser = "";
    private final String dbPassword = "";

    public UserDao() {
        log.debug("Initializing UserDao with url={}", url);
        createTableIfNotExists();
    }

    // Lightweight DTO for safe returns (no password, no token)
    public static class UserRecord {
        public int id;
        public String username;
        public String email;
        public Date dob;
        public String address;
        public String pin;
        public String phone;
    }

    private void createTableIfNotExists() {
        String sql = "CREATE TABLE users (" +
                "id INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY, " +
                "username VARCHAR(255) UNIQUE NOT NULL, " +
                "password VARCHAR(255) NOT NULL, " +
                "email VARCHAR(255), " +
                "dob DATE, " +
                "address VARCHAR(500), " +
                "pin VARCHAR(20), " +
                "phone VARCHAR(20), " +
                "auth_token VARCHAR(64) UNIQUE" +
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

    /** Generate a new opaque bearer token (no dashes). */
    private String newToken() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    /** Backward-compatible: existing signup that only has username/password.
     *  Now also stores a generated auth_token so /me can work. */
    public boolean saveUser(String username, String password) {
        String token = newToken();
        String sql = "INSERT INTO users (username, password, auth_token) VALUES (?, ?, ?)";
        log.debug("saveUser(username,password) called for username={}", username);
        try (Connection conn = DriverManager.getConnection(url, dbUser, dbPassword);
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, username);
            stmt.setString(2, password); // TODO: hash later
            stmt.setString(3, token);
            int updated = stmt.executeUpdate();
            log.info("saveUser: inserted username={}, updated={}", username, updated);
            return updated > 0;
        } catch (SQLException e) {
            log.warn("saveUser failed for username={} (SQLState={}, ErrorCode={}, Message={})",
                    username, e.getSQLState(), e.getErrorCode(), e.getMessage());
            return false;
        }
    }

    /** New: full-save with extended fields. Any nullable param may be null.
     *  Also stores a generated auth_token. */
    public boolean saveUser(String username,
                            String password,
                            String email,
                            LocalDate dob,
                            String address,
                            String pin,
                            String phone) {
        String token = newToken();
        String sql = "INSERT INTO users (username, password, email, dob, address, pin, phone, auth_token) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
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
            stmt.setString(8, token);

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

    /** List all usernames (unchanged). */
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

    /** Basic credential check (unchanged). */
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

    /** NEW: fetch the bearer token for a username (for returning after signup/login). */
    public String getAuthToken(String username) {
        String sql = "SELECT auth_token FROM users WHERE username=?";
        log.debug("getAuthToken called for username={}", username);
        try (Connection conn = DriverManager.getConnection(url, dbUser, dbPassword);
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;
                String token = rs.getString(1);
                log.info("getAuthToken: username={} -> tokenFound={}", username, token != null);
                return token;
            }
        } catch (SQLException e) {
            log.error("getAuthToken failed for username={} (SQLState={}, ErrorCode={}, Message={})",
                    username, e.getSQLState(), e.getErrorCode(), e.getMessage(), e);
            return null;
        }
    }

    /** NEW: set/rotate a token for a username (useful if you add a /login later). */
    public String rotateAuthToken(String username) {
        String token = newToken();
        String sql = "UPDATE users SET auth_token=? WHERE username=?";
        log.debug("rotateAuthToken called for username={}", username);
        try (Connection conn = DriverManager.getConnection(url, dbUser, dbPassword);
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, token);
            ps.setString(2, username);
            int updated = ps.executeUpdate();
            log.info("rotateAuthToken: username={}, updated={}", username, updated);
            return updated > 0 ? token : null;
        } catch (SQLException e) {
            log.error("rotateAuthToken failed for username={} (SQLState={}, ErrorCode={}, Message={})",
                    username, e.getSQLState(), e.getErrorCode(), e.getMessage(), e);
            return null;
        }
    }

    /** NEW: fetch a user profile by bearer token — use this in /users/me. */
    public UserRecord getUserByAuthToken(String token) {
        String sql = "SELECT id, username, email, dob, address, pin, phone " +
                "FROM users WHERE auth_token = ?";
        log.debug("getUserByAuthToken called");
        try (Connection conn = DriverManager.getConnection(url, dbUser, dbPassword);
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, token);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    log.info("getUserByAuthToken: no match");
                    return null;
                }
                UserRecord u = new UserRecord();
                u.id = rs.getInt("id");
                u.username = rs.getString("username");
                u.email = rs.getString("email");
                u.dob = rs.getDate("dob");
                u.address = rs.getString("address");
                u.pin = rs.getString("pin");
                u.phone = rs.getString("phone");
                log.info("getUserByAuthToken: match for userId={}, username={}", u.id, u.username);
                return u;
            }
        } catch (SQLException e) {
            log.error("getUserByAuthToken failed (SQLState={}, ErrorCode={}, Message={})",
                    e.getSQLState(), e.getErrorCode(), e.getMessage(), e);
            return null;
        }
    }

    /** NEW: fetch a user profile by username — use when you already know the username. */
    public UserRecord getUserByUsername(String username) {
        String sql = "SELECT id, username, email, dob, address, pin, phone " +
                "FROM users WHERE username = ?";
        log.debug("getUserByUsername called for username={}", username);
        try (Connection conn = DriverManager.getConnection(url, dbUser, dbPassword);
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    log.info("getUserByUsername: no match for username={}", username);
                    return null;
                }
                UserRecord u = new UserRecord();
                u.id = rs.getInt("id");
                u.username = rs.getString("username");
                u.email = rs.getString("email");
                u.dob = rs.getDate("dob");
                u.address = rs.getString("address");
                u.pin = rs.getString("pin");
                u.phone = rs.getString("phone");
                log.info("getUserByUsername: match for userId={}, username={}", u.id, u.username);
                return u;
            }
        } catch (SQLException e) {
            log.error("getUserByUsername failed for username={} (SQLState={}, ErrorCode={}, Message={})",
                    username, e.getSQLState(), e.getErrorCode(), e.getMessage(), e);
            return null;
        }
    }
    /** PATCH selected fields for the user identified by auth token. */
    public boolean patchUserByAuthToken(String token,
                                        String email,
                                        LocalDate dob,
                                        String address,
                                        String pin,
                                        String phone) {
        StringBuilder sql = new StringBuilder("UPDATE users SET ");
        java.util.List<Object> params = new java.util.ArrayList<>();

        if (email != null)   { sql.append("email=?, ");   params.add(email); }
        if (dob != null)     { sql.append("dob=?, ");     params.add(Date.valueOf(dob)); }
        if (address != null) { sql.append("address=?, "); params.add(address); }
        if (pin != null)     { sql.append("pin=?, ");     params.add(pin); }
        if (phone != null)   { sql.append("phone=?, ");   params.add(phone); }

        if (params.isEmpty()) {
            log.info("patchUserByAuthToken: no fields provided");
            return false; // nothing to update
        }

        // remove trailing comma+space
        sql.setLength(sql.length() - 2);
        sql.append(" WHERE auth_token = ?");
        params.add(token);

        log.debug("patchUserByAuthToken SQL={}", sql);

        try (Connection conn = DriverManager.getConnection(url, dbUser, dbPassword);
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {

            for (int i = 0; i < params.size(); i++) {
                Object p = params.get(i);
                if (p instanceof Date d) {
                    ps.setDate(i + 1, d);
                } else {
                    ps.setString(i + 1, (String) p);
                }
            }

            int updated = ps.executeUpdate();
            log.info("patchUserByAuthToken: updated={}", updated);
            return updated > 0;
        } catch (SQLException e) {
            log.error("patchUserByAuthToken failed (SQLState={}, ErrorCode={}, Message={})",
                    e.getSQLState(), e.getErrorCode(), e.getMessage(), e);
            return false;
        }
    }
}
