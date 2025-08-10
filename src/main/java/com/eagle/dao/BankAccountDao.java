package com.eagle.dao;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.math.BigDecimal;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class BankAccountDao {
    private static final Logger log = LogManager.getLogger(BankAccountDao.class);
    private static final Random RAND = new Random();

    private final String url = "jdbc:derby:memory:eagleDB;create=true";
    private final String dbUser = "";
    private final String dbPassword = "";

    public static class AccountRecord {
        public int id;
        public int userId;
        public String accountNumber;
        public BigDecimal balance;  // 2dp
    }

    public BankAccountDao() {
        createTableIfNotExists();
    }

    private void createTableIfNotExists() {
        String sql = "CREATE TABLE bank_accounts (" +
                "id INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY, " +
                "user_id INT NOT NULL, " +
                "account_number VARCHAR(64) UNIQUE NOT NULL, " +
                "balance DECIMAL(18,2) DEFAULT 0 NOT NULL, " +
                "CONSTRAINT fk_accounts_user FOREIGN KEY (user_id) REFERENCES users(id)" +
                ")";
        try (Connection conn = DriverManager.getConnection(url, dbUser, dbPassword);
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.executeUpdate();
            log.info("Table 'bank_accounts' created");
        } catch (SQLException e) {
            if ("X0Y32".equals(e.getSQLState())) {
                log.debug("Table 'bank_accounts' already exists");
            } else {
                log.error("Error creating table 'bank_accounts' ({} / {}): {}",
                        e.getSQLState(), e.getErrorCode(), e.getMessage(), e);
            }
        }
    }

    /** Create an account for the user; generates account number if null/blank. */
    public AccountRecord createForUser(int userId, String accountNumber) {
        String acct = (accountNumber == null || accountNumber.isBlank())
                ? generateAccountNumber()
                : accountNumber.trim();

        String sql = "INSERT INTO bank_accounts (user_id, account_number, balance) VALUES (?, ?, 0)";
        try (Connection conn = DriverManager.getConnection(url, dbUser, dbPassword);
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, userId);
            ps.setString(2, acct);
            int updated = ps.executeUpdate();
            if (updated == 0) return null;

            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    AccountRecord r = new AccountRecord();
                    r.id = keys.getInt(1);
                    r.userId = userId;
                    r.accountNumber = acct;
                    r.balance = BigDecimal.ZERO;
                    return r;
                }
            }
        } catch (SQLException e) {
            if ("23505".equals(e.getSQLState())) { // unique violation: try a new number once if we generated it
                if (accountNumber == null || accountNumber.isBlank()) {
                    return createForUser(userId, generateAccountNumber());
                }
            }
            log.error("createForUser failed ({} / {}): {}", e.getSQLState(), e.getErrorCode(), e.getMessage(), e);
        }
        return null;
    }

    /** Get an account by id. */
    public AccountRecord getById(int id) {
        String sql = "SELECT id, user_id, account_number, balance FROM bank_accounts WHERE id = ?";
        try (Connection conn = DriverManager.getConnection(url, dbUser, dbPassword);
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;
                AccountRecord r = new AccountRecord();
                r.id = rs.getInt("id");
                r.userId = rs.getInt("user_id");
                r.accountNumber = rs.getString("account_number");
                r.balance = rs.getBigDecimal("balance");
                return r;
            }
        } catch (SQLException e) {
            log.error("getById failed ({} / {}): {}", e.getSQLState(), e.getErrorCode(), e.getMessage(), e);
            return null;
        }
    }

    /** List accounts for a user. */
    public List<AccountRecord> listByUserId(int userId) {
        String sql = "SELECT id, user_id, account_number, balance FROM bank_accounts WHERE user_id = ? ORDER BY id";
        List<AccountRecord> out = new ArrayList<>();
        try (Connection conn = DriverManager.getConnection(url, dbUser, dbPassword);
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    AccountRecord r = new AccountRecord();
                    r.id = rs.getInt("id");
                    r.userId = rs.getInt("user_id");
                    r.accountNumber = rs.getString("account_number");
                    r.balance = rs.getBigDecimal("balance");
                    out.add(r);
                }
            }
        } catch (SQLException e) {
            log.error("listByUserId failed ({} / {}): {}", e.getSQLState(), e.getErrorCode(), e.getMessage(), e);
        }
        return out;
    }

    /** Delete if balance == 0. Returns true if deleted. */
    public boolean deleteIfZeroBalance(int accountId, int ownerUserId) {
        // Do it in one SQL with owner + balance check for safety
        String sql = "DELETE FROM bank_accounts WHERE id = ? AND user_id = ? AND balance = 0";
        try (Connection conn = DriverManager.getConnection(url, dbUser, dbPassword);
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, accountId);
            ps.setInt(2, ownerUserId);
            int updated = ps.executeUpdate();
            return updated > 0;
        } catch (SQLException e) {
            log.error("deleteIfZeroBalance failed ({} / {}): {}", e.getSQLState(), e.getErrorCode(), e.getMessage(), e);
            return false;
        }
    }

    private String generateAccountNumber() {
        StringBuilder sb = new StringBuilder(12);
        for (int i = 0; i < 12; i++) sb.append(RAND.nextInt(10));
        return sb.toString();
    }
}
