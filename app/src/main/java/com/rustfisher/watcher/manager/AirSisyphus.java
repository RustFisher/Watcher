package com.rustfisher.watcher.manager;

import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pInfo;
import android.util.Log;

import com.rustfisher.watcher.WiFiApp;
import com.rustfisher.watcher.beans.MsgBean;
import com.rustfisher.watcher.utils.AppConfigs;
import com.rustfisher.watcher.utils.ClientTransferThread;
import com.rustfisher.watcher.utils.ReceiveSocketThread;

import java.net.InetAddress;

/**
 * Sisyphus in the air, who holds WiFi p2p status
 * Created by Rust Fisher on 2017/2/28.
 */
public final class AirSisyphus {

    private static final String TAG = "rustApp";
    private volatile static byte[] onePicData;
    private static AirSisyphus instance = new AirSisyphus();
    private volatile WifiP2pDevice mDevice;
    private volatile WifiP2pInfo wifiP2pInfo;
    private static String localIPAddress = "*.*.*.*";

    private ReceiveSocketThread mGroupOwnerThread;      // Group owner part
    private ClientTransferThread mClientTransferThread; // Client part

    private AirSisyphus() {
        mDevice = new WifiP2pDevice();
    }

    public static byte[] getOnePicData() {
        return onePicData;
    }

    public static void setOnePicData(byte[] onePicData) {
        AirSisyphus.onePicData = onePicData;
    }

    public static AirSisyphus getInstance() {
        return instance;
    }

    public synchronized boolean isConnected() {
        return mDevice.status == WifiP2pDevice.CONNECTED;
    }

    public synchronized void setDevice(WifiP2pDevice mDevice) {
        this.mDevice = new WifiP2pDevice(mDevice);
    }

    public String getLocalMacAddressStr() {
        return mDevice.deviceAddress;
    }

    public synchronized void setWifiP2pInfo(WifiP2pInfo wifiP2pInfo) {
        this.wifiP2pInfo = new WifiP2pInfo(wifiP2pInfo);
    }

    public synchronized InetAddress getOwnerInetAddress() {
        return wifiP2pInfo.groupOwnerAddress;
    }

    public static void setLocalIPAddress(String ip) {
        Log.d(TAG, "setLocalIPAddress: " + ip);
        localIPAddress = ip;
    }

    public static String getLocalIPAddress() {
        return localIPAddress;
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

    // As a client, I know the group owner address
    private void startClientTransferThread() {
        if (null == mClientTransferThread) {
            mClientTransferThread =
                    new ClientTransferThread(AirSisyphus.getInstance().getOwnerInetAddress(), WiFiApp.getApp().getApplicationContext());
            mClientTransferThread.start();
        }
    }

    public void stopClientTransferThread() {
        if (null != mClientTransferThread) {
            mClientTransferThread.closeThread();
            mClientTransferThread = null;
        }
    }

    public void sendMsgBeanToGroupOwner(MsgBean bean) {
        if (null != mClientTransferThread) {
            mClientTransferThread.sendMsgBean(bean);
        } else {
            Log.e(TAG, "fail. mClientTransferThread is NULL");
        }
    }

    public void sendCameraJPEG(byte[] data) {
        if (isClient()) {
            sendMsgBeanToGroupOwner(new MsgBean(data, MsgBean.TYPE_JPEG));
        } else if (isGroupOwner()) {
            groupOwnerSend(new MsgBean(data, MsgBean.TYPE_JPEG));
        }
    }

    public void sendPNGOut(byte[] picData) {
        if (isClient()) {
            sendMsgBeanToGroupOwner(new MsgBean(picData, MsgBean.TYPE_PNG));
        } else if (isGroupOwner()) {
            groupOwnerSend(new MsgBean(picData, MsgBean.TYPE_PNG));
        }
    }

    public void sendStringMsg(String msg) {
        if (isClient()) {
            sendMsgBeanToGroupOwner(new MsgBean(msg));
        } else if (isGroupOwner()) {
            groupOwnerSend(new MsgBean(msg));
        }
    }

    public void stopGroupOwnerThread() {
        if (null != mGroupOwnerThread) {
            mGroupOwnerThread.interrupt();
            mGroupOwnerThread = null;
        }
    }

    private void groupOwnerSend(MsgBean msg) {
        if (mGroupOwnerThread != null) {
            mGroupOwnerThread.sendMsgBean(msg);
        } else {
            Log.e(TAG, "[service] send fail");
        }
    }

    public void asGroupOwner() {
        if (mGroupOwnerThread == null) {
            mGroupOwnerThread = new ReceiveSocketThread(AppConfigs.PORT_GROUP_OWNER);
            mGroupOwnerThread.start();
        }
        stopClientTransferThread();
    }

    public void asClient() {
        startClientTransferThread();
    }

    public void exitDevice() {
        stopClientTransferThread();
        stopGroupOwnerThread();
    }

}
