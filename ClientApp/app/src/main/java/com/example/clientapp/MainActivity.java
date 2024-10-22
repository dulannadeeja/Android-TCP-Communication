package com.example.clientapp;

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

import com.example.clientapp.databinding.ActivityMainBinding;

public class MainActivity extends AppCompatActivity {
    Client client;
    NetworkManager networkManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        ActivityMainBinding binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        networkManager = new NetworkManager(getApplicationContext());
        client = new Client();

        NetworkManager.isWifiConnected.observe(this, isConnected -> {
            runOnUiThread(() -> {
                if (isConnected) {
                    // Device is connected to Wi-Fi
                    binding.tvWifiStatus.setVisibility(View.INVISIBLE);
                    if (Boolean.TRUE.equals(client.isClientConnected.getValue())) {
                        binding.tvConnectionStatus.setText("Connected");
                        Log.d("Connection Status", "Connected");
                        binding.connectButton.setOnClickListener(v -> {
                            client.disconnect();
                            Log.d("Connection Status", "Disconnected");
                        });
                    } else {
                        binding.tvConnectionStatus.setText("Disconnected");
                        binding.connectButton.setOnClickListener(v -> {
                            Log.d("Connection Status", "Connecting");
                            if (isValidIP(binding.etServerIp.getText().toString())) {
                                if (isValidPort(binding.etPort.getText().toString())) {
                                    new Thread(() -> {
                                        Log.d("Connection Status", "Connecting to server");
                                        client.connectToServer(binding.etServerIp.getText().toString(), Integer.parseInt(binding.etPort.getText().toString()));
                                    }).start();
                                } else {
                                    binding.etPort.setError("Invalid Port");
                                }
                            } else {
                                binding.etServerIp.setError("Invalid IP Address");
                            }
                        });
                    }
                } else {
                    // Device is not connected to Wi-Fi
                    binding.tvWifiStatus.setVisibility(View.VISIBLE);
                }
            });
        });

        client.isClientConnected.observe(
                this,
                isConnected -> {
                    runOnUiThread(() -> {
                        if (isConnected) {
                            binding.tvConnectionStatus.setText("Connected");
                        } else {
                            binding.tvConnectionStatus.setText("Disconnected");
                        }
                    });
                }
        );

        client.receivedMessage.observe(
                this,
                message -> {
                    runOnUiThread(() -> {
                        if (message != null) {
                            binding.tvMessageReceived.setText(message);
                        } else {
                            binding.tvMessageReceived.setText("No message received");
                        }
                    });
                }
        );

        binding.sendButton.setOnClickListener(v -> {
            String message = binding.etMessage.getText().toString();
            if (!message.isEmpty()) {
                client.sendMessage(message);
            }else{
                binding.etMessage.setError("Message cannot be empty");
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        IntentFilter filter = new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION);
        registerReceiver(networkManager, filter);
    }

    @Override
    protected void onStop() {
        super.onStop();
        unregisterReceiver(networkManager);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Disconnect when the app is closed
        client.disconnect();
    }

    public Boolean isValidIP(String ip) {
        String[] parts = ip.split("\\.");
        if (parts.length != 4) {
            return false;
        }
        for (String s : parts) {
            int i = Integer.parseInt(s);
            if ((i < 0) || (i > 255)) {
                return false;
            }
        }
        return true;
    }

    public Boolean isValidPort(String port) {
        try {
            int i = Integer.parseInt(port);
            if ((i < 0) || (i > 65535)) {
                return false;
            }
        } catch (NumberFormatException e) {
            return false;
        }
        return true;
    }
}