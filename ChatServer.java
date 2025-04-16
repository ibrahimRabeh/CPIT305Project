import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

public class ChatServer {
    private Set<ClientHandler> clientHandlers;
    private ServerSocket serverSocket;
    private ExecutorService pool;
    private PrintWriter logWriter;
    
    public ChatServer(int port) {
        clientHandlers = ConcurrentHashMap.newKeySet();
        pool = Executors.newCachedThreadPool();
        try {
            serverSocket = new ServerSocket(port);
            // Open or create the log file (chat_history.txt) in append mode.
            logWriter = new PrintWriter(new FileWriter("chat_history.txt", true), true);
            System.out.println("Server started on port " + port);
        } catch (IOException ex) {
            ex.printStackTrace();
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
        logMessage(message);
        for (ClientHandler client : clientHandlers) {
            if (client != excludeHandler) {
                client.sendMessage(message);
            }
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
