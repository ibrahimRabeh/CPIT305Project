public class Test {
    public static void main(String[] args) {
        try {
            Class.forName("org.h2.Driver");
            System.out.println("H2 Driver loaded successfully!");
        } catch (ClassNotFoundException e) {
            System.out.println("Error loading H2 Driver: " + e.getMessage());
        }
    }
} 