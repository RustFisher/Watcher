package com.rustfisher.watcher.manager;

import android.content.Context;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.util.Log;

import com.rustfisher.watcher.utils.BroadcastReceiveThread;
import com.rustfisher.watcher.utils.BroadcastThread;
import com.rustfisher.watcher.utils.DatagramReceiveThread;
import com.rustfisher.watcher.utils.DatagramSendThread;
import com.rustfisher.watcher.utils.LocalUtils;

import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * 数据报管理器
 * Created on 2019-6-24
 */
public class DatagramMgr {
    private static final String TAG = "rustAppDatagramMgr";
    public static final int COMMON_UDP_PORT = 9696;
    public static final int UDP_BROADCAST_PORT = 9697; // UDP广播的端口
    private static DatagramReceiveThread datagramReceiveThread;
    private static DatagramSendThread datagramSendThread;

    private static BroadcastReceiveThread broadcastReceiveThread; // 用来接收广播数据
    private static BroadcastThread broadcastThread;               // 发送广播

    public static void prepare(Context context) {
        WifiManager wifiManager = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        if (wifiManager != null) {
            WifiInfo wifiInfo = wifiManager.getConnectionInfo();
            final String localIP = LocalUtils.intToIpStr(wifiInfo.getIpAddress());
            Log.d(TAG, "prepare: 本地ip: " + localIP);
        } else {
            Log.e(TAG, "prepare: 无法获取WiFiManager");
        }
        final String targetIp = "192.168.2.133"; // 192.168.2.133; 192.168.2.124
        restartBroadcastThread(context);
        restartDatagramReceiveThread(COMMON_UDP_PORT);
//        restartSendThread(targetIp);
    }

    public static void finishDatagramReceiveThread() {
        if (datagramReceiveThread != null) {
            datagramReceiveThread.interrupt();
            datagramReceiveThread = null;
        }
    }

    public static void restartDatagramReceiveThread(int port) {
        finishDatagramReceiveThread();
        datagramReceiveThread = new DatagramReceiveThread(port);
        datagramReceiveThread.start();
    }

    public static void restartSendThread(String hostName) {
        Log.d(TAG, "restartSendThread: hostName: " + hostName);
        if (null != datagramSendThread) {
            datagramSendThread.interrupt();
            datagramSendThread = null;
        }
        try {
            datagramSendThread = new DatagramSendThread(InetAddress.getByName(hostName), COMMON_UDP_PORT);
            datagramSendThread.start();
            Log.d(TAG, "restartSendThread: 重启发送线程 ");
        } catch (UnknownHostException e) {
            Log.e(TAG, "restartSendThread: ", e);
        }
    }

    // 启动广播收发线程
    public static void restartBroadcastThread(Context context) {
        if (null != broadcastThread) {
            broadcastThread.interrupt();
            broadcastThread = null;
        }
        String localIp = "error";
        WifiManager wifiManager = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        if (wifiManager != null) {
            WifiInfo wifiInfo = wifiManager.getConnectionInfo();
            localIp = LocalUtils.intToIpStr(wifiInfo.getIpAddress());
        }
        Log.d(TAG, "restartBroadcastThread: localIP: " + localIp);
        broadcastThread = new BroadcastThread(context, localIp, UDP_BROADCAST_PORT);
        broadcastThread.start();

        if (null != broadcastReceiveThread) {
            broadcastReceiveThread.interrupt();
            broadcastReceiveThread = null;
        }
        broadcastReceiveThread = new BroadcastReceiveThread(localIp, UDP_BROADCAST_PORT);
        broadcastReceiveThread.start();
    }
}
