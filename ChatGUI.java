import java.awt.*;
import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.swing.*;

public class ChatGUI {
    private JFrame frame;
    private JTextArea chatArea;
    private JScrollPane scrollPane;
    private JTextField messageField;
    private JButton sendButton;
    private JButton fileButton;
    private JButton logoutButton;
    private ChatClient client;
    
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
            
            // Print working directory for debugging
            
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
        frame = new JFrame("Chat Application - " + client.getUserName());
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(600, 400);
        
        chatArea = new JTextArea();
        chatArea.setEditable(false);
        scrollPane = new JScrollPane(chatArea);
        
        messageField = new JTextField();
        sendButton = new JButton("Send");
        fileButton = new JButton("Send File");
        logoutButton = new JButton("Logout");
        
        JPanel bottomPanel = new JPanel(new BorderLayout());
        bottomPanel.add(messageField, BorderLayout.CENTER);
        
        JPanel buttonPanel = new JPanel(new FlowLayout());
        buttonPanel.add(sendButton);
        buttonPanel.add(fileButton);
        buttonPanel.add(logoutButton);
        bottomPanel.add(buttonPanel, BorderLayout.EAST);
        
        frame.add(scrollPane, BorderLayout.CENTER);
        frame.add(bottomPanel, BorderLayout.SOUTH);
        
        sendButton.addActionListener(e -> sendMessage());
        messageField.addActionListener(e -> sendMessage());
        fileButton.addActionListener(e -> sendFile());
        logoutButton.addActionListener(e -> logout());
        
        frame.setVisible(true);
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
        // Show login dialog
        JTextField usernameField = new JTextField(20);
        JPasswordField passwordField = new JPasswordField(20);
        
        JPanel loginPanel = new JPanel(new GridLayout(2, 2));
        loginPanel.add(new JLabel("Username:"));
        loginPanel.add(usernameField);
        loginPanel.add(new JLabel("Password:"));
        loginPanel.add(passwordField);
        
        int result = JOptionPane.showConfirmDialog(null, loginPanel, 
            "Login", JOptionPane.OK_CANCEL_OPTION);
            
        if (result == JOptionPane.OK_OPTION) {
            String username = usernameField.getText();
            String password = new String(passwordField.getPassword());
            
            // Use InMemoryDB for authentication
            if (InMemoryDB.authenticate(username, password)) {
                ChatClient client = new ChatClient("localhost", 8800);
                client.setUserName(username);
                
                // Create and show the GUI
                SwingUtilities.invokeLater(() -> {
                    new ChatGUI(client);
                    client.execute();
                });
            } else {
                JOptionPane.showMessageDialog(null, "Invalid username or password");
                System.exit(0);
            }
        } else {
            System.exit(0);
        }
    }
}