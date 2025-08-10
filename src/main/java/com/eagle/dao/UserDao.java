package com.eagle.dao;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

public class UserDao {
    private final String url = "jdbc:derby:eagleDB;create=true"; // embedded Derby DB
    private final String dbUser = "";
    private final String dbPassword = "";

    public UserDao() {
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
            stmt.executeUpdate();
            System.out.println("Table 'users' created.");
        } catch (SQLException e) {
            // Derby error code X0Y32 means table already exists
            if (!"X0Y32".equals(e.getSQLState())) {
                System.err.println("Error creating table: " + e.getMessage());
            }
        }
    }

    public boolean saveUser(String username, String password) {
        String sql = "INSERT INTO users (username, password) VALUES (?, ?)";
        try (Connection conn = DriverManager.getConnection(url, dbUser, dbPassword);
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, username);
            stmt.setString(2, password);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Error saving user: " + e.getMessage());
            return false;
        }
    }

    public List<String> getAllUsers() {
        List<String> users = new ArrayList<>();
        String sql = "SELECT username FROM users";
        try (Connection conn = DriverManager.getConnection(url, dbUser, dbPassword);
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                users.add(rs.getString("username"));
            }
        } catch (SQLException e) {
            System.err.println("Error fetching users: " + e.getMessage());
        }
        return users;
    }
    public boolean validateUser(String username, String password) {
        final String sql = "SELECT COUNT(*) FROM users WHERE username=? AND password=?";
        try (var conn = java.sql.DriverManager.getConnection(url, dbUser, dbPassword);
             var ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            ps.setString(2, password); // TODO: switch to hashed later
            try (var rs = ps.executeQuery()) {
                rs.next(); return rs.getInt(1) > 0;
            }
        } catch (java.sql.SQLException e) {
            System.err.println("validateUser: " + e.getMessage());
            return false;
        }
    }

}
