package com.example.serverapp;

import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.serverapp.databinding.ActivityMainBinding;

public class MainActivity extends AppCompatActivity {

    private Server server;
    private NetworkManager networkManager;
    private IPManager ipManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActivityMainBinding binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        EdgeToEdge.enable(this);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        server = new Server();
        networkManager = new NetworkManager(getApplicationContext());
        ipManager = new IPManager(getApplicationContext(),networkManager);

        NetworkManager.isWifiConnected.observe(this, isConnected -> {
            runOnUiThread(() -> {
                if (isConnected) {
                    // Device is connected to Wi-Fi
                    binding.tvWifiStatus.setVisibility(View.INVISIBLE);
                } else {
                    // Device is not connected to Wi-Fi
                    binding.tvWifiStatus.setVisibility(View.VISIBLE);
                }
            });

            ipManager.getDeviceLocalIPAddress(ipAddress -> {
                runOnUiThread(() -> {
                    Log.d("IP Address", ipAddress != null ? ipAddress : "Not connected to Wi-Fi");
                    if (ipAddress != null) {
                        binding.tvServerIp.setText(ipAddress);
                    } else {
                        binding.tvServerIp.setText("XXX.XXX.XXX.XXX");
                    }
                });
            });
        });

        Server.isServerRunning.observe(this, isRunning -> {
            runOnUiThread(() -> {
                if (isRunning) {
                    Log.d("Server", "Server is running");
                    binding.tvServerStatus.setText("running");
                    binding.spinButton.setText("Stop Server");
                    binding.spinButton.setOnClickListener(v -> server.stopServer());
                } else {
                    binding.tvServerStatus.setText("terminated");
                    binding.spinButton.setText("Spin-Up Server");
                    Log.d("Server", "Server is not running");
                    binding.spinButton.setOnClickListener(v -> {
                        String port = binding.etPort.getText().toString();
                        if (isValidPort(port)) {
                            new Thread(() -> server.startServer(Integer.parseInt(port))).start();
                        } else {
                            binding.etPort.setError("Invalid port number");
                        }
                    });
                }
            });
        });

        Server.isClientConnected.observe(this, isConnected -> {
            runOnUiThread(() -> {
                if (isConnected) {
                    Log.d("Client", "Client is connected");
                    binding.tvConnectionStatus.setText("connected");
                } else {
                    Log.d("Client", "Client is disconnected");
                    binding.tvConnectionStatus.setText("disconnected");
                }
            });
        });

        binding.sendButton.setOnClickListener(v -> {
            String message = binding.etMessage.getText().toString();
            if (!message.isEmpty()) {
                server.broadcastMessage(message);
            }else {
                binding.etMessage.setError("Message cannot be empty");
            }
        });

        Server.receivedMessage.observe(this, message -> {
            runOnUiThread(() -> {
                if (message != null) {
                    binding.tvMessageReceived.setText(message);
                } else {
                    binding.tvMessageReceived.setText("No message received");
                }
            });
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        IntentFilter filter = new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION);
        registerReceiver(networkManager, filter);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Stop the server when the activity is destroyed
        server.stopServer();
    }

    @Override
    protected void onStop() {
        super.onStop();
        unregisterReceiver(networkManager);
    }

    public Boolean isValidPort(String port) {
        try {
            int portNumber = Integer.parseInt(port);
            return portNumber >= 0 && portNumber <= 65535;
        } catch (NumberFormatException e) {
            return false;
        }
    }
}