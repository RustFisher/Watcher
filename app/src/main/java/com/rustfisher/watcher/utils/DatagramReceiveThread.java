package com.rustfisher.watcher.utils;

import android.util.Log;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;

/**
 * 接收数据包线程
 * Created on 2019-6-24
 */
public class DatagramReceiveThread extends Thread {
    private static final String TAG = "rustAppDR";
    private DatagramSocket datagramSocket;
    private DatagramPacket datagramPacket;

    public DatagramReceiveThread(int port) {
        try {
            this.datagramSocket = new DatagramSocket(port);
            byte[] buf = new byte[2048];
            this.datagramPacket = new DatagramPacket(buf, buf.length);
        } catch (SocketException e) {
            Log.e(TAG, "DatagramReceiveThread: ", e);
        }
    }

    @Override
    public void run() {
        super.run();
        if (datagramSocket == null) {
            Log.e(TAG, "创建socket失败");
            return;
        }
        Log.d(TAG, "run: 开始运行");
        while (!isInterrupted()) {
            try {
                datagramSocket.receive(datagramPacket);
            } catch (IOException e) {
                e.printStackTrace();
            }
            byte[] data = datagramPacket.getData();
            Log.d(TAG, "run: 收到数据 " + bytes2Hex(data));
        }
        Log.d(TAG, "run: 运行完毕");
    }

    public static String bytes2Hex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        String tmp;
        for (byte b : bytes) {
            tmp = Integer.toHexString(0xFF & b);
            if (tmp.length() == 1) {
                tmp = "0" + tmp;
            }
            sb.append(tmp);
        }
        return sb.toString();
    }

}
