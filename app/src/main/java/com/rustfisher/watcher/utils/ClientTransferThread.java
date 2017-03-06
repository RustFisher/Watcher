package com.rustfisher.watcher.utils;

import android.util.Log;

import com.rustfisher.watcher.manager.LocalDevice;

import java.io.IOException;
import java.io.InputStream;
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
    private byte[] msg = "Hello from the client".getBytes();
    private boolean mmRunning;
    private OutputStream os;

    public ClientTransferThread(InetAddress hostAddress) {
        this.host = hostAddress;
        mmRunning = true;
    }

    public void send(byte[] msg) {
        this.msg = msg;
        try {
            os.write(msg);
            os.flush();
        } catch (Exception e) {
            e.printStackTrace();
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
                os.write((WPProtocol.MY_IP_ADDRESS + "#" + LocalDevice.getLocalIPAddress()).getBytes());
                InputStream is = socket.getInputStream();
                while (!isInterrupted() && mmRunning) {
                    byte[] buffer = new byte[1000];
                    int readCount = is.read(buffer);
                    if (readCount > 0) {
                        Log.d(TAG, "ClientTransferThread run: readCount=" + readCount);
//                        boolean isAddressCMD = LocalDevice.isAddressCMD(buffer, readCount);
//                        if (isAddressCMD) {
//                            // Receive a client address, now start new a socket to the client
//                            LocalDevice.getInstance().startGroupOwnerTransferThread();
//                            LocalUtils.log_d_Str(buffer, readCount);
//                        } else {
//                        }

                    }
                }
//                while (mmRunning && !isInterrupted()) {
//                    if (!mmIsPause) {
//                        os.write(msg);
//                        os.flush();
//                        pauseThread();
//                    } else {
//                        onThreadWait();
//                    }
//                }
            }
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        Log.d(TAG, getId() + " ClientTransferThread exits.");
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
