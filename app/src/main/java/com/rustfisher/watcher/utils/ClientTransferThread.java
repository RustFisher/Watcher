package com.rustfisher.watcher.utils;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.rustfisher.watcher.beans.MsgBean;
import com.rustfisher.watcher.manager.LocalDevice;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
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
    private boolean running;
    private OutputStream os;
    private ObjectOutputStream oos;

    private Context ctx;

    public ClientTransferThread(InetAddress hostAddress, Context context) {
        this.host = hostAddress;
        running = true;
        this.ctx = context;
    }

    public void sendMsgBean(MsgBean bean) {
        try {
            oos.writeObject(bean);
            oos.flush();
        } catch (Exception e) {
            e.printStackTrace();
            Log.e(TAG, "[Client] sendMsgBean: fail", e);
        }
    }

    @Override
    public void run() {
        super.run();
        Log.d(TAG, getId() + " ClientTransferThread runs.");
        Socket socket = new Socket();
        try {
            Thread.sleep(1000);
            socket.bind(null);
            socket.connect((new InetSocketAddress(host, AppConfigs.PORT_GROUP_OWNER)), SOCKET_TIMEOUT);
            Log.d(TAG, "Client socket is connected: " + socket.isConnected());
            if (socket.isConnected()) {
                os = socket.getOutputStream();
                oos = new ObjectOutputStream(os);
                InputStream is = socket.getInputStream();
                ObjectInputStream ois = new ObjectInputStream(is);
                while (!isInterrupted() && running) {
                    try {
                        MsgBean readInObj = (MsgBean) ois.readObject();
                        if (null != readInObj) {
                            if (readInObj.hasText()) {
                                Log.d(TAG, "[Client] got: " + readInObj.getMsg());
                                Intent textIntent = new Intent(AppConfigs.MSG_ONE_STR);
                                textIntent.putExtra(AppConfigs.MSG_ONE_STR, readInObj.getMsg());
                                ctx.sendBroadcast(textIntent);
                            }
                            if (readInObj.hasPNG()) {
                                Log.d(TAG, "[Client] got a PNG.");
                                LocalDevice.setOnePicData(readInObj.getPNGBytes());
                                Intent in = new Intent(AppConfigs.MSG_ONE_PIC);
                                ctx.sendBroadcast(in);
                            }
                            if (readInObj.hasJPEG()) {
                                Log.d(TAG, "[Client] got jpg");
                                new SaveJpgThread(readInObj.getJpegBytes()).start();
                            }
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
            socket.close();
        } catch (Exception e) {
            e.printStackTrace();
            Log.e(TAG, "Client thread: ", e);
        }
        Log.e(TAG, getId() + " ClientTransferThread has stopped.");
    }

    public synchronized void closeThread() {
        try {
            running = false;
            ctx = null;
            notify();
            interrupt();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
