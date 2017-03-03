package com.rustfisher.watcher.manager;

import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pInfo;
import android.util.Log;

import com.rustfisher.watcher.utils.ClientTransferThread;

import java.net.InetAddress;
import java.net.UnknownHostException;

import com.rustfisher.watcher.utils.GroupOwnerTransferThread;
import com.rustfisher.watcher.utils.WPProtocol;

/**
 * Holds status
 * Created by Rust Fisher on 2017/2/28.
 */
public final class LocalDevice {

    private static final String TAG = "rustApp";
    private static LocalDevice instance = new LocalDevice();
    private volatile static String clientIPAddress;
    private volatile WifiP2pDevice mDevice;
    private volatile WifiP2pInfo wifiP2pInfo;
    private PART myPart;
    private ClientTransferThread mClientTransferThread;
    private GroupOwnerTransferThread mGroupOwnerTransferThread;
    private static String localIPAddress = "192.168.0.1";

    private LocalDevice() {
        mDevice = new WifiP2pDevice();
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
        localIPAddress = ip;
    }

    public static String getLocalIPAddress() {
        return localIPAddress;
    }

    public static byte[] IpStr2Bytes(String ip) {
        String[] ipArr = ip.split("\\.");
        byte[] ipBytes = new byte[ipArr.length];
        for (int i = 0; i < ipBytes.length; i++) {
            ipBytes[i] = Integer.valueOf(ipArr[i]).byteValue();
        }
        return ipBytes;
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
    public void startClientTransferThread() {
        if (null == mClientTransferThread) {
            mClientTransferThread = new ClientTransferThread(LocalDevice.getInstance().getOwnerInetAddress());
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

    // As a group owner, I can find the client when I know the address.
    public void startGroupOwnerTransferThread() {
        try {
            if (null == mGroupOwnerTransferThread) {
                byte[] addressBytes = IpStr2Bytes(clientIPAddress);
                mGroupOwnerTransferThread = new GroupOwnerTransferThread(InetAddress.getByAddress(addressBytes));
                mGroupOwnerTransferThread.start();
            }
        } catch (UnknownHostException e) {
            e.printStackTrace();
            Log.e(TAG, "Can not get the client address, startGroupOwnerTransferThread fail! ", e.getCause());
        }
    }

    public void stopGroupOwnerTransferThread() {
        if (null != mGroupOwnerTransferThread) {
            mGroupOwnerTransferThread.closeThread();
            mGroupOwnerTransferThread = null;
        }
    }

    public void sendMsgToClient(String msg) {
        if (null != mGroupOwnerTransferThread) {
            mGroupOwnerTransferThread.send(msg);
        } else {
            Log.e(TAG, "Can not send msg, null == mGroupOwnerTransferThread");
        }
    }

    public static boolean isAddressCMD(byte[] cmd, int len) {
        if (null == cmd) {
            return false;
        }
        byte[] input = new byte[len];
        System.arraycopy(cmd, 0, input, 0, len);
        String inputStr = new String(input);
        String[] inputCMDArr = inputStr.split("#");
        if (inputCMDArr.length != 2) {
            return false;
        }
        if (inputCMDArr[0].equals(WPProtocol.MY_IP_ADDRESS)) {
            clientIPAddress = inputCMDArr[1]; // store client ip address
            Log.d(TAG, "clientIPAddress =  " + clientIPAddress);
            return true;
        }
        return false;
    }

    public static String intToIpStr(int i) {
        return (i & 0xFF) + "." +
                ((i >> 8) & 0xFF) + "." +
                ((i >> 16) & 0xFF) + "." +
                (i >> 24 & 0xFF);
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
