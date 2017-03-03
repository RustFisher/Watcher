package com.rustfisher.watcher.utils;

import android.util.Log;

import com.rustfisher.watcher.manager.LocalDevice;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;

/**
 * Create a socket to client as a group owner.
 * Created by Rust Fisher on 2017/3/2.
 */
public class GroupOwnerTransferThread extends Thread {
    private static final String TAG = "rustApp";
    private static final int SOCKET_TIMEOUT = 5000;

    private InetAddress host;
    private String msg = "Hello, this is GroupOwner. How R U ?";
    private boolean mmRunning;
    private boolean mmIsPause;

    public GroupOwnerTransferThread(InetAddress hostAddress) {
        this.host = hostAddress;
        mmRunning = true;
    }

    public void send(String msg) {
        this.msg = msg;
        resumeThread();
    }

    @Override
    public void run() {
        super.run();
        Log.d(TAG, getId() + " GroupOwnerTransferThread runs.");
        Socket socket = new Socket();
        try {
            socket.bind(null);
            socket.connect((new InetSocketAddress(host, AppConfigs.PORT_CLIENT)), SOCKET_TIMEOUT);
            Log.d(TAG, "I am a group owner, connect to client. Socket is connected: " + socket.isConnected());
            if (socket.isConnected()) {
                OutputStream os = socket.getOutputStream();
                os.write(msg.getBytes());
                pauseThread();
                while (mmRunning && !isInterrupted()) {
                    if (!mmIsPause) {
                        os.write(msg.getBytes());
                        os.flush();
                        pauseThread();
                    } else {
                        onThreadWait();
                    }
                }
            }
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        Log.d(TAG, getId() + " GroupOwnerTransferThread exits.");
    }

    public synchronized void pauseThread() {
        mmIsPause = true;
    }

    private void onThreadWait() {
        try {
            synchronized (this) {
                this.wait();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public synchronized void resumeThread() {
        mmIsPause = false;
        this.notify();
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
