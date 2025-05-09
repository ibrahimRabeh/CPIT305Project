import java.io.File;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.sql.*;
import java.util.Base64;

/**
 * A SQLite-based database manager for handling user authentication
 * without requiring external database libraries beyond the built-in JDBC.
 */
public class DBManager {
    // SQLite connection string - creates/uses a file in the current directory
    private static final String DB_URL = "jdbc:sqlite:chatapp.db";
    
    static {
        try {
            // Initialize the database
            initializeDatabase();
        } catch (Exception e) {
            System.err.println("Database initialization error: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Initialize the database, creating tables if they don't exist
     */
    private static void initializeDatabase() throws SQLException {
        // Check if the database file exists
        File dbFile = new File("chatapp.db");
        boolean dbExists = dbFile.exists();
        
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {
            
            // Create users table if it doesn't exist
            stmt.execute("CREATE TABLE IF NOT EXISTS users (" +
                        "username TEXT PRIMARY KEY, " +
                        "password TEXT NOT NULL, " +
                        "salt TEXT NOT NULL" +
                        ")");
            
            // If database didn't exist before, add default users
            if (!dbExists) {
                addUser(conn, "admin", "admin123");
                addUser(conn, "user1", "password1");
                addUser(conn, "user2", "password2");
                System.out.println("Database initialized with default users.");
            }
        }
    }
    
    /**
     * Get a database connection
     */
    private static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(DB_URL);
    }
    
    /**
     * Add a user directly to the database (internal method)
     */
    private static void addUser(Connection conn, String username, String password) throws SQLException {
        byte[] salt = generateSalt();
        String hashedPassword = hashPassword(password, salt);
        String saltBase64 = Base64.getEncoder().encodeToString(salt);
        
        String sql = "INSERT INTO users (username, password, salt) VALUES (?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            ps.setString(2, hashedPassword);
            ps.setString(3, saltBase64);
            ps.executeUpdate();
        }
    }
    
    /**
     * Generate a salt for password hashing
     */
    private static byte[] generateSalt() {
        SecureRandom random = new SecureRandom();
        byte[] salt = new byte[16];
        random.nextBytes(salt);
        return salt;
    }
    
    /**
     * Hash a password with the provided salt
     */
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
    
    /**
     * Register a new user
     */
    public static boolean registerUser(String username, String password) {
        if (username == null || username.trim().isEmpty() || 
            password == null || password.trim().isEmpty()) {
            return false;
        }
        
        try (Connection conn = getConnection()) {
            // Check if username already exists
            String checkSql = "SELECT COUNT(*) FROM users WHERE username = ?";
            try (PreparedStatement checkPs = conn.prepareStatement(checkSql)) {
                checkPs.setString(1, username);
                try (ResultSet rs = checkPs.executeQuery()) {
                    if (rs.next() && rs.getInt(1) > 0) {
                        return false; // Username already exists
                    }
                }
            }
            
            // Username is available, register the new user
            byte[] salt = generateSalt();
            String hashedPassword = hashPassword(password, salt);
            String saltBase64 = Base64.getEncoder().encodeToString(salt);
            
            String sql = "INSERT INTO users (username, password, salt) VALUES (?, ?, ?)";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, username);
                ps.setString(2, hashedPassword);
                ps.setString(3, saltBase64);
                return ps.executeUpdate() > 0;
            }
        } catch (SQLException e) {
            System.err.println("Error registering user: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * Authenticate a user
     */
    public static boolean authenticate(String username, String password) {
        if (username == null || username.trim().isEmpty() || 
            password == null || password.trim().isEmpty()) {
            return false;
        }
        
        try (Connection conn = getConnection()) {
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
            System.err.println("Error authenticating user: " + e.getMessage());
            e.printStackTrace();
        }
        return false;
    }
    
    /**
     * Change a user's password
     */
    public static boolean changePassword(String username, String oldPassword, String newPassword) {
        // First authenticate with the old password
        if (!authenticate(username, oldPassword)) {
            return false;
        }
        
        try (Connection conn = getConnection()) {
            // Generate new salt and hash for the new password
            byte[] salt = generateSalt();
            String hashedPassword = hashPassword(newPassword, salt);
            String saltBase64 = Base64.getEncoder().encodeToString(salt);
            
            // Update the password in the database
            String sql = "UPDATE users SET password = ?, salt = ? WHERE username = ?";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, hashedPassword);
                ps.setString(2, saltBase64);
                ps.setString(3, username);
                return ps.executeUpdate() > 0;
            }
        } catch (SQLException e) {
            System.err.println("Error changing password: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * Delete a user
     */
    public static boolean deleteUser(String username) {
        if (username == null || username.trim().isEmpty()) {
            return false;
        }
        
        try (Connection conn = getConnection()) {
            String sql = "DELETE FROM users WHERE username = ?";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, username);
                return ps.executeUpdate() > 0;
            }
        } catch (SQLException e) {
            System.err.println("Error deleting user: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * List all usernames (for admin purposes)
     */
    public static String[] listUsers() {
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT username FROM users ORDER BY username")) {
            
            // Count the number of users
            rs.last();
            int count = rs.getRow();
            rs.beforeFirst();
            
            // Collect all usernames
            String[] usernames = new String[count];
            int index = 0;
            while (rs.next()) {
                usernames[index++] = rs.getString("username");
            }
            
            return usernames;
        } catch (SQLException e) {
            System.err.println("Error listing users: " + e.getMessage());
            e.printStackTrace();
            return new String[0];
        }
    }
    
    /**
     * Check if a user exists
     */
    public static boolean userExists(String username) {
        try (Connection conn = getConnection()) {
            String sql = "SELECT COUNT(*) FROM users WHERE username = ?";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, username);
                try (ResultSet rs = ps.executeQuery()) {
                    return rs.next() && rs.getInt(1) > 0;
                }
            }
        } catch (SQLException e) {
            System.err.println("Error checking if user exists: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * Count the number of registered users
     */
    public static int getUserCount() {
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM users")) {
            
            if (rs.next()) {
                return rs.getInt(1);
            }
            return 0;
        } catch (SQLException e) {
            System.err.println("Error counting users: " + e.getMessage());
            e.printStackTrace();
            return 0;
        }
    }
    
    /**
     * Get database file path
     */
    public static String getDatabasePath() {
        File dbFile = new File("chatapp.db");
        return dbFile.getAbsolutePath();
    }

    /**
     * Test the database functionality
     */
    public static void main(String[] args) {
        try {
            System.out.println("Database file location: " + getDatabasePath());
            System.out.println("Total users: " + getUserCount());
            
            System.out.println("\nAll users:");
            String[] users = listUsers();
            for (String user : users) {
                System.out.println("- " + user);
            }
            
            // Test authentication
            System.out.println("\nTesting authentication:");
            System.out.println("admin/admin123: " + authenticate("admin", "admin123"));
            System.out.println("admin/wrongpass: " + authenticate("admin", "wrongpass"));
            
            // Test adding a new user
            String testUser = "testuser_" + System.currentTimeMillis();
            System.out.println("\nAdding new user '" + testUser + "':");
            boolean registered = registerUser(testUser, "testpass");
            System.out.println("User registered: " + registered);
            System.out.println("Authentication for new user: " + authenticate(testUser, "testpass"));
            
            // Test changing password
            System.out.println("\nChanging password for '" + testUser + "':");
            boolean changed = changePassword(testUser, "testpass", "newpass");
            System.out.println("Password changed: " + changed);
            System.out.println("Authentication with old password: " + authenticate(testUser, "testpass"));
            System.out.println("Authentication with new password: " + authenticate(testUser, "newpass"));
            
            // Clean up test user
            System.out.println("\nDeleting test user:");
            boolean deleted = deleteUser(testUser);
            System.out.println("User deleted: " + deleted);
            System.out.println("User still exists: " + userExists(testUser));
            
        } catch (Exception e) {
            System.err.println("Error in test: " + e.getMessage());
            e.printStackTrace();
        }
    }
}