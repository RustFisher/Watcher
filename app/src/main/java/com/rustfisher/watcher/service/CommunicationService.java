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

import com.rustfisher.watcher.manager.AirSisyphus;
import com.rustfisher.watcher.utils.LocalUtils;

/**
 * Hold some service
 * Created by Rust Fisher on 2017/2/28.
 */
public class CommunicationService extends Service {

    private static final String TAG = "rustApp";

    public static final String MSG_STOP = "com.rustfisher.stop_CommunicationService";

    private WifiP2pManager mManager;
    private WifiP2pManager.Channel mChannel;
    private AirSisyphus mAirSisyphus = AirSisyphus.getInstance();


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
        WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        WifiInfo wifiInfo = wifiManager.getConnectionInfo();
        AirSisyphus.setLocalIPAddress(LocalUtils.intToIpStr(wifiInfo.getIpAddress()));
        IntentFilter intentFilter = LocalUtils.makeWiFiP2pIntentFilter();
        intentFilter.addAction(MSG_STOP);
        registerReceiver(mReceiver, intentFilter);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mAirSisyphus.exitDevice();
        unregisterReceiver(mReceiver);
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
                            mAirSisyphus.setWifiP2pInfo(info);
                            if (info.isGroupOwner) {
                                mAirSisyphus.asGroupOwner();
                            } else {
                                mAirSisyphus.asClient();
                            }
                        }
                    });
                } else {
                    mAirSisyphus.stopClientTransferThread();
                    mAirSisyphus.stopGroupOwnerThread();
                }
            } else if (WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION.equals(action)) {
                WifiP2pDevice device = intent.getParcelableExtra(WifiP2pManager.EXTRA_WIFI_P2P_DEVICE);
                mAirSisyphus.setDevice(device);
            }
        }
    };

}
