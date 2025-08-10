package com.eagle.service;

import com.eagle.dao.UserDao;

public class UserService {
    private final UserDao userDao;

    public UserService() {
        this.userDao = new UserDao();
    }

    public boolean signup(String username, String password) {
        // Optional: hash password before saving
        String hashedPassword = hashPassword(password);
        return userDao.saveUser(username, hashedPassword);
    }

    private String hashPassword(String password) {
        // Simple SHA-256 hashing
        try {
            java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");
            byte[] encodedHash = digest.digest(password.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : encodedHash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            throw new RuntimeException("Error hashing password", e);
        }
    }
}
