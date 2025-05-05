import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.swing.*;
import javax.swing.border.EmptyBorder;

public class ChatGUI {
    private JFrame frame;
    private JTextArea chatArea;
    private JScrollPane scrollPane;
    private JTextField messageField;
    private JButton sendButton;
    private JButton fileButton;
    private JButton logoutButton;
    private ChatClient client;
    
    // Colors and styling
    private static final Color BACKGROUND_COLOR = new Color(240, 240, 245);
    private static final Color TEXT_AREA_COLOR = new Color(248, 248, 255);
    private static final Color TEXT_COLOR = new Color(25, 25, 25);
    private static final Color HEADER_COLOR = new Color(60, 90, 153);
    private static final Color BUTTON_COLOR = new Color(79, 129, 189);
    private static final Color BUTTON_TEXT_COLOR = new Color(25, 25, 25);
    private static final Font MAIN_FONT = new Font("Arial", Font.PLAIN, 16);
    private static final Font CHAT_FONT = new Font("Arial", Font.PLAIN, 18);
    private static final Font HEADER_FONT = new Font("Arial", Font.BOLD, 18);
    
    public ChatGUI(ChatClient client) {
        this.client = client;
        client.setGUI(this);
        createAndShowGUI();
        
        // Load only important history messages
        loadFilteredChatHistory();
    }
    
    private void loadFilteredChatHistory() {
        try {
            // Check if chat_history.txt exists in the current directory
            File historyFile = new File("chat_history.txt");
            
            if (historyFile.exists()) {
                
                BufferedReader reader = new BufferedReader(new FileReader(historyFile));
                String line;
                List<String> importantMessages = new ArrayList<>();
                
                // Pattern to match the log format
                Pattern pattern = Pattern.compile("\\[(.*?)\\] (.*?): (.*)");
                
                while ((line = reader.readLine()) != null) {
                    Matcher matcher = pattern.matcher(line);
                    if (matcher.find()) {
                        String sender = matcher.group(2).trim();
                        String content = matcher.group(3).trim();
                        
                        // Skip single character spam messages and short garbage
                        if (content.length() <= 2 && !content.contains("file:")) {
                            continue;
                        }
                        
                        // Skip repeated 'g' characters
                        if (content.matches("g+")) {
                            continue;
                        }
                        
                        if (sender.equals("Server")) {
                            importantMessages.add("[System]: " + content);
                        } else if (content.startsWith("sent file:")) {
                            importantMessages.add("[" + sender + "] sent a file: " + content.substring(10).trim());
                        } else {
                            importantMessages.add("[" + sender + "]: " + content);
                        }
                    }
                }
                reader.close();
                
                // Show only the last 20 important messages
                int startIndex = Math.max(0, importantMessages.size() - 20);
                for (int i = startIndex; i < importantMessages.size(); i++) {
                    appendMessage(importantMessages.get(i));
                }
            } else {
                appendMessage("[System]: Chat history file not found in current directory.");
                
                // Try to look for the file in the parent directory
                File parentDirHistoryFile = new File("../chat_history.txt");
                if (parentDirHistoryFile.exists()) {
                    appendMessage("[System]: Found chat history in parent directory: " + parentDirHistoryFile.getAbsolutePath());
                } else {
                    appendMessage("[System]: No chat history file found.");
                }
            }
        } catch (Exception e) {
            appendMessage("[System]: Failed to load chat history: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private void createAndShowGUI() {
        // Get screen dimensions for responsive sizing
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        int width = (int)(screenSize.width * 0.7);
        int height = (int)(screenSize.height * 0.7);
        
        frame = new JFrame("Chat Application - " + client.getUserName());
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(width, height);
        frame.setMinimumSize(new Dimension(800, 600));
        frame.setLocationRelativeTo(null); // Center the window
        frame.getContentPane().setBackground(BACKGROUND_COLOR);
        
        // Create a header panel
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(HEADER_COLOR);
        headerPanel.setBorder(new EmptyBorder(10, 10, 10, 10));
        
        JLabel titleLabel = new JLabel("Connected as: " + client.getUserName());
        titleLabel.setForeground(Color.WHITE);
        titleLabel.setFont(HEADER_FONT);
        headerPanel.add(titleLabel, BorderLayout.WEST);
        
        // Chat area with custom styling
        chatArea = new JTextArea();
        chatArea.setEditable(false);
        chatArea.setFont(CHAT_FONT);
        chatArea.setBackground(TEXT_AREA_COLOR);
        chatArea.setForeground(TEXT_COLOR);
        chatArea.setLineWrap(true);
        chatArea.setWrapStyleWord(true);
        chatArea.setBorder(new EmptyBorder(15, 15, 15, 15));
        
        scrollPane = new JScrollPane(chatArea);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        
        // Message input area
        messageField = new JTextField();
        messageField.setFont(new Font("Arial", Font.PLAIN, 16));
        messageField.setForeground(TEXT_COLOR);
        messageField.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(180, 180, 180)),
            BorderFactory.createEmptyBorder(8, 8, 8, 8)));
        
        // Style buttons
        sendButton = createStyledButton("Send");
        fileButton = createStyledButton("Send File");
        logoutButton = createStyledButton("Logout");
        
        // Layout for input and buttons
        JPanel messagePanel = new JPanel(new BorderLayout(5, 0));
        messagePanel.setBorder(new EmptyBorder(10, 10, 10, 10));
        messagePanel.setBackground(BACKGROUND_COLOR);
        messagePanel.add(messageField, BorderLayout.CENTER);
        
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 0));
        buttonPanel.setBackground(BACKGROUND_COLOR);
        buttonPanel.add(sendButton);
        buttonPanel.add(fileButton);
        buttonPanel.add(logoutButton);
        
        messagePanel.add(buttonPanel, BorderLayout.EAST);
        
        // Main layout
        frame.setLayout(new BorderLayout());
        frame.add(headerPanel, BorderLayout.NORTH);
        frame.add(scrollPane, BorderLayout.CENTER);
        frame.add(messagePanel, BorderLayout.SOUTH);
        
        // Add window resize listener
        frame.addComponentListener(new ComponentAdapter() {
            public void componentResized(ComponentEvent e) {
                // Adjust components if needed
                scrollPane.getVerticalScrollBar().setValue(scrollPane.getVerticalScrollBar().getMaximum());
            }
        });
        
        // Add event listeners
        sendButton.addActionListener(e -> sendMessage());
        messageField.addActionListener(e -> sendMessage());
        fileButton.addActionListener(e -> sendFile());
        logoutButton.addActionListener(e -> logout());
        
        frame.setVisible(true);
    }
    
    private JButton createStyledButton(String text) {
        JButton button = new JButton(text);
        button.setBackground(BUTTON_COLOR);
        button.setForeground(BUTTON_TEXT_COLOR);
        button.setFont(new Font("Arial", Font.BOLD, 16));
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        button.setPreferredSize(new Dimension(110, 36));
        
        // Add hover effect
        button.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                button.setBackground(BUTTON_COLOR.darker());
            }
            
            @Override
            public void mouseExited(MouseEvent e) {
                button.setBackground(BUTTON_COLOR);
            }
        });
        
        return button;
    }
    
    public void appendMessage(String message) {
        SwingUtilities.invokeLater(() -> {
            chatArea.append(message + "\n");
            // These two lines ensure the scroll pane shows the latest messages
            chatArea.setCaretPosition(chatArea.getDocument().getLength());
            scrollPane.getVerticalScrollBar().setValue(scrollPane.getVerticalScrollBar().getMaximum());
        });
    }
    
    private void sendMessage() {
        String message = messageField.getText().trim();
        if (!message.isEmpty()) {
            // Echo the message in the local UI immediately
            appendMessage("[" + client.getUserName() + "]: " + message);
            
            // Send to server
            client.sendMessage(new Message(Message.MessageType.CHAT, client.getUserName(), message));
            messageField.setText("");
        }
    }
    
    private void sendFile() {
        JFileChooser fileChooser = new JFileChooser();
        int result = fileChooser.showOpenDialog(frame);
        if (result == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fileChooser.getSelectedFile();
            
            // Echo the file send in the UI
            appendMessage("[" + client.getUserName() + "] sent a file: " + selectedFile.getName());
            
            // Send to server
            client.sendFile(selectedFile);
        }
    }
    
    private void logout() {
        client.sendMessage(new Message(Message.MessageType.LOGOUT, client.getUserName(), "Logging out"));
        frame.dispose();
        System.exit(0);
    }

    
    public static void main(String[] args) {
        try {
            // Set system look and feel
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        // Show login dialog with enhanced styling
        SwingUtilities.invokeLater(() -> showLoginDialog());
    }
    
    private static void showLoginDialog() {
        // Use fixed size for login dialog - more appropriate than percentage based
        int loginWidth = 500;
        int loginHeight = 500;
        
        // Create custom styled components
        JTextField usernameField = new JTextField(20);
        JPasswordField passwordField = new JPasswordField(20);
        
        // Style text fields
        usernameField.setFont(new Font("Arial", Font.PLAIN, 16));
        passwordField.setFont(new Font("Arial", Font.PLAIN, 16));
        
        // Create a styled panel
        JPanel loginPanel = new JPanel();
        loginPanel.setLayout(new BoxLayout(loginPanel, BoxLayout.Y_AXIS));
        loginPanel.setBackground(BACKGROUND_COLOR);
        loginPanel.setBorder(new EmptyBorder(20, 20, 20, 20));
        
        // Add logo or welcome text
        JLabel welcomeLabel = new JLabel("Welcome to Chat");
        welcomeLabel.setFont(new Font("Arial", Font.BOLD, 24));
        welcomeLabel.setForeground(HEADER_COLOR);
        welcomeLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        loginPanel.add(welcomeLabel);
        
        loginPanel.add(Box.createRigidArea(new Dimension(0, 20)));
        
        // Username field with label
        JPanel usernamePanel = new JPanel();
        usernamePanel.setLayout(new BoxLayout(usernamePanel, BoxLayout.Y_AXIS));
        usernamePanel.setBackground(BACKGROUND_COLOR);
        usernameField.setForeground(Color.BLACK);
        JLabel userLabel = new JLabel("Username:");
        userLabel.setForeground(Color.BLACK);
        userLabel.setFont(new Font("Arial", Font.PLAIN, 20));
        usernamePanel.add(userLabel);
        usernamePanel.add(Box.createRigidArea(new Dimension(0, 5)));
        usernamePanel.add(usernameField);
        loginPanel.add(usernamePanel);
        
        loginPanel.add(Box.createRigidArea(new Dimension(0, 15)));
        
        // Password field with label
        JPanel passwordPanel = new JPanel();
        passwordPanel.setLayout(new BoxLayout(passwordPanel, BoxLayout.Y_AXIS));
        passwordPanel.setBackground(BACKGROUND_COLOR);
        passwordField.setForeground(Color.BLACK);
        JLabel passLabel = new JLabel("Password:");
        passLabel.setForeground(Color.BLACK);
        passLabel.setFont(new Font("Arial", Font.PLAIN, 20));
        passLabel.setBackground(TEXT_COLOR);
        passwordPanel.add(passLabel);
        passwordPanel.add(Box.createRigidArea(new Dimension(0, 5)));
        passwordPanel.add(passwordField);
        loginPanel.add(passwordPanel);
        
        // Create custom dialog
        final JDialog loginDialog = new JDialog((Frame)null, "Login", true);
        loginDialog.setSize(loginWidth, loginHeight);
        loginDialog.setLocationRelativeTo(null);
        loginDialog.setResizable(false);
        
        // Button panel
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonPanel.setBackground(BACKGROUND_COLOR);
        
        JButton loginButton = new JButton("Login");
        loginButton.setBackground(BUTTON_COLOR);
        loginButton.setForeground(BUTTON_TEXT_COLOR);
        loginButton.setFont(new Font("Arial", Font.BOLD, 16));
        loginButton.setPreferredSize(new Dimension(100, 40));
        loginButton.setFocusPainted(false);
        JButton cancelButton = new JButton("Cancel");
        cancelButton.setFont(new Font("Arial", Font.BOLD, 16));
        cancelButton.setPreferredSize(new Dimension(100, 40));
        cancelButton.setFocusPainted(false);
        
        buttonPanel.add(loginButton);
        buttonPanel.add(cancelButton);
        
        loginPanel.add(Box.createRigidArea(new Dimension(0, 20)));
        loginPanel.add(buttonPanel);
        
        loginDialog.add(loginPanel);
        
        // Button actions
        final boolean[] result = {false};
        
        loginButton.addActionListener(e -> {
            String username = usernameField.getText();
            String password = new String(passwordField.getPassword());
            
            // Use InMemoryDB for authentication
            if (InMemoryDB.authenticate(username, password)) {
                result[0] = true;
                loginDialog.dispose();
                
                // Start client
                ChatClient client = new ChatClient("localhost", 8800);
                client.setUserName(username);
                
                // Create and show the GUI
                SwingUtilities.invokeLater(() -> {
                    new ChatGUI(client);
                    client.execute();
                });
            } else {
                JOptionPane.showMessageDialog(loginDialog, 
                    "Invalid username or password", 
                    "Authentication Failed", 
                    JOptionPane.ERROR_MESSAGE);
            }
        });
        
        cancelButton.addActionListener(e -> {
            loginDialog.dispose();
            System.exit(0);
        });
        
        // Allow Enter key to submit
        passwordField.addActionListener(e -> loginButton.doClick());
        
        loginDialog.setVisible(true);
        
        // If dialog was closed without login
        if (!result[0]) {
            System.exit(0);
        }
    }
}