package org.shared;

import java.sql.*;
import javax.crypto.*;
import javax.crypto.spec.*;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

public class SQliteManager {

    private final String url = "jdbc:sqlite:ffmpegserverdb.db";
    private final String ENCRYPTION_KEY;

    public SQliteManager(String encryptionKey) throws Exception {
        this.ENCRYPTION_KEY = encryptionKey;
        initDatabase();
    }

    private void initDatabase() throws SQLException {
        try (Connection conn = DriverManager.getConnection(url);
             Statement stmt = conn.createStatement()) {
            // Create jobs table
            stmt.execute("CREATE TABLE IF NOT EXISTS jobs (id INTEGER PRIMARY KEY, filename TEXT, ip TEXT, time TEXT)");
            // Create clients table
            stmt.execute("CREATE TABLE IF NOT EXISTS clients (ip TEXT PRIMARY KEY, password TEXT)");
        }
    }

    public void addJob(String filename, String ip, String time) throws SQLException {
        try (Connection conn = DriverManager.getConnection(url);
             PreparedStatement pstmt = conn.prepareStatement("INSERT INTO jobs(filename, ip, time) VALUES(?, ?, ?)")) {
            pstmt.setString(1, filename);
            pstmt.setString(2, ip);
            pstmt.setString(3, time);
            pstmt.executeUpdate();
        }
    }

    public List<String> getJobs() throws SQLException {
        return getJobs(null);
    }

    // Get all jobs for a specific IP (or all jobs if IP is null)
    public List<String> getJobs(String ip) throws SQLException {
        List<String> jobs = new ArrayList<>();

        String query = ip == null ?
                "SELECT * FROM jobs" :
                "SELECT * FROM jobs WHERE ip = ?";

        try (Connection conn = DriverManager.getConnection(url);
             PreparedStatement pstmt = conn.prepareStatement(query)) {

            if (ip != null) {
                pstmt.setString(1, ip);
            }

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    String jobDetail = "ID: " + rs.getInt("id") + ", Filename: " + rs.getString("filename") + ", IP: " + rs.getString("ip") + ", Time: " + rs.getString("time");
                    jobs.add(jobDetail);
                }
            }
        }
        return jobs;
    }



    public void addClientPassword(String ip, String password) throws Exception {
        // Encrypt the password
        String encryptedPassword = encryptPassword(password);

        try (Connection conn = DriverManager.getConnection(url);
             PreparedStatement pstmt = conn.prepareStatement("INSERT OR REPLACE INTO clients(ip, password) VALUES(?, ?)")) {
            pstmt.setString(1, ip);
            pstmt.setString(2, encryptedPassword);
            pstmt.executeUpdate();
        }
    }

    private String getStoredPassword(String ip) throws SQLException {
        try (Connection conn = DriverManager.getConnection(url);
             PreparedStatement pstmt = conn.prepareStatement("SELECT password FROM clients WHERE ip = ?")) {
            pstmt.setString(1, ip);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("password");
                } else {
                    return null;
                }
            }
        }
    }

    public boolean verifyClientPassword(String ip, String providedPassword) throws Exception {
        String storedEncryptedPassword = getStoredPassword(ip);
        if (storedEncryptedPassword == null) {
            // No password stored for this IP
            return false;
        }

        String encryptedProvidedPassword = encryptPassword(providedPassword);

        return storedEncryptedPassword.equals(encryptedProvidedPassword);
    }

    public ResultSet executeCustomQuery(String query) throws SQLException {
        Connection conn = DriverManager.getConnection(url);  // Don't close connection for a custom query
        Statement stmt = conn.createStatement();
        return stmt.executeQuery(query);
    }
    private static final byte[] SALT = {
            (byte) 0x21, (byte) 0x22, (byte) 0x23, (byte) 0x24,
            (byte) 0x25, (byte) 0x26, (byte) 0x27, (byte) 0x28
    };  // Ideally, this should be generated and stored securely.
    private static final int ITERATION_COUNT = 65536;
    private String encryptPassword(String password) throws Exception {
        PBEKeySpec pbeKeySpec = new PBEKeySpec(ENCRYPTION_KEY.toCharArray(), SALT, ITERATION_COUNT);
        SecretKeyFactory secretKeyFactory = SecretKeyFactory.getInstance("PBEWithMD5AndDES");
        SecretKey secretKey = secretKeyFactory.generateSecret(pbeKeySpec);

        Cipher cipher = Cipher.getInstance("PBEWithMD5AndDES");
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, new PBEParameterSpec(SALT, ITERATION_COUNT));

        byte[] encryptedBytes = cipher.doFinal(password.getBytes());
        return Base64.getEncoder().encodeToString(encryptedBytes);
    }
}