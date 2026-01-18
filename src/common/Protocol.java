package common;

/**
 * Defines the communication protocol between client and server.
 * Contains constants and utility methods for protocol operations.
 */
public class Protocol {
    
    // Protocol Commands
    public static final String CMD_JOIN = "JOIN";
    public static final String CMD_LEAVE = "LEAVE";
    public static final String CMD_MESSAGE = "MESSAGE";
    public static final String CMD_PRIVATE = "PRIVATE";
    public static final String CMD_USER_LIST = "USER_LIST";
    public static final String CMD_ERROR = "ERROR";
    
    // Default server configuration
    public static final int DEFAULT_PORT = 8888;
    public static final String DEFAULT_HOST = "localhost";
    
    // Message delimiters
    public static final String DELIMITER = "|";
    public static final String PRIVATE_MSG_PREFIX = "/msg";
    
    /**
     * Check if a message is a private message command
     * Format: /msg username message content
     * 
     * @param message The message to check
     * @return true if it's a private message command
     */
    public static boolean isPrivateMessage(String message) {
        return message != null && message.trim().startsWith(PRIVATE_MSG_PREFIX);
    }
    
    /**
     * Parse private message to extract recipient and content
     * Format: /msg username message content
     * 
     * @param message The private message command
     * @return Array [recipient, content] or null if invalid
     */
    public static String[] parsePrivateMessage(String message) {
        if (!isPrivateMessage(message)) {
            return null;
        }
        
        String[] parts = message.trim().split("\\s+", 3);
        
        if (parts.length < 3) {
            return null; // Invalid format
        }
        
        String recipient = parts[1];
        String content = parts[2];
        
        return new String[]{recipient, content};
    }
    
    /**
     * Create a standardized error message
     * 
     * @param errorMessage The error description
     * @return Formatted error message
     */
    public static String createErrorMessage(String errorMessage) {
        return CMD_ERROR + DELIMITER + errorMessage;
    }
    
    /**
     * Validate username format
     * 
     * @param username The username to validate
     * @return true if valid, false otherwise
     */
    public static boolean isValidUsername(String username) {
        if (username == null || username.trim().isEmpty()) {
            return false;
        }
        
        // Username should be 2-20 characters, alphanumeric and underscores only
        return username.matches("^[a-zA-Z0-9_]{2,20}$");
    }
}
