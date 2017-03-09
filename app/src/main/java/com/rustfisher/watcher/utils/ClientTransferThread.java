package com.rustfisher.watcher.utils;

import android.util.Log;

import com.rustfisher.watcher.beans.MsgBean;
import com.rustfisher.watcher.manager.LocalDevice;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;

/**
 * Create a socket to group owner as a client.
 * Created by Rust Fisher on 2017/3/1.
 */
public class ClientTransferThread extends Thread {
    private static final String TAG = "rustApp";
    private static final int SOCKET_TIMEOUT = 5000;

    private InetAddress host;
    private boolean mmRunning;
    private OutputStream os;
    private ObjectOutputStream oos;

    public ClientTransferThread(InetAddress hostAddress) {
        this.host = hostAddress;
        mmRunning = true;
    }

    public void sendMsgBean(MsgBean bean) {
        try {
            oos.writeObject(bean);
            oos.flush();
        } catch (Exception e) {
            e.printStackTrace();
            Log.e(TAG, "sendMsgBean: fail", e);
        }
    }

    @Override
    public void run() {
        super.run();
        Log.d(TAG, getId() + " ClientTransferThread runs.");
        Socket socket = new Socket();
        try {
            socket.bind(null);
            socket.connect((new InetSocketAddress(host, AppConfigs.PORT_GROUP_OWNER)), SOCKET_TIMEOUT);
            Log.d(TAG, "Client socket is connected: " + socket.isConnected());
            if (socket.isConnected()) {
                os = socket.getOutputStream();
                oos = new ObjectOutputStream(os);
                InputStream is = socket.getInputStream();
                while (!isInterrupted() && mmRunning) {
                    byte[] buffer = new byte[1000];
                    int readCount = is.read(buffer);
                    if (readCount > 0) {
                        Log.d(TAG, "ClientTransferThread run: readCount=" + readCount);

                    }
                }
            }
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        Log.e(TAG, getId() + " ClientTransferThread has stopped.");
    }

    public synchronized void closeThread() {
        try {
            mmRunning = false;
            notify();
            interrupt();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
