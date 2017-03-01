package com.rustfisher.watcher.service;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.NetworkInfo;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;

import com.rustfisher.watcher.utils.AppConfigs;
import com.rustfisher.watcher.utils.LocalUtils;
import com.rustfisher.watcher.manager.LocalDevice;

import java.io.IOException;
import java.io.InputStream;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * Hold some service
 * Created by Rust Fisher on 2017/2/28.
 */
public class CommunicationService extends Service {

    private static final String TAG = "rustApp";

    public static final String MSG_STOP = "com.rustfisher.stop_CommunicationService";

    private WifiP2pManager mManager;
    private WifiP2pManager.Channel mChannel;
    private LocalDevice mLocalDevice = LocalDevice.getInstance();

    private ReceiveSocketThread mGroupOwnerThread;
    private ReceiveSocketThread mClientThread;

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

        IntentFilter intentFilter = LocalUtils.makeWiFiP2pIntentFilter();
        intentFilter.addAction(MSG_STOP);
        registerReceiver(mReceiver, intentFilter);

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        unregisterReceiver(mReceiver);
        Log.d(TAG, "Service onDestroy, 88");
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
        stopClientThread();
    }

    private void stopClientThread() {
        if (null != mClientThread) {
            mClientThread.interrupt();
            mClientThread = null;
        }
    }

    private void stopGroupOwnerThread() {
        if (null != mGroupOwnerThread) {
            mGroupOwnerThread.interrupt();
            mGroupOwnerThread = null;
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
        if (null == mClientThread) {
            mClientThread = new ReceiveSocketThread(AppConfigs.PORT_CLIENT);
            mClientThread.start();
        }
        mLocalDevice.startClientTransferThread();
    }

    /**
     * Receive data via socket.
     */
    class ReceiveSocketThread extends Thread {

        private volatile boolean running = true;
        private ServerSocket serverSocket;
        private final int port;

        public ReceiveSocketThread(int port) {
            this.port = port;
        }

        public boolean isRunning() {
            return running;
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
            Log.d(TAG, "socket run at " + port);
            try {
                serverSocket = new ServerSocket(port);
                Log.d(TAG, "Socket opened");
                Socket client = serverSocket.accept();
                InputStream inputstream = client.getInputStream();
                while (!isInterrupted() && running) {
                    byte[] buffer = new byte[1024];
                    int readCount = inputstream.read(buffer);
                    if (readCount > 0) {
                        LocalUtils.logd(buffer, readCount);
                    }
                }
                Log.d(TAG, "connection over");
            } catch (IOException e) {
                e.printStackTrace();
            }
            Log.d(TAG, "socket thread exits.");
        }
    }

}
