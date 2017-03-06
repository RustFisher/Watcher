package com.rustfisher.watcher.utils;


import android.content.IntentFilter;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pManager;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public final class LocalUtils {

    private static final String TAG = "rustApp";

    public static IntentFilter makeWiFiP2pIntentFilter() {
        IntentFilter intentFilter;
        intentFilter = new IntentFilter();
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);
        return intentFilter;
    }

    public static String getDeviceStatusStr(int deviceStatus) {
        switch (deviceStatus) {
            case WifiP2pDevice.AVAILABLE:
                return "Available";
            case WifiP2pDevice.INVITED:
                return "Invited";
            case WifiP2pDevice.CONNECTED:
                return "Connected";
            case WifiP2pDevice.FAILED:
                return "Failed";
            case WifiP2pDevice.UNAVAILABLE:
                return "Unavailable";
            default:
                return "Unknown";

        }
    }

    public static boolean copyFile(InputStream inputStream, OutputStream out) {
        byte buf[] = new byte[1024];
        int len;
        try {
            while ((len = inputStream.read(buf)) != -1) {
                out.write(buf, 0, len);

            }
            out.close();
            inputStream.close();
        } catch (IOException e) {
            Log.d(TAG, e.toString());
            return false;
        }
        return true;
    }

    public static void log_d_Str(byte[] bytes, int len) {
        byte[] bytes1 = new byte[len];
        System.arraycopy(bytes, 0, bytes1, 0, len);
        Log.d(TAG, new String(bytes1));
    }

    private static StringBuilder sb = new StringBuilder();

    public static void logBytes(byte[] bytes, int len) {
        sb = new StringBuilder("] ");
        for (int i = 0; i < len; i++) {
            sb.append(Integer.toHexString(0xff & bytes[i])).append(" ");
        }
        Log.d(TAG, "[len=" + len + sb.toString());
    }

}
