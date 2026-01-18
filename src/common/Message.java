package common;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Represents a message in the chat system.
 * This class encapsulates all message-related information including
 * sender, content, timestamp, and message type.
 */
public class Message implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private String sender;
    private String content;
    private LocalDateTime timestamp;
    private MessageType type;
    private String recipient; // For private messages
    private String fileName;  // For file/image messages
    
    /**
     * Message types supported by the chat system
     */
    public enum MessageType {
        NORMAL,      // Regular broadcast message
        PRIVATE,     // Private message to specific user
        SYSTEM,      // System notification
        JOIN,        // User joined
        LEAVE,       // User left
        USER_LIST,   // List of connected users
        IMAGE,       // Image message (content is base64)
        FILE,        // File message (content is base64)
        VOICE        // Voice message (content is base64 audio)
    }
    
    /**
     * Constructor for creating a new message
     * 
     * @param sender The username of the message sender
     * @param content The message content
     * @param type The type of message
     */
    public Message(String sender, String content, MessageType type) {
        this.sender = sender;
        this.content = content;
        this.timestamp = LocalDateTime.now();
        this.type = type;
        this.recipient = null;
    }
    
    /**
     * Constructor for private messages
     * 
     * @param sender The username of the message sender
     * @param content The message content
     * @param type The type of message
     * @param recipient The username of the recipient
     */
    public Message(String sender, String content, MessageType type, String recipient) {
        this(sender, content, type);
        this.recipient = recipient;
    }
    
    // Getters and Setters
    
    public String getSender() {
        return sender;
    }
    
    public String getContent() {
        return content;
    }
    
    public LocalDateTime getTimestamp() {
        return timestamp;
    }
    
    public MessageType getType() {
        return type;
    }
    
    public String getRecipient() {
        return recipient;
    }
    
    public void setRecipient(String recipient) {
        this.recipient = recipient;
    }
    
    public String getFileName() {
        return fileName;
    }
    
    public void setFileName(String fileName) {
        this.fileName = fileName;
    }
    
    /**
     * Get formatted timestamp string
     * 
     * @return Formatted timestamp (HH:mm:ss)
     */
    public String getFormattedTimestamp() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm:ss");
        return timestamp.format(formatter);
    }
    
    /**
     * Convert message to protocol string format
     * Format: TYPE|SENDER|TIMESTAMP|CONTENT|RECIPIENT|FILENAME
     * 
     * @return String representation for network transmission
     */
    public String toProtocolString() {
        StringBuilder sb = new StringBuilder();
        sb.append(type.name()).append("|");
        sb.append(sender).append("|");
        sb.append(getFormattedTimestamp()).append("|");
        sb.append(content.replace("|", "<!PIPE!>")); // Escape pipes in content
        
        if (recipient != null && !recipient.isEmpty()) {
            sb.append("|").append(recipient);
        } else {
            sb.append("|"); // Empty recipient field
        }
        
        if (fileName != null && !fileName.isEmpty()) {
            sb.append("|").append(fileName);
        }
        
        return sb.toString();
    }
    
    /**
     * Parse a protocol string to create a Message object
     * 
     * @param protocolString The string in protocol format
     * @return Message object
     */
    public static Message fromProtocolString(String protocolString) {
        String[] parts = protocolString.split("\\|", 6);
        
        if (parts.length < 4) {
            return null;
        }
        
        MessageType type = MessageType.valueOf(parts[0]);
        String sender = parts[1];
        String content = parts[3].replace("<!PIPE!>", "|"); // Unescape pipes
        
        Message message = new Message(sender, content, type);
        
        // Set recipient if it exists (for private messages, images, files)
        if (parts.length >= 5 && !parts[4].isEmpty()) {
            message.setRecipient(parts[4]);
        }
        
        // Set filename if it exists (for images and files)
        if (parts.length == 6 && !parts[5].isEmpty()) {
            message.setFileName(parts[5]);
        }
        
        return message;
    }
    
    @Override
    public String toString() {
        return String.format("[%s] %s: %s", getFormattedTimestamp(), sender, content);
    }
}
