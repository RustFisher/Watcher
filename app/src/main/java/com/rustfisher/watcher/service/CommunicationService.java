package com.rustfisher.watcher.service;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;

import com.rustfisher.watcher.beans.MsgBean;
import com.rustfisher.watcher.utils.AppConfigs;
import com.rustfisher.watcher.utils.LocalUtils;
import com.rustfisher.watcher.manager.LocalDevice;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;

/**
 * Hold some service
 * Created by Rust Fisher on 2017/2/28.
 */
public class CommunicationService extends Service {

    private static final String TAG = "rustApp";

    public static final String MSG_STOP = "com.rustfisher.stop_CommunicationService";
    public static final String MSG_ONE_PIC = "com.rustfisher.MSG_ONE_PIC";
    public static final String MSG_ONE_CAMERA = "com.rustfisher.msg_camera";
    public static final String MSG_ONE_STR = "com.rustfisher.msg_one_str";

    private WifiP2pManager mManager;
    private WifiP2pManager.Channel mChannel;
    private LocalDevice mLocalDevice = LocalDevice.getInstance();

    private ReceiveSocketThread mGroupOwnerThread;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mManager = (WifiP2pManager) getSystemService(Context.WIFI_P2P_SERVICE);
        mChannel = mManager.initialize(this, getMainLooper(), null);
        WifiManager wifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
        WifiInfo wifiInfo = wifiManager.getConnectionInfo();
        LocalDevice.setLocalIPAddress(LocalDevice.intToIpStr(wifiInfo.getIpAddress()));
        IntentFilter intentFilter = LocalUtils.makeWiFiP2pIntentFilter();
        intentFilter.addAction(MSG_STOP);
        registerReceiver(mReceiver, intentFilter);
        mLocalDevice.setService(this);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mLocalDevice.exitDevice();
        unregisterReceiver(mReceiver);
        mLocalDevice.setService(null);
        Log.d(TAG, "Service onDestroy, bye!");
    }

    private BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (MSG_STOP.equals(action)) {
                stopSelf();
            } else if (WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION.equals(action)) {
                Log.d(TAG, "[Service] wifi p2p connection changed");
                NetworkInfo netInfo = intent.getParcelableExtra(WifiP2pManager.EXTRA_NETWORK_INFO);
                if (netInfo.isConnected()) {
                    mManager.requestConnectionInfo(mChannel, new WifiP2pManager.ConnectionInfoListener() {
                        @Override
                        public void onConnectionInfoAvailable(WifiP2pInfo info) {
                            mLocalDevice.setWifiP2pInfo(info);
                            if (info.isGroupOwner) {
                                asGroupOwner();
                            } else {
                                asClient();
                            }
                        }
                    });
                } else {
                    stopAllSocketThread();
                }
            } else if (WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION.equals(action)) {
                WifiP2pDevice device = intent.getParcelableExtra(WifiP2pManager.EXTRA_WIFI_P2P_DEVICE);
                mLocalDevice.setDevice(device);
            }
        }
    };

    private void stopAllSocketThread() {
        mLocalDevice.stopClientTransferThread();
        stopGroupOwnerThread();
    }

    private void stopGroupOwnerThread() {
        if (null != mGroupOwnerThread) {
            mGroupOwnerThread.interrupt();
            mGroupOwnerThread = null;
        }
    }

    public void send(byte[] msg) {
        if (mGroupOwnerThread != null) {
            mGroupOwnerThread.send(msg);
        } else {
            Log.e(TAG, "send fail");
        }
    }

    private void asGroupOwner() {
        if (mGroupOwnerThread == null) {
            mGroupOwnerThread = new ReceiveSocketThread(AppConfigs.PORT_GROUP_OWNER);
            mGroupOwnerThread.start();
        }
        mLocalDevice.stopClientTransferThread();
    }

    private void asClient() {
        mLocalDevice.startClientTransferThread();
    }

    /**
     * Receive data via socket.
     */
    class ReceiveSocketThread extends Thread {
        private volatile boolean running = true;
        private ServerSocket serverSocket;
        private final int port;
        private ArrayList<Byte> bufferList;
        private InputStream inputstream;
        private ObjectInputStream ois;
        private OutputStream outputStream;

        public ReceiveSocketThread(int port) {
            this.port = port;
            bufferList = new ArrayList<>();
            bufferList.ensureCapacity(10000);
        }

        public void send(byte[] msg) {
            if (null != outputStream) {
                try {
                    outputStream.write(msg);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else {
                Log.e(TAG, "null == outputStream ");
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
            Log.d(TAG, "Receive socket run at " + port);
            try {
                serverSocket = new ServerSocket(port);
                Log.d(TAG, "Group owner socket opened, waiting...");
                Socket client = serverSocket.accept();
                Log.d(TAG, "Group owner socket accepted. We can talk now!");
                inputstream = client.getInputStream();
                ois = new ObjectInputStream(inputstream);
                outputStream = client.getOutputStream();
                while (!isInterrupted() && running) {

                    MsgBean readInObj = (MsgBean) ois.readObject();
                    if (null != readInObj) {
                        if (readInObj.hasText()) {
                            Log.d(TAG, "Server got: " + readInObj.getMsg());
                            Intent textIntent = new Intent(MSG_ONE_STR);
                            textIntent.putExtra(MSG_ONE_STR, readInObj.getMsg());
                            sendBroadcast(textIntent);
                        }
                        if (readInObj.hasPNG()) {
                            Log.d(TAG, "Server got a PNG.");
                            LocalDevice.setOnePicData(readInObj.getPNGBytes());
                            Intent in = new Intent(MSG_ONE_PIC);
                            sendBroadcast(in);
                        }
                        if (readInObj.hasJPEG()) {
                            Intent jIntent = new Intent(MSG_ONE_CAMERA);
                            jIntent.putExtra(MSG_ONE_CAMERA, readInObj);
                            sendBroadcast(jIntent);
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

}
