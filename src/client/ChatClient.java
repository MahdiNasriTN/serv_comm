package client;

import common.Message;
import common.Protocol;
import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

/**
 * ChatClient - Handles the client-side networking logic.
 * Manages connection to the server, sending messages,
 * and receiving messages in a separate thread.
 */
public class ChatClient {
    
    private Socket socket;
    private BufferedReader input;
    private PrintWriter output;
    private String username;
    private boolean connected;
    
    // Listener for incoming messages
    private MessageListener messageListener;
    
    // Thread for receiving messages
    private Thread receiveThread;
    
    /**
     * Interface for message listeners
     */
    public interface MessageListener {
        void onMessageReceived(Message message);
        void onConnectionStatusChanged(boolean connected, String statusMessage);
        void onUserListUpdated(List<String> users);
    }
    
    /**
     * Constructor
     */
    public ChatClient() {
        this.connected = false;
    }
    
    /**
     * Set the message listener
     * 
     * @param listener The listener to set
     */
    public void setMessageListener(MessageListener listener) {
        this.messageListener = listener;
    }
    
    /**
     * Connect to the chat server
     * 
     * @param host Server hostname or IP
     * @param port Server port
     * @param username Desired username
     * @return true if connection successful
     */
    public boolean connect(String host, int port, String username) {
        try {
            System.out.println("[ChatClient] Starting connection to " + host + ":" + port);
            // Validate username format
            if (!Protocol.isValidUsername(username)) {
                System.out.println("[ChatClient] Username validation failed");
                notifyConnectionStatus(false, "Invalid username format. Use 2-20 alphanumeric characters.");
                return false;
            }
            
            this.username = username;
            System.out.println("[ChatClient] Username validated: " + username);
            
            // Create socket connection
            socket = new Socket(host, port);
            System.out.println("[ChatClient] Socket connected");
            
            // Initialize streams
            input = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            output = new PrintWriter(socket.getOutputStream(), true);
            System.out.println("[ChatClient] Streams initialized");
            
            // Authenticate with server
            System.out.println("[ChatClient] Starting authentication...");
            if (!authenticate()) {
                System.out.println("[ChatClient] Authentication failed");
                disconnect();
                return false;
            }
            System.out.println("[ChatClient] Authentication succeeded");
            
            connected = true;
            System.out.println("[ChatClient] Connected flag set to true");
            
            // Notify successful connection BEFORE starting receive thread
            // to ensure GUI updates immediately
            System.out.println("[ChatClient] About to notify connection status...");
            notifyConnectionStatus(true, "Connected to server as " + username);
            System.out.println("[ChatClient] Connection status notified");
            
            // Start receiving messages
            System.out.println("[ChatClient] Starting receive thread...");
            startReceiving();
            System.out.println("[ChatClient] Receive thread started");
            
            return true;
            
        } catch (IOException e) {
            System.out.println("[ChatClient] IOException caught: " + e.getMessage());
            e.printStackTrace();
            notifyConnectionStatus(false, "Connection failed: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Authenticate with the server
     * 
     * @return true if authentication successful
     */
    private boolean authenticate() throws IOException {
        System.out.println("[ChatClient] authenticate() called");
        // Wait for username request
        System.out.println("[ChatClient] Waiting for server response...");
        String serverResponse = input.readLine();
        System.out.println("[ChatClient] Received: " + serverResponse);
        
        if (!"ENTER_USERNAME".equals(serverResponse)) {
            System.out.println("[ChatClient] Unexpected response, expected ENTER_USERNAME");
            notifyConnectionStatus(false, "Unexpected server response");
            return false;
        }
        
        // Send username
        System.out.println("[ChatClient] Sending username: " + username);
        output.println(username);
        
        // Wait for authentication result
        System.out.println("[ChatClient] Waiting for auth result...");
        serverResponse = input.readLine();
        System.out.println("[ChatClient] Auth response: " + serverResponse);
        
        if (serverResponse == null) {
            System.out.println("[ChatClient] Server disconnected during auth");
            notifyConnectionStatus(false, "Server disconnected");
            return false;
        }
        
        // Authentication response must be SUCCESS or ERROR
        // If we receive a protocol message (e.g., USER_LIST), it means SUCCESS was already sent
        // and buffered, so we need to check if the response is a protocol message
        if (serverResponse.startsWith("SUCCESS")) {
            System.out.println("[ChatClient] SUCCESS response received");
            return true;
        } else if (serverResponse.startsWith("ERROR")) {
            System.out.println("[ChatClient] ERROR response received");
            String[] parts = serverResponse.split("\\|", 2);
            String errorMessage = parts.length > 1 ? parts[1] : "Authentication failed";
            notifyConnectionStatus(false, errorMessage);
            return false;
        } else {
            // This might be a protocol message that arrived too early
            // This shouldn't happen with proper server implementation, but let's handle it
            System.out.println("[ChatClient] Received protocol message instead of auth response: " + serverResponse);
            System.out.println("[ChatClient] This indicates a server-side race condition");
            notifyConnectionStatus(false, "Authentication protocol error");
            return false;
        }
    }
    
    /**
     * Start the thread that receives messages from the server
     */
    private void startReceiving() {
        receiveThread = new Thread(() -> {
            try {
                String messageString;
                while (connected && (messageString = input.readLine()) != null) {
                    processIncomingMessage(messageString);
                }
            } catch (IOException e) {
                if (connected) {
                    notifyConnectionStatus(false, "Connection lost: " + e.getMessage());
                }
            } finally {
                disconnect();
            }
        });
        
        receiveThread.setDaemon(true);
        receiveThread.start();
    }
    
    /**
     * Process an incoming message from the server
     * 
     * @param messageString The raw message string
     */
    private void processIncomingMessage(String messageString) {
        try {
            Message message = Message.fromProtocolString(messageString);
            
            if (message == null) {
                return;
            }
            
            // Handle different message types
            switch (message.getType()) {
                case USER_LIST:
                    // Update user list
                    String[] users = message.getContent().split(",");
                    List<String> userList = new ArrayList<>();
                    for (String user : users) {
                        if (!user.trim().isEmpty()) {
                            userList.add(user.trim());
                        }
                    }
                    notifyUserListUpdate(userList);
                    break;
                    
                case NORMAL:
                case PRIVATE:
                case SYSTEM:
                case JOIN:
                case LEAVE:
                    // Notify listener
                    if (messageListener != null) {
                        messageListener.onMessageReceived(message);
                    }
                    break;
                    
                default:
                    break;
            }
            
        } catch (Exception e) {
            System.err.println("Error processing message: " + e.getMessage());
        }
    }
    
    /**
     * Send a regular message
     * 
     * @param content The message content
     */
    public void sendMessage(String content) {
        if (!connected || output == null) {
            return;
        }
        
        // Check if it's a private message command
        if (Protocol.isPrivateMessage(content)) {
            String[] parsed = Protocol.parsePrivateMessage(content);
            if (parsed != null) {
                sendPrivateMessage(parsed[0], parsed[1]);
            } else {
                // Invalid private message format
                Message errorMsg = new Message("System", 
                    "Invalid private message format. Use: /msg username message", 
                    Message.MessageType.SYSTEM);
                if (messageListener != null) {
                    messageListener.onMessageReceived(errorMsg);
                }
            }
        } else {
            // Regular message
            Message message = new Message(username, content, Message.MessageType.NORMAL);
            output.println(message.toProtocolString());
        }
    }
    
    /**
     * Send a private message to a specific user
     * 
     * @param recipient The recipient's username
     * @param content The message content
     */
    public void sendPrivateMessage(String recipient, String content) {
        if (!connected || output == null) {
            return;
        }
        
        Message message = new Message(username, content, Message.MessageType.PRIVATE, recipient);
        output.println(message.toProtocolString());
    }
    
    /**
     * Send an image file
     * 
     * @param imageData Base64 encoded image data
     * @param fileName The image file name
     * @param recipient The recipient username (null for broadcast)
     */
    public void sendImage(String imageData, String fileName, String recipient) {
        if (!connected || output == null) {
            return;
        }
        
        Message.MessageType type = (recipient != null && !recipient.isEmpty()) ? 
            Message.MessageType.PRIVATE : Message.MessageType.IMAGE;
        
        Message message = new Message(username, imageData, type);
        message.setFileName(fileName);
        if (recipient != null && !recipient.isEmpty()) {
            message.setRecipient(recipient);
        }
        
        output.println(message.toProtocolString());
    }
    
    /**
     * Send a file
     * 
     * @param fileData Base64 encoded file data
     * @param fileName The file name
     * @param recipient The recipient username (null for broadcast)
     */
    public void sendFile(String fileData, String fileName, String recipient) {
        if (!connected || output == null) {
            return;
        }
        
        Message message = new Message(username, fileData, Message.MessageType.FILE);
        message.setFileName(fileName);
        if (recipient != null && !recipient.isEmpty()) {
            message.setRecipient(recipient);
        }
        
        output.println(message.toProtocolString());
    }
    
    /**
     * Disconnect from the server
     */
    public void disconnect() {
        if (!connected) {
            return;
        }
        
        connected = false;
        
        try {
            // Send leave message
            if (output != null) {
                Message leaveMessage = new Message(username, "Leaving", Message.MessageType.LEAVE);
                output.println(leaveMessage.toProtocolString());
            }
            
            // Close streams and socket
            if (input != null) {
                input.close();
            }
            if (output != null) {
                output.close();
            }
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
            
            notifyConnectionStatus(false, "Disconnected from server");
            
        } catch (IOException e) {
            System.err.println("Error disconnecting: " + e.getMessage());
        }
    }
    
    /**
     * Check if currently connected
     * 
     * @return true if connected
     */
    public boolean isConnected() {
        return connected;
    }
    
    /**
     * Get the current username
     * 
     * @return The username
     */
    public String getUsername() {
        return username;
    }
    
    /**
     * Notify listener of connection status change
     * 
     * @param connected Connection status
     * @param message Status message
     */
    private void notifyConnectionStatus(boolean connected, String message) {
        System.out.println("[ChatClient] Status update: connected=" + connected + ", message=" + message);
        if (messageListener != null) {
            messageListener.onConnectionStatusChanged(connected, message);
        } else {
            System.err.println("[ChatClient] WARNING: messageListener is null!");
        }
    }
    
    /**
     * Notify listener of user list update
     * 
     * @param users List of connected users
     */
    private void notifyUserListUpdate(List<String> users) {
        if (messageListener != null) {
            messageListener.onUserListUpdated(users);
        }
    }
}
