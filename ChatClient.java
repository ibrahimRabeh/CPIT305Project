import java.io.*;
import java.net.*;
import java.util.Scanner;

public class ChatClient {
    private String hostname;
    private int port;
    private String userName;
    private Socket socket;
    private ObjectOutputStream out;
    private ObjectInputStream in;
    private ChatGUI gui;
    
    public ChatClient(String hostname, int port) {
        this.hostname = hostname;
        this.port = port;
    }
    
    public void setGUI(ChatGUI gui) {
        this.gui = gui;
    }
    
    public String getUserName() {
        return userName;
    }
    
    public void execute() {
        try {
            socket = new Socket(hostname, port);
            System.out.println("Connected to the chat server.");

            out = new ObjectOutputStream(socket.getOutputStream());
            in = new ObjectInputStream(socket.getInputStream());
            
            // Send a login message.
            sendMessage(new Message(Message.MessageType.LOGIN, userName, userName + " logged in."));
            
            // Start a new thread to read messages from the server.
            new Thread(new ReadThread()).start();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }
    
    private byte[] readFile(File file) {
        try {
            FileInputStream fis = new FileInputStream(file);
            byte[] data = new byte[(int) file.length()];
            fis.read(data);
            fis.close();
            return data;
        } catch (IOException ex) {
            ex.printStackTrace();
            return null;
        }
    }
    
    public void sendMessage(Message message) {
        try {
            out.writeObject(message);
            out.flush();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }
    
    public void sendFile(File file) {
        if (!file.exists()) {
            System.out.println("File not found.");
            return;
        }
        byte[] fileData = readFile(file);
        Message fileMessage = new Message(Message.MessageType.FILE, userName, file.getName(), fileData);
        sendMessage(fileMessage);
        System.out.println("File sent: " + file.getName());
    }
    
    private void closeConnection() {
        try {
            if (in != null) in.close();
            if (out != null) out.close();
            if (socket != null) socket.close();
            System.out.println("Disconnected from server.");
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }
    
    public void setUserName(String userName) {
        this.userName = userName;
    }
    
    // Thread to continuously read messages from the server.
    private class ReadThread implements Runnable {
        public void run() {
            try {
                while (true) {
                    Message message = (Message) in.readObject();
                    if (message != null) {
                        String displayMessage;
                        switch(message.getType()) {
                            case CHAT:
                                displayMessage = "[" + message.getSender() + "]: " + message.getContent();
                                break;
                            case FILE:
                                displayMessage = "[" + message.getSender() + "] sent a file: " + message.getFileName();
                                break;
                            case SYSTEM:
                                displayMessage = "[System]: " + message.getContent();
                                break;
                            default:
                                continue;
                        }
                        if (gui != null) {
                            gui.appendMessage(displayMessage);
                        } else {
                            System.out.println(displayMessage);
                        }
                    }
                }
            } catch (IOException | ClassNotFoundException ex) {
                System.out.println("Server connection closed.");
            }
        }
    }
    
    public static void main(String[] args) {
        String hostname = "localhost";
        int port = 8800;
        Scanner scanner = new Scanner(System.in);
        
        System.out.println("Enter your username:");
        String userName = scanner.nextLine();
        System.out.println("Enter your password:");
        String password = scanner.nextLine();
        
        // Authenticate the user via InMemoryDB
        if (!InMemoryDB.authenticate(userName, password)) {
            System.out.println("Authentication failed. Exiting.");
            System.exit(0);
        }
        System.out.println("Authentication successful. Welcome, " + userName + "!");
        
        ChatClient client = new ChatClient(hostname, port);
        client.setUserName(userName);
        client.execute();
    }
}
