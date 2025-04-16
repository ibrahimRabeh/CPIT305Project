import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.sql.*;
import java.util.Base64;

public class DBManager {
    private static final String DB_URL = "jdbc:h2:mem:chatdb;DB_CLOSE_DELAY=-1";
    private static final String DB_USER = "sa";
    private static final String DB_PASSWORD = "";
    
    static {
        try {
            // Load the H2 JDBC driver
            Class.forName("org.h2.Driver");
            
            // Initialize the database
            try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
                 Statement stmt = conn.createStatement()) {
                
                // Create users table if it doesn't exist
                stmt.execute("CREATE TABLE IF NOT EXISTS users (" +
                    "username VARCHAR(50) PRIMARY KEY, " +
                    "password VARCHAR(100) NOT NULL, " +
                    "salt VARCHAR(100) NOT NULL" +  // Changed to VARCHAR to store Base64 string
                    ")");
                
                // Insert some test users if the table is empty
                ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM users");
                rs.next();
                if (rs.getInt(1) == 0) {
                    // Add default users
                    addUser(stmt, "admin", "admin123");
                    addUser(stmt, "user1", "password1");
                    addUser(stmt, "user2", "password2");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    private static void addUser(Statement stmt, String username, String password) throws SQLException {
        byte[] salt = generateSalt();
        String hashedPassword = hashPassword(password, salt);
        String saltBase64 = Base64.getEncoder().encodeToString(salt);
        
        stmt.execute(String.format(
            "INSERT INTO users (username, password, salt) VALUES ('%s', '%s', '%s')",
            username, hashedPassword, saltBase64
        ));
    }
    
    private static String hashPassword(String password, byte[] salt) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            md.update(salt);
            byte[] hashedPassword = md.digest(password.getBytes());
            return Base64.getEncoder().encodeToString(hashedPassword);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Error hashing password", e);
        }
    }
    
    private static byte[] generateSalt() {
        SecureRandom random = new SecureRandom();
        byte[] salt = new byte[16];
        random.nextBytes(salt);
        return salt;
    }
    
    public static boolean registerUser(String username, String password) {
        if (username == null || username.trim().isEmpty() || 
            password == null || password.trim().isEmpty()) {
            return false;
        }
        
        byte[] salt = generateSalt();
        String hashedPassword = hashPassword(password, salt);
        String saltBase64 = Base64.getEncoder().encodeToString(salt);
        
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD)) {
            String sql = "INSERT INTO users (username, password, salt) VALUES (?, ?, ?)";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, username);
                ps.setString(2, hashedPassword);
                ps.setString(3, saltBase64);
                return ps.executeUpdate() > 0;
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
    
    public static boolean authenticate(String username, String password) {
        if (username == null || username.trim().isEmpty() || 
            password == null || password.trim().isEmpty()) {
            return false;
        }
        
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD)) {
            String sql = "SELECT password, salt FROM users WHERE username = ?";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, username);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        String storedPassword = rs.getString("password");
                        String saltBase64 = rs.getString("salt");
                        byte[] salt = Base64.getDecoder().decode(saltBase64);
                        String hashedPassword = hashPassword(password, salt);
                        return storedPassword.equals(hashedPassword);
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }
}
