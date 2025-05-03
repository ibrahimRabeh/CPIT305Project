import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

public class ChatServer {
    private Set<ClientHandler> clientHandlers;
    private ServerSocket serverSocket;
    private ExecutorService pool;
    private PrintWriter logWriter;
    private List<Message> messageHistory;  // Added to store message history
    private static final int MAX_HISTORY_SIZE = 100;  // Limit history size
    
    public ChatServer(int port) {
        clientHandlers = ConcurrentHashMap.newKeySet();
        pool = Executors.newCachedThreadPool();
        messageHistory = Collections.synchronizedList(new ArrayList<>());  // Initialize message history
        
        try {
            serverSocket = new ServerSocket(port);
            // Open or create the log file (chat_history.txt) in append mode.
            logWriter = new PrintWriter(new FileWriter("./CPIT305Project/chat_history.txt", true), true);
            System.out.println("Server started on port " + port);
            
            // Load chat history from file
            loadChatHistory();
            
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }
    
    // Load chat history from file
    private void loadChatHistory() {
        try (BufferedReader reader = new BufferedReader(new FileReader("./CPIT305Project/chat_history.txt"))) {
            String line;
            while ((line = reader.readLine()) != null) {
                // Parse basic history format and create message objects
                if (line.matches("\\[.*\\] .*: .*")) {
                    try {
                        // Extract sender and content from log line
                        int firstColon = line.indexOf(":");
                        int secondColon = line.indexOf(":", firstColon + 1);
                        
                        if (secondColon > 0) {
                            String sender = line.substring(firstColon + 1, secondColon).trim();
                            String content = line.substring(secondColon + 1).trim();
                            
                            // Create appropriate message type
                            Message.MessageType type = Message.MessageType.CHAT;
                            if (sender.equals("Server")) {
                                type = Message.MessageType.SYSTEM;
                            } else if (content.startsWith("sent file:")) {
                                // Skip file messages in history
                                continue;
                            }
                            
                            Message historyMsg = new Message(type, sender, content);
                            messageHistory.add(historyMsg);
                            
                            // Keep history at a reasonable size
                            if (messageHistory.size() > MAX_HISTORY_SIZE) {
                                messageHistory.remove(0);
                            }
                        }
                    } catch (Exception e) {
                        System.out.println("Error parsing history line: " + line);
                    }
                }
            }
            System.out.println("Loaded " + messageHistory.size() + " messages from history");
        } catch (IOException e) {
            System.out.println("No chat history file found or error reading it. Starting with empty history.");
        }
    }
    
    public void start() {
        try {
            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("New client connected: " + clientSocket.getInetAddress());
                ClientHandler handler = new ClientHandler(clientSocket, this);
                clientHandlers.add(handler);
                pool.execute(handler);
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        } finally {
            shutdown();
        }
    }
    
    // Broadcast a message to all connected clients except the sender.
    public void broadcast(Message message, ClientHandler excludeHandler) {
        // Add to history if it's a chat or system message
        if (message.getType() == Message.MessageType.CHAT || 
            message.getType() == Message.MessageType.SYSTEM) {
            messageHistory.add(message);
            // Keep history at a reasonable size
            if (messageHistory.size() > MAX_HISTORY_SIZE) {
                messageHistory.remove(0);
            }
        }
        
        logMessage(message);
        for (ClientHandler client : clientHandlers) {
            if (client != excludeHandler) {
                client.sendMessage(message);
            }
        }
    }
    
    // Send chat history to a newly connected client
    public void sendHistoryToClient(ClientHandler client) {
        System.out.println("Sending history to client: " + client.getClientName());
        for (Message message : messageHistory) {
            client.sendMessage(message);
        }
    }
    
    // Remove a client from the set.
    public void removeClient(ClientHandler clientHandler) {
        clientHandlers.remove(clientHandler);
        System.out.println("Client disconnected: " + clientHandler.getClientName());
    }
    
    public void shutdown() {
        try {
            if (serverSocket != null) serverSocket.close();
            if (logWriter != null) logWriter.close();
            pool.shutdown();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }
    
    // Log the message to a file in a thread-safe manner.
    private synchronized void logMessage(Message message) {
        String logEntry = String.format("[%s] %s: %s", 
            message.getTimestamp(), 
            message.getSender(), 
            message.getType() == Message.MessageType.FILE ? 
                "sent file: " + message.getFileName() : 
                message.getContent());
        logWriter.println(logEntry);
    }
    
    public static void main(String[] args) {
        int port = 8800;
        ChatServer server = new ChatServer(port);
        server.start();
    }
}