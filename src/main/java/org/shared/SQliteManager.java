package org.shared;

import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
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
            // Create jobs table with a 'status' column
            stmt.execute("CREATE TABLE IF NOT EXISTS jobs (id INTEGER PRIMARY KEY, filename TEXT, ip TEXT, time TEXT, status TEXT)");
            // Create clients table
            stmt.execute("CREATE TABLE IF NOT EXISTS clients (ip TEXT PRIMARY KEY, password TEXT)");
        }
    }

    public void addJob(String filename, String ip, String time, JobStatus status) throws SQLException {
        try (Connection conn = DriverManager.getConnection(url);
             PreparedStatement pstmt = conn.prepareStatement("INSERT INTO jobs(filename, ip, time, status) VALUES(?, ?, ?, ?)")) {
            pstmt.setString(1, filename);
            pstmt.setString(2, ip);
            pstmt.setString(3, time);
            pstmt.setString(4, status.name());
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
                    String jobDetail = "ID: " + rs.getInt("id") + ", Filename: " + rs.getString("filename") + ", IP: " + rs.getString("ip") + ", Time: " + rs.getString("time") + ", Status: " + rs.getString("status");
                    jobs.add(jobDetail);
                }
            }
        }
        return jobs;
    }

    public ResultSet executeCustomQuery(String query) throws SQLException {
        Connection conn = DriverManager.getConnection(url);  // Don't close connection for a custom query
        Statement stmt = conn.createStatement();
        return stmt.executeQuery(query);
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

    public boolean verifyClientPassword(String ip, String providedPassword) {
        try {
            String masterEncryptedPassword = getStoredPassword("master");
            String encryptedProvidedPassword = encryptPassword(providedPassword);

            // Check against master password
            if (masterEncryptedPassword != null && masterEncryptedPassword.equals(encryptedProvidedPassword)) {
                return true;
            }

            // If not master, check the password for the specific IP
            String storedEncryptedPassword = getStoredPassword(ip);
            if (storedEncryptedPassword == null) {
                // No password stored for this IP
                return false;
            }

            return storedEncryptedPassword.equals(encryptedProvidedPassword);
        } catch (SQLException | InvalidAlgorithmParameterException | NoSuchPaddingException |
                 IllegalBlockSizeException | NoSuchAlgorithmException | InvalidKeySpecException |
                 BadPaddingException | InvalidKeyException e) {

            e.printStackTrace();
            return false;
        }
    }

    private static final byte[] SALT = {
            (byte) 0x21, (byte) 0x22, (byte) 0x23, (byte) 0x24,
            (byte) 0x25, (byte) 0x26, (byte) 0x27, (byte) 0x28
    };  // Ideally, this should be generated and stored securely.
    private static final int ITERATION_COUNT = 65536;
    private String encryptPassword(String password) throws NoSuchAlgorithmException, InvalidKeySpecException, NoSuchPaddingException, InvalidAlgorithmParameterException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException {
        PBEKeySpec pbeKeySpec = new PBEKeySpec(ENCRYPTION_KEY.toCharArray(), SALT, ITERATION_COUNT);
        SecretKeyFactory secretKeyFactory = SecretKeyFactory.getInstance("PBEWithMD5AndDES");
        SecretKey secretKey = secretKeyFactory.generateSecret(pbeKeySpec);

        Cipher cipher = Cipher.getInstance("PBEWithMD5AndDES");
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, new PBEParameterSpec(SALT, ITERATION_COUNT));

        byte[] encryptedBytes = cipher.doFinal(password.getBytes());
        return Base64.getEncoder().encodeToString(encryptedBytes);
    }
}