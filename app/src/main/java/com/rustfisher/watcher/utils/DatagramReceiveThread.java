package com.rustfisher.watcher.utils;

import android.util.Log;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;

/**
 * 接收数据包线程
 * Created on 2019-6-24
 */
public class DatagramReceiveThread extends Thread {
    private static final String TAG = "rustAppDR";
    private DatagramSocket datagramSocket;
    private DatagramPacket datagramPacket;

    public DatagramReceiveThread(DatagramSocket socket, DatagramPacket packet) {
        this.datagramSocket = socket;
        this.datagramPacket = packet;
    }

    @Override
    public void run() {
        super.run();
        Log.d(TAG, "run: 开始运行");
        while (!isInterrupted()) {
            try {
                datagramSocket.receive(datagramPacket);
            } catch (IOException e) {
                e.printStackTrace();
            }
            byte[] data = datagramPacket.getData();
            Log.d(TAG, "run: 收到数据 " + data.length);
        }
        Log.d(TAG, "run: 运行完毕");
    }
}
