package com.example.serverapp;

import android.util.Log;

import androidx.lifecycle.MutableLiveData;

import java.io.*;
import java.net.*;
import java.util.concurrent.*;

public class Server {

    private ServerSocket serverSocket;
    private ExecutorService executorService;
    public static MutableLiveData<Boolean> isServerRunning = new MutableLiveData<>(false);
    public static MutableLiveData<Boolean> isClientConnected = new MutableLiveData<>(false);
    public static MutableLiveData<String> receivedMessage = new MutableLiveData<>();

    // To hold all active client handlers
    private final ConcurrentHashMap<Socket, ClientHandler> clientHandlers = new ConcurrentHashMap<>();

    public Server() {
        // Create a single-threaded executor for server tasks
        executorService = Executors.newFixedThreadPool(4);
    }

    // Start the server and listen for incoming connections
    public void startServer(int port) {
        try {
            // Create a server socket
            serverSocket = new ServerSocket(port);
            Log.d("Server", "Server started on port " + port);
            isServerRunning.postValue(true);

            // Keep accepting client connections
            while (!serverSocket.isClosed()) {
                // Accept a client connection (customer display app)
                Log.d("Server", "Waiting for a client to connect...");
                Socket clientSocket = serverSocket.accept();
                Log.d("Server", "Accepted connection from " + clientSocket.getInetAddress());
                isClientConnected.postValue(true);

                // Create a ClientHandler for the connected client and store it
                ClientHandler clientHandler = new ClientHandler(clientSocket);
                clientHandlers.put(clientSocket, clientHandler);

                // Submit a task to the thread pool to handle the client
                executorService.submit(clientHandler);
            }

        } catch (IOException e) {
            Log.e("Server", "Error starting the server: " + e.getMessage());
            isServerRunning.postValue(false);
        }
    }

    // Stop the server and shut down the executor service
    public void stopServer() {
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
            if (executorService != null && !executorService.isShutdown()) {
                executorService.shutdown();
            }
            isServerRunning.postValue(false);
        } catch (IOException e) {
            Log.e("Server", "Error stopping the server: " + e.getMessage());
        }
    }

    // Public method to send a message to a specific client
    public void sendMessageToClient(Socket clientSocket, String message) {
        ClientHandler handler = clientHandlers.get(clientSocket);
        if (handler != null) {
            handler.sendMessage(message);
        } else {
            Log.d("Server", "Client not found for socket: " + clientSocket.getInetAddress());
        }
    }

    // Public method to broadcast a message to all connected clients
    public void broadcastMessage(String message) {
        for (ClientHandler handler : clientHandlers.values()) {
            handler.sendMessage(message);
        }
    }

    // ClientHandler runs in a separate thread, handling communication with the client
    private static class ClientHandler implements Runnable {
        private final Socket clientSocket;
        private PrintWriter output;
        private ExecutorService executorService;

        public ClientHandler(Socket socket) {
            this.clientSocket = socket;
            this.executorService = Executors.newFixedThreadPool(2);
        }

        @Override
        public void run() {
            try {
                Log.d("ClientHandler", "Handling client: " + clientSocket.getInetAddress());
                // Setup input and output streams
                BufferedReader input = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                output = new PrintWriter(clientSocket.getOutputStream(), true);

                // Read messages sent by the client
                String clientMessage;
                while ((clientMessage = input.readLine()) != null) {
                    try {
                        // Process the message
                        Log.d("ClientHandler", "Received message from client: " + clientMessage);
                        receivedMessage.postValue(clientMessage);
                        // Send a response back to the client
                        output.println("message accepted from server side.");
                    } catch (Exception e) {
                        Log.e("ClientHandler", "Error processing message: " + e.getMessage());
                    }
                }

                // Close client connection when done
                clientSocket.close();
                isClientConnected.postValue(false);
                Log.d("ClientHandler", "Client disconnected: " + clientSocket.getInetAddress());

            } catch (IOException e) {
                Log.e("ClientHandler", "Error handling client: " + e.getMessage());
            }
        }

        // Public method to send a message to the client
        public void sendMessage(String message) {
            executorService.submit(()->{
                if (output != null) {
                    output.println(message);
                    Log.d("ClientHandler", "Sent message to client: " + message);
                } else {
                    Log.e("ClientHandler", "Output stream is null. Cannot send message.");
                }
            });
        }
    }
}
