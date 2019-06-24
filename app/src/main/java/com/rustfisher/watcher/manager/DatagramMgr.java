package com.rustfisher.watcher.manager;

import android.content.Context;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.util.Log;

import com.rustfisher.watcher.utils.DatagramReceiveThread;
import com.rustfisher.watcher.utils.LocalUtils;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

/**
 * 数据报管理器
 * Created on 2019-6-24
 */
public class DatagramMgr {
    private static final String TAG = "rustAppDatagramMgr";
    public static final int COMMON_UDP_PORT = 8081;
    public static final int UDP_RECEIVE_DATA_BUFFER_LEN = 2048;
    private static DatagramReceiveThread datagramReceiveThread;
    private static DatagramSocket datagramSocket;
    private static DatagramPacket receiveDatagramPacket;
    private static DatagramPacket sendDatagramPacket;

    /**
     * 应用启动时，尽早调用
     */
    public static void prepare(Context context) {
        WifiManager wifiManager = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        if (wifiManager != null) {
            WifiInfo wifiInfo = wifiManager.getConnectionInfo();
            final String localIP = LocalUtils.intToIpStr(wifiInfo.getIpAddress());
            Log.d(TAG, "prepare: 本地ip: " + localIP);
            try {
                datagramSocket = new DatagramSocket();
                final byte[] rData = new byte[UDP_RECEIVE_DATA_BUFFER_LEN];
                InetAddress inetAddress = InetAddress.getByName(localIP);
                receiveDatagramPacket = new DatagramPacket(rData, rData.length, inetAddress, COMMON_UDP_PORT);
                restartDatagramReceiveThread(datagramSocket, receiveDatagramPacket);
            } catch (Exception e) {
                e.printStackTrace();
                Log.e(TAG, "prepare: ", e);
            }
        } else {
            Log.e(TAG, "prepare: 无法获取WiFiManager");
        }
    }

    public static void finishDatagramReceiveThread() {
        if (datagramReceiveThread != null) {
            datagramReceiveThread.interrupt();
            datagramReceiveThread = null;
        }
    }

    public static void restartDatagramReceiveThread(DatagramSocket socket, DatagramPacket packet) {
        finishDatagramReceiveThread();
        datagramReceiveThread = new DatagramReceiveThread(socket, packet);
        datagramReceiveThread.start();
    }
}
