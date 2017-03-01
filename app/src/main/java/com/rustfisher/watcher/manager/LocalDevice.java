package com.rustfisher.watcher.manager;

import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pInfo;
import android.util.Log;

import com.rustfisher.watcher.utils.ClientTransferThread;

import java.net.InetAddress;

/**
 * Holds status
 * Created by Rust Fisher on 2017/2/28.
 */
public final class LocalDevice {

    private static final String TAG = "rustApp";
    private static LocalDevice instance = new LocalDevice();
    private volatile WifiP2pDevice device;
    private volatile WifiP2pInfo wifiP2pInfo;
    private PART myPart;
    private ClientTransferThread mClientTransferThread;

    private LocalDevice() {
        device = new WifiP2pDevice();
        myPart = PART.WATCHER;
    }

    public void setMyPart(PART myPart) {
        this.myPart = myPart;
    }

    public boolean isMonitor() {
        return PART.MONITOR.equals(myPart);
    }

    public boolean isWatcher() {
        return PART.WATCHER.equals(myPart);
    }

    public static LocalDevice getInstance() {
        return instance;
    }

    public synchronized boolean isConnected() {
        return device.status == WifiP2pDevice.CONNECTED;
    }

    public synchronized void setDevice(WifiP2pDevice device) {
        this.device = new WifiP2pDevice(device);
    }

    public synchronized void setWifiP2pInfo(WifiP2pInfo wifiP2pInfo) {
        this.wifiP2pInfo = new WifiP2pInfo(wifiP2pInfo);
    }

    public synchronized InetAddress getOwnerAddress() {
        return wifiP2pInfo.groupOwnerAddress;
    }

    /**
     * Must connected.
     */
    public synchronized boolean isGroupOwner() {
        return isConnected() && wifiP2pInfo.isGroupOwner;
    }

    public synchronized boolean isClient() {
        return isConnected() && !wifiP2pInfo.isGroupOwner;
    }

    public void startClientTransferThread() {
        if (null == mClientTransferThread) {
            mClientTransferThread = new ClientTransferThread(LocalDevice.getInstance().getOwnerAddress());
            mClientTransferThread.start();
        }
    }

    public void stopClientTransferThread() {
        if (null != mClientTransferThread) {
            mClientTransferThread.closeThread();
            mClientTransferThread = null;
        }
    }

    public void sendMsgToGroupOwner(String msg) {
        if (null != mClientTransferThread) {
            mClientTransferThread.send(msg);
        } else {
            Log.e(TAG, "mClientTransferThread is NULL");
        }
    }

    /**
     * MONITOR receives data from WATCHER.
     */
    public enum PART {
        MONITOR(0, "monitor"),
        WATCHER(1, "watcher");

        int code;
        String comment;

        PART(int code, String comment) {
            this.code = code;
            this.comment = comment;
        }
    }
}
