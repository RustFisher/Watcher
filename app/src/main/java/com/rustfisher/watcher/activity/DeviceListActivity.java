package com.rustfisher.watcher.activity;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.NetworkInfo;
import android.net.wifi.WpsInfo;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import com.rustfisher.watcher.R;
import com.rustfisher.watcher.utils.LocalUtils;
import com.rustfisher.watcher.views.DeviceListAdapter;

import java.util.Locale;

/**
 * Find and connect Wifi p2p device here.
 * Created by Rust Fisher on 2017/3/1.
 */
public class DeviceListActivity extends Activity {

    private static final String TAG = "rustApp";
    private Button mDisconnectBtn;
    private TextView mInfoTv;
    private TextView mScanBtn;
    private ListView mDeviceLv;
    private DeviceListAdapter mListAdapter;

    private WifiP2pManager mWifiP2pManager;
    private WifiP2pManager.Channel mChannel;

    private ProgressDialog mConnectDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.act_device);
        initUI();
        initUtils();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mConnectDialog.dismiss();
        unregisterReceiver(mBroadcastReceiver);
    }

    private void initUI() {
        mDisconnectBtn = (Button) findViewById(R.id.disconnectBtn);
        mDeviceLv = (ListView) findViewById(R.id.deviceLv);
        mScanBtn = (TextView) findViewById(R.id.scanDeviceBtn);
        mInfoTv = (TextView) findViewById(R.id.infoTv);

        mListAdapter = new DeviceListAdapter(getLayoutInflater());
        mDeviceLv.setAdapter(mListAdapter);

        mConnectDialog = new ProgressDialog(this);
        mConnectDialog.setMessage("Connecting...");

        mDisconnectBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mWifiP2pManager.removeGroup(mChannel, new WifiP2pManager.ActionListener() {
                    @Override
                    public void onSuccess() {
                        mDisconnectBtn.setVisibility(View.GONE);
                    }

                    @Override
                    public void onFailure(int reason) {
                        mDisconnectBtn.setVisibility(View.VISIBLE);
                        Log.e(TAG, "removeGroup onFailure: " + reason);
                    }
                });
            }
        });

        mScanBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mWifiP2pManager.discoverPeers(mChannel, new WifiP2pManager.ActionListener() {
                    @Override
                    public void onSuccess() {
                        mListAdapter.clear();
                        mListAdapter.notifyDataSetChanged();
                    }

                    @Override
                    public void onFailure(int reason) {
                        Log.e(TAG, "WifiP2pManager.discoverPeers onFailure " + reason);
                    }
                });
            }
        });

        mDeviceLv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                connectDevice(mListAdapter.getItem(position));
            }
        });
    }

    private void initUtils() {
        mWifiP2pManager = (WifiP2pManager) getSystemService(Context.WIFI_P2P_SERVICE);
        mChannel = mWifiP2pManager.initialize(this, getMainLooper(), null);
        registerReceiver(mBroadcastReceiver, LocalUtils.makeWiFiP2pIntentFilter());
    }

    private void connectDevice(final WifiP2pDevice device) {
        if (null == device) {
            return;
        }
        final WifiP2pConfig config = new WifiP2pConfig();
        config.deviceAddress = device.deviceAddress;
        config.groupOwnerIntent = 0; // I do not want to be the group owner
        config.wps.setup = WpsInfo.PBC;
        mWifiP2pManager.connect(mChannel, config, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                Log.d(TAG, "onSuccess connect to " + config.deviceAddress);
                mConnectDialog.setMessage("Connect to " + device.deviceName);
                mConnectDialog.show();
            }

            @Override
            public void onFailure(int reason) {
                Log.d(TAG, "connect onFailure: " + reason);
            }
        });
    }


    WifiP2pManager.PeerListListener mPeerListListener = new WifiP2pManager.PeerListListener() {
        @Override
        public void onPeersAvailable(WifiP2pDeviceList peers) {
            mListAdapter.setList(peers.getDeviceList());
            mListAdapter.notifyDataSetChanged();
        }
    };

    // fixme Every time this activity starts, we can receive these broadcasts. Why?
    private BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION.equals(action)) {
                int state = intent.getIntExtra(WifiP2pManager.EXTRA_WIFI_STATE, -1);
                if (state == WifiP2pManager.WIFI_P2P_STATE_ENABLED) {
                    Log.d(TAG, "Wifi P2P is enabled");
                } else {
                    Log.d(TAG, "Wi-Fi P2P is not enabled");
                }
            } else if (WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION.equals(action)) {
                mWifiP2pManager.requestPeers(mChannel, mPeerListListener);
            } else if (WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION.equals(action)) {
                NetworkInfo networkInfo = intent.getParcelableExtra(WifiP2pManager.EXTRA_NETWORK_INFO);
                mDisconnectBtn.setVisibility(networkInfo.isConnected() ? View.VISIBLE : View.GONE);
                if (null != mConnectDialog) {
                    mConnectDialog.dismiss();
                }
            } else if (WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION.equals(action)) {
                WifiP2pDevice device = intent.getParcelableExtra(WifiP2pManager.EXTRA_WIFI_P2P_DEVICE);
                updateLocalDeviceInfo(device);
            }
        }
    };

    private void updateLocalDeviceInfo(WifiP2pDevice device) {
        mInfoTv.setText(String.format(Locale.ENGLISH, "Local Name: %s\nLocal mac address: %s\nLocalStatus: %s",
                device.deviceName, device.deviceAddress, LocalUtils.getDeviceStatusStr(device.status)));
    }

}
