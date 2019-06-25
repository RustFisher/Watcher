package com.rustfisher.watcher.utils;

import android.util.Log;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;

/**
 * 数据包发送线程
 * Created on 2019-6-25
 */
public class DatagramSendThread extends Thread {
    private static final String TAG = "rustAppDST";
    private DatagramSocket datagramSocket;
    private DatagramPacket datagramPacket;

    public DatagramSendThread(InetAddress inetAddress, int port) {
        try {
            this.datagramSocket = new DatagramSocket();
            byte[] sendBuffer = new byte[2048];
            this.datagramPacket = new DatagramPacket(sendBuffer, sendBuffer.length, inetAddress, port);
            Log.d(TAG, "DatagramSendThread: inetAddress: " + inetAddress + ", port: " + port);
        } catch (SocketException e) {
            Log.e(TAG, "DatagramSendThread: ", e);
        }
    }

    @Override
    public void run() {
        super.run();
        int n = 0;
        byte[] sendTmp = {1, 3, 5, 7, 9, 2, 4};
        while (!isInterrupted()) {
            try {
                Thread.sleep(1000);
                sendTmp[0] = (byte) n++;
                datagramPacket.setData(sendTmp);
                datagramSocket.send(datagramPacket);
                Log.d(TAG, "run: 发送数据");
            } catch (Exception e) {
                e.printStackTrace();
                Log.e(TAG, "发送线程 ", e);
            }
        }
    }
}
