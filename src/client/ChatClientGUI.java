package client;

import common.Message;
import common.Protocol;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.text.*;
import java.awt.*;
import java.awt.event.*;
import java.util.List;

/**
 * ChatClientGUI - Graphical User Interface for the chat client.
 * Built using Java Swing, provides a user-friendly interface
 * for connecting to the server and chatting with other users.
 */
public class ChatClientGUI extends JFrame implements ChatClient.MessageListener {
    
    // UI Components
    private JTextField usernameField;
    private JTextField serverField;
    private JTextField portField;
    private JButton connectButton;
    private JButton disconnectButton;
    
    private JTextPane chatArea;
    private StyledDocument chatDocument;
    private JTextField messageField;
    private JButton sendButton;
    
    private JList<String> userList;
    private DefaultListModel<String> userListModel;
    
    private JLabel statusLabel;
    
    // Client instance
    private ChatClient client;
    
    // Colors for different message types
    private static final Color COLOR_SYSTEM = new Color(100, 100, 100);
    private static final Color COLOR_PRIVATE = new Color(0, 100, 200);
    private static final Color COLOR_SENT = new Color(0, 150, 0);
    private static final Color COLOR_RECEIVED = new Color(50, 50, 50);
    
    /**
     * Constructor - Initialize the GUI
     */
    public ChatClientGUI() {
        super("Chat Application");
        
        client = new ChatClient();
        client.setMessageListener(this);
        
        initializeUI();
        
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(900, 600);
        setLocationRelativeTo(null);
        
        // Add window closing listener
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                if (client.isConnected()) {
                    client.disconnect();
                }
            }
        });
    }
    
    /**
     * Initialize all UI components
     */
    private void initializeUI() {
        setLayout(new BorderLayout(10, 10));
        
        // Create main panels
        add(createConnectionPanel(), BorderLayout.NORTH);
        add(createChatPanel(), BorderLayout.CENTER);
        add(createInputPanel(), BorderLayout.SOUTH);
        add(createUserListPanel(), BorderLayout.EAST);
        
        // Initially disable chat components
        setChatComponentsEnabled(false);
    }
    
    /**
     * Create the connection panel (top)
     */
    private JPanel createConnectionPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
        panel.setBorder(new EmptyBorder(5, 5, 5, 5));
        panel.setBackground(new Color(240, 240, 240));
        
        // Username
        panel.add(new JLabel("Username:"));
        usernameField = new JTextField(15);
        panel.add(usernameField);
        
        // Server
        panel.add(new JLabel("Server:"));
        serverField = new JTextField(Protocol.DEFAULT_HOST, 12);
        panel.add(serverField);
        
        // Port
        panel.add(new JLabel("Port:"));
        portField = new JTextField(String.valueOf(Protocol.DEFAULT_PORT), 6);
        panel.add(portField);
        
        // Connect button
        connectButton = new JButton("Connect");
        connectButton.setBackground(new Color(76, 175, 80));
        connectButton.setForeground(Color.WHITE);
        connectButton.setFocusPainted(false);
        connectButton.addActionListener(e -> connectToServer());
        panel.add(connectButton);
        
        // Disconnect button
        disconnectButton = new JButton("Disconnect");
        disconnectButton.setBackground(new Color(244, 67, 54));
        disconnectButton.setForeground(Color.WHITE);
        disconnectButton.setFocusPainted(false);
        disconnectButton.setEnabled(false);
        disconnectButton.addActionListener(e -> disconnectFromServer());
        panel.add(disconnectButton);
        
        // Status label
        statusLabel = new JLabel("Not connected");
        statusLabel.setForeground(Color.RED);
        panel.add(statusLabel);
        
        return panel;
    }
    
    /**
     * Create the chat area panel (center)
     */
    private JPanel createChatPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createTitledBorder("Chat"));
        
        // Chat area
        chatArea = new JTextPane();
        chatArea.setEditable(false);
        chatArea.setFont(new Font("Arial", Font.PLAIN, 13));
        chatDocument = chatArea.getStyledDocument();
        
        JScrollPane scrollPane = new JScrollPane(chatArea);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        
        panel.add(scrollPane, BorderLayout.CENTER);
        
        return panel;
    }
    
    /**
     * Create the input panel (bottom)
     */
    private JPanel createInputPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(new EmptyBorder(5, 5, 5, 5));
        
        // Message field
        messageField = new JTextField();
        messageField.setFont(new Font("Arial", Font.PLAIN, 13));
        messageField.addActionListener(e -> sendMessage());
        
        // Send button
        sendButton = new JButton("Send");
        sendButton.setBackground(new Color(33, 150, 243));
        sendButton.setForeground(Color.WHITE);
        sendButton.setFocusPainted(false);
        sendButton.addActionListener(e -> sendMessage());
        
        panel.add(messageField, BorderLayout.CENTER);
        panel.add(sendButton, BorderLayout.EAST);
        
        // Add instruction label
        JLabel instructionLabel = new JLabel("Tip: Use /msg username message for private messages");
        instructionLabel.setFont(new Font("Arial", Font.ITALIC, 11));
        instructionLabel.setForeground(Color.GRAY);
        panel.add(instructionLabel, BorderLayout.NORTH);
        
        return panel;
    }
    
    /**
     * Create the user list panel (right)
     */
    private JPanel createUserListPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createTitledBorder("Online Users"));
        panel.setPreferredSize(new Dimension(150, 0));
        
        // User list
        userListModel = new DefaultListModel<>();
        userList = new JList<>(userListModel);
        userList.setFont(new Font("Arial", Font.PLAIN, 12));
        userList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        
        // Double-click to start private message
        userList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    String selectedUser = userList.getSelectedValue();
                    if (selectedUser != null && !selectedUser.equals(client.getUsername())) {
                        messageField.setText("/msg " + selectedUser + " ");
                        messageField.requestFocus();
                    }
                }
            }
        });
        
        JScrollPane scrollPane = new JScrollPane(userList);
        panel.add(scrollPane, BorderLayout.CENTER);
        
        return panel;
    }
    
    /**
     * Connect to the server
     */
    private void connectToServer() {
        String username = usernameField.getText().trim();
        String server = serverField.getText().trim();
        String portStr = portField.getText().trim();
        
        // Validate inputs
        if (username.isEmpty()) {
            showError("Please enter a username");
            return;
        }
        
        if (server.isEmpty()) {
            showError("Please enter a server address");
            return;
        }
        
        int port;
        try {
            port = Integer.parseInt(portStr);
            if (port < 1 || port > 65535) {
                showError("Port must be between 1 and 65535");
                return;
            }
        } catch (NumberFormatException e) {
            showError("Invalid port number");
            return;
        }
        
        // Disable connect button
        connectButton.setEnabled(false);
        statusLabel.setText("Connecting...");
        statusLabel.setForeground(Color.ORANGE);
        
        // Connect in a separate thread
        new Thread(() -> {
            System.out.println("[ChatClientGUI] Attempting to connect to server: " + server + ":" + port + " as " + username);
            boolean success = client.connect(server, port, username);
            
            SwingUtilities.invokeLater(() -> {
                if (!success) {
                    statusLabel.setText("Connection failed");
                    statusLabel.setForeground(Color.RED);
                    connectButton.setEnabled(true);
                }
            });
        }).start();
    }
    
    /**
     * Disconnect from the server
     */
    private void disconnectFromServer() {
        client.disconnect();
    }
    
    /**
     * Send a message
     */
    private void sendMessage() {
        String message = messageField.getText().trim();
        
        if (message.isEmpty() || !client.isConnected()) {
            return;
        }
        
        // Display sent message in chat (if not a private message command)
        if (!Protocol.isPrivateMessage(message)) {
            Message sentMessage = new Message(client.getUsername(), message, Message.MessageType.NORMAL);
            displayMessage(sentMessage, true);
        }
        
        // Send to server
        client.sendMessage(message);
        
        // Clear input field
        messageField.setText("");
        messageField.requestFocus();
    }
    
    /**
     * Display a message in the chat area
     * 
     * @param message The message to display
     * @param isSent Whether this message was sent by the user
     */
    private void displayMessage(Message message, boolean isSent) {
        SwingUtilities.invokeLater(() -> {
            try {
                // Create styled attributes
                SimpleAttributeSet attrs = new SimpleAttributeSet();
                
                // Set color based on message type
                if (message.getType() == Message.MessageType.SYSTEM) {
                    StyleConstants.setForeground(attrs, COLOR_SYSTEM);
                    StyleConstants.setItalic(attrs, true);
                } else if (message.getType() == Message.MessageType.PRIVATE) {
                    StyleConstants.setForeground(attrs, COLOR_PRIVATE);
                    StyleConstants.setBold(attrs, true);
                } else if (isSent) {
                    StyleConstants.setForeground(attrs, COLOR_SENT);
                } else {
                    StyleConstants.setForeground(attrs, COLOR_RECEIVED);
                }
                
                // Format the message
                String displayText;
                if (message.getType() == Message.MessageType.SYSTEM) {
                    displayText = String.format("[%s] %s\n", 
                        message.getFormattedTimestamp(), 
                        message.getContent());
                } else if (message.getType() == Message.MessageType.PRIVATE) {
                    displayText = String.format("[%s] [PRIVATE from %s] %s\n", 
                        message.getFormattedTimestamp(), 
                        message.getSender(), 
                        message.getContent());
                } else {
                    displayText = String.format("[%s] %s: %s\n", 
                        message.getFormattedTimestamp(), 
                        message.getSender(), 
                        message.getContent());
                }
                
                // Insert the text
                chatDocument.insertString(chatDocument.getLength(), displayText, attrs);
                
                // Auto-scroll to bottom
                chatArea.setCaretPosition(chatDocument.getLength());
                
            } catch (BadLocationException e) {
                e.printStackTrace();
            }
        });
    }
    
    /**
     * Enable or disable chat components
     * 
     * @param enabled Whether to enable the components
     */
    private void setChatComponentsEnabled(boolean enabled) {
        messageField.setEnabled(enabled);
        sendButton.setEnabled(enabled);
        chatArea.setEnabled(enabled);
        userList.setEnabled(enabled);
    }
    
    /**
     * Show an error dialog
     * 
     * @param message The error message
     */
    private void showError(String message) {
        JOptionPane.showMessageDialog(this, message, "Error", JOptionPane.ERROR_MESSAGE);
    }
    
    // MessageListener implementation
    
    @Override
    public void onMessageReceived(Message message) {
        displayMessage(message, false);
    }
    
    @Override
    public void onConnectionStatusChanged(boolean connected, String statusMessage) {
        System.out.println("[ChatClientGUI] onConnectionStatusChanged called: connected=" + connected + ", message=" + statusMessage);
        SwingUtilities.invokeLater(() -> {
            System.out.println("[ChatClientGUI] Updating GUI: connected=" + connected);
            if (connected) {
                // Connected
                statusLabel.setText("Connected");
                statusLabel.setForeground(new Color(76, 175, 80));
                
                connectButton.setEnabled(false);
                disconnectButton.setEnabled(true);
                
                usernameField.setEnabled(false);
                serverField.setEnabled(false);
                portField.setEnabled(false);
                
                setChatComponentsEnabled(true);
                messageField.requestFocus();
                
                // Clear chat area
                chatArea.setText("");
                
                // Welcome message
                Message welcomeMsg = new Message("System", statusMessage, Message.MessageType.SYSTEM);
                displayMessage(welcomeMsg, false);
                
            } else {
                // Disconnected
                statusLabel.setText("Not connected");
                statusLabel.setForeground(Color.RED);
                
                connectButton.setEnabled(true);
                disconnectButton.setEnabled(false);
                
                usernameField.setEnabled(true);
                serverField.setEnabled(true);
                portField.setEnabled(true);
                
                setChatComponentsEnabled(false);
                
                // Clear user list
                userListModel.clear();
                
                // Show status message
                if (!statusMessage.isEmpty()) {
                    Message statusMsg = new Message("System", statusMessage, Message.MessageType.SYSTEM);
                    displayMessage(statusMsg, false);
                }
            }
        });
    }
    
    @Override
    public void onUserListUpdated(List<String> users) {
        SwingUtilities.invokeLater(() -> {
            userListModel.clear();
            for (String user : users) {
                userListModel.addElement(user);
            }
        });
    }
    
    /**
     * Main method to start the client GUI
     * 
     * @param args Command line arguments (not used)
     */
    public static void main(String[] args) {
        // Set look and feel
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            // Use default look and feel
        }
        
        // Create and show GUI
        SwingUtilities.invokeLater(() -> {
            ChatClientGUI gui = new ChatClientGUI();
            gui.setVisible(true);
        });
    }
}
