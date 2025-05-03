# Java Chat App

A simple multi-user chat application with file sharing.

## Setup

### Requirements
- JDK 17+
- H2 Database jar (included)

### Compiling
```
javac --release 17 -cp h2-2.2.224.jar *.java
```

### Running

Start the server:
```
java -cp ".;h2-2.2.224.jar" ChatServer
```

Start the client:
```
java -cp ".;h2-2.2.224.jar" ChatGUI
```

Note: On Linux/Mac, use `:` instead of `;` in the classpath.

## Login Credentials

Use any of these accounts:
- admin / admin123
- user1 / password1
- user2 / password2

## Features

- Text messaging
- File sharing
- Chat history
- User authentication

## Files

- ChatServer.java - Handles incoming connections
- ChatClient.java - Client connection logic
- ChatGUI.java - User interface
- ClientHandler.java - Per-client connection handler
- Message.java - Message structure
- InMemoryDB.java - Simple user authentication
- DBManager.java - Database user authentication

## Troubleshooting

- If messages aren't appearing, check if chat_history.txt exists
- Make sure server is running before starting clients
- Received files go to the "received_files" directory