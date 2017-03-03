package com.rustfisher.watcher.activity;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.NetworkInfo;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.rustfisher.watcher.R;
import com.rustfisher.watcher.utils.LocalUtils;
import com.rustfisher.watcher.manager.LocalDevice;
import com.rustfisher.watcher.service.CommunicationService;

import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    public static final String TAG = "rustApp";
    private TextView mLogTv;
    private TextView mLocalInfoTv;
    private LinearLayout mMsgLin;
    private Button mDisconnectBtn;
    private Button mSendMsgBtn;
    private EditText mClientEt;

    private WifiP2pManager mWifiP2pManager;
    private WifiP2pManager.Channel mChannel;
    private LocalDevice mLocalDevice = LocalDevice.getInstance();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.act_main);
        initUI();
        initUtils();
    }

    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(mBroadcastReceiver, LocalUtils.makeWiFiP2pIntentFilter());
        mMsgLin.setVisibility(LocalDevice.getInstance().isClient() ? View.VISIBLE : View.INVISIBLE);
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(mBroadcastReceiver);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        exitApp();
        Log.d(TAG, "MainActivity onDestroy, bye!");
    }

    private void exitApp() {
        sendBroadcast(new Intent(CommunicationService.MSG_STOP));
    }

    private void initUI() {
        mClientEt = (EditText) findViewById(R.id.et);
        mSendMsgBtn = (Button) findViewById(R.id.sendBtn);
        mMsgLin = (LinearLayout) findViewById(R.id.clientLin);
        mDisconnectBtn = (Button) findViewById(R.id.disconnectBtn);
        mLocalInfoTv = (TextView) findViewById(R.id.localInfoTv);
        mLogTv = (TextView) findViewById(R.id.logTv);

        mDisconnectBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Use removeGroup to disconnect a connecting channel
                mWifiP2pManager.removeGroup(mChannel, new WifiP2pManager.ActionListener() {
                    @Override
                    public void onSuccess() {
                        Log.d(TAG, "onSuccess: removeGroup");
                    }

                    @Override
                    public void onFailure(int reason) {
                        Log.d(TAG, "onFailure: removeGroup " + reason);
                    }
                });
            }
        });

        findViewById(R.id.goScanActBtn).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(getApplicationContext(), DeviceListActivity.class));
            }
        });

        mSendMsgBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mLocalDevice.isClient()) {
                    mLocalDevice.sendMsgToGroupOwner(mClientEt.getText().toString());
                    mClientEt.setText("");
                } else if (mLocalDevice.isGroupOwner()) {
                    mLocalDevice.sendMsgToClient(mClientEt.getText().toString());
                    mClientEt.setText("");
                }
            }
        });
    }

    private void initUtils() {
        startService(new Intent(getApplicationContext(), CommunicationService.class));
        mWifiP2pManager = (WifiP2pManager) getSystemService(Context.WIFI_P2P_SERVICE);
        mChannel = mWifiP2pManager.initialize(this, getMainLooper(), null);
        Log.d(TAG, "Main manager " + mWifiP2pManager.toString() + " MainAct mChannel: " + mChannel);
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Log.d(TAG, "InetAddress.getLocalHost:  " + InetAddress.getLocalHost());
                    Log.d(TAG, "Inet4Address.getLocalHost: " + Inet4Address.getLocalHost());
                    Log.d(TAG, "Inet6Address.getLocalHost: " + Inet6Address.getLocalHost());
                } catch (UnknownHostException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    private BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION.equals(action)) {
                int state = intent.getIntExtra(WifiP2pManager.EXTRA_WIFI_STATE, -1);
                if (state == WifiP2pManager.WIFI_P2P_STATE_ENABLED) {
                    logUI("Wifi P2P is enabled");
                } else {
                    logUI("Wi-Fi P2P is not enabled");
                }
            } else if (WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION.equals(action)) {

            } else if (WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION.equals(action)) {
                NetworkInfo netInfo = intent.getParcelableExtra(WifiP2pManager.EXTRA_NETWORK_INFO);
                mDisconnectBtn.setVisibility(netInfo.isConnected() ? View.VISIBLE : View.INVISIBLE);
                mMsgLin.setVisibility(netInfo.isConnected() ? View.VISIBLE : View.INVISIBLE);
            } else if (WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION.equals(action)) {
                WifiP2pDevice device = intent.getParcelableExtra(WifiP2pManager.EXTRA_WIFI_P2P_DEVICE);
                updateLocalDeviceInfo(device);
            }
        }
    };

    private void updateLocalDeviceInfo(WifiP2pDevice device) {
        mLocalInfoTv.setText(String.format(Locale.ENGLISH, "Name: %s\nMac address: %s\nStatus: %s",
                device.deviceName, device.deviceAddress, LocalUtils.getDeviceStatusStr(device.status)));
    }

    private void logUI(String str) {
        Log.d(TAG, str);
        mLogTv.append(str);
        mLogTv.append("\n");
    }

}
