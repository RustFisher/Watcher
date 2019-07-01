package com.rustfisher.watcher.utils;

import android.util.Log;

import com.google.gson.Gson;
import com.rustfisher.watcher.transfer.WaProtocol;
import com.rustfisher.watcher.transfer.model.BaseMsg;
import com.rustfisher.watcher.transfer.model.commond.BroadcastMsg;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;

/**
 * 接收广播数据包线程
 * Created on 2019-7-1
 */
public class BroadcastReceiveThread extends Thread {
    private static final String TAG = "rustAppDR";
    private DatagramSocket datagramSocket;
    private DatagramPacket datagramPacket;
    private String localIp; // 本地ip地址

    public BroadcastReceiveThread(String localIp, int port) {
        setName("广播接收线程 ");
        this.localIp = localIp;
        try {
            this.datagramSocket = new DatagramSocket(port);
            byte[] buf = new byte[20480];
            this.datagramPacket = new DatagramPacket(buf, buf.length);
        } catch (SocketException e) {
            Log.e(TAG, getName(), e);
        }
    }

    @Override
    public void run() {
        super.run();
        if (datagramSocket == null) {
            Log.e(TAG, getName() + " 创建socket失败");
            return;
        }
        Log.d(TAG, getName() + " run: 开始运行");
        while (!isInterrupted()) {
            try {
                datagramSocket.receive(datagramPacket);
            } catch (IOException e) {
                e.printStackTrace();
                break;
            }
            byte[] gotData = new byte[datagramPacket.getLength()];
            System.arraycopy(datagramPacket.getData(), 0, gotData, 0, gotData.length);
            final String gotJson = new String(gotData);
            Gson gson = new Gson();
            BaseMsg msg = gson.fromJson(gotJson, BaseMsg.class);
            try {
                switch (msg.getCmd()) {
                    case WaProtocol.CODE_BROADCAST:
                        BroadcastMsg broadcastMsg = gson.fromJson(gotJson, BroadcastMsg.class);
                        if (localIp.equals(broadcastMsg.getLan_ipv4())) {
                            continue;// 收到了本机的广播
                        }
                        break;
                }
            } catch (Exception e) {
                Log.e(TAG, getName() + "转换json出错: ", e);
            }
            Log.d(TAG, getName() + "收到msg: " + msg);
            Log.d(TAG, getName() + " run: address: " + datagramPacket.getAddress() + ", port: " + datagramPacket.getPort()
                    + ", len: " + datagramPacket.getLength() + ", offset: " + datagramPacket.getOffset() + ", " + gotJson);
//            Log.d(TAG, "run: 收到数据 " + bytes2Hex(data));
        }
        Log.d(TAG, getName() + "run: 运行完毕");
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
