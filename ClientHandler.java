import java.io.*;
import java.net.*;

public class ClientHandler implements Runnable {
    private Socket clientSocket;
    private ChatServer server;
    private ObjectOutputStream out;
    private ObjectInputStream in;
    private String clientName;
    
    public ClientHandler(Socket socket, ChatServer server) {
        this.clientSocket = socket;
        this.server = server;
    }
    
    public void run() {
        try {
            out = new ObjectOutputStream(clientSocket.getOutputStream());
            in = new ObjectInputStream(clientSocket.getInputStream());
            
            // Expect first message to be a LOGIN message.
            Message loginMsg = (Message) in.readObject();
            if (loginMsg.getType() == Message.MessageType.LOGIN) {
                clientName = loginMsg.getSender();
                System.out.println("User logged in: " + clientName);
                // Broadcast to notify others.
                Message sysMsg = new Message(Message.MessageType.SYSTEM, "Server", clientName + " has joined the chat.");
                server.broadcast(sysMsg, this);
            }
            
            Message message;
            while ((message = (Message) in.readObject()) != null) {
                switch(message.getType()) {
                    case CHAT:
                        System.out.println("[" + clientName + "]: " + message.getContent());
                        server.broadcast(message, this);
                        break;
                    case FILE:
                        System.out.println("[" + clientName + "] sent a file: " + message.getFileName());
                        server.broadcast(message, this);
                        saveFile(message);
                        break;
                    case LOGOUT:
                        System.out.println(clientName + " requested logout.");
                        Message byeMsg = new Message(Message.MessageType.SYSTEM, "Server", clientName + " has left the chat.");
                        server.broadcast(byeMsg, this);
                        closeConnection();
                        return;
                    default:
                        break;
                }
            }
        } catch (IOException | ClassNotFoundException ex) {
            System.out.println("Connection lost with client " + clientName);
        } finally {
            closeConnection();
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
    
    public String getClientName() {
        return clientName;
    }
    
    // Saves the file sent by the client into the "received_files" directory.
    private void saveFile(Message fileMessage) {
        try {
            File dir = new File("received_files");
            if (!dir.exists()) {
                dir.mkdir();
            }
            File file = new File(dir, fileMessage.getFileName());
            try (FileOutputStream fos = new FileOutputStream(file)) {
                fos.write(fileMessage.getFileData());
            }
            System.out.println("File saved: " + file.getAbsolutePath());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    private void closeConnection() {
        try {
            if (in != null) in.close();
            if (out != null) out.close();
            if (clientSocket != null) clientSocket.close();
            server.removeClient(this);
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }
}
