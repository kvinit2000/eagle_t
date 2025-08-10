package com.eagle.dao;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.math.BigDecimal;
import java.sql.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class TransactionDao {
    private static final Logger log = LogManager.getLogger(TransactionDao.class);

    private final String url = "jdbc:derby:memory:eagleDB;create=true";
    private final String dbUser = "";
    private final String dbPassword = "";

    public static class TxRecord {
        public int id;
        public int accountId;
        public String type;         // "DEPOSIT" | "WITHDRAW"
        public BigDecimal amount;   // >0
        public Timestamp createdAt; // insertion time
        public BigDecimal balanceAfter;
    }

    public TransactionDao() {
        createTableIfNotExists();
    }

    private void createTableIfNotExists() {
        String sql = "CREATE TABLE transactions (" +
                "id INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY, " +
                "account_id INT NOT NULL, " +
                "type VARCHAR(16) NOT NULL, " +
                "amount DECIMAL(18,2) NOT NULL, " +
                "created_at TIMESTAMP NOT NULL, " +
                "balance_after DECIMAL(18,2) NOT NULL, " +
                "CONSTRAINT fk_tx_account FOREIGN KEY (account_id) REFERENCES bank_accounts(id)" +
                ")";
        try (Connection conn = DriverManager.getConnection(url, dbUser, dbPassword);
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.executeUpdate();
            log.info("Table 'transactions' created");
        } catch (SQLException e) {
            if ("X0Y32".equals(e.getSQLState())) {
                log.debug("Table 'transactions' already exists");
            } else {
                log.error("Error creating 'transactions' ({} / {}): {}", e.getSQLState(), e.getErrorCode(), e.getMessage(), e);
            }
        }
    }

    /** Atomic deposit. Returns created TxRecord or null. */
    public TxRecord deposit(int accountId, BigDecimal amount) {
        if (amount == null || amount.signum() <= 0) return null;

        String updateSql =
                "UPDATE bank_accounts SET balance = balance + ? WHERE id = ?";
        String selectBalanceSql =
                "SELECT balance FROM bank_accounts WHERE id = ?";
        String insertTxSql =
                "INSERT INTO transactions (account_id, type, amount, created_at, balance_after) VALUES (?, ?, ?, ?, ?)";

        try (Connection conn = DriverManager.getConnection(url, dbUser, dbPassword)) {
            conn.setAutoCommit(false);
            try (PreparedStatement up = conn.prepareStatement(updateSql)) {
                up.setBigDecimal(1, amount);
                up.setInt(2, accountId);
                int updated = up.executeUpdate();
                if (updated != 1) {
                    conn.rollback();
                    return null;
                }
            }

            BigDecimal newBal;
            try (PreparedStatement sb = conn.prepareStatement(selectBalanceSql)) {
                sb.setInt(1, accountId);
                try (ResultSet rs = sb.executeQuery()) {
                    if (!rs.next()) { conn.rollback(); return null; }
                    newBal = rs.getBigDecimal(1);
                }
            }

            Timestamp now = Timestamp.from(Instant.now());
            try (PreparedStatement ins = conn.prepareStatement(insertTxSql, Statement.RETURN_GENERATED_KEYS)) {
                ins.setInt(1, accountId);
                ins.setString(2, "DEPOSIT");
                ins.setBigDecimal(3, amount.setScale(2));
                ins.setTimestamp(4, now);
                ins.setBigDecimal(5, newBal.setScale(2));
                ins.executeUpdate();
                try (ResultSet keys = ins.getGeneratedKeys()) {
                    TxRecord tx = new TxRecord();
                    if (keys.next()) tx.id = keys.getInt(1);
                    tx.accountId = accountId;
                    tx.type = "DEPOSIT";
                    tx.amount = amount.setScale(2);
                    tx.createdAt = now;
                    tx.balanceAfter = newBal.setScale(2);
                    conn.commit();
                    return tx;
                }
            } catch (SQLException ex) {
                conn.rollback();
                throw ex;
            }
        } catch (SQLException e) {
            log.error("deposit failed ({} / {}): {}", e.getSQLState(), e.getErrorCode(), e.getMessage(), e);
            return null;
        }
    }

    /** Atomic withdraw (checks sufficient funds). Returns TxRecord or null if insufficient. */
    public TxRecord withdraw(int accountId, BigDecimal amount) {
        if (amount == null || amount.signum() <= 0) return null;

        String updateSql =
                "UPDATE bank_accounts SET balance = balance - ? WHERE id = ? AND balance >= ?";
        String selectBalanceSql =
                "SELECT balance FROM bank_accounts WHERE id = ?";
        String insertTxSql =
                "INSERT INTO transactions (account_id, type, amount, created_at, balance_after) VALUES (?, ?, ?, ?, ?)";

        try (Connection conn = DriverManager.getConnection(url, dbUser, dbPassword)) {
            conn.setAutoCommit(false);
            try (PreparedStatement up = conn.prepareStatement(updateSql)) {
                up.setBigDecimal(1, amount);
                up.setInt(2, accountId);
                up.setBigDecimal(3, amount);
                int updated = up.executeUpdate();
                if (updated != 1) {
                    conn.rollback(); // insufficient funds or missing account
                    return null;
                }
            }

            BigDecimal newBal;
            try (PreparedStatement sb = conn.prepareStatement(selectBalanceSql)) {
                sb.setInt(1, accountId);
                try (ResultSet rs = sb.executeQuery()) {
                    if (!rs.next()) { conn.rollback(); return null; }
                    newBal = rs.getBigDecimal(1);
                }
            }

            Timestamp now = Timestamp.from(Instant.now());
            try (PreparedStatement ins = conn.prepareStatement(insertTxSql, Statement.RETURN_GENERATED_KEYS)) {
                ins.setInt(1, accountId);
                ins.setString(2, "WITHDRAW");
                ins.setBigDecimal(3, amount.setScale(2));
                ins.setTimestamp(4, now);
                ins.setBigDecimal(5, newBal.setScale(2));
                ins.executeUpdate();
                try (ResultSet keys = ins.getGeneratedKeys()) {
                    TxRecord tx = new TxRecord();
                    if (keys.next()) tx.id = keys.getInt(1);
                    tx.accountId = accountId;
                    tx.type = "WITHDRAW";
                    tx.amount = amount.setScale(2);
                    tx.createdAt = now;
                    tx.balanceAfter = newBal.setScale(2);
                    conn.commit();
                    return tx;
                }
            } catch (SQLException ex) {
                conn.rollback();
                throw ex;
            }
        } catch (SQLException e) {
            log.error("withdraw failed ({} / {}): {}", e.getSQLState(), e.getErrorCode(), e.getMessage(), e);
            return null;
        }
    }

    /** List newest-first. */
    public List<TxRecord> listByAccountId(int accountId, int limit, int offset) {
        String sql = "SELECT id, account_id, type, amount, created_at, balance_after " +
                "FROM transactions WHERE account_id=? ORDER BY id DESC FETCH FIRST ? ROWS ONLY";
        List<TxRecord> out = new ArrayList<>();
        try (Connection conn = DriverManager.getConnection(url, dbUser, dbPassword);
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, accountId);
            ps.setInt(2, limit <= 0 ? 100 : limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    TxRecord t = new TxRecord();
                    t.id = rs.getInt("id");
                    t.accountId = rs.getInt("account_id");
                    t.type = rs.getString("type");
                    t.amount = rs.getBigDecimal("amount");
                    t.createdAt = rs.getTimestamp("created_at");
                    t.balanceAfter = rs.getBigDecimal("balance_after");
                    out.add(t);
                }
            }
        } catch (SQLException e) {
            log.error("listByAccountId failed ({} / {}): {}", e.getSQLState(), e.getErrorCode(), e.getMessage(), e);
        }
        return out;
    }
}
