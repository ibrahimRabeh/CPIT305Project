import java.util.HashMap;
import java.util.Map;

public class InMemoryDB {
    private static final Map<String, String> users = new HashMap<>();
    
    static {
        // Add some default users for testing
        users.put("admin", "admin123");
        users.put("user1", "password1");
        users.put("user2", "password2");
    }
    
    public static boolean authenticate(String username, String password) {
        String storedPassword = users.get(username);
        return storedPassword != null && storedPassword.equals(password);
    }
    
    public static boolean registerUser(String username, String password) {
        if (username == null || username.trim().isEmpty() || 
            password == null || password.trim().isEmpty()) {
            return false;
        }
        
        if (users.containsKey(username)) {
            return false; // User already exists
        }
        
        users.put(username, password);
        return true;
    }
} 