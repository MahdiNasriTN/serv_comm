package client;

import common.Message;
import common.Protocol;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.text.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.Files;
import java.util.Base64;
import java.util.List;
import javax.imageio.ImageIO;
import javax.sound.sampled.*;

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
    private JButton imageButton;
    private JButton fileButton;
    private JButton allButton;
    private JButton voiceButton;
    
    private JList<String> userList;
    private DefaultListModel<String> userListModel;
    
    private JLabel statusLabel;
    private String privateMessageRecipient; // Current private message recipient
    
    // Voice recording
    private TargetDataLine microphone;
    private ByteArrayOutputStream recordedAudio;
    private boolean isRecording = false;
    private long recordingStartTime;
    
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
        connectButton.setOpaque(true);
        connectButton.setBorderPainted(false);
        connectButton.addActionListener(e -> connectToServer());
        panel.add(connectButton);
        
        // Disconnect button
        disconnectButton = new JButton("Disconnect");
        disconnectButton.setBackground(new Color(244, 67, 54));
        disconnectButton.setForeground(Color.WHITE);
        disconnectButton.setFocusPainted(false);
        disconnectButton.setOpaque(true);
        disconnectButton.setBorderPainted(false);
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
        
        // Add mouse listener for clickable links (files and voice messages)
        chatArea.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                int pos = chatArea.viewToModel2D(e.getPoint());
                Element element = chatDocument.getCharacterElement(pos);
                AttributeSet as = element.getAttributes();
                
                // Handle file download links
                if (as.getAttribute("fileData") != null) {
                    String fileData = (String) as.getAttribute("fileData");
                    String fileName = (String) as.getAttribute("fileName");
                    downloadFile(fileData, fileName);
                }
                
                // Handle voice playback links
                if (as.getAttribute("voiceData") != null) {
                    String voiceData = (String) as.getAttribute("voiceData");
                    playVoiceMessage(voiceData);
                }
            }
        });
        
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
        
        // Buttons panel
        JPanel buttonsPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 0));
        
        // All button (exit private chat)
        allButton = new JButton("All");
        allButton.setBackground(new Color(96, 125, 139));
        allButton.setForeground(Color.WHITE);
        allButton.setFocusPainted(false);
        allButton.setOpaque(true);
        allButton.setBorderPainted(false);
        allButton.addActionListener(e -> exitPrivateChat());
        allButton.setVisible(false); // Hidden by default
        buttonsPanel.add(allButton);
        
        // Image button
        imageButton = new JButton("📷 Image");
        imageButton.setBackground(new Color(156, 39, 176));
        imageButton.setForeground(Color.WHITE);
        imageButton.setFocusPainted(false);
        imageButton.setOpaque(true);
        imageButton.setBorderPainted(false);
        imageButton.addActionListener(e -> selectAndSendImage());
        buttonsPanel.add(imageButton);
        
        // File button
        fileButton = new JButton("📎 File");
        fileButton.setBackground(new Color(255, 152, 0));
        fileButton.setForeground(Color.WHITE);
        fileButton.setFocusPainted(false);
        fileButton.setOpaque(true);
        fileButton.setBorderPainted(false);
        fileButton.addActionListener(e -> selectAndSendFile());
        buttonsPanel.add(fileButton);
        
        // Voice button (press and hold to record)
        voiceButton = new JButton("🎤 Voice");
        voiceButton.setBackground(new Color(76, 175, 80));
        voiceButton.setForeground(Color.WHITE);
        voiceButton.setFocusPainted(false);
        voiceButton.setOpaque(true);
        voiceButton.setBorderPainted(false);
        
        // Add mouse listeners for press-and-hold
        voiceButton.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (client.isConnected()) {
                    startRecording();
                }
            }
            
            @Override
            public void mouseReleased(MouseEvent e) {
                if (isRecording) {
                    stopRecording();
                }
            }
        });
        buttonsPanel.add(voiceButton);
        
        // Send button
        sendButton = new JButton("Send");
        sendButton.setBackground(new Color(33, 150, 243));
        sendButton.setForeground(Color.WHITE);
        sendButton.setFocusPainted(false);
        sendButton.setOpaque(true);
        sendButton.setBorderPainted(false);
        sendButton.addActionListener(e -> sendMessage());
        buttonsPanel.add(sendButton);
        
        panel.add(messageField, BorderLayout.CENTER);
        panel.add(buttonsPanel, BorderLayout.EAST);
        
        // Add instruction label
        JLabel instructionLabel = new JLabel("Tip: Double-click a user to chat privately");
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
                        startPrivateChat(selectedUser);
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
        
        // Check if in private chat mode
        if (privateMessageRecipient != null) {
            // Send as private message
            client.sendPrivateMessage(privateMessageRecipient, message);
            
            // Display sent private message
            Message sentMessage = new Message(client.getUsername(), message, Message.MessageType.PRIVATE);
            sentMessage.setRecipient(privateMessageRecipient);
            displayMessage(sentMessage, true);
        } else {
            // Display sent message in chat (if not a private message command)
            if (!Protocol.isPrivateMessage(message)) {
                Message sentMessage = new Message(client.getUsername(), message, Message.MessageType.NORMAL);
                displayMessage(sentMessage, true);
            }
            
            // Send to server
            client.sendMessage(message);
        }
        
        // Clear input field
        messageField.setText("");
        messageField.requestFocus();
    }
    
    /**
     * Start private chat mode with a specific user
     */
    private void startPrivateChat(String username) {
        privateMessageRecipient = username;
        messageField.setBorder(BorderFactory.createLineBorder(COLOR_PRIVATE, 2));
        
        // Show notification
        Message systemMsg = new Message("System", 
            "Private chat mode with " + username + ". Click 'All' button to exit.",
            Message.MessageType.SYSTEM);
        displayMessage(systemMsg, false);
        
        // Update send button
        sendButton.setText("Send to " + username);
        sendButton.setBackground(COLOR_PRIVATE);
        
        // Show All button
        allButton.setVisible(true);
        
        messageField.requestFocus();
    }
    
    /**
     * Exit private chat mode
     */
    private void exitPrivateChat() {
        privateMessageRecipient = null;
        messageField.setBorder(UIManager.getBorder("TextField.border"));
        sendButton.setText("Send");
        sendButton.setBackground(new Color(33, 150, 243));
        
        // Hide All button
        allButton.setVisible(false);
        
        // Show notification
        Message systemMsg = new Message("System", 
            "Back to all chat mode",
            Message.MessageType.SYSTEM);
        displayMessage(systemMsg, false);
    }
    
    /**
     * Select and send an image file
     */
    private void selectAndSendImage() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter(
            "Image Files", "jpg", "jpeg", "png", "gif", "bmp"));
        
        int result = fileChooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            File file = fileChooser.getSelectedFile();
            
            // Check file size (limit to 5MB)
            if (file.length() > 5 * 1024 * 1024) {
                showError("Image file is too large. Maximum size is 5MB.");
                return;
            }
            
            try {
                byte[] fileBytes = Files.readAllBytes(file.toPath());
                String base64Data = Base64.getEncoder().encodeToString(fileBytes);
                
                // Send image
                client.sendImage(base64Data, file.getName(), privateMessageRecipient);
                
                // Display sent image
                Message imageMessage = new Message(client.getUsername(), base64Data, Message.MessageType.IMAGE);
                imageMessage.setFileName(file.getName());
                displayMessage(imageMessage, true);
                
            } catch (IOException e) {
                showError("Error reading image file: " + e.getMessage());
            }
        }
    }
    
    /**
     * Select and send a file
     */
    private void selectAndSendFile() {
        JFileChooser fileChooser = new JFileChooser();
        
        int result = fileChooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            File file = fileChooser.getSelectedFile();
            
            // Check file size (limit to 10MB)
            if (file.length() > 10 * 1024 * 1024) {
                showError("File is too large. Maximum size is 10MB.");
                return;
            }
            
            try {
                byte[] fileBytes = Files.readAllBytes(file.toPath());
                String base64Data = Base64.getEncoder().encodeToString(fileBytes);
                
                // Send file
                client.sendFile(base64Data, file.getName(), privateMessageRecipient);
                
                // Display sent file
                Message fileMessage = new Message(client.getUsername(), base64Data, Message.MessageType.FILE);
                fileMessage.setFileName(file.getName());
                displayMessage(fileMessage, true);
                
            } catch (IOException e) {
                showError("Error reading file: " + e.getMessage());
            }
        }
    }
    
    /**
     * Start recording voice message
     */
    private void startRecording() {
        try {
            // Audio format: 16kHz, 16-bit, mono
            AudioFormat format = new AudioFormat(16000, 16, 1, true, true);
            DataLine.Info info = new DataLine.Info(TargetDataLine.class, format);
            
            if (!AudioSystem.isLineSupported(info)) {
                showError("Microphone not supported");
                return;
            }
            
            microphone = (TargetDataLine) AudioSystem.getLine(info);
            microphone.open(format);
            microphone.start();
            
            recordedAudio = new ByteArrayOutputStream();
            isRecording = true;
            recordingStartTime = System.currentTimeMillis();
            
            // Change button appearance
            voiceButton.setBackground(Color.RED);
            voiceButton.setText("🔴 Recording...");
            
            // Start recording thread
            new Thread(() -> {
                byte[] buffer = new byte[1024];
                while (isRecording) {
                    int bytesRead = microphone.read(buffer, 0, buffer.length);
                    if (bytesRead > 0) {
                        recordedAudio.write(buffer, 0, bytesRead);
                    }
                }
            }).start();
            
        } catch (LineUnavailableException e) {
            showError("Cannot access microphone: " + e.getMessage());
            isRecording = false;
        }
    }
    
    /**
     * Stop recording and send voice message
     */
    private void stopRecording() {
        if (!isRecording) return;
        
        isRecording = false;
        
        // Calculate duration
        long duration = (System.currentTimeMillis() - recordingStartTime) / 1000;
        
        // Reset button
        voiceButton.setBackground(new Color(76, 175, 80));
        voiceButton.setText("🎤 Voice");
        
        // Stop microphone
        if (microphone != null) {
            microphone.stop();
            microphone.close();
        }
        
        // Check minimum duration (at least 1 second)
        if (duration < 1) {
            showError("Voice message too short (minimum 1 second)");
            return;
        }
        
        // Check maximum duration (60 seconds)
        if (duration > 60) {
            showError("Voice message too long (maximum 60 seconds)");
            return;
        }
        
        try {
            // Convert to base64
            byte[] audioBytes = recordedAudio.toByteArray();
            String base64Audio = Base64.getEncoder().encodeToString(audioBytes);
            
            // Send voice message
            client.sendVoice(base64Audio, (int) duration, privateMessageRecipient);
            
            // Display sent voice message
            Message voiceMessage = new Message(client.getUsername(), base64Audio, Message.MessageType.VOICE);
            voiceMessage.setFileName(duration + "s");
            if (privateMessageRecipient != null) {
                voiceMessage.setRecipient(privateMessageRecipient);
            }
            displayMessage(voiceMessage, true);
            
        } catch (Exception e) {
            showError("Error sending voice message: " + e.getMessage());
        }
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
                // Handle different message types
                if (message.getType() == Message.MessageType.IMAGE) {
                    displayImageMessage(message, isSent);
                } else if (message.getType() == Message.MessageType.FILE) {
                    displayFileMessage(message, isSent);
                } else if (message.getType() == Message.MessageType.VOICE) {
                    displayVoiceMessage(message, isSent);
                } else {
                    displayTextMessage(message, isSent);
                }
                
                // Auto-scroll to bottom
                chatArea.setCaretPosition(chatDocument.getLength());
                
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }
    
    /**
     * Display a text message
     */
    private void displayTextMessage(Message message, boolean isSent) throws BadLocationException {
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
            String direction = isSent ? "to" : "from";
            String otherUser = isSent ? message.getRecipient() : message.getSender();
            displayText = String.format("[%s] [PRIVATE %s %s] %s\n", 
                message.getFormattedTimestamp(), 
                direction,
                otherUser,
                message.getContent());
        } else {
            displayText = String.format("[%s] %s: %s\n", 
                message.getFormattedTimestamp(), 
                message.getSender(), 
                message.getContent());
        }
        
        // Insert the text
        chatDocument.insertString(chatDocument.getLength(), displayText, attrs);
    }
    
    /**
     * Display an image message
     */
    private void displayImageMessage(Message message, boolean isSent) throws BadLocationException {
        // Display header
        SimpleAttributeSet attrs = new SimpleAttributeSet();
        
        // Check if it's a private image
        boolean isPrivate = message.getRecipient() != null && !message.getRecipient().isEmpty();
        
        if (isPrivate) {
            StyleConstants.setForeground(attrs, COLOR_PRIVATE);
            StyleConstants.setBold(attrs, true);
        } else {
            StyleConstants.setForeground(attrs, isSent ? COLOR_SENT : COLOR_RECEIVED);
            StyleConstants.setBold(attrs, true);
        }
        
        String header;
        if (isPrivate) {
            String direction = isSent ? "to" : "from";
            String otherUser = isSent ? message.getRecipient() : message.getSender();
            header = String.format("[%s] [PRIVATE %s %s] Image: %s\n",
                message.getFormattedTimestamp(),
                direction,
                otherUser,
                message.getFileName());
        } else {
            header = String.format("[%s] %s sent an image: %s\n",
                message.getFormattedTimestamp(),
                message.getSender(),
                message.getFileName());
        }
        chatDocument.insertString(chatDocument.getLength(), header, attrs);
        
        // Display image
        try {
            byte[] imageBytes = Base64.getDecoder().decode(message.getContent());
            BufferedImage img = ImageIO.read(new ByteArrayInputStream(imageBytes));
            
            if (img != null) {
                // Scale image if too large
                int maxWidth = 300;
                int maxHeight = 300;
                if (img.getWidth() > maxWidth || img.getHeight() > maxHeight) {
                    double scale = Math.min((double)maxWidth / img.getWidth(), (double)maxHeight / img.getHeight());
                    int newWidth = (int)(img.getWidth() * scale);
                    int newHeight = (int)(img.getHeight() * scale);
                    
                    Image scaledImg = img.getScaledInstance(newWidth, newHeight, Image.SCALE_SMOOTH);
                    BufferedImage bufferedScaled = new BufferedImage(newWidth, newHeight, BufferedImage.TYPE_INT_ARGB);
                    Graphics2D g2d = bufferedScaled.createGraphics();
                    g2d.drawImage(scaledImg, 0, 0, null);
                    g2d.dispose();
                    img = bufferedScaled;
                }
                
                // Insert image
                chatArea.setCaretPosition(chatDocument.getLength());
                chatArea.insertIcon(new ImageIcon(img));
                chatDocument.insertString(chatDocument.getLength(), "\n", attrs);
            }
        } catch (Exception e) {
            chatDocument.insertString(chatDocument.getLength(), "  [Error displaying image]\n", attrs);
        }
    }
    
    /**
     * Display a file message
     */
    private void displayFileMessage(Message message, boolean isSent) throws BadLocationException {
        // Display file info
        SimpleAttributeSet attrs = new SimpleAttributeSet();
        
        // Check if it's a private file
        boolean isPrivate = message.getRecipient() != null && !message.getRecipient().isEmpty();
        
        if (isPrivate) {
            StyleConstants.setForeground(attrs, COLOR_PRIVATE);
            StyleConstants.setBold(attrs, true);
        } else {
            StyleConstants.setForeground(attrs, isSent ? COLOR_SENT : COLOR_RECEIVED);
            StyleConstants.setBold(attrs, true);
        }
        
        String fileInfo;
        if (isPrivate) {
            String direction = isSent ? "to" : "from";
            String otherUser = isSent ? message.getRecipient() : message.getSender();
            fileInfo = String.format("[%s] [PRIVATE %s %s] File: %s ",
                message.getFormattedTimestamp(),
                direction,
                otherUser,
                message.getFileName());
        } else {
            fileInfo = String.format("[%s] %s sent a file: %s ",
                message.getFormattedTimestamp(),
                message.getSender(),
                message.getFileName());
        }
        chatDocument.insertString(chatDocument.getLength(), fileInfo, attrs);
        
        // Add clickable "Download" link
        SimpleAttributeSet linkAttrs = new SimpleAttributeSet();
        StyleConstants.setForeground(linkAttrs, Color.BLUE);
        StyleConstants.setUnderline(linkAttrs, true);
        linkAttrs.addAttribute("fileData", message.getContent());
        linkAttrs.addAttribute("fileName", message.getFileName());
        
        chatDocument.insertString(chatDocument.getLength(), "[Download]", linkAttrs);
        chatDocument.insertString(chatDocument.getLength(), "\n", attrs);
    }
    
    /**
     * Download a file from base64 data
     */
    private void downloadFile(String base64Data, String fileName) {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setSelectedFile(new File(fileName));
        
        int result = fileChooser.showSaveDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            try {
                byte[] fileBytes = Base64.getDecoder().decode(base64Data);
                Files.write(fileChooser.getSelectedFile().toPath(), fileBytes);
                
                JOptionPane.showMessageDialog(this, 
                    "File saved successfully!", 
                    "Success", 
                    JOptionPane.INFORMATION_MESSAGE);
            } catch (IOException e) {
                showError("Error saving file: " + e.getMessage());
            }
        }
    }
    
    /**
     * Display a voice message
     */
    private void displayVoiceMessage(Message message, boolean isSent) throws BadLocationException {
        // Display voice info
        SimpleAttributeSet attrs = new SimpleAttributeSet();
        
        // Check if it's a private voice message
        boolean isPrivate = message.getRecipient() != null && !message.getRecipient().isEmpty();
        
        if (isPrivate) {
            StyleConstants.setForeground(attrs, COLOR_PRIVATE);
            StyleConstants.setBold(attrs, true);
        } else {
            StyleConstants.setForeground(attrs, isSent ? COLOR_SENT : COLOR_RECEIVED);
            StyleConstants.setBold(attrs, true);
        }
        
        String voiceInfo;
        if (isPrivate) {
            String direction = isSent ? "to" : "from";
            String otherUser = isSent ? message.getRecipient() : message.getSender();
            voiceInfo = String.format("[%s] [PRIVATE %s %s] 🎤 Voice %s ",
                message.getFormattedTimestamp(),
                direction,
                otherUser,
                message.getFileName());
        } else {
            voiceInfo = String.format("[%s] %s 🎤 Voice %s ",
                message.getFormattedTimestamp(),
                message.getSender(),
                message.getFileName());
        }
        chatDocument.insertString(chatDocument.getLength(), voiceInfo, attrs);
        
        // Create and add "Listen" button
        JButton listenButton = new JButton("Listen");
        listenButton.setBackground(new Color(76, 175, 80));
        listenButton.setForeground(Color.WHITE);
        listenButton.setFocusPainted(false);
        listenButton.setOpaque(true);
        listenButton.setBorderPainted(false);
        listenButton.setFont(new Font("Arial", Font.BOLD, 11));
        listenButton.setMargin(new Insets(2, 8, 2, 8));
        
        // Store voice data and add click listener
        final String voiceData = message.getContent();
        listenButton.addActionListener(e -> {
            System.out.println("[ChatClientGUI] Listen button clicked!");
            playVoiceMessage(voiceData);
        });
        
        // Insert button into chat area
        chatArea.setCaretPosition(chatDocument.getLength());
        chatArea.insertComponent(listenButton);
        
        chatDocument.insertString(chatDocument.getLength(), "\n", attrs);
    }
    
    /**
     * Play a voice message from base64 data
     */
    private void playVoiceMessage(String base64Audio) {
        new Thread(() -> {
            try {
                System.out.println("[ChatClientGUI] Starting voice playback...");
                byte[] audioBytes = Base64.getDecoder().decode(base64Audio);
                System.out.println("[ChatClientGUI] Decoded audio: " + audioBytes.length + " bytes");
                
                // Audio format must match recording format
                AudioFormat format = new AudioFormat(16000, 16, 1, true, true);
                DataLine.Info info = new DataLine.Info(SourceDataLine.class, format);
                
                if (!AudioSystem.isLineSupported(info)) {
                    System.err.println("[ChatClientGUI] Audio line not supported!");
                    SwingUtilities.invokeLater(() -> 
                        showError("Audio playback not supported on this system"));
                    return;
                }
                
                System.out.println("[ChatClientGUI] Getting audio line...");
                SourceDataLine speaker = (SourceDataLine) AudioSystem.getLine(info);
                System.out.println("[ChatClientGUI] Opening speaker...");
                speaker.open(format);
                System.out.println("[ChatClientGUI] Starting playback...");
                speaker.start();
                
                // Play audio in chunks for better responsiveness
                int chunkSize = 4096;
                int offset = 0;
                while (offset < audioBytes.length) {
                    int bytesToWrite = Math.min(chunkSize, audioBytes.length - offset);
                    speaker.write(audioBytes, offset, bytesToWrite);
                    offset += bytesToWrite;
                }
                
                System.out.println("[ChatClientGUI] Draining speaker...");
                speaker.drain();
                speaker.close();
                System.out.println("[ChatClientGUI] Voice playback completed!");
                
            } catch (Exception e) {
                e.printStackTrace();
                SwingUtilities.invokeLater(() -> 
                    showError("Error playing voice message: " + e.getMessage()));
            }
        }).start();
    }
    
    /**
     * Enable or disable chat components
     * 
     * @param enabled Whether to enable the components
     */
    private void setChatComponentsEnabled(boolean enabled) {
        messageField.setEnabled(enabled);
        sendButton.setEnabled(enabled);
        imageButton.setEnabled(enabled);
        fileButton.setEnabled(enabled);
        voiceButton.setEnabled(enabled);
        allButton.setEnabled(enabled);
        chatArea.setEnabled(enabled);
        userList.setEnabled(enabled);
        
        // Hide All button when disabled
        if (!enabled) {
            allButton.setVisible(false);
            exitPrivateChat();
        }
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
                
                // Update window title with username
                setTitle("Chat Application - " + client.getUsername());
                
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
                
                // Reset window title
                setTitle("Chat Application");
                
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
