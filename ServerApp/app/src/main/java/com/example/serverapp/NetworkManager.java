package com.example.serverapp;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;
import android.util.Log;

import androidx.lifecycle.MutableLiveData;

import java.lang.ref.WeakReference;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class NetworkManager extends BroadcastReceiver {
    private static final String TAG = "NetworkManager";
    private final ExecutorService executorService;
    private final WeakReference<Context> contextRef;
    private final ConnectivityManager connectivityManager;
    public static MutableLiveData<Boolean> isWifiConnected = new MutableLiveData<>(false);

    /**
     * Constructs a NetworkManager to monitor network changes.
     *
     * @param context the application context
     */
    public NetworkManager(Context context) {
        this.contextRef = new WeakReference<>(context);
        this.executorService = Executors.newFixedThreadPool(2);
        this.connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        if (connectivityManager == null) {
            Log.e(TAG, "ConnectivityManager is null");
            return;
        }

        isWifiConnected(isConnected -> {
            if (isConnected) {
                Log.d(TAG, "Wi-Fi connected");
                isWifiConnected.postValue(true);
            } else {
                Log.d(TAG, "Wi-Fi disconnected");
                isWifiConnected.postValue(false);
            }
        });
    }

    /**
     * Checks if the device is connected to Wi-Fi.
     *
     * @param callback the callback to receive the result
     */
    public void isWifiConnected(WifiConnectionCallback callback) {
        executorService.submit(() -> {
            try {
                Context context = contextRef.get();
                if (context == null) {
                    Log.e(TAG, "Context is no longer available");
                    callback.onResult(false);
                    return;
                }

                ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
                NetworkCapabilities capabilities = connectivityManager.getNetworkCapabilities(connectivityManager.getActiveNetwork());
                boolean isConnected = capabilities != null && capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI);
                callback.onResult(isConnected);
            } catch (Exception e) {
                Log.e(TAG, "Error checking Wi-Fi connection: " + e.getMessage(), e);
                callback.onResult(false); // Handle the error gracefully
            }
        });
    }

    /**
     * Shuts down the executor service. Call this when it's no longer needed.
     */
    public void shutdown() {
        executorService.shutdown();
    }

    /**
     * Callback interface for Wi-Fi connection status.
     */
    public interface WifiConnectionCallback {
        void onResult(boolean isConnected);
    }
}
