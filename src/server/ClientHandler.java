package server;

import common.Message;
import common.Protocol;
import java.io.*;
import java.net.Socket;

/**
 * ClientHandler - Handles communication with a single client.
 * Each instance runs in its own thread, allowing the server
 * to handle multiple clients concurrently.
 */
public class ClientHandler implements Runnable {
    
    private final Socket socket;
    private final ChatServer server;
    private BufferedReader input;
    private PrintWriter output;
    private String username;
    private boolean connected;
    
    /**
     * Constructor
     * 
     * @param socket The client's socket connection
     * @param server Reference to the main server
     */
    public ClientHandler(Socket socket, ChatServer server) {
        this.socket = socket;
        this.server = server;
        this.connected = true;
    }
    
    /**
     * Main thread execution method
     * Handles the client communication lifecycle
     */
    @Override
    public void run() {
        try {
            // Initialize input and output streams
            input = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            output = new PrintWriter(socket.getOutputStream(), true);
            
            // Authentication phase - get username
            if (!authenticate()) {
                closeConnection();
                return;
            }
            
            server.log("Client " + username + " connected from " + socket.getInetAddress());
            
            // Message handling loop
            String messageString;
            while (connected && (messageString = input.readLine()) != null) {
                processMessage(messageString);
            }
            
        } catch (IOException e) {
            if (connected) {
                server.log("Error handling client " + username + ": " + e.getMessage());
            }
        } finally {
            closeConnection();
        }
    }
    
    /**
     * Authenticate the client by getting and validating the username
     * 
     * @return true if authentication successful, false otherwise
     */
    private boolean authenticate() throws IOException {
        // Request username
        output.println("ENTER_USERNAME");
        
        // Read username
        String proposedUsername = input.readLine();
        
        if (proposedUsername == null) {
            return false;
        }
        
        proposedUsername = proposedUsername.trim();
        
        // Validate username format
        if (!Protocol.isValidUsername(proposedUsername)) {
            output.println("ERROR|Invalid username format. Use 2-20 alphanumeric characters.");
            return false;
        }
        
        // Check if username is already taken
        if (server.isUsernameTaken(proposedUsername)) {
            output.println("ERROR|Username already taken. Please try another.");
            return false;
        }
        
        // Register the client
        this.username = proposedUsername;
        if (server.registerClient(username, this)) {
            output.println("SUCCESS|Welcome to the chat, " + username + "!");
            
            // IMPORTANT: Notify others AFTER sending SUCCESS to avoid race condition
            // where broadcast messages arrive before the client finishes authentication
            server.notifyUserJoined(username);
            
            return true;
        } else {
            output.println("ERROR|Could not register username.");
            return false;
        }
    }
    
    /**
     * Process an incoming message from the client
     * 
     * @param messageString The raw message string
     */
    private void processMessage(String messageString) {
        try {
            Message message = Message.fromProtocolString(messageString);
            
            if (message == null) {
                server.log("Invalid message format from " + username);
                return;
            }
            
            // Handle different message types
            switch (message.getType()) {
                case NORMAL:
                    // Broadcast to all other clients
                    server.broadcast(message, username);
                    server.log(username + ": " + message.getContent());
                    break;
                    
                case IMAGE:
                    // Broadcast image to all other clients
                    server.broadcast(message, username);
                    server.log(username + " sent an image: " + message.getFileName());
                    break;
                    
                case FILE:
                    // Broadcast or send file privately
                    String fileRecipient = message.getRecipient();
                    if (fileRecipient != null && !fileRecipient.isEmpty()) {
                        boolean sent = server.sendPrivateMessage(message, fileRecipient);
                        if (sent) {
                            Message confirmation = new Message(
                                "System",
                                "File sent to " + fileRecipient,
                                Message.MessageType.SYSTEM
                            );
                            sendMessage(confirmation);
                            server.log(username + " -> " + fileRecipient + " (file): " + message.getFileName());
                        } else {
                            Message error = new Message(
                                "System",
                                "User '" + fileRecipient + "' not found",
                                Message.MessageType.SYSTEM
                            );
                            sendMessage(error);
                        }
                    } else {
                        server.broadcast(message, username);
                        server.log(username + " sent a file: " + message.getFileName());
                    }
                    break;
                    
                case PRIVATE:
                    // Send private message
                    String recipient = message.getRecipient();
                    if (recipient != null) {
                        boolean sent = server.sendPrivateMessage(message, recipient);
                        
                        if (sent) {
                            // Send confirmation back to sender
                            Message confirmation = new Message(
                                "System",
                                "Private message sent to " + recipient,
                                Message.MessageType.SYSTEM
                            );
                            sendMessage(confirmation);
                            server.log(username + " -> " + recipient + " (private): " + message.getContent());
                        } else {
                            // User not found
                            Message error = new Message(
                                "System",
                                "User '" + recipient + "' not found",
                                Message.MessageType.SYSTEM
                            );
                            sendMessage(error);
                        }
                    }
                    break;
                    
                case LEAVE:
                    // Client is leaving
                    connected = false;
                    break;
                    
                default:
                    server.log("Unhandled message type from " + username + ": " + message.getType());
                    break;
            }
            
        } catch (Exception e) {
            server.log("Error processing message from " + username + ": " + e.getMessage());
        }
    }
    
    /**
     * Send a message to this client
     * 
     * @param message The message to send
     */
    public void sendMessage(Message message) {
        if (output != null && !socket.isClosed()) {
            output.println(message.toProtocolString());
        }
    }
    
    /**
     * Get the client's username
     * 
     * @return The username
     */
    public String getUsername() {
        return username;
    }
    
    /**
     * Close the connection to this client
     */
    public void closeConnection() {
        connected = false;
        
        try {
            // Unregister from server
            if (username != null) {
                server.unregisterClient(username);
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
            
        } catch (IOException e) {
            server.log("Error closing connection for " + username + ": " + e.getMessage());
        }
    }
}
