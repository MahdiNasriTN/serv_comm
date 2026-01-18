package server;

import common.Message;
import common.Protocol;
import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * ChatServer - Main server class for the chat application.
 * Handles incoming client connections, manages connected clients,
 * and coordinates message broadcasting.
 */
public class ChatServer {
    
    private ServerSocket serverSocket;
    private final int port;
    private boolean running;
    
    // Thread-safe map to store all connected clients
    // Key: username, Value: ClientHandler
    private final Map<String, ClientHandler> clients;
    
    /**
     * Constructor with default port
     */
    public ChatServer() {
        this(Protocol.DEFAULT_PORT);
    }
    
    /**
     * Constructor with custom port
     * 
     * @param port The port number to listen on
     */
    public ChatServer(int port) {
        this.port = port;
        this.clients = new ConcurrentHashMap<>();
        this.running = false;
    }
    
    /**
     * Start the chat server
     */
    public void start() {
        try {
            serverSocket = new ServerSocket(port);
            running = true;
            
            log("Chat Server started on port " + port);
            log("Waiting for client connections...");
            
            // Main server loop - accept incoming connections
            while (running) {
                try {
                    Socket clientSocket = serverSocket.accept();
                    
                    // Create a new client handler thread for this connection
                    ClientHandler clientHandler = new ClientHandler(clientSocket, this);
                    
                    // Start the client handler thread
                    Thread clientThread = new Thread(clientHandler);
                    clientThread.start();
                    
                } catch (IOException e) {
                    if (running) {
                        log("Error accepting client connection: " + e.getMessage());
                    }
                }
            }
            
        } catch (IOException e) {
            log("Could not start server on port " + port + ": " + e.getMessage());
        } finally {
            stop();
        }
    }
    
    /**
     * Stop the chat server
     */
    public void stop() {
        running = false;
        
        try {
            // Close all client connections
            for (ClientHandler client : clients.values()) {
                client.closeConnection();
            }
            clients.clear();
            
            // Close server socket
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
            
            log("Chat Server stopped.");
            
        } catch (IOException e) {
            log("Error stopping server: " + e.getMessage());
        }
    }
    
    /**
     * Register a new client with the server
     * 
     * @param username The client's username
     * @param clientHandler The client's handler
     * @return true if registration successful, false if username already exists
     */
    public synchronized boolean registerClient(String username, ClientHandler clientHandler) {
        if (clients.containsKey(username)) {
            return false;
        }
        
        clients.put(username, clientHandler);
        log("Client registered: " + username + " (Total clients: " + clients.size() + ")");
        
        // NOTE: Do NOT broadcast here - let the ClientHandler do it after sending SUCCESS
        // to avoid race condition where USER_LIST arrives before SUCCESS response
        
        return true;
    }
    
    /**
     * Notify all clients about a new user joining
     * Called by ClientHandler after authentication is complete
     * 
     * @param username The username of the user who joined
     */
    public void notifyUserJoined(String username) {
        // Send user list to all clients
        broadcastUserList();
        
        // Notify all clients about new user
        Message joinMessage = new Message("System", username + " has joined the chat", Message.MessageType.SYSTEM);
        broadcast(joinMessage, null);
    }
    
    /**
     * Unregister a client from the server
     * 
     * @param username The client's username
     */
    public synchronized void unregisterClient(String username) {
        if (clients.remove(username) != null) {
            log("Client disconnected: " + username + " (Total clients: " + clients.size() + ")");
            
            // Notify all clients about user leaving
            Message leaveMessage = new Message("System", username + " has left the chat", Message.MessageType.SYSTEM);
            broadcast(leaveMessage, null);
            
            // Send updated user list
            broadcastUserList();
        }
    }
    
    /**
     * Broadcast a message to all connected clients except the sender
     * 
     * @param message The message to broadcast
     * @param senderUsername The username of the sender (null to send to all)
     */
    public void broadcast(Message message, String senderUsername) {
        for (Map.Entry<String, ClientHandler> entry : clients.entrySet()) {
            // Don't send the message back to the sender (unless it's a system message)
            if (senderUsername == null || !entry.getKey().equals(senderUsername)) {
                entry.getValue().sendMessage(message);
            }
        }
    }
    
    /**
     * Send a private message to a specific client
     * 
     * @param message The message to send
     * @param recipientUsername The recipient's username
     * @return true if sent successfully, false if recipient not found
     */
    public boolean sendPrivateMessage(Message message, String recipientUsername) {
        ClientHandler recipient = clients.get(recipientUsername);
        
        if (recipient != null) {
            recipient.sendMessage(message);
            return true;
        }
        
        return false;
    }
    
    /**
     * Broadcast the list of connected users to all clients
     */
    private void broadcastUserList() {
        String userList = String.join(",", clients.keySet());
        Message userListMessage = new Message("System", userList, Message.MessageType.USER_LIST);
        
        for (ClientHandler client : clients.values()) {
            client.sendMessage(userListMessage);
        }
    }
    
    /**
     * Get list of connected usernames
     * 
     * @return List of usernames
     */
    public List<String> getConnectedUsers() {
        return new ArrayList<>(clients.keySet());
    }
    
    /**
     * Check if a username is already taken
     * 
     * @param username The username to check
     * @return true if username exists
     */
    public boolean isUsernameTaken(String username) {
        return clients.containsKey(username);
    }
    
    /**
     * Log a message with timestamp to the console
     * 
     * @param message The message to log
     */
    public void log(String message) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        System.out.println("[" + timestamp + "] " + message);
    }
    
    /**
     * Main method to start the server
     * 
     * @param args Command line arguments (optional: port number)
     */
    public static void main(String[] args) {
        int port = Protocol.DEFAULT_PORT;
        
        // Check if custom port is provided
        if (args.length > 0) {
            try {
                port = Integer.parseInt(args[0]);
            } catch (NumberFormatException e) {
                System.out.println("Invalid port number. Using default port: " + Protocol.DEFAULT_PORT);
            }
        }
        
        ChatServer server = new ChatServer(port);
        
        // Add shutdown hook to gracefully stop the server
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("\nShutting down server...");
            server.stop();
        }));
        
        server.start();
    }
}
