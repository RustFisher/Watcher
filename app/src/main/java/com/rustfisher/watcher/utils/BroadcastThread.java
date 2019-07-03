package com.rustfisher.watcher.utils;

import android.content.Context;
import android.net.DhcpInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.util.Log;

import com.google.gson.Gson;
import com.rustfisher.watcher.transfer.model.commond.BroadcastMsg;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

/**
 * 发送广播，告知本机IP地址
 * JSON格式文本
 * Created on 2019-7-1
 */
public class BroadcastThread extends Thread {
    private static final String TAG = "rustAppServerBroadcast";
    private DatagramSocket datagramSocket;
    private DatagramPacket datagramPacket;
    private BroadcastMsg broadcastMsg;

    public BroadcastThread(Context context, String myIPv4, int broadcastPort) {
        setName("UDP广播线程 ");
        broadcastMsg = new BroadcastMsg();
        broadcastMsg.setCmd(255);
        broadcastMsg.setLan_ipv4(myIPv4);
        broadcastMsg.setNickname(Build.MODEL + "|" + myIPv4);
        broadcastMsg.setMsg("Anyone here?");
        Log.d(TAG, "BroadcastThread: broadcastMsg " + broadcastMsg);
        try {
            InetAddress broadcastAddress = getBroadcastAddress(context);
            datagramSocket = new DatagramSocket(); // 新建datagramSocket发送广播
            datagramSocket.setBroadcast(true);
            byte[] sendBuffer = new byte[2048];
            this.datagramPacket = new DatagramPacket(sendBuffer, sendBuffer.length, broadcastAddress, broadcastPort);
            Log.d(TAG, "UDP广播线程: broadcastAddress: " + broadcastAddress + ", broadcastPort: " + broadcastPort);
        } catch (Exception e) {
            Log.e(TAG, "UDP广播线程: ", e);
        }
    }

    @Override
    public void run() {
        super.run();
        Gson gson = new Gson();
        String json = gson.toJson(broadcastMsg);
        Log.d(TAG, getName() + "要发送的数据: " + json);
        int n = 0;
        while (!isInterrupted()) {
            try {
                Thread.sleep(1500);
                datagramPacket.setData(json.getBytes());
                datagramSocket.send(datagramPacket);
                Log.d(TAG, getName() + "run(): 发送数据: " + ++n);
            } catch (Exception e) {
                e.printStackTrace();
                Log.e(TAG, getName() + "发送线程 ", e);
                break;
            }
        }
        try {
            datagramSocket.close();
            datagramSocket = null;
        } catch (Exception e) {
            Log.e(TAG, getName() + "关闭发送socket出错: ", e);
        }
        Log.d(TAG, getName() + "运行结束");
    }

    private InetAddress getBroadcastAddress(Context context) throws IOException {
        WifiManager wifi = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        if (wifi == null) {
            return null;
        }
        DhcpInfo dhcp = wifi.getDhcpInfo();
        // handle null somehow

        int broadcast = (dhcp.ipAddress & dhcp.netmask) | ~dhcp.netmask;
        byte[] quads = new byte[4];
        for (int k = 0; k < 4; k++)
            quads[k] = (byte) ((broadcast >> k * 8) & 0xFF);
        return InetAddress.getByAddress(quads);
    }
}
