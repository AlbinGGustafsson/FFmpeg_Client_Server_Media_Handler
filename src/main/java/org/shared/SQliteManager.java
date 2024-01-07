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

/**
 * En klass som hanterar kommunikation med en SQLite-databas.
 * Inkluderar funktionalitet för krypterade lösenord.
 */
public class SQliteManager {

    private final String url = "jdbc:sqlite:ffmpegserverdb.db";
    private final String ENCRYPTION_KEY;

    /**
     * Initierar databasen.
     *
     * @param encryptionKey Krypteringsnyckeln som användsför lösenorden i databasen.
     * @throws Exception Om det uppstår ett undantag vid initialiseringen av databasen.
     */
    public SQliteManager(String encryptionKey) throws Exception {
        this.ENCRYPTION_KEY = encryptionKey;
        initDatabase();
    }

    /**
     * Skapar ett nytt table om det inte redan finns.
     */
    private void initDatabase() throws SQLException {
        try (Connection conn = DriverManager.getConnection(url);
             Statement stmt = conn.createStatement()) {
            // Create jobs table with a 'status' column
            stmt.execute("CREATE TABLE IF NOT EXISTS jobs (id INTEGER PRIMARY KEY, filename TEXT, ip TEXT, time TEXT, status TEXT)");
            // Create clients table
            stmt.execute("CREATE TABLE IF NOT EXISTS clients (ip TEXT PRIMARY KEY, password TEXT)");
        }
    }

    /**
     * Lägger till ett jobb i databasen.
     *
     * @param filename Filnamnet för jobbet.
     * @param ip       IP-adressen för klienten som skickade jobbet.
     * @param time     Tidpunkten då jobbet skapades.
     * @param status   Statusen för jobbet.
     * @throws SQLException Om det uppstår ett SQL-relaterat fel.
     */
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


    /**
     * Hämtar en lista med jobb från databasen baserat på en valfri IP-adress.
     *
     * @param ip IP-adressen för klienten (kan vara null för att hämta alla jobb).
     * @return En lista med jobb som matchar filtreringen.
     * @throws SQLException Om det uppstår ett SQL-relaterat fel.
     */
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

    /**
     * Metod för att köra custom query:s mot databasen.
     */
    public ResultSet executeCustomQuery(String query) throws SQLException {
        Connection conn = DriverManager.getConnection(url);
        Statement stmt = conn.createStatement();
        return stmt.executeQuery(query);
    }

    /**
     * Lägger till eller ersätter ett lösenord i databasen för en specifik IP-adress.
     *
     * @param ip       IP-adressen för klienten.
     * @param password Lösenordet som ska läggas till eller uppdateras.
     * @throws Exception Om det uppstår ett undantag vid kryptering eller databasoperationer.
     */
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

    /**
     * Hämtar och returnerar lösenordet för en given IP-adress från databasen.
     *
     * @param ip IP-adressen för klienten vars lösenord ska hämtas.
     * @return Det lagrade lösenordet om det finns, annars null.
     * @throws SQLException Om det uppstår ett SQL-relaterat fel.
     */
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

    /**
     * Verifierar om det angivna lösenordet matchar det lagrade lösenordet för en given IP-adress.
     *
     * @param ip IP-adressen för klienten vars lösenord ska verifieras.
     * @param providedPassword Det angivna lösenordet som ska verifieras.
     * @return true om lösenordet är korrekt, annars false.
     */
    public boolean verifyClientPassword(String ip, String providedPassword) {
        try {
            String masterEncryptedPassword = getStoredPassword("master");
            String encryptedProvidedPassword = encryptPassword(providedPassword);

            // Kollar mot masterlösen
            if (masterEncryptedPassword != null && masterEncryptedPassword.equals(encryptedProvidedPassword)) {
                return true;
            }

            // mot specifik ip
            String storedEncryptedPassword = getStoredPassword(ip);
            if (storedEncryptedPassword == null) {
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
    };
    private static final int ITERATION_COUNT = 65536;

    /**
     * Krypterar ett lösenord med hjälp av PBEWithMD5AndDES-algoritmen.
     * Kastar massa roliga exceptions i olika lägen.
     *
     * @param password Lösenordet som ska krypteras.
     * @return Det krypterade lösenordet som en Base64-kodad sträng.
     */
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