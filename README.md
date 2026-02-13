# Java TCP Chat Application

A fully-featured client-server chat application built with Java using TCP sockets and Swing GUI. Supports real-time messaging, private chats, multimedia sharing (images, files, voice messages), and multi-user communication.

## Table of Contents
- [Features](#features)
- [Architecture](#architecture)
- [Requirements](#requirements)
- [Installation & Compilation](#installation--compilation)
- [Running the Application](#running-the-application)
- [Usage Guide](#usage-guide)
- [Protocol Documentation](#protocol-documentation)
- [Project Structure](#project-structure)
- [Technical Implementation](#technical-implementation)

---

## Features

### Core Features
- **Real-time messaging** - Instant message delivery to all connected clients
- **Multi-user support** - Multiple clients can connect simultaneously
- **User authentication** - Username validation and duplicate prevention
- **Online user list** - Real-time display of all connected users
- **Connection management** - Clean connect/disconnect handling
- **Server logging** - Timestamped console logs for all events

### Advanced Features
- **Private messaging** - Send messages to specific users via `/msg username message` command
- **Private chat mode** - Double-click a user to enter dedicated private chat with visual indicators
- **Image sharing** - Send and receive images with inline display (max 5MB)
- **File sharing** - Share files with download links (max 10MB)
- **Voice messages** - Record and send voice messages (1-60 seconds, press-and-hold recording)
- **Message timestamps** - All messages include formatted timestamps
- **Color-coded messages** - Different colors for sent, received, private, and system messages

---

## Architecture

### Server Side

The server follows a **multi-threaded architecture** where each client connection is handled by a dedicated thread.

#### Components:
1. **ChatServer** (Main Server Class)
   - Listens for incoming connections on port 8888
   - Manages all connected clients using a thread-safe `ConcurrentHashMap`
   - Handles client registration and removal
   - Broadcasts messages to all clients or routes private messages
   - Maintains server-side logging

2. **ClientHandler** (Per-Client Thread)
   - Runs in a separate thread for each connected client
   - Handles user authentication
   - Processes incoming messages from the client
   - Routes messages based on type (broadcast, private, image, file, voice)
   - Manages graceful disconnection

#### Server Workflow:
1. Server starts and binds to port 8888
2. Listens for client connections in an infinite loop
3. When a client connects:
   - Creates a new Socket for the connection
   - Spawns a new ClientHandler thread
   - ClientHandler authenticates the user
   - Registers the client in the active clients map
   - Notifies all users about the new connection
4. Processes messages from the client:
   - NORMAL messages → Broadcast to all clients
   - PRIVATE messages → Route to specific recipient
   - IMAGE/FILE/VOICE messages → Route to all or specific recipient
5. On disconnect:
   - Removes client from active clients map
   - Notifies all remaining users
   - Closes all resources

### Client Side

The client uses a **separation of concerns** pattern with networking logic separated from the GUI.

#### Components:
1. **ChatClient** (Networking Layer)
   - Manages TCP socket connection to the server
   - Handles authentication process
   - Sends messages to the server
   - Receives messages in a separate thread
   - Implements MessageListener interface for GUI callbacks

2. **ChatClientGUI** (Presentation Layer)
   - Swing-based graphical user interface
   - Displays chat messages with formatting
   - Manages user input and button actions
   - Shows online users list
   - Handles multimedia display (images, files, voice playback)

#### Client Workflow:
1. User enters username, server address, and port
2. Clicks Connect button
3. ChatClient establishes TCP connection
4. Authenticates with server:
   - Receives ENTER_USERNAME prompt
   - Sends username
   - Waits for SUCCESS or ERROR response
5. On successful authentication:
   - Enables chat interface
   - Starts receive thread to listen for incoming messages
   - Updates title bar with username
6. User can:
   - Send text messages
   - Send private messages (command or UI)
   - Share images, files, or voice recordings
   - View online users
7. Receive thread continuously listens for:
   - NORMAL messages (broadcasts)
   - PRIVATE messages (direct messages)
   - IMAGE/FILE/VOICE messages
   - USER_LIST updates
   - JOIN/LEAVE notifications
   - SYSTEM messages

---

## Requirements

- **Java Development Kit (JDK)** 8 or higher
- **Operating System:** Windows, macOS, or Linux
- **Network:** Server and clients must be able to communicate via TCP (same network or accessible IP)
- **Audio:** Microphone required for voice message recording

---

## Installation & Compilation

### 1. Directory Structure
Ensure your project has the following structure:
```
tp_commun/
├── src/
│   ├── common/
│   │   ├── Message.java
│   │   └── Protocol.java
│   ├── server/
│   │   ├── ChatServer.java
│   │   └── ClientHandler.java
│   └── client/
│       ├── ChatClient.java
│       └── ChatClientGUI.java
├── bin/
└── README.md
```

### 2. Compile the Project
Open a terminal in the project root directory and run:

```bash
javac -d bin src/common/*.java src/server/*.java src/client/*.java
```

This compiles all source files and places the `.class` files in the `bin` directory.

---

## Running the Application

### Starting the Server

1. Open a terminal in the project root directory
2. Run the server:
   ```bash
   java -cp bin server.ChatServer
   ```
3. You should see:
   ```
   Chat Server started on port 8888
   Waiting for clients to connect...
   ```

**Note:** The server must be running before clients can connect.

### Starting the Client

1. Open a **new terminal** (keep the server running)
2. Run the client:
   ```bash
   java -cp bin client.ChatClientGUI
   ```
3. The GUI window will appear

### Connecting Multiple Clients

To test multi-user functionality:
- Open additional terminals and run the client command again
- Each client will open in a separate window
- You can connect with different usernames

### Connecting Over Local Network (LAN)

The server automatically accepts connections from any computer on your local network.

#### On the Server Computer:

1. Start the server as usual:
   ```bash
   java -cp bin server.ChatServer
   ```

2. The server will display all available IP addresses:
   ```
   Chat Server started on port 8888
   Server is accepting connections on the following addresses:
     - localhost (127.0.0.1:8888) - For local connections
     - 192.168.1.100:8888 - For LAN connections (Wi-Fi)
     - 10.0.0.50:8888 - For LAN connections (Ethernet)
   Waiting for client connections...
   ```

3. **Note the LAN IP address** (e.g., `192.168.1.100`) - this is what other computers will use to connect

#### On Client Computers:

1. Ensure both computers are on the same network (connected to the same Wi-Fi or router)

2. Start the client application:
   ```bash
   java -cp bin client.ChatClientGUI
   ```

3. In the connection dialog:
   - **Username:** Enter your desired username
   - **Server:** Enter the server's LAN IP address (e.g., `192.168.1.100`)
   - **Port:** `8888` (default)
   - Click **Connect**

#### Troubleshooting LAN Connections:

**If clients cannot connect:**

1. **Firewall Settings:**
   - **Windows:** Allow Java through Windows Defender Firewall
     - Go to: Control Panel → System and Security → Windows Defender Firewall → Allow an app
     - Add Java or allow port 8888
   
   - **Linux:** Allow port 8888
     ```bash
     sudo ufw allow 8888/tcp
     ```
   
   - **macOS:** Allow Java in System Preferences → Security & Privacy → Firewall

2. **Network Connectivity:**
   - Verify both computers are on the same network
   - Test connectivity with ping:
     ```bash
     ping 192.168.1.100
     ```

3. **Server IP Address:**
   - Make sure you're using the LAN IP (192.168.x.x or 10.0.x.x)
   - NOT localhost or 127.0.0.1 (these only work on the same computer)

4. **Port Conflicts:**
   - Ensure no other application is using port 8888
   - You can use a custom port:
     ```bash
     java -cp bin server.ChatServer 9999
     ```
     Then connect clients to port 9999

---

## Usage Guide

### Connecting to the Server

1. **Enter Username:** Type a unique username (3-20 characters, alphanumeric and underscores only)
2. **Server Address:** Default is `localhost` (use IP address for remote connections)
3. **Port:** Default is `8888`
4. **Click Connect:** If successful, the chat area becomes enabled

### Sending Messages

#### Regular Messages (Broadcast)
- Type your message in the text field at the bottom
- Press **Enter** or click **Send**
- Message is sent to all connected users

#### Private Messages
**Method 1: Command**
- Type: `/msg username your message here`
- Example: `/msg alice Hello Alice!`

**Method 2: UI (Recommended)**
- Double-click a username in the Online Users list
- The interface enters private chat mode:
  - Message field gets a blue border
  - Send button changes to "Send to username"
  - All button appears to exit private mode
- Type and send messages normally
- Click **All** button to return to group chat

### Sharing Multimedia

#### Sending Images
1. Click the **📷 Image** button
2. Select an image file (JPG, PNG, GIF, BMP)
3. Maximum size: 5MB
4. Image will display inline in the chat for all recipients

#### Sending Files
1. Click the **📎 File** button
2. Select any file
3. Maximum size: 10MB
4. Recipients see a download link
5. Click the link to save the file

#### Sending Voice Messages
1. **Press and hold** the **🎤 Voice** button
2. Speak into your microphone (1-60 seconds)
3. **Release** the button to send
4. Recipients see a **Listen** button to play the voice message

**Tips:**
- Button turns red while recording
- Maximum duration: 60 seconds
- Minimum duration: 1 second

### Disconnecting
- Click the **Disconnect** button
- Or close the window
- Server will notify all users of your disconnection

---

## Protocol Documentation

### Message Format

Messages are transmitted as pipe-delimited strings:
```
TYPE|SENDER|TIMESTAMP|CONTENT|RECIPIENT|FILENAME
```

**Fields:**
- `TYPE` - Message type (NORMAL, PRIVATE, IMAGE, FILE, VOICE, SYSTEM, JOIN, LEAVE, USER_LIST)
- `SENDER` - Username of the sender
- `TIMESTAMP` - Unix timestamp in milliseconds
- `CONTENT` - Message content or base64-encoded data
- `RECIPIENT` - Target username (for private messages, empty for broadcast)
- `FILENAME` - Original filename or duration (for IMAGE/FILE/VOICE types)

**Special Characters:**
- Pipe characters (`|`) in content are escaped as `<!PIPE!>`

### Message Types

| Type | Description | Direction |
|------|-------------|-----------|
| `NORMAL` | Regular broadcast message | Client → Server → All Clients |
| `PRIVATE` | Direct message to specific user | Client → Server → Recipient |
| `IMAGE` | Image file (base64 encoded) | Client → Server → All/Recipient |
| `FILE` | File attachment (base64 encoded) | Client → Server → All/Recipient |
| `VOICE` | Voice recording (base64 encoded) | Client → Server → All/Recipient |
| `SYSTEM` | Server notification | Server → Client(s) |
| `JOIN` | User joined notification | Server → All Clients |
| `LEAVE` | User left notification | Server → All Clients |
| `USER_LIST` | List of online users | Server → Client |

### Authentication Flow

1. **Client connects** → Server creates socket connection
2. **Server sends:** `ENTER_USERNAME`
3. **Client sends:** `USERNAME username`
4. **Server validates:**
   - Username format (alphanumeric, underscore, 3-20 chars)
   - Not already taken
5. **Server responds:**
   - Success: `SUCCESS|Welcome to the chat, username!`
   - Failure: `ERROR|reason`
6. **On success:**
   - Server registers client
   - Sends current user list to the new client
   - Broadcasts JOIN notification to all other clients

### Private Message Command

Format: `/msg recipient message content`

Example:
```
/msg alice Hello Alice, how are you?
```

Server parses the command and routes the message only to the recipient.

---

## Project Structure

### Package: `common`
Shared classes used by both server and client.

#### `Message.java`
- Represents a chat message
- Fields: type, sender, timestamp, content, recipient, fileName
- Methods for serialization/deserialization
- Enum `MessageType` with all message types

#### `Protocol.java`
- Protocol constants (commands, default host/port)
- Username validation
- Private message parsing utilities

### Package: `server`
Server-side components.

#### `ChatServer.java`
- Main server class with `main()` method
- Manages `ConcurrentHashMap<String, ClientHandler>` of active clients
- Methods:
  - `start()` - Starts the server and listens for connections
  - `registerClient()` - Adds a new client to the map
  - `removeClient()` - Removes a client from the map
  - `broadcast()` - Sends a message to all clients
  - `sendPrivateMessage()` - Sends a message to a specific client
  - `sendUserList()` - Sends the current user list to a client
  - `notifyUserJoined()` - Notifies all users of a new connection

#### `ClientHandler.java`
- Implements `Runnable` for multi-threading
- Handles one client connection
- Methods:
  - `run()` - Main thread method (authenticate → process messages)
  - `authenticate()` - Validates and registers the user
  - `processMessage()` - Routes incoming messages
  - `sendMessage()` - Sends a message to this client

### Package: `client`
Client-side components.

#### `ChatClient.java`
- Manages network communication
- Methods:
  - `connect()` - Establishes connection and authenticates
  - `disconnect()` - Closes the connection
  - `sendMessage()` - Sends a text message
  - `sendImage()` - Sends an image file
  - `sendFile()` - Sends a file
  - `sendVoice()` - Sends a voice recording
  - `startReceiving()` - Starts the receive thread
  - `processIncomingMessage()` - Handles incoming messages
- Interface: `MessageListener` for GUI callbacks

#### `ChatClientGUI.java`
- Swing-based user interface
- Components:
  - Connection panel (username, server, port, connect/disconnect buttons)
  - Chat area (JTextPane with styled messages)
  - Input panel (message field, buttons for send/image/file/voice)
  - User list (online users with double-click support)
- Methods:
  - `displayMessage()` - Routes messages to appropriate display method
  - `displayImageMessage()` - Shows images inline
  - `displayFileMessage()` - Shows download links
  - `displayVoiceMessage()` - Shows listen buttons
  - `playVoiceMessage()` - Plays voice recordings
  - `startRecording()` / `stopRecording()` - Voice recording logic

---

## Technical Implementation

### Multi-threading
- **Server:** One thread per connected client (ClientHandler)
- **Client:** Separate thread for receiving messages (prevents UI blocking)
- **Thread Safety:** `ConcurrentHashMap` for client storage, synchronized message processing

### Network Communication
- **Protocol:** TCP/IP using Java `Socket` and `ServerSocket`
- **Streams:** 
  - `BufferedReader` for reading text-based messages
  - `PrintWriter` for sending messages
- **Port:** Default 8888 (configurable)
- **Encoding:** UTF-8 for text, Base64 for binary data

### Audio Recording & Playback
- **API:** `javax.sound.sampled`
- **Format:** 16kHz sample rate, 16-bit depth, mono, signed PCM, big-endian
- **Recording:** `TargetDataLine` captures microphone input
- **Playback:** `SourceDataLine` plays audio output
- **Encoding:** Raw audio bytes encoded to Base64 for transmission

### Image & File Handling
- **Image Display:** `ImageIO` reads images, scaled to max 300x300 pixels
- **File Download:** Base64 decoded and written to disk
- **Size Limits:** 
  - Images: 5MB
  - Files: 10MB

### GUI Components
- **Framework:** Java Swing
- **Layout Managers:** BorderLayout, FlowLayout
- **Styled Text:** `JTextPane` with `StyledDocument` for colored messages
- **Components in Text:** `insertComponent()` for inline buttons

### Message Color Coding
- **Green:** Messages you sent
- **Black:** Messages you received
- **Blue:** Private messages
- **Gray:** System notifications

### Error Handling
- Connection failures with user-friendly error dialogs
- Input validation (username format, file sizes)
- Graceful disconnection handling
- Audio system compatibility checks

---

## Common Issues & Solutions

### Server won't start
- **Issue:** Port 8888 already in use
- **Solution:** Close other applications using the port or start server with custom port: `java -cp bin server.ChatServer 9999`

### Client can't connect (localhost)
- **Issue:** Server not running or wrong port
- **Solution:** 
  - Ensure server is started first
  - Verify you're using port 8888 (or the custom port if changed)
  - Use `localhost` or `127.0.0.1` as server address

### Client can't connect (LAN)
- **Issue:** Cannot connect from another computer on the network
- **Solution:**
  - Verify both computers are on the same network
  - Use the server's LAN IP address (shown when server starts), NOT localhost
  - Check firewall settings on server computer (allow port 8888)
  - Test network connectivity: `ping <server-ip>`
  - Ensure server is not bound to localhost only (our implementation binds to all interfaces by default)

### Firewall blocking connections
- **Issue:** Firewall preventing incoming connections
- **Solution:**
  - **Windows:** Windows Defender Firewall → Allow an app → Add Java
  - **Linux:** `sudo ufw allow 8888/tcp`
  - **macOS:** System Preferences → Security & Privacy → Firewall → Allow Java

### Voice recording doesn't work
- **Issue:** Microphone not available
- **Solution:** Check microphone permissions and availability in system settings

### Images/Files don't send
- **Issue:** File too large
- **Solution:** 
  - Images: Maximum 5MB (compress if needed)
  - Files: Maximum 10MB (split large files)

### Getting server IP address
- **Issue:** Don't know which IP to use for LAN connections
- **Solution:** 
  - The server displays all available IP addresses when it starts
  - Look for addresses starting with `192.168.x.x` or `10.0.x.x` (these are LAN addresses)
  - Alternatively, run `ipconfig` (Windows) or `ifconfig` (Linux/Mac) on the server computer

---

## Future Enhancements

Potential features for future versions:
- End-to-end encryption
- Message history persistence
- User accounts with passwords
- Group chat rooms
- Emoji support
- Read receipts
- Typing indicators
- File transfer progress bars

---

## License

This project is created for educational purposes.

---

## Authors

Developed as a TCP networking and GUI programming demonstration in Java.
