package com.example.clientapp;

import android.util.Log;

import androidx.lifecycle.MutableLiveData;

import java.io.*;
import java.net.*;
import java.util.concurrent.*;

public class Client {

    private Socket socket;
    private BufferedReader input;
    private PrintWriter output;
    private final ExecutorService executorService;
    public MutableLiveData<Boolean> isClientConnected = new MutableLiveData<>(false);
    public MutableLiveData<String> receivedMessage = new MutableLiveData<>();

    public Client() {
        // Create a single-threaded executor for client tasks
        executorService = Executors.newFixedThreadPool(4);
    }

    // Connect to the server and handle communication
    public void connectToServer(String serverIp, int port) {
        executorService.submit(() -> {
            try {
                // Connect to the server
                Log.d("Client", "Connecting to server at " + serverIp + ":" + port);
                socket = new Socket(serverIp, port);
                Log.d("Client", "Connected to server at " + serverIp + ":" + port);
                isClientConnected.postValue(true);

                // Setup input and output streams
                input = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                output = new PrintWriter(socket.getOutputStream(), true);

                // Start listening for messages from the server
                executorService.submit(new ServerMessageListener());

                // Send message to the server
                sendMessage("Hello from client!");

            } catch (IOException e) {
                Log.d("Client", "Error connecting to server: " + e.getMessage());
                isClientConnected.postValue(false);
            }
        });
    }

    public void sendMessage(String message) {
        executorService.submit(() -> {
            if (output != null) {
                output.println(message);
            }
        });
    }

    // Runnable to listen for server messages
    private class ServerMessageListener implements Runnable {
        @Override
        public void run() {
            try {
                String serverMessage;
                while ((serverMessage = input.readLine()) != null) {
                    Log.d("Client", "Received message from server: " + serverMessage);
                    receivedMessage.postValue(serverMessage);
                }
            } catch (IOException e) {
                Log.d("Client", "Error reading from server: " + e.getMessage());
            }
        }
    }

    // Disconnect the client and shut down the executor service
    public void disconnect() {
        executorService.submit(() -> {
            try {
                if (socket != null) {
                    socket.close();
                }
                if (!executorService.isShutdown()) {
                    executorService.shutdown();
                }
                isClientConnected.postValue(false);
            } catch (IOException e) {
                Log.d("Client", "Error disconnecting from server: " + e.getMessage());
            }
        });
    }
}
