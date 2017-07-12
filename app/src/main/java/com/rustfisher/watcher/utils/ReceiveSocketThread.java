package com.rustfisher.watcher.utils;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.rustfisher.watcher.WiFiApp;
import com.rustfisher.watcher.beans.MsgBean;
import com.rustfisher.watcher.manager.AirSisyphus;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * Receive data via socket.
 */
public class ReceiveSocketThread extends Thread {
    private static final String TAG = "rustApp";

    private volatile boolean running = true;
    private ServerSocket serverSocket;
    private final int port;
    private InputStream inputstream;
    private ObjectInputStream ois;
    private ObjectOutputStream oos;
    private OutputStream outputStream;
    private Context ctx;

    public ReceiveSocketThread(int port) {
        this.ctx = WiFiApp.getApp().getApplicationContext();
        this.port = port;
    }

    public void sendMsgBean(MsgBean msgBean) {
        if (null != oos) {
            try {
                oos.writeObject(msgBean);
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            Log.e(TAG, "[service] sendMsgBean: fail");
        }
    }

    @Override
    public void interrupt() {
        running = false;
        try {
            serverSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        super.interrupt();
    }

    @Override
    public void run() {
        super.run();
        Log.d(TAG, "Receive socket listens to " + port);
        try {
            serverSocket = new ServerSocket(port);
            Log.d(TAG, "Group owner socket opened, waiting...");
            Socket client = serverSocket.accept();
            Log.d(TAG, "Group owner socket accepted. We can talk now!");
            inputstream = client.getInputStream();
            ois = new ObjectInputStream(inputstream);
            outputStream = client.getOutputStream();
            oos = new ObjectOutputStream(outputStream);
            while (!isInterrupted() && running) {
                Log.d(TAG, "Read obj");
                MsgBean readInObj = (MsgBean) ois.readObject();
                Log.d(TAG, "Read in obj");
                if (null != readInObj) {
                    if (readInObj.hasText()) {
                        Log.d(TAG, "[Server] got: " + readInObj.getMsg());
                        Intent textIntent = new Intent(AppConfigs.MSG_ONE_STR);
                        textIntent.putExtra(AppConfigs.MSG_ONE_STR, readInObj.getMsg());
                        ctx.sendBroadcast(textIntent);
                    }
                    if (readInObj.hasPNG()) {
                        Log.d(TAG, "[Server] got a PNG.");
                        AirSisyphus.setOnePicData(readInObj.getPNGBytes());
                        Intent in = new Intent(AppConfigs.MSG_ONE_PIC);
                        ctx.sendBroadcast(in);
                    }
                    if (readInObj.hasJPEG()) {
                        Log.d(TAG, "[Server] got jpg");
                        new SaveJpgThread(readInObj.getJpegBytes()).start();
                    }
                }
            }
            Log.d(TAG, "connection over");
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (null != inputstream) {
                    inputstream.close();
                }
                if (null != outputStream) {
                    outputStream.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        Log.e(TAG, "Group owner socket thread exits.");
    }
}