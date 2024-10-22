package com.example.serverapp;

import android.content.Context;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.util.Log;
import java.lang.ref.WeakReference;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class IPManager {
    private static final String TAG = "IPManager";
    private final WeakReference<Context> contextRef;
    private final NetworkManager networkManager;
    private final ExecutorService executorService;

    /**
     * Constructs an IPManager to retrieve the device's local IP address.
     *
     * @param context      the application context
     * @param networkManager the instance of NetworkManager to check Wi-Fi connection
     */
    public IPManager(Context context, NetworkManager networkManager) {
        this.contextRef = new WeakReference<>(context);
        this.networkManager = networkManager;
        this.executorService = Executors.newSingleThreadExecutor();
    }

    /**
     * Retrieves the device's local IP address if connected to Wi-Fi.
     *
     * @param callback the callback to receive the result
     */
    public void getDeviceLocalIPAddress(IPAddressCallback callback) {
        executorService.submit(() -> { // Run the task in the background
            networkManager.isWifiConnected(isConnected -> {
                if (isConnected) {
                    Context context = contextRef.get();
                    if (context != null) {
                        WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
                        if (wifiManager != null) {
                            WifiInfo wifiInfo = wifiManager.getConnectionInfo();
                            int ipAddress = wifiInfo.getIpAddress();
                            String ipString = String.format("%d.%d.%d.%d",
                                    (ipAddress & 0xff),
                                    (ipAddress >> 8 & 0xff),
                                    (ipAddress >> 16 & 0xff),
                                    (ipAddress >> 24 & 0xff));
                            callback.onResult(ipString);
                        } else {
                            Log.e(TAG, "WifiManager is null");
                            callback.onResult(null);
                        }
                    } else {
                        Log.e(TAG, "Context is no longer available");
                        callback.onResult(null);
                    }
                } else {
                    callback.onResult(null); // Not connected to Wi-Fi
                }
            });
        });
    }

    /**
     * Callback interface for IP address retrieval.
     */
    public interface IPAddressCallback {
        void onResult(String ipAddress);
    }
}
