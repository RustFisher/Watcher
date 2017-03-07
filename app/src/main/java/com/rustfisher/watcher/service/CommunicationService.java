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

import com.rustfisher.watcher.utils.AppConfigs;
import com.rustfisher.watcher.utils.LocalUtils;
import com.rustfisher.watcher.manager.LocalDevice;
import com.rustfisher.watcher.utils.WPProtocol;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Locale;

/**
 * Hold some service
 * Created by Rust Fisher on 2017/2/28.
 */
public class CommunicationService extends Service {

    private static final String TAG = "rustApp";

    public static final String MSG_STOP = "com.rustfisher.stop_CommunicationService";
    public static final String MSG_ONE_PIC = "com.rustfisher.MSG_ONE_PIC";
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
                outputStream = client.getOutputStream();
                while (!isInterrupted() && running) {
                    byte[] buffer = new byte[WPProtocol.BASE_ONE_PACKAGE_DATA_LEN + WPProtocol.DATA_HEAD_PNG.length + WPProtocol.DATA_END.length];
                    int readCount = inputstream.read(buffer);
                    if (readCount > 0) {
                        arrangeDataBuffer(buffer, readCount);
                    }
                }
                Log.d(TAG, "connection over");
            } catch (IOException e) {
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
            Log.d(TAG, "socket thread exits.");
        }

        private void arrangeDataBuffer(byte[] buffer, int readCount) {
            Log.d(TAG, "arrangeDataBuffer: read count=" + readCount);
//            LocalUtils.logBytes(buffer, readCount);
            for (int i = 0; i < readCount; i++) {
                bufferList.add(buffer[i]);
            }
            final int baseDataLen = WPProtocol.BASE_ONE_PACKAGE_DATA_LEN;
            for (int i = 0; i < bufferList.size() && (bufferList.size() >= (baseDataLen + WPProtocol.DATA_HEAD_PNG.length + WPProtocol.DATA_END.length)); i++) {
                // scan the buffer list
                int bufferCount = bufferList.size();
                int startIndex = -1;
                int stopIndex = -1;
                int msgType = -1;
//                LocalUtils.logList(bufferList);
                boolean foundStart = false;
                boolean foundEnd = false;
                for (int scanIndex = 0; scanIndex <= bufferCount - 3; scanIndex++) {
                    if (!foundStart) {
                        if (bufferList.get(scanIndex) == WPProtocol.DATA_HEAD_1 &&
                                bufferList.get(scanIndex + 1) == WPProtocol.DATA_HEAD_2) {
                            if (bufferList.get(scanIndex + 2) == WPProtocol.DATA_TYPE_PIC) {
                                foundStart = true;
                                msgType = WPProtocol.DATA_TYPE_PIC;
                                startIndex = scanIndex + 3;
                            } else if (bufferList.get(scanIndex + 2) == WPProtocol.DATA_TYPE_STR) {
                                foundStart = true;
                                msgType = WPProtocol.DATA_TYPE_STR;
                                startIndex = scanIndex + 3;
                            }
                        }
                    }
                    if (!foundEnd) {
                        if (bufferList.get(scanIndex) == WPProtocol.DATA_END_1 &&
                                bufferList.get(scanIndex + 1) == WPProtocol.DATA_END_2 &&
                                bufferList.get(scanIndex + 2) == WPProtocol.DATA_END_3) {
                            stopIndex = scanIndex - 1;
                            foundEnd = true;
                        }
                    }
                }
                if (startIndex < stopIndex && startIndex > 0) {
                    int dataLen = stopIndex - startIndex + 1;
                    byte[] dataBytes = new byte[dataLen];
                    Log.d(TAG, String.format(Locale.ENGLISH, "we got len=%d , bufferCount = %d, [%d, %d]",
                            dataLen, bufferCount, startIndex, stopIndex));
                    for (int m = 0; m < dataLen; m++) {
                        dataBytes[m] = bufferList.get(m + startIndex);
                    }

                    int leftDataCount = bufferCount - stopIndex - WPProtocol.DATA_END_LEN;
                    if (leftDataCount <= 0) {
                        bufferList.clear();
                    } else {
                        ArrayList<Byte> leftList = new ArrayList<>();
                        for (int add_i = 0; add_i < leftDataCount; add_i++) {
                            leftList.add(bufferList.get(bufferCount - leftDataCount + add_i));
                        }
                        bufferList = new ArrayList<>(leftList);
                    }
//                    while (bufferList.size() >= (bufferCount - stopIndex - 3)) {
//                        bufferList.remove(0);
//                    }

                    Log.d(TAG, "got one pic, then list size == " + bufferList.size());
                    switch (msgType) {
                        case WPProtocol.DATA_TYPE_PIC:
                            LocalDevice.setOnePicData(dataBytes);
                            Intent in = new Intent(MSG_ONE_PIC);
                            sendBroadcast(in);
                            break;
                        case WPProtocol.DATA_TYPE_STR:
                            Intent inStr = new Intent(MSG_ONE_STR);
                            String msg = new String(dataBytes);
                            Log.d(TAG, "msg = " + msg);
                            inStr.putExtra(MSG_ONE_STR, msg);
                            sendBroadcast(inStr);
                            break;
                    }
                } else if (startIndex >= 3 && stopIndex < startIndex) {
                    Log.d(TAG, "we got start index but no stop index. start index " + startIndex);
                    while (bufferList.size() >= (bufferCount - startIndex + 4)) {
                        bufferList.remove(0);
                    }
                    break;
                } else if (startIndex == -1 && stopIndex > 0) {
                    Log.d(TAG, "only find the stop index");
                    while (bufferList.size() >= (bufferCount - stopIndex + 2)) {
                        bufferList.remove(0);
                    }
                    break;
                } else {
                    // we got nothing
                    Log.d(TAG, "got nothing, clear buffer");
                    byte f1 = bufferList.get(bufferCount - 3);
                    byte f2 = bufferList.get(bufferCount - 2);
                    byte f3 = bufferList.get(bufferCount - 1);
                    bufferList.clear();
                    bufferList.add(f1);
                    bufferList.add(f2);
                    bufferList.add(f3);
                }
//                LocalUtils.logList(bufferList);
            }
        }
    }

}
