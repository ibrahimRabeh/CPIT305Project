import java.awt.*;
import java.io.File;
import javax.swing.*;

public class ChatGUI {
    private JFrame frame;
    private JTextArea chatArea;
    private JTextField messageField;
    private JButton sendButton;
    private JButton fileButton;
    private JButton logoutButton;
    private ChatClient client;
    
    public ChatGUI(ChatClient client) {
        this.client = client;
        client.setGUI(this);
        createAndShowGUI();
    }
    
    private void createAndShowGUI() {
        // Create the main frame
        frame = new JFrame("Chat Application - " + client.getUserName());
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(600, 400);
        
        // Create components
        chatArea = new JTextArea();
        chatArea.setEditable(false);
        JScrollPane scrollPane = new JScrollPane(chatArea);
        
        messageField = new JTextField();
        sendButton = new JButton("Send");
        fileButton = new JButton("Send File");
        logoutButton = new JButton("Logout");
        
        // Create bottom panel
        JPanel bottomPanel = new JPanel(new BorderLayout());
        bottomPanel.add(messageField, BorderLayout.CENTER);
        
        JPanel buttonPanel = new JPanel(new FlowLayout());
        buttonPanel.add(sendButton);
        buttonPanel.add(fileButton);
        buttonPanel.add(logoutButton);
        bottomPanel.add(buttonPanel, BorderLayout.EAST);
        
        // Add components to frame
        frame.add(scrollPane, BorderLayout.CENTER);
        frame.add(bottomPanel, BorderLayout.SOUTH);
        
        // Add action listeners
        sendButton.addActionListener(e -> sendMessage());
        messageField.addActionListener(e -> sendMessage());
        fileButton.addActionListener(e -> sendFile());
        logoutButton.addActionListener(e -> logout());
        
        // Show the frame
        frame.setVisible(true);
    }
    
    private void sendMessage() {
        String message = messageField.getText().trim();
        if (!message.isEmpty()) {
            client.sendMessage(new Message(Message.MessageType.CHAT, client.getUserName(), message));
            messageField.setText("");
        }
    }
    
    private void sendFile() {
        JFileChooser fileChooser = new JFileChooser();
        int result = fileChooser.showOpenDialog(frame);
        if (result == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fileChooser.getSelectedFile();
            client.sendFile(selectedFile);
        }
    }
    
    private void logout() {
        client.sendMessage(new Message(Message.MessageType.LOGOUT, client.getUserName(), "Logging out"));
        frame.dispose();
        System.exit(0);
    }
    
    public void appendMessage(String message) {
        chatArea.append(message + "\n");
        chatArea.setCaretPosition(chatArea.getDocument().getLength());
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
            
            if (DBManager.authenticate(username, password)) {
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