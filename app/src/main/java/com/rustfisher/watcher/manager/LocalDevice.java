package com.rustfisher.watcher.manager;

import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pInfo;
import android.util.Log;

import com.rustfisher.watcher.beans.MsgBean;
import com.rustfisher.watcher.service.CommunicationService;
import com.rustfisher.watcher.utils.ClientTransferThread;
import com.rustfisher.watcher.utils.WPProtocol;

import java.net.InetAddress;

/**
 * Holds status
 * Created by Rust Fisher on 2017/2/28.
 */
public final class LocalDevice {

    private static final String TAG = "rustApp";
    private volatile static boolean sendingOutCameraView = false;
    private volatile static byte[] onePicData;
    private static LocalDevice instance = new LocalDevice();
    private volatile static String clientIPAddress;
    private volatile WifiP2pDevice mDevice;
    private volatile WifiP2pInfo wifiP2pInfo;
    private PART myPart;
    private ClientTransferThread mClientTransferThread;
    private static String localIPAddress = "192.168.0.1";

    private LocalDevice() {
        mDevice = new WifiP2pDevice();
        myPart = PART.WATCHER;
    }

    private CommunicationService service;

    public CommunicationService getService() {
        return service;
    }

    public void setService(CommunicationService service) {
        this.service = service;
    }

    public static byte[] getOnePicData() {
        return onePicData;
    }

    public static void setOnePicData(byte[] onePicData) {
        LocalDevice.onePicData = onePicData;
    }

    public synchronized static boolean isSendingOutCameraView() {
        return sendingOutCameraView;
    }

    public synchronized static void setSendingOutCameraView(boolean s) {
        LocalDevice.sendingOutCameraView = s;
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

    public void sendMsgBeanToGroupOwner(MsgBean bean) {
        if (null != mClientTransferThread) {
            mClientTransferThread.sendMsgBean(bean);
        } else {
            Log.e(TAG, "fail. mClientTransferThread is NULL");
        }
    }

    public void sendPNGOut(byte[] picData) {
        if (isClient()) {
            sendMsgBeanToGroupOwner(new MsgBean(picData, MsgBean.TYPE_PNG));
        } else if (isGroupOwner()) {
        }
    }

    public void sendStringMsg(String msg) {
        if (isClient()) {
            sendMsgBeanToGroupOwner(new MsgBean(msg));
        } else if (isGroupOwner()) {
            getService().send(WPProtocol.DATA_HEAD_STR);
            getService().send(msg.getBytes());
            getService().send(WPProtocol.DATA_END);
        }
    }

    public void exitDevice() {
        stopClientTransferThread();
    }

    public static boolean isAddressCMD(byte[] cmd, int len) {
        if (null == cmd) {
            return false;
        }
        if (len > 52) {
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
