import java.io.Serializable;
import java.util.Date;

public class Message implements Serializable {
    private static final long serialVersionUID = 1L;
    
    public enum MessageType {
        CHAT,
        FILE,
        LOGIN,
        LOGOUT,
        SYSTEM
    }
    
    private MessageType type;
    private String sender;
    private String content;
    private byte[] fileData;
    private String fileName;
    private Date timestamp;
    
    // Constructor for text messages (CHAT, LOGIN, LOGOUT, SYSTEM)
    public Message(MessageType type, String sender, String content) {
        this.type = type;
        this.sender = sender;
        this.content = content;
        this.timestamp = new Date();
    }
    
    // Constructor for file messages
    public Message(MessageType type, String sender, String fileName, byte[] fileData) {
        this.type = type;
        this.sender = sender;
        this.fileName = fileName;
        this.fileData = fileData;
        this.timestamp = new Date();
    }
    
    public MessageType getType() {
        return type;
    }
    
    public String getSender() {
        return sender;
    }
    
    public String getContent() {
        return content;
    }
    
    public byte[] getFileData() {
        return fileData;
    }
    
    public String getFileName() {
        return fileName;
    }
    
    public Date getTimestamp() {
        return timestamp;
    }
}
